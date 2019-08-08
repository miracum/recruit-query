FROM mcr.microsoft.com/dotnet/core/sdk:3.0 AS build
WORKDIR /src
COPY ["Query/Query.csproj", "Query/"]
RUN dotnet restore "Query/Query.csproj"
COPY . .
WORKDIR /src/Query
RUN dotnet build "Query.csproj" -c Release -o /app

FROM build AS publish
RUN dotnet publish "Query.csproj" -c Release -o /app

FROM mcr.microsoft.com/dotnet/core/aspnet:3.0 AS deploy
WORKDIR /app
COPY --from=publish /app .
ENTRYPOINT ["dotnet", "Query.dll"]
