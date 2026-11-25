# Stage 1: build the jar with Maven, reusing downloaded artifacts between builds.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=bind,from=maven-cache,source=.,target=/tmp/maven-repository,ro \
    --mount=type=cache,target=/root/.m2 \
    mkdir -p /root/.m2/repository && \
    cp -a /tmp/maven-repository/. /root/.m2/repository/ && \
    mvn -q -B package -DskipTests

# Stage 2: run it on a small JRE image, as a non-root user.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
USER spring
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
# Keep the JVM inside the container memory limit (Railway, Docker).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
ENTRYPOINT ["java", "-jar", "app.jar"]
