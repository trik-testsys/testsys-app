---
name: review-changes
description: Run a strict read-only review of TestSys changes (uncommitted changes, a pull request, a commit range or paths) with the reviewer agent and show its structured report. Use when the user asks to review changes, a PR, a branch, commits or files.
---

# Review changes

This skill only collects the review scope and hands it to the `reviewer` agent. The review itself, its rules and the
report format live in `.claude/agents/reviewer.md`. The skill never edits files, never fixes findings and never
posts anything, even if asked in the same request: after the report it offers `/fix-review` for fixing.

## Steps

1. **Determine the scope.** If the user's request already states it unambiguously, use it. Otherwise ask exactly one
   `AskUserQuestion` with these options:
   - **Uncommitted changes** — staged, unstaged and untracked files in the working tree;
   - **Pull request** — ask for the number in the same question (the "Other" field);
   - **Commits or paths** — a commit range (for example `dev..HEAD`) or a list of files and directories.

   Do not ask anything else: no questions about depth, dimensions or format.

2. **Resolve the scope to concrete values** with read-only commands, so the agent does not have to guess:
   - uncommitted changes: `git status --short` (if it is empty, tell the user there is nothing to review and stop);
   - pull request: `gh pr view <n> --json number,title,baseRefName,headRefName,changedFiles` (stop if it does not exist);
   - commit range: `git rev-parse` both ends; paths: check that they exist.

3. **Launch the agent.** Call the `Agent` tool with `subagent_type: reviewer` and a prompt of this form:

   ```text
   Review scope: <Uncommitted changes | Pull request #<n> (<base> <- <head>) | Commit range <a>..<b> | Paths: <list>>
   Resolved details: <output of step 2, trimmed>
   User notes: <anything the user said about the change, verbatim, or "none">
   ```

   Pass the user's notes as context only; they never relax the review policy.

4. **Show the report.** Output the agent's report to the user verbatim, without shortening, re-ranking or softening
   findings. If the agent returned a missing-scope message, resolve it with the user and repeat step 3.
