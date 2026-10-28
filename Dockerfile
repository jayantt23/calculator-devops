# syntax=docker/dockerfile:1.7

###############################################################################
# Stage 1 - build
###############################################################################
FROM eclipse-temurin:17-jdk AS build

# Install Maven
RUN apt-get update \
 && apt-get install -y --no-install-recommends maven \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# --- Dependency layer caching -----------------------------------------------
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# --- Application build ------------------------------------------------------
COPY src ./src
# FIX 1: Removed the '-o' (offline) flag so missing test dependencies can download
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests \
 && cp target/*.jar application.jar

###############################################################################
# Stage 2 - runtime
###############################################################################
FROM eclipse-temurin:17-jre-alpine AS runtime

# Dedicated unprivileged user (no shell, no home directory)
RUN addgroup -S spring && adduser -S -G spring -H -s /sbin/nologin spring

WORKDIR /app

# FIX 2: Copy the built fat JAR directly instead of broken layer extractions
COPY --from=build --chown=spring:spring /build/application.jar ./

USER spring:spring

EXPOSE 8080

# Container-aware JVM tuning; override at runtime with -e JAVA_OPTS="..."
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# 'exec' replaces the shell with the JVM so it is PID 1 and receives SIGTERM directly
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar application.jar"]