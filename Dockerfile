FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
RUN mvn -B -ntp -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true -pl web -am package \
    && cp web/target/web-*.jar /src/app.jar

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S haypacomer && adduser -S haypacomer -G haypacomer
WORKDIR /app
COPY --from=build --chown=haypacomer:haypacomer /src/app.jar /app/app.jar
USER haypacomer
ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- "http://127.0.0.1:${PORT}/actuator/health/readiness" > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
