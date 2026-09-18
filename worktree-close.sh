#!/bin/bash
# Removes a branch worktree, its private VS Code workspace, and the local branch.
# Run from the main checkout: ./worktree-close.sh feature/58742-forms-add-step-back-button-fn

set -euo pipefail

if (( $# != 1 )); then
    echo "Usage: $0 <branch-name>" >&2
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

WORKTREE_ROOT=""
REGISTERED_PATH=""
while IFS= read -r -d '' FIELD; do
    case "$FIELD" in
        worktree\ *) REGISTERED_PATH="${FIELD#worktree }" ;;
        "branch refs/heads/$BRANCH_NAME") WORKTREE_ROOT="$REGISTERED_PATH" ;;
    esac
done < <(git worktree list --porcelain -z)

if [[ -z "$WORKTREE_ROOT" ]]; then
    echo "No worktree found for branch: $BRANCH_NAME" >&2
    exit 1
fi
if [[ "$WORKTREE_ROOT" == "$MAIN_WORKTREE" ]]; then
    echo "Refusing to remove the main worktree: $WORKTREE_ROOT" >&2
    exit 1
fi

if [[ ! -f "$WORKTREE_ROOT/.git" ]]; then
    echo "Linked worktree metadata is missing: $WORKTREE_ROOT" >&2
    exit 1
fi
if [[ "$(git -C "$WORKTREE_ROOT" branch --show-current)" != "$BRANCH_NAME" ]]; then
    echo "Worktree is no longer on branch $BRANCH_NAME: $WORKTREE_ROOT" >&2
    exit 1
fi
WORKTREE_STATUS="$(git -C "$WORKTREE_ROOT" status --short --untracked-files=all)"
if [[ -n "$WORKTREE_STATUS" ]]; then
    printf 'Worktree has uncommitted or untracked files:\n%s\n' "$WORKTREE_STATUS" >&2
    echo "Commit, stash, or remove them before closing the worktree." >&2
    exit 1
fi

# Match git branch -d: require commits to be reachable from the upstream or HEAD.
MERGE_TARGET="$(git rev-parse --verify --quiet "$BRANCH_NAME@{upstream}" || git rev-parse HEAD)"
if ! git merge-base --is-ancestor "refs/heads/$BRANCH_NAME" "$MERGE_TARGET"; then
    echo "Branch is not fully merged into its upstream or the main checkout: $BRANCH_NAME" >&2
    echo "Merge or push its commits before closing the worktree." >&2
    exit 1
fi

WORKTREE_NAME="${BRANCH_NAME//\//-}"
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
printf '  Local branch: %s\n  Worktree directory: %s\n' "$BRANCH_NAME" "$WORKTREE_ROOT"
manage_workspace check
echo
if ! IFS= read -r -p "Type Y to confirm removal: " CONFIRM || [[ "$CONFIRM" != "Y" ]]; then
    echo "Cancelled. Nothing was removed."
    exit 0
fi

echo "Removing worktree: $WORKTREE_ROOT"
if ! git worktree remove "$WORKTREE_ROOT"; then
    # Git may have removed its metadata before a build recreated empty directories.
    if [[ -e "$WORKTREE_ROOT/.git" ]]; then
        exit 1
    fi
    for attempt in 1 2 3; do
        if [[ ! -d "$WORKTREE_ROOT" ]]; then break; fi
        sleep 1
        find "$WORKTREE_ROOT" -depth -type d -exec rmdir {} \; 2>/dev/null || true
    done
    if [[ -e "$WORKTREE_ROOT" ]]; then
        echo "Files remain in $WORKTREE_ROOT; inspect them before removing the directory." >&2
        exit 1
    fi
fi
echo "Removed worktree: $WORKTREE_ROOT"

manage_workspace remove
echo "Removing local branch: $BRANCH_NAME"
git branch -d -- "$BRANCH_NAME"
