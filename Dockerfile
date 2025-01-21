# Use an official Java runtime as a parent image
FROM eclipse-temurin:19-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY target/JobNet-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port (update if not using 8080)
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]
