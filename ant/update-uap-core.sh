#!/usr/bin/env bash

set -euo pipefail

UPSTREAM_REPOSITORY="https://github.com/ua-parser/uap-core.git"
UPSTREAM_RAW_URL="https://raw.githubusercontent.com/ua-parser/uap-core"
UPSTREAM_REF="${1:-refs/heads/master}"
REGEXES_FILE="src/main/resources/ua-parser/regexes-webjet.yaml"
COMMIT_FILE="src/main/resources/ua-parser/upstream-commit.txt"
TEMP_FILE="$(mktemp "${TMPDIR:-/tmp}/webjet-uap-core.XXXXXX")"

cleanup() {
    rm -f "$TEMP_FILE"
}
trap cleanup EXIT

if [[ "$UPSTREAM_REF" =~ ^[0-9a-fA-F]{40}$ ]]; then
    UPSTREAM_COMMIT="$(printf '%s' "$UPSTREAM_REF" | tr '[:upper:]' '[:lower:]')"
else
    UPSTREAM_COMMIT="$(git ls-remote "$UPSTREAM_REPOSITORY" "$UPSTREAM_REF" | awk 'NR == 1 { print $1 }')"
fi

if [[ ! "$UPSTREAM_COMMIT" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Failed to resolve uap-core ref: $UPSTREAM_REF" >&2
    exit 1
fi

echo "Downloading uap-core regexes.yaml from commit $UPSTREAM_COMMIT"
curl --fail --location --silent --show-error --retry 3 \
    "$UPSTREAM_RAW_URL/$UPSTREAM_COMMIT/regexes.yaml" \
    --output "$TEMP_FILE"

for section in user_agent_parsers os_parsers device_parsers; do
    if ! grep -q "^${section}:$" "$TEMP_FILE"; then
        echo "Downloaded file does not contain the required ${section} section" >&2
        exit 1
    fi
done

if (( $(wc -c < "$TEMP_FILE") < 100000 )); then
    echo "Downloaded regexes.yaml is unexpectedly small" >&2
    exit 1
fi

if cmp -s "$TEMP_FILE" "$REGEXES_FILE"; then
    echo "regexes-webjet.yaml is already up to date"
else
    cp "$TEMP_FILE" "$REGEXES_FILE"
    echo "Updated $REGEXES_FILE"
fi

printf '%s\n' "$UPSTREAM_COMMIT" > "$COMMIT_FILE"
echo "Recorded upstream commit in $COMMIT_FILE"
