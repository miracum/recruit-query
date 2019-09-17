using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Query
{
    /// <inheritdoc />
    public class OmopCohortProvider : ICohortProvider
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="OmopCohortProvider"/> class.
        /// </summary>
        /// <param name="apiClient">The <see cref="IAtlasApiClient"/> to use.</param>
        /// <param name="dbClient">The <see cref="IOmopDatabaseClient"/> to use.</param>
        public OmopCohortProvider(IAtlasApiClient apiClient, IOmopDatabaseClient dbClient)
        {
            ApiClient = apiClient;
            DbClient = dbClient;
        }

        private IAtlasApiClient ApiClient { get; }

        private IOmopDatabaseClient DbClient { get; }

        /// <inheritdoc />
        public async Task<List<string>> GetAsync(string id)
        {
            await ApiClient.GenerateCohortAsync(id);

            return await DbClient.GetIdsFromCohort(id);
        }
    }
}
