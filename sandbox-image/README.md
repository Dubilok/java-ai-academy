# Sandbox runner image

Built in Epic E3-T1. Base `openjdk:21-slim` plus the JUnit Platform Console Standalone jar so a
container can compile and run a single-file solution with its test class offline.

Hard requirements (`CLAUDE.md` §6.2): non-root user, no network, read-only rootfs, pinned by digest.
Never add a package to this image "just to debug" — rebuild it deliberately.
