# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

TestSys — a Kotlin/JVM system for grading TRIK Studio solutions. Gradle multi-module build, Kotlin 2.2, JDK 21,
Spring Boot 3.5 / Hibernate, PostgreSQL + Liquibase, KSP, Detekt.

## Documentation is the source of truth

The project documentation (Russian) owns every rule and fact. This file and everything under `.claude/` only point
to it.

- **One direction only:** Claude files link to the docs; the docs never link to or mention Claude files.
  Never add a reference to `CLAUDE.md`, `.claude/` or a skill into any document. The only exception is the root
  `README.md`, which lists the skills and agents (see "Где лежит документация" in `docs/docs.md`); keep that list
  in sync when a skill or agent is added, renamed or removed.
- **Do not restate the docs here or in skills.** If a rule is missing, add it to the owning document
  (see `docs/docs.md`) and link to it from here.
- Before any task, read the documents relevant to it from the map below; follow them over your own defaults.
- When a change alters documented behaviour, paths or names, update the docs in the same change, following
  `docs/docs.md`.
- Files for Claude (`CLAUDE.md`, `.claude/**`) are written in English.

## Where to look

| Task                                                   | Read                                                  |
|--------------------------------------------------------|-------------------------------------------------------|
| Any task: list of all documents                        | `docs/docs.md`, section "Перечень документов"         |
| Writing or changing documentation                      | `docs/docs.md`                                        |
| Domain terms (Задача, Тур, Соревнование, Роли, …)      | `docs/domain/definitions.md`                          |
| What the system does, feature codifiers `testsys.*`    | `docs/domain/features.md`                             |
| Modules, dependencies, build commands, CI, code placement | `docs/project/structure.md`                        |
| Code style, error handling, KDoc                       | `docs/project/code-style.md`                          |
| Tests: tools, naming, scenario kinds, structure        | `docs/project/unit-tests.md`                          |
| Domain model, ports, builder DSL                       | `testsys-domain/README.md`                            |
| Persistence layers, Snowflake ids, node id, files, schema | `testsys-infra/database/README.md`                 |
| Localization                                           | `testsys-infra/localization/README.md`                |
| Operations, `@Feature`, operation error model          | `testsys-operation/README.md`                         |
| Adding a feature / an entity / a port / a localized message | `docs/guides/implement-feature.md`, `docs/guides/implement-entity.md`, `docs/guides/implement-port.md`, `docs/guides/add-localization.md` |

## Skills and agents

Before creating or changing an agent, skill or hook, read `.claude/agent-authoring.md` (platform facts, environment
quirks, patterns and best practices).

- `/review-changes` skill → `reviewer` agent (+ `review-verifier`) — strict read-only review of changes with a structured
  report. The agents are guarded by `.claude/hooks/review-readonly-guard.sh`; keep its allowlist in sync with the
  checks the reviewer is told to run.
- `/fix-review` skill → `fixer` agent — fixes selected findings of a reviewer report in the working tree: `MODE: plan`
  (re-check, plan, open decisions asked by the skill) then `MODE: apply` (edits, build and tests). Never commits;
  guarded by `.claude/hooks/write-guard.sh`.
- `/implement` skill → `coder` agent — implements a task following the guides in `docs/guides` with a checklist
  self-check: `MODE: plan` (checklist-based plan; all questions asked by the skill) then `MODE: apply` (autonomous,
  no questions; build and tests). Never commits; shares `.claude/hooks/write-guard.sh` with `fixer`.
