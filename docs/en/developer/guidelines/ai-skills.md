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
4. In the same chat, create a GitHub issue using the project skill:

   ```text
   Použi skill wj-gh-issue-from-chat. Najprv over, či už v
   webjetcms/webjetcms neexistuje rovnaké issue. Ak nie, priprav nové
   z tejto diskusie vrátane akceptačných kritérií, dohodnutého plánu a testov.
   ```

5. Review the suggestion and confirm the issue creation. The skill will use the English title, leave the body in the language of the discussion, and suggest a branch name. It will not create a branch. Continue with the [Implementation and Control](#implementation-and-control) section.

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

3. Confirm the clarified assignment or suggested correction. If there are any significant decisions, write them into an existing issue so that they are available outside of the chat.

## Implementation and control

1. Create or select a working branch according to [project rules](gitlab.md). Use `feature/<číslo-issue>-<popis>` for new functionality, `hotfix/<číslo-issue>-<popis>` for a fix; start from the correct target branch.
2. In the chat with the agreed plan, enter:

   ```text
   Použi skill implement pre issue [URL] podľa dohodnutého plánu.
   Dodrž AGENTS.md a príslušné wj-* skilly. Spusti relevantné testy
   a doplň dokumentáciu. Nevytváraj commit ani push; zmeny nechaj na kontrolu.
   ```

3. Request a project review before committing:

   ```text
   Použi skill wj-code-review na necommitnuté zmeny k issue [URL].
   Over splnenie akceptačných kritérií aj spätnú kompatibilitu.
   ```

4. Check diff, test results, and application behavior against the [Definition of Done](definition-of-done.md). Commit and other Git actions are performed by the developer; the prohibition of agent commits also applies when using the general skill `implement`.
