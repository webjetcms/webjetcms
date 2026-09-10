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

# Search input fields are workspace UI state, not .vscode/settings.json settings.
# Seed only a new storage directory, before VS Code loads its database into memory.
if [[ "$(uname -s)" == "Darwin" ]]; then
    if command -v sqlite3 >/dev/null 2>&1; then
        echo "Copying VS Code search filters"
        if ! node - "$LOCAL_FILES_SOURCE" "$WORKTREE_PATH" <<'NODE'
const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const [sourceFolder, targetFolder] = process.argv.slice(2);
const storageRoot = path.join(os.homedir(), 'Library/Application Support/Code/User/workspaceStorage');
const searchKey = 'memento/workbench.view.search';

function workspaceStorage(folder) {
    // Match VS Code's macOS folder identity, including Node's birthtime rounding.
    // https://github.com/microsoft/vscode/blob/main/src/vs/platform/workspaces/node/workspaces.ts
    const birthtime = fs.statSync(folder).birthtime.getTime();
    const id = crypto.createHash('md5').update(folder).update(birthtime ? String(birthtime) : '').digest('hex');
    return path.join(storageRoot, id);
}

try {
    const sourceDb = path.join(workspaceStorage(sourceFolder), 'state.vscdb');
    if (!fs.existsSync(sourceDb)) {
        console.log('No saved VS Code search filters found for the source folder; skipping.');
        process.exit(0);
    }

    const saved = execFileSync('sqlite3', ['-readonly', sourceDb,
        `SELECT value FROM ItemTable WHERE key = '${searchKey}';`], { encoding: 'utf8' }).trim();
    const sourceQuery = saved ? JSON.parse(saved).query : undefined;
    if (!sourceQuery || (typeof sourceQuery.folderIncludes !== 'string' && typeof sourceQuery.folderExclusions !== 'string')) {
        console.log('No saved VS Code search filters found for the source folder; skipping.');
        process.exit(0);
    }

    const query = { queryDetailsExpanded: true };
    for (const key of ['folderIncludes', 'folderExclusions', 'useExcludesAndIgnoreFiles']) {
        if (Object.hasOwn(sourceQuery, key)) query[key] = sourceQuery[key];
    }

    const targetStorage = workspaceStorage(targetFolder);
    if (fs.existsSync(targetStorage)) {
        console.log('VS Code workspace storage already exists for the target folder; leaving it unchanged.');
        process.exit(0);
    }
    fs.mkdirSync(targetStorage);
    fs.writeFileSync(path.join(targetStorage, 'workspace.json'), JSON.stringify({
        folder: require('url').pathToFileURL(targetFolder).href
    }, null, 2));

    // Hex literals preserve quotes, backslashes and Unicode in user-entered patterns.
    const searchState = Buffer.from(JSON.stringify({ query })).toString('hex');
    const storageTargets = Buffer.from(JSON.stringify({ [searchKey]: 1 })).toString('hex');
    execFileSync('sqlite3', [path.join(targetStorage, 'state.vscdb')], {
        input: `BEGIN;
            CREATE TABLE ItemTable (key TEXT UNIQUE ON CONFLICT REPLACE, value BLOB);
            INSERT INTO ItemTable VALUES ('${searchKey}', CAST(X'${searchState}' AS TEXT));
            INSERT INTO ItemTable VALUES ('__$__targetStorageMarker', CAST(X'${storageTargets}' AS TEXT));
            COMMIT;`,
        encoding: 'utf8'
    });
    console.log('VS Code search filters copied.');
} catch (error) {
    console.error(`Could not copy VS Code search filters: ${error.message}`);
    process.exitCode = 1;
}
NODE
        then
            echo "Opening VS Code without copying search filters." >&2
        fi
    else
        echo "sqlite3 is not available; skipping VS Code search filters." >&2
    fi
fi

echo "Opening the new worktree in VS Code"
(
    cd -- "$WORKTREE_PATH"
    code .
)
