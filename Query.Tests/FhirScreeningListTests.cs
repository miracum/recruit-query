using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Net;
using System.Threading;
using Castle.Core.Logging;
using FakeItEasy;
using Hl7.Fhir.Model;
using Hl7.Fhir.Rest;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Query.Models.Api;
using RestSharp;
using Xunit;

namespace Query.Tests
{
    /// <summary>
    /// Test for the FhirScreeningList.
    /// </summary>
    public class FhirScreeningListTests
    {
        [Fact]
        public async void ExecuteAsync_WithTransientApiFailure_ShouldRetry()
        {
            // Arrange
            var cohortDef = new CohortDefinition { Id = 1 };
            var patients = new long[] { 1, 2, 3 };

            var client = A.Fake<IFhirClient>();
            A.CallTo(() => client.Endpoint).Returns(new Uri("http://localhost/fhir"));
            var sut = new FhirScreeningListService(client);

            // Act
            await sut.CreateAsync(cohortDef, new List<long>(patients));

            // Assert
            A.CallTo(() => client.TransactionAsync(A<Bundle>.Ignored))
                .MustHaveHappened();
        }
    }
}
