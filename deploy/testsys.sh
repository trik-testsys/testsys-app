#!/bin/sh
# Starts, stops and cleans the Docker Compose environments of TestSys. See docs/project/deployment.md.
# Runs in sh on Linux and in Git Bash on Windows from any directory; Compose paths are relative to deploy/.
# Globbing is off: service names are passed to Compose unquoted.
set -euf

usage() {
  cat >&2 <<'EOF'
Usage: sh deploy/testsys.sh <environment> <command> [options] [<service>...]
Environments:
  dev, e2e     local environments; variables from deploy/env/<environment>.env and <environment>.local.env
  prod         production; variables from the file given by --env-file or TESTSYS_ENV_FILE
  node         separate grading node; variables from the file given by --env-file or TESTSYS_ENV_FILE
  dev and prod also run Prometheus and Grafana; every command includes them
Commands:
  up [--node] [--no-build]           build the application image and start the environment
  down [--node]                      stop and remove the containers, keep the volumes
  clean [--rmi]                      dev and e2e only: also remove the volumes and the node workspace contents
  logs [--node] [-f|--follow] [<service>...]
  ps [--node] [<service>...]
  config [--node]                    print the resolved Compose model, including secrets
Options:
  --node             add the grading node to the project of dev, e2e or prod
  --no-build         start the existing application image without building it
  --rmi              also remove the application image built for the environment
  --env-file <path>  variable file of prod or node, kept outside the Git checkout
  -h, --help         show this help
EOF
  exit "${1:-2}"
}

fail() {
  echo "$*" >&2
  exit 1
}

usage_error() {
  echo "$*" >&2
  usage 2
}

for argument in "$@"; do
  case "$argument" in
    -h | --help) usage 0 ;;
  esac
done

[ $# -ge 2 ] || usage
environment=$1
command=$2
shift 2

case "$environment" in
  dev | e2e | prod | node) ;;
  *) usage_error "Unknown environment '$environment'" ;;
esac
case "$command" in
  up | down | clean | logs | ps | config) ;;
  *) usage_error "Unknown command '$command'" ;;
esac

with_node=no
no_build=no
remove_image=no
follow=no
env_file_option=
env_file_given=no
services=

option_error() {
  usage_error "The option $1 is not accepted by '$environment $command'"
}

while [ $# -gt 0 ]; do
  case "$1" in
    --node)
      case "$command" in up | down | logs | ps | config) ;; *) option_error "$1" ;; esac
      with_node=yes
      ;;
    --no-build)
      [ "$command" = up ] || option_error "$1"
      no_build=yes
      ;;
    --rmi)
      [ "$command" = clean ] || option_error "$1"
      remove_image=yes
      ;;
    -f | --follow)
      [ "$command" = logs ] || option_error "$1"
      follow=yes
      ;;
    --env-file | --env-file=*)
      [ "$env_file_given" = no ] || usage_error "The option --env-file is given more than once"
      if [ "$1" = --env-file ]; then
        [ $# -ge 2 ] || usage_error "The option --env-file needs a path"
        shift
        env_file_option=$1
      else
        env_file_option=${1#--env-file=}
      fi
      [ -n "$env_file_option" ] || usage_error "The option --env-file needs a path"
      env_file_given=yes
      ;;
    -*) usage_error "Unknown option '$1'" ;;
    *)
      case "$command" in logs | ps) ;; *) usage_error "The command '$command' takes no service names, got '$1'" ;; esac
      services="$services $1"
      ;;
  esac
  shift
done

case "$environment" in
  prod | node)
    if [ "$command" = clean ]; then
      fail "The command 'clean' is not available for '$environment': it removes volumes and workspaces of dev and e2e only." \
        "Use '$environment down' to stop the environment."
    fi
    ;;
  dev | e2e)
    [ "$env_file_given" = no ] ||
      usage_error "The option --env-file is not accepted by '$environment': it reads deploy/env/$environment.env and $environment.local.env"
    ;;
esac
if [ "$environment" = node ]; then
  [ "$with_node" = no ] || usage_error "The option --node is not accepted by 'node': the grading node is the whole project"
  [ "$no_build" = no ] || usage_error "The option --no-build is not accepted by 'node': the project builds no images"
fi

# The variable file of prod and node lives outside the checkout. A relative path is resolved from the directory
# of the call, before the script changes the directory.
env_file=
case "$environment" in
  prod | node)
    if [ "$env_file_given" = yes ]; then
      env_file=$env_file_option
    else
      env_file=${TESTSYS_ENV_FILE-}
    fi
    template=prod.env.example
    [ "$environment" = prod ] || template=grading-node.env.example
    if [ -z "$env_file" ]; then
      fail "The '$environment' environment needs its variable file. Copy deploy/env/$template to a directory" \
        "outside the Git checkout, fill it in and pass its path with --env-file <path> or TESTSYS_ENV_FILE."
    fi
    case "$env_file" in
      /* | [A-Za-z]:[\\/]*) ;;
      *) env_file="$(pwd)/$env_file" ;;
    esac
    if [ ! -f "$env_file" ]; then
      fail "The variable file '$env_file' does not exist. Copy deploy/env/$template to a directory outside" \
        "the Git checkout, fill it in and pass its path with --env-file <path> or TESTSYS_ENV_FILE."
    fi
    ;;
esac

cd "$(dirname "$0")"

# Git Bash converts shell variables that look like POSIX paths into Windows paths before starting docker;
# paths of the Docker daemon, such as TESTSYS_GRADING_NODE_WORKSPACE, must reach Compose unchanged.
MSYS2_ENV_CONV_EXCL=TESTSYS_
export MSYS2_ENV_CONV_EXCL

case "$environment" in
  node) project=testsys-grading-node ;;
  *) project=testsys-$environment ;;
esac

# Dev and e2e read the tracked file and then the local file of the machine; a later file overrides an earlier one.
local_env_file=
case "$environment" in
  dev | e2e)
    env_file=env/$environment.env
    if [ -f "env/$environment.local.env" ]; then
      local_env_file=env/$environment.local.env
    fi
    ;;
esac

# Prometheus and Grafana belong to dev and prod. Their file is passed to every command of these environments, so
# "down --volumes" also removes their volumes and "--remove-orphans" never removes their containers.
monitoring_file=
case "$environment" in
  dev | prod) monitoring_file=compose.monitoring.yml ;;
esac

# Runs Compose with the project name, variable files and Compose files of the environment. The override of the
# environment follows the shared files; further -f options given to this function, such as the node file, come last.
compose() {
  if [ "$environment" = node ]; then
    docker compose -p "$project" --env-file "$env_file" -f compose.grading-node.yml "$@"
    return
  fi
  if [ -n "$monitoring_file" ]; then
    set -- -f "$monitoring_file" -f "compose.$environment.yml" "$@"
  else
    set -- -f "compose.$environment.yml" "$@"
  fi
  if [ -n "$local_env_file" ]; then
    docker compose -p "$project" --env-file "$env_file" --env-file "$local_env_file" -f compose.yml "$@"
  else
    docker compose -p "$project" --env-file "$env_file" -f compose.yml "$@"
  fi
}

node_file=
if [ "$with_node" = yes ]; then
  node_file=compose.grading-node.yml
fi

compose_stack() {
  if [ -n "$node_file" ]; then
    compose -f "$node_file" "$@"
  else
    compose "$@"
  fi
}

# Reads TESTSYS_GRADING_NODE_WORKSPACE as Compose sees it and checks it. Prints nothing if it is not set.
node_workspace_root() {
  root=$(compose_stack config --environment | sed -n 's/^TESTSYS_GRADING_NODE_WORKSPACE=//p')
  case "$root" in
    '') ;;
    *,*) fail "TESTSYS_GRADING_NODE_WORKSPACE must not contain commas, got '$root'" ;;
    *[[:space:]]*) fail "TESTSYS_GRADING_NODE_WORKSPACE must not contain whitespace, got '$root'" ;;
    /*) printf '%s\n' "${root%/}" ;;
    *) fail "TESTSYS_GRADING_NODE_WORKSPACE must be an absolute path, got '$root'" ;;
  esac
}

# Runs a command in a throwaway container with the directory of the Docker daemon mounted as /workspace.
# A bind mount of a missing directory fails instead of creating it.
in_workspace() {
  source_directory=$1
  entrypoint=$2
  shift 2
  MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*' docker run --rm \
    --mount "type=bind,source=$source_directory,target=/workspace" \
    --entrypoint "$entrypoint" postgres:17-alpine "$@"
}

# Creates the workspace subdirectory of the node of this project in the existing workspace directory.
create_node_workspace() {
  # Reports a missing tag or workspace with the message of Compose.
  compose_stack config --quiet
  root=$(node_workspace_root)
  [ -n "$root" ] || fail "TESTSYS_GRADING_NODE_WORKSPACE is not set"
  echo "Creating the grading node workspace $root/$project"
  if ! in_workspace "$root" mkdir -p "/workspace/$project"; then
    fail "Could not create $root/$project, see the error above; the directory $root must exist"
  fi
}

# Prints the image name of the application service. "config --images app" also lists the images of the services
# it depends on, so the images of postgres are left out.
app_image() {
  dependency_images=$(compose_stack config --images postgres)
  compose_stack config --images app | while IFS= read -r service_image; do
    case "
$dependency_images
" in
      *"
$service_image
"*) ;;
      *) printf '%s\n' "$service_image" ;;
    esac
  done
}

run_up() {
  if [ "$environment" = node ] || [ -n "$node_file" ]; then
    create_node_workspace
  fi
  if [ "$environment" = node ]; then
    compose up -d --remove-orphans
    return
  fi

  if [ "$no_build" = yes ]; then
    # Compose would create the other containers before failing on a missing image, so the image is checked first.
    image=$(app_image)
    if ! docker image inspect "$image" >/dev/null 2>&1; then
      hint="build it with '$environment up'"
      [ "$environment" != prod ] || hint="$hint or set TESTSYS_APP_IMAGE to an existing image"
      fail "The application image '$image' does not exist; $hint"
    fi
    build_option=--no-build
  else
    build_option=--build
  fi

  case "$environment" in
    dev)
      # db-seed exits after every start, so "up --wait" would fail; its exit code is checked separately.
      compose_stack up -d "$build_option" --remove-orphans
      echo "Waiting for db-seed"
      # "wait" blocks while db-seed runs and fails if it has already exited, so the exit code is read from "ps".
      compose_stack wait db-seed >/dev/null 2>&1 || true
      # An unchanged container is restarted and keeps the output of earlier starts; only the last start is shown.
      seed_container=$(compose_stack ps -a -q db-seed)
      [ -n "$seed_container" ] || fail "The db-seed container was not created"
      seed_started=$(docker inspect --format '{{.State.StartedAt}}' "$seed_container")
      docker logs --since "$seed_started" "$seed_container"
      seed_state=$(compose_stack ps -a --format '{{.State}} {{.ExitCode}}' db-seed)
      [ "$seed_state" = "exited 0" ] || fail "db-seed did not finish successfully (state and exit code: $seed_state)"
      ;;
    *) compose_stack up -d "$build_option" --remove-orphans --wait ;;
  esac
}

run_clean() {
  root=$(node_workspace_root)

  # Without the tag or the workspace the node file cannot be used; --remove-orphans removes the node container anyway.
  if compose -f compose.grading-node.yml config --quiet 2>/dev/null; then
    node_file=compose.grading-node.yml
  fi

  echo "Removing containers, network and volumes of the project $project"
  if [ "$remove_image" = yes ]; then
    compose_stack down --volumes --remove-orphans --rmi local
  else
    compose_stack down --volumes --remove-orphans
  fi

  if [ -z "$root" ]; then
    echo "TESTSYS_GRADING_NODE_WORKSPACE is not set, the grading node workspace is not cleaned"
    return
  fi
  # The workspace is a path of the Docker daemon host and its files belong to the node, so a container removes them.
  echo "Removing the contents of the grading node workspace $root/$project"
  if ! in_workspace "$root/$project" find /workspace -mindepth 1 -delete; then
    fail "Could not clean $root/$project, see the error above; if the directory exists, remove its contents manually"
  fi
}

case "$command" in
  up) run_up ;;
  down) compose_stack down --remove-orphans ;;
  clean) run_clean ;;
  logs)
    follow_option=
    [ "$follow" = no ] || follow_option=--follow
    # shellcheck disable=SC2086 # The option and the service names are split on purpose; globbing is off.
    compose_stack logs $follow_option $services
    ;;
  ps)
    # shellcheck disable=SC2086 # The service names are split on purpose; globbing is off.
    compose_stack ps -a $services
    ;;
  config) compose_stack config ;;
esac
