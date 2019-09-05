using System.Collections.Generic;
using System.Threading.Tasks;

namespace Query
{
    /// <summary>
    /// Interface to the OMOP database.
    /// </summary>
    public interface IOmopDatabaseClient
    {
        /// <summary>
        /// Gets the list of patient ids belonging to a given cohort.
        /// </summary>
        /// <param name="id">Internal identifier of the cohort.</param>
        /// <returns>A list of patient identifiers belonging to the given cohort.</returns>
        Task<List<string>> GetIdsFromCohort(string id);
    }
}
