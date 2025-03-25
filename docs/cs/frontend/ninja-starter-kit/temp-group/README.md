# Nastavení

## Vytvoření nové skupiny šablon

Nová skupina se vytváří ve výchozím nastavení pro **výchozí jazyk** na projektu (ve většině případů to je **čeština**, ale nastavuje se to v konstantě `defaultLanguage`).

Čili všechny zadané hodnoty (kromě Název skupiny a Složka) v polích při vytváření skupiny se ve výchozím nastavení vytvářejí v textových klíčů pro výchozí jazyk.

## Smazání skupiny

Skupinu šablon je možné **smazat** jen v tom případě, **pokud** daná skupina šablon **není použita** (nemá ji žádná šablona nastavenou).

Při smazání skupiny se **nesmaže fyzicky šablona** v adresáři /template/, **ale smažou se všechny informace o skupině šablon uložené v databázi** + všechny zadefinované textové klíče pro danou skupinu.

## Jazyk pro úpravu textů

Tabulka se seznamem skupin šablon **zobrazuje texty ve výchozím nastavení pro výchozí výchozí jazyk** na projektu (ve většině případů to je čeština, ale nastavuje se to v konstantě `defaultLanguage`).

V případě zvolení jiného jazyka se tabulka načte s novými texty pro vybraný jazyk.

- Pokud v tomto jazyce není text definován, zobrazí se prázdná buňka (bez textu).
- To, že se zobrazuje prázdná buňka však neznamená, že se na front-end zobrazí prázdná hodnota.
  - **Zobrazení textu na front-end funguje následovně:**
    - hledá text definovaný v **aktuálně zobrazeném jazyce** (pro příklad např. v EN)
    - pokud text neexistuje, hledá v jazyce nastaveném v konstantě `defaultLanguage` (pro příklad např. CZ)
    - pokud text neexistuje, hledá na tvrdo v **CS** jazyce

## Úprava skupiny

Každou skupinu šablon lze upravit.

Při úpravě se otevře modální okno, ve kterém se nacházejí pole s výchozími hodnotami, které jsou nastaveny pro aktuálně zvolený jazyk.

**Příklad úpravy:**

- zvolený Jazyk pro úpravu textů je Anglicky
- při úpravě skupiny se pole předvolí nastavenými hodnotami pro anglický jazyk
- Při úpravě šablony nelze změnit:
  - jazyk (ten se přebírá z výběrového pole Jazyk pro úpravu textů)
  - pole Název a Složka se nastavují globálně pro všechny jazyky, nelze pole definovat individuálně pro každý jazyk

## Použití skupiny

V úpravě virtuální šablony je pole **Skupina šablon** ve které se z listu všech skupin šablon nastavuje jedna skupina podle potřeby.

Po vybrání skupiny se ve výběrovém poli **HTML Šablona** zobrazí všechny **dostupné HTML šablony pro zvolenou skupinu** (zobrazí se všechny **JSP soubory** ve fyzickém adresáři zvolené skupiny kromě adresáře **/includes/**).

## Použití prefixu textových klíčů

*Příklad použití prefixu:*

```properties
testujemPrefix.editor.field_a=GPS súradnice
```

Pro všechny šablony ve skupině s nastaveným prefixem **testujiPrefix** se nastaví **název volitelného pole A** ve stránce na "**GPS souřadnice**".

## Migrace skupiny

V případě migrace skupiny na jiné prostředí, je třeba udělat tyto kroky:
- přenést fyzický adresář skupiny v `/templates/` z prostředí A na prostředí B
  - jsou-li v JSP souborech migrované šablony přímo linkovány moduly a jiné JSP soubory, které na prostředí B nejsou, přenést i ty
- zkontrolovat nastavené proměnné `$wj-install-name` a `$wj-template-name` v souboru `_variables.scss`
- Vytvoření a nastavení skupiny na prostředí B podle vzoru z prostředí A
- Exportování textových klíčů z prostředí A s prefixem nastaveným v poli **Prefix textových klíčů** a importování na prostředí B
