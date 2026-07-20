FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY ajkmasjid/pom.xml .
COPY ajkmasjid/src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=120s \
  CMD curl -sf http://localhost:8080/api/v1/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
