# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 - build
# The pom is copied on its own first so that dependency resolution is cached
# and a source-only change does not re-download the world. Maven comes from the
# build image; the repo's ./mvnw wrapper is for developers and CI.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src/ src/
RUN mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 - split the fat jar into Spring Boot layers, so that dependencies and
# application code land in separate image layers.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /extract
COPY --from=build /build/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---------------------------------------------------------------------------
# Stage 3 - runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup --system --gid 1001 spring \
 && adduser --system --uid 1001 --ingroup spring spring

WORKDIR /app

# Least-changing layer first: third-party jars, then the application itself.
COPY --from=extract --chown=spring:spring /extract/extracted/dependencies/ ./
COPY --from=extract --chown=spring:spring /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=spring:spring /extract/extracted/application/ ./

USER spring
EXPOSE 8080

# Container-aware heap sizing; override JAVA_OPTS per environment.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
