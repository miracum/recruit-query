using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Query.Models.Omop;

namespace Query
{
    /// <inheritdoc />
    public class OmopDatabaseClient : IOmopDatabaseClient
    {
        private readonly OmopContext context;

        /// <summary>
        /// Initializes a new instance of the <see cref="OmopDatabaseClient"/> class.
        /// Creates new Object of OmopConnector.
        /// </summary>
        public OmopDatabaseClient(OmopContext context)
        {
            this.context = context;
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

            for (int i = 0; i < subjectsArray.Count(); i++)
            {
                subjectIds.Add(subjectsArray[i].SubjectId.ToString());
            }

            return subjectIds;
        }
    }
}
