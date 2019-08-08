# query - The OMOP/ATLAS-based Query Module

## Project setup

Start OMOP, ATLAS, and a FHIR server as external development services:

Expects a docker volume "volume-pg" to be generated and filled with OMOP data according to step 1 of the [ohdsi-omop-v5 README](https://gitlab.miracum.org/miracum-etl/ohdsi-omop-v5).

```
docker-compose -f docker/docker-compose.dev.yml up
```

The default port-mappings start a FHIR-Server on localhost:8082, the OMOP WebAPI on localhost:8081/WebAPI, and ATLAS on localhost:8081/atlas. 