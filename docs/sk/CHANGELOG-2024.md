# Zoznam zmien verzia 2024

## 2024.0-SNAPSHOT

> Upozornenie: na spustenie verzie 2024 je potrebné mať na serveri inštalovanú Java verzie 17.

### Prechod na Java 17

WebJET CMS verzie 2024 prešiel na Java verzie 17. Obsahuje nasledovné zmeny:

- Aktualizované viaceré knižnice, napr. `AspectJ 1.9.19, lombok 1.18.28`.
- Aktualizovaná knižnica Eclipselink na štandardnú verziu, použitie WebJET CMS `PkeyGenerator` nastavené pomocou triedy `JpaSessionCustomizer` a `WJGenSequence`.
- Aktualizovaný `gradle` na verziu 8.1.
- Odstránená stará knižnica ```ch.lambdaj```, použite štandardné Java Lambda výrazy (#54425).

### Bezpečnosť

- 404 - pridaná možnosť vypnúť ochranu volania 404 stránky (počet požiadaviek) podobne ako iné spam ochrany nastavením IP adresy do konf. premennej `spamProtectionDisabledIPs`. Pre danú IP adresu sa vypnú aj ďalšie SPAM ochrany (pre  opakované volania).

### Systémové zmeny

- WebJET CMS je dostupný priamo v [repozitári maven central](https://repo1.maven.org/maven2/com/webjetcms/webjetcms/), GitHub projekty [basecms](https://github.com/webjetcms/basecms) a [democms](https://github.com/webjetcms/democms) upravené na použitie priamo tohto repozitára. Zostavenie je mierne odlišné od pôvodného zostavenia, knižnice `wj*.jar` sú spojené do `webjet-VERZIA-libs.jar`. Použitá knižnica [pd4ml](https://pd4ml.com/support-topics/maven/) je vo verzii 4, pre generovanie PDF súborov vyžaduje zadanie licencie do súboru `pd4ml.lic` v [pracovnom priečinku](https://pd4ml.com/support-topics/pd4ml-v4-programmers-manual/) servera alebo priečinku kde sa nachádza `pd4ml.jar`. Neskôr bude doplnená možnosť zadať licenčné číslo cez konfiguračnú premennú (#43144).
- Zrušená podpora plno textového indexovania `rar` archívov (#43144).

## 2024.0

> Verzia 2024.0 obsahuje novú verziu **aktualizácie s opisom zmien**, **klonovanie štruktúry** integrované s funkciou zrkadlenia (vrátane možnosti prekladov), pridáva možnosť **obnoviť** web stránku, alebo **celý priečinok z koša**, pridáva **editor typu HTML** a možnosť nastavenia typu editora priamo pre šablónu, **aplikáciam** je možné **zapnúť zobrazenie len pre zvolené typy zariadení** mobil, tablet, PC a samozrejme zlepšuje bezpečnosť a komfort práce.

Zoznam zmien je zhodný s verziou [2023.53-java17](CHANGELOG-2023.md).