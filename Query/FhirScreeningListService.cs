using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Hl7.Fhir.Model;
using Hl7.Fhir.Rest;

namespace Query
{
    /// <inheritdoc />
    public class FhirScreeningListService : IScreeningListService
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="FhirScreeningListService"/> class.
        /// </summary>
        /// <param name="fhirClient">The FHIR client for communication with a FHIR server.</param>
        public FhirScreeningListService(IFhirClient fhirClient)
        {
            this.FhirClient = fhirClient;
        }

        private IFhirClient FhirClient { get; }

        /// <inheritdoc />
        public async Task<List> CreateScreeningListAsync(string cohortId, IEnumerable<string> patientIds)
        {
            var trxBuilder = new TransactionBuilder(FhirClient.Endpoint);

            foreach (var id in patientIds)
            {
                var patient = new Patient();
                patient.Identifier.Add(new Identifier(FhirSystems.OmopSubjectIdentifier, id));
                patient.Id = id;

                // create the patients if they don't already exist
                var patientCondition = new SearchParams();
                patientCondition.Add("identifier", $"{FhirSystems.OmopSubjectIdentifier}|{id}");
                trxBuilder.Create(patient, patientCondition);
            }

            var entries = patientIds.Select(id =>
            {
                var entry = new List.EntryComponent();
                entry.Item = new ResourceReference($"Patient/{id}");
                return entry;
            });

            var screeningList = new List();
            screeningList.Identifier.Add(new Identifier(FhirSystems.ScreeningListCohortIdentifier, cohortId));
            screeningList.Mode = ListMode.Working;
            screeningList.Code = new CodeableConcept(FhirSystems.ScreeningListCodingSystem, "screening-recommendations");
            screeningList.Entry.AddRange(entries);

            var listCondition = new SearchParams();
            listCondition.Add("identifier", $"{FhirSystems.ScreeningListCohortIdentifier}|{cohortId}");
            trxBuilder.Update(listCondition, screeningList);

            var patientBundle = trxBuilder.ToBundle();
            patientBundle.Type = Bundle.BundleType.Transaction;
            var created = await FhirClient.TransactionAsync(patientBundle);

            var listResults = await FhirClient.SearchAsync<List>(listCondition);
            return listResults.Entry.FirstOrDefault().Resource as List;
        }
    }
}
