# syntax=docker/dockerfile:1

# ==============================================================================
# Multi-Stage Build: Crunchy Code Bookstore Backend
# Stage 1: Dependency resolution and compiled JAR packaging
# Stage 2: Hardened, minimal JRE runtime with non-root security context
# ==============================================================================

FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile and package application (skipping tests for container image artifact)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Production Runtime Stage
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Create non-root system group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder /workspace/target/crunchy-java-backend-t5-monkey9-Cyber-cat-Spidy-1.0.0.jar app.jar

# Enforce file permissions
RUN chown -R appuser:appgroup /app

USER appuser

# Expose standard web traffic port (Render and Railway inject PORT at runtime)
EXPOSE 8080

# Environment Defaults
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
