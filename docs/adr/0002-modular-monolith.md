# ADR-0002: Start as a modular monolith, not microservices

**Status:** Accepted · **Date:** 2026-07-23

## Context
The original architecture sketch showed an API gateway with separate auth, course, and execution
services. The system has one team, one database, zero users today, and no measured load.

## Decision
Build a single Spring Boot application organised into feature packages (`auth`, `catalog`, `progress`,
`sandbox`, `ai`, `interview`) with enforced boundaries: no cross-package entity references, no
repository access across features, communication via public service interfaces only. ArchUnit tests
enforce this (E1-T8).

The sandbox is the exception — it is a genuinely separate *runtime* (ephemeral containers), and its
execution engine sits behind a `CodeExecutionEngine` interface so it can move to a dedicated host or
Kubernetes Jobs without touching call sites (see Q2 in `CLAUDE.md` §13).

## Consequences
- One deployable, one transaction boundary, one database — dramatically simpler to build and debug.
- Enforced boundaries mean any feature package can be extracted into a service later if load justifies it.
- We give up independent scaling per feature. Acceptable: the only component with a plausible
  independent scaling need is the sandbox, which is already isolated.
