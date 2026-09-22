---
name: fix-review
description: Fix findings from a reviewer report (produced by /review-changes) in the TestSys working tree with the fixer agent - pick the report, choose the findings, resolve open decisions up front, then apply fixes and run builds and tests. Use when the user asks to fix, apply or address review findings. Never commits.
---

# Fix review findings

This skill collects everything the `fixer` agent needs and resolves all decisions with the user **before** any file
is changed. The fixing rules, limits and report formats live in `.claude/agents/fixer.md`. The skill itself never
edits files, never commits and never posts anything.

## Steps

1. **Find the report.** A reviewer report is recognised by `## Summary` with a `**Verdict:**` line and a `## Findings`
   section of `### Issue N (<severity>): <title>` blocks.
   - If this conversation contains such a report, ask one `AskUserQuestion`: **Last report from this conversation**
     (name its Scope line) or **Another source** (a file path or pasted text in the "Other" field).
   - If there is none, ask for the source the same way: a file path, pasted report text, or run `/review-changes`
     first (then stop).
   Read a file source with `Read`. If the text does not match the format above, tell the user and stop.

2. **Check freshness.** Run `git status --short`. If the report's scope is uncommitted changes and files from its
   Locations no longer appear or were clearly rewritten, tell the user the report may be stale; plan mode re-checks
   every finding against the current code either way.

3. **Choose findings.** Print a compact numbered list of the report's issues — number, severity, title, first
   Location — grouped as **Findings** and **Pre-existing issues**. **Needs human check** items are not fixable: list
   them separately as excluded. Then ask one `AskUserQuestion`:
   - with at most 4 issues in total: `multiSelect` with one option per issue;
   - otherwise options **All Error and Warning (N)** (recommended), **Only Error (N)**, **All findings including
     Info (N)**, and the "Other" field for explicit numbers (for example `1, 3, 5`), which may include
     pre-existing issues.
   If nothing is selected, stop.

4. **Plan.** Launch `Agent` with `subagent_type: fixer` and the prompt:

   ```text
   MODE: plan
   Review scope: <Scope line of the report>
   Selected findings: <each selected issue verbatim, with its section name>
   User notes: <anything the user said about the fixes, verbatim, or "none">
   ```

   If the agent returns a missing-input message, resolve it and repeat.

5. **Resolve decisions.** Show the user a short summary of the plan: per issue its status and the files it touches;
   for `Already fixed` and `Won't fix (refuted)` also the one-line reason. For every `Needs decision` item ask
   `AskUserQuestion` using the agent's question and options (recommended option first, plus an option to skip the
   issue); batch up to 4 questions per call. Do not ask anything else. If no item remains to fix, stop.

6. **Apply.** Launch a new `Agent` with `subagent_type: fixer` and the prompt:

   ```text
   MODE: apply
   Review scope: <same as in step 4>
   Selected findings: <same as in step 4>
   Plan: <the plan from step 4, verbatim>
   Decisions: <Issue N: chosen option, verbatim; or "none">
   User notes: <same as in step 4>
   ```

7. **Show the result.** Output the agent's report verbatim. If items are `Not fixed` with a new decision, offer to
   resolve it and run steps 4–6 for those items only. Suggest reviewing `git diff` and re-running `/review-changes`;
   do not commit.
