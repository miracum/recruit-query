FROM mcr.microsoft.com/dotnet/core/sdk:3.0 AS build
WORKDIR /src
COPY ["Query.Tests/Query.Tests.csproj", "Query.Tests/"]
RUN dotnet restore "Query.Tests/Query.Tests.csproj"
COPY . .
WORKDIR /src/Query.Tests
RUN dotnet test

# FROM build AS publish
# RUN dotnet publish "Query.csproj" -c Release -o /app

# FROM mcr.microsoft.com/dotnet/core/aspnet:3.0 AS deploy
# WORKDIR /app
# COPY --from=publish /app .
# ENTRYPOINT ["dotnet", "Query.dll"]
