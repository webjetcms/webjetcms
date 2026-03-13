---
name: wj-docs-pr
description: 'Updates the documentation in the currently open Markdown file based on current pull request changes in the Git repository. Use when: updating docs from PR changes, documenting all commits in a PR, syncing markdown documentation with merged or open PR code changes.'
argument-hint: 'Optionally specify which part of the documentation the changes apply to (e.g. "MetadataCleaner" or "page templates").'
---

# wj-docs-pr

Analyzes changes introduced in the current pull request (compared to the default branch) and updates the content of the currently open Markdown document in Slovak.

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

Open the file `/docs/sk/CHANGELOG-2026.md` and find the section `## 2026.0-SNAPSHOT`. Add or append a record in the format:

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
- [ ] CHANGELOG contains an entry with ticket number `(#XXXX)`

## Notes

- If the change is purely refactoring (no change in behavior), do not **edit** the documentation, just inform the user. However, update the CHANGELOG whenever there is a visible change in behavior or a fix.
- If the current Markdown file does not contain a section for changed classes/functions, suggest where to add it.
- If the branch does not have a standard format (e.g. `main`, `develop`), omit the ticket number and notify the user.
- Unlike `wj-docs-uncommited`, this skill covers **all committed changes in the PR branch**, not just local unstaged/staged edits.
