# Use Maven to build the application
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Use a smaller JRE image for running the application
FROM openjdk:17-slim
WORKDIR /app

# Set timezone to GMT+3
ENV TZ=Asia/Baghdad
RUN apt-get update && \
    apt-get install -y tzdata && \
    ln -fs /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    dpkg-reconfigure -f noninteractive tzdata && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Set environment variables
ENV BOT_TOKEN=""

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 