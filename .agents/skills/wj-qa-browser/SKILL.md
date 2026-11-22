---
name: wj-qa-browser
description: Run exploratory end-to-end QA of WebJET CMS changes by clicking through the running app in a browser. Use for PR acceptance testing, exploring alternative and negative user paths, and reporting reproducible bugs or discrepancies with the user guide.
---

# WebJET Browser QA

Test the changed workflows through the rendered UI and deliver a QA report in the user's language. Use this workflow for live functional testing. Use `wj-ui-review` for a focused visual or usability review and `wj-codeceptjs` for writing automated regression tests. Keep product code, documentation, and automated tests untouched during a QA run.

## Scope the run

1. Resolve the user-specified PR, local changes, or named workflow. For a PR, use its actual target and diff. For current-branch changes, use the relevant merge base and include in-scope working-tree edits. Read the relevant acceptance criteria and user documentation for the changed workflows. In WebJET core, start with the relevant language and audience under `docs/`.
2. Turn each relevant user-facing change into a check with an expected outcome and its source. Cover the main path first, then plan alternative and negative paths based on what a user could reasonably try from the UI. Prioritize paths that could lose work, affect the wrong record, or leave the user stuck. If the PR and the guide prescribe different results or leave the result unclear, record that before testing and preserve the discrepancy in the report.
3. Resolve the target URL or tab from the request and project context. An open tab can be a starting point when it matches the requested workflow. Establish the environment, selected domain, relevant user role, and whether the running build represents the changes. Use available project or build evidence. When the deployed revision is uncertain, report observed behavior with that limitation and qualify any attribution to the PR.

## Test in the browser

- Use the browser controls available in the session. In Codex desktop, prefer the built-in browser (`@Browser`) and follow the active browser tool's instructions. Navigate, click, fill, and check the visible outcome as a user would. Use source and existing tests to select scenarios and explain a confirmed finding. A check counts as executed only after exercising it through the UI.
- Use test records created for the run or fixtures designated for it. When the environment is shared or its purpose is uncertain, start with inspection and avoid submissions affecting existing content. Exercise publishing, deletion, or other consequential actions only within the authorized test scope. Clean up test records when safe and in scope, and report any remaining test data.
- Check outcomes that matter across screens. For example, after saving a setting, reopen the record or reload to check persistence. For a preview, verify what the editor sees and, when relevant, check the separate public view.
- Exercise plausible alternative paths even when the guide is silent. Try another visible entry point, a different order of actions, skipping a step that appears optional, or leaving and returning to the workflow. Check whether the user reaches the correct result or gets clear guidance on how to continue.
- Exercise relevant negative paths and recovery. Submit incomplete or invalid input, cancel or navigate away with unsaved edits, retry after an error or unclear feedback, or resume after the selected context changes. Check the relevant roles or conflicting edits when the scenario calls for them and test accounts or fixtures are available. Check the feedback, which data was saved, whether the user can recover their work, and what happens when they correct the problem and continue. Watch for lost work, duplicate saves, or changes to the wrong record.
- Reproduce unexpected behavior from a known starting state when safe. Record the exact visible steps, action labels and error text. Capture a screenshot where it helps establish the outcome, with sensitive values omitted. When a step is blocked by the browser, login, role, environment or test data, mark it blocked and continue with independent checks.

## Report

Lead with findings in order of user impact. For each finding, include the affected workflow and its preconditions, numbered steps to reproduce, actual result, expected result, and useful evidence. Link to the relevant requirement when available. For undocumented paths, explain the UI cue or data integrity expectation behind the finding. State whether it reproduced and whether attribution to the PR was verified.

- **Product defect** when the observed behavior breaks an established requirement or causes a concrete workflow failure such as losing saved work.
- **Documentation mismatch** when verified behavior matches the intended requirement and the guide gives a different procedure or result.
- **Needs clarification** when the intended behavior cannot be determined from the available evidence. Report the observed behavior and cite the conflicting or missing requirement.

Close with the environment and build information, outcomes for the main, alternative and negative paths exercised, and checks that remain blocked or untested with their reasons. State when no issues were found in the verified scope. Keep the report in the conversation unless the user requests a file or another destination.
