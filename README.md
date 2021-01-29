# query

> The OMOP and ATLAS-based Query Module

## Development

Start OMOP DB, ATLAS, a FHIR server, and the screening list module as external development services:

```sh
docker-compose -f deploy/docker-compose.dev.yml up
```

Note that this uses the `docker.miracum.org/miracum-etl/omop/empty:latest-cdm5.3.1` image to create an
empty OMOP database which takes around 5 minutes to startup.

This starts the services on the following localhost ports:

| Service        | Port  | URL                                     |
| -------------- | ----- | --------------------------------------- |
| FHIR Server    | 8082  | <http://localhost:8082/>                |
| OHDSI ATLAS    | 8083  | <http://localhost:8083/atlas>           |
| Screening List | 8084  | <http://localhost:8084/>                |
| OMOP Database  | 25432 | jdbc:postgresql://localhost:25432/OHDSI |

Create a sample Cohort in Atlas by running the following:

```sh
curl --header "Content-Type: application/json" \
     --request POST \
     --data @deploy/test-cohort.json \
     http://localhost:8083/WebAPI/cohortdefinition
```

## Build

Non-docker builds require JDK 11+ to be installed on your machine.

### Build from commandline

```sh
./gradlew build
```

The generated .jar file will be located in the `build/libs` directory.

### Docker

```sh
docker build -t query:test  .
```

## Contributing

### Setup pre-commit hooks

This sets up a pre-commit hook to enforce basic file sanity checks:

```sh
pre-commit install
```
