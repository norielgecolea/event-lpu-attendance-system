# Build the Spring Boot WAR, then run it on Tomcat.
# Context must be the repo root (see docker-compose.yml).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY event-attendance-system/pom.xml .
COPY event-attendance-system/src ./src
RUN mvn -q -DskipTests package \
  && test -f target/event-attendance-system.war

FROM tomcat:11.0.22-jdk21

# Optional video tooling (kept for parity with gate stack)
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Baked WAR — copied into the mounted webapps volume on every container start
COPY --from=build /build/target/event-attendance-system.war /opt/wars/event-attendance-system.war
COPY docker/tomcat/docker-entrypoint.sh /usr/local/bin/event-tomcat-entrypoint.sh
RUN chmod +x /usr/local/bin/event-tomcat-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/event-tomcat-entrypoint.sh"]
CMD ["catalina.sh", "run"]
