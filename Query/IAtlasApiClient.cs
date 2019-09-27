using System.Collections.Generic;
using System.Threading.Tasks;
using Query.Models.Api;

namespace Query
{
    /// <summary>
    /// Handles REST calls to the ohdsi api to generate a new cohort.
    /// The task finishes if the cohort creation is completed.
    /// </summary>
    public interface IAtlasApiClient
    {
        /// <summary>
        /// Request the OHDSI API to create a cohort.
        /// The task finishes if the cohort creation is completed.
        /// </summary>
        /// <param name="cohortId">The id from the cohort.</param>
        /// <returns>Return a boolean for the creation status.</returns>
        Task<bool> GenerateCohortAsync(int cohortId);

        /// <summary>
        /// Returns a collection of all <see cref="CohortDefinition"/> present on the server.
        /// </summary>
        /// <returns>A <see cref="Task{TResult}"/> representing the result of the asynchronous operation.</returns>
        List<CohortDefinition> GetCohortDefinitions();
    }
}
