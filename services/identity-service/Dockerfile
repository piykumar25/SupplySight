# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY services/common/pom.xml services/common/
COPY services/identity-service/pom.xml services/identity-service/

# Download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -pl services/identity-service -am

# Copy source code
COPY services/common/src services/common/src
COPY services/identity-service/src services/identity-service/src

# Build the application
RUN ./mvnw clean package -DskipTests -pl services/identity-service -am

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup
USER appuser

# Copy the built artifact
COPY --from=build /app/services/identity-service/target/identity-service-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Expose port
EXPOSE 8081

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
