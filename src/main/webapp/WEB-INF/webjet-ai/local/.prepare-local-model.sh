#!/usr/bin/env bash

set -euo pipefail

if (( $# < 1 || $# > 2 )); then
    echo "Usage: $0 GRADLE_TASK [--overwrite]" >&2
    exit 2
fi

GRADLE_TASK="$1"
OVERWRITE_OPTION="${2:-}"

if [[ -n "${OVERWRITE_OPTION}" && "${OVERWRITE_OPTION}" != "--overwrite" ]]; then
    echo "Only the optional --overwrite argument is supported." >&2
    exit 2
fi

SCRIPT_DIRECTORY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIRECTORY}/../../../../../.." && pwd)"

if [[ -n "${OVERWRITE_OPTION}" ]]; then
    exec "${PROJECT_ROOT}/gradlew" \
        --project-dir "${PROJECT_ROOT}" \
        -PoverwriteLocalAiModel=true \
        "${GRADLE_TASK}"
fi

exec "${PROJECT_ROOT}/gradlew" \
    --project-dir "${PROJECT_ROOT}" \
    "${GRADLE_TASK}"
