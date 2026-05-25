---
name: wj-translate-comments
description: 'Translates code comments from Slovak to English without changing runtime behavior. Use when: code contains Slovak comments, reviewers request English-only comments, cleaning legacy comments before merge, standardizing documentation language in source files and tests.'
argument-hint: 'Optionally specify target and strictness, for example: "attached file", "files in current PR", "only modified files", "preserve original terminology", or "skip generated files".'
---

# wj-translate-comments

Converts Slovak comments in code to clear English while preserving technical meaning, formatting, and project conventions.

## When to Use

- You see Slovak comments in Java, JS/TS, HTML, CSS/SCSS, SQL, YAML, or test files
- A pull request must meet English-only comment conventions
- You are normalizing legacy modules before broader refactoring
- Review feedback requests better international readability

## Procedure

### 1. Resolve Target Files (Primary Goal)

Use this priority order:

- If a file is explicitly attached in chat, process that attached file first.
- Otherwise process files changed in the active pull request.
- If neither is available, ask the user for explicit scope.

For pull request mode, focus on changed files only.

### 2. Confirm Scope and Safety Constraints

If the user did not specify scope, ask for one:

- only changed files
- selected directories
- entire repository

Always apply these constraints:

- translate comments only
- do not rename symbols, change logic, or alter string literals used at runtime
- keep formatting, indentation, and line wrapping style

### 3. Identify Candidate Files

Start from the requested scope and prioritize files likely to contain comments:

- source files in src/main and src/test
- templates and admin/frontend assets
- config files with inline comments

Exclude by default:

- generated files
- minified assets
- vendor or third-party code
- binary files

### 4. Detect Slovak Comments

Find comments likely written in Slovak and classify by style:

- line comments
- block comments
- JavaDoc and documentation blocks
- inline trailing comments

Decision point:

- If confidence is low (mixed language or ambiguous term), keep the original and ask the user for preferred wording.
- If confidence is high, translate directly.

### 5. Translate with Meaning Preservation

For each comment:

- preserve intent and domain meaning, not literal word-by-word translation
- keep domain names, class names, config keys, and product terms unchanged
- keep placeholders, URLs, tags, and code fragments unchanged
- keep TODO/FIXME semantics and priority markers unchanged
- keep original leading/trailing whitespace on every comment line
- in JavaDoc/block comments, preserve the exact prefix structure (`/**`, `*`, `*/`) and alignment

Preferred style:

- short, direct English
- action-oriented phrasing for TODO comments
- grammar and punctuation suitable for technical docs

Whitespace guard example (JavaDoc):

Before:
```java
	/**
	 * Zoznam podporovanych jazykov oddelenych ciarkou.
	 * Predvolene: sk,cs,en
	 */
```

After (correct):
```java
	/**
	 * Comma-separated list of supported languages.
	 * Default: sk,cs,en
	 */
```

After (incorrect - do not do this):
```java
	/**
		* Comma-separated list of supported languages.
		* Default: sk,cs,en
	 */
```

### 6. Apply Edits Carefully

Update only comment text in place.

Preserve comment formatting exactly:

- do not change indentation depth of comment lines
- do not add/remove spaces before `*` in JavaDoc lines
- do not reflow wrapping unless explicitly requested
- do not normalize tabs to spaces or spaces to tabs

Do not change:

- executable code
- identifiers
- method signatures
- API paths
- test assertions and expected values unless they are comments

### 7. Run Validation Checks

After editing:

- verify no behavioral diff outside comments
- run lint/compile/tests if requested or when risk is non-trivial
- ensure no accidental encoding or formatting damage
- for each edited block comment/JavaDoc, compare line prefixes and leading whitespace with the original; only comment text should differ

### 8. Report Results

Summarize:

- files updated
- representative before/after examples
- unclear translations that need user confirmation
- any skipped files and why
- target resolution used (attached file or PR file list)

## Quality Criteria

A translation pass is complete when all criteria are true:

- all Slovak comments in scope are translated or explicitly marked for clarification
- code behavior is unchanged
- formatting and comment structure are preserved
- terminology is consistent across files
- comments are understandable to an English-speaking maintainer

## Fallback Rules

- If a comment is domain-sensitive and ambiguous, propose 2-3 English variants and ask user to choose.
- If comments mix Slovak and English, normalize to English but keep proper nouns unchanged.
- If legal or compliance text appears in comments, request confirmation before rewriting nuanced wording.
