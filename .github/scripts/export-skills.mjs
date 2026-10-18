import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { lstat, mkdir, readFile, writeFile, chmod } from 'node:fs/promises';
import { dirname, join, posix, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

const SOURCE = 'https://github.com/webjetcms/webjetcms';
const SKILLS = '.agents/skills/';
const KNOWLEDGE = '.github/agents/';
const README = '.github/skills-distribution/README.md';
const EXCLUDED_SKILLS = '.github/skills-distribution/excluded-skills.txt';

function git(root, args) {
    return execFileSync('git', ['-C', root, ...args], { encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 });
}

function markdownWithoutCode(text) {
    return text.replace(/^\s*(`{3,}|~{3,})[^\n]*\n[\s\S]*?^\s*\1\s*$/gm, '');
}

/** Rewrite local Markdown links using the source-to-distribution file map. */
function rewriteLinks(text, sourcePath, destination, mappings, tracked, revision) {
    return text.replace(/(\]\()([^\s)]+)(\))/g, (match, start, target, end) => {
        if (/^(?:[a-z][a-z\d+.-]*:|#|\/)/i.test(target) || /[{}]/.test(target)) return match;
        const [path, fragment] = target.split('#', 2);
        const sourceTarget = posix.normalize(posix.join(posix.dirname(sourcePath), decodeURI(path)));
        const mapped = mappings.get(sourceTarget);
        const suffix = fragment === undefined ? '' : `#${fragment}`;
        if (mapped) return `${start}${encodeURI(posix.relative(posix.dirname(destination), mapped))}${suffix}${end}`;
        if (tracked.has(sourceTarget)) return `${start}${SOURCE}/blob/${revision}/${sourceTarget}${suffix}${end}`;
        return match;
    });
}

/** Validate required skill metadata and local links without requiring a CMS build. */
export function validateDistribution(files) {
    const names = [];
    for (const [path, { content }] of files) {
        if (!path.endsWith('.md')) continue;
        const text = content.toString('utf8');
        if (/^skills\/[^/]+\/SKILL\.md$/.test(path)) {
            const frontmatter = text.match(/^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/)?.[1];
            const name = frontmatter?.match(/^name:\s*['"]?([a-z0-9-]+)['"]?\s*$/m)?.[1];
            const description = frontmatter?.match(/^description:\s*(\S[^\r\n]*)/m)?.[1];
            if (!name || name.length > 64 || /--|^-|-$/.test(name) || name !== path.split('/')[1] || !description || /^(?:['"]{2}|null|~|\[|\{)/.test(description)) {
                throw new Error(`Invalid skill name or description: ${path}`);
            }
            names.push(name);
        }
        for (const match of markdownWithoutCode(text).matchAll(/\]\(([^\s)]+)\)/g)) {
            const target = match[1];
            if (/^(?:[a-z][a-z\d+.-]*:|#)/i.test(target)) continue;
            const localPath = decodeURI(target.split('#')[0]);
            const resolved = posix.normalize(posix.join(posix.dirname(path), localPath));
            if (localPath.startsWith('/') || !files.has(resolved)) throw new Error(`Broken distribution link in ${path}: ${target}`);
        }
    }
    for (const required of ['wj-project-context', 'wj-setup']) {
        if (!names.includes(required)) throw new Error(`Missing required tracked skill: ${required}. Stage new distribution files before exporting.`);
    }
    return names.sort();
}

/** Export only Git-indexed resources; generated knowledge remains outside the source tree. */
export async function exportSkills({ root, output, revision }) {
    root = resolve(root);
    output = resolve(output);
    revision ??= git(root, ['rev-parse', 'HEAD']).trim();
    if (!/^[a-f0-9]{40}$/.test(revision)) throw new Error('Source revision must be a full Git commit SHA.');
    const tracked = new Set(git(root, ['ls-files', '-z']).split('\0').filter(Boolean));
    const excluded = new Set((await readFile(join(root, EXCLUDED_SKILLS), 'utf8'))
        .split(/\r?\n/).map((line) => line.trim()).filter((line) => line && !line.startsWith('#')));
    for (const name of excluded) {
        if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(name) || !tracked.has(`${SKILLS}${name}/SKILL.md`)) {
            throw new Error(`Invalid or unknown excluded skill in ${EXCLUDED_SKILLS}: ${name}`);
        }
    }
    const mappings = new Map();
    for (const path of [...tracked].sort()) {
        if (path.startsWith(SKILLS)) {
            const resource = path.slice(SKILLS.length);
            if (!excluded.has(resource.split('/')[0])) mappings.set(path, `skills/${resource}`);
        }
        else if (path.startsWith(KNOWLEDGE)) mappings.set(path, `skills/wj-project-context/references/${path.slice(KNOWLEDGE.length)}`);
    }
    mappings.set(README, 'README.md');
    mappings.set('LICENSE.md', 'LICENSE.md');
    const files = new Map();
    for (const [source, destination] of mappings) {
        if (!tracked.has(source)) throw new Error(`Required file is not tracked: ${source}`);
        if (files.has(destination)) throw new Error(`Duplicate export destination: ${destination}`);
        const path = join(root, source);
        const stat = await lstat(path);
        if (!stat.isFile()) throw new Error(`Only regular tracked files can be exported: ${source}`);
        let content = await readFile(path);
        if (source.endsWith('.md')) content = Buffer.from(rewriteLinks(content.toString('utf8'), source, destination, mappings, tracked, revision));
        files.set(destination, { content, executable: (stat.mode & 0o111) !== 0 });
    }
    if (!files.has('skills/wj-project-context/references/AGENTS.md')) throw new Error('Canonical WebJET knowledge index is missing.');
    const skills = validateDistribution(files);
    const hash = createHash('sha256');
    for (const [path, file] of [...files].sort(([a], [b]) => a.localeCompare(b))) {
        // A new source commit alone must not trigger a distribution update.
        const content = path.endsWith('.md') ? file.content.toString('utf8').replaceAll(`${SOURCE}/blob/${revision}/`, `${SOURCE}/blob/SOURCE_REVISION/`) : file.content;
        hash.update(path).update('\0').update(file.executable ? 'x' : '-').update('\0').update(content).update('\0');
    }
    const manifest = { sourceRepository: SOURCE, sourceCommit: revision, contentHash: hash.digest('hex'), skills };
    files.set('source.json', { content: Buffer.from(JSON.stringify(manifest, null, 2) + '\n'), executable: false });
    // Refuse an existing destination instead of deleting arbitrary caller files.
    await mkdir(output, { recursive: false });
    for (const [path, file] of files) {
        const destination = join(output, path);
        await mkdir(dirname(destination), { recursive: true });
        await writeFile(destination, file.content);
        await chmod(destination, file.executable ? 0o755 : 0o644);
    }
    return manifest;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try {
        const { values } = parseArgs({ options: { output: { type: 'string' } } });
        if (!values.output) throw new Error('Usage: node .github/scripts/export-skills.mjs --output <new-directory>');
        const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
        const result = await exportSkills({ root, output: values.output });
        console.log(`Exported ${result.skills.length} skills to ${resolve(values.output)} (${result.contentHash}).`);
    } catch (error) {
        console.error(error.message);
        process.exitCode = 1;
    }
}
