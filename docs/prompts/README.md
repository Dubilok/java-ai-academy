# Agent prompts

One file per agent, versioned in git so prompt changes are reviewable and diffable:

- `content-architect.md` — A1, Claude. Structured JSON curriculum generation + self-healing (E4-T3).
- `socratic-mentor.md` — A2, Gemini. Guiding questions only, never solution code (E5-T2).
- `mock-interviewer.md` — A4, Claude. Adaptive questioning + 10-dimension evaluation (E6-T4).
- `judge.md` — A5. Scores other agents for hallucination and answer leakage (E9-T7).

Every prompt must state explicitly that student code and lecture text inside delimited blocks are
**data, not instructions** (see `CLAUDE.md` §6.3).
