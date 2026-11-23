---
name: wj-add-javadoc
description: 'Add and review JavaDoc comments on Java classes and methods. Use when: adding missing class or method comments, auditing JavaDoc for wrong or missing @param/@return/@throws, ensuring English-only documentation, reviewing public API surface before merge. Skips trivial methods whose names are self-explanatory. Never adds inline comments inside method bodies.'
argument-hint: 'Optionally specify scope, for example: "attached file", "files changed in PR", "all files in package sk.iway.iwcm.components.forms".'
---

# wj-add-javadoc

Adds, corrects, and audits JavaDoc comments on Java classes and methods following project conventions. All output is in English.

## When to Use

- A class is missing a top-level comment explaining its purpose
- Methods have no JavaDoc or have outdated/inaccurate parameter descriptions
- A pull request introduces new public methods without documentation
- You need to audit an existing file for comment quality before code review

## Scope Rules

- **Add**: class-level and method-level JavaDoc (`/** ... */`) only
- **Skip**: constructor bodies, method bodies, field inline comments, lambda blocks
- **Skip trivial methods**: if a method is 2–8 lines AND the name unambiguously describes what it does (e.g. `getId()`, `setName()`, simple boolean checks), do not add JavaDoc
- **Always use English** — translate any existing non-English comments found
- **Do not restructure or rename** any code; comment changes only

## Procedure

### 1. Resolve Target Files

Priority order:

1. File explicitly attached in chat — process that file first
2. Files changed in the active pull request
3. Ask the user if neither is available

### 2. Read and Understand the File

Before writing any comment:

- Read the full class to understand its overall purpose and role
- Identify existing JavaDoc blocks — note which are missing, incomplete, or incorrect
- Note `@param` tags vs actual method parameters — flag mismatches

### 3. Evaluate What Needs Comments

For each class and method, apply these criteria:

| Target | Needs comment? |
|--------|---------------|
| Class / interface / enum | Always — describe purpose, typical use, and any important context |
| Public method with business logic | Yes — describe what it does, all params, return value, and checked exceptions |
| Public method 2–8 lines, name is self-explanatory | Skip |
| Protected / package-private method with non-obvious logic | Yes |
| Private method shorter than ~10 lines | Skip unless logic is complex |
| Simple getters / setters | Skip |
| `@Override` methods where the interface already documents | Skip unless behavior differs from the interface contract |

### 4. Write or Update JavaDoc

Follow standard JavaDoc conventions:

```java
/**
 * One-sentence summary of what the method does.
 *
 * Longer explanation if needed — include preconditions, side effects,
 * or notable behavior. Omit if the summary line is sufficient.
 *
 * @param paramName  description of what this parameter represents
 * @param anotherParam  description; note valid ranges or null-safety
 * @return description of the return value; note when null can be returned
 * @throws SomeException  condition under which this is thrown
 */
```

Rules:
- First sentence is a concise summary ending with a period
- `@param` entries must match the actual method signature — remove stale ones, add missing ones
- `@return` is required for non-void methods unless trivial (skip for simple getters)
- `@throws` is required for checked exceptions in the signature
- Do not use `@author` or `@version` unless the existing file already uses them
- Do not pad with obvious filler like "This method..." or "Gets the..."

### 5. Fix Existing Incorrect Comments

If an existing comment is found that:
- References a parameter that no longer exists → remove that `@param`
- Is missing a `@param` for a parameter that exists → add it
- Has a wrong return description → correct it
- Is in a non-English language → translate it

### 6. Validate

After all edits, verify:
- Every non-trivial class has a class-level JavaDoc
- Every targeted method has correct `@param`/`@return`/`@throws` coverage
- No comments were added inside method bodies
- No trivial methods were unnecessarily commented
- All text is in English
