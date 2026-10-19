# Použití AI skills

Skilly dávají AI asistentovi postup pro konkrétní úlohu. Nejprve dokončete [instalaci a nastavení skillů](../install/README.md#nastavení-ai-asistenta-codex). Následující zadání pište do **chatu Codexu otevřeného v projektu**, nikoli do terminálu. Text v hranatých závorkách nahraďte vlastním obsahem.

## Nový požadavek z e-mailu nebo dokumentu

1. Založte chat pro požadavek. Vložte text e-mailu a přiložte obrázky nebo dokument. Doplňte, co se má změnit a pro koho.
2. Spusťte **`grill-me`** pro vyjasnění zadání:

   ```text
   Použi skill grill-me na túto požiadavku:
   [text požiadavky]
   Zohľadni priložené obrázky a dokumenty aj existujúce riešenie vo WebJET CMS.
   Vyjasnime rozsah, očakávané správanie, oprávnenia a hraničné prípady.
   Zatiaľ neimplementuj.
   ```

3. Odpovídejte na otázky. Pokud něco nevíte, označte to jako otevřenou otázku pro zadavatele. Na závěr si nechte shrnout akceptační kritéria, navrhnout implementační plán a testy a potvrďte dohodnutý rozsah.
4. Ve stejném chatu vytvořte GitHub issue pomocí projektového skillu:

   ```text
   Použi skill wj-gh-issue-from-chat. Najprv over, či už v
   webjetcms/webjetcms neexistuje rovnaké issue. Ak nie, priprav nové
   z tejto diskusie vrátane akceptačných kritérií, dohodnutého plánu a testov.
   ```

5. Zkontrolujte návrh a potvrďte vytvoření issue. Skill použije anglický název, tělo ponechá v jazyce diskuse a navrhne název větve. Větev nevytváří. Pokračujte částí [Implementace a kontrola](#implementace-a-kontrola).

## Požadavek nebo chyba již existuje v GitHub Issues

1. Vložte odkaz na issue. Při chybě doplňte prostředí, verzi WebJET CMS, kroky reprodukce, očekávaný a skutečný výsledek, případně screenshot nebo log.
2. Při **nové funkčnosti nebo nejasném zadání** použijte:

   ```text
   Načítaj issue [URL] vrátane komentárov. Použi skill grill-me
   na vyjasnenie zadania vzhľadom na existujúci kód.
   Na záver zhrň akceptačné kritériá, implementačný plán a testy.
   Zatiaľ neimplementuj a nevytváraj nové issue.
   ```

   Při **nahlášené chybě** použijte:

   ```text
   Použi skill diagnosing-bugs na issue [URL] vrátane komentárov.
   Kroky reprodukcie a prostredie: [doplňte].
   Najprv chybu reprodukuj, nájdi príčinu a navrhni opravu aj jej overenie.
   Pred samotnou opravou počkaj na moje potvrdenie. Nevytváraj nové issue.
   ```

3. Potvrďte vyjasněné zadání nebo navrženou opravu. Pokud přibyla podstatná rozhodnutí, zapište je do stávajícího issue, aby byla dostupná i mimo chatu.

## Implementace a kontrola

1. Vytvořte nebo vyberte pracovní větev podle [pravidel projektu](gitlab.md). Pro novou funkčnost použijte `feature/<číslo-issue>-<popis>`, pro opravu `hotfix/<číslo-issue>-<popis>` ; vycházejte ze správné cílové větve.
2. V chatu s dohodnutým plánem zadejte:

   ```text
   Použi skill implement pre issue [URL] podľa dohodnutého plánu.
   Dodrž AGENTS.md a príslušné wj-* skilly. Spusti relevantné testy
   a doplň dokumentáciu. Nevytváraj commit ani push; zmeny nechaj na kontrolu.
   ```

3. Před commitem si vyžádejte projektovou kontrolu:

   ```text
   Použi skill wj-code-review na necommitnuté zmeny k issue [URL].
   Over splnenie akceptačných kritérií aj spätnú kompatibilitu.
   ```

4. Zkontrolujte diff, výsledky testů a chování aplikace podle [Definice hotového](definition-of-done.md). Commit a další Git postup provádí vývojář; zákaz commitů agentem platí i při použití obecného skillu `implement`.
