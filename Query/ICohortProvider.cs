using System.Collections.Generic;
using System.Threading.Tasks;

namespace Query
{
    /// <summary>
    /// Allows for querying patient records.
    /// </summary>
    public interface ICohortProvider
    {
        /// <summary>
        /// Returns a list of patient identifiers included in the cohort identified by <paramref name="id"/>.
        /// </summary>
        /// <param name="id">Cohort identifier.</param>
        /// <returns>A list of patient identifiers included in the cohort.</returns>
        Task<List<string>> GetAsync(int id);
    }
}
