# Using AI skills

Skills give the AI ​​assistant a procedure for a specific task. First, complete [installation and setup of skills](../install/README.md#nastavenie-ai-asistenta-codex). Type the following commands in **the Codex chat opened in the project**, not in the terminal. Replace the text in square brackets with your own content.

## New request from email or document

1. Create a chat for the request. Enter the text of the email and attach images or a document. Fill in what needs to be changed and for whom.
2. Run **`grill-me`** to clarify the assignment:

   ```text
   Použi skill grill-me na túto požiadavku:
   [text požiadavky]
   Zohľadni priložené obrázky a dokumenty aj existujúce riešenie vo WebJET CMS.
   Vyjasnime rozsah, očakávané správanie, oprávnenia a hraničné prípady.
   Zatiaľ neimplementuj.
   ```

3. Answer the questions. If you don't know something, mark it as an open question for the sponsor. Finally, have the acceptance criteria summarized, an implementation plan and tests proposed, and the agreed scope confirmed.

After clarifying the assignment, choose one of the following options.

### Save the assignment to a GitHub issue

1. In the same chat, create a GitHub issue using the project skill:

   ```text
   Použi skill wj-gh-issue-from-chat. Najprv over, či už v
   webjetcms/webjetcms neexistuje rovnaké issue. Ak nie, priprav nové
   z tejto diskusie vrátane akceptačných kritérií, dohodnutého plánu a testov.
   ```

2. Review the suggestion and confirm the issue creation. The skill will use the English title, leave the body in the language of the discussion, and suggest a branch name. It will not create a branch. Continue with the [Implementation and Control](#implementation-and-control) section.

### Implement directly from chat without issue

1. Select a working branch or worktree according to the [Implementation and Review](#implementation-and-review) section. If you are already working in the correct folder and branch, continue in them.
2. **In the same chat** that contains the request, attachments, and agreed conclusions from `grill-me`, enter:

   ```text
   Použi skill implement podľa odsúhlaseného zadania, akceptačných kritérií,
   plánu a testov v tomto chate. Nevytváraj GitHub issue.
   ```

   Skill `implement` also accepts input directly from the conversation; it does not need an issue number. If you are moving to a new chat or another worktree, transfer the agreed summary and necessary attachments to the new chat.
3. After implementation, continue checking the code and result in steps 3 and 4 of the [Implementation and review](#implementation-and-review). When checking, instead of a link to the issue, include the assignment agreed in the chat.

## The request or bug already exists in GitHub Issues

1. Insert a link to the issue. In the case of an error, add the environment, WebJET CMS version, reproduction steps, expected and actual result, or a screenshot or log.
2. For **new functionality or unclear input** use:

   ```text
   Načítaj issue [URL] vrátane komentárov. Použi skill grill-me
   na vyjasnenie zadania vzhľadom na existujúci kód.
   Na záver zhrň akceptačné kritériá, implementačný plán a testy.
   Zatiaľ neimplementuj a nevytváraj nové issue.
   ```

   For **reported error** use:

   ```text
   Použi skill diagnosing-bugs na issue [URL] vrátane komentárov.
   Kroky reprodukcie a prostredie: [doplňte].
   Najprv chybu reprodukuj, nájdi príčinu a navrhni opravu aj jej overenie.
   Pred samotnou opravou počkaj na moje potvrdenie. Nevytváraj nové issue.
   ```

3. Confirm the clarified assignment or suggested correction. Then **in the same chat** transfer the agreed conclusions to the existing issue:

   ```text
   Použi skill wj-gh-issue-from-grill pre issue [URL].
   Zapracuj odsúhlasené závery tejto diskusie do jeho popisu.
   ```

   The skill will keep the original assignment and add or update decisions, acceptance criteria, implementation plan, testing, and any open questions. It will replace outdated parts of the plan without creating a new issue and will not start implementation. Use `wj-gh-issue-from-chat` only to create a new issue.
4. Open the returned link and check the updated description. The plan is now saved in the issue. You can also continue the implementation in a new chat with just its link.

## Implementation and control

1. **Before implementing, choose a branch or worktree to work on.** You can continue in the current branch, use an existing worktree, or create a new one. You can work on multiple smaller issues in a single branch or worktree. To create a new worktree, from the root of the repository, run in a terminal, for example:

   ```shell
   ./worktree-add.sh --new feature/ID-short-description
   ```

   Replace the ID and description with the issue. The script will create a new branch from `origin/main`, prepare a worktree with dependencies, and open it in VS Code.

   For a hotfix release, first create branch `hotfix/<číslo-issue>-<popis>` from the corresponding branch `hotfix/YEAR.0-main` according to [project rules](gitlab.md), then run `./worktree-add.sh hotfix/<číslo-issue>-<popis>` without `--new`. The `--new` parameter always comes from `origin/main`, even if the new branch name starts with `hotfix/`.
2. Open the selected working folder in Codex. If you have a GitHub issue, in the same or a new chat, run the skill `implement` with its number (without an issue, use [direct input from chat](#implement-direct-from-chat-without-issue)):

   ```text
   /implement ID
   ```

   Replace `ID` with your issue number, for example `302`. Setting this repository in `docs/agents/issue-tracker.md` determines the GitHub project `webjetcms/webjetcms` and loading the issue and comments. Therefore, first save the assignment, acceptance criteria, and plan to the issue as described above.

   Instead of a number, you can use `/implement [URL]`. If the interface does not offer the shortcut `/implement`, type `Použi skill implement pre issue [URL].` The skill will load the ticket and perform implementation and testing according to the assignment and project rules.

3. Request a project review before committing:

   ```text
   Použi skill wj-code-review na necommitnuté zmeny k issue [URL].
   Over splnenie akceptačných kritérií aj spätnú kompatibilitu.
   ```

4. Check diff, test results, and application behavior against the [Definition of Done](definition-of-done.md). Commit and other Git actions are performed by the developer. Disallowing agents from creating commits in the project `AGENTS.md` takes precedence over the final commit step of the general skill `implement`.

## When to use implement and when to use implement-spec

| Skill | Entrance | Procedure |
| --- | --- | --- |
| `implement` | A single issue, assignment, or selected group of tickets. | Implements assigned work, uses TDD where appropriate, runs tests, and finally reviews the code. |
| `implement-spec` | The entire specification divided into related tickets, typically the result of `to-spec` and `to-tickets`. | It manages dependencies between tickets and delegates them to subagents in separate branches and worktrees. It merges the results into a common integration branch and controls the entire solution. |

**For our normal procedure, use `implement`** even if the issue contains a detailed plan from `grill-me`. The length of the plan itself and multiple smaller issues in one branch do not require `implement-spec`.

`implement-spec` is designed to coordinate larger work divided into dependent tickets. Its default procedure creates branches and worktrees, merges changes and, depending on the way you work with the issue tracker, also handles PR or ticket closing. In WebJET CMS, it must be adapted in advance to the project rules, especially the prohibition of commits by agents and the choice of branch by programmers. For individual issues or chat assignments, use the procedure in the [Implementation and control](#implementation-and-control) section.

## WebJET skills overview

Select a skill based on the task and copy the example assignment into the AI ​​assistant chat opened in the project. Replace the data in square brackets with your own. The skill name refers to its complete procedure and requirements. The assistant can also select the skill automatically based on the assignment; explicitly specifying the name will specify the required procedure.

The overview contains WebJET skills from this repository. General skills like `grill-me` or `implement` are installed separately according to the [setup instructions](../install/README.md#nastavenie-ai-asistenta-codex). The skill marked **Iba core** is not part of the `webjetcms/skills` distribution.

<!-- webjet-skills-catalog:start -->

| Skill | What is it for and when to use it? | Example of chat entry |
| --- | --- | --- |
| [`wj-accessibility`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-accessibility/SKILL.md) | Checks and improves web accessibility according to WCAG 2.2, such as keyboard controls, element descriptions, and screen reader support. | Use the wj-accessibility skill to check the accessibility of the form [URL]. |
| [`wj-add-javadoc`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-add-javadoc/SKILL.md) | It adds and corrects the English JavaDoc of classes and methods, including parameters, return values, and exceptions. It only changes comments. | Use the skill wj-add-javadoc for class [path to Java file]. |
| [`wj-add-jsdoc`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-add-jsdoc/SKILL.md) | It completes and checks English JSDoc in JavaScript according to the actual behavior of functions and their interface. It only changes comments. | Use the wj-add-jsdoc skill for JavaScript files changed in this branch. |
| [`wj-add-junit-comments`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-add-junit-comments/SKILL.md) | It documents in English what JUnit test classes and methods verify. It does not change the behavior of the tests. | Use the skill wj-add-junit-comments for test [path to Java file]. |
| [`wj-anti-slop`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-anti-slop/SKILL.md) | It refines the Slovak documentation, removes fluff, and explains functions through user tasks. It preserves technical meaning. | Use the wj-anti-slop skill for documentation [path to Markdown file]. |
| [`wj-appstore-app`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-appstore-app/SKILL.md) | It creates applications embedded in pages via the AppStore, including Spring MVC components, Thymeleaf views, and editor parameters. | Use the wj-appstore-app skill to create an application [description and parameters]. |
| [`wj-cke-stylescombo`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-cke-stylescombo/SKILL.md) | Develops and debugs the Styles menu in CKEditor 4, its selection, undo, and built plugin integration. **Core only:** Also requires the libs-ckeditor4 repository; not distributed via webjetcms/skills. | Use the skill wj-cke-stylescombo to fix the Styles menu: [error and reproduction steps]. |
| [`wj-code-review`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-code-review/SKILL.md) | It checks the correctness of changes, backward compatibility, and code quality before committing or merging a PR. | Use the wj-code-review skill for uncommitted changes to the [issue description or URL] task. |
| [`wj-codeceptjs`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-codeceptjs/SKILL.md) | Writes and edits E2E tests via CodeceptJS and Playwright with existing WebJET CMS helper functions. | Use the wj-codeceptjs skill to test creating, editing, and deleting a record in [application]. |
| [`wj-codeceptjs-check`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-codeceptjs-check/SKILL.md) | Finds and reruns existing tests based on the list of failures from Allure. This is used to verify the fix; it does not change the tests. | Use the wj-codeceptjs-check skill for these failures from Allure on [environment URL]: [list]. |
| [`wj-confirm-issue`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-confirm-issue/SKILL.md) | Will review a specific code review finding or reported issue and propose a solution for approval before implementation. | Use the wj-confirm-issue skill to verify this finding: [finding and file]. |
| [`wj-content-marketing`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-content-marketing/SKILL.md) | From Git changes, it suggests topics and content for blogs, social posts, emails, or press releases about delivered features. | Use the wj-content-marketing skill and suggest blog topics from the changes in [commit range]. |
| [`wj-create-feature-video`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-create-feature-video/SKILL.md) | Prepares a product video from PR or branch: script, localized commentary, recordings, and editing with WebJET conclusion. Requires tools and approaches listed in the skill, including ElevenLabs and Tesseract. | Use the skill wj-create-feature-video for PR [URL], language Slovak, target group editors. |
| [`wj-dashboard-widget`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-dashboard-widget/SKILL.md) | Creates and edits admin home screen widgets, including data, settings, permissions, and responsive variants. | Use the wj-dashboard-widget skill to create a widget [data, settings, and permissions]. |
| [`wj-datatable`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-datatable/SKILL.md) | Creates and modifies administrative CRUD applications: JPA entity, repository, REST controller, DataTable page, and tests. | Use the wj-datatable skill to create a record [fields, rules, and permissions]. |
| [`wj-dependencycheck`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-dependencycheck/SKILL.md) | Evaluates OWASP Dependency Check findings based on severity and usability in WebJET CMS. Submits a proposal for decision before changing dependencies or exceptions. | Use the wj-dependencycheck skill on report [path], handle severities HIGH and CRITICAL. |
| [`wj-docs-pr`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-docs-pr/SKILL.md) | Updates the selected Markdown document according to all relevant changes in the PR or according to local changes. | Use the wj-docs-pr skill to update [document path] according to PR [URL]. |
| [`wj-gh-issue-from-chat`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-gh-issue-from-chat/SKILL.md) | Creates a draft of a new GitHub issue from the discussion, decisions, and plan, saves it after confirmation, and suggests a branch name. It does not create a branch. | Use the skill wj-gh-issue-from-chat and prepare an issue from the agreed assignment in this chat. |
| [`wj-gh-issue-from-grill`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-gh-issue-from-grill/SKILL.md) | It transfers the agreed conclusions of the discussion into an existing issue, including criteria, plan, and tests. It does not create a new issue or start implementation. | Use the skill wj-gh-issue-from-grill for issue [URL] and incorporate the agreed conclusions of this chat. |
| [`wj-page-builder`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-page-builder/SKILL.md) | Creates and edits PageBuilder HTML blocks, including nesting, editable areas, columns, and duplicatable elements. | Use the wj-page-builder skill to create a reference block with three columns and duplicable items. |
| [`wj-project-context`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-project-context/SKILL.md) | It loads the WebJET CMS architecture and conventions and verifies their applicability for a specific core version or customer project. | Use the skill wj-project-context and over to correctly read the configuration and translations in this project. |
| [`wj-sales-features`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-sales-features/SKILL.md) | Prepares a Slovak business description of functions from the PR, branch or annexes: benefits, advantages and value for the customer. | Use the wj-sales-features skill for PR [URL] and describe the benefits for the customer. |
| [`wj-setup`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-setup/SKILL.md) | Once installed, WebJET skills will connect to project guidelines for Codex, GitHub Copilot, and Claude Code. It will retain your own project rules. | Use the wj-setup skill to link the installed WebJET skills to this project. |
| [`wj-simplify-implementation`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-simplify-implementation/SKILL.md) | It simplifies implementation in PR or local changes and removes unnecessary complexity while preserving behavior. | Use the wj-simplify-implementation skill for uncommitted changes to this feature. |
| [`wj-stylescombo`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-stylescombo/SKILL.md) | Adds or restores style metadata in editor.css or editor.scss: names, categories, groups, and constraints according to the full template styles. | Use the skill wj-stylescombo: add missing metadata in [editor.scss], complete styles are in [ninja.scss], language Slovak. |
| [`wj-translate-comments`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-translate-comments/SKILL.md) | Translates Slovak comments in code into English without changing the logic or texts of the application. | Use the wj-translate-comments skill only on comments in [file or folder]. |
| [`wj-ui-review`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-ui-review/SKILL.md) | It checks the existing interface, its layout, typography, colors and interactions against WebJET CMS conventions. It makes specific visual corrections upon request. | Use the wj-ui-review skill on the [URL or screenshot] screen and list the findings without editing. |

<!-- webjet-skills-catalog:end -->

### How the report is published

The README of the [webjetcms/skills](https://github.com/webjetcms/skills) repository takes the table between the tags `webjet-skills-catalog:start` and `webjet-skills-catalog:end` from the English version `docs/en/developer/guidelines/ai-skills.md`. After editing the Slovak overview, run the translation and include the updated English version in the changes. When adding, renaming, or deleting a skill, also edit its row in the table. The export verifies the completeness of the list; the description and example should also be updated when the purpose of the skill changes.

The [WebJET skills](https://github.com/webjetcms/webjetcms/blob/main/.github/workflows/skills.yml) workflow validates the export during a pull request and publishes the distribution after pushing the relevant changes to `main`. It tracks skills, shared rules, export scripts, distribution template, English version of this document, and license. It can also be run manually via `workflow_dispatch` on branch `main`.

The README base is maintained in [.github/skills-distribution/README.md](https://github.com/webjetcms/webjetcms/blob/main/.github/skills-distribution/README.md). The [export-skills.mjs](https://github.com/webjetcms/webjetcms/blob/main/.github/scripts/export-skills.mjs) script inserts an English table into it during export and adjusts the links to the distributed files. Skills listed in [excluded-skills.txt](https://github.com/webjetcms/webjetcms/blob/main/.github/skills-distribution/excluded-skills.txt) are excluded from the distribution and from its table.

Publishing requires repository secret `WEBJET_SKILLS_TOKEN` with permission `contents:write` for `webjetcms/skills`. If the exported content has not changed, a new commit will not be created. Do not manually edit the README in the distribution repository, as another export will overwrite the changes.
