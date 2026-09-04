---
name: kdoc-audit
description: Use when writing, reviewing or fixing KDoc in this repository's Kotlin modules - after adding public declarations, when asked to align documentation with project conventions, or before a PR that touches *.kt files.
---

# KDoc audit

## Overview

The rules live in `CLAUDE.md` ("KDoc conventions"); this skill is the procedure and the checker
that enforce them. Never re-derive the checks by hand: run the script.

## Procedure

1. Read the "KDoc conventions" section of `CLAUDE.md` and the resolutions below.
2. Baseline: `python3 .claude/skills/kdoc-audit/scripts/check_kdoc.py <module>/src/main/kotlin`
   (or a single file). Run `--help` for codes.
3. Fix file by file. Only KDoc changes; code, imports and formatting stay untouched.
   For doc-heavy files rewrite the whole file, otherwise use spot edits.
   Remove imports that were only referenced from deleted KDoc links.
4. Re-run the checker until it reports `0 violations`.
5. Build: `./gradlew :<module>:build` (warnings are errors, Detekt autoCorrect runs, max 140 columns)
   and compile the modules that depend on it (`:testsys-infra:database:compileKotlin` for the domain).
6. Prove code is unchanged: `diff <(check_kdoc.py --strip OLD) <(check_kdoc.py --strip NEW)` per file
   (OLD from `git show HEAD:path` or a backup copy), or
   `git diff -U0 -- path | grep '^[-+]' | grep -vE '^[-+]{3}|^[-+]\s*(/\*\*|\*)'` which must print nothing.

## Resolutions of recurring ambiguities

| Question | Decision |
|----------|----------|
| KDoc on private/internal/protected | Optional, prose only, **no `@since`** |
| `override` members (incl. every `build()` in this repo) | No KDoc at all; an `override val` constructor property gets no `@property` either (the base type documents it) |
| Public non-override function | Always KDoc, even when trivial |
| What is an "aggregate" for links | Entity `X` plus what exists only for it: `XId`, `XData`, its sealed state types (content/status/kind/result), role data of a role, and any sealed/value type used by exactly one `XData`, whatever its name. All of them link `[X]` |
| Shared value types (`Score`, `FileData`, `TrikSupportedLanguage`) | Used by several entities: standalone, no links to their users |
| `@param`/`@return` | Ports in `contract`, `load(...)`, `file(name, content)`, two-lambda functions, `Builder.build`. DSL setters say "Sets [owner] from a raw id." when they wrap a value and "Sets [name]." when they just assign |
| Type parameters | `@param T ...` first, then `@property`, then `@since` |
| Feature codifiers `testsys.*` | Only when the feature exists in `docs/features.md` |
| Property docs in companion objects | Mention in the class summary (`[UNSORTED] is ...`), no block above |

## House phrases

- `Identifier of a [Task].` / `Data of a [Task].` / `@property data the data of the task.`
- `Builder of [TaskData]. Required: [owner], [name].` / `Builder of [Task] entities. Required: [id], [createdAt], [data].`
- `@property name the name of the task, or `null` if not set yet.`
- `Sets [owner] from a raw id.` / `Sets [tests] from raw ids.`
- `Selects [SubmissionStatus.Queued].` / `DSL chooser of a [SubmissionStatus].`

## Common mistakes

- Removing every `[link]` including aggregate ones (`TaskId` must say `[Task]`).
- Leaving `@since` on a private helper, or adding KDoc to an override.
- Documenting a property above itself "because it is long"; put it in `@property`.
- Deleting a KDoc link and leaving its now-unused import.
- Claiming done without step 4 and 5 output.
