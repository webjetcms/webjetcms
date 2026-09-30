---
name: wj-simplify-implementation
description: Simplify WebJET CMS implementation changes in a PR or uncommitted code by removing unnecessary complexity while preserving behavior. Use when asked to simplify an implementation or reduce overengineering, rather than for general code review or performance tuning.
---

# Simplify Implementation

Make the selected implementation easier to understand and maintain. Apply justified simplifications directly, unless the user asks only for suggestions or review.

## Select the scope

- Accept `PR` (optionally with a PR number or URL) or `uncommitted`; also accept the spelling `uncommited` and the alias `local`.
- Use the mode already specified in the request or conversation. Otherwise ask whether the user wants to simplify the PR or uncommitted changes, and wait for the answer.
- **Uncommitted:** inspect staged and unstaged diffs and relevant untracked files using `git diff`, `git diff --cached`, and `git status --short`.
- **PR:** inspect all changes from the merge base to the PR head, not just the latest commit. Resolve the actual PR base and head when available. For the current branch without PR metadata, follow repository base-branch conventions (`origin/hotfix/2026.0-main` for `hotfix/*`, otherwise `origin/main`). Compare explicitly against the head commit so unrelated working-tree changes are excluded. If the base is missing or ambiguous, ask rather than guessing.
- Before editing a PR, verify that the local checkout matches its head. Preserve existing local edits; if they overlap and cannot be safely separated, clarify before editing. Do not reset or stash the user's work automatically.

## Find unnecessary complexity

Read the changed code, relevant callers and tests, and applicable repository instructions. Focus on complexity introduced or modified within the selected scope:

- Overengineering or abstractions without a concrete need.
- Excessive validation, repeated safety checks, fallbacks, or defensive branches.
- Handling of unrealistic or unsupported edge cases.
- Duplicate checks or layers with no clear responsibility.
- Code made harder to understand for minor practical benefits.

Before removing a check or fallback, establish why it is unnecessary from actual contracts, callers, or existing guarantees. Keep required input validation, permissions, domain isolation, backward compatibility, and supported error handling. Lack of test coverage alone does not prove a case is unsupported.

## Simplify and verify

Prefer the smallest clear implementation that preserves intended behavior, public contracts, and repository conventions. Reuse existing helpers when they reduce complexity. Avoid unrelated refactors, speculative optimizations, new frameworks, or reducing line count at the expense of readability.

Apply clear improvements without an extra approval step. Leave uncertain candidates unchanged and briefly explain the uncertainty. If no worthwhile simplification exists, say so without manufacturing changes.

Check the resulting diff and run relevant existing tests or checks appropriate to the changes. Add or adjust tests only when needed to verify meaningful behavior. Report checks that could not be run. Leave changes uncommitted and preserve the staging state. Never create commits; pushing or publishing PR comments requires a separate user request.

Summarize in the user's language what was simplified, why it helps, which files changed, and how behavior was verified. Mention unresolved candidates only when useful.
