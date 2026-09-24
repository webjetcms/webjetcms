#!/bin/sh
cd ..

# Check if on main branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "release/2026.18" ]; then
    echo "ERROR: You must be on 'release/2026.18' branch. Current branch: $CURRENT_BRANCH"
    exit 1
fi

# Fetch and check if main is up to date with origin
git fetch origin release/2026.18
LOCAL=$(git rev-parse release/2026.18)
REMOTE=$(git rev-parse origin/release/2026.18)
if [ "$LOCAL" != "$REMOTE" ]; then
    echo "ERROR: Local 'release/2026.18' is not up to date with origin/release/2026.18. Please push/pull first."
    exit 1
fi

echo "--> Pulling hotfix/2026.0-main from origin"
git fetch origin hotfix/2026.0-main

echo "--> Merging origin/hotfix/2026.0-main into release/2026.18"
if ! git merge origin/hotfix/2026.0-main -m "Merge hotfix/2026.0-main into release/2026.18"; then
    echo "---> WARNING: Merge conflict detected. Resolve conflicts in VS Code, then commit."
    exit 1
fi
