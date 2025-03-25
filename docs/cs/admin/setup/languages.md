# Jazyky

- Konf. proměnná `languages` určuje jaké jazyky jsou dostupné při výběru jazyka v cms. pro nastavení jazyka složky, nebo šablony. Netýká se aplikace galerie, která zatím nemá dynamický jazykový model.
- Zadávají se dvoumístné kódy oddělené čárkami, výchozí hodnota je `sk,cz,en,de,pl,hu,cho,ru,esp`.
- Pokud se rozhodnete nějaký jazyk přidat, např. `it`, tak kromě jeho přidání do proměnné `languages` je třeba doplnit:
  - Překladový klíč `language.it` s hodnotou `Taliansky` v aplikaci Překladové klíče.
  - Soubor s překladovými klíči `text_it.properties`, nebo pro projekt `text_it-INSTALL_NAME.properties`, kde `INSTALL_NAME` je jméno instalace.
