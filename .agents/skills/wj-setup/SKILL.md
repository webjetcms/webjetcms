---
name: wj-setup
description: Connect installed WebJET CMS skills to a project's persistent instructions for Codex, GitHub Copilot and Claude Code. Use after installing WebJET skills, when setting up an older basecms project, or when repairing the managed WebJET instruction blocks.
---

# Set Up WebJET Skills

Use this skill when the user requests WebJET agent setup. Installing skills with `npx skills add` does not run this setup automatically.

1. Resolve the intended project root from the user's request or current checkout. Inspect its existing instructions. Do not initialize a user's home directory or modify global instruction files.
2. Confirm that `wj-project-context` is available in the active skill catalog or in the project's `.agents/skills` or `.claude/skills`. If missing, install it in the same scope as this skill using `npx skills@latest add webjetcms/skills --skill wj-project-context --agent <agent>`; add `--global` only for a requested global installation. Resolve the agent to `codex`, `github-copilot` or `claude-code`.
3. Run the bundled helper with Node.js 22.20 or newer. Resolve `SKILL_DIR` to this installed skill, not to a fixed repository path:

   ```sh
   node "$SKILL_DIR/scripts/setup-project.mjs" --project "/absolute/project/path" --scope project
   ```

   Use `--scope global` when the context skill is installed globally. This still edits only the specified project's instruction files. A global reference uses the skill name, never a personal absolute path. Project scope requires a local context skill and produces relative links.
4. Report the changed files and any existing overriding instructions. The helper manages only marked blocks in `AGENTS.md`, an existing `AGENTS.override.md`, `.github/copilot-instructions.md` and `CLAUDE.md`. It preserves other text and is safe to rerun. If markers are damaged or paths are symlinks, report the error; inspect with the user rather than deleting or replacing their instructions.
5. Ask the agent to read `wj-project-context` for subsequent WebJET tasks. If the host has not picked up changed persistent instructions, start a new session. Do not claim that installation adds unavailable APIs, test infrastructure, plugins or credentials.

For a preview, add `--dry-run`. All files are checked before any write. No CMS dependencies, application files, Git commits or global instructions are changed by the helper.
