using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;

namespace Query.Controllers
{
    /// <summary>
    /// Implements the cohort REST endpoint.
    /// </summary>
    [Route("api/[controller]")]
    [ApiController]
    public class CohortsController : ControllerBase
    {
        /// <summary>
        /// Initializes a new instance of the <see cref="CohortsController"/> class.
        /// </summary>
        /// <param name="cohortProvider">The cohort provider to use.</param>
        public CohortsController(ICohortProvider cohortProvider)
        {
            Cohorts = cohortProvider;
        }

        private ICohortProvider Cohorts { get; }

        /// <summary>
        /// Returns a list of patient identifiers included in the cohort identified by <paramref name="id"/>.
        /// </summary>
        /// <param name="id">Atlas cohort identifier.</param>
        /// <returns>A list of patient identifiers included in the cohort.</returns>
        [HttpGet("{id}", Name = "Get")]
        public async Task<List<string>> Get(string id)
        {
            return await Cohorts.GetAsync(id);
        }
    }
}
