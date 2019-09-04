using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Query
{
    public class OmopConnector : IOmopDatabaseClient
    {
        public static bool CheckConnection()
        {
            return true;
        }

        public Task<List<string>> GetIdsFromCohort(string id)
        {
            throw new NotImplementedException();
        }
    }
}
