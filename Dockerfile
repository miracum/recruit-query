FROM gradle:6.1.1-jdk11 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon --info

FROM adoptopenjdk:11-jre-openj9
COPY --from=build /home/gradle/src/build/libs/*.jar /opt/query.jar
ARG VERSION=0.0.0
ENV app.version=${VERSION}
ENTRYPOINT ["java", "-jar", "/opt/query.jar"]

LABEL maintainer="miracum.org" \
    org.label-schema.schema-version="1.0" \
    org.label-schema.vendor="MIRACUM" \
    org.label-schema.name="query" \
    org.label-schema.description="MIRACUM Use Case 1 Query Module" \
    org.label-schema.vcs-url="https://gitlab.miracum.org/uc1/recruit/query"
