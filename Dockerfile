FROM maven:3.6.2-jdk-11-slim AS build
WORKDIR /src
COPY . .
RUN mvn -DskipTests install --batch-mode --errors --fail-at-end --show-version
RUN mvn -DskipTests assembly:assembly

FROM openjdk:11-jre-slim
COPY --from=build /src/target/query-module.jar /opt/query-module.jar
ENTRYPOINT ["java", "-Dquery.startupDelay=40000", "-jar","/opt/query-module.jar"]

LABEL maintainer="miracum"
