using System.Net;
using FakeItEasy;
using RestSharp;
using Xunit;

namespace Query.Tests
{
    /// <summary>
    /// Test the AtlasApiClient.
    /// </summary>
    public class AtlasApiClientTests
    {
        /// <summary>
        /// Successfully generate a cohort.
        /// </summary>
        [Fact]
        public async void GenerateCohortAsync_Successful()
        {
            // Arrange
            const int cohortId = 1;
            var ohdsiClientFake = A.Fake<IRestClient>();
            var okResponse = new RestResponse
            {
                StatusCode = HttpStatusCode.OK,
            };
            var completeResponse = new RestResponse
            {
                StatusCode = HttpStatusCode.OK,
                Content = "[{'status': 'COMPLETE'}]",
            };
            var pendingResponse = new RestResponse
            {
                StatusCode = HttpStatusCode.OK,
                Content = "[{'status': 'PENDING'}]",
            };
            A.CallTo(() => ohdsiClientFake.Execute(A<IRestRequest>._)).Returns(okResponse).Once().Then.Returns(pendingResponse).Twice().Then.Returns(completeResponse);

            // Act
            var atlasApiClient = new AtlasApiClient(ohdsiClientFake);
            bool result = await atlasApiClient.GenerateCohortAsync(cohortId);

            // Assert
            Assert.True(result);
        }

        /// <summary>
        /// Fails at generating a cohort.
        /// </summary>
        [Fact]
        public async void GenerateCohortAsync_Failed()
        {
            // Arrange
            const int cohortId = 1;
            var ohdsiClientFake = A.Fake<IRestClient>();
            var errorResponse = new RestResponse()
            {
                StatusCode = HttpStatusCode.InternalServerError,
            };
            A.CallTo(() => ohdsiClientFake.Execute(A<IRestRequest>._)).Returns(errorResponse);

            // Act
            var atlasApiClient = new AtlasApiClient(ohdsiClientFake);
            bool result = await atlasApiClient.GenerateCohortAsync(cohortId);

            // Assert
            Assert.False(result);
        }
    }
}
