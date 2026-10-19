import { lstat, readFile, realpath, mkdir, writeFile } from 'node:fs/promises';
import { homedir } from 'node:os';
import { dirname, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

const START = '<!-- webjet-skills:start -->';
const END = '<!-- webjet-skills:end -->';

async function statIfPresent(path) {
    try {
        return await lstat(path);
    } catch (error) {
        if (error.code === 'ENOENT') return null;
        throw error;
    }
}

/** Replace only the managed block, preserving all other bytes and line endings. */
export function updateBlock(original, body) {
    const starts = [...original.matchAll(/^<!-- webjet-skills:start -->\r?$/gm)];
    const ends = [...original.matchAll(/^<!-- webjet-skills:end -->\r?$/gm)];
    const markerCount = [...original.matchAll(/<!--\s*webjet-skills\b/g)].length;
    if (starts.length !== ends.length || starts.length > 1 || markerCount !== starts.length + ends.length || (starts.length === 1 && starts[0].index >= ends[0].index)) {
        throw new Error('Malformed or duplicate WebJET markers; repair them before running setup.');
    }
    let fence;
    for (const line of original.split(/\r?\n/)) {
        const boundary = line.match(/^ {0,3}(`{3,}|~{3,})(.*)$/);
        if (boundary) {
            if (!fence) fence = boundary[1];
            else if (boundary[1][0] === fence[0] && boundary[1].length >= fence.length && !boundary[2].trim()) fence = undefined;
        }
        if (fence && /<!--\s*webjet-skills\b/.test(line)) throw new Error('Malformed WebJET markers inside a code example; integrate these instructions manually.');
    }
    if (fence) throw new Error('Malformed Markdown: close the code fence before running setup.');
    const newline = original.includes('\r\n') ? '\r\n' : '\n';
    const block = [START, ...body.split('\n'), END].join(newline);
    if (starts.length === 1) {
        return original.slice(0, starts[0].index) + block + original.slice(ends[0].index + END.length);
    }
    const separator = original.length === 0 ? '' : original.endsWith(newline + newline) ? '' : original.endsWith(newline) ? newline : newline + newline;
    return original + separator + block + newline;
}

/** Plan all instruction edits before writing, refusing symlinks that could escape the project. */
export async function setupProject({ project, scope = 'project', dryRun = false }) {
    if (!['project', 'global'].includes(scope)) throw new Error('Scope must be project or global.');
    const root = await realpath(resolve(project));
    const personal = await realpath(homedir());
    const globalDirectories = [personal, ...['.codex', '.claude', '.copilot', '.agents'].map((name) => join(personal, name)), process.env.CODEX_HOME, process.env.CLAUDE_CONFIG_DIR].filter(Boolean);
    for (const directory of globalDirectories) {
        const resolvedDirectory = await realpath(directory).catch(() => resolve(directory));
        if (root === resolvedDirectory || dirname(root) === root) {
            throw new Error('Choose a project directory, not a global agent directory, home or filesystem root.');
        }
    }
    let context;
    if (scope === 'project') {
        for (const directory of ['.agents/skills', '.claude/skills']) {
            const candidate = join(directory, 'wj-project-context/SKILL.md');
            try {
                await readFile(join(root, candidate), 'utf8');
                context = candidate.split(sep).join('/');
                break;
            } catch (error) {
                if (error.code !== 'ENOENT') throw error;
            }
        }
        if (!context) throw new Error('Install wj-project-context in this project, or use --scope global for an existing global installation.');
    }
    const contextInstruction = context
        ? `Before WebJET work, read [wj-project-context](${context}) and only its relevant references.`
        : 'Before WebJET work, locate and read the installed wj-project-context skill from the active skill catalog and only its relevant references.';
    const guidance = `## WebJET CMS\n\n${contextInstruction}\nFollow this project's own instructions over shared WebJET defaults. Verify the installed CMS version and available APIs before using examples. If the context skill is missing, report it and install it through the documented WebJET skills setup.\nKeep project-specific rules outside this managed block.`;
    const files = new Map([
        ['AGENTS.md', guidance],
        ['.github/copilot-instructions.md', 'Before WebJET work, read the project [AGENTS.md](../AGENTS.md) and follow its WebJET context guidance. Project-specific instructions take precedence over shared defaults.'],
        ['CLAUDE.md', '@AGENTS.md']
    ]);
    if (await statIfPresent(join(root, 'AGENTS.override.md'))) files.set('AGENTS.override.md', guidance);
    const changes = [];
    for (const [name, body] of files) {
        const path = join(root, name);
        let current = root;
        for (const part of relative(root, path).split(sep)) {
            current = join(current, part);
            const stat = await statIfPresent(current);
            if (stat?.isSymbolicLink()) throw new Error(`Refusing to edit through a symlink: ${name}`);
            if (stat && (current === path ? !stat.isFile() : !stat.isDirectory())) throw new Error(`Unexpected file type: ${name}`);
        }
        const original = await statIfPresent(path) ? await readFile(path, 'utf8') : '';
        let updated;
        try {
            updated = updateBlock(original, body);
        } catch (error) {
            throw new Error(`${name}: ${error.message}`);
        }
        if (updated !== original) changes.push({ name, path, updated });
    }
    if (!dryRun) {
        for (const change of changes) {
            await mkdir(dirname(change.path), { recursive: true });
            await writeFile(change.path, change.updated, 'utf8');
        }
    }
    return changes.map(({ name }) => name);
}

if (process.argv[1] && await realpath(resolve(process.argv[1])) === await realpath(fileURLToPath(import.meta.url))) {
    try {
        const { values } = parseArgs({ options: {
            project: { type: 'string' }, scope: { type: 'string', default: 'project' },
            'dry-run': { type: 'boolean', default: false }
        } });
        if (!values.project) throw new Error('Usage: node setup-project.mjs --project <directory> --scope project|global [--dry-run]');
        const changes = await setupProject({ project: values.project, scope: values.scope, dryRun: values['dry-run'] });
        console.log(changes.length ? `${values['dry-run'] ? 'Would update' : 'Updated'}: ${changes.join(', ')}` : 'WebJET instruction blocks are already up to date.');
    } catch (error) {
        console.error(error.message);
        process.exitCode = 1;
    }
}
