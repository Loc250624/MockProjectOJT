# Multi-stage Dockerfile for Spring Boot LumiNa E-learning Application (Java 17)

# Stage 1: Build stage with JDK 17
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Production runtime stage with JRE 17
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/elearning-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
