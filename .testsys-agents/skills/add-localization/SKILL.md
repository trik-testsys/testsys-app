---
name: add-localization
description: Create or explicitly change TestSys ru-RU source messages from meaning and usage context through localization-generator assistance, add related golden checks, and independently review language. Other regions are preserved; expected missing translations are reported honestly. Never commits.
---

# Add source localization

This skill coordinates `ru-RU` source authoring by the
[generator](../../roles/localization-generator.md) in `assistance` and the independent
[reviewer](../../roles/localization-reviewer.md). It collects human decisions and saves reviewer research.
Translations for other regions belong to [generate-localization](../generate-localization/SKILL.md).

## Prepare

1. Find the root with `git rev-parse --show-toplevel`, or the nearest parent containing `AGENTS.md` and
   `.testsys-agents/`. Read root instructions, the current client's [tool mapping](../../tool-mapping.md),
   [document registry](../../../docs/docs.md#перечень-документов) and all of the
   [guide](../../../docs/guides/add-localization.md). Record `git status --short` and preserve existing changes.
2. Collect the intended message meaning and usage context. Keys, bundles and parameters may be omitted: the
   generator chooses them. Preserve an explicitly supplied contract. Identify exactly which existing messages
   the user explicitly requests to change; adding a message does not authorize unrelated rewrites.
3. Region is fixed to `RU / ru-RU`; other regions, including stubs, remain untouched. Read relevant notes at
   `.testsys-agents/resoures/localization/ru-RU.md` if present. Ask unresolved meaning, contract or scope questions
   and wait. Otherwise proceed without separate intermediate-plan approval.

## Coordinate the cycle

Use the sequential five-iteration coordination in
[generate-localization](../generate-localization/SKILL.md#coordinate-the-cycle), with these source-authoring inputs
and conditions. This link shares coordination, not the translation mode or its write scope.

1. Start the named `localization-generator` in a separate context with all fields of its input contract:
   `MODE: assistance`, task, `Region: RU / ru-RU`, `Caller: human`, meaning/usages, requested contract or `none`,
   canonical Russian research path, `Iteration: 1` and feedback or `none`. Wait for its writes and checks.
2. Distinguish new-source missing keys/bundles in other registered regions from MF2 and other errors. Expected
   omissions alone do not start another repair pass or authorize changing other regions. Report codegen failure
   as `failed`, blocked later stages as `not-run` and contracts as proposed if API was not generated. Added golden
   checks must cover branches, but their expected strings do not establish successful rendering.
3. Start a fresh named `localization-reviewer` on the actual `ru-RU` messages, intended source meaning, usages,
   related terms and golden tests. Supply every reviewer field and factual check evidence, without generator
   reasoning or previous assessments. Text review can proceed when expected omissions block rendering, with
   that limitation in coverage. Save confirmed reviewer research yourself; the reviewer never writes.
4. Pass confirmed unambiguous in-scope defects to the generator for iterations `2..5`; review each reviewable
   revision in a fresh context. The one counter includes initial generation, structural checks, golden tests when
   possible and language review; blocked stages still consume a pass. Return ambiguity/forbidden changes early,
   and remaining problems after iteration five, to the human. Never fan out or substitute generic roles.

## Show the result

Show changed source keys/files, related golden checks, saved research, actual command results and independent
review coverage/verdict. Include the generator's actual created MF2 and its explanation of parameters,
declarations, branches, formatting and term references. State expected incomplete translations and API/test
availability separately; completeness across regions remains necessary for a successful build. Name unavailable
capabilities and unresolved questions. Do not commit, publish or change global/client enforcement settings.
