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
        /// <returns> A list of Ids.</returns>
        public async Task<List<string>> GetIdsFromCohort(int cohortId)
        {
            var subjects = await context.Cohort
                .Where(c => c.CohortDefinitionId == cohortId)
                .ToListAsync();

            return subjects
                .Select(subject => subject.SubjectId.ToString())
                .ToList();
        }
    }
}
