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
