# Nastavenie

## Vytvorenie novej skupiny šablón

Nová skupina sa vytvára predvolene pre **východzí jazyk** na projekte (vo väčšine prípadov to je **slovenčina**, ale nastavuje sa to v konštante `defaultLanguage`).

Čiže všetky zadané hodnoty (okrem Názov skupiny a Priečinok) v poliach pri vytváraní skupiny sa predvolene vytvárajú v textových kľúčov pre východzí jazyk.

## Zmazanie skupiny

Skupinu šablón je možné **zmazať** len v tom prípade, **ak** daná skupina šablón **nie je použitá** (nemá ju žiadna šablóna nastavenú).

Pri zmazaní skupiny sa **nezmaže fyzicky šablóna** v adresári /template/, **ale zmažú sa všetky informácie o skupine šablón uložené v databáze** + všetky zadefinované textové kľúče pre danú skupinu.

## Jazyk pre úpravu textov

Tabuľka so zoznamom skupín šablón **zobrazuje texty predvolene pre nastavený východzí jazyk** na projekte (vo väčšine prípadov to je slovenčina, ale nastavuje sa to v konštante `defaultLanguage`).

V prípade zvolenia iného jazyku sa tabuľka načíta s novými textami pre vybraný jazyk.
- Ak v tomto jazyku text nie je zadefinovaný, zobrazí sa prázdna bunka (bez textu).
- To že sa zobrazuje prázdna bunka však neznamená, že sa na front-end zobrazí prázdna hodnota.
    - **Zobrazenie textu na front-end funguje nasledovne:**
        - hľadá text zadefinovaný v **aktuálne zobrazenom jazyku** (pre príklad napr. v EN)
        - ak text neexistuje, hľadá v jazyku nastavenom v konštante `defaultLanguage` (pre príklad napr. CZ)
        - ak text neexistuje, hľadá na tvrdo v **SK** jazyku

## Úprava skupiny

Každú skupinu šablón je možné upraviť.

Pri úprave sa otvorí modálne okno, v ktorom sa nachádzajú polia s predvolenými hodnotami, ktoré sú nastavené pre aktuálne zvolený jazyk.

**Príklad úpravy:**

- zvolený Jazyk pre úpravu textov je Anglicky
- pri úprave skupiny sa polia predvolia nastavenými hodnotami pre anglický jazyk
- Pri úprave šablóny nie je možné zmeniť:
    - jazyk (ten sa preberá z výberového poľa Jazyk pre úpravu textov)
    - polia Názov a Priečinok sa nastavujú globálne pre všetky jazyky, nedá sa pole definovať individuálne pre každý jazyk

## Použitie skupiny

V úprave virtuálnej šablóny je pole **Skupina šablón** v ktorej sa z listu všetkých skupín šablón nastavuje jedna skupina podľa potreby.

Po vybratí skupiny sa vo výberovom poli **HTML Šablóna** zobrazia všetky **dostupné HTML šablóny pre zvolenú skupinu** (zobrazia sa všetky **JSP súbory** vo fyzickom adresári zvolenej skupiny okrem adresáru **/includes/**).

## Použitie prefixu textových kľúčov

*Príklad použitia prefixu:*
```properties
testujemPrefix.editor.field_a=GPS súradnice
```

Pre všetky šablóny v skupine s nastaveným prefixom **testujemPrefix** sa nastaví **názov voliteľného poľa A** v stránke na "**GPS súradnice**".

## Migrácia skupiny

V prípade migrácie skupiny na iné prostredie, je potrebné spraviť tieto kroky:

- preniesť fyzický adresár skupiny v `/templates/` z prostredia A na prostredie B
    - ak sú v JSP súboroch migrovanej šablóny priamo linkované moduly a iné JSP súbory, ktoré na prostredí B nie sú, preniesť aj tie
- skontrolovať nastavené premenné `$wj-install-name` a `$wj-template-name` v súbore `_variables.scss`
- Vytvorenie a nastavenie skupiny na prostredí B podľa vzoru z prostredia A
- Exportovanie textových kľúčov z prostredia A s prefixom nastaveným v poli **Prefix textových kľúčov** a importovanie na prostredie B