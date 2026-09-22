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

Treat every change in the selected PR or uncommitted diff as intentional scope unless the user explicitly narrows it. A PR may legitimately contain several features and small fixes that are not mentioned in its title, issue, or the original request. Do not delete, revert, or weaken a change merely because it appears unrelated to the main topic or is absent from the written assignment. Review such a change on its own merits and simplify it only when doing so preserves its intended behavior. If its purpose cannot be established from the code, callers, tests, or repository context, leave it unchanged rather than guessing.

## Find unnecessary complexity

Read the changed code, relevant callers and tests, and applicable repository instructions. Focus on complexity introduced or modified within the selected scope:

- Overengineering or abstractions without a concrete need.
- Excessive validation, repeated safety checks, fallbacks, or defensive branches.
- Handling of unrealistic or unsupported edge cases.
- Duplicate checks or layers with no clear responsibility.
- Code made harder to understand for minor practical benefits.

Before removing a check, fallback, branch, or other behavior, establish why it is unnecessary from actual contracts, callers, existing guarantees, or clear redundancy within that change. Relevance to the PR's main topic is not evidence that code is unnecessary. Keep required input validation, permissions, domain isolation, backward compatibility, and supported error handling. Lack of test coverage or omission from the original assignment does not prove a case is unsupported.

## Simplify and verify

Prefer the smallest clear implementation that preserves intended behavior, public contracts, and repository conventions. Reuse existing helpers when they reduce complexity. Avoid unrelated refactors, speculative optimizations, new frameworks, or reducing line count at the expense of readability.

Apply clear improvements without an extra approval step. Leave uncertain candidates unchanged and briefly explain the uncertainty. If no worthwhile simplification exists, say so without manufacturing changes.

Check the resulting diff and run relevant existing tests or checks appropriate to the changes. Add or adjust tests only when needed to verify meaningful behavior. Report checks that could not be run. Leave changes uncommitted and preserve the staging state. Never create commits; pushing or publishing PR comments requires a separate user request.

## Report the result

Report in the user's language and lead with the outcome. Do not stop at a generic list of edited files or short statements such as "removed unnecessary code." For every material simplification, or for a clearly identified group of equivalent mechanical edits, explain:

- **What changed:** describe the relevant before-and-after implementation, without pasting a large diff.
- **Why:** identify the unnecessary complexity and the concrete code contracts, callers, guarantees, or duplication that made the simplification safe.
- **What improved:** state the practical benefit, such as a clearer control flow, fewer duplicated decisions, a smaller maintenance surface, or easier verification. Avoid unsupported claims about performance or correctness.
- **Where:** name every affected repository-relative file and the relevant Java class plus method, or the equivalent symbol for non-Java code.
- **Verification:** state which behavior or contract was preserved and list the tests or checks that verified it, including anything that could not be run.

When no worthwhile simplification exists, explain that conclusion and what was inspected. Mention uncertain candidates that were deliberately preserved when that helps the user understand why no edit was made. Also call out any scoped change that looked separate from the PR's main topic and was intentionally retained; do not treat its presence as a defect.
