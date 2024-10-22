# Nastavení

## Vytvoření nové skupiny šablon

Ve výchozím nastavení je vytvořena nová skupina pro **výchozí jazyk** na projektu (ve většině případů je to **Slovenská**, ale je nastavena v konstantě `defaultLanguage`).

To znamená, že všechny hodnoty zadané do polí při vytváření skupiny (kromě názvu skupiny a složky) se ve výchozím nastavení vytvoří v textových klíčích pro výchozí jazyk.

## Odstranění skupiny

Skupinu šablon lze **smazat** pro všechny případy, **Pokud** danou skupinu šablon **nepoužívá se** (žádná šablona ji nemá nastavenou).

Při odstraňování skupiny se **fyzicky neodstraní šablonu** v adresáři /template/, **ale všechny informace o skupině šablon uložené v databázi jsou odstraněny.** + všechny definované textové klíče pro skupinu.

## Jazyk pro úpravu textu

Tabulka se seznamem skupin šablon **zobrazuje texty ve výchozím nastavení pro nastavený výchozí jazyk.** na projektu (ve většině případů je to slovenština, ale je nastavena v konstantní `defaultLanguage`).

Pokud je zvolen jiný jazyk, do tabulky se načtou nové texty pro zvolený jazyk.

- Pokud není v tomto jazyce definován žádný text, zobrazí se prázdná buňka (bez textu).
- To, že se zobrazí prázdná buňka, však neznamená, že front-end zobrazí prázdnou hodnotu.
  - **Zobrazení textu ve front-endu funguje následovně:**
    - vyhledá text definovaný v položce **aktuálně zobrazený jazyk** (například v EN)
    - pokud text neexistuje, hledá v jazyce nastaveném v konstantě `defaultLanguage` (například CZ)
    - pokud text neexistuje, hledání v databázi **CS** jazyk

## Úprava skupiny

Každou skupinu šablon lze přizpůsobit.

Při úpravách se otevře modální okno, které obsahuje pole s výchozími hodnotami nastavenými pro aktuálně zvolený jazyk.

**Příklad úpravy:**

- vybraný jazyk pro úpravu textu je angličtina
- při úpravě skupiny se v polích nastaví výchozí hodnoty pro angličtinu.
- Při úpravě šablony není možné ji měnit:
  - jazyk (přebírá se z pole pro výběr jazyka pro úpravu textu).
  - pole Název a Složka jsou nastavena globálně pro všechny jazyky, není možné definovat pole pro každý jazyk zvlášť.

## Použití skupiny

V úpravě virtuální šablony je pole **Skupina šablon** ve kterém se podle potřeby nastaví jedna skupina ze seznamu všech skupin šablon.

Po výběru skupiny se v poli pro výběr **Šablona HTML** zobrazit vše **dostupné šablony HTML pro vybranou skupinu** (vše **Soubory JSP** ve fyzickém adresáři vybrané skupiny, kromě adresáře **/includes/**).

## Použití předpon textových klíčů

*Příklad použití předpony:*

```properties
testujemPrefix.editor.field_a=GPS súradnice
```

Pro všechny šablony ve skupině s nastavenou předponou **I testPrefix** je nastaven **název nepovinného pole A** na stránce "**Souřadnice GPS**".

## Migrace skupiny

Pokud migrujete skupinu do jiného prostředí, musíte provést následující kroky:
- přenést fyzický adresář skupiny do `/templates/` z prostředí A do prostředí B
  - pokud soubory JSP migrované šablony obsahují přímo propojené moduly a další soubory JSP, které nejsou v prostředí B, migrujte i je.
- zkontrolovat nastavené proměnné `$wj-install-name` a `$wj-template-name` v souboru `_variables.scss`
- Vytvoření a nastavení skupiny v prostředí B na základě vzoru z prostředí A
- Export textových klíčů z prostředí A s předponou nastavenou v poli **Předpona textových klíčů** a import do prostředí B
