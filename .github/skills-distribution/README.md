# WebJET CMS skills

WebJET CMS development skills and architecture references for Codex, GitHub Copilot and Claude Code, including customer projects created from older basecms templates.

This repository is a generated distribution. **Edit skills and submit pull requests in [webjetcms/webjetcms](https://github.com/webjetcms/webjetcms)**: skills live in `.agents/skills`, shared knowledge in `.github/agents`. Direct edits to this distribution are replaced by the next export. `source.json` records the source commit and content hash. Original license information and skill attribution are retained; see [LICENSE.md](LICENSE.md) and each skill's metadata.

## Install

Installation uses the existing [skills CLI](https://github.com/vercel-labs/skills). Install Node.js 22.20 or newer, npm and Git. The tested installer version is `skills@1.7.2`. A newer installer may have different runtime requirements.

Run in the customer project's root, and select the agents to use:

```sh
npx skills@latest add webjetcms/skills --skill '*'
```

For a personal installation available across projects:

```sh
npx skills@latest add webjetcms/skills --skill '*' --global
```

For unattended installation, select the target agents explicitly:

```sh
npx --yes skills@latest add webjetcms/skills --skill '*' --agent codex github-copilot claude-code --yes
```

Add `--copy` when you want independent copies instead of symlinks, for example on Windows without symlink support. Skills are placed in the agent's recognized directories; npm is only used to run the installer. No CMS dependency or project `package.json` needs to change.

To install only the datatable workflow and its setup/context companions:

```sh
npx skills@latest add webjetcms/skills --skill wj-datatable wj-codeceptjs wj-project-context wj-setup
```

The installer does not resolve dependencies between skills. Include the companions a selected workflow requires. Existing skills can be overwritten: see the migration instructions below before replacing manual copies.

## Connect project instructions

After installation, ask your agent:

> Use wj-setup to connect the installed WebJET CMS skills to this project's instructions. Preserve the project's existing rules.

Installation alone does not execute this step. Setup adds managed blocks to the project instruction files for the three supported agents, including an existing root `AGENTS.override.md`. It preserves other content. Local context links are relative; global installations refer to the installed skill by name. Global instruction files are never changed.

For a local installation, the equivalent command is:

```sh
node .agents/skills/wj-setup/scripts/setup-project.mjs --project . --scope project
```

For global installations, let the agent locate the installed `wj-setup` script and pass `--scope global` with the customer project directory. Add `--dry-run` to preview. Keep custom rules outside `<!-- webjet-skills:start -->` and `<!-- webjet-skills:end -->`. If markers are damaged, repair them before rerunning; setup does not guess which user text to replace. Symlinked instruction files need manual integration.

For a single request that covers installation and setup, paste this into your coding agent:

> Install all official WebJET CMS skills from https://github.com/webjetcms/skills into this project using npx skills. Target the coding agent I am currently using. Before replacing existing skills, preserve and compare any manual copies. Then use wj-setup to connect wj-project-context to this project's instructions without replacing my custom rules. Do not update WebJET CMS or basecms.

For personal installation, replace "into this project" with "globally for my user; then configure this project's instructions". The agent needs terminal access, network access and permission to write the chosen directories. Giving the source URL avoids relying on search by an ambiguous name.

## Use and compatibility

Ask for a WebJET task, or name a skill explicitly, for example `wj-datatable`. The host can choose a skill by its description. Start a new session if changed persistent instructions have not been loaded.

One maintained skill set serves core and customer projects. Skills inspect the installed CMS version, available APIs and project conventions. They do not guarantee that a new framework feature exists in every historical CMS version. They do not upgrade CMS, create missing test infrastructure or install external plugins automatically. Specialized workflows, such as feature videos, require their documented tools and helpers.

Project rules take precedence over shared defaults. Framework source paths are reference locations, not a requirement to copy the CMS source into a customer project.

## Update and pin

Use the installer's interactive update command to select the intended scope:

```sh
npx skills@latest update
```

To update selected WebJET skills without updating unrelated skills:

```sh
npx skills@latest update wj-project-context wj-setup wj-datatable wj-codeceptjs --project
```

Use `--global` for global updates. Re-run `wj-setup` after updating it. The installer updates skill folders, while setup updates only its marked instruction blocks. Keep local customizations in project instructions or separately named skills. Do not expect the installer to merge edits inside installed WebJET skills.

For a reproducible installation, replace `DISTRIBUTION_COMMIT_SHA` with the full commit SHA from **this distribution repository**, not the source CMS revision in `source.json`:

```sh
npx skills@1.7.2 add https://github.com/webjetcms/skills/tree/DISTRIBUTION_COMMIT_SHA/skills --skill '*'
```

Retain the installer's project lock file and installed files according to your team's policy. For a pinned deployment, change the selected revision explicitly and verify it; do not use an unqualified update as a release policy.

## Migrate manual copies

1. Inspect existing `wj-*` folders in both project and global skill directories. Back up any local changes outside the installation destinations.
2. Compare them with the distribution. Move customer-specific rules into the project's instructions or a separately named skill before replacement.
3. Install in the chosen scope and verify the skill catalog. Remove obsolete duplicates only after comparing them; project and global copies can otherwise both appear.
4. Run `wj-setup` and review its diff. It does not remove old hand-written references to `.github/agents`; reconcile those manually if they conflict with the new context.

To remove the integration, use `npx skills remove` for the selected WebJET skills and remove only the marked WebJET blocks from project instruction files. Keep unrelated project instructions.
