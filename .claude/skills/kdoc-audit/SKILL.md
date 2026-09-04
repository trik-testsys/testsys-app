---
name: kdoc-audit
description: Use when writing, reviewing or fixing KDoc in this repository's Kotlin modules - after adding public declarations, when asked to align documentation with project conventions, or before a PR that touches *.kt files.
---

# KDoc audit

## Overview

The rules live in `CLAUDE.md` ("KDoc conventions"); this skill is the procedure and the checker.
**The checker is a safety net, not the definition of done.** It cannot judge wording; you rewrite every
block, the checker only catches what you missed. A run whose diff is insertions-only has failed.

## Procedure

1. Read the "KDoc conventions" section of `CLAUDE.md` and the resolutions below.
2. Baseline: `python3 .claude/skills/kdoc-audit/scripts/check_kdoc.py <module>/src/main/kotlin`.
   Note both numbers of the last line: violations **and KDoc lines**. Run `--help` for the codes.
3. Rewrite **every** KDoc block of every file, flagged or not, to the target shape:
   one or two sentences, then `@param` (type params first), `@property`, `@return`/`@throws`, `@since`.
   Delete: second paragraphs, bullet lists of inherited/overridden members, `NOTE`/`CAUTION` prose that
   restates what the code enforces, `@see`, `@param x the x`. Add only what is missing.
   Only KDoc changes; code, imports and formatting stay untouched, except imports used solely by deleted links.
4. Re-run the checker until `0 violations`; compare KDoc lines with the baseline.
5. Build: `./gradlew :<module>:build` (warnings are errors, Detekt autoCorrect, 140 columns) and compile
   dependants (`:testsys-infra:database:compileKotlin` after touching the domain).
6. Prove code is unchanged: `git diff -U0 -- path | grep '^[-+]' | grep -vE '^[-+]{3}|^[-+]\s*(/\*\*|\*)'`
   must print nothing (or `diff <(check_kdoc.py --strip OLD) <(check_kdoc.py --strip NEW)`).

## Definition of done

- Checker: `0 violations`.
- KDoc lines did not grow beyond ~5 lines per previously undocumented declaration; on a module that already
  had docs the count goes **down**. Report baseline and final numbers.
- `git diff --stat` shows deletions, not only insertions.
- Build green, code diff empty.

## Before / after

```kotlin
/**
 * Base abstract class for each persistence adapter. Implements default behavior for next methods of [EntityRepository] contract:
 * - [EntityRepository.findById]
 * - [EntityRepository.findByIds]
 * ... (12 more bullet lines, a CAUTION paragraph, 5 @see tags)
 */
```
```kotlin
/**
 * Base of persistence adapters: implements finding, loading, removing and the list overloads of [EntityRepository];
 * subclasses provide [save], [update] and [assemble]. Overloads that need non-default [Transactional] settings
 * must be overridden together (Spring AOP self-invocation).
 *
 * @param Data the data type a new entity is created from.
 * ...
 */
```

## Resolutions of recurring ambiguities

| Question | Decision |
|----------|----------|
| KDoc on private/internal/protected | Optional, prose only, **no `@since`** |
| `override` members (incl. every `build()` in this repo) | No KDoc at all; an `override val` constructor property gets no `@property` either |
| Public non-override function | Always KDoc, even when trivial |
| What is an "aggregate" for links | Entity `X` plus what exists only for it: `XId`, `XData`, its sealed state types, role data of a role, any sealed/value type used by exactly one `XData`. All of them link `[X]` |
| Shared value types (`Score`, `FileData`, `TrikSupportedLanguage`) | Standalone: no links to their users |
| `@param`/`@return` | Ports in `contract`, `load(...)`, `file(name, content)`, two-lambda functions, `Builder.build`. DSL setters: "Sets [owner] from a raw id." or "Sets [name]." |
| Type parameters | `@param T ...` first, then `@property`, then `@since` |
| Feature codifiers `testsys.*` | Only when the feature exists in `docs/features.md` |
| Property docs in companion objects | Mention in the class summary (`[UNSORTED] is ...`), no block above |
| Local functions/classes inside a function body | Never public: no KDoc, the checker skips them |

## House phrases

- `Identifier of a [Task].` / `Data of a [Task].` / `@property data the data of the task.`
- `Builder of [TaskData]. Required: [owner], [name].` / `Builder of [Task] entities. Required: [id], [createdAt], [data].`
- `@property name the name of the task, or `null` if not set yet.`
- `Persistence adapter of [Class] entities backed by [ClassJpaEntity].` / `Mapping between [Class] and [ClassJpaEntity].`

## Red flags: stop and go back to step 3

- "I fixed everything the checker reported" - the checker reports structure, not wording.
- "The existing text is accurate, so I kept it" - accurate and long is still a violation of rule 2.
- Diff has no `-` lines in a file that already had KDoc.
- KDoc line count went up on a module that was already documented.
