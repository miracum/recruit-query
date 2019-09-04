using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Query
{
    public class OmopCohortProvider : ICohortProvider
    {
        public OmopCohortProvider(IAtlasApiClient apiClient, IOmopDatabaseClient dbClient)
        {
            ApiClient = apiClient;
            DbClient = dbClient;
        }

        public IAtlasApiClient ApiClient { get; }

        public IOmopDatabaseClient DbClient { get; }

        public async Task<List<string>> GetAsync(string id)
        {
            await ApiClient.GenerateCohortAsync();

            return await DbClient.GetIdsFromCohort(id);
        }
    }
}
