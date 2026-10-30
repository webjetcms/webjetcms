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

## Přehled WebJET skills

Vyberte skill podle úkolu a zkopírujte příklad zadání do chatu AI asistenta otevřeného v projektu. Údaje v hranatých závorkách nahraďte vlastními. Název skillu odkazuje na jeho úplný postup a požadavky. Asistent může skill vybrat také automaticky podle zadání; výslovné uvedení názvu určí požadovaný postup.

Přehled obsahuje WebJET skilly z tohoto repozitáře. Obecné skilly jako `grill-me` nebo `implement` se instalují samostatně podle [návodu k nastavení](../install/README.md#nastavení-ai-asistenta-codex). Skill označený **Pouze core** není součástí distribuce `webjetcms/skills`.

<!-- webjet-skills-catalog:start -->

| Skill | K čemu slouží a kdy jej použít | Příklad zadání do chatu |
| --- | --- | --- |
| [`wj-accessibility`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-accessibility/SKILL.md) | Kontroluje a zlepšuje přístupnost webu podle WCAG 2.2, například ovládání klávesnicí, popisy prvků a podporu čteček obrazovky. | Použij skill wj-accessibility pro kontrolu přístupnosti formuláře [URL]. |
| [`wj-add-javadoc`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-add-javadoc/SKILL.md) | Doplňuje a opravuje anglický JavaDoc tříd a metod včetně parametrů, návratových hodnot a výjimek. Mění pouze komentáře. | Použij skill wj-add-javadoc pro třídu [cesta k Java souboru]. |
| [`wj-add-jsdoc`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-add-jsdoc/SKILL.md) | Doplňuje a kontroluje anglický JSDoc v JavaScriptu podle skutečného chování funkcí a jejich rozhraní. Mění pouze komentáře. | Použij skill wj-add-jsdoc pro JavaScript soubory změněné v této větvi. |
| [`wj-add-junit-comments`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-add-junit-comments/SKILL.md) | Dokumentuje anglicky, což ověřují JUnit testovací třídy a metody. Chování testů nemění. | Použij skill wj-add-junit-comments pro test [cesta k Java souboru]. |
| [`wj-anti-slop`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-anti-slop/SKILL.md) | Upřesňuje českou dokumentaci, odstraňuje vatu a vysvětluje funkce přes úkoly uživatele. Zachovává technický význam. | Použijte skill wj-anti-slop pro dokumentaci [cesta k Markdown souboru]. |
| [`wj-appstore-app`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-appstore-app/SKILL.md) | Vytváří aplikace vkládané do stránek přes AppStore, včetně Spring MVC komponenty, Thymeleaf zobrazení a parametrů editoru. | Použijte skill wj-appstore-app k vytvoření aplikace [popis a parametry]. |
| [`wj-cke-stylescombo`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-cke-stylescombo/SKILL.md) | Vyvíjí a ladí nabídku Styly v CKEditoru 4, její výběr, undo a integraci sestaveného pluginu. **Pouze core:** vyžaduje také repozitář libs-ckeditor4; nedistribuuje se přes webjetcms/skills. | Použij skill wj-cke-stylescombo k opravě nabídky Styly: [chyba a kroky reprodukce]. |
| [`wj-code-review`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-code-review/SKILL.md) | Kontroluje správnost změn, zpětnou kompatibilitu a kvalitu kódu před commitem nebo sloučením PR. | Použij skill wj-code-review k nekommitovaným změnám k zadání [popis nebo URL issue]. |
| [`wj-codeceptjs`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-codeceptjs/SKILL.md) | Píše a upravuje E2E testy přes CodeceptJS a Playwright se stávajícími pomocnými funkcemi WebJET CMS. | Použij skill wj-codeceptjs pro test vytvoření, úpravy a smazání záznamu v [aplikace]. |
| [`wj-codeceptjs-check`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-codeceptjs-check/SKILL.md) | Vyhledá a znovu spustí stávající testy podle seznamu selhání z Allure. Slouží k ověření opravy; testy nemění. | Použij skill wj-codeceptjs-check pro tato selhání z Allure na [URL prostředí]: [seznam]. |
| [`wj-confirm-issue`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-confirm-issue/SKILL.md) | Prověří konkrétní nález z kontroly kódu nebo nahlášený problém a navrhne řešení pro odsouhlasení před implementací. | Použij skill wj-confirm-issue k prověření tohoto nálezu: [nález a soubor]. |
| [`wj-content-marketing`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-content-marketing/SKILL.md) | Z Git změn navrhuje témata a obsah blogů, sociálních příspěvků, emailů či tiskových zpráv o dodaných funkcích. | Použij skill wj-content-marketing a navrhni blogová témata ze změn v [rozsah commitů]. |
| [`wj-create-feature-video`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-create-feature-video/SKILL.md) | Připravuje produktové video z PR nebo větve: scénář, lokalizovaný komentář, nahrávky a střih se závěrem WebJET. Vyžaduje nástroje a přístupy uvedené ve skille, včetně ElevenLabs a Tesseract. | Použij skill wj-create-feature-video pro PR [URL], jazyk čeština, cílová skupina redaktoři. |
| [`wj-dashboard-widget`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-dashboard-widget/SKILL.md) | Vytváří a upravuje widgety úvodní obrazovky administrace včetně dat, nastavení, oprávnění a responzivních variant. | Použijte skill wj-dashboard-widget k vytvoření widgetu [údaje, nastavení a oprávnění]. |
| [`wj-datatable`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-datatable/SKILL.md) | Vytváří a upravuje administrační CRUD aplikace: JPA entitu, repository, REST controller, DataTable stránku a testy. | Použij skill wj-datatable k vytvoření evidence [pole, pravidla a oprávnění]. |
| [`wj-dependencycheck`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-dependencycheck/SKILL.md) | Vyhodnocuje nálezy OWASP Dependency Check podle závažnosti a použitelnosti ve WebJET CMS. Před změnou závislostí nebo výjimek předloží návrh na rozhodnutí. | Použij skill wj-dependencycheck pro report [cesta], zpracuj závažnosti HIGH a CRITICAL. |
| [`wj-docs-pr`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-docs-pr/SKILL.md) | Aktualizuje vybraný Markdown dokument podle všech relevantních změn v PR nebo podle lokálních změn. | Použij skill wj-docs-pr k aktualizaci [cesta k dokumentu] podle PR [URL]. |
| [`wj-gh-issue-from-chat`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-gh-issue-from-chat/SKILL.md) | Vytvoří návrh nového GitHub issue z diskuse, rozhodnutí a plánu, po potvrzení jej uloží a navrhne název větve. Větev nevytváří. | Použij skill wj-gh-issue-from-chat a připrav issue z odsouhlaseného zadání v tomto chatu. |
| [`wj-gh-issue-from-grill`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-gh-issue-from-grill/SKILL.md) | Přenáší odsouhlasené závěry diskuse do stávajícího issue včetně kritérií, plánu a testů. Nevytváří nové issue ani nezačíná implementaci. | Použij skill wj-gh-issue-from-grill pro issue [URL] a zapracuj odsouhlasené závěry tohoto chatu. |
| [`wj-page-builder`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-page-builder/SKILL.md) | Vytváří a upravuje HTML bloky PageBuildera včetně vnoření, editovatelných oblastí, sloupců a duplikovatelných prvků. | Použijte skill wj-page-builder k vytvoření bloku referencí se třemi sloupci a duplikovatelnými položkami. |
| [`wj-project-context`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-project-context/SKILL.md) | Načte architekturu a konvence WebJET CMS a ověří jejich použitelnost pro verzi konkrétního core nebo zákaznického projektu. | Použij skill wj-project-context a over, jak v tomto projektu správně číst konfiguraci a překlady. |
| [`wj-sales-features`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-sales-features/SKILL.md) | Připravuje český obchodní popis funkcí z PR, větve nebo příloh: přínosy, výhody a hodnotu pro zákazníka. | Použij skill wj-sales-features pro PR [URL] a popiš přínosy pro zákazníka. |
| [`wj-setup`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-setup/SKILL.md) | Po instalaci propojí WebJET skills s projektovými pokyny pro Codex, GitHub Copilot a Claude Code. Zachová vlastní pravidla projektu. | Použij skill wj-setup k propojení nainstalovaných WebJET skills s tímto projektem. |
| [`wj-simplify-implementation`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-simplify-implementation/SKILL.md) | Zjednodušuje implementaci v PR nebo lokálních změnách a odstraňuje zbytečnou složitost při zachování chování. | Použij skill wj-simplify-implementation k nekommitovaným změnám této funkce. |
| [`wj-stylescombo`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-stylescombo/SKILL.md) | Doplňuje nebo obnovuje metadata stylů v editor.css nebo editor.scss: názvy, kategorie, skupiny a omezení podle úplných stylů šablony. | Použij skill wj-stylescombo: doplň chybějící metadata v [editor.scss], úplné styly jsou v [ninja.scss], jazyk čeština. |
| [`wj-translate-comments`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-translate-comments/SKILL.md) | Překládá české komentáře v kódu do angličtiny beze změny logiky nebo textů aplikace. | Použij skill wj-translate-comments pouze pro komentáře v [soubor nebo složku]. |
| [`wj-ui-review`](https://github.com/webjetcms/webjetcms/blob/main/.agents/skills/wj-ui-review/SKILL.md) | Kontroluje stávající rozhraní, jeho rozložení, typografii, barvy a interakce vůči zvyklostem WebJET CMS. Na požádání provede konkrétní vizuální opravy. | Použij skill wj-ui-review na obrazovku [URL nebo screenshot] a vypiš nálezy bez úprav. |

<!-- webjet-skills-catalog:end -->

### Jak se přehled publikuje

README repozitáře [webjetcms/skills](https://github.com/webjetcms/skills) přebírá tabulku mezi značkami `webjet-skills-catalog:start` a `webjet-skills-catalog:end` z anglické verze `docs/en/developer/guidelines/ai-skills.md`. Po úpravě českého přehledu spusťte překlad a zahrňte aktualizovanou anglickou verzi do změn. Při přidání, přejmenování nebo odstranění skillu upravte i jeho řádek v tabulce. Export ověřuje úplnost seznamu; popis a příklad je třeba aktualizovat i při změně účelu skillu.

Workflow [WebJET skills](https://github.com/webjetcms/webjetcms/blob/main/.github/workflows/skills.yml) při pull requestu ověří export a po pushnutí relevantních změn do `main` publikuje distribuci. Sleduje skilly, sdílená pravidla, exportní skripty, distribuční šablonu, anglickou verzi tohoto dokumentu a licenci. Dá se spustit i ručně přes `workflow_dispatch` na větvi `main`.

Základ README se udržuje v [.github/skills-distribution/README.md](https://github.com/webjetcms/webjetcms/blob/main/.github/skills-distribution/README.md). Skript [export-skills.mjs](https://github.com/webjetcms/webjetcms/blob/main/.github/scripts/export-skills.mjs) do něj při exportu vloží anglickou tabulku a upraví odkazy na distribuované soubory. Skilly uvedené v [excluded-skills.txt](https://github.com/webjetcms/webjetcms/blob/main/.github/skills-distribution/excluded-skills.txt) vynechá z distribuce iz její tabulky.

Publikování vyžaduje repository secret `WEBJET_SKILLS_TOKEN` s oprávněním `contents:write` pro `webjetcms/skills`. Pokud se exportovaný obsah nezměnil, nový commit nevznikne. README v distribučním repozitáři neupravujte ručně, další export by změny přepsal.
