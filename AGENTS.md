# WebJET CMS: Agent Entry Point

This is the WebJET CMS core repository. Start with the [knowledge index](.github/agents/AGENTS.md) for the actual stack, source layout and shared conventions, then read the [working guide](.github/agents/CONTRIBUTING.md) for setup and verification commands.

Read only the topics relevant to the task:

| Topic | Reference |
| --- | --- |
| Configuration, startup, persistence and audit | [System](.github/agents/AGENTS-system.md) |
| Admin CRUD, REST, filtering and validation | [DataTable](.github/agents/AGENTS-datatable.md) |
| Page editing, saving and approval | [Editor](.github/agents/AGENTS-editor.md) |
| Pages, folders, templates and history | [Doc](.github/agents/AGENTS-doc.md) |
| Authentication and permissions | [Users](.github/agents/AGENTS-users.md) |
| Applications, AppStore and dashboard widgets | [Components](.github/agents/AGENTS-components.md) |
| Domain-aware shared helpers | [Common](.github/agents/AGENTS-common.md) |
| Core utilities and escaping | [Utils](.github/agents/AGENTS-utils.md) |
| Administration frontend and interaction | [Design](.github/agents/AGENTS-design.md) |

Task-specific workflows live in `.agents/skills/`. Use the applicable installed `wj-*` skill and verify its examples against the current code. The canonical `.github/agents/` references are exported with `wj-project-context`; keep customer-specific rules in the customer's own project instructions.

## Git workflow

- Agents MUST NOT create Git commits under any circumstances, on `main` or any other branch. Leave all changes uncommitted for the user to review and commit.
- Each feature or hotfix should normally use a dedicated branch named `feature/<id>-<short-description>` or `hotfix/<id>-<short-description>`. Continue on a suitable existing task branch.
- Preserve unrelated local changes. Follow the root [contribution guide](CONTRIBUTING.md) for PR targets and release bookkeeping.

## Agent skills

Use Matt Pocock's engineering skills with the configuration below. Existing WebJET rules and applicable `wj-*` workflows take precedence over generic skill instructions, including Git operations; leave all changes uncommitted for the user.

### Issue tracker

Track issues and specs in GitHub Issues for `webjetcms/webjetcms`. Read [issue tracker conventions](docs/agents/issue-tracker.md) before reading or updating tickets.

### Triage labels

Use the five canonical triage roles. Read [label mappings](docs/agents/triage-labels.md) before classifying issues.

### Domain docs

Use a single root glossary and shared ADR directory. Read [domain documentation rules](docs/agents/domain.md) when an engineering skill explores domain concepts or architectural decisions.
