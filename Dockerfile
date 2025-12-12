# ---------- Stage 1: Build ----------
FROM maven:3.8.4-openjdk-17 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests


# ---------- Stage 2: Run ----------
FROM openjdk:17.0.1-jdk-slim

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/Pandal-Hopperv2-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
