# Étape 1 : compiler le .jar avec Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=bind,from=maven-cache,source=.,target=/tmp/maven-repository,ro \
    --mount=type=cache,target=/root/.m2 \
    mkdir -p /root/.m2/repository && \
    cp -a /tmp/maven-repository/. /root/.m2/repository/ && \
    mvn -q -B package -DskipTests

# Étape 2 : l'exécuter sur une petite image Java
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
USER spring
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
