using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Hl7.Fhir.Model;
using Hl7.Fhir.Rest;
using Query.Models.Api;

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
        public async Task<List> CreateScreeningListAsync(CohortDefinition cohortMeta, IEnumerable<string> patientIds)
        {
            var cohortId = cohortMeta.Id.ToString();

            var trxBuilder = new TransactionBuilder(FhirClient.Endpoint);

            var ids = patientIds.ToArray();

            foreach (var id in ids)
            {
                var patient = new Patient();
                patient.Identifier.Add(new Identifier(FhirSystems.OmopSubjectIdentifier, id));
                patient.Id = id;

                // create the patients if they don't already exist
                var patientCondition = new SearchParams();
                patientCondition.Add("identifier", $"{FhirSystems.OmopSubjectIdentifier}|{id}");
                trxBuilder.Create(patient, patientCondition);
            }

            var entries = ids.Select(id =>
            {
                var entry = new List.EntryComponent { Item = new ResourceReference($"Patient/{id}") };
                return entry;
            });

            var researchStudy = new ResearchStudy
            {
                Title = cohortMeta.Name,
                Description = new Markdown(cohortMeta.Description),
                Identifier = new List<Identifier>()
                {
                    new Identifier { System = FhirSystems.OmopCohortIdentifier, Value = cohortId, },
                },
            };

            var studyUpdateCondition = new SearchParams();
            studyUpdateCondition.Add("identifier", $"{FhirSystems.OmopCohortIdentifier}|{cohortId}");
            trxBuilder.Update(studyUpdateCondition, researchStudy);

            var researchStudyId = $"urn:uuid:{Guid.NewGuid()}";
            var screeningList = new List();
            screeningList.Identifier.Add(new Identifier(FhirSystems.ScreeningListCohortIdentifier, cohortId));
            screeningList.Mode = ListMode.Working;
            screeningList.Code = new CodeableConcept(FhirSystems.ScreeningListCodingSystem, "screening-recommendations");
            screeningList.Extension.Add(new Extension
            {
                Url = FhirSystems.ScreeningListStudyReference,
                Value = new ResourceReference(researchStudyId),
            });
            screeningList.Entry.AddRange(entries);

            var listUpdateCondition = new SearchParams();
            listUpdateCondition.Add("identifier", $"{FhirSystems.ScreeningListCohortIdentifier}|{cohortId}");
            trxBuilder.Update(listUpdateCondition, screeningList);

            var bundle = trxBuilder.ToBundle();
            bundle.Type = Bundle.BundleType.Transaction;
            var rsEntry = bundle.Entry.First(entry => entry.Resource.ResourceType == ResourceType.ResearchStudy);
            rsEntry.FullUrl = researchStudyId;
            await FhirClient.TransactionAsync(bundle);
            var listResults = await FhirClient.SearchAsync<List>(listUpdateCondition);

            return listResults.Entry.FirstOrDefault()?.Resource as List;
        }
    }
}
