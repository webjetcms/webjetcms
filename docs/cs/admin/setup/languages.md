# Jazyky

- Konf. proměnná `languages` určuje, které jazyky jsou k dispozici při výběru jazyka v cms, např. pro nastavení jazyka složky nebo šablony. Neplatí pro aplikaci galerie, která zatím nemá dynamický jazykový model.
- Zadávají se dvoumístné kódy oddělené čárkami, výchozí hodnota je `sk,cz,en,de,pl,hu,cho,ru,esp`.
- Pokud se rozhodnete přidat nějaký jazyk, např. `it`, kromě přidání do proměnné `languages` je třeba doplnit:
  - Překladový klíč `language.it` s hodnotou `Taliansky` v aplikaci Překladové klíče.
  - Soubor s překladovými klíči `text_it.properties` nebo pro projekt `text_it-INSTALL_NAME.properties` kde `INSTALL_NAME` je název instalace.
