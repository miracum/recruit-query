# query - The OMOP/ATLAS-based Query Module

## Project setup

Start OMOP, ATLAS, and a FHIR server as external development services:

Expects a docker volume "volume-pg" to be generated and filled with OMOP data according to step 1 of the [ohdsi-omop-v5 README](https://gitlab.miracum.org/miracum/etl/ohdsi-omop-v5).

```sh
docker-compose -f deploy/docker-compose.dev.yml up
```

The default port-mappings start a FHIR-Server on <http://localhost:8083/fhir>, the OMOP WebAPI on <http://localhost:8082/WebAPI>, and ATLAS on <http://localhost:8082/atlas>.

## Build

Non-docker builds requires a Oracle or OpenJDK to be installed on your machine.

### Build from commandline

```sh
./gradlew build
```

The generated .jar file will be located in the build/libs directory.

### Docker

```sh
docker build -t query:test -f Dockerfile .
```

## Contributing

### Setup commitlint

This sets up a pre-commit hook checking if the commit message follows the [conventional commit spec](https://www.conventionalcommits.org/en/v1.0.0/).

```sh
npm install
```
