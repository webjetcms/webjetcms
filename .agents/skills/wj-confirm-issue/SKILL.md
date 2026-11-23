---
name: wj-confirm-issue
description: Reassess a specific code-review finding or reported issue, explain whether it is valid, and propose a concrete solution for the user to approve before implementation. Use when asked to verify, challenge, or explain a supplied finding; not for a general code review or a direct request to implement a fix.
---

# Confirm an Issue

Re-analyze the supplied issue against the current code, explain where the problem is, and recommend a solution the user can review before deciding whether to implement it.

## Scope and authorization

- Accept a pasted finding, issue description, or an unambiguous reference to a finding in the conversation. Ask which issue to examine only when the target cannot be determined from context.
- Treat the finding as a claim to investigate, including findings from your own earlier review. Do not assume it is correct or preserve its original severity without evidence.
- Limit the investigation to the issue and the code needed to understand its impact. Do not expand it into a full PR review or unrelated cleanup.
- This workflow authorizes investigation and a proposal. Preserve project files, configuration, and Git state; do not implement the fix or add regression tests before the user approves the proposal. Existing tests and isolated temporary reproductions may be used when they do not alter application data or project sources.

## Reassess the finding

Read the relevant current implementation, callers, tests, and applicable repository guidance. Check the actual input contracts and supported usage. When the finding concerns a regression, compare the relevant change with its previous behavior and distinguish an introduced defect from an existing limitation.

Trace a concrete trigger through the code to the claimed outcome. Use a focused existing test or safe reproduction when it adds confidence. Look for guards, configuration, or caller guarantees that could invalidate the finding. Separate observed behavior from inference, and explain any verification limits. Failure to reproduce alone does not prove that the issue is invalid.

State whether the issue is confirmed, partially valid, disproved, already fixed, or still inconclusive. Explain the conclusion using exact file locations and relevant symbols. Revise the scope or severity when the evidence warrants it.

## Explain and propose

Respond in the user's language unless they request another language. Keep the explanation proportional to the issue and cover:

- **Verdict and evidence:** what was checked, what was observed, and what remains uncertain.
- **Problem and impact:** the triggering conditions, expected versus actual behavior, the responsible code, and who or what is affected.
- **Recommended solution:** propose the simplest correct fix, changing as few classes and functions as possible. Reuse existing code and patterns; avoid unnecessary abstractions, unrelated refactoring, and complex mechanisms when a straightforward change is sufficient. Identify the affected files or methods and any material compatibility consequences. Use a short illustrative snippet or pseudocode when useful; do not apply it.
- **Verification plan:** how to demonstrate the fix and protect the relevant existing behavior. Distinguish checks already run from tests proposed for implementation.

Offer alternatives only when they involve a meaningful tradeoff. If the finding is disproved or already fixed, explain why and do not manufacture a code change. If evidence is insufficient, identify the missing information needed to decide.

## Hand off for a decision

Finish the analysis and make the recommended change concrete before asking whether the user wants it implemented. State that implementation is awaiting their decision because this skill separates confirmation from implementation, and link to this SKILL.md when explaining that boundary.

Stop after presenting the proposal. Do not treat approval to investigate, silence, or agreement that the issue exists as approval to implement. If the user later explicitly approves the proposed implementation, proceed within that approved scope without asking for the same approval again.
