#!/bin/bash
# Creates or checks out a branch worktree and opens a private VS Code workspace.
# Workspace settings and Peacock colors are stored in ~/.vscode-workspaces/.
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
WORKSPACE_FILE="$HOME/.vscode-workspaces/webjetcms-$WORKTREE_NAME.code-workspace"

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
    # Use main as the starting point without making it the branch's sync target.
    git -C "$REPO_ROOT" worktree add --no-track -b "$BRANCH_NAME" "$WORKTREE_PATH" origin/main
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

echo "Preparing private VS Code workspace: $WORKSPACE_FILE"
node - "$WORKTREE_PATH" "$WORKSPACE_FILE" <<'NODE'
const fs = require('fs');
const path = require('path');
const [targetFolder, workspaceFile] = process.argv.slice(2);

// Keep existing workspace settings, including the color chosen by Peacock.
if (!fs.existsSync(workspaceFile)) {
    fs.mkdirSync(path.dirname(workspaceFile), { recursive: true });
    fs.writeFileSync(workspaceFile, JSON.stringify({
        folders: [{ name: 'webjetcms', path: targetFolder }],
        settings: {
            'window.title': '${activeRepositoryBranchName}${separator}${rootName}${separator}${dirty}${activeEditorShort}',
            'window.commandCenter': false,
            'window.titleBarStyle': 'custom',
            'scm.defaultViewMode': 'tree',
            'search.defaultViewMode': 'tree',
            'githubPullRequests.defaultCreateOption': 'createDraft',
            'peacock.surpriseMeOnStartup': true,
            'peacock.surpriseMeFromFavoritesOnly': true
        },
        tasks: {
            version: '2.0.0',
            tasks: [
                ['Admin v9', 'src/main/webapp/admin/v9'],
                ['Webapp', 'src/main/webapp']
            ].map(([label, directory]) => ({
                label,
                type: 'process',
                command: process.env.SHELL || '/bin/zsh',
                args: ['-l', '-i'],
                options: { cwd: path.join(targetFolder, directory) },
                isBackground: true,
                problemMatcher: [],
                runOptions: { runOn: 'folderOpen', instanceLimit: 1, instancePolicy: 'silent' },
                presentation: {
                    reveal: 'always',
                    focus: false,
                    panel: 'dedicated',
                    group: 'webjetcms-worktree',
                    echo: false,
                    showReuseMessage: false
                }
            }))
        }
    }, null, 2) + '\n', { flag: 'wx' });
}
NODE
echo "If VS Code asks to allow automatic tasks, choose Allow to open the workspace terminals."

# Search input fields and pinned editors are stored in workspace UI state.
# Seed only a new storage directory, before VS Code loads its database into memory.
if [[ "$(uname -s)" == "Darwin" ]]; then
    if command -v sqlite3 >/dev/null 2>&1; then
        echo "Preparing VS Code search filters and pinned changelog"
        if ! node - "$LOCAL_FILES_SOURCE" "$WORKSPACE_FILE" "$WORKTREE_PATH" <<'NODE'
const fs = require('fs');
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const [sourceFolder, workspaceFile, targetFolder] = process.argv.slice(2);
const storageRoot = path.join(os.homedir(), 'Library/Application Support/Code/User/workspaceStorage');
const searchKey = 'memento/workbench.view.search';
const editorKey = 'memento/workbench.parts.editor';

function workspaceStorage(folder) {
    // Match VS Code's macOS folder identity, including Node's birthtime rounding.
    // https://github.com/microsoft/vscode/blob/main/src/vs/platform/workspaces/node/workspaces.ts
    const birthtime = fs.statSync(folder).birthtime.getTime();
    const id = crypto.createHash('md5').update(folder).update(birthtime ? String(birthtime) : '').digest('hex');
    return path.join(storageRoot, id);
}

try {
    // Unlike folder identities, workspace file identities use the lowercase path on macOS.
    const workspaceId = crypto.createHash('md5').update(workspaceFile.toLowerCase()).digest('hex');
    const targetStorage = path.join(storageRoot, workspaceId);
    if (fs.existsSync(targetStorage)) {
        console.log('VS Code workspace storage already exists for the target folder; leaving it unchanged.');
        process.exit(0);
    }

    const states = {};
    // Missing or unreadable search history must not prevent pinning the changelog.
    try {
        const sourceDb = path.join(workspaceStorage(sourceFolder), 'state.vscdb');
        if (fs.existsSync(sourceDb)) {
            const saved = execFileSync('sqlite3', ['-readonly', sourceDb,
                `SELECT value FROM ItemTable WHERE key = '${searchKey}';`], { encoding: 'utf8' }).trim();
            const sourceQuery = saved ? JSON.parse(saved).query : undefined;
            if (sourceQuery && (typeof sourceQuery.folderIncludes === 'string' || typeof sourceQuery.folderExclusions === 'string')) {
                const query = { queryDetailsExpanded: true };
                for (const key of ['folderIncludes', 'folderExclusions', 'useExcludesAndIgnoreFiles']) {
                    if (Object.hasOwn(sourceQuery, key)) query[key] = sourceQuery[key];
                }

                // Workspace files resolve ./ paths through the root folder's display name.
                for (const key of ['folderIncludes', 'folderExclusions']) {
                    if (typeof query[key] === 'string') {
                        query[key] = query[key].replace(/(^|,)(\s*)\.\//g, '$1$2./webjetcms/');
                    }
                }
                states[searchKey] = { query };
            }
        }
        if (!states[searchKey]) console.log('No saved VS Code search filters found for the source folder; skipping filters.');
    } catch (error) {
        console.error(`Could not copy VS Code search filters: ${error.message}`);
    }

    const changelog = path.join(targetFolder, 'docs/sk/CHANGELOG-2026.md');
    if (fs.existsSync(changelog)) {
        // Match VS Code's editorPart/editorGroupModel serialization. "Pin" is sticky index 0.
        // https://github.com/microsoft/vscode/blob/main/src/vs/workbench/common/editor/editorGroupModel.ts
        const group = {
            id: 0,
            editors: [{
                id: 'workbench.editors.files.fileEditorInput',
                value: JSON.stringify({ resourceJSON: { scheme: 'file', path: changelog } })
            }],
            mru: [0],
            sticky: 0
        };
        states[editorKey] = {
            'editorpart.state': {
                serializedGrid: {
                    root: { type: 'branch', data: [{ type: 'leaf', data: group, size: 1200 }], size: 800 },
                    orientation: 1,
                    width: 1200,
                    height: 800
                },
                activeGroup: 0,
                mostRecentActiveGroups: [0]
            }
        };
    } else {
        console.log('Changelog does not exist in this worktree; skipping pinned editor.');
    }

    if (Object.keys(states).length === 0) process.exit(0);
    fs.mkdirSync(targetStorage, { recursive: true });
    fs.writeFileSync(path.join(targetStorage, 'workspace.json'), JSON.stringify({
        workspace: require('url').pathToFileURL(workspaceFile).href
    }, null, 2));

    // Hex literals preserve quotes, backslashes and Unicode in user-entered patterns.
    states['__$__targetStorageMarker'] = Object.fromEntries(
        Object.keys(states).map(key => [key, key === editorKey ? 0 : 1])
    );
    const inserts = Object.entries(states).map(([key, state]) => {
        const value = Buffer.from(JSON.stringify(state)).toString('hex');
        return `INSERT INTO ItemTable VALUES ('${key}', CAST(X'${value}' AS TEXT));`;
    }).join('\n');
    execFileSync('sqlite3', [path.join(targetStorage, 'state.vscdb')], {
        input: `BEGIN;
            CREATE TABLE ItemTable (key TEXT UNIQUE ON CONFLICT REPLACE, value BLOB);
            ${inserts}
            COMMIT;`,
        encoding: 'utf8'
    });
    console.log('VS Code workspace UI state prepared.');
} catch (error) {
    console.error(`Could not prepare VS Code workspace UI state: ${error.message}`);
    process.exitCode = 1;
}
NODE
        then
            echo "Opening VS Code without the prepared workspace UI state." >&2
        fi
    else
        echo "sqlite3 is not available; skipping VS Code search filters and pinned changelog." >&2
    fi
fi

echo "Opening the new worktree in VS Code"
(
    cd -- "$WORKTREE_PATH"
    code --new-window "$WORKSPACE_FILE"
)
