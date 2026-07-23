---
description: Execute the next unchecked task from the CLAUDE.md roadmap, verify it, and record the result
argument-hint: "[optional task id, e.g. E3-T4]"
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Execute one roadmap task

You are implementing the Java AI Academy platform. Read `@CLAUDE.md` in full before doing anything —
it is the single source of truth for architecture (§2), package layout (§3), coding standards (§5),
security requirements (§6), data model (§7), API contract (§8), the roadmap (§9), and verification
commands (§10).

Requested task: **$ARGUMENTS** (if empty, take the first `[ ]` task in document order from §9).

## Procedure

### 1. Select and confirm scope
- Identify the task id and its epic. If the requested task has unmet dependencies (an earlier `[ ]`
  task it needs), say so and propose the correct task instead of charging ahead.
- If the task is marked `[!]` blocked, stop and point at the relevant question in §13.
- State in one or two sentences: the task id, what you will build, and which files you expect to touch.

### 2. Check for ambiguity **before** writing code
If the task requires a decision that isn't already settled in `CLAUDE.md` — a library choice, a schema
shape, an auth flow detail, anything with two defensible answers — **do not pick one silently**.
Stop and run the `/dev-task-open-question` workflow instead. Guessing here costs more than asking.

### 3. Implement
- Follow §5 standards for the language you're in. Constructor injection, records for DTOs, no `any`
  in TypeScript, no blocking calls on the EDT in Kotlin.
- Respect the §3 package layout — feature packages, no top-level `services/` or `models/`.
- Honour §6 security rules exactly. Every sandbox flag in §6.2 is mandatory; never relax one to make
  a test pass.
- Small, focused diffs. Don't refactor unrelated code, don't add speculative abstraction for future
  epics, don't upgrade dependencies that weren't part of the task.
- Write the tests **as part of the task**, not afterwards. Cover the failure path: the timeout, the
  403, the malformed payload, the exhausted retry — not just the happy case.

### 4. Verify — with real commands, not optimism
Run the verification commands from §10 that apply. At minimum:
- Backend: `cd backend && ./gradlew spotlessApply build`
- Frontend: `cd frontend && npm run lint && npm run typecheck && npm run test`
- Plugin: `cd ide-plugin && ./gradlew test`
- Anything touching the sandbox: run the adversarial suite (E3-T8).

If something fails, fix it and re-run. Do not report a task complete on the strength of a partial run.
If it fails for an environmental reason you can't resolve (Docker not running, missing API key), say
so plainly and mark the task `[~]` in progress with a note — an honest partial beats a false `[x]`.

### 5. Record the outcome in CLAUDE.md
- Flip the checkbox in §9: `[ ]` → `[x]` (or `[~]` / `[!]` if not finished).
- Append a row to the §12 progress log: date, task id, ✅/⚠️, and a one-line note that would be
  useful to someone reading it cold in three weeks.
- Update §7 if the schema changed, §8 if the API changed, and add an ADR under `docs/adr/` if you made
  an architectural decision.

### 6. Report back
Keep it short:
- **Done:** what now works that didn't before
- **Files:** created / modified
- **Verified by:** the exact command(s) run and their result
- **Notes:** anything surprising, any debt taken on deliberately
- **Next:** the next `[ ]` task in the roadmap

Then stop. Do not roll straight into the next task — the user decides when to continue.
