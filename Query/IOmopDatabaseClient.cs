using System.Collections.Generic;
using System.Threading.Tasks;

namespace Query
{
    public interface IOmopDatabaseClient
    {
        Task<List<string>> GetIdsFromCohort(string id);
    }
}
