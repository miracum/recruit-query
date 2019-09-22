using System.Threading.Tasks;

namespace Query
{
    /// <summary>
    /// Handles REST calls to the ohdsi api to generate a new cohort.
    /// The task finishes if the cohort creation is completed.
    /// </summary>
    public interface IAtlasApiClient
    {
        /// <summary>
        /// Request the ohdsi API to create a cohort.
        /// The task finishes if the cohort creation is completed.
        /// </summary>
        /// <param name="cohortId">The id from the cohort.</param>
        /// <returns>Return a boolean for the creation status.</returns>
        Task<bool> GenerateCohortAsync(string cohortId);
    }
}
