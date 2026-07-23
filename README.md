# Java AI Academy

An interactive Java learning platform with instant Docker-sandboxed code verification, an AI-authored
curriculum, a Socratic AI tutor, an IntelliJ IDEA plugin, and an AI mock interviewer.

**Start here: [`CLAUDE.md`](./CLAUDE.md)** — architecture, coding standards, database schema, API
contract, and the full implementation roadmap. It is the single source of truth for both humans and
the AI agent building this.

## Quickstart

```bash
cp .env.example .env                                   # fill in secrets
docker compose -f infra/docker-compose.yml up -d       # Postgres 16 + Redis 7
docker compose -f infra/docker-compose.yml ps          # both should be healthy
```

Then open Claude Code in this directory and run:

```
/dev-task-step
```

It reads the roadmap in `CLAUDE.md` §9, implements the next unchecked task with tests, verifies it,
and records the result. When it hits something ambiguous it will run `/dev-task-open-question` and
stop for your decision rather than guessing.

## Layout

| Path | What lives there |
|---|---|
| `backend/` | Spring Boot 3.3, Java 21 — API, sandbox engine, agent orchestration |
| `frontend/` | Next.js 14 web workspace (Monaco editor, quest map, admin console) |
| `ide-plugin/` | Kotlin IntelliJ IDEA plugin |
| `sandbox-image/` | Hardened `openjdk:21-slim` runner image |
| `infra/` | Local stack, deployment config |
| `docs/` | API contract, agent prompts, ADRs |
| `.claude/commands/` | `/dev-task-step`, `/dev-task-open-question` |

## Commands

| Command | Does |
|---|---|
| `/dev-task-step` | Implements the next roadmap task, tests it, updates `CLAUDE.md` |
| `/dev-task-step E3-T4` | Implements a specific task |
| `/dev-task-open-question` | Logs a decision or blocker in §13 and waits for your answer |

Using a Claude Code version that prefers skills over commands? Migrate with:

```bash
mkdir -p .claude/skills/dev-task-step .claude/skills/dev-task-open-question
git mv .claude/commands/dev-task-step.md .claude/skills/dev-task-step/SKILL.md
git mv .claude/commands/dev-task-open-question.md .claude/skills/dev-task-open-question/SKILL.md
```

Both forms produce the same `/dev-task-step` command and support the same frontmatter.

## Ground rules

- Untrusted code — student *and* AI-generated — runs only in the sandbox described in `CLAUDE.md` §6.2.
  Every hardening flag is mandatory.
- Content must be original. The learning *mechanics* are inspired by JavaRush; the text, tasks, and
  artwork must not be copied from it (see Q3 in `CLAUDE.md` §13).
- No secrets in git. `.env.example` documents every variable.
