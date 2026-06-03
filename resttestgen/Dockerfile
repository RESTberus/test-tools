# Stage 1: Build Java with Gradle
FROM gradle:8.14-jdk17 AS builder

WORKDIR /app

COPY build.gradle* ./
COPY src/ ./src/

RUN gradle clean build -x test

# Stage 2: Runtime (Eclipse Temurin 17 JRE)
FROM eclipse-temurin:17-jre-jammy AS runtime

ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError -XX:+UseParallelGC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=75.0"

WORKDIR /tool

# Copy built JAR from build stage
COPY --from=builder /app/build/libs/*-all.jar ./resttestgen.jar

# Copy config files
COPY rtg-config.yml ./rtg-config.yml

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar resttestgen.jar"]
