#!/usr/bin/env bash
# PreToolUse guard for the writing agents (fixer, coder).
#
# Contract: reads the hook JSON from stdin, exits 0 to allow the call and 2 to block it (stderr is shown to the
# agent). Claude Code does NOT block on any other exit code or on a timeout, so every unexpected condition must end
# in `exit 2` (fail closed).
#
# Policy: a writing agent may edit files in the working tree and run builds, but must not change git state, write to
# GitHub, delete trees or touch its own guard. This is a denylist that protects against mistakes, not a security
# boundary: command wrappers (`bash -c`, `eval`, `xargs`, `env`, ...) and command substitution are blocked because
# they would hide the real command from the checks below.
#
# Edit/Write: blocked only for `.claude/hooks/` and `.claude/settings*`.
# Bash: per segment (split on `&&`, `||`, `;`, `|`, `&`, newlines) — git only read-only subcommands, gh only reads,
# no recursive rm, no `find -delete/-exec`, Gradle only with -Pdetekt.autoCorrect=false.

trap 'echo "write guard: internal error, call blocked" >&2; exit 2' ERR
set -o pipefail

block() {
    echo "write guard: blocked - $1. See section \"Limits\" of your agent file." >&2
    exit 2
}

input=$(cat | tr -d '\r') || block "cannot read hook input"

tool=$(printf '%s' "$input" | sed -nE 's/.*"tool_name"[[:space:]]*:[[:space:]]*"([^"]*)".*/\1/p' | head -n 1)
[ -n "$tool" ] || block "cannot parse the tool name"

protected_path() { # $1 = text; succeeds if it mentions a protected Claude path
    printf '%s' "$1" | grep -qiE '\.claude([\\/]+)(hooks|settings)'
}

case "$tool" in
    Edit|Write|MultiEdit|NotebookEdit)
        path=$(printf '%s' "$input" | sed -nE 's/.*"(file_path|notebook_path)"[[:space:]]*:[[:space:]]*"(([^"\\]|\\.)*)".*/\2/p' | head -n 1)
        [ -n "$path" ] || block "cannot parse the file path"
        protected_path "$path" && block "writing agents must not modify .claude/hooks or .claude/settings"
        exit 0 ;;
    Bash) ;;
    *) block "tool '$tool' is not allowed for writing agents" ;;
esac

raw=$(printf '%s' "$input" | sed -nE 's/.*"command"[[:space:]]*:[[:space:]]*"(([^"\\]|\\.)*)".*/\1/p' | head -n 1)
[ -n "$raw" ] || block "cannot parse the command"

case "$raw" in
    *'\u'*) block "unicode escapes in the command are not allowed" ;;
esac

# Decode JSON string escapes. `\\` goes through a placeholder so that `\\n` stays a backslash followed by `n`.
cmd=$(printf '%s' "$raw" | sed -e 's/\\\\/\x01/g' -e 's/\\"/"/g' -e 's/\\\//\//g' -e 's/\\t/\t/g' -e 's/\\n/\n/g' -e 's/\x01/\\/g')

case "$cmd" in
    *'`'*|*'$('*|*'<('*|*'>('*) block "command and process substitution are not allowed; run the inner command separately" ;;
esac

protected_path "$cmd" && block "Bash commands must not reference .claude/hooks or .claude/settings"

segments=$(printf '%s' "$cmd" | sed -E -e 's/&&|\|\||;|\||&/\n/g')

has_token() { # $1 = token, rest = tokens
    local needle=$1; shift
    local t
    for t in "$@"; do [ "$t" = "$needle" ] && return 0; done
    return 1
}

check_git() {
    shift # git
    while [ $# -gt 0 ]; do
        case "$1" in
            -C) shift 2 ;;
            --no-pager) shift ;;
            -*) block "git global option '$1' is not allowed" ;;
            *) break ;;
        esac
    done
    local sub=${1:-}
    [ -n "$sub" ] || block "bare git is not allowed"
    shift
    local t
    for t in "$@"; do
        case "$t" in --output*|--ext-diff) block "git option '$t' is not allowed" ;; esac
    done
    case "$sub" in
        status|diff|log|show|blame|rev-parse|ls-files|ls-tree|merge-base|cat-file|describe|shortlog|grep|rev-list|name-rev) return 0 ;;
        branch)
            for t in "$@"; do
                case "$t" in
                    --show-current|--list|-a|--all|-r|--remotes|-v|-vv|--contains|--merged|--no-merged) ;;
                    -*) block "git branch option '$t' is not allowed" ;;
                    *) ;;
                esac
            done
            return 0 ;;
        stash)
            case "${1:-}" in list|show) return 0 ;; esac
            block "git stash '${1:-}' is not allowed" ;;
        remote)
            case "${1:-}" in ""|-v|show|get-url) return 0 ;; esac
            block "git remote '${1:-}' is not allowed" ;;
        *) block "git $sub changes or may change repository state; writing agents only edit the working tree" ;;
    esac
}

check_gh() {
    shift # gh
    local group=${1:-} action=${2:-}
    case "$group $action" in
        "pr view"|"pr diff"|"pr checks"|"pr list"|"repo view"|"run view"|"run list") return 0 ;;
        "api "*)
            local t
            for t in "$@"; do
                case "$t" in
                    -X|--method|-f|-F|--field|--raw-field|--input|-X*|--method=*) block "gh api option '$t' is not allowed (GET only)" ;;
                esac
            done
            return 0 ;;
    esac
    block "gh $group $action is not allowed; writing agents never write to GitHub"
}

while IFS= read -r segment; do
    segment=$(printf '%s' "$segment" | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//')
    [ -n "$segment" ] || continue

    read -r -a tokens <<< "$segment"
    # Normalise `"/usr/bin/git"` to `git` so paths and quotes do not hide the command.
    first=$(printf '%s' "${tokens[0]}" | tr -d "\"'")
    first=${first##*/}
    first=${first##*\\}
    case "$first" in *.exe) first=${first%.exe} ;; esac

    case "$first" in
        git)
            check_git "${tokens[@]}" ;;
        gh)
            check_gh "${tokens[@]}" ;;
        gradlew|gradlew.bat|gradle)
            has_token "-Pdetekt.autoCorrect=false" "${tokens[@]}" \
                || block "every Gradle call must pass -Pdetekt.autoCorrect=false, otherwise Detekt rewrites unrelated sources" ;;
        rm)
            for t in "${tokens[@]:1}"; do
                case "$t" in
                    --recursive|--dir) block "recursive rm is not allowed; delete single files only" ;;
                    --*) ;;
                    -*[rRd]*) block "recursive rm is not allowed; delete single files only" ;;
                esac
            done ;;
        rmdir)
            block "rmdir is not allowed" ;;
        find)
            for t in "${tokens[@]}"; do
                case "$t" in -delete|-exec|-execdir|-ok|-okdir) block "find action '$t' is not allowed" ;; esac
            done ;;
        python|python3|py)
            case "${tokens[1]:-}" in
                scripts/check_kdoc.py|*/scripts/check_kdoc.py) ;;
                *) block "python is allowed only for the KDoc checker scripts/check_kdoc.py" ;;
            esac ;;
        bash|sh|zsh|dash|eval|exec|source|.|env|xargs|command|builtin|nohup|time|timeout|sudo|su|powershell|pwsh|cmd|wsl|perl|ruby|node)
            block "'$first' can hide the real command and is not allowed" ;;
        curl|wget|scp|ssh|rsync)
            block "network command '$first' is not allowed" ;;
        *)
            ;;
    esac
done <<< "$segments"

exit 0
