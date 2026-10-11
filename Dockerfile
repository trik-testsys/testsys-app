# syntax=docker/dockerfile:1

# Builds the executable jar of testsys-web:app; the Vaadin frontend is built in production mode by bootJar.
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace
COPY . .
# Gradle and Node.js downloaded by the Vaadin plugin are cached between builds.
# gradlew may be checked out with CRLF line endings on Windows.
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.vaadin \
    sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew :testsys-web:app:bootJar -Pdetekt.autoCorrect=false --no-daemon \
    && find testsys-web/app/build/libs -maxdepth 1 -name 'app-*.jar' ! -name '*-plain.jar' \
        -exec cp {} /workspace/testsys-app.jar \; \
    && test -f /workspace/testsys-app.jar

# Runs the jar without environment settings or secrets; Compose files supply them.
FROM eclipse-temurin:21-jre-noble
RUN groupadd --system --gid 10001 testsys \
    && useradd --system --uid 10001 --gid testsys --no-create-home --home-dir /nonexistent \
        --shell /usr/sbin/nologin testsys \
    && mkdir -p /app /var/lib/testsys/files \
    && chown testsys:testsys /var/lib/testsys/files
COPY --from=build /workspace/testsys-app.jar /app/app.jar
USER testsys
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
