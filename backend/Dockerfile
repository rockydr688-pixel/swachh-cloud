# ==========================================
# CloudClean AI - Spring Boot Backend Dockerfile
# Multi-stage build with Eclipse Temurin JDK 21
# ==========================================

# Stage 1: Build Application
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy Maven POM and pre-fetch dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Package application JAR without running tests (tests run in CI/CD pipeline)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime Container
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add unprivileged user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy packaged JAR from build stage
COPY --from=build /app/target/cloudclean-backend-1.0.0-SNAPSHOT.jar app.jar

# Set ownership
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8000

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
