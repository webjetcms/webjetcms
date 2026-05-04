#!/bin/sh
cd ..

# Check if on main branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "ERROR: You must be on 'main' branch. Current branch: $CURRENT_BRANCH"
    exit 1
fi

# Fetch and check if main is up to date with origin
git fetch origin main
LOCAL=$(git rev-parse main)
REMOTE=$(git rev-parse origin/main)
if [ "$LOCAL" != "$REMOTE" ]; then
    echo "ERROR: Local 'main' is not up to date with origin/main. Please push/pull first."
    exit 1
fi

echo "--> Pulling hotfix/2026.0-main from origin"
git fetch origin hotfix/2026.0-main

echo "--> Merging origin/hotfix/2026.0-main into main"
if ! git merge origin/hotfix/2026.0-main -m "Merge hotfix/2026.0-main into main"; then
    echo "---> WARNING: Merge conflict detected. Resolve conflicts in VS Code, then commit."
    exit 1
fi
