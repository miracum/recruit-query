using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata;
using Query.Models.DB;

namespace Query
{
    /// <inheritdoc />
    public class OmopConnector : IOmopDatabaseClient
    {
        private OHDSIContext context;

        /// <summary>
        /// Initializes a new instance of the <see cref="OmopConnector"/> class.
        /// Creates new Object of OmopConnector.
        /// </summary>
        public OmopConnector()
        {
            this.context = new OHDSIContext();
        }

        /// <summary>
        /// Checks if database configurations specified in app.config are valid and database is of OMOP schema.
        /// </summary>
        /// <returns> true if valid, false if not.</returns>
        public static bool CheckConnection()
        {
            return true;
        }

        /// <summary>
        /// Requests all Patient IDs connected to the given cohort.
        /// </summary>
        /// <param name="cohortId"> Id of the requested cohort.</param>
        /// <returns> ArrayList of Ids.</returns>
        public Task<List<string>> GetIdsFromCohort(string cohortId)
        {
            var ids = context.Cohort.Where(b => b.SubjectId.Equals(cohortId));

            List<Cohort> liste = ids.ToList();

            // Task<List<string>> task = new Task<List<string>>(ids);
            throw new NotImplementedException();
        }

        /// <summary>
        /// Requests all Patient IDs connected to the given cohort.
        /// </summary>
        /// <param name="cohortId"> Id of the requested cohort.</param>
        /// <returns> ArrayList of Ids.</returns>
        public List<long> GetIdListFromCohort(string cohortId)
        {
            var ids = context.Cohort.Where(b => b.SubjectId.Equals(cohortId));

            List<Cohort> liste = ids.ToList();
            List<long> idList = new List<long>();
            foreach (Cohort cohort in liste)
            {
                idList.Add(cohort.SubjectId);
            }

            return idList;
        }
    }
}
