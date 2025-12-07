# syntax=docker/dockerfile:1.7

# Build runs outside the image; this Dockerfile only packages the JAR and OTEL agent.
# Expect the application JAR to be present at auth-api/build/libs/*.jar before docker build.

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

# Re-declare ARG inside the stage to make it available to instructions in this stage
ARG OTEL_AGENT_VERSION=2.8.0

# Prepare location for the agent and add it
RUN mkdir -p /opt/otel
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar

# Copy application jar built by Gradle
COPY build/libs/*.jar /app/app.jar

ENV OTEL_AGENT_ENABLED=true

RUN cat <<'EOF' >/entrypoint.sh
#!/usr/bin/env sh
set -e

if [ "${OTEL_AGENT_ENABLED:-true}" = "true" ]; then
  AGENT_OPTION="-javaagent:/opt/otel/opentelemetry-javaagent.jar"
  if [ -n "${JAVA_TOOL_OPTIONS}" ]; then
    export JAVA_TOOL_OPTIONS="${AGENT_OPTION} ${JAVA_TOOL_OPTIONS}"
  else
    export JAVA_TOOL_OPTIONS="${AGENT_OPTION}"
  fi
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar "$@"
EOF

RUN chmod +x /entrypoint.sh

# Common OTEL defaults (can be overridden at deploy time)
ENV OTEL_TRACES_EXPORTER=otlp \
    OTEL_METRICS_EXPORTER=otlp \
    OTEL_LOGS_EXPORTER=otlp \
    OTEL_EXPORTER_OTLP_PROTOCOL=grpc

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
