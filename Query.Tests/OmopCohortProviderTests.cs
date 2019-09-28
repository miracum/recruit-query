using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Net;
using System.Threading;
using Castle.Core.Logging;
using FakeItEasy;
using FluentAssertions;
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
    /// Test for the OmopCohortProvider.
    /// </summary>
    public class OmopCohortProviderTests
    {
        [Fact]
        public async void GetAsync_WithAnyCohortId_ShouldReturnPatientListFromDatabaseClient()
        {
            // Arrange
            var patients = new long[] { 1, 2, 3 };

            var dbClient = A.Fake<IOmopDatabaseClient>();
            A.CallTo(() => dbClient.GetIdsFromCohort(A<int>.Ignored))
                .Returns(new List<long>(patients));
            var sut = new OmopCohortProvider(A.Fake<IAtlasApiClient>(), dbClient);

            // Act
            var result = await sut.GetAsync(1);

            // Assert
            result.Should().Contain(patients);
        }
    }
}
