# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY backend backend
RUN --mount=type=cache,target=/root/.m2 mvn -pl backend/payment-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/backend/payment-service/target/payment-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 9005
ENTRYPOINT ["java","-jar","app.jar"]
