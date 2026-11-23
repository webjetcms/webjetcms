# Issue tracker: GitHub

Issues and specs live in GitHub Issues for `webjetcms/webjetcms`. Use the `gh` CLI with `--repo webjetcms/webjetcms` to avoid selecting a fork remote.

## Conventions

- Search existing issues before creating a new one.
- When a skill says "publish to the issue tracker", create a GitHub issue. When it says "fetch the relevant ticket", read the issue and its comments.
- For an issue derived from a chat, use the installed `wj-gh-issue-from-chat` workflow, including its English title convention.
- Pass multiline issue bodies and comments through a UTF-8 file using `--body-file`.
- Read [triage label mappings](triage-labels.md) before applying or removing labels.
- Follow the repository's [contribution guide](../../CONTRIBUTING.md) and [security policy](../../.github/SECURITY.md) when reporting issues.
- Ticket workflows remain subject to the [repository Git rules](../../AGENTS.md#git-workflow). Leave implementation changes uncommitted for the user.

## Pull requests as a triage surface

**PRs as a request surface: no.**

## Planning and dependencies

For `wayfinder`, use one map issue with the label `wayfinder:map`, and child tickets labelled `wayfinder:research`, `wayfinder:prototype`, `wayfinder:grilling`, or `wayfinder:task`, as appropriate. Create these labels only when that workflow is used.

Use native GitHub sub-issues for parent-child relationships and native issue dependencies for blockers when supported by the installed CLI/API. Dependency API calls require the issue database ID, not its displayed issue number.

If native relationships are unavailable, maintain a task list in the parent, a `Part of #<parent>` line in each child, and a `Blocked by: #<number>` line for blockers. A ticket is ready only when all blockers are closed. Scope frontier queries to the map's children and select the first unassigned, unblocked open ticket in map order.

When the invoked workflow calls for claiming a ticket, assign it to the driving developer. After resolving it, record the result, close the ticket, and link the decision from the map.
