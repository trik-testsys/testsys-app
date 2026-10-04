# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

TestSys — a Kotlin/JVM system for grading TRIK Studio solutions. Gradle multi-module build, Kotlin 2.4, JDK 21,
Spring Boot 4 / Hibernate 7 (the web module adds Vaadin 25 Flow), PostgreSQL + Liquibase, KSP, Detekt.

## Documentation is the source of truth

The project documentation (Russian) owns every rule and fact. This file, `.agents/skills/` and `.codex/agents/`
only point to it.

- **One direction only:** Codex files link to the docs; the docs never link to or mention Codex files.
  Never add a reference to `AGENTS.md`, `.agents/`, `.codex/` or a skill into any document. The only exception is the root
  `README.md`, which lists the skills and agents (see "Где лежит документация" in `docs/docs.md`); keep that list
  in sync when a skill or agent is added, renamed or removed.
- **Do not restate the docs here or in skills.** If a rule is missing, add it to the owning document
  (see `docs/docs.md`) and link to it from here.
- Before any task, read the documents relevant to it from the map below; follow them over your own defaults.
- When a change alters documented behaviour, paths or names, update the docs in the same change, following
  `docs/docs.md`.
- Files for Codex (`AGENTS.md`, `.agents/skills/**`, `.codex/agents/**`) are written in English.

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
| Web UI: design tokens and interface rules              | `docs/project/ui-design.md`                           |
| Web UI in Kotlin: pages, grid, DSL components          | `testsys-web/components/README.md`                            |
| Operations, `@Feature`, operation error model          | `testsys-operation/README.md`                         |
| Adding a feature / an entity / a port / a localized message | `docs/guides/implement-feature.md`, `docs/guides/implement-entity.md`, `docs/guides/implement-port.md`, `docs/guides/add-localization.md` |

## Skills and agents

Skills live in `.agents/skills/`; agent definitions and their limits live in `.codex/agents/*.toml`.
Before changing a skill or agent, read its current definition and the project documents it references.
There are no repository-local Codex hooks; follow the limits in the agent definitions and the active sandbox.

- `/review-changes` skill → `reviewer` agent (+ `review-verifier`) — strict read-only review of changes with a structured
  report. Both agents remain read-only; see `.codex/agents/reviewer.toml` and `.codex/agents/review-verifier.toml`.
- `/fix-review` skill → `fixer` agent — fixes selected findings of a reviewer report in the working tree: `MODE: plan`
  (re-check, plan, open decisions asked by the skill) then `MODE: apply` (edits, build and tests). Never commits;
  limits are in `.codex/agents/fixer.toml`.
- `/implement` skill → `coder` agent — implements a task following the guides in `docs/guides` with a checklist
  self-check: `MODE: plan` (checklist-based plan; all questions asked by the skill) then `MODE: apply` (autonomous,
  no questions; build and tests). Never commits; limits are in `.codex/agents/coder.toml`.
- `/testsys-design` skill (no agent) — designs and builds web UI with the components in
  `testsys-web/components/`, following `docs/project/ui-design.md`.
