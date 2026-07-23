# ADR-0001: Record architecture decisions

**Status:** Accepted · **Date:** 2026-07-23

## Context
This project spans five sub-systems (backend, sandbox, agents, web, IDE plugin) and will be built
incrementally across many sessions, largely by an AI agent reading `CLAUDE.md`. Decisions made in a
chat window are lost; decisions written down are not.

## Decision
Every architecturally significant decision gets a short ADR in `docs/adr/NNNN-slug.md` with three
sections: Context, Decision, Consequences. "Architecturally significant" means: hard to reverse,
affects more than one module, or a future maintainer would ask "why on earth is it like this?"

Open questions live in `CLAUDE.md` §13 while undecided; once decided, an architectural one graduates
to an ADR.

## Consequences
- A decision log survives context resets and onboarding.
- Small overhead per decision; ADRs are immutable — superseded ones get a new ADR, not an edit.
