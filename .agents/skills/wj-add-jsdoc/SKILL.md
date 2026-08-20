---
name: wj-add-jsdoc
description: Add and review English JSDoc comments for JavaScript classes, constructors, functions, methods, exported APIs, and non-obvious reusable helpers. Use when adding missing JavaScript documentation, auditing stale or invalid @param/@returns/@throws tags and types, documenting options objects, callbacks or async return contracts, translating audited JSDoc to English, or reviewing changed JavaScript files before merge. Makes comment-only changes and skips trivial functions and one-off callbacks.
---

# Add JavaScript JSDoc

Add, correct, and audit JSDoc according to the JavaScript code's actual contract. Write all added or updated documentation in English.

## Scope and Safety

- Process JavaScript files (`.js`, `.mjs`, and `.cjs`) in the requested scope. Do not apply JavaScript typing rules to TypeScript files.
- Add declaration-adjacent JSDoc blocks (`/** ... */`) only. Do not add explanatory inline comments inside implementation bodies or local `@type` annotations by default.
- Make comment-only changes. Do not rename, restructure, reformat, or change executable code.
- Exclude generated, minified, vendored, and third-party files unless the user explicitly includes them.
- Do not classify `src/main/webapp/admin/v9/npm_packages/webjetdatatables` as third-party merely from its path; it contains first-party code.
- Preserve accurate existing documentation. Do not rewrite it only to prefer different wording or tag style.
- Translate non-English text in JSDoc blocks being added, corrected, or audited. Leave unrelated line and block comments to `wj-translate-comments` unless the user requests a broader translation.

## Workflow

### 1. Resolve the Target

Process targets in this order:

1. JavaScript files explicitly named, attached, or open in the user's context.
2. JavaScript files changed in the current worktree or pull request.
3. Ask the user for a target when neither provides a clear scope.

### 2. Understand the Complete Contract

Read each complete file before editing. Inspect imports, exports, callers, tests, or adjacent types only when needed to establish:

- whether a declaration is public, exported, reused, or purely local;
- accepted parameter types, optional values, defaults, and object properties;
- every return path, including `null`, `undefined`, and promises;
- observable side effects, emitted events, callback behavior, and deliberately thrown errors;
- whether existing JSDoc is stale, incomplete, misleading, or not in English.

Infer types from code evidence, not names alone. Do not invent object shapes, nullability, callback signatures, thrown errors, or nominal types. Use `*` only when the API intentionally accepts any value, not as a substitute for investigation.

### 3. Decide What to Document

| Target | Document? |
|---|---|
| Exported class, function, or non-trivial public method | Yes; describe its externally observable contract |
| Constructor with parameters, configuration, side effects, or important invariants | Yes |
| Reused local/private helper with non-obvious logic | Yes |
| Public options object or reused callback/object shape | Yes; use property paths, `@callback`, or `@typedef` only when useful |
| Exported constant or property whose type or allowed values are not obvious | Yes; use declaration-adjacent `@type` when it adds value |
| Simple getter/setter, short obvious helper, or thin delegation wrapper | Skip |
| Anonymous one-off callback or straightforward event/lifecycle handler | Skip unless its contract is non-obvious or differs from the framework contract |
| Local variable or implementation detail inside a function | Skip by default |

Do not infer visibility from a leading underscore alone. Add `@public`, `@protected`, or `@private` only when the API contract or an established local convention proves it.

### 4. Write Precise JSDoc

Use a concise summary sentence that ends with a period. Describe behavior and constraints rather than restating the symbol name or implementation steps. Add details about preconditions, side effects, or events only when they matter to callers.

```javascript
/**
 * Loads a chart definition and renders it in the configured container.
 *
 * @param {Object} options - Rendering options.
 * @param {string} options.targetSelector - CSS selector for the container.
 * @param {string} [options.mode="default"] - Rendering mode.
 * @param {function(string): boolean} [options.filter] - Determines whether a series is rendered.
 * @returns {Promise<HTMLElement|null>} The rendered chart element, or `null` when no target exists.
 */
async function renderChart(options) {
```

Apply these rules:

- Match every `@param` name and order to the actual signature. Remove stale tags and add missing ones.
- Use lowercase primitive types such as `string`, `number`, `boolean`, and `symbol`.
- Express arrays as `Type[]`, unions as `Type|null`, and async results as `Promise<Type>`.
- Mark optional parameters as `[name]` and include `[name=default]` only when the runtime default is verified.
- Document options-object properties as `options.property`; include required, optional, and default semantics accurately.
- For callback parameters, document when they run, their arguments, and the meaning of their return value when those details form part of the contract.
- For public custom events, document the event name and, when verified, its `detail` shape, bubbling, and cancelability on the API that emits it. Do not document every listener callback.
- Use `@returns` for non-trivial value-producing functions and actual promise contracts, including non-`async` functions that return a promise. Use `Promise<Type>` for promise resolution values and do not add `@async` to timer- or callback-based code.
- Never add `@returns` to a constructor. Omit it for functions that return no meaningful value and for skipped trivial accessors.
- Use `@throws` only for errors deliberately exposed by the function's contract, not every error that a dependency might propagate.
- Introduce `@typedef`, `@property`, or `@callback` only for a complex shape reused across declarations. Keep a one-off shape next to its parameter when that is clearer.
- Use `@this`, `@deprecated`, `@see`, or `@example` only when code evidence or caller needs justify them. A new or corrected `@deprecated` tag must identify the replacement or migration path when one exists.
- Do not add `@author`, `@version`, empty `@description`, redundant `@public`, or filler such as "This function...".

### 5. Correct Existing JSDoc

Correct a block when it:

- names parameters that no longer exist or omits current parameters;
- uses an invalid or inaccurate type, optional marker, default, object property, or return contract;
- contradicts an implementation, caller, test, export, side effect, event, or thrown error;
- contains empty tags, vague filler, or non-English documentation;
- claims visibility, deprecation, or behavior that cannot be established from the code.

Preserve an accurate block even if another valid representation or wording is possible.

### 6. Validate

After editing, verify that:

- every targeted non-trivial public/exported declaration has useful JSDoc;
- every tag maps to the current signature and observed behavior;
- types agree with all relevant branches, defaults, callbacks, and promise resolution values;
- every added or updated sentence is in English;
- each block is immediately before the declaration it documents;
- trivial declarations and implementation details were not over-documented;
- generated and third-party files were not changed;
- the diff contains comment changes only and leaves runtime behavior intact;
- `git diff --check` reports no whitespace errors for the edited paths when Git is available.
