import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { cp, mkdir, mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const distribution = resolve(process.argv[2] ?? '');
const installer = resolve(process.argv[3] ?? '');
if (process.argv.length !== 4) throw new Error('Usage: node test-skills-install.mjs <export-directory> <skills/bin/cli.mjs>');
const { parse: parseYaml } = createRequire(installer)('yaml');
const manifest = JSON.parse(await readFile(join(distribution, 'source.json'), 'utf8'));
const scratch = await mkdtemp(join(tmpdir(), 'webjet-skills-install-'));

async function compareDirectory(expected, actual) {
    for (const entry of await readdir(expected, { withFileTypes: true })) {
        const source = join(expected, entry.name);
        const destination = join(actual, entry.name);
        if (entry.isDirectory()) await compareDirectory(source, destination);
        else assert.deepEqual(await readFile(destination), await readFile(source), destination);
    }
}

try {
    for (const name of manifest.skills) {
        const skill = await readFile(join(distribution, 'skills', name, 'SKILL.md'), 'utf8');
        const metadata = parseYaml(skill.match(/^---\r?\n([\s\S]*?)\r?\n---/)?.[1] ?? '');
        assert.equal(metadata.name, name);
        assert.equal(typeof metadata.description, 'string');
        assert.ok(metadata.description.trim().length > 0);
        const agentMetadata = await readFile(join(distribution, 'skills', name, 'agents/openai.yaml'), 'utf8').catch((error) => {
            if (error.code === 'ENOENT') return null;
            throw error;
        });
        if (agentMetadata !== null) assert.equal(typeof parseYaml(agentMetadata).interface, 'object');
    }
    // Override homedir only inside the child runtime. Never change the user's HOME or CODEX_HOME.
    const wrapper = join(scratch, 'isolated-installer.mjs');
    await writeFile(wrapper, `import os from 'node:os';\nimport { syncBuiltinESMExports } from 'node:module';\nos.homedir = () => process.env.WEBJET_SKILLS_TEST_HOME;\nsyncBuiltinESMExports();\nprocess.argv = [process.execPath, ${JSON.stringify(installer)}, ...process.argv.slice(2)];\nawait import(${JSON.stringify(pathToFileURL(installer).href)});\n`);
    for (const global of [false, true]) {
        for (const copy of [false, true]) {
            const label = `${global ? 'global' : 'project'}-${copy ? 'copy' : 'symlink'}`;
            const root = join(scratch, label);
            const personal = join(root, 'personal');
            const project = join(root, 'project with spaces');
            const source = join(root, 'distribution');
            await mkdir(personal, { recursive: true });
            await mkdir(project, { recursive: true });
            await cp(distribution, source, { recursive: true });
            const args = ['add', source, '--skill', '*', '--agent', 'codex', 'github-copilot', 'claude-code', '--yes'];
            if (global) args.push('--global');
            if (copy) args.push('--copy');
            const run = (arguments_) => execFileSync(process.execPath, [wrapper, ...arguments_], {
                cwd: project, encoding: 'utf8', timeout: 120000, maxBuffer: 4 * 1024 * 1024,
                env: {
                    ...process.env,
                    WEBJET_SKILLS_TEST_HOME: personal,
                    CLAUDE_CONFIG_DIR: join(personal, '.claude'),
                    XDG_STATE_HOME: join(personal, '.local/state'),
                    DISABLE_TELEMETRY: '1', DO_NOT_TRACK: '1', NO_COLOR: '1'
                }
            });
            const listing = run(['add', source, '--list']);
            assert.match(listing, new RegExp(`Found ${manifest.skills.length} skills`));
            run(args);
            // Codex and Copilot share the universal .agents directory in skills@1.7.2.
            const destinations = ['.agents/skills', '.claude/skills'].map((path) => join(global ? personal : project, path));
            for (const destination of destinations) {
                for (const name of manifest.skills) await compareDirectory(join(source, 'skills', name), join(destination, name));
            }
            await writeFile(join(project, 'AGENTS.md'), '# Customer rules\n\nKeep our migration workflow.\n');
            const setup = join(destinations.at(-1), 'wj-setup/scripts/setup-project.mjs');
            const configure = () => execFileSync(process.execPath, [setup, '--project', project, '--scope', global ? 'global' : 'project'], { encoding: 'utf8' });
            assert.match(configure(), /Updated:/);
            const instructions = await readFile(join(project, 'AGENTS.md'), 'utf8');
            assert.match(instructions, /^# Customer rules/);
            // Local sources do not model a remote update endpoint; reinstall changed content to test replacement.
            const context = join(source, 'skills/wj-project-context/SKILL.md');
            await writeFile(context, (await readFile(context, 'utf8')) + '\nUpdated distribution fixture.\n');
            run(args);
            for (const destination of destinations) assert.match(await readFile(join(destination, 'wj-project-context/SKILL.md'), 'utf8'), /Updated distribution fixture/);
            assert.equal(await readFile(join(project, 'AGENTS.md'), 'utf8'), instructions);
            assert.match(configure(), /already up to date/);
            console.log(`Verified ${label}: ${manifest.skills.length} skills, three agents, replacement and preserved project instructions.`);
        }
    }
} finally {
    await rm(scratch, { recursive: true, force: true });
}
