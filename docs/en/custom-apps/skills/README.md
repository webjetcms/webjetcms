# WebJET CMS skills for AI agents

WebJET CMS skills include procedures for creating applications, DataTables, PageBuilder blocks, tests, documentation, and code review. You can also add them to an older client project based on basecms without updating its template or the CMS itself.

**Codex, GitHub Copilot and Claude Code** are supported. The distribution source is [webjetcms/skills](https://github.com/webjetcms/skills). Content is maintained in the main WebJET CMS repository and is automatically exported to the distribution repository.

## Installation

Installation is handled by the existing [skills CLI](https://github.com/vercel-labs/skills) tool. You need Node.js at least 22.20, npm, and Git. The verified installer version is `skills@1.7.2` ; a newer version may change Node.js requirements.

In the root folder of the client project, run:

```sh
npx skills@latest add webjetcms/skills --skill '*'
```

Select the agents you are using. For a personal installation available in multiple projects, add `--global`:

```sh
npx skills@latest add webjetcms/skills --skill '*' --global
```

For installation without interactive selection, specify the agents directly:

```sh
npx --yes skills@latest add webjetcms/skills --skill '*' --agent codex github-copilot claude-code --yes
```

The `--copy` parameter creates separate copies instead of symbolic links. This is useful, for example, on Windows without symlink support. The installation does not require adding a dependency to the project `package.json`.

If you only need a specific procedure, select the skill and its related skills. For example, for DataTable applications:

```sh
npx skills@latest add webjetcms/skills --skill wj-datatable wj-codeceptjs wj-project-context wj-setup
```

The installer does not automatically fill in dependencies between skills. It may overwrite existing files; when migrating from manual copies, follow the section below first.

## Link to project rules

After installation, enter the following into the agent:

> Use wj-setup to set up WebJET CMS rules for this project. Keep its existing instructions.

Skill `wj-setup` will add the marked blocks to `AGENTS.md`, `.github/copilot-instructions.md` and `CLAUDE.md`. It will also take into account the existing root `AGENTS.override.md`. Custom instructions outside the marked blocks will be preserved. Re-running will not create duplicates.

For project installation, you can also run the helper script manually:

```sh
node .agents/skills/wj-setup/scripts/setup-project.mjs --project . --scope project
```

To preview the changes, add `--dry-run`. For global installation, the agent finds the script in the installed skill and uses `--scope global` ; only the specified project is still modified. The personal absolute path is not written to shared instructions, and the global instruction files are not changed.

Write your own rules outside the `<!-- webjet-skills:start -->` and `<!-- webjet-skills:end -->` tags. If the tags are broken or the instruction files are symbolic links, the script will reject the change and inform you why. Please check and link such files manually.

`npx skills add` does not run the project setup itself. You can send a single request to the agent for both installation and setup:

> Install all official WebJET CMS skills from https://github.com/webjetcms/skills into this project via npx skills for the agent I'm currently using. Before replacing existing skills, save and compare any manual modifications. Then use wj-setup and attach wj-project-context to the project rules without overwriting your own instructions. Do not update WebJET CMS or basecms.

For a global installation, replace "to this project" with "globally for my user and then set the rules for this project". The agent needs terminal, network, and write access to the target folders. A link to the source is more reliable than just typing "WebJET skills".

## Usage and older projects

For example, enter "Create WebJET DataTable application" or explicitly name it `wj-datatable`. The agent can select the skill based on its description. If it has not loaded the changed project rules, start a new session.

Skill `wj-project-context` exposes the WebJET architecture and conventions. The agent should first verify the CMS version, available APIs, package names, translation files, SQL updates, and testing tools for the given project. Project guidelines take precedence over common recommendations.

A single current skillset does not mean that every historical version of the CMS supports all of the described features. If a feature or required tools are missing, the agent should report the limitation. Installing skills does not add video infrastructure, CodeceptJS helpers, external plugins, or logins.

## Update and pin a version

Run an interactive update and select the desired scope:

```sh
npx skills@latest update
```

Specifically, you can update WebJET skills without affecting others as follows:

```sh
npx skills@latest update wj-project-context wj-setup wj-datatable wj-codeceptjs --project
```

For global skills, use `--global`. After updating `wj-setup`, run it again. Local changes to installed skills are not automatically merged; keep custom customizations in project rules or in separately named skills.

If you need a specific revision, replace `DISTRIBUTION_COMMIT_SHA` with the full SHA of the commit from the **webjetcms/skills** repository:

```sh
npx skills@1.7.2 add https://github.com/webjetcms/skills/tree/DISTRIBUTION_COMMIT_SHA/skills --skill '*'
```

This is not the SHA of the source CMS listed in `source.json`. For pinned installations, change the revision explicitly; do not use the generic update command as a replacement for a managed release. Lock the installer file and store installed files according to team policy.

## Transition from manually copied skills

1. Check the `wj-*` folders in both the project and the agent global folders and back up your custom modifications outside of the installation target.
2. Compare them with the distribution. Move the customer rules to project instructions or a separately named skill.
3. Install the selected set and verify the skills catalog. Remove old duplicates only after comparison; both the project and global copies can be displayed simultaneously.
4. Run `wj-setup` and check the diff. The script does not delete old manually written references to `.github/agents` ; fix any conflicts when checking.

To uninstall, use `npx skills remove` for the selected WebJET skills and remove only the marked WebJET blocks from the project instructions.

## Distribution maintenance

Skills are edited in `.agents/skills` of the main WebJET CMS repository, agent documents in `.github/agents`. Distribution copies are not edited manually.

Exclude core development-only skills in the `.github/skills-distribution/excluded-skills.txt` file. On each line, specify the exact directory name from `.agents/skills`, such as `wj-cke-stylescombo`, which is already in the list. Blank lines and lines starting with `#` are ignored; patterns like `wj-*` are not supported. The export omits the entire skill directory, including references, scripts, and other files, as well as its manifest entry. The source skill remains in the main repository. Unknown names or exclusion of the required skills `wj-project-context` and `wj-setup` will stop the export with an error.

Changing the exclusion list will automatically trigger the existing workflow. The next time you publish, synchronization will also remove the excluded skill from the distribution repository if it was already published there.

To start the export from the main repository to a new folder:

```sh
node .github/scripts/export-skills.mjs --output /tmp/webjet-skills-export
node --test .github/scripts/skills.test.mjs
```

The export reads the working contents of files registered in the Git index. New files must therefore first be included in Git; untracked local skills are not exported. The script does not overwrite an existing target folder. A local export with uncommitted changes is used for review; a published export is created from a clean CI checkout. The manifest contains the source commit and the hash of the distribution contents, without a timestamp.

Before the first publication, the administrator:

1. Creates a public repository `webjetcms/skills` with an initialized branch `main`.
2. In the source repository, set the Actions secret `WEBJET_SKILLS_TOKEN`, whose token has the right `Contents: write` only for the distribution repository.
3. Allows this token to write to the distribution branch according to its protection rules.
4. After merging the implementation, the **WebJET skills** workflow will run on the `main` branch and verify the first installation from the public repository.

The workflow checks for PRs without a publish token. After relevant changes to `main` or a manual run, it exports the content and updates the distribution after successful tests. Matching content does not create a new distribution commit. The distribution repository is intended solely for the generated output; synchronization removes files that no longer belong in the export.

## Distribution verification

Automated tests check for export of watched files, metadata, bundled links, and preservation of custom project guidelines. The integration test uses the actual `skills@1.7.2` in temporary folders: it installs the entire package for the three agents project-wide and globally, with symlinks and via `--copy`, then replaces skills with newer content and re-sets the project. GitHub Actions runs these checks on both Ubuntu and Windows.

Before the first release, also verify the agent's behavior in specific projects:

| Task | Expected behavior |
| --- | --- |
| Creating an application in core and current basecms | The agent selects an existing application package, correct imports, and project translations. It does not create core-only files in the client project. |
| Code review in an older project without an internal changelog | The agent will review changes against the correct branch and will not report missing `docs/sk/CHANGELOG-<year> .md` if the project does not require it. |
| Request for newer API or test helper | The agent checks for availability in a specific dependency and configuration. If an API or helper is missing, it names a limitation without automatically updating the CMS. |

The installation test does not replace compiling the created application or testing against a running CMS. Verify the first download and update via the public source `webjetcms/skills` after it is published.
