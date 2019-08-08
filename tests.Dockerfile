FROM mcr.microsoft.com/dotnet/core/sdk:3.0 AS build
WORKDIR /src
COPY ["Query.Tests/Query.Tests.csproj", "Query.Tests/"]
RUN dotnet restore "Query.Tests/Query.Tests.csproj"
COPY . .
WORKDIR /src/Query.Tests
RUN dotnet test
