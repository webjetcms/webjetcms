---
name: wj-docs-uncommited
description: 'Updates the documentation in the currently open Markdown file based on uncommitted changes in the Git repository. Use when: updating docs from uncommitted changes, documenting staged or unstaged code changes, syncing markdown documentation with local code edits.'
argument-hint: 'Optionally specify which part of the documentation the changes apply to (e.g. "MetadataCleaner" or "page templates").'
---

# wj-docs-uncommited

Analyzes uncommitted changes in the repository and updates the content of the currently open Markdown document in Slovak.

## When to use

- You want to document new features or fixes before committing
- The open Markdown file is out of date with the current state of the code
- You are preparing a PR and want to keep the documentation in sync with the changes

## Procedure

### 1. Find uncommitted changes

Run the following commands to get a list of changed files:

```bash
git diff --name-only
git diff --cached --name-only
git status --short
```

Collect:

- New files (`??`, `A`)
- Modified files (`M`)
- Deleted files (`D`, `R`)

### 2. Examine the content of the changes

For each relevant file (Java, JS, HTML templates, configuration) find **what changed**:

```bash
git diff <file>
git diff --cached <file>
```

Search for:

- New public methods, classes, REST endpoints
- Changed parameters or behavior of existing functions
- New configuration keys or properties
- Added / removed dependencies
- Bugfixes

### 3. Read the currently open Markdown file

Read the entire contents of the currently open `.md` file to understand its structure and context.

Identify the sections that relate to the changed files (e.g. by class, module, function name).

### 4. Suggest and write updates

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

### 5. Update the CHANGELOG

Find the ticket number from the name of the current git branch:

```bash
git rev-parse --abbrev-ref HEAD
```

The branch has the format `feature/XXXX-description` or `hotfix/XXXX-description` — the ticket number is `XXXX` (the digits between `/` and the first `-`).

Open the file `/docs/sk/CHANGELOG-2026.md` and find the section `## 2026.0-SNAPSHOT`. Add or append a record in the format:

```
- <brief description of the change in Slovak> (#XXXX)
```

where `#XXXX` is the ticket number from the branch name. If a record for this change already exists (without a ticket number), append `(#XXXX)` to the end of the line.

### 6. Quality Check

After the update, check:
- [ ] All changes from `git diff` are covered or intentionally omitted
- [ ] Documentation is in Slovak
- [ ] File structure is preserved
- [ ] New parts are consistent with existing style
- [ ] CHANGELOG contains an entry with ticket number `(#XXXX)`

## Notes

- If the change is purely refactoring (no change in behavior), do not **edit** the documentation, just inform the user. However, update the CHANGELOG whenever there is a visible change in behavior or a fix.
- If the current Markdown file does not contain a section for changed classes/functions, suggest where to add it.
- If the branch does not have a standard format (e.g. `main`, `develop`), omit the ticket number and notify the user.