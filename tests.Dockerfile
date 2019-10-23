FROM maven:3-jdk-8

LABEL maintainer="miracum"

COPY ./src /usr/src/query/src
COPY ./pom.xml /usr/src/query/
COPY ./assembly.xml /usr/src/query/
WORKDIR /usr/src/query
RUN mvn -DskipTests -P test assembly:assembly
CMD ["java", "-Dquery.startupDelay=40000","-jar", "/usr/src/query/target/query-module.jar"]