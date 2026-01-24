# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY services/common/pom.xml services/common/
COPY services/prediction-engine-service/pom.xml services/prediction-engine-service/
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -pl services/prediction-engine-service -am
COPY services/common/src services/common/src
COPY services/prediction-engine-service/src services/prediction-engine-service/src
RUN ./mvnw clean package -DskipTests -pl services/prediction-engine-service -am

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup
USER appuser
COPY --from=build /app/services/prediction-engine-service/target/prediction-engine-service-*.jar app.jar
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1
EXPOSE 8085
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
