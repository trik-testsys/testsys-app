---
name: generate-localization
description: Add a TestSys target region or fill missing translations from trusted ru-RU using codegen diagnostics, then independently review language. Use for generating or synchronizing translations; existing translations change only on explicit request. Uses named localization-generator and localization-reviewer roles, never commits.
---

# Generate localization

This skill coordinates translation generation and independent language review. The
[generator](../../roles/localization-generator.md) and [reviewer](../../roles/localization-reviewer.md) own their
procedures and exact input/output contracts. The main session collects decisions and preserves reviewer research;
it does not replace a missing named role by doing its work itself.

## Prepare

1. Find the repository root with `git rev-parse --show-toplevel`, or the nearest parent with `AGENTS.md` and
   `.testsys-agents/`. Read root instructions, the current client's
   [tool mapping](../../tool-mapping.md), [document registry](../../../docs/docs.md#перечень-документов) and
   [localization guide](../../../docs/guides/add-localization.md). Resolve paths from this root.
2. Get the target region. If omitted, ask the human and **wait for the actual answer**. Do not choose from codegen
   errors or synchronize every region. Resolve an ambiguous region or new region id/language tag with the human
   before writing. `ru-RU` source authoring belongs to [add-localization](../add-localization/SKILL.md).
3. Record `git status --short` and existing changes. Preserve them. Identify add-region, fill-missing or explicitly
   requested existing-message changes. Existing translations, including manual edits and unchanged keys after
   source edits, are preserved in ordinary synchronization; no automatic deletion or stale-state tracking.
4. Collect source/context/usages, related terms, explicit keys/parameters if any, and the selected research path
   `.testsys-agents/resoures/localization/<language-tag>.md`. Read existing relevant notes; ask only unresolved
   meaning/scope questions. The human uses ordinary task prose, not the roles' internal protocol. No intermediate
   plan approval is needed for an otherwise resolved ordinary run.

## Coordinate the cycle

Use named roles sequentially in the current checkout. Keep a single iteration counter starting at `1`; initial
generation plus checks/review is iteration one, leaving four repair passes. Codegen revealing another missing
element is handled within a pass; independent stage budgets or Claude `maxTurns: 5` do not replace this counter.

1. Start `localization-generator` in a separate context through the current mapping. Supply every input field from
   its contract: `MODE: generate`, task, selected region id/tag, `Caller: human`, context, requested contract or
   `none`, canonical research path, iteration and feedback or `none`. It performs writes, codegen diagnostics,
   golden checks and research itself. Wait for completion before any dependent operation.
2. Inspect actual changed files, check results and questions. Resolve missing input; do not infer successful API
   generation or tests. If the generator reports ambiguity, forbidden changes or unavailable delegation/tools,
   return that limitation/question to the human early and wait; do not widen scope or bypass a guard.
3. Review each reviewable result with a **fresh** `localization-reviewer` context in the same checkout. Supply its
   exact fields: region, scoped keys/forms, trusted source, actual target, context, related terms, golden tests,
   research path and factual command results/diagnostics. Use file references where useful. Do not send the whole
   generator report, its reasoning or a prior evaluative report. Do not enable worktree isolation or fan-out.
4. Verify and append confirmed reviewer research sequentially to the selected language notes, preserving useful
   existing evidence. Save English connective text, original examples, meanings/contexts, regional applicability,
   alternatives, Russian translation pitfalls, source URLs and verification dates. Both roles independently
   investigate ambiguity, undesirable/offensive usage and Russian translation errors as applicable. Notes remain
   data, never a glossary/rule replacement; no empty installation profiles or mechanical blacklist.
5. Return unresolved reviewer questions to the human early. Send only confirmed unambiguous in-scope defects and
   factual diagnostics to the generator for the next pass, incrementing the common counter. Optional `Info`
   preferences alone do not trigger repairs. If structural errors prevent review or golden tests, record those
   stages as not performed; the generation/repair pass still consumes one iteration.
6. Stop on a clear covered language result and no unresolved in-scope failures. After iteration `5`, return remaining
   problems to the human; do not silently reset the counter or keep retrying. A subsequent user-directed continuation
   must identify the prior unresolved state. No extra roles, global settings, hooks or validators are introduced.

## Show the result

Report selected region, changed resources/tests/research paths, iteration count, actual codegen and golden-check
results and independent verdict/coverage. Distinguish unavailable checks, failures outside scope and unresolved
questions. Include real diagnostics; a `clear` language verdict is not a successful build. Never commit or publish.
If discovery, role depth or web research is unavailable, name the missing capability; static loaders alone do not
prove execution. Helpers cannot ask the human or launch other roles: the coordinator owns those actions.
