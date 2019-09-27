using System.Collections.Generic;
using System.Threading.Tasks;
using Query.Models.Api;
using Fhir = Hl7.Fhir.Model;

namespace Query
{
    /// <summary>
    /// Service to generate screening lists from the given patient ids.
    /// </summary>
    public interface IScreeningListService
    {
        /// <summary>
        /// Creates and stores a screening list for the given cohort.
        /// </summary>
        /// <param name="cohortMeta">The defintion for the cohort of the cohort for which to create the screening list.</param>
        /// <param name="patients">The members of the cohort as patient identifiers.</param>
        /// <returns>A <see cref="Task"/> representing the result of the asynchronous operation.</returns>
        Task<Fhir.List> CreateScreeningListAsync(CohortDefinition cohortMeta, IEnumerable<string> patients);
    }
}
