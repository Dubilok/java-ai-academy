---
description: Log an ambiguity, architectural decision, or blocker in CLAUDE.md and pause for the user's answer
argument-hint: "[the question, or leave empty to use the current blocker]"
allowed-tools: Read, Write, Edit, Glob, Grep
---

# Raise an open question

Something needs a human decision before implementation continues. Your job here is **not** to solve it —
it's to frame it well enough that the user can decide in under a minute, then stop.

Question: **$ARGUMENTS** (if empty, use the ambiguity you just hit in the current task.)

## Procedure

### 1. Try to answer it yourself first
Before escalating, check whether the answer already exists:
- `CLAUDE.md` §2 (architecture), §4 (stack), §5 (standards), §6 (security), §7/§8 (schema and API)
- `docs/adr/` — a past decision may already cover this
- The existing code — a convention may already be established

If you find the answer, say where it was and carry on. Don't spend the user's attention on a
question the project has already answered.

### 2. Classify it
- **Blocking** — implementation cannot proceed correctly. Stop and ask.
- **Non-blocking** — you can proceed with a reasonable default. Ask, but state your default and keep working.
- **Product decision** — scope, UX, content, or cost. Always the user's call, never yours.

### 3. Write it into §13 of CLAUDE.md
Append using exactly this format, numbering sequentially from the last question:

```
### Q<n> — <one-line title>            [OPEN]
**Raised:** <yyyy-mm-dd> during <task id>
**Context:** why this came up and what it blocks — 2–3 sentences, no essay
**Options:**
- **A — <name>.** <how it works> · Pro: … · Con: …
- **B — <name>.** <how it works> · Pro: … · Con: …
- **C — <name>.** <how it works> · Pro: … · Con: …
**Recommendation:** <the option you'd choose> — <one sentence on why>
**Reversibility:** <cheap to change later / expensive / one-way door>
**Decision:** _pending_
```

If the question blocks a roadmap task, also mark that task `[!]` in §9 with a pointer: `[!] E3-T2 … (blocked by Q4)`.

### 4. Present it to the user in chat
Same content, formatted for a quick read:

> **Q4: <title>** — blocks E3-T2
>
> <two sentences of context>
>
> **A.** <option> — <trade-off in a half-line>
> **B.** <option> — <trade-off in a half-line>
> **C.** <option> — <trade-off in a half-line>
>
> **My recommendation: B**, because <one sentence>. Reversible later at moderate cost.
>
> Reply with A, B, C, or your own answer.

Rules for this summary:
- Two to four options. One is not a choice; six is a research project.
- Every option gets a real trade-off. If an option has no downside, it isn't an option — it's the answer.
- Always give a recommendation. "It depends" is not useful to someone who hired an architect.
- Say whether the decision is reversible. It changes how much thought it deserves.
- No jargon the user hasn't already used in the project docs.

### 5. Stop
End your turn. Do not implement a workaround, do not proceed on the assumption that your recommendation
will be accepted, and do not ask a second question in the same message.

### 6. When the user answers (next turn)
- Update §13: change `[OPEN]` to `[RESOLVED <yyyy-mm-dd>]`, fill **Decision** with the chosen option and
  the user's reasoning if they gave any, and add a **Consequences** line listing what changes as a result.
- If the decision is architectural, write an ADR in `docs/adr/NNNN-<slug>.md` (context → decision → consequences).
- Unblock the task in §9: `[!]` → `[ ]`.
- Append to the §12 log: `<date> | Q<n> | ✅ resolved | <one-line summary>`.
- Then continue with `/dev-task-step` **only if the user asks you to.**
