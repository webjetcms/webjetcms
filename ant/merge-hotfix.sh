#!/bin/sh
cd ..

# Check if on main branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "hotfix/2026.0-main-jakarta" ]; then
    echo "ERROR: You must be on 'hotfix/2026.0-main-jakarta' branch. Current branch: $CURRENT_BRANCH"
    exit 1
fi

# Fetch and check if main is up to date with origin
git fetch origin hotfix/2026.0-main-jakarta
LOCAL=$(git rev-parse hotfix/2026.0-main-jakarta)
REMOTE=$(git rev-parse origin/hotfix/2026.0-main-jakarta)
if [ "$LOCAL" != "$REMOTE" ]; then
    echo "ERROR: Local 'hotfix/2026.0-main-jakarta' is not up to date with origin/hotfix/2026.0-main-jakarta. Please push/pull first."
    exit 1
fi

echo "--> Pulling hotfix/2026.0-main from origin"
git fetch origin hotfix/2026.0-main

echo "--> Merging origin/hotfix/2026.0-main into hotfix/2026.0-main-jakarta"
if ! git merge origin/hotfix/2026.0-main -m "Merge hotfix/2026.0-main into hotfix/2026.0-main-jakarta"; then
    echo "---> WARNING: Merge conflict detected. Resolve conflicts in VS Code, then commit."
    exit 1
fi
