FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
ARG MODULE=service-a
COPY pom.xml /src/pom.xml
COPY services /src/services
RUN mvn -q -pl services/${MODULE} -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ARG MODULE=service-a
ARG OTEL_AGENT_VERSION=2.31.1
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
COPY --from=build /src/services/${MODULE}/target/app.jar /app/app.jar
COPY services/entrypoint.sh /entrypoint.sh
RUN sed -i 's/\r$//' /entrypoint.sh && chmod +x /entrypoint.sh
ENV OTEL_ENABLED=true
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
