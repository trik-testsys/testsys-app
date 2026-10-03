# TestSys tool mapping

This reference maps actions used by TestSys skills and roles to tools of the current LLM client
(Claude Code, Codex, etc.). The sections below currently document Claude Code/Codex as examples.
It does not define project rules or grant permissions. Setup is owned by [testsys-llm-sync.md](../testsys-llm-sync.md);
role contracts are in [roles/](roles/). Read only the section for the current client and the shared IDEA section
when those tools are available. If the current client has no section, follow [Adding a client](#adding-a-client)
before running the workflow. Recheck the actual session schemas after a client update or context loss.

## Claude Code

| Action | Tool and inputs | Limits |
|--------|-----------------|--------|
| Read a file | `Read(file_path)`; `offset` and `limit` for a slice | Resolve repository paths from the root. |
| Find text / files | `Grep(pattern, path)` / `Glob(pattern, path)` | Read the returned matches before deciding; shell `rg` is an alternative. |
| Edit / create | `Edit(file_path, old_string, new_string)` / `Write(file_path, content)` | These are absent for review roles; writing roles are guarded. |
| Run a command | `Bash(command)` | Use the native role allowlist and hooks. Every Gradle command includes `-Pdetekt.autoCorrect=false`. |
| Ask for a decision | `AskUserQuestion(questions)` with each question's `question`, `header`, `options` and optional `multiSelect` | At most four questions per call; use accompanying free text for paths or selections not in the options. If unavailable or unsuitable, ask in chat and wait for the reply. |
| Start a named role | `Agent(subagent_type, prompt)`; `subagent_type` is `coder`, `reviewer`, `review-verifier` or `fixer` | The local definition must exist. Use a new separate context for each plan/apply run. Preserve the role's native model and effort. Do not substitute an unrestricted generic agent. |
| Wait / continue | Wait for the `Agent` result; to continue, use its returned agent identifier with `Agent(resume, prompt)` when supported | If a background run returns a task identifier, use the session's task-result tool for that identifier. Inspect its schema; do not invent a wait-tool name or infer completion from silence. |

Writing roles use [.testsys-agents/hooks/write-guard.sh](hooks/write-guard.sh); review roles use
[review-readonly-guard.sh](hooks/review-readonly-guard.sh). Local role definitions connect these through
`PreToolUse`. A matching hook runs only in a session where the client actually loads and trusts that configuration.
CLI discovery and a noninteractive run alone do not establish that trust. Do not disable a hook to finish a step;
use a permitted alternative or report the blocked check.

The writing guard is a command denylist intended to catch mistakes; it is not a complete shell or filesystem
security boundary. The review guard allowlists shell checks; build outputs and the review contract's narrowly
allowed Git operations remain side effects. Both reject hidden shell wrappers and require the Gradle property.
Role scope and the host's filesystem permissions still apply to every tool.

## Codex

The following names and parameters are available in this desktop session. A CLI or a later version can expose
the same action under a different namespace: inspect its schema before use. The TOML role loader and its
contract must be loaded in the child context, regardless of the parent having read them.

| Action | Tool and inputs | Limits |
|--------|-----------------|--------|
| Read / search | `functions.exec` → `tools.exec_command({cmd, workdir})`; use `Get-Content` in PowerShell and `rg` / `rg --files` | Reading/searching is also allowed through the shared IDEA tools. Use actual shell syntax; Bash and PowerShell quoting differ. |
| Edit / create | `functions.exec` → `tools.apply_patch(patch)` | Review roles do not edit. A shell write must obey the same scope and filesystem restrictions. |
| Run a command | `functions.exec` → `tools.exec_command({cmd, workdir, yield_time_ms, max_output_tokens})` | It returns either completion or `session_id`; poll with `tools.write_stdin({session_id, chars: ""})`. Every Gradle command includes `-Pdetekt.autoCorrect=false`. |
| Wait for composed tools | `functions.wait({cell_id})` | Use only when `functions.exec` returns a running cell ID; this differs from the shell's session ID. |
| Ask asynchronously, when exposed | `functions.request_user_input_async({questions: [{title, options?}]})` | `title` is a complete question; optional `options` is an array of strings. The tool returns immediately and the actual reply arrives as a new message; a preselected option is not submitted automatically. It supports required decisions and plan approval in sessions exposing this schema. Wait for the actual reply. |
| Ask through the blocking tool, when permitted | `functions.request_user_input({questions})` | This schema limits it to three short questions, each with `id`, `header`, `question` and `options[{label,description}]`; no multiselect. It is unavailable outside its supported mode and cannot request permission here. Use a normal chat question and wait for required decisions/approval; silence is not an answer. |
| Start a named role | `collaboration.spawn_agent({task_name, agent_type, message})` | Set `agent_type` to the named role, pass its full input, and keep inherited model settings. For a role needing a fresh context, set `fork_turns: "none"` and include all required data in `message`. Role availability/permissions are checked by the client. |
| Wait / continue | `collaboration.wait_agent({timeout_ms})`, `collaboration.send_message({target, message})`, `collaboration.followup_task({target, message})` | Wait notifications are not result text; inspect delivered messages. `followup_task` starts a turn for an idle child; `send_message` alone does not. These are direct tools, never methods of `functions.exec`'s `tools` object. |

Some sessions instead expose `spawn_agent`, `wait_agent`, `send_message` and a child identifier directly. Use their
actual schemas and the installed named role, not guessed parameter names. If no named separate-context role is
available, report that limitation; running the whole procedure in the parent is not an equivalent replacement.

Codex discovers local `.agents/skills/<skill>` links to the [shared skills](skills/) and reads the root [AGENTS.md](../AGENTS.md). Its local
`.codex/agents/*.toml` preserves the role's effort and inherits the model. Those definitions have no Claude
`tools`, `hooks` or `PreToolUse` fields. Restrictions come from the loaded role contract, session tool permissions,
sandbox and approval checks; this setup does not provide Claude's shell allowlist as a Codex hook. Do not change
the sandbox, global model or permission settings to imitate another client. Report blocked checks honestly.

## Shared IDEA tools

Use these only when the current session exposes the corresponding `mcp__idea__*` schema. Pass `projectPath`
with the absolute repository root on every call. In Codex they are methods of `functions.exec`'s `tools` object;
in Claude they are native MCP tools allowed by the local role definition.

| Action | Tool and checked inputs |
|--------|-------------------------|
| Read a file | `mcp__idea__read_file({file_path, offset?, limit?, projectPath})`: offset is 1-based; limit at most 5000. |
| Search symbols | `mcp__idea__search_symbol({q, paths?, limit?, include_external?, projectPath})`. |
| Search text / regex / filenames | `mcp__idea__search_text`, `search_regex`, `search_file` with `{q, paths?, limit?, projectPath}`; filename search additionally supports `includeExcluded`. |
| Inspect a declaration | `mcp__idea__get_symbol_info({filePath, line, column, projectPath})`: line and column are 1-based. |
| Inspect calls | `mcp__idea__analyze_calls({symbolFqn, analysisKind, projectPath, depth?, maxChildren?, maxNodes?, treePath?, childOffset?, timeout?})`: use `INCOMING_CALLS` or `OUTGOING_CALLS`, and the returned exact signature/tree path for ambiguous targets or paging. |
| Diagnostics | `mcp__idea__get_file_problems({filePath, errorsOnly?, timeout?, projectPath})`. |
| Batch inspections | `mcp__idea__lint_files({files, min_severity?, timeout?, projectPath})`: severity is `warning` or `error`; report incomplete, timed-out or unsupported files. |

File/text search can fall back to `rg`, and reading to the shell. Text search does not reproduce the IDE's call
hierarchy, symbol resolution or compiler inspections: record those checks as unavailable rather than claiming
equivalence. Build through the project's documented command when an IDE build cannot express the mandatory
Gradle property. Database, debugger, refactoring, formatting and terminal-execution IDEA tools are outside these
role mappings; an available MCP tool is not automatically permitted by a role.

When `JAVA_HOME` is unset, set it to the installed JDK 21 path in the same command as Gradle, using the current shell's syntax. For Bash use `export JAVA_HOME=<jdk-21-path> && ./gradlew <tasks> -Pdetekt.autoCorrect=false`; for PowerShell use `$env:JAVA_HOME='<jdk-21-path>'; ./gradlew.bat <tasks> -Pdetekt.autoCorrect=false`.

## Adding a client

Add a section for the requested client using its actual session tool schemas. Map every action required by the
shared workflows: file reading/search/editing, commands and scripts, user decisions, named roles in separate
contexts, waiting and continuation, and available IDEA actions. Include checked tool names and parameters,
availability conditions, permitted alternatives and the limits of those alternatives.

Add the corresponding native bootstrap/setup section to [testsys-llm-sync.md](../testsys-llm-sync.md). It must
load root `AGENTS.md`, the current mapping and the correct shared role independently in each separate context.
Document the client's actual permissions and enforcement; a mapping is not a hook or an equivalent sandbox.
If a mandatory action lacks an available equivalent, report the limitation instead of guessing a capability
or skipping the step. Shared skills and role procedures remain unchanged when another client is added.
