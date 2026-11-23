# Zaokrouhlování cen

Zaokrouhlování zajistí, aby se cena v košíku počítala ze zobrazené ceny za kus s DPH. Například cena **1,594 €** se zaokrouhlí na **1,59 €** a zákazník za tři kusy zaplatí **4,77 €**.

## Zapnutí

V konfiguraci nastavte:

- **`basketRoundPrices=true`** – zapne zaokrouhlování. Výchozí hodnota `false` ponechá původní výpočet.
- **`currencyFormat=0.00`** – nastaví cenu za kus na dvě desetinná místa. Pro levné produkty můžete použít více míst.

Funkce podporuje EUR a CZK. Obchod i objednávka používají nastavenou systémovou měnu.

## Jak se počítá cena

1. Cena za kus s DPH se po slevách a případném přepočtu měny zaokrouhlí podle `currencyFormat`.
2. Zaokrouhlená cena se vynásobí počtem kusů. Výsledná částka za produkt se zaokrouhlí na dvě desetinná místa.
3. Košík sčítá částky za produkty, dopravu a platbu. Poplatky se zaokrouhlují stejným způsobem.

Používá se běžné zaokrouhlování: například **1,594 → 1,59** a **1,595 → 1,60**. DPH se počítá ze součtu cen s DPH pro každou sazbu zvlášť a rozdělí se mezi položky tak, aby součty seděly.

### Počet desetinných míst

Příklad pro původní cenu **1,594 s DPH** a **3 kusy**:

| `currencyFormat` | Zobrazená cena za kus | Částka za 3 kusy k úhradě |
| --- | ---: | ---: |
| `0` | 2,00 | 6,00 |
| `0.0` | 1,60 | 4,80 |
| `0.00` | 1,59 | 4,77 |
| `0.0000` | 1,5940 | 4,78 |
| `0.00##` | 1,594 | 4,78 |

`0` znamená povinnou číslici, `#` volitelnou. Vzor `0.00##` proto zachová až čtyři desetinná místa, ale nezobrazuje zbytečné koncové nuly. Ceny se vždy zobrazují alespoň na dvě místa. U `0.0000` se také částka na úhradu 4,78 zobrazí jako 4,7800.

### Velmi levné produkty

U rezistoru za **0,0001 €** použijte například `currencyFormat=0.0000`. Potom **10 000 kusů stojí 1,00 €**. U `0.00` by se již cena za kus zaokrouhlila na nulu. I při vyšší přesnosti se výsledná částka za produkt zaokrouhlí na centy; částka menší než 0,005 € bude nulová.

Pro velká množství upravte také limit `basketMaxQty`, který je ve výchozím nastavení 1000 kusů.

## Euro a česká koruna

U **karty nebo bankovního převodu** funguje výpočet stejně v EUR i CZK: cena za kus se řídí `currencyFormat` a částka k úhradě má přesnost na dvě desetinná místa.

Rozdíl je při výběru **hotovosti**:

- **Slovensko:** výsledná hotovostní částka se zaokrouhluje na nejbližších 0,05 €. Například 4,77 € → 4,75 €. Pokud je celá platba jen 0,01 € nebo 0,02 €, zaokrouhlí se na 0,05 €.
- **Česko:** výsledná hotovostní částka se zaokrouhluje na celé koruny. Například 4,77 Kč → 5 Kč.

**Hotovostní zaokrouhlení tato funkce neprovádí.** Řeší jej pokladna nebo kurýr podle skutečného způsobu platby. Dobírka nemusí znamenat hotovost – kurýrovi lze zaplatit i kartou.

## Před nasazením

Šablony používající značku `iway:curr` převezmou nové formátování automaticky. Vlastní výpočty cen je třeba ověřit zvlášť. Po zapnutí vymažte cache seznamů produktů a vyzkoušejte nákup. Při odeslání objednávky se ceny produktů obnoví dle aktuálních cen v obchodě.

Nastavení přesnosti zvolte při spuštění obchodu a později jej měňte opatrně. Změna nastavení **nemění uloženou celkovou cenu starších objednávek**, může však změnit zobrazení jejich položek a rozpis DPH. Při úpravě položek se objednávka přepočítá podle aktuálních nastavení.
