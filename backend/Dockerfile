# ── Backend Dockerfile ────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-eng curl
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /app/uploads/receipts /app/uploads/profiles
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
