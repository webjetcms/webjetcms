---
name: wj-docs-pr
description: 'Updates the documentation in the currently open Markdown file based on Git changes. Use when: updating docs from PR changes, documenting all commits in a PR, syncing markdown documentation with local uncommitted edits or merged/open PR code changes.'
argument-hint: 'Optionally specify which part of the documentation the changes apply to (e.g. "MetadataCleaner" or "page templates").'
---

# wj-docs-pr

Analyzes Git changes and updates the content of the currently open Markdown document in Slovak.

Use the target project's actual PR base, documentation layout and release conventions. `origin/main` in the command examples is a placeholder: resolve the PR base or remote default branch before comparing. In a customer project, do not require or create core changelog/roadmap files. Use `wj-project-context` when available to distinguish shared WebJET conventions from core-only workflows. Resolve paths against the target project root, not the installed skill directory.

## When to use

- You want to document changes introduced by the current PR before merging
- You want to document uncommitted (staged/unstaged) local changes
- The open Markdown file is out of date with the current state of the code

## Procedure

### 1. Determine the scope of changes

**Ask the user:**

> "Chcete zdokumentovať zmeny z celého pull requestu (všetky commity vetvy), alebo len necommitnuté lokálne zmeny?"

Based on the answer, use one of the two modes:

#### Mode A — Pull Request (all commits in branch)

```bash
git rev-parse --abbrev-ref HEAD
git diff --name-only $(git merge-base HEAD origin/main)
```

For each file:
```bash
git diff $(git merge-base HEAD origin/main) -- <file>
```

#### Mode B — Uncommitted changes only (staged + unstaged)

```bash
git diff --name-only
git diff --cached --name-only
git status --short
```

For each file:
```bash
git diff <file>
git diff --cached <file>
```

Collect in both modes:

- New files
- Modified files
- Deleted files

### 2. Extract the ticket number

```bash
git rev-parse --abbrev-ref HEAD
```

Branch format: `feature/XXXX-description` or `hotfix/XXXX-description` — the ticket number is `XXXX` (digits between `/` and the first `-`).

### 3. Examine the content of the changes

For each relevant file (Java, JS, HTML templates, configuration) search for:

- New public methods, classes, REST endpoints
- Changed parameters or behavior of existing functions
- New configuration keys or properties
- Added / removed dependencies
- Bugfixes

### 4. Read the currently open Markdown file

Read the entire contents of the currently open `.md` file to understand its structure and context.

Identify the sections that relate to the changed files (e.g. by class, module, function name).

### 5. Suggest and write updates

Update the documentation directly in the open Markdown file:

- **Language**: Slovak only
- **Style**: Concise, technical, understandable for the developer
- **Format**: Preserve the existing document structure (sections, headings, lists)
- **Scope**: Edit only sections related to the changes, do not delete irrelevant parts

Update types:
- Add a description of a new feature to the relevant section
- Edit the description of the method/class whose behavior has changed
- Add new configuration options to a table or list
- Mark fixed bugs if relevant to the user

### 6. Update the CHANGELOG

If the target project requires a changelog, locate its current release file and section. In core this is the corresponding `docs/sk/CHANGELOG-<year>.md` file. Do not assume a fixed year or create this layout in a customer project. Where the convention applies, add or append a record in the format:

```
- <brief description of the change in Slovak> (#XXXX)
```

where `#XXXX` is the ticket number from the branch name. If a record for this change already exists (without a ticket number), append `(#XXXX)` to the end of the line.

### 7. Quality Check

After the update, check:
- [ ] All relevant changes are covered or intentionally omitted
- [ ] Documentation is in Slovak
- [ ] File structure is preserved
- [ ] New parts are consistent with existing style
- [ ] The applicable project changelog is updated, with a ticket number only when known.

## Notes

- If the change is purely refactoring (no change in behavior), do not **edit** the documentation, just inform the user. For visible changes or fixes, update a changelog only when the project's conventions require it.
- If the current Markdown file does not contain a section for changed classes/functions, suggest where to add it.
- If the branch does not have a standard format (e.g. `main`, `develop`), omit the ticket number and notify the user.


## When to use

- You want to document all changes introduced by the current PR before merging
- The open Markdown file is out of date with the changes in the current branch
- You are finalizing a PR and want to keep the documentation in sync

## Procedure

### 1. Identify the current branch and PR base

```bash
git rev-parse --abbrev-ref HEAD
git merge-base HEAD origin/main
```

Extract the ticket number from the branch name — format is `feature/XXXX-description` or `hotfix/XXXX-description`, the ticket number is `XXXX` (digits between `/` and the first `-`).

### 2. Get the list of files changed in the PR

Compare the current branch against the merge-base (not against `origin/main` directly, to avoid including unrelated commits):

```bash
git diff --name-only $(git merge-base HEAD origin/main)
```

Collect:

- New files (`A`)
- Modified files (`M`)
- Deleted files (`D`, `R`)

### 3. Examine the content of the changes

For each relevant file (Java, JS, HTML templates, configuration) find **what changed**:

```bash
git diff $(git merge-base HEAD origin/main) -- <file>
```

Search for:

- New public methods, classes, REST endpoints
- Changed parameters or behavior of existing functions
- New configuration keys or properties
- Added / removed dependencies
- Bugfixes

### 4. Read the currently open Markdown file

Read the entire contents of the currently open `.md` file to understand its structure and context.

Identify the sections that relate to the changed files (e.g. by class, module, function name).

### 5. Suggest and write updates

Update the documentation directly in the open Markdown file:

- **Language**: Slovak only
- **Style**: Concise, technical, understandable for the developer
- **Format**: Preserve the existing document structure (sections, headings, lists)
- **Scope**: Edit only sections related to the changes, do not delete irrelevant parts

Update types:
- Add a description of a new feature to the relevant section
- Edit the description of the method/class whose behavior has changed
- Add new configuration options to a table or list
- Mark fixed bugs if relevant to the user

### 6. Update the CHANGELOG

Find the ticket number from the name of the current git branch:

```bash
git rev-parse --abbrev-ref HEAD
```

The branch has the format `feature/XXXX-description` or `hotfix/XXXX-description` — the ticket number is `XXXX` (the digits between `/` and the first `-`).

If the target project requires a changelog, locate its current release file and section. In core this is the corresponding `docs/sk/CHANGELOG-<year>.md` file. Do not assume a fixed year or create this layout in a customer project. Where the convention applies, add or append a record in the format:

```
- <brief description of the change in Slovak> (#XXXX)
```

where `#XXXX` is the ticket number from the branch name. If a record for this change already exists (without a ticket number), append `(#XXXX)` to the end of the line.

### 7. Quality Check

After the update, check:
- [ ] All relevant changes from `git diff $(git merge-base HEAD origin/main)` are covered or intentionally omitted
- [ ] Documentation is in Slovak
- [ ] File structure is preserved
- [ ] New parts are consistent with existing style
- [ ] The applicable project changelog is updated, with a ticket number only when known.

## Notes

- If the change is purely refactoring (no change in behavior), do not **edit** the documentation, just inform the user. For visible changes or fixes, update a changelog only when the project's conventions require it.
- If the current Markdown file does not contain a section for changed classes/functions, suggest where to add it.
- If the branch does not have a standard format (e.g. `main`, `develop`), omit the ticket number and notify the user.
- Unlike `wj-docs-uncommited`, this skill covers **all committed changes in the PR branch**, not just local unstaged/staged edits.
