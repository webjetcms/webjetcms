---
name: wj-code-review
description: 'Reviews code changes for correctness, backward compatibility, and code quality. Use when: reviewing staged or unstaged changes before commit, reviewing all changes in a pull request before merge, checking backward compatibility of modifications, performing code review of local changes, analyzing new code patterns and suggesting improvements.'
argument-hint: 'Optionally specify which files or area to focus the review on (e.g. "REST controllers" or "JPA entities"). You can also specify the mode: "PR" to review all commits in the current branch, or "local" to review only uncommitted changes.'
---

# wj-code-review

Performs a read-only code review of changes in the repository — either all commits in the current branch (PR mode) or only uncommitted local changes. Reports findings, risks, and improvement suggestions without modifying any files.

## When to Use

- Before committing changes to verify correctness and backward compatibility
- Before merging a pull request to review all changes in the branch
- When you want a second opinion on your code changes
- To check for common anti-patterns, security issues, and potential regressions
- To analyze new code for quality and adherence to project conventions

## Procedure

### 1. Determine the Scope of Changes

**Ask the user (if not already specified):**

> "Do you want to review all commits in the current branch (PR mode), or only uncommitted local changes?"

Based on the answer, use one of the two modes:

When running branch-level review commands in this skill, resolve the comparison base as follows:

- Branches starting with `hotfix/` -> `origin/hotfix/2026.0-main`
- All other branches -> `origin/main`

#### Mode A — Pull Request (all commits in branch)

```bash
BASE_BRANCH=origin/main
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ "$CURRENT_BRANCH" == hotfix/* ]]; then BASE_BRANCH=origin/hotfix/2026.0-main; fi
git diff --name-only $(git merge-base HEAD $BASE_BRANCH)
```

For each file:
```bash
git diff $(git merge-base HEAD $BASE_BRANCH) -- <file>
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

In both modes, categorize the files:

- **Modified** (`M`) — existing files with changes
- **Added** (`A`, `??`) — new files
- **Deleted** (`D`) — removed files
- **Renamed** (`R`) — moved/renamed files

### 2. Examine Each Change in Detail

Get the full diff for each changed file using the appropriate command from Step 1 (Mode A or Mode B).

For new (untracked) files, read the entire file content.

### 3. Backward Compatibility Analysis

For each **modified** file, check:

- **Public API changes**: Were method signatures, return types, or parameter types changed? Could callers break?
- **Database schema**: Were JPA entity fields added/removed/renamed? Are there corresponding Liquibase/migration scripts?
- **REST endpoints**: Were URL paths, request/response DTOs, or HTTP methods changed? Could frontend or external consumers break?
- **Configuration keys**: Were property names changed or removed? Could existing deployments fail?
- **Interface/abstract class changes**: Were contracts changed that require updates in all implementations?
- **Behavioral changes**: Does existing logic now produce different results for the same inputs?

### 4. Correctness Analysis

For each change, verify:

- **Null safety**: Are new nullable values properly handled? No unguarded dereferences?
- **Edge cases**: Does the logic handle empty collections, null inputs, boundary values?
- **Transaction boundaries**: Are database operations properly transactional? Could partial failures leave inconsistent state?
- **Thread safety**: Are shared mutable resources properly synchronized?
- **Resource management**: Are streams, connections, and other resources properly closed (try-with-resources)?
- **Error handling**: Are exceptions caught at the right level? Are error messages meaningful?
- **SQL/JPQL correctness**: Are queries correct? Are parameters properly bound (no injection risk)?

### 5. Code Quality & Pattern Analysis

Review new code for:

- **Readability**: Avoid nested ternary operators. Prefer clear if/else blocks.
- **Naming**: Are variable/method/class names descriptive and consistent with the codebase?
- **DRY principle**: Is there duplicated logic that should be extracted?
- **Single Responsibility**: Do methods/classes do one thing well?
- **Project conventions**: Follow patterns from AGENTS.md — use `Constants.get*` for config, `CloudToolsForCore.getDomainName()` for domain, `DomainIdRepository` for domain-scoped persistence.
- **DataTable conventions**: If modifying DataTable-related code, consult AGENTS-datatable.md for proper patterns.
- **Test coverage**: Are there corresponding test changes for new/modified logic? Do JPA entity tests use Repository classes for save/load?

### 6. Security Review

Check for:

- **Injection vulnerabilities**: SQL injection, XSS, command injection
- **Access control**: Are endpoints properly secured? Are authorization checks in place?
- **Sensitive data exposure**: Are passwords, tokens, or secrets handled safely?
- **Input validation**: Is user input validated at system boundaries?

### 7. Report Findings

**DO NOT modify any files.** Only produce a written report.

Structure the report as follows:

#### Critical Issues
Problems that will likely cause bugs, data loss, or security vulnerabilities. These must be fixed before commit.

#### Backward Compatibility Concerns
Changes that could break existing functionality, API consumers, or deployments.

#### Suggestions for Improvement
Code quality improvements, better patterns, or minor issues that are not blocking but worth addressing.

#### Positive Observations
Good patterns, clean code, or improvements worth acknowledging.

#### Missing CHANGELOG / ROADMAP Entries
Flag if the CHANGELOG entry in `/docs/sk/CHANGELOG-2026.md` is missing for this branch's ticket number, or if a matching ROADMAP item is not checked off.

For each finding, include:
- **File and location** (line numbers or method names)
- **What was found** (clear description of the issue)
- **Why it matters** (impact or risk)
- **Suggested fix** (concrete recommendation, with code example if helpful)

### 8. Check CHANGELOG Entry

Find the ticket number from the current git branch:

```bash
git rev-parse --abbrev-ref HEAD
```

The branch has the format `feature/XXXX-description` or `hotfix/XXXX-description` — the ticket number is `XXXX` (the digits between `/` and the first `-`).

Check if `/docs/sk/CHANGELOG-2026.md` contains an entry with `(#XXXX)` in the `## 2026.0-SNAPSHOT` section. If not, report it as a missing CHANGELOG entry.

Also check `/docs/sk/ROADMAP.md` — if the change matches a planned item, verify it is checked off with `(#XXXX)`.

### 9. Final Summary

Provide a brief overall assessment:
- How many files were reviewed
- Overall risk level (low / medium / high)
- Whether the changes are safe to commit or need further work
- Whether CHANGELOG entry is present

## Rules

- NEVER modify any files — this is a read-only review
- Write all findings in the language the user used (Slovak if the user wrote in Slovak, English otherwise)
- Follow the project's coding standards from copilot-instructions.md and AGENTS.md
- Focus on real issues, not style nitpicks (unless they violate project conventions)
- Be specific — always reference exact files, line numbers, and code snippets
- Consider the broader impact of changes on the rest of the system
