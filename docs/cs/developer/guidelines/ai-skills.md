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

Po vyjasnění zadání si vyberte jednu z následujících možností.

### Uložit zadání do GitHub issue

1. Ve stejném chatu vytvořte GitHub issue pomocí projektového skillu:

   ```text
   Použi skill wj-gh-issue-from-chat. Najprv over, či už v
   webjetcms/webjetcms neexistuje rovnaké issue. Ak nie, priprav nové
   z tejto diskusie vrátane akceptačných kritérií, dohodnutého plánu a testov.
   ```

2. Zkontrolujte návrh a potvrďte vytvoření issue. Skill použije anglický název, tělo ponechá v jazyce diskuse a navrhne název větve. Větev nevytváří. Pokračujte částí [Implementace a kontrola](#implementace-a-kontrola).

### Implementovat přímo z chatu bez issue

1. Vyberte pracovní větev nebo worktree podle části [Implementace a kontrola](#implementace-a-kontrola). Pokud již pracujete ve správné složce a větvi, pokračujte v nich.
2. **V tomtéž chatu**, který obsahuje požadavek, přílohy a odsouhlasené závěry z `grill-me`, zadejte:

   ```text
   Použi skill implement podľa odsúhlaseného zadania, akceptačných kritérií,
   plánu a testov v tomto chate. Nevytváraj GitHub issue.
   ```

   Skill `implement` přijímá i zadání přímo z konverzace; číslo issue nepotřebuje. Pokud přecházíte do nového chatu nebo jiné worktree, přeneste do nového chatu odsouhlasené shrnutí a potřebné přílohy.
3. Po implementaci pokračujte kontrolou kódu a výsledku v krocích 3 a 4 části [Implementace a kontrola](#implementace-a-kontrola). Při kontrole místo odkazu na issue uveďte zadání odsouhlasené v chatu.

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

3. Potvrďte vyjasněné zadání nebo navrženou opravu. Potom **v tomtéž chatě** přeneste dohodnuté závěry do stávajícího issue:

   ```text
   Použi skill wj-gh-issue-from-grill pre issue [URL].
   Zapracuj odsúhlasené závery tejto diskusie do jeho popisu.
   ```

   Skill zachová původní zadání a doplní nebo aktualizuje rozhodnutí, akceptační kritéria, implementační plán, testování a případné otevřené otázky. Nahradí překonané části plánu bez vytvoření nového issue a nezačne implementaci. `wj-gh-issue-from-chat` používejte pouze k vytvoření nového issue.
4. Otevřete vrácený odkaz a zkontrolujte aktualizovaný popis. Plán je nyní uložen v issue. Při implementaci můžete pokračovat iv novém chatu pouze s jeho odkazem.

## Implementace a kontrola

1. **Před implementací vyberte větev nebo worktree, ve které budete pracovat.** Můžete pokračovat v aktuální větvi, použít existující worktree nebo vytvořit novou. Několik menší issues můžete řešit v jedné větvi nebo worktree. Chcete-li novou worktree, z kořene repozitáře spusťte v terminálu například:

   ```shell
   ./worktree-add.sh --new feature/ID-short-description
   ```

   ID a popis nahraďte podle issue. Skript vytvoří novou větev z `origin/main`, připraví worktree se závislostmi a otevře ji ve VS Code.

   Pro release hotfix nejprve vytvořte větev `hotfix/<číslo-issue>-<popis>` z odpovídající větve `hotfix/YEAR.0-main` podle [pravidel projektu](gitlab.md), poté spusťte `./worktree-add.sh hotfix/<číslo-issue>-<popis>` bez `--new`. Parametr `--new` vždy vychází z `origin/main`, i když název nové větve začíná na `hotfix/`.
2. V Codexu otevřete zvolenou pracovní složku. Pokud máte GitHub issue, ve stejném nebo novém chatu spusťte skill `implement` s jeho číslem (bez issue použijte [zadání přímo z chatu](#implementovat-přímo-z-chatu-bez-issue)):

   ```text
   /implement ID
   ```

   `ID` nahraďte číslem svého issue, například `302`. Nastavení tohoto repozitáře v `docs/agents/issue-tracker.md` určuje GitHub projekt `webjetcms/webjetcms` a načtení issue i komentářů. Zadání, akceptační kritéria a plán proto nejprve uložte do issue podle postupu výše.

   Namísto čísla můžete použít `/implement [URL]`. Pokud rozhraní nenabízí zkratku `/implement`, napište `Použi skill implement pre issue [URL].` Skill načte ticket a provede implementaci a testování podle zadání a pravidel projektu.

3. Před commitem si vyžádejte projektovou kontrolu:

   ```text
   Použi skill wj-code-review na necommitnuté zmeny k issue [URL].
   Over splnenie akceptačných kritérií aj spätnú kompatibilitu.
   ```

4. Zkontrolujte diff, výsledky testů a chování aplikace podle [Definice hotového](definition-of-done.md). Commit a další Git postup provádí vývojář. Zákaz vytváření commitů agentem v projektovém `AGENTS.md` má přednost před závěrečným commit krokem obecného skillu `implement`.

## Kdy použít implement a kdy implement-spec

| Skill | Vstup | Postup |
| --- | --- | --- |
| `implement` | Jedno issue, zadání nebo vybraná skupina ticketů. | Implementuje zadanou práci, používá TDD tam, kde je vhodné, spouští testy a na konci kontrolu kódu. |
| `implement-spec` | Celá specifikace rozdělena na související tickety, typicky výsledek `to-spec` a `to-tickets`. | Řídí závislosti mezi tikety a deleguje je subagentem do samostatných větví a worktree. Výsledky slučuje do společné integrační větve a kontroluje celé řešení. |

**Pro náš běžný postup použijte `implement`**, i když issue obsahuje podrobný plán z `grill-me`. Samotná délka plánu ani více menších issues v jedné větvi nevyžadují `implement-spec`.

`implement-spec` je určen ke koordinaci větší práce rozdělené na závislé tickety. Jeho výchozí postup vytváří větve a worktree, slučuje změny a podle způsobu práce s issue trackerem řeší také PR nebo uzavírání ticketů. Ve WebJET CMS je třeba jej předem přizpůsobit pravidlům projektu, zejména zákazu commitů agentům a volbě větve programátorům. Pro jednotlivé issue nebo zadání v chatu použijte postup v části [Implementace a kontrola](#implementace-a-kontrola).
