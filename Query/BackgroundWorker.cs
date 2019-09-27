using System;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Polly;

namespace Query
{
    /// <inheritdoc/>
    public class BackgroundWorker : BackgroundService
    {
        private readonly ILogger<BackgroundWorker> logger;

        /// <summary>
        /// Initializes a new instance of the <see cref="BackgroundWorker"/> class.
        /// </summary>
        /// <param name="logger">The <see cref="ILogger"/>  to use.</param>
        /// <param name="apiClient">The <see cref="IAtlasApiClient"/>  to use.</param>
        /// <param name="cohortProvider">The <see cref="ICohortProvider"/>  to use.</param>
        /// <param name="screeningList">The <see cref="IScreeningListService"/>  to use.</param>
        /// <param name="config">The <see cref="IConfiguration"/>  to use.</param>
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

                // retry with exponential backoff
                var retryPolicy = Policy
                    .Handle<ApplicationException>()
                    .Or<Npgsql.PostgresException>()
                    .Or<Npgsql.NpgsqlException>()
                    .WaitAndRetryForeverAsync(
                        retryAttempt => TimeSpan.FromSeconds(5 * retryAttempt),
                        (exception, timespan) =>
                        {
                            logger.LogWarning(
                                exception,
                                "Failed to poll for cohort and create screening list.. Retrying in {retryTimeSeconds}",
                                timespan.TotalSeconds);
                        });

                await retryPolicy.ExecuteAsync(async () =>
                {
                    var cohortDefinitions = AtlasApi.GetCohortDefinitions();

                    logger.LogInformation("Found {numCohortDefinitions} cohort definitions.", cohortDefinitions.Count);

                    foreach (var cohortDefinition in cohortDefinitions)
                    {
                        logger.LogDebug("Generating screening list for {cohortId}.", cohortDefinition.Id);

                        var cohort = await Cohorts.GetAsync(cohortDefinition.Id);
                        await ScreeningList.CreateScreeningListAsync(cohortDefinition, cohort);
                    }
                });

                var nextRunAfterMinutes = Config.GetValue<int>("OmopPollTimeMinutes");
                logger.LogInformation("Done generating screening lists for all cohorts. Next run in {nextRunAfterMinutes}", nextRunAfterMinutes);
                await Task.Delay(TimeSpan.FromMinutes(nextRunAfterMinutes), stoppingToken);
            }
        }
    }
}
