using System;
using System.Collections.Generic;
using System.Net;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Query.Models.Api;
using RestSharp;

namespace Query
{
    /// <summary>
    /// Handles REST calls to the OHDSI api to generate a new cohort.
    /// The task finishes if the cohort creation is completed.
    /// </summary>
    public class AtlasApiClient : IAtlasApiClient
    {
        private const string GenerateCohortRequestTemplate = "cohortdefinition/{cohortId}/generate/OHDSI-CDMV5";
        private const string CohortStatusRequestTemplate = "cohortdefinition/{cohortId}/info";
        private const string CohortDefinitionTemplate = "cohortdefinition";
        private const string Complete = "COMPLETE";
        private const string Pending = "PENDING";
        private readonly TimeSpan waitTime = TimeSpan.FromSeconds(10);
        private readonly IRestClient ohdsiClient;

        /// <summary>
        /// Initializes a new instance of the <see cref="AtlasApiClient"/> class.
        /// </summary>
        /// <param name="ohdsiClient">Client for Rest calls.</param>
        public AtlasApiClient(IRestClient ohdsiClient)
        {
            this.ohdsiClient = ohdsiClient;
        }

        /// <inheritdoc />
        public List<CohortDefinition> GetCohortDefinitions()
        {
            var cohortDefinitionRequest = new RestRequest(CohortDefinitionTemplate, Method.GET);
            var response = ohdsiClient.Execute(cohortDefinitionRequest);

            if (response.StatusCode != HttpStatusCode.OK)
            {
                throw new ApplicationException(
                    "Error retrieving list of cohort definitions.",
                    response.ErrorException);
            }

            return JsonConvert.DeserializeObject<List<CohortDefinition>>(response.Content);
        }

        /// <inheritdoc />
        public async Task<bool> GenerateCohortAsync(int cohortId)
        {
            if (!StartCohortGeneration(cohortId))
            {
                return false;
            }

            return await QueryCohortGenerationStatus(cohortId);
        }

        private bool StartCohortGeneration(int cohortId)
        {
            var generateCohortRequest = new RestRequest(GenerateCohortRequestTemplate, Method.GET);
            generateCohortRequest.AddUrlSegment("cohortId", cohortId);
            var generateResponse = ohdsiClient.Execute(generateCohortRequest);
            return generateResponse.StatusCode == HttpStatusCode.OK;
        }

        private async Task<bool> QueryCohortGenerationStatus(int cohortId)
        {
            var cohortStatusRequest = new RestRequest(CohortStatusRequestTemplate, Method.GET);
            cohortStatusRequest.AddUrlSegment("cohortId", cohortId);
            while (true)
            {
                var cohortStatusResponse = ohdsiClient.Execute(cohortStatusRequest);
                var jsonResponse = JsonConvert.DeserializeObject<dynamic>(cohortStatusResponse.Content);
                HttpStatusCode httpStatusCode = cohortStatusResponse.StatusCode;
                if (httpStatusCode == HttpStatusCode.OK)
                {
                    string generationStatus = jsonResponse[0].status;
                    if (!generationStatus.Equals(Pending))
                    {
                        return generationStatus.Equals(Complete);
                    }
                    else
                    {
                        await Task.Delay(waitTime);
                    }
                }
                else
                {
                    return false;
                }
            }
        }
    }
}
