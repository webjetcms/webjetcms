#!/bin/sh
set -e

cd "$(dirname "$0")/.."

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" = "main" ] || [ "$CURRENT_BRANCH" = "HEAD" ]; then
    echo "ERROR: Switch to the branch you want to merge main into. Current branch: $CURRENT_BRANCH" >&2
    exit 1
fi

STATUS=$(git status --porcelain)
if [ -n "$STATUS" ]; then
    echo "ERROR: Commit or stash your local changes before merging main." >&2
    exit 1
fi

echo "--> Fetching main from origin"
git fetch origin main

echo "--> Merging origin/main into $CURRENT_BRANCH"
git merge FETCH_HEAD -m "Merge main into $CURRENT_BRANCH" || {
    MERGE_STATUS=$?
    echo "ERROR: Merge failed. Check the Git output above. If there are conflicts, resolve them in VS Code and commit, or cancel with git merge --abort." >&2
    exit "$MERGE_STATUS"
}
