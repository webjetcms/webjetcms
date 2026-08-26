---
name: wj-codeceptjs-check
description: 'Locate and rerun existing WebJET CMS CodeceptJS E2E tests from pasted Allure failure lists, locally or against production, then report results and failed scenario names per test file. Use after a fix or corrective deployment to verify previously failing tests; do not use to create or modify tests.'
---

# WebJET CodeceptJS Failure Recheck

Map failed Allure feature identifiers to existing CodeceptJS files, run each resolved file once against the user-selected target, and report the current result. This is a verification workflow: do not edit test files, add tags, install dependencies, or attempt fixes unless the user separately requests that work.

## Choose the Target Before Running Tests

If the current request does not already explicitly choose a target, ask in the user's language whether to run:

- **Local** with `npm run all`
- **Production** with `npm run all:prod`

Wait for the answer before invoking any `npm` test command. Do not infer production from deployment context. Production selection authorizes only the specifically resolved test files, not the full suite or automatic command retries. E2E scenarios may exercise write operations on the selected target.

Using `all:prod` is allowed by this skill only for an explicitly selected post-deployment production check. It is not the default for local branch verification.

## Normalize the Allure List

- Normalize ordinary and non-breaking whitespace.
- Treat Markdown-escaped underscores such as `\_` as literal underscores before matching. Apply this silently and do not add a normalization note to the report.
- Extract feature identifiers in their original order and deduplicate repeated identifiers.
- Ignore blank lines and entries containing only scenario numbers, such as `1`, `2`, or `1 3`. If a pasted row combines a feature and trailing scenario numbers, use the feature identifier and treat the numbers only as report context.
- Do not interpret the old scenario numbers as the result of the new run; the new CodeceptJS summary is authoritative.

## Resolve Exact Feature Matches

Search only JavaScript files below `src/test/webapp/tests`. Exclude every path containing a `screenshots` segment even if a future directory is added below `tests`.

A file matches only when the string value in its `Feature(...)` call equals the supplied identifier exactly. Support single or double quotes and optional whitespace around the argument. Do not match substrings and do not infer a path from the feature name or filename.

For example, `admin.character-encoding` resolves from:

```javascript
Feature('admin.character-encoding');
```

to:

```text
tests/admin/character-encoding.js
```

Use the path beginning with `tests/` for execution and reporting. If an identifier has no eligible match or more than one eligible match, report it as unresolved or ambiguous and do not run a guessed file. Never fall back to an `npm run all` or `npm run all:prod` command without a file argument.

When several identifiers resolve to the same file, run that file only once and preserve the order of its first occurrence.

## Preflight and Execute

Set the command working directory to the repository's `src/test/webapp` directory. Prefer the tool's `workdir` option instead of relying on the caller's current directory.

For every unique resolved path, run one command at a time:

```shell
# Local
npm run all tests/admin/character-encoding.js

# Production
npm run all:prod tests/admin/character-encoding.js
```

Before a production run, verify that `CODECEPT_DEFAULT_DOMAIN_NAME` is non-empty; otherwise the configured URL becomes invalid. Check any required authentication environment variables without displaying credential values. Report missing variable names only.

Run files sequentially so each process has an unambiguous result. Continue after an ordinary test failure. Do not rerun a failed command automatically. If a setup, dependency, configuration, or connectivity error clearly affects all remaining files, stop and mark the rest as not run rather than repeating the same failing operation.

Retain stdout, stderr, and the exit code from every invocation. If the command yields an ongoing tool session, poll it through process completion and the final summary; do not classify a result from an early or truncated output chunk.

## Classify Each Current Result

Use the exit code together with the final CodeceptJS console summary for that individual command. Do not count files in `build/test/allure-results`, because results from earlier runs may still be present.

- Exit code `0` with at least one passed test: `Passed (N passed)`.
- Non-zero exit code with a CodeceptJS failure summary: `Failed (N failed): Scenario one; Scenario two`.
  - Extract every failed scenario title from that command's current console output. Prefer the numbered failure details below `-- FAILURES:` and cross-check them against the final failed count.
  - In each numbered failure, use the leaf title: the last non-empty title-path line after the Feature or suite name and before the error message or stack. Cross-check it against the earlier `✖ <scenario title>` line.
  - Preserve console order and the exact scenario title. Do not substitute the scenario numbers from the pasted Allure list, error messages, or failed step names.
  - List each numbered failure once; do not duplicate a title merely because it also appeared in the earlier `✖` line.
  - Identify a hook failure as `Hook: <hook title>` rather than presenting it as a scenario, and include the failed-hook count when available.
  - If fewer titles are available than the final failed count, state how many names are unavailable; never invent a title.
- Non-zero exit code without a test summary: `Execution error` with a short cause; do not invent a failed-test count.
- Zero executed tests, including a skipped-only result: `No tests executed`, not `Passed`.
- A file not attempted after a global execution error: `Not run` with the shared cause.

Use the final summary after CodeceptJS's own configured step retries; do not add another retry layer.

## Report

Return a Markdown table with exactly two cells per row. Emit the header literally as `| Test file | Result |`; do not concatenate the two header labels.

| Test file | Result |
|---|---|
| `tests/admin/character-encoding.js` | Passed (12 passed) |
| `tests/admin/csrf.js` | Failed (2 failed): Rejects a missing CSRF token; Rejects an invalid CSRF token |

Keep the count and all failed scenario names in the `Result` cell on one Markdown source line. Separate names with semicolons or `<br>`, and escape a literal `|` in a scenario title as `\|` so it cannot create another table cell.

Only when at least one feature identifier is unresolved or ambiguous, list those identifiers immediately after the table so none are silently omitted. If there are none, omit that line or section entirely; never report `none` or successful normalization details.

Briefly state whether the target was Local or Production. Do not diagnose or modify failing tests unless the user asks for that as a separate task.
