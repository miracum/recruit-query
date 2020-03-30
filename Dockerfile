FROM gradle:6.3.0-jdk11 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon --info

FROM adoptopenjdk:11-jre-openj9
COPY --from=build /home/gradle/src/build/libs/*.jar /opt/query.jar
ARG VERSION=0.0.0
ENV app.version=${VERSION}
ENTRYPOINT ["java", "-jar", "/opt/query.jar"]

ARG GIT_REF=""
ARG BUILD_TIME=""

LABEL org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="miracum.org" \
    org.opencontainers.image.source="https://gitlab.miracum.org/miracum/uc1/recruit/query" \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="miracum.org" \
    org.opencontainers.image.title="uc1-recruit-query" \
    org.opencontainers.image.description="Query module of the patient recruitment system."
