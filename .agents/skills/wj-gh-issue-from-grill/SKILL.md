---
name: wj-gh-issue-from-grill
description: Update an existing GitHub issue body with agreed decisions, acceptance criteria, implementation plan and tests from a grill-me, grill-with-docs or other clarification discussion. Use when transferring a resolved discussion into an existing issue for later implementation; use wj-gh-issue-from-chat for a new issue.
---

# GitHub Issue from Grill

Persist the agreed outcome of the current discussion in an existing GitHub issue so another developer or a fresh chat can implement it without the conversation history.

## Scope

Update only the target issue's body. Preserve its title, state, labels, assignees, milestone, project membership and comments. Leave project files and Git state unchanged; implementation, commits, pushes and new issues are outside this workflow.

A request to transfer the discussion into an issue authorizes this body update. Reuse that authorization without asking for another confirmation of already agreed decisions. If the user asks only for a draft, present it without publishing.

## 1. Resolve the issue and discussion

- Use the issue URL or repository and number supplied by the user or unambiguously established in this chat. If the target is missing or ambiguous, ask for it.
- Read repository instructions and issue-tracker configuration when available. For a bare issue number, resolve the repository from that configuration or the origin remote; do not assume a fork is the target.
- Use the available GitHub connector or authenticated `gh` CLI. Read the current issue body and comments, and verify the target is an issue, not a pull request. With `gh`, pass an explicit `--repo OWNER/REPO` on issue commands.
- Gather the user's original request, accepted clarifications, agreed plan and tests from this chat. Assistant proposals are not accepted decisions merely because they appeared in a response. If the discussion is unavailable, request its summary instead of inventing a plan.
- Resolve material contradictions between the discussion, current body and later comments with the user. Keep genuinely undecided points as open questions rather than choosing for them.

## 2. Merge into the body

Preserve the original request, reproduction steps, links, attachment references, relevant checklists and unrelated sections. Keep the existing issue language unless the user requests another language.

Add or update the following sections, adapting their headings to the issue language and existing structure:

```markdown
## Doplnenia a rozhodnutia

<Agreed clarifications, constraints and scope decisions.>

## Akceptačné kritériá

<Observable expected behavior.>

## Implementačný plán

<Agreed implementation steps in execution order.>

## Testovanie

<Agreed verification, including relevant scenarios and prerequisites.>

## Otvorené otázky

<Unresolved points, only if any remain.>
```

Use existing equivalent sections instead of appending duplicates. Replace superseded plan entries with the agreed current plan while retaining the original request as context. Make accepted changes from the original request explicit in the decisions section. Preserve completed checklist items unless an agreed scope change invalidates them.

Include only sections supported by the discussion; do not invent implementation steps or claim tests have run when they are only planned. Keep enough concrete identifiers, paths and conditions for implementation in a fresh chat. Exclude conversational filler, rejected alternatives unless they explain a decision, and unrelated material.

If the body already records the agreed outcome, report that no update is needed.

## 3. Update and verify

Immediately before writing, fetch the body and comments again. If they changed since the initial read, merge against the latest content; clarify new material conflicts before writing. This check reduces overwrite risk but is not an atomic lock.

For `gh`, write the complete merged body as UTF-8 to a temporary file outside the repository, then use:

```shell
gh issue edit ISSUE_NUMBER --repo OWNER/REPO --body-file /absolute/path/to/temporary-body.md
```

Use an actual temporary file path and the resolved repository and issue number. Pass multiline content through the file, not shell interpolation. With a connector, use its structured body field and a conditional update if supported.

If a write returns an uncertain result, read the issue before retrying. After a successful write, fetch the saved body and verify it matches the intended update. Do not overwrite newer remote edits merely to make verification pass; report the conflict. Remove temporary files you created when they are no longer needed.

## 4. Finish

Return the issue link and a brief list of the sections updated. Identify any remaining open questions or verification limitation. State that implementation has not started and that the saved issue is the input for the next implementation chat.
