# ── stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Copy Gradle wrapper and dependency descriptors first (layer cache)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts 2>/dev/null || true
COPY gradle.properties gradle.properties 2>/dev/null || true

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# Copy source and build the fat jar
COPY src src
COPY config config
RUN ./gradlew bootJar --no-daemon -x test -x spotbugsMain -x spotbugsTest -x spotlessCheck \
    && mv build/libs/*.jar /workspace/app.jar

# ── stage 2: extract layers ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS extractor
WORKDIR /workspace
COPY --from=build /workspace/app.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── stage 3: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

# Run as non-root
RUN groupadd --gid 1001 academy && useradd --uid 1001 --gid academy --shell /bin/bash --create-home academy
USER academy

WORKDIR /app

# Copy Spring Boot layers in cache-friendly order (least → most volatile)
COPY --from=extractor /workspace/dependencies/ ./
COPY --from=extractor /workspace/spring-boot-loader/ ./
COPY --from=extractor /workspace/snapshot-dependencies/ ./
COPY --from=extractor /workspace/application/ ./

EXPOSE 8080

# Enable virtual threads and tune JVM for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseZGC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
