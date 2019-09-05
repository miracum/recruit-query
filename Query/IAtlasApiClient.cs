using System.Threading.Tasks;

namespace Query
{
    /// <summary>
    /// The client to the OHDSI Atlas API.
    /// </summary>
    public interface IAtlasApiClient
    {
        /// <summary>
        /// Generates the cohort in the OMOP database.
        /// </summary>
        /// <returns>A task that represents the asynchronous generation operation.</returns>
        Task GenerateCohortAsync();
    }
}
