---
name: implement
description: Implement a TestSys task (free text, a testsys.* feature from features.md, or a task from a file) with the coder agent - build a guide-based plan, resolve every question with the user up front, then implement autonomously with a checklist self-check, build and tests. Use when the user asks to implement, add or build a feature, entity, port, localized message or other code change. Never commits.
---

# Implement a task

This skill collects the task, runs the `coder` agent in plan mode, resolves **all** questions with the user, and then
runs the implementation, which asks nothing. The rules of work, guide handling and report formats live in
`.claude/agents/coder.md`. The skill itself never edits files, never commits and never posts anything.

## Steps

1. **Get the task.** If the request states it, use it. Otherwise ask one `AskUserQuestion`: **Describe in text**,
   **Feature from features.md** (codifier in the "Other" field) or **Task from a file** (path in the "Other" field).
   Resolve it:
   - text — use verbatim;
   - feature — find the codifier in `docs/domain/features.md` and take its entry verbatim; if it does not exist, tell
     the user and ask for a text description instead;
   - file — `Read` it; if it holds several tasks (for example `TODO.md`), ask which item.

2. **Check the working tree.** Run `git status --short`. If there are uncommitted changes, tell the user they will be
   mixed with the implementation in one diff and ask whether to continue.

3. **Plan.** Launch `Agent` with `subagent_type: coder` and the prompt:

   ```text
   MODE: plan
   Task source: <Text | Feature <codifier> | File <path>[, item <…>]>
   Task: <verbatim>
   User notes: <anything else the user said, verbatim, or "none">
   ```

   If the agent returns a missing-input message, resolve it and repeat.

4. **Resolve everything now.** This is the last point where the user is asked anything.
   1. Show a compact plan: guides used (or **No guide**), the checklist rows, assumptions, documentation changes
      (quote the exact text for `features.md` / `definitions.md`), out of scope.
   2. Ask every `Needs decision` item with `AskUserQuestion`, using the agent's question and options, recommended
      option first; batch up to 4 questions per call.
   3. Ask one final `AskUserQuestion`: **Approve the plan** / **Correct the plan** (corrections in the "Other" field)
      / **Cancel**. Corrections that change the scope, guides or checklist → repeat step 3 with the corrections in
      `User notes` and the previous answers; small corrections (an assumption, a name) → pass them in `Decisions`.

5. **Implement.** Launch a new `Agent` with `subagent_type: coder` and the prompt:

   ```text
   MODE: apply
   Task source: <same as in step 3>
   Task: <same as in step 3>
   Plan: <the approved plan, verbatim>
   Decisions: <D1: chosen option; …; corrections to assumptions, verbatim; or "none">
   User notes: <same as in step 3>
   ```

   Do not interrupt the run with questions.

6. **Show the result.** Output the agent's report verbatim. Point out rows marked `Not done`, a failed build and
   the decisions the agent made on its own. Suggest reviewing `git diff` and running `/review-changes`, and
   `/fix-review` for its findings; do not commit.
