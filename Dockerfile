# Image de l'application uniquement : Ollama tourne dans son propre conteneur (voir docker-compose.yml)

# Étape 1 : compilation du jar exécutable avec le wrapper Gradle
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar')" /app/app.jar

# Étape 2 : image d'exécution légère, sans outils de build
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
