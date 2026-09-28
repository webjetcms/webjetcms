#!/bin/bash
# Removes a branch or commit worktree and its private VS Code workspace.
# Also removes the local branch when closing a branch worktree.
# Run from the main checkout: ./worktree-close.sh feature/58742-forms-add-step-back-button-fn
# Or use a commit ID: ./worktree-close.sh cef6ca9a74d0bba276e5eef414182503b45ef3b7

set -euo pipefail

if (( $# != 1 )); then
    echo "Usage: $0 <branch-name|commit-id>" >&2
    exit 1
fi

BRANCH_NAME="$1"
if ! git check-ref-format "refs/heads/$BRANCH_NAME" >/dev/null 2>&1 || [[ "$BRANCH_NAME" == -* ]]; then
    echo "Invalid branch name: $BRANCH_NAME" >&2
    exit 1
fi

CURRENT_WORKTREE="$(git rev-parse --show-toplevel)"
COMMON_GIT_DIR="$(git rev-parse --path-format=absolute --git-common-dir)"
MAIN_WORKTREE="$(dirname -- "$COMMON_GIT_DIR")"

if [[ "$CURRENT_WORKTREE" != "$MAIN_WORKTREE" ]]; then
    echo "Run this script from the main checkout: $MAIN_WORKTREE" >&2
    exit 1
fi
cd -- "$MAIN_WORKTREE"

WORKTREE_NAME="${BRANCH_NAME//\//-}"
COMMIT_ID=""
# Match the commit ID detection and directory naming in worktree-add.sh.
if [[ "$BRANCH_NAME" =~ ^[0-9a-fA-F]{4,40}$ ]] \
    && ! git show-ref --verify --quiet "refs/heads/$BRANCH_NAME" \
    && ! git show-ref --verify --quiet "refs/remotes/origin/$BRANCH_NAME"; then
    if ! COMMIT_ID="$(git rev-parse --verify --quiet "$BRANCH_NAME^{commit}")"; then
        echo "Cannot resolve commit ID: $BRANCH_NAME" >&2
        exit 1
    fi
    WORKTREE_NAME="commit-${COMMIT_ID:0:12}"
fi

EXPECTED_WORKTREE_ROOT="$(dirname -- "$MAIN_WORKTREE")/webjetcms-$WORKTREE_NAME"
WORKTREE_ROOT=""
REGISTERED_PATH=""
while IFS= read -r -d '' FIELD; do
    case "$FIELD" in
        worktree\ *)
            REGISTERED_PATH="${FIELD#worktree }"
            if [[ -n "$COMMIT_ID" && "$REGISTERED_PATH" == "$EXPECTED_WORKTREE_ROOT" ]]; then
                WORKTREE_ROOT="$REGISTERED_PATH"
            fi
            ;;
        "branch refs/heads/$BRANCH_NAME")
            if [[ -z "$COMMIT_ID" ]]; then WORKTREE_ROOT="$REGISTERED_PATH"; fi
            ;;
    esac
done < <(git worktree list --porcelain -z)

worktree_is_unregistered() {
    # With pipefail, a failed Git query also prevents recursive deletion.
    git worktree list --porcelain -z | (
        while IFS= read -r -d '' FIELD; do
            if [[ "$FIELD" == "worktree $1" ]]; then exit 1; fi
        done
        exit 0
    )
}

WORKTREE_REGISTERED=true
if [[ -z "$WORKTREE_ROOT" ]]; then
    WORKTREE_REGISTERED=false
    WORKTREE_ROOT="$EXPECTED_WORKTREE_ROOT"
    if ! worktree_is_unregistered "$WORKTREE_ROOT"; then
        echo "Cannot clean up a directory registered to another worktree or whose Git registration cannot be verified: $WORKTREE_ROOT" >&2
        exit 1
    fi
    echo "No registered worktree found; cleaning up remaining files: $WORKTREE_ROOT"
fi
if [[ "$WORKTREE_ROOT" == "$MAIN_WORKTREE" ]]; then
    echo "Refusing to remove the main worktree: $WORKTREE_ROOT" >&2
    exit 1
fi

if [[ "$WORKTREE_REGISTERED" == true ]]; then
    if [[ ! -f "$WORKTREE_ROOT/.git" ]]; then
        echo "Linked worktree metadata is missing: $WORKTREE_ROOT" >&2
        exit 1
    fi
    if [[ -n "$COMMIT_ID" ]]; then
        if [[ "$(git -C "$WORKTREE_ROOT" rev-parse HEAD)" != "$COMMIT_ID" \
            || -n "$(git -C "$WORKTREE_ROOT" branch --show-current)" ]]; then
            echo "Worktree is not detached at commit $COMMIT_ID: $WORKTREE_ROOT" >&2
            exit 1
        fi
    elif [[ "$(git -C "$WORKTREE_ROOT" branch --show-current)" != "$BRANCH_NAME" ]]; then
        echo "Worktree is no longer on branch $BRANCH_NAME: $WORKTREE_ROOT" >&2
        exit 1
    fi
    WORKTREE_STATUS="$(git -C "$WORKTREE_ROOT" status --short --untracked-files=all)"
    if [[ -n "$WORKTREE_STATUS" ]]; then
        printf 'Worktree has uncommitted or untracked files:\n%s\n' "$WORKTREE_STATUS" >&2
        echo "Commit, stash, or remove them before closing the worktree." >&2
        exit 1
    fi
fi

LOCAL_BRANCH_EXISTS=false
if [[ -z "$COMMIT_ID" ]] && git show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
    LOCAL_BRANCH_EXISTS=true
    # Match git branch -d: require commits to be reachable from the upstream or HEAD.
    MERGE_TARGET="$(git rev-parse --verify --quiet "$BRANCH_NAME@{upstream}" || git rev-parse HEAD)"
    if ! git merge-base --is-ancestor "refs/heads/$BRANCH_NAME" "$MERGE_TARGET"; then
        echo "Branch is not fully merged into its upstream or the main checkout: $BRANCH_NAME" >&2
        echo "Merge or push its commits before closing the worktree." >&2
        exit 1
    fi
fi

WORKSPACE_FILE="$HOME/.vscode-workspaces/webjetcms-$WORKTREE_NAME.code-workspace"
if [[ -f "$WORKSPACE_FILE" ]] && ! command -v node >/dev/null 2>&1; then
    echo "node is required to clean up the private VS Code workspace." >&2
    exit 1
fi

manage_workspace() {
    if [[ ! -f "$WORKSPACE_FILE" ]]; then return; fi
    node - "$WORKTREE_ROOT" "$WORKSPACE_FILE" "$1" <<'NODE'
const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const [worktreeRoot, workspaceFile, action] = process.argv.slice(2);
const workspace = JSON.parse(fs.readFileSync(workspaceFile, 'utf8'));
if (workspace.folders?.length !== 1 ||
    path.resolve(path.dirname(workspaceFile), workspace.folders[0].path) !== worktreeRoot) {
    throw new Error(`Workspace does not reference only the target worktree: ${workspaceFile}`);
}
// Match the macOS workspace identity used by worktree-add.sh.
let storage;
if (process.platform === 'darwin') {
    const workspaceId = crypto.createHash('md5').update(workspaceFile.toLowerCase()).digest('hex');
    storage = path.join(os.homedir(), 'Library/Application Support/Code/User/workspaceStorage', workspaceId);
}
if (action === 'check') {
    console.log(`  VS Code workspace file: ${workspaceFile}`);
    if (storage) console.log(`  VS Code workspace storage directory: ${storage}`);
    process.exit(0);
}

if (storage) {
    console.log(`Removing VS Code workspace storage: ${storage}`);
    fs.rmSync(storage, { recursive: true, force: true });
}
console.log(`Removing VS Code workspace file: ${workspaceFile}`);
fs.unlinkSync(workspaceFile);
console.log(`Removed VS Code workspace: ${workspaceFile}`);
NODE
}

echo "The following will be removed:"
if [[ -n "$COMMIT_ID" ]]; then
    printf '  Worktree at commit (detached HEAD): %s\n' "$COMMIT_ID"
elif [[ "$LOCAL_BRANCH_EXISTS" == true ]]; then
    printf '  Local branch: %s\n' "$BRANCH_NAME"
fi
printf '  Worktree directory: %s\n' "$WORKTREE_ROOT"
manage_workspace check
echo
if ! IFS= read -r -p "Type Y to confirm removal: " CONFIRM || [[ "$CONFIRM" != "Y" ]]; then
    echo "Cancelled. Nothing was removed."
    exit 0
fi

echo "Removing worktree: $WORKTREE_ROOT"
if [[ "$WORKTREE_REGISTERED" == true ]]; then
    if ! git worktree remove "$WORKTREE_ROOT"; then
        echo "Git removal failed; checking registration before cleaning up remaining files."
    fi
fi
if ! worktree_is_unregistered "$WORKTREE_ROOT"; then
    echo "Cannot remove remaining files while the worktree is registered or its Git registration cannot be verified: $WORKTREE_ROOT" >&2
    exit 1
fi
# Build tools may leave or recreate files after Git removes the worktree metadata.
for attempt in 1 2 3; do
    if [[ ! -e "$WORKTREE_ROOT" && ! -L "$WORKTREE_ROOT" ]]; then break; fi
    if (( attempt > 1 )); then sleep 1; fi
    rm -rf -- "$WORKTREE_ROOT" || true
done
if [[ -e "$WORKTREE_ROOT" || -L "$WORKTREE_ROOT" ]]; then
    echo "Could not remove the remaining worktree directory: $WORKTREE_ROOT" >&2
    exit 1
fi
echo "Removed worktree: $WORKTREE_ROOT"

manage_workspace remove
if [[ "$LOCAL_BRANCH_EXISTS" == true ]]; then
    echo "Removing local branch: $BRANCH_NAME"
    git branch -d -- "$BRANCH_NAME"
fi
