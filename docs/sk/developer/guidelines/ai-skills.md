# Použitie AI skills

Skilly dávajú AI asistentovi postup pre konkrétnu úlohu. Najprv dokončite [inštaláciu a nastavenie skillov](../install/README.md#nastavenie-ai-asistenta-codex). Nasledujúce zadania píšte do **chatu Codexu otvoreného v projekte**, nie do terminálu. Text v hranatých zátvorkách nahraďte vlastným obsahom.

## Nová požiadavka z e-mailu alebo dokumentu

1. Založte chat pre požiadavku. Vložte text e-mailu a priložte obrázky alebo dokument. Doplňte, čo sa má zmeniť a pre koho.
2. Spustite **`grill-me`** na vyjasnenie zadania:

   ```text
   Použi skill grill-me na túto požiadavku:
   [text požiadavky]
   Zohľadni priložené obrázky a dokumenty aj existujúce riešenie vo WebJET CMS.
   Vyjasnime rozsah, očakávané správanie, oprávnenia a hraničné prípady.
   Zatiaľ neimplementuj.
   ```

3. Odpovedajte na otázky. Ak niečo neviete, označte to ako otvorenú otázku pre zadávateľa. Na záver si nechajte zhrnúť akceptačné kritériá, navrhnúť implementačný plán a testy a potvrďte dohodnutý rozsah.

Po vyjasnení zadania si vyberte jednu z nasledujúcich možností.

### Uložiť zadanie do GitHub issue

1. V tom istom chate vytvorte GitHub issue pomocou projektového skillu:

   ```text
   Použi skill wj-gh-issue-from-chat. Najprv over, či už v
   webjetcms/webjetcms neexistuje rovnaké issue. Ak nie, priprav nové
   z tejto diskusie vrátane akceptačných kritérií, dohodnutého plánu a testov.
   ```

2. Skontrolujte návrh a potvrďte vytvorenie issue. Skill použije anglický názov, telo ponechá v jazyku diskusie a navrhne názov vetvy. Vetvu nevytvára. Pokračujte časťou [Implementácia a kontrola](#implementácia-a-kontrola).

### Implementovať priamo z chatu bez issue

1. Vyberte pracovnú vetvu alebo worktree podľa časti [Implementácia a kontrola](#implementácia-a-kontrola). Ak už pracujete v správnom priečinku a vetve, pokračujte v nich.
2. **V tom istom chate**, ktorý obsahuje požiadavku, prílohy a odsúhlasené závery z `grill-me`, zadajte:

   ```text
   Použi skill implement podľa odsúhlaseného zadania, akceptačných kritérií,
   plánu a testov v tomto chate. Nevytváraj GitHub issue.
   ```

   Skill `implement` prijíma aj zadanie priamo z konverzácie; číslo issue nepotrebuje. Ak prechádzate do nového chatu alebo inej worktree, preneste do nového chatu odsúhlasené zhrnutie a potrebné prílohy.
3. Po implementácii pokračujte kontrolou kódu a výsledku v krokoch 3 a 4 časti [Implementácia a kontrola](#implementácia-a-kontrola). Pri kontrole namiesto odkazu na issue uveďte zadanie odsúhlasené v chate.

## Požiadavka alebo chyba už existuje v GitHub Issues

1. Vložte odkaz na issue. Pri chybe doplňte prostredie, verziu WebJET CMS, kroky reprodukcie, očakávaný a skutočný výsledok, prípadne screenshot alebo log.
2. Pri **novej funkčnosti alebo nejasnom zadaní** použite:

   ```text
   Načítaj issue [URL] vrátane komentárov. Použi skill grill-me
   na vyjasnenie zadania vzhľadom na existujúci kód.
   Na záver zhrň akceptačné kritériá, implementačný plán a testy.
   Zatiaľ neimplementuj a nevytváraj nové issue.
   ```

   Pri **nahlásenej chybe** použite:

   ```text
   Použi skill diagnosing-bugs na issue [URL] vrátane komentárov.
   Kroky reprodukcie a prostredie: [doplňte].
   Najprv chybu reprodukuj, nájdi príčinu a navrhni opravu aj jej overenie.
   Pred samotnou opravou počkaj na moje potvrdenie. Nevytváraj nové issue.
   ```

3. Potvrďte vyjasnené zadanie alebo navrhnutú opravu. Potom **v tom istom chate** preneste dohodnuté závery do existujúceho issue:

   ```text
   Použi skill wj-gh-issue-from-grill pre issue [URL].
   Zapracuj odsúhlasené závery tejto diskusie do jeho popisu.
   ```

   Skill zachová pôvodné zadanie a doplní alebo aktualizuje rozhodnutia, akceptačné kritériá, implementačný plán, testovanie a prípadné otvorené otázky. Nahradí prekonané časti plánu bez vytvorenia nového issue a nezačne implementáciu. `wj-gh-issue-from-chat` používajte iba na vytvorenie nového issue.
4. Otvorte vrátený odkaz a skontrolujte aktualizovaný popis. Plán je teraz uložený v issue. Pri implementácii môžete pokračovať aj v novom chate iba s jeho odkazom.

## Implementácia a kontrola

1. **Pred implementáciou vyberte vetvu alebo worktree, v ktorej budete pracovať.** Môžete pokračovať v aktuálnej vetve, použiť existujúcu worktree alebo vytvoriť novú. Viaceré menšie issues môžete riešiť v jednej vetve alebo worktree. Ak chcete novú worktree, z koreňa repozitára spustite v termináli napríklad:

   ```shell
   ./worktree-add.sh --new feature/ID-short-description
   ```

   ID a popis nahraďte podľa issue. Skript vytvorí novú vetvu z `origin/main`, pripraví worktree so závislosťami a otvorí ju vo VS Code.

   Pre release hotfix najprv vytvorte vetvu `hotfix/<číslo-issue>-<popis>` zo zodpovedajúcej vetvy `hotfix/YEAR.0-main` podľa [pravidiel projektu](gitlab.md), potom spustite `./worktree-add.sh hotfix/<číslo-issue>-<popis>` bez `--new`. Parameter `--new` vždy vychádza z `origin/main`, aj keď názov novej vetvy začína na `hotfix/`.
2. V Codexe otvorte zvolený pracovný priečinok. Ak máte GitHub issue, v rovnakom alebo novom chate spustite skill `implement` s jeho číslom (bez issue použite [zadanie priamo z chatu](#implementovať-priamo-z-chatu-bez-issue)):

   ```text
   /implement ID
   ```

   `ID` nahraďte číslom svojho issue, napríklad `302`. Nastavenie tohto repozitára v `docs/agents/issue-tracker.md` určuje GitHub projekt `webjetcms/webjetcms` a načítanie issue aj komentárov. Zadanie, akceptačné kritériá a plán preto najprv uložte do issue podľa postupu vyššie.

   Namiesto čísla môžete použiť `/implement [URL]`. Ak rozhranie neponúka skratku `/implement`, napíšte `Použi skill implement pre issue [URL].` Skill načíta ticket a vykoná implementáciu a testovanie podľa zadania a pravidiel projektu.

3. Pred commitom si vyžiadajte projektovú kontrolu:

   ```text
   Použi skill wj-code-review na necommitnuté zmeny k issue [URL].
   Over splnenie akceptačných kritérií aj spätnú kompatibilitu.
   ```

4. Skontrolujte diff, výsledky testov a správanie aplikácie podľa [Definície hotového](definition-of-done.md). Commit a ďalší Git postup vykonáva vývojár. Zákaz vytvárania commitov agentom v projektovom `AGENTS.md` má prednosť pred záverečným commit krokom všeobecného skillu `implement`.

## Kedy použiť implement a kedy implement-spec

| Skill | Vstup | Postup |
| --- | --- | --- |
| `implement` | Jedno issue, zadanie alebo vybraná skupina ticketov. | Implementuje zadanú prácu, používa TDD tam, kde je vhodné, spúšťa testy a na konci kontrolu kódu. |
| `implement-spec` | Celá špecifikácia rozdelená na súvisiace tickety, typicky výsledok `to-spec` a `to-tickets`. | Riadi závislosti medzi ticketmi a deleguje ich subagentom do samostatných vetiev a worktree. Výsledky zlučuje do spoločnej integračnej vetvy a kontroluje celé riešenie. |

**Pre náš bežný postup použite `implement`**, aj keď issue obsahuje podrobný plán z `grill-me`. Samotná dĺžka plánu ani viac menších issues v jednej vetve nevyžadujú `implement-spec`.

`implement-spec` je určený na koordináciu väčšej práce rozdelenej na závislé tickety. Jeho predvolený postup vytvára vetvy a worktree, zlučuje zmeny a podľa spôsobu práce s issue trackerom rieši aj PR alebo uzatváranie ticketov. Vo WebJET CMS ho treba vopred prispôsobiť pravidlám projektu, najmä zákazu commitov agentom a voľbe vetvy programátorom. Pre jednotlivé issue alebo zadanie v chate použite postup v časti [Implementácia a kontrola](#implementácia-a-kontrola).
