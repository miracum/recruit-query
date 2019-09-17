﻿using System.Collections.Generic;
using System.Threading.Tasks;
using FakeItEasy;
using FluentAssertions;
using Query.Controllers;
using Xunit;

namespace Query.Tests
{
    /// <summary>
    /// Placeholder.
    /// </summary>
    public class CohortsControllerTests
    {
        /// <summary>
        /// Placeholder.
        /// </summary>
        [Fact]
        public async void Get_WithAnyId_ReturnsListOfPatients()
        {
            // Arrange
            var cohort = new List<string> { "A", "B", "C" };
            var provider = A.Fake<ICohortProvider>();

            A.CallTo(() => provider.GetAsync(A<string>.Ignored))
                .Returns(Task.FromResult(cohort));

            var sut = new CohortsController(provider, A.Fake<IScreeningListService>());

            // Act
            var result = await sut.Get("1");

            // Assert
            result.Should().Contain(cohort);
        }
    }
}
