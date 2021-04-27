# query

> The OMOP-based Query Module

## Development

Start OMOP DB, ATLAS, a FHIR server, and the screening list module as external development services:

```sh
docker-compose -f deploy/docker-compose.dev.yml up
```

Note that this uses the `quay.io/miracum/omop:test-data` image to create an OMOP database with test.

This starts the services on the following localhost ports:

| Service        | Port  | URL                                     |
| -------------- | ----- | --------------------------------------- |
| FHIR Server    | 8082  | <http://localhost:8082/>                |
| OHDSI ATLAS    | 8083  | <http://localhost:8083/atlas>           |
| Screening List | 8084  | <http://localhost:8084/>                |
| OMOP Database  | 25432 | jdbc:postgresql://localhost:25432/ohdsi |

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

## How the `quay.io/miracum/omop:test-data` image is build

1. a Postgres image with the OMOP CDM setup is created by
   following <https://gitlab.miracum.org/miracum/etl/ohdsi-omop-v5/-/tree/master/precreated/README.md>
1. sample data is added to the empty `cds_cdm` by
   applying [init-sample-db.sql](deploy/init-sample-db.sql)
1. sample cohorts are created by running

   ```sh
   curl --header "Content-Type: application/json" \
        --request POST \
        --data @deploy/sample-f-cohort.json \
        http://localhost:8083/WebAPI/cohortdefinition
   ```

   ```sh
    curl --header "Content-Type: application/json" \
         --request POST \
         --data @deploy/sample-m-cohort.json \
         http://localhost:8083/WebAPI/cohortdefinition
   ```

1. all connections to the DB are closed, and it's gracefully stopped by logging into the container
   and running

   ```sh
   su postgres
   pg_ctl stop
   ```

   This basically ensures that the postgres container doesn't take as long to restart,
   see <https://github.com/docker-library/postgres/issues/544>

1. the running container is committed to a new image by running

   ```sh
   docker commit deploy_omopdb_1 quay.io/miracum/omop:test-data
   ```

1. finally, the image is pushed to quay.io

   ```sh
   docker push quay.io/miracum/omop:test-data
   ```
