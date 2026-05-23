# ==========================================
# STAGE 1: Build compiling standard fat JAR
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy the pom.xml first to leverage Docker layer caching for Maven dependencies
COPY pom.xml .

# Pre-fetch all dependencies to make builds faster on subsequent runs
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Compile and package the application, skipping unit tests during packaging
# (they are verified locally and in CI/CD pipelines instead)
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Lightweight Alpine JRE Runtime
# ==========================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create a non-root system user for superior security in container environments
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

# Copy only the compiled fat JAR from the builder stage
COPY --from=builder /app/target/springboot-backend-*.jar ./app.jar

# Define port 3000 as exposing, which matches application.yml port mapping
EXPOSE 3000

# Set JVM configurations for optimal memory footprint inside containers
ENTRYPOINT ["java", "-XX:+UseParallelGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
