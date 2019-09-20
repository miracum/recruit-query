using System;
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
        /// <param name="screeningList">The screening list creation service.</param>
        public CohortsController(ICohortProvider cohortProvider, IScreeningListService screeningList)
        {
            Cohorts = cohortProvider;
            ScreeningList = screeningList;
        }

        private ICohortProvider Cohorts { get; }

        private IScreeningListService ScreeningList { get; }

        /// <summary>
        /// Returns a list of patient identifiers included in the cohort identified by <paramref name="id"/>.
        /// </summary>
        /// <param name="id">Atlas cohort identifier.</param>
        /// <returns>A list of patient identifiers included in the cohort.</returns>
        [HttpGet("{id}", Name = "Get")]
        public async Task<IEnumerable<string>> Get(string id)
        {
            var cohort = await Cohorts.GetAsync(id);

            // await ScreeningList.CreateScreeningListAsync(id, cohort);
            return cohort;
        }
    }
}
