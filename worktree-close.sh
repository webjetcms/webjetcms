#!/bin/bash
# Removes the current linked worktree and closes its VS Code window on macOS.

set -euo pipefail

WORKTREE_ROOT="$(git rev-parse --show-toplevel)"
COMMON_GIT_DIR="$(git rev-parse --path-format=absolute --git-common-dir)"
MAIN_WORKTREE="$(dirname -- "$COMMON_GIT_DIR")"

if [[ "$WORKTREE_ROOT" == "$MAIN_WORKTREE" ]]; then
    echo "Refusing to remove the main worktree: $WORKTREE_ROOT" >&2
    exit 1
fi

WORKTREE_STATUS="$(git -C "$WORKTREE_ROOT" status --short --untracked-files=all)"
if [[ -n "$WORKTREE_STATUS" ]]; then
    echo "The current worktree has uncommitted or untracked files:" >&2
    printf '%s\n' "$WORKTREE_STATUS" >&2
    echo "Commit, stash, or remove them before closing the worktree." >&2
    exit 1
fi

BRANCH_NAME="$(git -C "$WORKTREE_ROOT" branch --show-current)"

cd -- "$MAIN_WORKTREE"
git worktree remove "$WORKTREE_ROOT"
git worktree prune

echo "Removed worktree: $WORKTREE_ROOT"
if [[ -n "$BRANCH_NAME" ]]; then
    echo "Branch preserved: $BRANCH_NAME"
fi

if [[ "${TERM_PROGRAM:-}" == "vscode" && "$(uname -s)" == "Darwin" ]]; then
    if ! osascript \
        -e 'tell application "Visual Studio Code" to activate' \
        -e 'delay 0.2' \
        -e 'tell application "System Events" to keystroke "w" using {command down, shift down}' \
        >/dev/null; then
        echo "Could not close the VS Code window automatically." >&2
    fi
else
    echo "Close the editor window manually; automatic closing is available only from a VS Code terminal on macOS."
fi
