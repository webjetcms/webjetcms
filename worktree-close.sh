#!/bin/bash
# Closes the current worktree's VS Code window before removing it on macOS.

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

close_and_remove_worktree() {
    local worktree_root="$1"
    local main_worktree="$2"
    local branch_name="$3"
    local close_window="$4"

    cd -- "$main_worktree"

    if [[ "$close_window" == true ]]; then
        # Wait for the specific window to close, including any unsaved-file prompts.
        # Deleting first lets language servers recreate build directories during removal.
        osascript - "$(basename -- "$worktree_root")" <<'APPLESCRIPT'
on matchingWindows(worktreeName)
    set matches to {}
    tell application "System Events"
        if not (exists application process "Code") then return matches
        tell application process "Code"
            repeat with candidate in windows
                set windowTitle to name of candidate
                if windowTitle contains (worktreeName & " (Workspace)") or windowTitle ends with worktreeName or windowTitle contains (worktreeName & " — ") or windowTitle contains (worktreeName & " - ") then
                    set end of matches to contents of candidate
                end if
            end repeat
        end tell
    end tell
    return matches
end matchingWindows

on run argv
    set worktreeName to item 1 of argv
    set matches to my matchingWindows(worktreeName)
    if (count of matches) is not 1 then error "Cannot identify exactly one VS Code window for " & worktreeName
    set targetWindow to item 1 of matches
    tell application "System Events"
        click (first button of targetWindow whose subrole is "AXCloseButton")
    end tell
    repeat 300 times
        if (count of my matchingWindows(worktreeName)) is 0 then return
        delay 0.1
    end repeat
    error "VS Code window is still open; the worktree was not removed."
end run
APPLESCRIPT
        sleep 1
    fi

    # Saving a file while closing the window may have made the worktree dirty.
    if [[ ! -f "$worktree_root/.git" ]]; then
        echo "Linked worktree metadata is missing: $worktree_root" >&2
        return 1
    fi
    local status
    status="$(git -C "$worktree_root" status --short --untracked-files=all)"
    if [[ -n "$status" ]]; then
        printf 'Worktree changed while closing; it was not removed:\n%s\n' "$status" >&2
        return 1
    fi

    if ! git worktree remove "$worktree_root"; then
        # Git may already have removed its metadata when a build recreates empty directories.
        # rmdir only removes empty directories and never deletes remaining files or symlinks.
        if [[ -e "$worktree_root/.git" ]]; then
            return 1
        fi
        for attempt in 1 2 3; do
            if [[ ! -d "$worktree_root" ]]; then break; fi
            sleep 1
            find "$worktree_root" -depth -type d -exec rmdir {} \; 2>/dev/null
        done
        if [[ -e "$worktree_root" ]]; then
            echo "Files remain in $worktree_root; inspect them before removing the directory." >&2
            return 1
        fi
    fi
    git worktree prune

    echo "Removed worktree: $worktree_root"
    if [[ -n "$branch_name" ]]; then
        echo "Branch preserved: $branch_name"
    fi
}

if [[ "${TERM_PROGRAM:-}" == "vscode" && "$(uname -s)" == "Darwin" ]]; then
    # This worker must survive the integrated terminal closing with its VS Code window.
    CLOSE_LOG="$(mktemp "${TMPDIR:-/tmp}/webjet-worktree-close.XXXXXX")"
    echo "Closing VS Code, then removing the worktree. Log: $CLOSE_LOG"
    export -f close_and_remove_worktree
    cd -- "$MAIN_WORKTREE"
    nohup /bin/bash -euo pipefail -c 'close_and_remove_worktree "$@"' _ \
        "$WORKTREE_ROOT" "$MAIN_WORKTREE" "$BRANCH_NAME" true \
        >"$CLOSE_LOG" 2>&1 </dev/null &
else
    echo "The worktree's editor window should be closed before removal from an external terminal."
    close_and_remove_worktree "$WORKTREE_ROOT" "$MAIN_WORKTREE" "$BRANCH_NAME" false
fi
