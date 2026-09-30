#!/usr/bin/env node

const { spawnSync } = require('node:child_process');
const { resolve } = require('node:path');

function git(args, captureOutput = false) {
    const result = spawnSync('git', args, {
        cwd: resolve(__dirname, '..'),
        encoding: 'utf8',
        stdio: captureOutput ? ['ignore', 'pipe', 'inherit'] : 'inherit'
    });

    if (result.error) {
        console.error(`ERROR: ${result.error.message}`);
    }
    if (result.error || result.status !== 0) {
        if (args[0] === 'merge') {
            console.error('ERROR: Merge failed. Check the Git output above. If there are conflicts, resolve them in VS Code and commit, or cancel with git merge --abort.');
        }
        process.exit(result.status || 1);
    }

    return captureOutput ? result.stdout.trim() : '';
}

const currentBranch = git(['rev-parse', '--abbrev-ref', 'HEAD'], true);
if (currentBranch === 'main' || currentBranch === 'HEAD') {
    console.error(`ERROR: Switch to the branch you want to merge main into. Current branch: ${currentBranch}`);
    process.exit(1);
}

if (git(['status', '--porcelain'], true)) {
    console.error('ERROR: Commit or stash your local changes before merging main.');
    process.exit(1);
}

console.log('--> Fetching main from origin');
git(['fetch', 'origin', 'main']);

console.log(`--> Merging origin/main into ${currentBranch}`);
git(['merge', 'FETCH_HEAD', '-m', `Merge main into ${currentBranch}`]);
