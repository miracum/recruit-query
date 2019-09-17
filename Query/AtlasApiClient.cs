﻿using System.Configuration;
using System.Net;
using System.Threading.Tasks;
using Newtonsoft.Json;
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
        private readonly string complete = "COMPLETE";
        private readonly int waitTime = 10;
        private readonly IRestClient ohdsiClient;

        /// <summary>
        /// Initializes a new instance of the <see cref="AtlasApiClient"/> class.
        /// </summary>
        /// <param name="ohdsiClient">Client for Rest calls.</param>
        public AtlasApiClient(IRestClient ohdsiClient)
        {
            this.ohdsiClient = ohdsiClient;
        }

        /// <summary>
        /// Request the ohdsi API to create a cohort.
        /// The task finishes if the cohort creation is completed.
        /// </summary>
        /// <param name="cohortId">The id from the cohort.</param>
        /// <returns>Return a boolean for the creation status.</returns>
        public async Task<bool> GenerateCohortAsync(string cohortId)
        {
            var generateCohortRequest = new RestRequest(generateCohortRequestTemplate, Method.GET);
            generateCohortRequest.AddUrlSegment("cohortId", cohortId);
            var generateResponse = ohdsiClient.Execute(generateCohortRequest);
            HttpStatusCode statusCode = generateResponse.StatusCode;
            if (statusCode != HttpStatusCode.OK)
            {
                return false;
            }

            var cohortStatusRequest = new RestRequest(cohortStatusRequestTemplate, Method.GET);
            cohortStatusRequest.AddUrlSegment("cohortId", cohortId);
            while (true)
            {
                var cohortStatusResponse = ohdsiClient.Execute(cohortStatusRequest);
                var jsonResponse = JsonConvert.DeserializeObject<dynamic>(cohortStatusResponse.Content);
                HttpStatusCode httpStatusCode = cohortStatusResponse.StatusCode;
                string generationStatus = jsonResponse[0].status;
                if (httpStatusCode == HttpStatusCode.OK && generationStatus.Equals(complete))
                {
                    return true;
                }
                else
                {
                    await Task.Delay(waitTime);
                }
            }
        }
    }
}
