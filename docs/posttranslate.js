#!/usr/bin/env node

const path = require('path');
const fg = require('fast-glob');

let replaceInFileFn;

async function getReplaceInFile() {
  if (!replaceInFileFn) {
    const module = await import('replace-in-file');
    replaceInFileFn = module.replaceInFile;
  }

  return replaceInFileFn;
}

/**
 * Add new replacement rules here.
 * from can be string or RegExp, to can be string or function.
 * files is optional; when omitted, DEFAULT_FILES is used.
 */
const DEFAULT_FILES = ['cs/**/*.md', 'en/**/*.md'];

const fixes = [
  {
    from: /\/src\/main\/java\/(cs|en)\/iway\//g,
    to: '/src/main/java/sk/iway/',
  },
  {
    from: 'manuals/WebJET_pro_administratorů.docx',
    to: 'manuals/WebJET_pre_administratorov.docx'
  },
  {
    from: 'manuals/WebJET_pro_redaktorů.docx',
    to: 'manuals/WebJET_pre_redaktorov.docx'
  },
  { from: 'Zpráva indexovaných dat', to: 'Správa indexovaných dat' },
];

async function applyFix(fix) {
  const replaceInFile = await getReplaceInFile();
  const filePatterns = Array.isArray(fix.files) ? fix.files : DEFAULT_FILES;
  const files = await fg(filePatterns, {
    cwd: __dirname,
    absolute: true,
    onlyFiles: true,
  });

  const ruleLabel = typeof fix.comment === 'string' && fix.comment.length > 0
    ? fix.comment
    : String(fix.from);

  if (files.length === 0) {
    console.log(`- ${ruleLabel}: no matching files`);
    return { filesChanged: 0 };
  }

  const results = await replaceInFile({
    files,
    from: fix.from,
    to: fix.to,
    countMatches: true,
  });

  let filesChanged = 0;
  let replacements = 0;

  for (const result of results) {
    if (result.hasChanged) {
      filesChanged += 1;
    }
    if (typeof result.numMatches === 'number') {
      replacements += result.numMatches;
    }
  }

  if (replacements === 0) {
    console.log(`- ${ruleLabel}: no changes`);
    return { filesChanged, replacements };
  }

  console.log(`- ${ruleLabel}: ${replacements} replacements in ${filesChanged} files`);
  return { filesChanged, replacements };
}

async function run() {
  console.log('Running post-translation fixes...');

  let totalFilesChanged = 0;
  let totalReplacements = 0;

  for (const fix of fixes) {
    const { filesChanged, replacements } = await applyFix(fix);
    totalFilesChanged += filesChanged;
    totalReplacements += replacements || 0;
  }

  console.log(
    `Done. Total replacements: ${totalReplacements}, files changed: ${totalFilesChanged}`
  );
}

run().catch((error) => {
  const relativePath = path.relative(process.cwd(), __filename);
  console.error(`Post-translation fixes failed in ${relativePath}:`, error);
  process.exit(1);
});
