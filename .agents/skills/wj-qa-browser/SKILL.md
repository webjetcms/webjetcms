---
name: wj-qa-browser
description: Run exploratory end-to-end QA of WebJET CMS changes by clicking through the running app in a browser. Use for manual-style PR acceptance testing and reporting reproducible workflow bugs or differences from user documentation.
---

# WebJET Browser QA

Test the changed workflows through the rendered UI and deliver a QA report in the user's language. Use this workflow for live functional testing. Use `wj-ui-review` for a focused visual or usability review and `wj-codeceptjs` for writing automated regression tests. Keep product code, documentation, and automated tests untouched during a QA run.

## Scope the run

1. Resolve the user-specified PR, local changes, or named workflow. For a PR, use its actual target and diff. For current-branch changes, use the relevant merge base and include in-scope working-tree edits. Read the relevant acceptance criteria and user documentation for the changed workflows. In WebJET core, start with the relevant language and audience under `docs/`.
2. Turn each relevant user-facing change into a check with an expected outcome and its source. Cover the main path first, then affected states and nearby flows that could regress. If the PR and the guide prescribe different results or leave the result unclear, record that before testing and preserve the discrepancy in the report.
3. Resolve the target URL or tab from the request and project context. An open tab can be a starting point when it matches the requested workflow. Establish the environment, selected domain, relevant user role, and whether the running build represents the changes. Use available project or build evidence. When the deployed revision is uncertain, report observed behavior with that limitation and qualify any attribution to the PR.

## Test in the browser

- Use the browser controls available in the session. In Codex desktop, prefer the built-in browser (`@Browser`) and follow the active browser tool's instructions. Navigate, click, fill, and check the visible outcome as a user would. Use source and existing tests to select scenarios and explain a confirmed finding. A check counts as executed only after exercising it through the UI.
- Use test records created for the run or fixtures designated for it. When the environment is shared or its purpose is uncertain, start with inspection and avoid submissions affecting existing content. Exercise publishing, deletion, or other consequential actions only within the authorized test scope. Clean up test records when safe and in scope, and report any remaining test data.
- Check outcomes that matter across screens. For example, after saving a setting, reopen the record or reload to check persistence. For a preview, verify what the editor sees and, when relevant, check the separate public view. Explore validation, cancellation, permissions or conflicting edits when the change affects those paths and the required data or roles are available.
- Reproduce unexpected behavior from a known starting state when safe. Record the exact visible steps, action labels and error text. Capture a screenshot where it helps establish the outcome, with sensitive values omitted. When a step is blocked by the browser, login, role, environment or test data, mark it blocked and continue with independent checks.

## Report

Lead with findings in order of user impact. For each finding, include the affected workflow and its preconditions, numbered steps to reproduce, actual result, expected result with a link to the PR criterion or precise section of the guide, and useful evidence. State whether it reproduced and whether attribution to the PR was verified.

- **Product defect** when the observed behavior breaks an established requirement.
- **Documentation mismatch** when verified behavior matches the intended requirement and the guide gives a different procedure or result.
- **Needs clarification** when the intended behavior cannot be determined from the available evidence. Report the observed behavior and cite the conflicting or missing requirement.

Close with the environment and build information, scenarios that passed, and checks that remain blocked or untested with their reasons. State when no issues were found in the verified scope. Keep the report in the conversation unless the user requests a file or another destination.
