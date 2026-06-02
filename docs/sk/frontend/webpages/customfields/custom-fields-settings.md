# Tabuľka Voliteľné polia

Tabuľka Voliteľné polia umožňuje centrálne nastaviť vlastnosti voliteľných polí pre rôzne entity v systéme. Nastavenia sa nachádzajú v menu `Nastavenia` pod položkou `Voliteľné polia`. Pomocou tejto tabuľky je možné nastaviť parametre povinného poľa bez potreby editácie prekladových kľúčov.

!>**Upozornenie:** Aktuálne funguje nastavenie IBA parametru **Povinné pole**, zvyšok nastavení bude implementovaný v budúcnosti.

![](custom-fields-settings-datatable.png)

## Stĺpce tabuľky

Tabuľka obsahuje nasledovné stĺpce:

| Stĺpec | Popis |
| --- | --- |
| **Použiť pre entitu** | Názov triedy entity (napr. `sk.iway.iwcm.doc.DocDetails`), pre ktorú sa nastavenie aplikuje. Pole podporuje autocomplete - po zadaní aspoň 1 znaku sa zobrazia návrhy dostupných entít, ktoré využívajú voliteľné polia. |
| **Označenie poľa** | Písmeno abecedy (A-Z), ktorým sa identifikuje voliteľné pole. Zodpovedá názvom polí `field_A`, `field_B` atď. |
| **ID entity** | Voliteľné ID konkrétnej entity (napr. ID šablóny). Ak nie je zadané, nastavenie sa aplikuje globálne pre všetky entity danej triedy. |
| **Typ poľa** | Typ voliteľného poľa (napr. `text`, `textarea`, `boolean`, `number` atď.). |
| **Popis poľa** | Popis (label), ktorý sa zobrazí pri voliteľnom poli v editore. |
| **Tooltip poľa** | Text nápovedy, ktorý sa zobrazí po prejdení myšou ponad ikonu <i class="ti ti-info-circle"></i>. |
| **Povinné pole** | Ak je nastavené na `true`, pole bude povinné a pri uložení entity sa skontroluje, či je vyplnené. |

![](custom-fields-settings-editor.png)

## Priorita nastavení

Nastavenia sa aplikujú podľa priority:

1. **Globálne nastavenia** - záznamy bez vyplneného `ID entity` platia pre všetky entity danej triedy.
2. **Špecifické nastavenia** - záznamy s vyplneným `ID entity` majú vyššiu prioritu a prepíšu globálne nastavenia pre daný identifikátor.
3. **Bonusový kontext** - pre niektoré entity (napr. `DocDetails`) sa automaticky aplikuje aj kontext šablóny (`TemplateDetails`) podľa použitého ID šablóny, ktorý má najvyššiu prioritu.

Napr. pre web stránku (`DocDetails`) je možné nastaviť pole A ako povinné globálne (bez ID entity), ale pre stránky so šablónou s konkrétnym ID môže byť táto povinnosť prepísaná.

## Validácia

Kombinácia polí `Použiť pre entitu`, `Označenie poľa` a `ID entity` musí byť jedinečná. Systém nedovolí vytvoriť duplicitný záznam s rovnakou kombináciou týchto hodnôt.

## Povinné polia

Ak je pre voliteľné pole zapnutý príznak `Povinné pole`, systém automaticky:

- Označí pole ako povinné v editore (zobrazí sa vizuálne označenie povinného poľa).
- Pri ukladaní entity skontroluje, či je pole vyplnené. Ak nie je, zobrazí chybovú hlášku a uloženie sa nepovolí.
