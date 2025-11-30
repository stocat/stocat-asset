# syntax=docker/dockerfile:1.7

# Build runs outside the image; this Dockerfile only packages the JAR and OTEL agent.
# Expect the application JAR to be present at exchange-rate-crawler/build/libs/*.jar before docker build.

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

ARG OTEL_AGENT_VERSION=2.8.0

RUN mkdir -p /opt/otel
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar

COPY build/libs/*.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-javaagent:/opt/otel/opentelemetry-javaagent.jar"
ENV OTEL_TRACES_EXPORTER=otlp \
    OTEL_METRICS_EXPORTER=otlp \
    OTEL_LOGS_EXPORTER=otlp \
    OTEL_EXPORTER_OTLP_PROTOCOL=grpc

ENTRYPOINT ["java","-jar","/app/app.jar"]
