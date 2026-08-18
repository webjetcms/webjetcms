# AGENTS: Repository Knowledge Index

[Root](.github/agents/AGENTS.md)
[System](.github/agents/AGENTS-system.md)
[DataTable](.github/agents/AGENTS-datatable.md)
[Editor](.github/agents/AGENTS-editor.md)
[Doc](.github/agents/AGENTS-doc.md)
[Users](.github/agents/AGENTS-users.md)
[Components](.github/agents/AGENTS-components.md)
[Common](.github/agents/AGENTS-common.md)
[Utils](.github/agents/AGENTS-utils.md)

For canonical agent guidance and conventions, use [.github/agents/AGENTS.md](.github/agents/AGENTS.md).

## Git Workflow

- Agents MUST NOT create Git commits under any circumstances. All changes must remain uncommitted so the user can review and commit them.
- Agents MUST NEVER commit directly to the `main` branch. This prohibition also applies to every other branch: committing is always the user's responsibility.
- Each feature or hotfix should normally be developed on a dedicated branch named `feature/<id>-<short-description>` or `hotfix/<id>-<short-description>`, respectively.
