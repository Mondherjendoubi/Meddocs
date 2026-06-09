# syntax=docker/dockerfile:1

# ---- Build stage: compile + package the fat jar with Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the POM first and warm the dependency cache. This layer is only
# rebuilt when pom.xml changes, so source-only edits stay fast.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now the sources. Tests are skipped here: they need a live DB (@SpringBootTest),
# which isn't available during an image build — they run via CI / locally instead.
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage: small JRE image with just the jar ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# *.jar matches the repackaged Spring Boot jar (the plain one is *.jar.original).
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
