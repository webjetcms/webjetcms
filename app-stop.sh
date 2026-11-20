#!/bin/bash
# Stops this checkout's Spring Boot application server using its worktree marker.
# Use --force to skip graceful shutdown and kill this checkout's marked Java process.

set -euo pipefail

if (( $# > 1 )) || [[ "${1:-}" != "" && "${1:-}" != "--force" ]]; then
    echo "Usage: $0 [--force]" >&2
    exit 1
fi

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
cd -- "$PROJECT_ROOT"
# Match build.gradle without ambiguous whitespace in process command lines.
WORKTREE_ID="$(printf '%s' "$PROJECT_ROOT" | shasum -a 256 | cut -d ' ' -f 1)"
SERVER_MARKER="-Dwebjet.worktreeId=$WORKTREE_ID"

is_target_server() {
    local executable command
    executable="$(ps -p "$1" -o comm=)" || return 1
    [[ "$executable" == */java || "$executable" == java ]] || return 1
    command="$(ps -ww -p "$1" -o args=)" || return 1
    [[ " $command " == *" $SERVER_MARKER "* ]]
}

wait_for_servers() {
    local attempt pid running
    for (( attempt = 0; attempt < $1; attempt++ )); do
        running=false
        for pid in "${SERVER_PIDS[@]}"; do
            if is_target_server "$pid"; then running=true; break; fi
        done
        if [[ "$running" == false ]]; then return 0; fi
        sleep 1
    done
    for pid in "${SERVER_PIDS[@]}"; do
        if is_target_server "$pid"; then return 1; fi
    done
}

signal_servers() {
    local pid
    for pid in "${SERVER_PIDS[@]}"; do
        # Recheck identity before each signal in case the process has exited.
        if is_target_server "$pid"; then
            echo "Sending $1 to application server $pid in $PROJECT_ROOT"
            kill "-$1" "$pid" || true
        fi
    done
}

SERVER_PIDS=()
PROCESS_LIST="$(ps -axww -o pid=,args=)"
while read -r PID COMMAND; do
    if [[ " $COMMAND " == *" $SERVER_MARKER "* ]] && is_target_server "$PID"; then
        SERVER_PIDS+=("$PID")
    fi
done <<< "$PROCESS_LIST"

if (( ${#SERVER_PIDS[@]} == 0 )); then
    echo "No application server remains for $PROJECT_ROOT"
    exit 0
fi

if [[ "${1:-}" == "--force" ]]; then
    signal_servers KILL
else
    signal_servers TERM
    if wait_for_servers 8; then exit 0; fi
    signal_servers KILL
fi

if ! wait_for_servers 5; then
    echo "Could not stop the application server in $PROJECT_ROOT" >&2
    exit 1
fi
