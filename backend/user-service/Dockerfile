# syntax=docker/dockerfile:1.7
ARG SERVICE_NAME=user-service
ARG ARTIFACT_NAME=user-service-0.1.0-SNAPSHOT.jar

FROM maven:3.9.9-eclipse-temurin-21 AS builder
ARG SERVICE_NAME
WORKDIR /workspace
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY backend backend
RUN --mount=type=cache,target=/root/.m2 mvn -pl backend/${SERVICE_NAME} -am clean package -DskipTests
RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.charsets,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.management,jdk.unsupported,jdk.zipfs \
    --strip-java-debug-attributes \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=zip-6 \
    --output /opt/java-runtime

FROM debian:bookworm-slim AS runtime
ARG SERVICE_NAME
ARG ARTIFACT_NAME
ENV JAVA_HOME=/opt/java-runtime \
    PATH="/opt/java-runtime/bin:${PATH}"
WORKDIR /app
COPY --from=builder /opt/java-runtime /opt/java-runtime
COPY --from=builder /workspace/backend/${SERVICE_NAME}/target/${ARTIFACT_NAME} /app/app.jar
EXPOSE 9002
ENTRYPOINT ["java","-jar","/app/app.jar"]
