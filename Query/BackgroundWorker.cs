using System;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace Query
{
    /// <inheritdoc/>
    public class BackgroundWorker : BackgroundService
    {
        private readonly ILogger<BackgroundWorker> logger;

        /// <summary>
        /// Initializes a new instance of the <see cref="BackgroundWorker"/> class.
        /// </summary>
        /// <param name="logger"></param>
        /// <param name="apiClient"></param>
        /// <param name="cohortProvider"></param>
        /// <param name="screeningList"></param>
        /// <param name="config"></param>
        public BackgroundWorker(
            ILogger<BackgroundWorker> logger,
            IAtlasApiClient apiClient,
            ICohortProvider cohortProvider,
            IScreeningListService screeningList,
            IConfiguration config)
        {
            this.logger = logger;
            AtlasApi = apiClient;
            Cohorts = cohortProvider;
            ScreeningList = screeningList;
            Config = config;
        }

        private IAtlasApiClient AtlasApi { get; }

        private ICohortProvider Cohorts { get; }

        private IScreeningListService ScreeningList { get; }

        private IConfiguration Config { get; }

        /// <inheritdoc/>
        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                logger.LogInformation("Worker running at: {time}", DateTimeOffset.Now);

                try
                {
                    var cohortDefinitions = AtlasApi.GetCohortDefinitions();

                    logger.LogInformation("Found {numCohortDefinitions} cohort definitions.", cohortDefinitions.Count);

                    foreach (var cohortDefinition in cohortDefinitions)
                    {
                        logger.LogDebug("Generating screening list for {cohortId}.", cohortDefinition.Id);

                        var cohort = await Cohorts.GetAsync(cohortDefinition.Id);
                        await ScreeningList.CreateScreeningListAsync(cohortDefinition, cohort);
                    }
                }
                catch (Exception exc)
                {
                    logger.LogError(exc, "Failed to poll for and create screening list.");
                    continue;
                }

                await Task.Delay(TimeSpan.FromMinutes(Config.GetValue<int>("OmopPollTimeMinutes")), stoppingToken);
            }
        }
    }
}
