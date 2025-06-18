# Use Maven to build the application
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Use a smaller JRE image for running the application
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Set environment variables
ENV BOT_TOKEN=""

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 