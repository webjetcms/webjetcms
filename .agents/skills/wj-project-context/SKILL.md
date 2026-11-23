---
name: wj-project-context
description: Apply WebJET CMS architecture, conventions and version-aware guidance when developing or reviewing WebJET core or a customer project based on basecms. Use for WebJET APIs, project layout, DataTables, components, configuration and design conventions; not for unrelated projects.
---

# WebJET Project Context

Read the current project's instructions first. Its explicit conventions take precedence over these shared defaults. Resolve project paths against the current project root, and skill resources against this skill's directory, including for global installations.

## Identify the project

- Inspect the build configuration, existing application packages and relevant source files. A customer project normally consumes WebJET as a dependency; the core repository contains the framework sources. Do not identify core solely from a directory name or the presence of these installed skills.
- Determine the WebJET dependency version, Java version and `javax` versus `jakarta` imports from the project. Check the relevant API in existing code, dependency sources or version-matched documentation before copying an example. State uncertainty if compatibility cannot be established; do not upgrade CMS or basecms to make an example work.
- Discover the installation name, application package, language bundles, SQL migration convention and test commands from the project. Names such as `basecms` and `webjet9` in examples are placeholders, not universal defaults.
- Core changelog, roadmap, release and test-template conventions apply only to core development. Do not create those files or report them missing in a customer project unless that project requires them. Customer templates can be production code.

## Read the relevant knowledge

In an installed distribution, read `references/AGENTS.md` beside this skill. The exporter bundles the canonical `.github/agents` files there. When editing skills in the WebJET source repository, that generated directory is absent: use the repository's `.github/agents/AGENTS.md` instead, after confirming this is the core checkout. Do not create a second maintained copy.

Start with the index, then read only the relevant topic:

| Task | Reference |
| --- | --- |
| Configuration, persistence infrastructure, auditing | `AGENTS-system.md` |
| Admin CRUD, filtering, REST and validation | `AGENTS-datatable.md` |
| Editor and approval workflows | `AGENTS-editor.md` |
| Pages, groups, templates and history | `AGENTS-doc.md` |
| Users, permissions and passwords | `AGENTS-users.md` |
| Applications and components | `AGENTS-components.md` |
| Shared helpers | `AGENTS-common.md`, `AGENTS-utils.md` |
| Administration presentation | `AGENTS-design.md` |

References describe the current framework. Verify availability in the target version; source paths in them describe core and need not exist in a customer checkout. Do not edit dependency internals or compiled vendor assets to compensate for missing core sources.

Use specialized installed `wj-*` skills by name when relevant. A separately selected skill may not have all companion skills installed: report a missing companion and provide its installation command instead of assuming a sibling path exists. Install instructions are in the distribution README. Load only the references needed for the current task.
