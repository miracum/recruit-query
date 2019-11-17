FROM maven:3.6.2-jdk-11-slim AS build
WORKDIR /src
COPY . .
RUN mvn -DskipTests install --batch-mode --errors --fail-at-end --show-version
RUN mvn -DskipTests assembly:assembly

FROM openjdk:11-jre-slim
COPY --from=build /src/target/query.jar /opt/query.jar
ENTRYPOINT ["java", "-Dquery.startupDelay=40000", "-jar","/opt/query.jar"]

LABEL maintainer="miracum.org" \
    org.label-schema.schema-version="1.0" \
    org.label-schema.vendor="MIRACUM" \
    org.label-schema.name="query" \
    org.label-schema.description="MIRACUM Use Case 1 Query Module" \
    org.label-schema.vcs-url="https://gitlab.miracum.org/uc1/recruit/query"
