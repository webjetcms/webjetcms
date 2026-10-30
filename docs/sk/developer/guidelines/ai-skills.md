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

## Prehľad WebJET skills

Vyberte skill podľa úlohy a skopírujte príklad zadania do chatu AI asistenta otvoreného v projekte. Údaje v hranatých zátvorkách nahraďte vlastnými. Názov skillu odkazuje na jeho úplný postup a požiadavky. Asistent môže skill vybrať aj automaticky podľa zadania; výslovné uvedenie názvu určí požadovaný postup.

Prehľad obsahuje WebJET skilly z tohto repozitára. Všeobecné skilly ako `grill-me` alebo `implement` sa inštalujú samostatne podľa [návodu na nastavenie](../install/README.md#nastavenie-ai-asistenta-codex). Skill označený **Iba core** nie je súčasťou distribúcie `webjetcms/skills`.

<!-- webjet-skills-catalog:start -->
| Skill | Na čo slúži a kedy ho použiť | Príklad zadania do chatu |
| --- | --- | --- |
| [`wj-accessibility`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-accessibility/SKILL.md) | Kontroluje a zlepšuje prístupnosť webu podľa WCAG 2.2, napríklad ovládanie klávesnicou, popisy prvkov a podporu čítačiek obrazovky. | Použi skill wj-accessibility na kontrolu prístupnosti formulára [URL]. |
| [`wj-add-javadoc`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-add-javadoc/SKILL.md) | Dopĺňa a opravuje anglický JavaDoc tried a metód vrátane parametrov, návratových hodnôt a výnimiek. Mení iba komentáre. | Použi skill wj-add-javadoc pre triedu [cesta k Java súboru]. |
| [`wj-add-jsdoc`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-add-jsdoc/SKILL.md) | Dopĺňa a kontroluje anglický JSDoc v JavaScripte podľa skutočného správania funkcií a ich rozhrania. Mení iba komentáre. | Použi skill wj-add-jsdoc pre JavaScript súbory zmenené v tejto vetve. |
| [`wj-add-junit-comments`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-add-junit-comments/SKILL.md) | Dokumentuje po anglicky, čo overujú JUnit testovacie triedy a metódy. Správanie testov nemení. | Použi skill wj-add-junit-comments pre test [cesta k Java súboru]. |
| [`wj-anti-slop`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-anti-slop/SKILL.md) | Spresňuje slovenskú dokumentáciu, odstraňuje vatu a vysvetľuje funkcie cez úlohy používateľa. Zachováva technický význam. | Použi skill wj-anti-slop na dokumentáciu [cesta k Markdown súboru]. |
| [`wj-appstore-app`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-appstore-app/SKILL.md) | Vytvára aplikácie vkladané do stránok cez AppStore, vrátane Spring MVC komponentu, Thymeleaf zobrazenia a parametrov editora. | Použi skill wj-appstore-app na vytvorenie aplikácie [opis a parametre]. |
| [`wj-cke-stylescombo`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-cke-stylescombo/SKILL.md) | Vyvíja a ladí ponuku Štýly v CKEditore 4, jej výber, undo a integráciu zostaveného pluginu. **Iba core:** vyžaduje aj repozitár libs-ckeditor4; nedistribuuje sa cez webjetcms/skills. | Použi skill wj-cke-stylescombo na opravu ponuky Štýly: [chyba a kroky reprodukcie]. |
| [`wj-code-review`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-code-review/SKILL.md) | Kontroluje správnosť zmien, spätnú kompatibilitu a kvalitu kódu pred commitom alebo zlúčením PR. | Použi skill wj-code-review na necommitnuté zmeny k zadaniu [opis alebo URL issue]. |
| [`wj-codeceptjs`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-codeceptjs/SKILL.md) | Píše a upravuje E2E testy cez CodeceptJS a Playwright s existujúcimi pomocnými funkciami WebJET CMS. | Použi skill wj-codeceptjs na test vytvorenia, úpravy a zmazania záznamu v [aplikácia]. |
| [`wj-codeceptjs-check`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-codeceptjs-check/SKILL.md) | Vyhľadá a znovu spustí existujúce testy podľa zoznamu zlyhaní z Allure. Slúži na overenie opravy; testy nemení. | Použi skill wj-codeceptjs-check pre tieto zlyhania z Allure na [URL prostredia]: [zoznam]. |
| [`wj-confirm-issue`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-confirm-issue/SKILL.md) | Preverí konkrétny nález z kontroly kódu alebo nahlásený problém a navrhne riešenie na odsúhlasenie pred implementáciou. | Použi skill wj-confirm-issue na preverenie tohto nálezu: [nález a súbor]. |
| [`wj-content-marketing`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-content-marketing/SKILL.md) | Z Git zmien navrhuje témy a obsah blogov, sociálnych príspevkov, emailov či tlačových správ o dodaných funkciách. | Použi skill wj-content-marketing a navrhni blogové témy zo zmien v [rozsah commitov]. |
| [`wj-create-feature-video`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-create-feature-video/SKILL.md) | Pripravuje produktové video z PR alebo vetvy: scenár, lokalizovaný komentár, nahrávky a strih so záverom WebJET. Vyžaduje nástroje a prístupy uvedené v skille, vrátane ElevenLabs a Tesseract. | Použi skill wj-create-feature-video pre PR [URL], jazyk slovenčina, cieľová skupina redaktori. |
| [`wj-dashboard-widget`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-dashboard-widget/SKILL.md) | Vytvára a upravuje widgety úvodnej obrazovky administrácie vrátane dát, nastavení, oprávnení a responzívnych variantov. | Použi skill wj-dashboard-widget na vytvorenie widgetu [údaje, nastavenia a oprávnenia]. |
| [`wj-datatable`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-datatable/SKILL.md) | Vytvára a upravuje administračné CRUD aplikácie: JPA entitu, repository, REST controller, DataTable stránku a testy. | Použi skill wj-datatable na vytvorenie evidencie [polia, pravidlá a oprávnenia]. |
| [`wj-dependencycheck`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-dependencycheck/SKILL.md) | Vyhodnocuje nálezy OWASP Dependency Check podľa závažnosti a použiteľnosti vo WebJET CMS. Pred zmenou závislostí alebo výnimiek predloží návrh na rozhodnutie. | Použi skill wj-dependencycheck na report [cesta], spracuj závažnosti HIGH a CRITICAL. |
| [`wj-docs-pr`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-docs-pr/SKILL.md) | Aktualizuje vybraný Markdown dokument podľa všetkých relevantných zmien v PR alebo podľa lokálnych zmien. | Použi skill wj-docs-pr na aktualizáciu [cesta k dokumentu] podľa PR [URL]. |
| [`wj-gh-issue-from-chat`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-gh-issue-from-chat/SKILL.md) | Vytvorí návrh nového GitHub issue z diskusie, rozhodnutí a plánu, po potvrdení ho uloží a navrhne názov vetvy. Vetvu nevytvára. | Použi skill wj-gh-issue-from-chat a priprav issue z odsúhlaseného zadania v tomto chate. |
| [`wj-gh-issue-from-grill`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-gh-issue-from-grill/SKILL.md) | Prenáša odsúhlasené závery diskusie do existujúceho issue vrátane kritérií, plánu a testov. Nevytvára nové issue ani nezačína implementáciu. | Použi skill wj-gh-issue-from-grill pre issue [URL] a zapracuj odsúhlasené závery tohto chatu. |
| [`wj-page-builder`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-page-builder/SKILL.md) | Vytvára a upravuje HTML bloky PageBuildera vrátane vnorenia, editovateľných oblastí, stĺpcov a duplikovateľných prvkov. | Použi skill wj-page-builder na vytvorenie bloku referencií s tromi stĺpcami a duplikovateľnými položkami. |
| [`wj-project-context`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-project-context/SKILL.md) | Načíta architektúru a konvencie WebJET CMS a overí ich použiteľnosť pre verziu konkrétneho core alebo zákazníckeho projektu. | Použi skill wj-project-context a over, ako v tomto projekte správne čítať konfiguráciu a preklady. |
| [`wj-sales-features`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-sales-features/SKILL.md) | Pripravuje slovenský obchodný opis funkcií z PR, vetvy alebo príloh: prínosy, výhody a hodnotu pre zákazníka. | Použi skill wj-sales-features pre PR [URL] a opíš prínosy pre zákazníka. |
| [`wj-setup`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-setup/SKILL.md) | Po inštalácii prepojí WebJET skills s projektovými pokynmi pre Codex, GitHub Copilot a Claude Code. Zachová vlastné pravidlá projektu. | Použi skill wj-setup na prepojenie nainštalovaných WebJET skills s týmto projektom. |
| [`wj-simplify-implementation`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-simplify-implementation/SKILL.md) | Zjednodušuje implementáciu v PR alebo lokálnych zmenách a odstraňuje zbytočnú zložitosť pri zachovaní správania. | Použi skill wj-simplify-implementation na necommitnuté zmeny tejto funkcie. |
| [`wj-stylescombo`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-stylescombo/SKILL.md) | Dopĺňa alebo obnovuje metadáta štýlov v editor.css alebo editor.scss: názvy, kategórie, skupiny a obmedzenia podľa úplných štýlov šablóny. | Použi skill wj-stylescombo: doplň chýbajúce metadáta v [editor.scss], úplné štýly sú v [ninja.scss], jazyk slovenčina. |
| [`wj-translate-comments`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-translate-comments/SKILL.md) | Prekladá slovenské komentáre v kóde do angličtiny bez zmeny logiky alebo textov aplikácie. | Použi skill wj-translate-comments iba na komentáre v [súbor alebo priečinok]. |
| [`wj-ui-review`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-ui-review/SKILL.md) | Kontroluje existujúce rozhranie, jeho rozloženie, typografiu, farby a interakcie voči zvyklostiam WebJET CMS. Na požiadanie vykoná konkrétne vizuálne opravy. | Použi skill wj-ui-review na obrazovku [URL alebo screenshot] a vypíš nálezy bez úprav. |
<!-- webjet-skills-catalog:end -->

### Ako sa prehľad publikuje

README repozitára [webjetcms/skills](https://github.com/webjetcms/skills) preberá tabuľku medzi značkami `webjet-skills-catalog:start` a `webjet-skills-catalog:end` z anglickej verzie `docs/en/developer/guidelines/ai-skills.md`. Po úprave slovenského prehľadu spustite preklad a zahrňte aktualizovanú anglickú verziu do zmien. Pri pridaní, premenovaní alebo odstránení skillu upravte aj jeho riadok v tabuľke. Export overuje úplnosť zoznamu; popis a príklad treba aktualizovať aj pri zmene účelu skillu.

Workflow [WebJET skills](https://github.com/webjetcms/webjetcms/blob/main/.github/workflows/skills.yml) pri pull requeste overí export a po pushnutí relevantných zmien do `main` publikuje distribúciu. Sleduje skilly, zdieľané pravidlá, exportné skripty, distribučnú šablónu, anglickú verziu tohto dokumentu a licenciu. Dá sa spustiť aj ručne cez `workflow_dispatch` na vetve `main`.

Základ README sa udržiava v [.github/skills-distribution/README.md](https://github.com/webjetcms/webjetcms/blob/main/.github/skills-distribution/README.md). Skript [export-skills.mjs](https://github.com/webjetcms/webjetcms/blob/main/.github/scripts/export-skills.mjs) doň pri exporte vloží anglickú tabuľku a upraví odkazy na distribuované súbory. Skilly uvedené v [excluded-skills.txt](https://github.com/webjetcms/webjetcms/blob/main/.github/skills-distribution/excluded-skills.txt) vynechá z distribúcie aj z jej tabuľky.

Publikovanie vyžaduje repository secret `WEBJET_SKILLS_TOKEN` s oprávnením `contents:write` pre `webjetcms/skills`. Ak sa exportovaný obsah nezmenil, nový commit nevznikne. README v distribučnom repozitári neupravujte ručne, ďalší export by zmeny prepísal.
