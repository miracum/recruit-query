using System;
using System.Collections.Generic;
using System.Configuration;
using System.Net;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Query.Models.Api;
using RestSharp;

namespace Query
{
    /// <summary>
    /// Handles REST calls to the ohdsi api to generate a new cohort.
    /// The task finishes if the cohort creation is completed.
    /// </summary>
    public class AtlasApiClient : IAtlasApiClient
    {
        private readonly string generateCohortRequestTemplate = "cohortdefinition/{cohortId}/generate/OHDSI-CDMV5";
        private readonly string cohortStatusRequestTemplate = "cohortdefinition/{cohortId}/info";
        private readonly string cohortDefinitionTemplate = "cohortdefinition";
        private readonly string complete = "COMPLETE";
        private readonly string pending = "PENDING";
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
            var cohortDefinitionRequest = new RestRequest(cohortDefinitionTemplate, Method.GET);
            var response = ohdsiClient.Execute(cohortDefinitionRequest);

            if (response.StatusCode != HttpStatusCode.OK)
            {
                throw new ApplicationException($"Error retrieving list of cohort definitions.",
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
            return await QueryChohortGenerationStatus(cohortId);
        }

        private bool StartCohortGeneration(int cohortId)
        {
            var generateCohortRequest = new RestRequest(generateCohortRequestTemplate, Method.GET);
            generateCohortRequest.AddUrlSegment("cohortId", cohortId);
            var generateResponse = ohdsiClient.Execute(generateCohortRequest);
            return generateResponse.StatusCode == HttpStatusCode.OK;
        }

        private async Task<bool> QueryChohortGenerationStatus(int cohortId)
        {
            var cohortStatusRequest = new RestRequest(cohortStatusRequestTemplate, Method.GET);
            cohortStatusRequest.AddUrlSegment("cohortId", cohortId);
            while (true)
            {
                var cohortStatusResponse = ohdsiClient.Execute(cohortStatusRequest);
                var jsonResponse = JsonConvert.DeserializeObject<dynamic>(cohortStatusResponse.Content);
                HttpStatusCode httpStatusCode = cohortStatusResponse.StatusCode;
                if (httpStatusCode == HttpStatusCode.OK)
                {
                    string generationStatus = jsonResponse[0].status;
                    if (!generationStatus.Equals(pending))
                    {
                        return generationStatus.Equals(complete);
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
