# query - The OMOP/ATLAS-based Query Module

## Project setup

Start OMOP, ATLAS, and a FHIR server as external development services:

Expects a docker volume "volume-pg" to be generated and filled with OMOP data according to step 1 of the [ohdsi-omop-v5 README](https://gitlab.miracum.org/miracum-etl/ohdsi-omop-v5).

```
docker-compose -f docker/docker-compose.dev.yml up
```

The default port-mappings start a FHIR-Server on http://localhost:8083/fhir, the OMOP WebAPI on http://localhost:8082/WebAPI, and ATLAS on http://localhost:8082/atlas.

## Contributing

### Setup commitlint
This sets up a pre-commit hook checking if the commit message follows the [conventional commit spec](https://www.conventionalcommits.org/en/v1.0.0-beta.4/).
```
npm install
```


## Build

Non-docker builds requires a Oracle or OpenJDK and Maven-Runtime to be installed on your machine.

### Build from cmd
```sh
mvn -DskipTests=true assembly:assembly -P production
```
this is with skipping tests.
Jar will be in target-folder of project

### Build in eclipse/intelliJ
No need of installing maven, it's included.
Eclipse automatically selects the maven-profile which has the "activeByDefault=true".
Add a new profile in the pom.xml to fit your development environment.

### Run Test
```sh
mvn -P test install
```

### Docker

#### Build Image (TODO)
```sh
docker build -t query:test -f Dockerfile .
```