# Build stage
FROM maven:3.9-eclipse-temurin-19 AS build
WORKDIR /workspace

# Copy pom first to maximize layer caching
COPY pom.xml ./

# Download dependencies before copying sources
RUN mvn -q -DskipTests dependency:go-offline

# Copy source and build application jar
COPY src src
RUN mvn -q -DskipTests clean package

# Runtime stage
FROM eclipse-temurin:19-jre-alpine
WORKDIR /app

# Copy fat jar produced by Spring Boot plugin
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
