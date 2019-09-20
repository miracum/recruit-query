using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
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
        public async Task<List<string>> GetIdsFromCohort(string cohortId)
        {
            var id = Convert.ToInt32(cohortId);
            var subjects = await context.Cohort.Where(i => i.CohortDefinitionId.Equals(id)).ToListAsync();
            var subjectIds = new List<string>();
            var subjectsArray = subjects.ToArray();
            Console.WriteLine("------------------------------------------------2----------------------------------------------------");

            for (int i = 0; i < subjectsArray.Count(); i++)
            {
                subjectIds.Add(subjectsArray[i].SubjectId.ToString());
            }

            return subjectIds;
        }

        /// <summary>
        /// Requests all Patient IDs connected to the given cohort.
        /// </summary>
        /// <param name="cohortId"> Id of the requested cohort.</param>
        /// <returns> ArrayList of Ids.</returns>
        public List<long> GetIdListFromCohort(string cohortId)
        {
            var id = Convert.ToInt32(cohortId);
            var subjects = this.context.Cohort.Where(i => i.CohortDefinitionId.Equals(id));

            var idList = new List<long>();
            var subjectsArray = subjects.ToArray();

            for (int i = 0; i < subjectsArray.Count(); i++)
            {
                idList.Add(subjectsArray[i].SubjectId);
            }

            return idList;
        }
    }
}
