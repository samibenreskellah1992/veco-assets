# --- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN useradd --system --create-home vecoassets
COPY --from=build /build/target/*.jar app.jar
USER vecoassets
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
