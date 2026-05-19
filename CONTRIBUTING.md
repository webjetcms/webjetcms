# Contributing to WebJET CMS

Thank you for your interest in contributing! This document explains the process for submitting changes. For full developer documentation, visit [docs.webjetcms.sk](https://docs.webjetcms.sk).

## Getting Started

Follow the installation and launch guide to set up your local development environment:

- [Installation and launch](docs/en/developer/install/README.md)

## Branching Strategy

We maintain two long-lived branches:

| Branch | Purpose |
| -------- | --------- |
| `main` | Active development — new features and non-urgent improvements |
| `hotfix/YEAR.0-main` (e.g. `hotfix/2026.0-main`) | **Fixes only** — no new features are allowed here |

### Creating your branch

Use a prefix that matches the target branch:

| Your branch prefix | Base branch | PR target |
| -------------------- | ------------- | ----------- |
| `feature/<slug>` | `main` | `main` |
| `hotfix/<slug>` | `hotfix/YEAR.0-main` | `hotfix/YEAR.0-main` |

> **Rule:** Your pull request must always target the **same branch** you branched from. A `feature/` branch goes to `main`; a `hotfix/` branch goes to `hotfix/YEAR.0-main`.

## Commit Message Conventions

```txt
<type>(scope): short summary

Optional longer body explaining what and why.

Refs: #issue-id
```

Allowed types: `feat`, `fix`, `refactor`, `docs`, `chore`, `perf`, `test`, `build`.

Examples:

```txt
feat: datatable - add column visibility toggle
fix: users - prevent null pointer on missing role
docs: install - update Gradle wrapper version
```

## Submitting a Pull Request

Before opening a PR, make sure you have:

- [ ] **Tests** — add or update JUnit tests in `src/test/java/` and run them with `./gradlew test`; add or update E2E tests in `src/test/webapp/` (CodeceptJS + Playwright)
- [ ] **Documentation** — update the relevant page under `docs/` if you changed or added functionality
- [ ] **CHANGELOG** — add an entry to `docs/sk/CHANGELOG-2026.md` in the `## 2026-SNAPSHOT` section (in Slovak):

  ```txt
  <short description of the change> (#<pull request number>)
  ```

- [ ] **ROADMAP** — check `docs/sk/ROADMAP.md`; if your change completes a planned item, mark it `[x]` and append `(#<pull request number>)`

### Review process

- At least **1 reviewer** must approve the PR before it can be merged.
- A project maintainer performs the final merge.
- Keep PRs focused and incremental — prefer small, reviewable changes over large ones.
- Draft PRs are welcome for early feedback; CI skips WIP/draft PRs automatically.

## Reporting Issues

Please use [GitHub Issues](https://github.com/webjetcms/webjetcms/issues) to report bugs or request features. Before opening a new issue, search for existing ones to avoid duplicates.

## Security

Do **not** open a public issue for security vulnerabilities. See [.github/SECURITY.md](.github/SECURITY.md) for responsible disclosure instructions.

## License

WebJET CMS uses a dual license:

- **Apache License 2.0** for the open-source modules listed at [webjetcms.sk/aplikacie](https://www.webjetcms.sk/aplikacie/)
- **Commercial license** (Basic / Professional / Enterprise) for all other parts — © InterWay, a.s.

By contributing you agree that your changes may be distributed under these license terms.
