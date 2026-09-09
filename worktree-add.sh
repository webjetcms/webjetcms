#!/bin/bash
# Creates or checks out a branch worktree, prepares it, and opens it in VS Code.
#
# Usage examples:
#   ./worktree-add.sh feature/ckeditor-thumb-restrictions
#   ./worktree-add.sh --new feature/ckeditor-thumb-restrictions

set -euo pipefail

START_TIME=$SECONDS

print_elapsed_time() {
    local elapsed=$((SECONDS - START_TIME))
    printf '\nElapsed time: %02d:%02d:%02d\n' \
        $((elapsed / 3600)) \
        $(((elapsed % 3600) / 60)) \
        $((elapsed % 60))
}

trap print_elapsed_time EXIT

CREATE_NEW=false
if [[ "${1:-}" == "--new" ]]; then
    CREATE_NEW=true
    shift
fi

if (( $# > 1 )); then
    echo "Usage: $0 [--new] [branch-name]" >&2
    exit 1
fi

BRANCH_NAME="${1:-}"
if [[ -z "$BRANCH_NAME" ]]; then
    read -r -p "Branch name (for example feature/ckeditor-thumb-restrictions): " BRANCH_NAME
fi

if ! git check-ref-format --branch "$BRANCH_NAME" >/dev/null 2>&1; then
    echo "Invalid branch name: $BRANCH_NAME" >&2
    exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"
REPO_PARENT="$(dirname -- "$REPO_ROOT")"
LOCAL_FILES_SOURCE="$REPO_PARENT/webjetcms"
WORKTREE_NAME="${BRANCH_NAME//\//-}"
WORKTREE_PATH="$REPO_PARENT/webjetcms-$WORKTREE_NAME"

if [[ ! -d "$LOCAL_FILES_SOURCE" ]]; then
    echo "Local files source does not exist: $LOCAL_FILES_SOURCE" >&2
    exit 1
fi

if [[ -e "$WORKTREE_PATH" ]]; then
    echo "Target path already exists: $WORKTREE_PATH" >&2
    exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
    echo "npm is not available in PATH." >&2
    exit 1
fi

if ! command -v code >/dev/null 2>&1; then
    echo "VS Code command 'code' is not available in PATH." >&2
    exit 1
fi

if [[ "$CREATE_NEW" == true ]]; then
    if git -C "$REPO_ROOT" show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
        echo "Branch already exists locally. Run without --new to use it: $BRANCH_NAME" >&2
        exit 1
    fi

    echo "Fetching branches from origin"
    git -C "$REPO_ROOT" fetch origin

    if git -C "$REPO_ROOT" show-ref --verify --quiet "refs/remotes/origin/$BRANCH_NAME"; then
        echo "Branch already exists on origin. Run without --new to use it: $BRANCH_NAME" >&2
        exit 1
    fi

    if ! git -C "$REPO_ROOT" show-ref --verify --quiet "refs/remotes/origin/main"; then
        echo "Base branch origin/main does not exist." >&2
        exit 1
    fi

    echo "Creating new branch $BRANCH_NAME from origin/main in $WORKTREE_PATH"
    git -C "$REPO_ROOT" worktree add -b "$BRANCH_NAME" "$WORKTREE_PATH" origin/main
elif git -C "$REPO_ROOT" show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
    echo "Using local branch $BRANCH_NAME in $WORKTREE_PATH"
    git -C "$REPO_ROOT" worktree add "$WORKTREE_PATH" "$BRANCH_NAME"
else
    echo "Branch is not available locally; fetching branches from origin"
    git -C "$REPO_ROOT" fetch origin

    if ! git -C "$REPO_ROOT" show-ref --verify --quiet "refs/remotes/origin/$BRANCH_NAME"; then
        echo "Branch does not exist locally or on origin: $BRANCH_NAME" >&2
        echo "Use --new to create it." >&2
        exit 1
    fi

    echo "Creating local tracking branch for origin/$BRANCH_NAME in $WORKTREE_PATH"
    git -C "$REPO_ROOT" worktree add --track -b "$BRANCH_NAME" \
        "$WORKTREE_PATH" "origin/$BRANCH_NAME"
fi

echo "Copying local configuration and plugin files"
cp "$LOCAL_FILES_SOURCE"/src/main/resources/*.xml "$WORKTREE_PATH/src/main/resources/"
cp "$LOCAL_FILES_SOURCE"/src/main/webapp/admin/v9/src/js/plugins/*.zip \
    "$WORKTREE_PATH/src/main/webapp/admin/v9/src/js/plugins/"
mkdir -p "$WORKTREE_PATH/src/main/webapp/WEB-INF/fonts"
cp "$LOCAL_FILES_SOURCE"/src/main/webapp/WEB-INF/fonts/* \
    "$WORKTREE_PATH/src/main/webapp/WEB-INF/fonts/"

echo "Installing admin dependencies and building production assets"
npm --prefix "$WORKTREE_PATH/src/main/webapp/admin/v9" install
npm --prefix "$WORKTREE_PATH/src/main/webapp/admin/v9" run prod

echo "Installing test dependencies"
npm --prefix "$WORKTREE_PATH/src/test/webapp" install

echo "Installing documentation dependencies"
npm --prefix "$WORKTREE_PATH/docs" install

echo "Opening the new worktree in VS Code"
(
    cd -- "$WORKTREE_PATH"
    code .
)
