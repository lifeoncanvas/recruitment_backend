# ---- Build stage: compile the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- Run stage: small Java runtime image ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Run as an unprivileged user
RUN useradd --system --create-home licht
COPY --from=build /app/target/licht-backend-1.0.0.jar app.jar
RUN mkdir -p /app/uploads && chown -R licht /app
USER licht
EXPOSE 8080
# Secrets (database, ADMIN_PASSWORD, JWT_SECRET, storage) come from the host's environment settings
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
