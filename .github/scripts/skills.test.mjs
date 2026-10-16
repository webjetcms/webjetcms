import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtemp, mkdir, readFile, writeFile, rm, readdir, symlink } from 'node:fs/promises';
import { tmpdir, homedir } from 'node:os';
import { dirname, join } from 'node:path';
import test from 'node:test';
import { exportSkills } from './export-skills.mjs';
import { setupProject, updateBlock } from '../../.agents/skills/wj-setup/scripts/setup-project.mjs';

async function temporary(t) {
    const root = await mkdtemp(join(tmpdir(), 'webjet-skills-test-'));
    t.after(() => rm(root, { recursive: true, force: true }));
    return root;
}

async function put(root, path, content) {
    await mkdir(dirname(join(root, path)), { recursive: true });
    await writeFile(join(root, path), content);
}

async function sourceFixture(t) {
    const root = await temporary(t);
    const resources = {
        '.agents/skills/wj-project-context/SKILL.md': '---\nname: wj-project-context\ndescription: Read WebJET context.\n---\n# Context\n',
        '.agents/skills/wj-setup/SKILL.md': '---\nname: wj-setup\ndescription: Configure WebJET instructions.\n---\n# Setup\n',
        '.agents/skills/wj-example/SKILL.md': '---\nname: wj-example\ndescription: Demonstrate a workflow.\n---\n[Bundled](references/guide.md)\n[Architecture](../../../.github/agents/AGENTS.md)\n[Core](../../../src/example.java)\n',
        '.agents/skills/wj-example/references/guide.md': '# Guide\n',
        '.agents/skills/wj-example/scripts/helper.py': 'print("example")\n',
        '.agents/skills/wj-example/agents/openai.yaml': 'interface:\n  display_name: "Example"\n',
        '.agents/skills/wj-example/assets/example.txt': 'Asset\n',
        '.github/agents/AGENTS.md': '[System](AGENTS-system.md)\n',
        '.github/agents/AGENTS-system.md': '# System\n',
        '.github/skills-distribution/README.md': '# Distribution\n[License](LICENSE.md)\n',
        'LICENSE.md': 'Original licensing and attribution\n',
        'src/example.java': 'class Example {}\n'
    };
    for (const [path, content] of Object.entries(resources)) await put(root, path, content);
    execFileSync('git', ['init', '--quiet', root]);
    execFileSync('git', ['-C', root, 'add', '--all']);
    return root;
}

test('export includes tracked resources and knowledge, rewrites links, and excludes local skills and core source', async (t) => {
    const root = await sourceFixture(t);
    await put(root, '.agents/skills/private-local/SKILL.md', 'Private untracked instructions');
    const output = join(root, 'export');
    const result = await exportSkills({ root, output, revision: 'a'.repeat(40) });
    assert.deepEqual(result.skills, ['wj-example', 'wj-project-context', 'wj-setup']);
    assert.deepEqual((await readdir(output)).sort(), ['LICENSE.md', 'README.md', 'skills', 'source.json']);
    assert.equal(await readFile(join(output, 'skills/wj-example/assets/example.txt'), 'utf8'), 'Asset\n');
    assert.equal(await readFile(join(output, 'skills/wj-example/scripts/helper.py'), 'utf8'), 'print("example")\n');
    const skill = await readFile(join(output, 'skills/wj-example/SKILL.md'), 'utf8');
    assert.match(skill, /\.\.\/wj-project-context\/references\/AGENTS\.md/);
    assert.match(skill, new RegExp(`https://github.com/webjetcms/webjetcms/blob/${'a'.repeat(40)}/src/example.java`));
    assert.equal(await readFile(join(output, 'LICENSE.md'), 'utf8'), 'Original licensing and attribution\n');
    assert.deepEqual(JSON.parse(await readFile(join(output, 'source.json'), 'utf8')), result);
});

test('export is reproducible, content hashes ignore source-only revisions, and removed skills disappear', async (t) => {
    const root = await sourceFixture(t);
    const first = await exportSkills({ root, output: join(root, 'first'), revision: 'a'.repeat(40) });
    const second = await exportSkills({ root, output: join(root, 'second'), revision: 'b'.repeat(40) });
    assert.equal(first.contentHash, second.contentHash);
    await put(root, '.github/agents/AGENTS-system.md', '# Updated system\n');
    const changed = await exportSkills({ root, output: join(root, 'changed'), revision: 'b'.repeat(40) });
    assert.notEqual(changed.contentHash, first.contentHash);
    execFileSync('git', ['-C', root, 'rm', '--cached', '-r', '.agents/skills/wj-example']);
    const removed = await exportSkills({ root, output: join(root, 'removed'), revision: 'b'.repeat(40) });
    assert.deepEqual(removed.skills, ['wj-project-context', 'wj-setup']);
    await assert.rejects(exportSkills({ root, output: join(root, 'removed'), revision: 'b'.repeat(40) }), { code: 'EEXIST' });
});

test('invalid metadata and broken links fail before producing a distribution', async (t) => {
    const root = await sourceFixture(t);
    const path = '.agents/skills/wj-example/SKILL.md';
    await put(root, path, '---\nname: wrong-name\ndescription: Example.\n---\n');
    await assert.rejects(exportSkills({ root, output: join(root, 'invalid'), revision: 'a'.repeat(40) }), /Invalid skill/);
    await put(root, path, '---\nname: wj-example\ndescription: Example.\n---\n[Missing](references/missing.md)\n');
    await assert.rejects(exportSkills({ root, output: join(root, 'broken'), revision: 'a'.repeat(40) }), /Broken distribution link/);
    await assert.rejects(readFile(join(root, 'broken/source.json')), { code: 'ENOENT' });
});

test('fresh project setup creates relative integration for all three agents and is idempotent', async (t) => {
    const root = await temporary(t);
    await put(root, '.agents/skills/wj-project-context/SKILL.md', '# Context');
    const files = await setupProject({ project: root });
    assert.deepEqual(files, ['AGENTS.md', '.github/copilot-instructions.md', 'CLAUDE.md']);
    assert.match(await readFile(join(root, 'AGENTS.md'), 'utf8'), /\]\(\.agents\/skills\/wj-project-context\/SKILL.md\)/);
    assert.match(await readFile(join(root, 'CLAUDE.md'), 'utf8'), /@AGENTS.md/);
    assert.deepEqual(await setupProject({ project: root }), []);
});

test('setup preserves custom content and CRLF, handles an override, and migrates scope without personal paths', async (t) => {
    const root = await temporary(t);
    await put(root, '.claude/skills/wj-project-context/SKILL.md', '# Context');
    const custom = '# Customer rules\r\n\r\nUse our migration mechanism.\r\n';
    await put(root, 'AGENTS.md', custom);
    await put(root, 'AGENTS.override.md', 'Override rules stay here.\n');
    await put(root, 'CLAUDE.md', 'Keep Claude instructions.\n');
    await setupProject({ project: root });
    const local = await readFile(join(root, 'AGENTS.md'), 'utf8');
    assert.ok(local.startsWith(custom));
    assert.equal(local.replaceAll('\r\n', '').includes('\n'), false);
    assert.match(local, /\.claude\/skills\/wj-project-context/);
    await setupProject({ project: root, scope: 'global' });
    const global = await readFile(join(root, 'AGENTS.md'), 'utf8');
    assert.ok(global.startsWith(custom));
    assert.ok(!global.includes(root) && !global.includes(homedir()));
    assert.match(global, /active skill catalog/);
    assert.match(await readFile(join(root, 'AGENTS.override.md'), 'utf8'), /^Override rules stay here\./);
    assert.match(await readFile(join(root, 'CLAUDE.md'), 'utf8'), /^Keep Claude instructions\./);
    assert.deepEqual(await setupProject({ project: root, scope: 'global' }), []);
    const withSuffix = 'Before\n<!-- webjet-skills:start -->\nOld guidance\n<!-- webjet-skills:end -->\nAfter\n';
    assert.equal(updateBlock(withSuffix, 'New guidance'), withSuffix.replace('Old guidance', 'New guidance'));
});

test('malformed blocks fail before any file changes, including when the bad file is last', async (t) => {
    const root = await temporary(t);
    const malformed = '# Custom\n<!-- webjet-skills:start -->\nNever erase this.\n';
    await put(root, 'AGENTS.override.md', malformed);
    await assert.rejects(setupProject({ project: root, scope: 'global' }), /AGENTS.override.md: Malformed/);
    assert.deepEqual(await readdir(root), ['AGENTS.override.md']);
    assert.equal(await readFile(join(root, 'AGENTS.override.md'), 'utf8'), malformed);
    for (const text of [
        '<!-- webjet-skills:end -->\n<!-- webjet-skills:start -->',
        '<!-- webjet-skills:start -->\n<!-- webjet-skills:end -->\n<!-- webjet-skills:start -->\n<!-- webjet-skills:end -->',
        '<!-- webjet-skills:broken -->',
        '<!-- webjet-skills-start -->',
        'Inline <!-- webjet-skills:start --> example <!-- webjet-skills:end -->',
        '```md\n<!-- webjet-skills:start -->\nExample only\n<!-- webjet-skills:end -->\n```',
        'Unclosed example:\n```md\nKeep this code'
    ]) assert.throws(() => updateBlock(text, 'Replacement'), /Malformed/);
});

test('dry run does not write and missing local context is reported', async (t) => {
    const root = await temporary(t);
    await assert.rejects(setupProject({ project: root }), /Install wj-project-context/);
    assert.equal((await setupProject({ project: root, scope: 'global', dryRun: true })).length, 3);
    assert.deepEqual(await readdir(root), []);
    await assert.rejects(setupProject({ project: homedir(), scope: 'global' }), /home or filesystem root/);
});

test('setup refuses symlinked instruction directories without touching outside files', async (t) => {
    const root = await temporary(t);
    const outside = await temporary(t);
    await symlink(outside, join(root, '.github'), process.platform === 'win32' ? 'junction' : 'dir');
    await assert.rejects(setupProject({ project: root, scope: 'global' }), /symlink/);
    assert.deepEqual(await readdir(outside), []);
    await assert.rejects(readFile(join(root, 'AGENTS.md')), { code: 'ENOENT' });
});
