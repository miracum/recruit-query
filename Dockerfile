FROM gradle:6.4.1-jdk11 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon --info

# Collect and print code coverage information:
RUN gradle --no-daemon jacocoTestReport && \
    awk -F"," '{ instructions += $4 + $5; covered += $5 } END { print covered, "/", instructions,\
    " instructions covered"; print 100*covered/instructions, "% covered" }' build/jacoco/coverage.csv

FROM gcr.io/distroless/java:11
COPY --from=build /home/gradle/src/build/libs/*.jar /opt/query.jar
USER nonroot
ENV SPRING_PROFILES_ACTIVE=prod
CMD ["/opt/query.jar"]

ARG VERSION=0.0.0
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
