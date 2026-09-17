#!/usr/bin/env bash
# PreToolUse guard for the read-only review agents (reviewer, review-verifier).
#
# Contract: reads the hook JSON from stdin, exits 0 to allow the Bash call and 2 to block it (stderr is shown
# to the agent). Claude Code does NOT block on any other exit code or on a timeout, so every unexpected
# condition must end in `exit 2` (fail closed).
#
# Policy: allowlist. A command is allowed only if every segment (split on `&&`, `||`, `;`, `|`, `&`, newlines)
# starts with a known read-only command and passes that command's argument checks. No output redirection
# (except to /dev/null or between descriptors), no command substitution, no in-place editing.
# Gradle is allowed only with -Pdetekt.autoCorrect=false, because Detekt otherwise rewrites sources.

trap 'echo "review guard: internal error, command blocked" >&2; exit 2' ERR
set -o pipefail

block() {
    echo "review guard: blocked - $1. The review agents are read-only; see .claude/agents/reviewer.md, section \"Read-only contract\"." >&2
    exit 2
}

input=$(cat | tr -d '\r') || block "cannot read hook input"

tool=$(printf '%s' "$input" | sed -nE 's/.*"tool_name"[[:space:]]*:[[:space:]]*"([^"]*)".*/\1/p' | head -n 1)
[ "$tool" = "Bash" ] || block "tool '$tool' is not allowed for review agents"

raw=$(printf '%s' "$input" | sed -nE 's/.*"command"[[:space:]]*:[[:space:]]*"(([^"\\]|\\.)*)".*/\1/p' | head -n 1)
[ -n "$raw" ] || block "cannot parse the command"

case "$raw" in
    *'\u'*) block "unicode escapes in the command are not allowed" ;;
esac

# Decode JSON string escapes. `\\` goes through a placeholder so that `\\n` stays a backslash followed by `n`.
cmd=$(printf '%s' "$raw" | sed -e 's/\\\\/\x01/g' -e 's/\\"/"/g' -e 's/\\\//\//g' -e 's/\\t/\t/g' -e 's/\\n/\n/g' -e 's/\x01/\\/g')

case "$cmd" in
    *'`'*|*'$('*) block "command substitution is not allowed; run the inner command separately" ;;
esac

# Drop harmless redirections, then any remaining '>' is a write.
stripped=$(printf '%s' "$cmd" | sed -E -e 's/[0-9]*>&[0-9-]+//g' -e 's#[0-9&]*>[[:space:]]*/dev/null##g')
case "$stripped" in
    *'>'*) block "output redirection is not allowed" ;;
esac

segments=$(printf '%s' "$stripped" | sed -E -e 's/&&|\|\||;|\||&/\n/g')

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
            -c|--exec-path*|--git-dir*|--work-tree*) block "git global option '$1' is not allowed" ;;
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
        status|diff|log|show|blame|rev-parse|ls-files|ls-tree|merge-base|cat-file|describe|shortlog|grep|fetch|rev-list|name-rev) return 0 ;;
        branch)
            for t in "$@"; do
                case "$t" in
                    --show-current|--list|-a|--all|-r|--remotes|-v|-vv|--contains|--merged|--no-merged) ;;
                    -*) block "git branch option '$t' is not allowed" ;;
                    *) ;; # arguments of --contains/--merged or list patterns
                esac
            done
            return 0 ;;
        worktree)
            case "${1:-}" in add|list|remove|prune) return 0 ;; esac
            block "git worktree '${1:-}' is not allowed" ;;
        stash)
            case "${1:-}" in list|show) return 0 ;; esac
            block "git stash '${1:-}' is not allowed" ;;
        remote)
            case "${1:-}" in ""|-v|show|get-url) return 0 ;; esac
            block "git remote '${1:-}' is not allowed" ;;
        *) block "git $sub is not allowed" ;;
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
                    -X|--method|-f|-F|--field|--raw-field|--input|-X*|--method=*) block "gh api option '$t' is not allowed (read-only GET only)" ;;
                esac
            done
            return 0 ;;
    esac
    block "gh $group $action is not allowed"
}

while IFS= read -r segment; do
    # Trim.
    segment=$(printf '%s' "$segment" | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//')
    [ -n "$segment" ] || continue

    read -r -a tokens <<< "$segment"
    first=${tokens[0]}

    case "$first" in
        cd|pwd|ls|cat|head|tail|wc|grep|rg|uniq|diff|cut|tr|nl|stat|file|echo|printf|true|test|basename|dirname|realpath|which|date)
            ;;
        export)
            [ "${#tokens[@]}" -eq 2 ] && case "${tokens[1]}" in JAVA_HOME=*) true ;; *) false ;; esac \
                || block "only 'export JAVA_HOME=<path>' is allowed" ;;
        sort)
            for t in "${tokens[@]}"; do
                case "$t" in -o|-o*|--output*) block "sort output files are not allowed" ;; esac
            done ;;
        find)
            for t in "${tokens[@]}"; do
                case "$t" in -delete|-exec|-execdir|-ok|-okdir|-fprint|-fprint0|-fprintf|-fls) block "find action '$t' is not allowed" ;; esac
            done ;;
        sed)
            printf '%s' "$segment" | grep -qE "^sed -n ['\"]?[0-9\$]+(,[0-9\$]+)?p['\"]?([[:space:]]|$)" \
                || block "only 'sed -n <range>p <file>' is allowed" ;;
        git)
            check_git "${tokens[@]}" ;;
        gh)
            check_gh "${tokens[@]}" ;;
        ./gradlew|gradlew|./gradlew.bat|gradlew.bat)
            has_token "-Pdetekt.autoCorrect=false" "${tokens[@]}" \
                || block "every Gradle call must pass -Pdetekt.autoCorrect=false, otherwise Detekt rewrites sources" ;;
        python|python3|py)
            case "${tokens[1]:-}" in
                scripts/check_kdoc.py|*/scripts/check_kdoc.py) ;;
                *) block "python is allowed only for the KDoc checker" ;;
            esac ;;
        *)
            block "command '$first' is not on the read-only allowlist" ;;
    esac
done <<< "$segments"

exit 0
