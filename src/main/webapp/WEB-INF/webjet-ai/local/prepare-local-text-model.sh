#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIRECTORY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

exec "${SCRIPT_DIRECTORY}/.prepare-local-model.sh" \
    "prepareLocalAiTextModel" \
    "$@"
