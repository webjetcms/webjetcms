# Jazyky

- Konf. premenná `languages` určuje aké jazyky sú dostupné pri výbere jazyka v cms, napr. pre nastavenie jazyka priečinka, alebo šablóny. Netýka sa aplikácie galéria, ktorá zatiaľ nemá dynamický jazykový model.
- Zadávajú sa dvojmiestne kódy oddelené čiarkami, východzia hodnota je `sk,cz,en,de,pl,hu,cho,ru,esp`.
- Ak sa rozhodnete nejaký jazyk pridať, napr `it`, tak okrem jeho pridania do premennej `languages` je potrebné doplniť:
  - Prekladový kľúč `language.it` s hodnotou `Taliansky` v aplikácii Prekladové kľúče.
  - Súbor s prekladovými kľúčmi `text_it.properties`, alebo pre projekt `text_it-INSTALL_NAME.properties`, kde `INSTALL_NAME` je meno inštalácie.
