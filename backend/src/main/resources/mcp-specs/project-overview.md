# Java AI Academy — Project Overview

An interactive Java learning platform with automated code grading and AI-powered curriculum generation.

## Learning loop
```
read lecture → open task → write code → submit
→ Docker sandbox runs JUnit 5 → verdict in <5s
→ pass: XP + crystals · fail: Socratic AI hint
```

## Technology stack
- Backend: Spring Boot 3.3, Java 21, PostgreSQL 16, Redis 7
- Sandbox: Docker (openjdk:21-slim) with strict resource limits
- AI: Anthropic Claude (content + interviews), Google Gemini (Socratic hints)
- Vector search: pgvector (Bedrock Titan Embeddings)

## Content structure
- Course → Module → Lecture → Task
- Each Task has: title, description, difficulty (EASY/MEDIUM/HARD), templateCode, testCode, solutionCode, xpReward
- testCode and solutionCode are never exposed to students

## Task requirements for Content Architect
When generating a task, produce:
1. `title` — concise imperative (e.g. "Reverse a string without using StringBuilder")
2. `description` — 2-4 sentences explaining what to implement and any constraints
3. `difficulty` — EASY (1-3 lines), MEDIUM (5-20 lines), HARD (complex algorithm)
4. `templateCode` — Java class with a `// TODO` stub the student fills in
5. `testCode` — JUnit 5 test class that compiles and passes against the reference solution
6. `solutionCode` — minimal correct implementation; must pass all tests in the sandbox
7. `xpReward` — 50 (EASY), 100 (MEDIUM), 200 (HARD)

## Sandbox constraints
All containers run with: --network=none, --memory=128m, --cpus=0.5, --pids-limit=64,
--read-only rootfs, --cap-drop=ALL, --security-opt=no-new-privileges, 5s wall-clock timeout.

## Security rules
- testCode and solutionCode must never appear in student-facing API responses
- Generated content is treated as untrusted until verified in the sandbox
- Prompt injection: student/AI content is always wrapped in delimited blocks
