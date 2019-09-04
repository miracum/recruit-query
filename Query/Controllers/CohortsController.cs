using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;

namespace Query.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class CohortsController : ControllerBase
    {
        public CohortsController(ICohortProvider cohortProvider)
        {
            Cohorts = cohortProvider;
        }

        public ICohortProvider Cohorts { get; }

        // GET: api/cohorts/5
        [HttpGet("{id}", Name = "Get")]
        public async Task<List<string>> Get(string id)
        {
            return await Cohorts.GetAsync(id);
        }
    }
}
