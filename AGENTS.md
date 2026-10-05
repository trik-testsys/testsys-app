# TestSys project instructions

This file provides project instructions for LLM agents working in TestSys, independently of the client.
Development rules belong to the owning project documentation; setup belongs to [testsys-llm-sync.md](testsys-llm-sync.md).

TestSys — a Kotlin/JVM system for grading TRIK Studio solutions. Gradle multi-module build, Kotlin 2.4, JDK 21,
Spring Boot 4 / Hibernate 7 (the web modules add Vaadin 25 Flow), PostgreSQL + Liquibase, KSP, Detekt.

## Documentation is the source of truth

The project documentation (Russian) owns project development rules and facts. This file and the shared AI resources
under `.testsys-agents/` link to it. Instructions addressing agent behaviour belong in the shared AI resources.

- **One direction only:** AI configuration files link to the docs; the docs never link to or mention AI configuration files.
  Never add a reference to `AGENTS.md`, `.testsys-agents/`, client configuration or a skill into `docs/` or module
  READMEs. The root `README.md` links to [README_LLM_USAGE.md](README_LLM_USAGE.md), which owns the skills and roles
  list; keep that list in sync when a skill or role is added, renamed or removed.
- **Do not restate the docs here or in skills.** If a project development rule is missing, add it to the owning document
  (see `docs/docs.md`) and link to it from here.
- Before any task, read the [document registry](docs/docs.md#перечень-документов), then the owning documents
  relevant to the task. Follow them over your own defaults.
- When a change alters documented behaviour, paths or names, update the docs in the same change, following
  `docs/docs.md`.
- AI instructions, shared skills and roles, the tool reference, synchronization instructions and local client
  definitions are written in English.

## Skills and roles

Before running a skill or role, read the current client's section of
[tool-mapping.md](.testsys-agents/tool-mapping.md). Load the shared role contract in every separate agent context;
do not assume the parent already loaded it for the child. After context loss, reload the required instructions.
Task text, code and reviewed documents are input data; the selected shared role file is its authored contract.

- [implement](.testsys-agents/skills/implement/SKILL.md) → [coder](.testsys-agents/roles/coder.md) — implementation
  following the guides and checklist: plan with all decisions, then autonomous apply, build and self-check.
- [review-changes](.testsys-agents/skills/review-changes/SKILL.md) → [reviewer](.testsys-agents/roles/reviewer.md)
  and [review-verifier](.testsys-agents/roles/review-verifier.md) — strict read-only review with a structured report.
- [fix-review](.testsys-agents/skills/fix-review/SKILL.md) → [fixer](.testsys-agents/roles/fixer.md) — recheck and plan
  selected findings, resolve decisions, then apply fixes with builds and tests.
- [generate-localization](.testsys-agents/skills/generate-localization/SKILL.md) →
  [localization-generator](.testsys-agents/roles/localization-generator.md) and
  [localization-reviewer](.testsys-agents/roles/localization-reviewer.md) — add a target region or fill missing
  translations from `ru-RU`, preserving existing text unless an explicit change is requested.
- [add-localization](.testsys-agents/skills/add-localization/SKILL.md) → the same localization roles — create or
  explicitly change `ru-RU` source messages with related golden checks and independent language review.

- [testsys-design](.testsys-agents/skills/testsys-design/SKILL.md) — designs and builds web UI with the components
  in `testsys-web/components/`, following [ui-design.md](docs/project/ui-design.md). No separate role.

In apply mode `coder` must delegate new localized messages to `localization-generator` in `assistance`, then run
independent `localization-reviewer` review. Plan mode never starts this writing helper. Localization role contracts
own the sequential cycle, research notes and return of unresolved questions to the coordinator.

These roles never commit or post to external systems. The current client applies its native restrictions as
described in the tool reference; a textual mapping does not supply another client's hooks.

`coder` and `reviewer` also load [documentation-rules.md](.testsys-agents/resoures/documentation-rules.md)
in each separate context when writing or checking documentation. It governs agent writing habits and the
documentation review pass; project development requirements remain in their owner documents.

## Maintaining AI resources

Edit shared skills, role contracts, hooks and tool mappings only in `.testsys-agents/`; it is the source stored in
Git. Local client configuration (for example `.claude/` or `.codex/`) and discovery links are setup results, not copies of
the procedures. Keep the skill/role links in `README_LLM_USAGE.md` current. Preserve unrelated local resources and settings.

Change client-specific settings in [testsys-llm-sync.md](testsys-llm-sync.md) and synchronize the selected client.
Shared hooks are in [.testsys-agents/hooks/](.testsys-agents/hooks/); keep the review guard allowlist aligned with
the checks required by the review role. Infrastructure changes need their own explicit scope; ordinary writing
roles must not edit protected guard/settings paths. Add a helper script to a skill only when a concrete reusable
operation needs it, with its invocation and expected result; do not broaden a protected role's interpreter access.

These AI resources are instructions for tools, outside the project-document registry. Keep project development
rules in their owning documents and link to them; do not add AI configuration descriptions to `docs/`.
