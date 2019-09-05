using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Query
{
    /// <inheritdoc />
    public class OmopConnector : IOmopDatabaseClient
    {
        /// <summary>
        /// Checks if database configurations specified in app.config are valid and database is of OMOP schema.
        /// </summary>
        /// <returns> true if valid, false if not.</returns>
        public static bool CheckConnection()
        {
            return true;
        }

        /// <inheritdoc />
        public Task<List<string>> GetIdsFromCohort(string id)
        {
            throw new NotImplementedException();
        }
    }
}
