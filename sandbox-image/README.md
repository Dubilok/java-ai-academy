# Sandbox runner image

Built in E3-T1. Base `eclipse-temurin:21-jdk-jammy` (Eclipse Temurin JDK 21 on Ubuntu 22.04,
pinned by digest) plus JUnit Platform Console Standalone 1.10.3 so a container can compile and run
a single-file solution with its test class completely offline.

> **Note:** `openjdk:21-slim` was retired upstream. `eclipse-temurin:21-jdk-jammy` is the
> maintained drop-in replacement.

## Build

```bash
./sandbox-image/build.sh
```

This builds the image, verifies `java -version` under all §6.2 hardening flags, confirms the JUnit
Console Standalone jar is present, and asserts no containers are leaked.

## Hard requirements (CLAUDE.md §6.2)

Every container spawned from this image must be started with **all** of:

| Flag | Value |
|---|---|
| `--network=none` | always |
| `--memory=128m --memory-swap=128m` | always |
| `--cpus=0.5` | always |
| `--pids-limit=64` | always |
| `--read-only` | always |
| `--tmpfs /work:size=32m,noexec` | always |
| `--cap-drop=ALL` | always |
| `--security-opt=no-new-privileges` | always |
| `--user=1000:0` | always |
| wall-clock timeout 5s, hard-killed | always |

Never add a package to this image "just to debug" — rebuild it deliberately and update
the pinned digest and sha256 values in `Dockerfile` and `build.sh`.

## Updating the base image

```bash
docker pull eclipse-temurin:21-jdk-jammy
docker inspect eclipse-temurin:21-jdk-jammy --format '{{index .RepoDigests 0}}'
# Replace the sha256 in Dockerfile FROM line, then rebuild and re-verify.
```
