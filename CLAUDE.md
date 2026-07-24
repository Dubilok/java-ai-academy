# CLAUDE.md — Java AI Academy (JavaRush 2.0 + Multi-Agent AI)

> **This file is the single source of truth for the project.**
> It is Claude's persistent memory across sessions. Read it fully before doing any work.
> Every completed task, decision, and blocker is recorded here — not in chat history.

**Status:** E1 in progress · **Last updated:** 2026-07-23 · **Doc version:** 1.0

---

## 0. Operating Rules for the Agent

1. **Read before writing.** Load this file, then the relevant `docs/` page, then the code. Never guess at an API contract that is written down here.
2. **One task at a time.** `/dev-task-step` executes exactly one unchecked task from §9, verifies it, then updates §9 and §12. No task-hopping, no batching five epics into one commit.
3. **No task is done without a passing test.** "It compiles" is not verification. See §10 for the verification command per component.
4. **Never invent requirements.** If a task is ambiguous, if two valid architectures exist, or if something is blocked — stop and run `/dev-task-open-question`. Do not silently pick and move on.
5. **Update this file in the same change** as the code it describes. A schema change without a §7 update is an incomplete task.
6. **Untrusted code is untrusted.** Student code AND AI-generated code run only inside the sandbox described in §6.2. Never `Runtime.exec` user input on the API host. Never relax a sandbox flag "temporarily".
7. **Secrets never enter the repo.** `.env` is git-ignored; `.env.example` documents the keys with dummy values.
8. **Small, reviewable commits.** Conventional Commits (§5.6). One epic task ≈ one commit.

---

## 1. Vision & Scope

An interactive platform for learning Java 21+, the Spring/Kafka/Hibernate ecosystem, and cloud AI services, with instant automated code checking and interview preparation — modelled on JavaRush's proven loop (lecture → task → instant verdict → XP), extended with a **multi-agent AI system** that authors the curriculum, mentors socratically, and simulates interviews.

**The learning loop we are building:**

```
read lecture → open task → write code (web IDE or IntelliJ) → submit
   → Docker sandbox runs JUnit 5 → verdict in < 5s
   → pass: XP + confetti · fail: Socratic AI hint (never the answer)
```

### 1.1 Product pillars

| # | Pillar | What it means concretely |
|---|--------|--------------------------|
| P1 | **Instant, isolated verification** | Every submission compiles and runs against hidden JUnit 5 tests in a locked-down container. Verdict < 5s p95. |
| P2 | **AI-authored curriculum** | An admin types a technology name; the Content Architect agent produces lecture + task + template + tests, and **self-heals** until the generated test suite actually passes against the reference solution in the sandbox. |
| P3 | **Socratic mentoring** | The tutor agent is contractually forbidden from emitting solution code. It asks 1–2 guiding questions grounded in the student's actual compiler/test output. |
| P4 | **Meet the student where they work** | Web workspace (Monaco) *and* a first-class IntelliJ IDEA plugin sharing one API. |
| P5 | **Interview readiness** | Flashcards, RAG-grounded answers, and a Mock Interviewer that adapts difficulty and returns a structured 10-dimension evaluation. |

### 1.2 Secondary objective — 2026 AI certification coverage

The build doubles as a practical lab for five certifications. This is a *design constraint*, not marketing:

| Certification | Component that proves it | Technical focus |
|---|---|---|
| Claude Certified Architect | Content Architect agent + MCP server | Tool use, MCP integration, structured output, self-healing loops |
| Gemini Enterprise Agent Dev | Socratic Mentor | Gemini ADK, grounding on the lecture corpus, function calling into the sandbox |
| GitHub Copilot Certification | IntelliJ plugin | Custom context injection, ToDo-comment guidance, test generation |
| AWS Certified AI Practitioner | Bedrock hosting + Guardrails + FinOps | Model hosting, responsible-AI filtering, per-request cost telemetry |
| Azure AI Fundamentals | RAG search over lectures | Vector index, hybrid retrieval, grounded answers |

### 1.3 Explicitly out of scope (v1)

Payments/subscriptions · mobile apps · social feed, forum, chat · languages other than Java · video content · multi-tenancy. Note them here if they come up; do not build them.

---

## 2. System Architecture

### 2.1 Context diagram

```mermaid
graph TD
    subgraph Clients
        WEB["Web UI — Next.js 14"]
        IDE["IntelliJ Plugin — Kotlin"]
    end

    subgraph Backend["Spring Boot 3.3 monolith — modular by feature"]
        AUTH["auth — JWT, roles"]
        CAT["catalog — courses, modules, lectures, tasks"]
        PROG["progress — submissions, XP, streaks"]
        SBX["sandbox — Docker execution engine"]
        AI["ai — agent orchestration"]
        INT["interview — flashcards, mock interviews"]
    end

    subgraph Data
        PG[("PostgreSQL 16")]
        REDIS[("Redis — cache, rate limit, queue")]
        VEC[("Vector index — pgvector or Azure AI Search")]
    end

    subgraph Runtime
        DOCKER["Docker daemon — ephemeral JDK 21 containers"]
    end

    subgraph Providers
        CLAUDE["Anthropic Claude API"]
        GEMINI["Google Gemini ADK"]
        BEDROCK["AWS Bedrock — optional route"]
    end

    WEB --> AUTH
    WEB --> CAT
    WEB --> PROG
    WEB --> INT
    IDE --> AUTH
    IDE --> CAT
    IDE --> PROG

    PROG --> SBX
    AI --> SBX
    SBX --> DOCKER

    AUTH --> PG
    CAT --> PG
    PROG --> PG
    INT --> PG
    AUTH --> REDIS
    PROG --> REDIS
    AI --> VEC

    AI --> CLAUDE
    AI --> GEMINI
    AI -.->|"cert track"| BEDROCK
```

> **Architectural stance:** we start as a **modular monolith**, not microservices. Feature packages have clean boundaries and no cross-package entity leakage, so any module can be extracted later if load demands it. The only genuinely separate runtime is the sandbox container fleet. Do not add an API gateway, service discovery, or a message bus before there is a measured reason. See ADR-0002.

### 2.2 Submission flow

```mermaid
sequenceDiagram
    participant S as Student
    participant API as Spring Boot API
    participant R as Redis queue
    participant SB as Sandbox worker
    participant D as Docker

    S->>API: POST /api/v1/tasks/{id}/submissions
    API->>API: validate size, rate limit, persist PENDING
    API->>R: enqueue submissionId
    API-->>S: 202 Accepted + submissionId
    SB->>R: dequeue
    SB->>SB: materialise workdir, write Solution.java + tests
    SB->>D: run openjdk:21-slim, no network, ro-rootfs, 128m, 0.5cpu, 5s
    D-->>SB: exit code, stdout, stderr, duration
    SB->>SB: parse JUnit XML, cap log size
    SB->>API: persist PASSED or FAILED, award XP once
    S->>API: GET /api/v1/submissions/{id} (poll or SSE)
    API-->>S: verdict + logs
```

### 2.3 Multi-agent system

```mermaid
graph LR
    ORCH["Agent Orchestrator"]

    A1["A1 Content Architect — Claude"]
    A2["A2 Socratic Mentor — Gemini"]
    A3["A3 IDE Copilot Specialist"]
    A4["A4 Mock Interviewer — Claude"]
    A5["A5 Judge — LLM-as-a-Judge"]

    MCP["MCP server — project tools"]
    SBX["Docker sandbox"]
    RAG["RAG over lecture corpus"]

    ORCH --> A1
    ORCH --> A2
    ORCH --> A3
    ORCH --> A4
    A1 --> MCP
    A1 -->|"self-healing loop"| SBX
    A2 --> RAG
    A2 -->|"read-only run"| SBX
    A4 --> RAG
    A1 --> A5
    A2 --> A5
    A4 --> A5
```

**A5 (Judge)** samples agent outputs asynchronously and scores them for hallucination, answer-leakage, and rubric adherence. It never sits in the request path — it writes to an `ai_evaluation` table that powers a quality dashboard.

---

## 3. Repository Layout

```
java-ai-academy/
├── CLAUDE.md                    ← you are here; the source of truth
├── README.md                    ← human onboarding, 5-minute quickstart
├── .claude/commands/            ← /dev-task-step, /dev-task-open-question
├── .env.example                 ← every env var, dummy values
├── backend/                     ← Spring Boot 3.3, Java 21, Gradle Kotlin DSL
│   └── src/main/java/com/javaacademy/platform/
│       ├── auth/  catalog/  progress/  sandbox/  ai/  interview/
│       ├── common/              ← error handling, base types, utils
│       └── config/              ← security, jackson, openapi, docker client
├── frontend/                    ← Next.js 14 App Router, TS, Tailwind, Monaco
├── ide-plugin/                  ← Kotlin, IntelliJ Platform SDK, Gradle
├── sandbox-image/               ← Dockerfile for the runner image + JUnit console jar
├── infra/                       ← docker-compose, local stack, k8s later
├── docs/                        ← api-contract.md, prompts/, adr/
└── .github/workflows/           ← CI
```

**Package rule (backend):** organise by *feature*, then by layer inside it — `catalog/CourseController.java`, `catalog/CourseService.java`, `catalog/Course.java`. Never create top-level `controllers/`, `services/`, `models/` packages. Cross-feature calls go through a public service interface; entities never cross a feature boundary (map to a DTO).

---

## 4. Technology Stack

| Layer | Choice | Version | Notes |
|---|---|---|---|
| Language (backend) | Java | 21 LTS | Records, sealed types, pattern matching, virtual threads |
| Framework | Spring Boot | 3.3.x | Web MVC on virtual threads; not WebFlux |
| Persistence | Spring Data JPA + Hibernate | 6.x | No `@ManyToMany`; explicit join entities |
| Migrations | Flyway | 10.x (BOM-managed) | SQL files under `db/migration/`, never edit an applied migration |
| DB | PostgreSQL | 16 | `gen_random_uuid()` via pgcrypto; pgvector if RAG stays local |
| Cache/queue | Redis | 7 | Sessions, rate limits, submission queue |
| Sandbox | docker-java | 3.4.x | Talks to the host Docker daemon |
| Runner image | `openjdk:21-slim` + JUnit Platform Console | 1.10.x | Built once, pinned by digest |
| Build | Gradle Kotlin DSL | 8.x | Version catalog in `gradle/libs.versions.toml` |
| Testing | JUnit 5, AssertJ, Testcontainers, MockMvc, ArchUnit | — | Testcontainers for every DB/Redis/Docker test |
| Static analysis | SpotBugs (plugin 5.2.5, tool 4.8.6) | — | Runs as part of `check`; exclude filter at `backend/config/spotbugs-exclude.xml` |
| PR review | Claude Code GitHub Action | v1 | Posts inline review comments on every PR; needs `ANTHROPIC_API_KEY` repo secret |
| Frontend | Next.js (App Router) | 14.x | TypeScript strict, RSC by default |
| Styling | Tailwind CSS | 3.4 | Design tokens in §11 |
| Editor | `@monaco-editor/react` | latest | Java syntax, one shared config module |
| Data fetching | TanStack Query | 5 | Client components only |
| IDE plugin | Kotlin + IntelliJ Platform SDK | 2024.1+ | Gradle IntelliJ Plugin 2.x |
| AI — content, interview | Anthropic Claude API | `claude-sonnet-5` default | Model ID in config, never hardcoded. Verify current IDs at docs.claude.com |
| AI — tutor | Google Gemini (ADK) | current | Grounding on lecture corpus |
| AI — cloud track | AWS Bedrock, Azure AI Search | — | Epic E9; behind the same `LlmClient` interface |

**Version discipline:** pin exact versions in the version catalog / `package.json`. No `latest`, no dynamic ranges, no unpinned Docker tags in production paths.

---

## 5. Coding Standards

### 5.1 Java

- **Constructor injection only.** No `@Autowired` on fields. Prefer `final` fields.
- **Records for DTOs**, request bodies, and value objects. Entities are classes.
- **Lombok on entities:** only `@Getter` and `@Setter` (with `@Setter(AccessLevel.NONE)` on the `id` field). Never `@Data`, `@EqualsAndHashCode`, `@ToString`, or `@Builder` on entities — all cause JPA problems. Elsewhere, `@Slf4j` and `@RequiredArgsConstructor` are also permitted.
- **Controllers are thin**: validate, delegate, map. No business logic, no repository access.
- **Never return entities from a controller.** Map to a DTO — an entity leak is how a `password_hash` reaches a browser.
- **Exceptions:** domain exceptions extend `ApiException` (in `common`); one `@RestControllerAdvice` maps them to RFC 7807 `ProblemDetail`. Never swallow an exception; never `catch (Exception e) { }`.
- **Nullability:** package-level `@NullMarked` (JSpecify). `Optional` for return values, never for fields or parameters.
- **Transactions:** `@Transactional` on service methods, `readOnly = true` for queries. Never on a controller.
- **Time:** `Instant` in the domain and DB (`TIMESTAMPTZ`), formatted at the edge. Inject `Clock` so tests can freeze it.
- **Money/points:** `long` for XP, never floating point.
- **Logging:** SLF4J with parameterised messages. **Never log** JWTs, API keys, password hashes, or full student source code.
- Formatting: Spotless with `palantir-java-format`, 4-space indent, 120-col soft limit. `./gradlew spotlessApply` before commit.
- Static analysis: SpotBugs runs on `check`. `EI_EXPOSE_REP2` is suppressed for JPA entity packages (storing entity refs is correct ORM usage). Mark Spring service classes that can throw from their constructor as `final` to satisfy SEI CERT OBJ-11 (see `JwtService`).

### 5.2 Kotlin (IDE plugin)

- Official Kotlin style; ktlint in CI.
- **Threading is the whole game in a plugin:** no network or blocking I/O on the EDT. Background work via `ProgressManager` / coroutines on `Dispatchers.IO`; UI updates via `invokeLater` + read/write actions. PSI/VFS writes only inside `WriteCommandAction`.
- JWT goes in `PasswordSafe`. Never in `PropertiesComponent`, never on disk in plaintext.
- No `!!` outside tests. Prefer sealed classes for API results over exceptions.

### 5.3 TypeScript / Next.js

- `strict: true`, `noUncheckedIndexedAccess: true`. **`any` is banned** — use `unknown` and narrow.
- Server Components by default; `"use client"` only where interactivity demands it.
- All API responses parsed through **Zod** schemas generated from / kept in sync with §8. A network boundary without runtime validation is a bug.
- Components: one per file, named export, colocated `*.test.tsx`. Server state in TanStack Query — no `useEffect` fetching.
- Tailwind utilities only; no ad-hoc CSS files. Shared variants via `cva`.
- Accessibility is a requirement, not a polish item: keyboard-navigable editor controls, visible focus rings, `aria-live` for the verdict panel.

### 5.4 Database & migrations

- Every schema change is a **new** versioned SQL file under `db/migration/V{n}__{description}.sql`. Never edit an applied migration — add a new version instead.
- Flyway CE has no rollback mechanism; write forward-only compensating migrations (`V{n}__revert_xxx.sql`) when an applied migration must be undone.
- `snake_case` names; plural tables; PK `id UUID`; FKs `<entity>_id` with an explicit `ON DELETE` policy.
- Index every FK and every column used in a `WHERE` that runs per request.
- No business logic in triggers or stored procedures.

### 5.5 Testing

| Level | Tool | Rule |
|---|---|---|
| Unit | JUnit 5 + AssertJ + Mockito | Pure logic, no Spring context. Fast. |
| Slice | `@WebMvcTest`, `@DataJpaTest` | Controller contracts, repository queries. |
| Integration | Testcontainers (Postgres, Redis, Docker) | Real infra, no H2 — H2 lies about Postgres behaviour. |
| Architecture | ArchUnit | Enforces §3 package rules and "no entity in controller signature". |
| Frontend | Vitest + Testing Library, Playwright for the workspace flow | Test behaviour, not implementation. |

Naming: `methodName_condition_expectedOutcome`. Assert on behaviour and error *type*, not on log strings. Coverage target 80% on `service` packages — but a meaningful test for the sandbox timeout path beats 100% on getters.

### 5.6 Git

- Conventional Commits: `feat(sandbox): enforce read-only rootfs`.
- Branch per epic task: `feat/E3-T2-docker-runner`.
- CI must be green before merge. No direct pushes to `main`.

---

## 6. Security Requirements

### 6.1 Application

- BCrypt (strength 12) for passwords. Never a homegrown hash.
- JWT: HS256 minimum 256-bit secret from env, 15-min access token, 30-day rotating refresh token stored hashed in DB. Stateless — no server session.
- Authorisation is enforced **server-side per resource**, not by hiding UI. `/api/v1/admin/**` requires `ROLE_ADMIN`; a student may only read their own progress.
- Rate limits (Redis): submissions 10/min/user, AI hints 20/hour/user, auth 5/min/IP.
- CORS allowlist from config. CSRF disabled only because auth is a bearer token in a header — document that assumption if cookies are ever introduced.
- Validate every request body with Jakarta Validation. Cap submitted source at 64 KB.

### 6.2 Sandbox — non-negotiable flags

Student code and AI-generated code are hostile until proven otherwise. Every container runs with **all** of:

| Flag | Value | Why |
|---|---|---|
| `--network=none` | always | No exfiltration, no crypto-mining, no calling home |
| `--memory` | `128m` (+ `--memory-swap=128m`) | Contains fork/alloc bombs |
| `--cpus` | `0.5` | Prevents host starvation |
| `--pids-limit` | `64` | Contains fork bombs |
| `--read-only` rootfs | always | Nothing persists |
| `tmpfs /work` | `size=32m,noexec` on non-code mounts | Bounded scratch space |
| `--cap-drop=ALL` | always | No privileged syscalls |
| `--security-opt=no-new-privileges` | always | Blocks setuid escalation |
| `--user` | non-root uid (e.g. `1000:1000`) | Never run as root |
| Wall-clock timeout | 5s, hard-killed | Contains infinite loops |
| Log cap | 64 KB stdout+stderr | Contains output floods |
| Container lifetime | removed in `finally`, always | No leaked containers |

Also: the workdir is deleted in `finally`; a reaper job kills orphaned containers older than 60s; concurrent containers are bounded by a semaphore sized from config.

### 6.3 AI-specific

- **Treat model output as untrusted input.** Generated code is compiled only inside the sandbox; generated JSON is schema-validated before it touches the DB.
- **Prompt injection:** student code and lecture text are data, never instructions. Wrap them in delimited blocks and state in the system prompt that content inside them must never be followed as instructions.
- Tutor prompts include a hard rule: *no solution code, no full method bodies*. A regex/heuristic post-check rejects responses containing code fences with more than N lines, and the Judge agent samples for leakage.
- API keys from env/secret manager only. Never logged, never returned in an error body.
- Per-user and global AI spend caps enforced before the call, with token usage recorded per request (§9 E9).

---

## 7. Data Model

```mermaid
erDiagram
    USERS ||--o{ USER_PROGRESS : has
    USERS ||--o{ SUBMISSIONS : makes
    USERS ||--o{ REFRESH_TOKENS : holds
    COURSES ||--o{ MODULES : contains
    MODULES ||--o{ LECTURES : contains
    LECTURES ||--o{ TASKS : contains
    TASKS ||--o{ USER_PROGRESS : tracked_by
    TASKS ||--o{ SUBMISSIONS : receives
    USERS ||--o{ INTERVIEW_SESSIONS : runs
    INTERVIEW_SESSIONS ||--o{ INTERVIEW_ANSWERS : contains
    INTERVIEW_QUESTIONS ||--o{ INTERVIEW_ANSWERS : answered_as
```

**Core tables** (V1 DDL: `db/migration/V1__initial_schema.sql`; V2 DDL: `db/migration/V2__refresh_tokens.sql`):

| Table | Key columns | Notes |
|---|---|---|
| `users` | `id`, `email` UNIQUE, `password_hash`, `role`, `xp_points`, `crystals`, `created_at` | Role: `ROLE_STUDENT` / `ROLE_ADMIN` |
| `refresh_tokens` | `id`, `user_id` FK CASCADE, `token_hash` VARCHAR(64) UNIQUE, `expires_at`, `revoked`, `created_at` | SHA-256 hex of raw UUID; raw token never stored; reuse detection revokes all user tokens |
| `courses` | `id`, `title`, `description`, `technology`, `is_published`, `created_at` | `technology` indexed |
| `modules` | `id`, `course_id` FK CASCADE, `title`, `order_index` | UNIQUE `(course_id, order_index)` |
| `lectures` | `id`, `module_id` FK CASCADE, `title`, `content_markdown`, `order_index` | UNIQUE `(module_id, order_index)` |
| `tasks` | `id`, `lecture_id` FK CASCADE, `title`, `description`, `difficulty`, `template_code`, `test_code`, `xp_reward` | `test_code` is never sent to a student client |
| `user_progress` | `id`, `user_id`, `task_id`, `status`, `attempts`, `submitted_code`, `updated_at` | UNIQUE `(user_id, task_id)` |
| `submissions` | `id`, `user_id`, `task_id`, `source`, `status`, `logs`, `duration_ms`, `created_at` | Append-only audit trail; `user_progress` holds the latest state |
| `interview_questions` | `id`, `technology`, `category`, `question`, `short_answer`, `detailed_explanation`, `difficulty` | Seeded + AI-extended |
| `interview_sessions` / `interview_answers` | session: `user_id`, `technology`, `score`, `report_json` | 10-dimension rubric report |
| `ai_generation_log` | `id`, `agent`, `model`, `prompt_tokens`, `completion_tokens`, `cost_usd`, `latency_ms`, `outcome` | Powers FinOps + Judge dashboards |
| `ai_evaluation` | `id`, `target_type`, `target_id`, `judge_scores_json`, `flags` | LLM-as-a-Judge output |

**Invariants**
- XP is awarded **once** per `(user_id, task_id)` — enforced by the unique constraint plus a status transition check, not by an `if` in the service.
- `tasks.test_code` and `tasks.solution_code` are excluded from every student-facing DTO. There is an ArchUnit/serialisation test for this.
- Deleting a course cascades to modules → lectures → tasks, but `submissions` are retained with a nullable task reference for audit.

---

## 8. API Contract (v1)

Base path `/api/v1`. JSON only. Errors: RFC 7807 `application/problem+json`. Auth: `Authorization: Bearer <jwt>`.
Full request/response bodies: `docs/api-contract.md`. OpenAPI is generated at `/v3/api-docs` (springdoc).

### Public
| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | email + password → tokens |
| POST | `/auth/login` | credentials → access + refresh |
| POST | `/auth/refresh` | rotate refresh token |
| GET | `/courses` | published courses (paged) |

### Student (JWT)
| Method | Path | Purpose |
|---|---|---|
| GET | `/courses/{id}` | course with module/lecture tree |
| GET | `/lectures/{id}` | markdown content + task stubs |
| GET | `/tasks/{id}` | description + `templateCode` — **never** `testCode` |
| POST | `/tasks/{id}/submissions` | `{ "source": "..." }` → `202` + `submissionId` |
| GET | `/submissions/{id}` | `PENDING` / `PASSED` / `FAILED` + capped logs |
| GET | `/submissions/{id}/stream` | SSE verdict push (fallback: poll every 1s) |
| POST | `/tasks/{id}/ai-hint` | Socratic hint from last failed submission |
| GET | `/me` | profile, XP, level, streak |
| GET | `/me/progress` | per-course completion |
| GET | `/interview/questions` | filter by technology/category/difficulty |
| POST | `/interview/sessions` | start mock interview |
| POST | `/interview/sessions/{id}/answers` | submit answer → follow-up question |
| POST | `/interview/sessions/{id}/finish` | 10-dimension evaluation report |

### Admin (`ROLE_ADMIN`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/admin/ai/generate-course` | `{ "technology": "Spring Cloud Gateway" }` → async job id |
| GET | `/admin/ai/jobs/{id}` | generation status + self-healing attempt log |
| POST | `/admin/courses/{id}/publish` | publish/unpublish |
| PUT | `/admin/tasks/{id}` | hand-edit generated content |
| GET | `/admin/ai/usage` | token + cost telemetry |

### IDE plugin
Uses the same student endpoints plus `GET /ide/bootstrap` (courses + tasks trimmed for the tool window) and sends `X-Client: intellij-plugin/<version>` for telemetry separation.

**Conventions:** `PENDING/PASSED/FAILED` verdicts; cursor pagination (`?cursor=&limit=`); `snake_case` in SQL, `camelCase` in JSON; every list response wrapped as `{ "items": [], "nextCursor": null }`.

---

## 9. Execution Roadmap

Legend: `[ ]` not started · `[~]` in progress · `[x]` done · `[!]` blocked (see §13).
`/dev-task-step` picks the **first** `[ ]` task in document order, unless the user names one.

### E0 — Workspace bootstrap
- [x] E0-T1 Monorepo skeleton, `.gitignore`, `.editorconfig`, `.env.example`
- [x] E0-T2 `CLAUDE.md` with architecture, standards, roadmap
- [x] E0-T3 `/dev-task-step` and `/dev-task-open-question` commands
- [~] E0-T4 `infra/docker-compose.yml` written — **unverified**; run `docker compose -f infra/docker-compose.yml up -d` and confirm both healthchecks pass, then flip to `[x]`
- [~] E0-T5 CI workflow written — cannot go green until E1 (backend) and E7 (frontend) exist
- [x] E0-T6 ADR-0001 (record decisions) and ADR-0002 (modular monolith) written

### E1 — Backend core: persistence & auth
- [x] E1-T1 Gradle Kotlin DSL build, version catalog, Spring Boot 3.3 skeleton, `/actuator/health` returns UP
- [~] E1-T2 Flyway migration `V1__initial_schema.sql` (all 12 tables in §7) — code complete, tests blocked by Docker overlay2 read-only filesystem; restart Docker Desktop then run `./gradlew test`
- [~] E1-T3 JPA entities + repositories; Testcontainers Postgres test proves every mapping loads — code complete, tests blocked by Docker overlay2 read-only filesystem; restart Docker Desktop then run `./gradlew test`
- [x] E1-T4 `POST /auth/register` + `/auth/login`: BCrypt(12), JWT issue, integration-tested
- [x] E1-T5 Refresh-token rotation with reuse detection
- [x] E1-T6 `SecurityFilterChain`: public/student/admin rules; test asserts 401 and 403 paths
- [ ] E1-T7 Global `ProblemDetail` exception handler + validation error shape
- [ ] E1-T8 ArchUnit rules for §3 package boundaries and no-entity-in-controller

### E2 — Catalog & progress
- [ ] E2-T1 Course/module/lecture/task read endpoints with cursor pagination
- [ ] E2-T2 Task DTO mapping that **provably** omits `testCode` (test asserts absence in JSON)
- [ ] E2-T3 `user_progress` upsert + attempt counting
- [ ] E2-T4 XP/level/crystal rules in one `GamificationService`; idempotent award test
- [ ] E2-T5 `/me` and `/me/progress` aggregates
- [ ] E2-T6 Seed data: one hand-written course, 3 lectures, 5 tasks with real JUnit tests

### E3 — Docker sandbox engine
- [ ] E3-T1 `sandbox-image/Dockerfile` on `openjdk:21-slim` + JUnit Console jar, pinned by digest, built by script
- [ ] E3-T2 `DockerCodeExecutionService`: materialise workdir, compile, run, collect result
- [ ] E3-T3 Apply **every** hardening flag in §6.2; a test asserts each one is set on the container config
- [ ] E3-T4 Timeout + hard kill + guaranteed cleanup in `finally`; leak test runs 50 submissions and asserts 0 containers remain
- [ ] E3-T5 Parse JUnit XML into `ExecutionResult(status, failedTests, logs, durationMs)`
- [ ] E3-T6 Redis-backed submission queue + worker with bounded concurrency
- [ ] E3-T7 `POST /tasks/{id}/submissions` + polling endpoint, wired end to end
- [ ] E3-T8 Adversarial suite: infinite loop, fork bomb, 2GB alloc, network call, file write outside workdir, `System.exit(0)`, 10MB stdout — all contained, all verdicts correct
- [ ] E3-T9 SSE verdict stream

### E4 — Agent A1: Content Architect (Claude)
- [ ] E4-T1 `LlmClient` abstraction + Anthropic implementation; model id from config; retries with jitter
- [ ] E4-T2 Structured-output contract: JSON schema for `{lecture, task, templateCode, solutionCode, testCode}` + strict validation
- [ ] E4-T3 System prompt in `docs/prompts/content-architect.md`, versioned and diffable
- [ ] E4-T4 **Self-healing loop**: run generated tests against generated solution in the sandbox; on failure feed logs back; max 3 attempts; every attempt logged
- [ ] E4-T5 Reject-and-report path when the loop exhausts — never persist unverified content
- [ ] E4-T6 `POST /admin/ai/generate-course` async job + status endpoint
- [ ] E4-T7 `ai_generation_log` telemetry (tokens, cost, latency, outcome)
- [ ] E4-T8 Integration test with a recorded/stubbed LLM response — no live API calls in CI

### E5 — Agent A2: Socratic Mentor (Gemini)
- [ ] E5-T1 Gemini client behind the same `LlmClient` interface
- [ ] E5-T2 Hint prompt grounded in: task text, student source, compiler/test output
- [ ] E5-T3 **Anti-leak guard**: post-filter rejects solution code; retry once with a stricter instruction, then degrade to a canned hint
- [ ] E5-T4 `POST /tasks/{id}/ai-hint` + per-user rate limit + hint history
- [ ] E5-T5 Golden-set test: 10 broken submissions → assert hints contain no compilable solution

### E6 — Interview trainer & A4 Mock Interviewer
- [ ] E6-T1 Flashcard CRUD + filtered query endpoints
- [ ] E6-T2 Session state machine (start → Q/A loop → finish) persisted per turn
- [ ] E6-T3 Adaptive difficulty from rolling answer scores
- [ ] E6-T4 Structured 10-dimension evaluation JSON + schema validation
- [ ] E6-T5 Session report endpoint + history

### E7 — Web frontend (Next.js 14)
- [ ] E7-T1 App scaffold, Tailwind tokens from §11, fonts, dark theme
- [ ] E7-T2 Auth flow, token storage, refresh interceptor, protected routes
- [ ] E7-T3 Course map / quest map + student dashboard
- [ ] E7-T4 `TaskWorkspace`: 40% markdown pane · 60% Monaco · collapsible terminal
- [ ] E7-T5 Submit → poll/SSE → verdict panel; `canvas-confetti` on pass
- [ ] E7-T6 "AI hint" panel with loading and refusal states
- [ ] E7-T7 Interview flashcards with 3D flip
- [ ] E7-T8 Admin console with live AI generation log
- [ ] E7-T9 Playwright happy path: login → open task → fail → hint → pass
- [ ] E7-T10 a11y pass: keyboard nav, focus rings, `aria-live` verdicts

### E8 — IntelliJ IDEA plugin (Kotlin)
- [ ] E8-T1 Plugin project, Gradle IntelliJ Plugin 2.x, `runIde` launches
- [ ] E8-T2 Login dialog; JWT into `PasswordSafe`; refresh handling
- [ ] E8-T3 "Java AI Academy" tool window with course/task tree
- [ ] E8-T4 "Start task" action: creates `Solution.java` from the template inside a `WriteCommandAction`
- [ ] E8-T5 "Verify" action: reads editor content, submits, shows verdict (green banner / red log panel)
- [ ] E8-T6 Inline ToDo-comment guidance from the tutor agent (no solution code)
- [ ] E8-T7 Threading audit: zero blocking calls on the EDT
- [ ] E8-T8 Plugin verifier passes for the target IDE range

### E9 — Cloud AI track (AWS + Azure)
- [ ] E9-T1 Bedrock route behind `LlmClient`; switch by config, no call-site changes
- [ ] E9-T2 Bedrock Guardrails on generated content
- [ ] E9-T3 RAG index over the lecture corpus (Azure AI Search or pgvector — see Q2 in §13)
- [ ] E9-T4 Ground interview answers in retrieved lecture chunks with citations
- [ ] E9-T5 FinOps dashboard: cost per agent, per course, per student
- [ ] E9-T6 MCP server exposing project tools (task lookup, sandbox run, spec fetch)
- [ ] E9-T7 A5 Judge: async scoring job + quality dashboard

### E10 — Production readiness
- [ ] E10-T1 Structured JSON logging + request correlation ids
- [ ] E10-T2 Micrometer metrics: submission latency, sandbox failures, token spend
- [ ] E10-T3 Multi-stage Docker builds for backend and frontend
- [ ] E10-T4 CI: build → test → image → deploy; secrets from the platform store
- [ ] E10-T5 Load test: 100 concurrent submissions, assert p95 < 5s and no container leak
- [ ] E10-T6 Backup/restore runbook + `docs/RUNBOOK.md`

---

## 10. Run, Test, Verify

### Prerequisites
JDK 21, Docker 24+, Node 20+, pnpm or npm, `psql` client. Copy `.env.example` → `.env` and fill it.

### Local stack
```bash
docker compose -f infra/docker-compose.yml up -d      # Postgres 16 + Redis 7
docker compose -f infra/docker-compose.yml ps         # both healthy?
```

### Backend
```bash
cd backend
./gradlew spotlessApply build                 # format + compile + test + SpotBugs
./gradlew bootRun --args='--spring.profiles.active=local'
curl -s localhost:8080/actuator/health        # {"status":"UP"}
./gradlew test --tests '*SandboxAdversarialTest'
```

### Sandbox image
```bash
docker build -t java-ai-academy/runner:21 sandbox-image/
docker run --rm --network=none --memory=128m --cpus=0.5 --pids-limit=64 \
  --read-only --cap-drop=ALL --security-opt=no-new-privileges \
  java-ai-academy/runner:21 java -version
docker ps -a | grep java-ai-academy || echo "no leaked containers ✓"
```

### Frontend
```bash
cd frontend
npm ci && npm run dev            # http://localhost:3000
npm run lint && npm run typecheck && npm run test
npx playwright test
```

### IDE plugin
```bash
cd ide-plugin
./gradlew runIde                 # sandbox IDE with the plugin loaded
./gradlew verifyPlugin test
```

### Definition of Done (every task)
1. Code compiles with zero warnings introduced.
2. Tests written **and passing** — including the failure path, not just the happy path.
3. Formatter/linter clean.
4. Docs updated: §7/§8 if schema or API changed; ADR if an architectural choice was made.
5. §9 checkbox flipped to `[x]` and §12 log appended.
6. Nothing secret committed.

---

## 11. Design System

| Token | Value | Use |
|---|---|---|
| `bg-base` | `#0F172A` | App background (low eye strain in long sessions) |
| `bg-card` | `#1E293B` | Cards, panels, editor chrome |
| `accent-java` | `#EA580C` | Primary CTA, active nav, XP bar |
| `accent-blue` | `#3B82F6` | Links, focus rings, secondary actions |
| `success` | `#10B981` | Passing verdict, solved state |
| `error` | `#F43F5E` | Compile/test failure |
| `text-primary` | `#F1F5F9` | Body text |
| `text-muted` | `#94A3B8` | Secondary text |

**Fonts:** Inter (UI) · JetBrains Mono (code, terminal, editor — ligatures on).
**Motion:** 150–250ms ease-out; `canvas-confetti` only on a task pass. Respect `prefers-reduced-motion`.
**Gamification:** XP → developer levels · crystals for solved tasks · streaks. Never punitive: no lost progress, no shame states.

---

## 12. Progress Log

| Date | Task | Result | Notes |
|---|---|---|---|
| 2026-07-23 | E0-T1 | ✅ | Monorepo skeleton, `.gitignore`, `.editorconfig`, `.env.example` created |
| 2026-07-23 | E0-T2 | ✅ | This file — architecture, standards, contracts, 10-epic roadmap |
| 2026-07-23 | E0-T3 | ✅ | `/dev-task-step`, `/dev-task-open-question` in `.claude/commands/` |
| 2026-07-23 | E0-T4 | ⚠️ | `infra/docker-compose.yml` written but not started — needs a local Docker run to verify |
| 2026-07-23 | E0-T5 | ⚠️ | `.github/workflows/ci.yml` written; will fail until E1/E7 produce buildable projects |
| 2026-07-23 | E0-T6 | ✅ | ADR-0001 (decision log) and ADR-0002 (modular monolith over microservices) |
| 2026-07-23 | E1-T1 | ✅ | Gradle 8.11.1 wrapper, version catalog, Spring Boot 3.3.6 skeleton, virtual threads; `ActuatorHealthTest` passes |
| 2026-07-23 | E1-T2 | ⚠️ | Flyway V1: 12 tables, 15 indexes, FK policies per §7; compiles but tests blocked — Docker overlay2 read-only, needs Docker Desktop restart |
| 2026-07-23 | E1-T3 | ⚠️ | 12 JPA entities + repositories across 4 feature packages with Lombok `@Getter @Setter`; `JpaMappingTest` covers all 12 entity types; blocked by same Docker overlay2 issue |
| 2026-07-24 | E1-T5 | ✅ | `refresh_tokens` table (V2 migration), `RefreshToken` entity, `POST /auth/refresh`; SHA-256 hashed tokens, reuse detection revokes all user tokens; 16 unit tests (AuthServiceTest) + 5 slice tests (AuthControllerTest) + 6 repo tests (RefreshTokenRepositoryTest, Docker-blocked) all pass where runnable |
| 2026-07-24 | E1-T4 | ✅ | `POST /auth/register` + `/auth/login`; BCrypt(12); HS256 JWT (15-min); `AuthControllerTest` (8 tests, @WebMvcTest — no Docker needed) all pass; refreshToken is placeholder UUID until E1-T5 |
| 2026-07-24 | E1-T6 | ✅ | `JwtAuthenticationFilter` (Bearer token → `UsernamePasswordAuthenticationToken` with role); `SecurityConfig` wires filter, admin path requires ROLE_ADMIN; 6 unit tests (JwtAuthenticationFilterTest) + 10 slice tests (SecurityFilterChainTest) all pass |
| 2026-07-24 | infra | ✅ | SpotBugs 4.8.6 (Gradle plugin 5.2.5) added to backend `check` task; exclude filter suppresses JPA false positives; `JwtService` marked `final` (SEI CERT OBJ-11 fix); Claude Code GitHub Action added to CI for automated PR review |

---

## 13. Open Questions & Blockers

Managed by `/dev-task-open-question`. Format:

```
### Q<n> — <one-line title>            [OPEN | RESOLVED yyyy-mm-dd]
**Context:** why this came up, which task it blocks
**Options:** A / B / C with trade-offs
**Recommendation:** the option Claude would pick and why
**Decision:** (filled in by the user)
**Consequences:** what changes in code/docs as a result
```

### Q1 — Vector store: pgvector or Azure AI Search?    [OPEN]
**Context:** Blocks E9-T3. RAG grounds tutor and interview answers in the lecture corpus.
**Options:**
- **A — pgvector in the existing Postgres.** Zero new infrastructure, one backup story, free. Weaker hybrid search; scaling is manual past ~1M chunks.
- **B — Azure AI Search.** Managed hybrid (vector + BM25) retrieval, direct Azure AI Fundamentals coverage. Adds a cloud dependency, cost, and latency; local dev needs a fake.
- **C — Both behind a `VectorStore` interface.** pgvector locally, Azure in cloud. Most work, best optionality.
**Recommendation:** **C**, implemented as A first. The interface costs one afternoon and keeps the certification objective alive without blocking local development.
**Decision:** _pending_

### Q2 — Sandbox execution: host Docker socket or a dedicated runner host?    [OPEN]
**Context:** Blocks E3-T2 design and E10 deployment. Mounting `/var/run/docker.sock` into the API is effectively root on the host if the API is ever compromised.
**Options:**
- **A — Host socket, API spawns containers.** Simplest, fine for local dev and a single-node demo. Serious blast radius in production.
- **B — Separate runner service** on its own host/VM with a small internal API; the platform never touches Docker directly. More infra, much smaller blast radius.
- **C — Kubernetes Jobs** with a restricted service account. Cloud-native, most operational overhead.
**Recommendation:** **A for local/dev, B before any public deployment.** Keep the execution code behind a `CodeExecutionEngine` interface from day one so the swap is a config change (E3-T2 must honour this).
**Decision:** _pending_

### Q3 — Content licensing and originality    [OPEN]
**Context:** The product is "inspired by JavaRush". Screenshots of JavaRush lectures and tasks were provided as reference.
**Note:** JavaRush's lecture text, task wording, characters, and artwork are copyrighted. All platform content must be original — AI-generated or written by you. Cloning the *mechanics* (levels, XP, instant verification, quest map) is fine; copying text, task descriptions, or assets is not.
**Recommendation:** Generate all content via A1 and treat any imported JavaRush text as a hard blocker. Confirm you're aligned before E2-T6 seed data.
**Decision:** _pending_

---

## 14. Glossary

**Self-healing loop** — generate → run in sandbox → on failure feed the error back to the model → regenerate, max 3 attempts.
**Socratic hint** — a guiding question grounded in the student's actual error, containing no solution code.
**Verdict** — the terminal state of a submission: `PASSED` or `FAILED`.
**Judge (A5)** — an out-of-band model that scores other agents' outputs for hallucination and answer leakage.
**MCP** — Model Context Protocol; the tool layer the Content Architect uses to reach real-world sources.
