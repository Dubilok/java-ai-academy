# Sandbox runner image for Java AI Academy
#
# Pinned by digest — never use a floating tag in this file.
# To update: pull the new tag, get its digest with `docker inspect --format
# '{{index .RepoDigests 0}}'`, replace the sha256 below, rebuild, and update
# build.sh with the new sha256.
#
# openjdk:21-slim was retired upstream; eclipse-temurin:21-jdk-jammy is the
# maintained equivalent (Eclipse Temurin JDK 21 on Ubuntu 22.04).
FROM eclipse-temurin:21-jdk-jammy@sha256:9d8dcf999b0bce2453e913823595a5ff2a4e8e9e5d5241b45280d0ff069818ec

# JUnit Platform Console Standalone 1.10.3 — pinned by sha256
ARG JUNIT_VERSION=1.10.3
ARG JUNIT_SHA256=1455afad75d4eb4f44b493febc4a1bc32bc5f02eb25679361abd55e0f6421050
ARG JUNIT_JAR=junit-platform-console-standalone-${JUNIT_VERSION}.jar
ARG JUNIT_URL=https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/${JUNIT_VERSION}/${JUNIT_JAR}

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && curl -fsSL -o /opt/junit-platform-console-standalone.jar "${JUNIT_URL}" \
 && echo "${JUNIT_SHA256}  /opt/junit-platform-console-standalone.jar" | sha256sum -c \
 && apt-get purge -y --auto-remove curl \
 && rm -rf /var/lib/apt/lists/* \
 && chmod 444 /opt/junit-platform-console-standalone.jar

# Non-root user; uid/gid 1000 is what the runtime passes via --user=1000:1000.
RUN useradd --system --uid 1000 --gid 0 --no-create-home \
            --shell /usr/sbin/nologin runner

# Work directory is always provided as a bind-mount or tmpfs at runtime;
# it must be writable by uid 1000 so javac can write class files there.
RUN mkdir /work && chown 1000:0 /work && chmod 770 /work

WORKDIR /work
USER 1000:0

# No ENTRYPOINT or CMD — the runtime specifies the exact command.
