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
4. V tom istom chate vytvorte GitHub issue pomocou projektového skillu:

   ```text
   Použi skill wj-gh-issue-from-chat. Najprv over, či už v
   webjetcms/webjetcms neexistuje rovnaké issue. Ak nie, priprav nové
   z tejto diskusie vrátane akceptačných kritérií, dohodnutého plánu a testov.
   ```

5. Skontrolujte návrh a potvrďte vytvorenie issue. Skill použije anglický názov, telo ponechá v jazyku diskusie a navrhne názov vetvy. Vetvu nevytvára. Pokračujte časťou [Implementácia a kontrola](#implementácia-a-kontrola).

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

3. Potvrďte vyjasnené zadanie alebo navrhnutú opravu. Ak pribudli podstatné rozhodnutia, zapíšte ich do existujúceho issue, aby boli dostupné aj mimo chatu.

## Implementácia a kontrola

1. Vytvorte alebo vyberte pracovnú vetvu podľa [pravidiel projektu](gitlab.md). Pre novú funkčnosť použite `feature/<číslo-issue>-<popis>`, pre opravu `hotfix/<číslo-issue>-<popis>`; vychádzajte zo správnej cieľovej vetvy.
2. V chate s dohodnutým plánom zadajte:

   ```text
   Použi skill implement pre issue [URL] podľa dohodnutého plánu.
   Dodrž AGENTS.md a príslušné wj-* skilly. Spusti relevantné testy
   a doplň dokumentáciu. Nevytváraj commit ani push; zmeny nechaj na kontrolu.
   ```

3. Pred commitom si vyžiadajte projektovú kontrolu:

   ```text
   Použi skill wj-code-review na necommitnuté zmeny k issue [URL].
   Over splnenie akceptačných kritérií aj spätnú kompatibilitu.
   ```

4. Skontrolujte diff, výsledky testov a správanie aplikácie podľa [Definície hotového](definition-of-done.md). Commit a ďalší Git postup vykonáva vývojár; zákaz commitov agentom platí aj pri použití všeobecného skillu `implement`.
