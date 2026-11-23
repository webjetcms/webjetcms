---
name: wj-add-junit-comments
description: Add and review English JavaDoc comments for JUnit test classes and test methods. Use when documenting Java test files, explaining what a JUnit class covers, describing what @Test, @ParameterizedTest, @RepeatedTest, or @TestFactory methods verify, auditing test comments for accuracy, or updating comments after test behavior changes. Makes comment-only changes and does not alter test behavior.
---

# Add JUnit Comments

Add concise JavaDoc that explains the intent and coverage of JUnit tests. Describe behavior and expected outcomes, not the mechanics of the test implementation.

## Scope

- Add or update class-level JavaDoc on JUnit test classes, including relevant `@Nested` classes.
- Add or update method-level JavaDoc on methods annotated with `@Test`, `@ParameterizedTest`, `@RepeatedTest`, or `@TestFactory`.
- Treat meta-annotations that represent JUnit tests like the corresponding standard test annotation.
- Keep all comments in English.
- Make comment-only changes. Do not rename, restructure, or modify test or production code.
- Do not add inline comments inside method bodies.
- Skip lifecycle methods such as `@BeforeEach` and `@AfterEach`, data providers, and helper methods unless the user explicitly asks to document them or their purpose is genuinely non-obvious.

## Workflow

### 1. Resolve the target

Process targets in this order:

1. Files explicitly named or open in the user's context.
2. JUnit test files changed in the current worktree or pull request.
3. Ask for a target when no scope can be determined.

### 2. Understand the tests

Read each complete test class before editing. Identify:

- The production class, service, controller, or behavior under test.
- The scenarios grouped by the class and any `@Nested` classes.
- The condition, action, and observable outcome of each test method.
- Existing comments that are missing, stale, misleading, redundant, or not in English.

Infer intent from assertions, verifications, expected exceptions, test data, mocks, and method names. Do not claim behavior that the test does not assert or verify.

### 3. Document test classes

Every JUnit test class needs class-level JavaDoc that states what is tested. Mention the tested subject and the main behavior or responsibility covered by the class.

```java
/**
 * Tests access control and source-form tracking during form duplication in
 * {@link FormsController}.
 */
class FormsControllerTest {
```

For a `@Nested` class, describe the scenario or operation represented by that group without repeating the outer class comment.

### 4. Document test methods

Every JUnit test method needs JavaDoc that states the behavior being verified. Prefer one concise sentence covering the relevant condition and expected result.

```java
/**
 * Verifies that duplication is rejected when the user cannot access the source form.
 */
@Test
void rejectsDuplicationWhenUserCannotAccessSourceForm() {
```

Rules:

- Describe the test contract, not procedural details such as mock creation or method-call order.
- Include the precondition when it changes the expected behavior: "when", "given", or "for" phrasing is appropriate.
- State the expected result precisely, including returned values, state changes, thrown exceptions, or collaborator interactions when they are central to the test.
- Use "Verifies that ..." by default, but vary phrasing when another concise description reads more naturally.
- Do not merely split or repeat the camel-case method name; use the test body to clarify intent.
- Keep JavaDoc concise. Add a second paragraph only when essential context cannot fit accurately in the summary.
- For parameterized tests, describe the invariant verified across the supplied cases. Add `@param` tags only when parameter meaning is not clear from the method signature or parameter source.
- For test factories, describe the behavior covered by the generated dynamic tests. Add `@return` only when it provides useful information beyond the summary.
- Do not add `@throws` for exceptions that are part of the test implementation rather than the behavior under test.
- Do not add `@author` or `@version`.

### 5. Correct existing JavaDoc

Update a comment when it:

- Describes behavior no longer asserted or verified.
- Omits a material condition or expected outcome.
- Focuses on implementation steps instead of the tested contract.
- Uses vague filler such as "Tests the method" or only repeats the test name.
- Is written in a language other than English.

Preserve an accurate existing comment even if different wording could also work.

### 6. Validate

After editing, verify that:

- Every targeted test class and relevant `@Nested` class explains what it tests.
- Every targeted JUnit test method explains what behavior it verifies.
- Each description matches the assertions and verifications in the test body.
- Comments are English JavaDoc blocks placed immediately before the documented declaration.
- No comments were added inside method bodies.
- No runtime code, imports, annotations, formatting, or test behavior changed.
