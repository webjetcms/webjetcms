---
name: wj-ui-review
description: Review or polish an existing WebJET CMS interface before merge or a selected browser element shared in chat. Use for UI/UX critique, visual consistency, spacing, sizing, typography, colors and interaction states in a PR, local changes, a named screen or an attached element/screenshot. Report findings by default; implement scoped fixes when polish or corrections are requested. Not for creating a new visual identity or general backend code review.
---

# WebJET UI Review

Assess whether the changed interface is consistent with WebJET and supports the user's task. Preserve the established compact administration design and familiar workflows. A useful finding identifies a visible or behavioral defect, its evidence and the smallest suitable correction.

## Select the mode and scope

- **Review / critique / audit / before merge:** inspect and report without changing implementation files. Browser interaction is part of the review; preserve real content and do not submit destructive or publishing actions just to verify their appearance.
- **Polish / fix / apply corrections:** make the requested UI refinements and verify them. Existing authorization carries forward; do not ask again for routine fixes within that scope. If the requested outcome needs a redesign or changes product behavior, explain the tradeoff before expanding scope.
- An explicit file, component, URL, PR or user-selected subset determines the scope. Follow the user's exclusions.
- When the user refers to a browser element shared with the message (for example, "review this button"), resolve the attached selection first. Review that element and the immediate layout/interaction context needed to assess it; do not default to the whole page or branch. Follow [selected browser element handling](references/review-checklist.md#selected-browser-element). A selection supplied only as an example does not override an explicit PR or other target.
- For a pre-merge review, resolve the actual PR target or repository base convention, then inspect the branch diff from its merge base. Include relevant staged, unstaged and untracked UI work, distinguishing it from the committed PR. Do not review only the last commit or assume every repository targets `main`.
- For a local-change review, inspect staged and unstaged diffs and relevant untracked files. With no explicit scope, use the named/current feature and Git state; ask only if materially different targets remain plausible. A clean tree does not imply an empty branch diff.
- Map changed CSS, templates and JS to the screens and shared components they affect. Include a representative existing consumer when shared styles change; do not turn a feature review into an unrelated site-wide cleanup.

State the mode and target briefly before working, plus the comparison base for a diff-based review. An element review does not require a PR or Git changes. A merge-readiness assessment does not authorize merging, publishing or creating commits.

## Capture the initial appearance

For every browser-backed review or polish, capture a **Before** screenshot immediately after resolving the target, before exercising controls, changing viewport/state, editing source or rebuilding. Capture the browser viewport for a screen review, or the selected element with enough surrounding context to assess its layout. Follow [screenshot capture and comparison](references/review-checklist.md#screenshot-capture-and-comparison) for storage, matching captures and unavailable-browser handling.

Keep the original capture unchanged for the final report. In polish mode, capture **After** once the rebuilt result is loaded and verified in the same viewport and component state. For several affected screens or elements, preserve a matching pair for each edited target. In review-only mode, show the initial capture without implying that changes were made.

## Establish the WebJET baseline

Read the current project's instructions. Use `wj-project-context` when available to identify core versus a customer project and load the relevant `AGENTS-design.md`. In an installed distribution, knowledge is under that skill's `references/`; in the confirmed core checkout, it is maintained in `.github/agents/`. Resolve skills through the active catalog, not a fixed machine path.

Verify the intended styling against the target version's actual source and a comparable existing component. Core administration sources include:

- `src/main/webapp/admin/v9/src/scss/ninja.scss` and `2-helpers/_variables.scss` for tokens and runtime `--wj-*` properties;
- `3-base/_form.scss`, `_table.scss` and `_modal.scss` for shared control and DataTables rules;
- the relevant `4-layout/`, `5-modules/`, `6-pages/`, `8-views/` and Pug/HTML sources for context-specific behavior.

These are core reference paths, not prerequisites for customer checkouts. Use available project overrides, dependency assets and rendered reference components when core sources are absent. If a companion skill is missing, continue with available project evidence and disclose any material gap; do not install dependencies as part of a review.

Record the reference component, source token/rule and UI state used for comparison. Compare equivalent roles: a compact toolbar button, modal footer action and icon-only close control need not share every dimension. If documentation and rendering disagree, trace the loaded version, cascade and overrides before deciding which is wrong.

Do not copy a second token catalog into this skill. Do not impose a universal 4/8px grid, 16px minimum for every control label, a new font, or a new radius scale. Respect deliberate optical adjustments and established density; existing patterns do not excuse a demonstrated accessibility failure.

Use other installed skills only where needed:

- `wj-accessibility` for detailed accessibility findings, checking normative thresholds against the linked WCAG sources when uncertain;
- `wj-cke-stylescombo` for style-picker source ownership, editor modes and build/integration; preserve selection, undo and page content while inspecting the UI;
- `wj-datatable`, `wj-dashboard-widget` or `wj-page-builder` for the affected component's conventions;
- `wj-codeceptjs` when a meaningful regression needs a new or changed browser test.

A UI review complements `wj-code-review`; it does not require a separate backend audit for every visual change. When reviewing a customer-facing template, derive its visual baseline from that template and brief, not administration tokens.

## Inspect the experience, then confirm the cause

Read [the review checklist](references/review-checklist.md). Apply the parts relevant to the changed surface, rather than inventing every possible state for every control.

1. Follow the main user task in the rendered UI. Assess discoverability, grouping, primary action, selected state and feedback before reading scanner output. Note where a user must guess or repeat work.
2. Exercise the affected states and content variants with the available browser tools. Use the existing local/test instance and project test infrastructure. Verify that the URL and loaded assets correspond to the reviewed checkout; do not treat a stale bundle as the current result.
3. Compare the affected elements with the WebJET baseline. For precise claims, use DOM/computed-style measurements where permitted, then trace the declaration responsible. A suspicious literal or screenshot alone does not prove a layout defect.
4. Check the relevant responsive transitions and keyboard path. For core administration tables/modals this normally includes desktop, below 1200px and below 768px; adapt to the target project's supported layouts and actual breakpoints. Record the viewport used.

Use supported browser APIs and respect their read/write limits. Do not install Impeccable, run its launcher or hooks, inject an overlay, or create product/design documents as a prerequisite. This skill contains an independent WebJET workflow inspired by its review methods.

When a browser, authentication or server is unavailable, finish the useful source review and identify the missing visual checks. Screenshots can support visual observations, but pixel values inferred from them are estimates and interaction claims remain unverified. Do not describe source-only review as visual approval.

## Judge findings proportionally

For each candidate finding, establish the affected user task, reproducible state, actual behavior/value, expected behavior/value and its reference. Separate confirmed defects from design preferences or hypotheses needing a browser check.

- **P1 — fix before merge:** a core task is blocked, the interface misrepresents an operation, content is lost, or a required interaction is inaccessible.
- **P2 — actionable defect:** reproducible inconsistency, layout/state failure or usability friction with a clear consequence and correction. State whether it warrants fixing before merge in this context.
- **P3 — optional polish:** a minor visible refinement with a defensible benefit; do not turn taste into a blocker.

Prioritize user impact, then consistency and cosmetic detail. Group repeated symptoms with the same shared cause. Distinguish regressions introduced by the reviewed change from unrelated existing issues; report the latter separately only when relevant. Do not manufacture a score, a quota of findings or speculative problems. If nothing actionable is found, say so along with the verification coverage.

## Apply polish when requested

Fix the cause in the owning source and narrowest appropriate shared layer. Reuse existing component variants and semantic tokens; avoid page-specific overrides for a shared defect, broad vendor overrides, new frameworks and incidental refactors. Do not hand-edit compiled/minified assets. Follow the component's build/integration workflow, including separate CKEditor sources when applicable.

Preserve translated text and established save, close, selection and undo semantics unless changing them is part of the request. Inspect the result after a coherent batch of fixes, then revisit the affected flow and representative shared consumer. Build the owning bundle and run focused existing tests appropriate to the change. Add a regression only when it protects meaningful behavior; trivial spacing corrections need visual verification, not tests mirroring CSS literals.

Finish when the scoped findings are resolved and relevant checks pass, or explain the concrete remaining limitation. Capture the verified final appearance for comparison with the saved original. Review the final diff for accidental changes. Leave all work uncommitted.

## Report

Use the user's language and lead with the UI assessment: **ready within the verified scope**, **changes recommended before merge**, or **verification incomplete**. Separate unresolved defects from verification gaps. For a selected element, describe readiness or recommended changes for that element only; do not imply the page or branch passed review. This is an assessment of UI readiness, not approval of the entire PR.

For each finding, include:

- priority and a concrete title;
- file and line when established, plus the affected screen/element and state;
- observed behavior or measurement versus the expected result and its source;
- user impact and the smallest proposed correction; in polish mode, explain the applied fix;
- a screenshot or measurement reference when it materially helps reproduce the issue.

Include the checked viewports/states, builds/tests actually run and remaining gaps. Do not claim screen-reader, mobile-device, cross-browser or full WCAG verification from a desktop screenshot or automated scan. Keep optional visual suggestions distinct from merge blockers.

End the report with the screenshots displayed inline: **Before** and **After** for polish, or **Reviewed state** for review-only, with labels in the user's language. Showing an image during a tool call or linking its filename alone does not satisfy this requirement. Use Markdown image embeds with the actual absolute local artifact paths, or the host's supported inline image mechanism. Identify the target and viewport; disclose any differences in content or state that limit comparison. If a capture is unavailable, state which one is missing and why instead of claiming a complete before/after comparison.
