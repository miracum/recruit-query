# query - The OMOP/ATLAS-based Query Module

## Project setup

Start OMOP, ATLAS, and a FHIR server as external development services:

Expects a docker volume "volume-pg" to be generated and filled with OMOP data according to step 1 of the [ohdsi-omop-v5 README](https://gitlab.miracum.org/miracum-etl/ohdsi-omop-v5).

```
docker-compose -f docker/docker-compose.dev.yml up
```

The default port-mappings start a FHIR-Server on localhost:8082, the OMOP WebAPI on localhost:8081/WebAPI, and ATLAS on localhost:8081/atlas.

## Contributing

### Setup commitlint
This sets up a pre-commit hook checking if the commit message follows the [conventional commit spec](https://www.conventionalcommits.org/en/v1.0.0-beta.4/).
```
npm install
```


## Build

Non-docker builds requires .NET Core 3.0 to be installed on your machine.

### Build
```sh
dotnet restore
dotnet build
```

### Run Test
```sh
dotnet test
```

### Docker

#### Build Image
```sh
docker build -t query:test -f Dockerfile .
```