# Zaokrúhľovanie cien

Zaokrúhľovanie zabezpečí, aby sa cena v košíku počítala zo zobrazenej ceny za kus s DPH. Napríklad cena **1,594 €** sa zaokrúhli na **1,59 €** a zákazník za tri kusy zaplatí **4,77 €**.

## Zapnutie

V konfigurácii nastavte:

- **`basketRoundPrices=true`** – zapne zaokrúhľovanie. Predvolená hodnota `false` ponechá pôvodný výpočet.
- **`currencyFormat=0.00`** – nastaví cenu za kus na dve desatinné miesta. Pre lacné produkty môžete použiť viac miest.

Funkcia podporuje EUR a CZK. Obchod aj objednávka používajú nastavenú systémovú menu.

## Ako sa počíta cena

1. Cena za kus s DPH sa po zľavách a prípadnom prepočte meny zaokrúhli podľa `currencyFormat`.
2. Zaokrúhlená cena sa vynásobí počtom kusov. Výsledná suma za produkt sa zaokrúhli na dve desatinné miesta.
3. Košík sčíta sumy za produkty, dopravu a platbu. Poplatky sa zaokrúhľujú rovnakým spôsobom.

Používa sa bežné zaokrúhľovanie: napríklad **1,594 → 1,59** a **1,595 → 1,60**. DPH sa počíta zo súčtu cien s DPH pre každú sadzbu osobitne a rozdelí sa medzi položky tak, aby súčty sedeli.

### Počet desatinných miest

Príklad pre pôvodnú cenu **1,594 s DPH** a **3 kusy**:

| `currencyFormat` | Zobrazená cena za kus | Suma za 3 kusy na úhradu |
| --- | ---: | ---: |
| `0` | 2,00 | 6,00 |
| `0.0` | 1,60 | 4,80 |
| `0.00` | 1,59 | 4,77 |
| `0.0000` | 1,5940 | 4,78 |
| `0.00##` | 1,594 | 4,78 |

`0` znamená povinnú číslicu, `#` voliteľnú. Vzor `0.00##` preto zachová až štyri desatinné miesta, ale nezobrazuje zbytočné koncové nuly. Ceny sa vždy zobrazujú aspoň na dve miesta. Pri `0.0000` sa aj suma na úhradu 4,78 zobrazí ako 4,7800.

### Veľmi lacné produkty

Pri rezistore za **0,0001 €** použite napríklad `currencyFormat=0.0000`. Potom **10 000 kusov stojí 1,00 €**. Pri `0.00` by sa už cena za kus zaokrúhlila na nulu. Aj pri vyššej presnosti sa výsledná suma za produkt zaokrúhli na centy; suma menšia ako 0,005 € bude nulová.

Pre veľké množstvá upravte aj limit `basketMaxQty`, ktorý je predvolene 1000 kusov.

## Euro a česká koruna

Pri **karte alebo bankovom prevode** funguje výpočet rovnako v EUR aj CZK: cena za kus sa riadi `currencyFormat` a suma na úhradu má presnosť na dve desatinné miesta.

Rozdiel je pri výbere **hotovosti**:

- **Slovensko:** výsledná hotovostná suma sa zaokrúhľuje na najbližších 0,05 €. Napríklad 4,77 € → 4,75 €. Ak je celá platba len 0,01 € alebo 0,02 €, zaokrúhli sa na 0,05 €.
- **Česko:** výsledná hotovostná suma sa zaokrúhľuje na celé koruny. Napríklad 4,77 Kč → 5 Kč.

**Hotovostné zaokrúhlenie táto funkcia nevykonáva.** Rieši ho pokladnica alebo kuriér podľa skutočného spôsobu platby. Dobierka nemusí znamenať hotovosť – kuriérovi možno zaplatiť aj kartou.

## Pred nasadením

Šablóny používajúce značku `iway:curr` prevezmú nové formátovanie automaticky. Vlastné výpočty cien treba overiť osobitne. Po zapnutí vymažte cache zoznamov produktov a vyskúšajte nákup. Pri odoslaní objednávky sa ceny produktov obnovia podľa aktuálnych cien v obchode.

Nastavenie presnosti zvoľte pri spustení obchodu a neskôr ho meňte opatrne. Zmena nastavenia **nemení uloženú celkovú cenu starších objednávok**, môže však zmeniť zobrazenie ich položiek a rozpis DPH. Pri úprave položiek sa objednávka prepočíta podľa aktuálnych nastavení.
