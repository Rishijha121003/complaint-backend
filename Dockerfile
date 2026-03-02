FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
# Build command with extra flags
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Database connection parameters handled at runtime
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]