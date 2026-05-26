# BackEnd pro práci s grafy

Práce s grafy je výhradně práce FrontEndu a BackEnd zajišťuje pouze data pro tyto grafy. Jelikož každý graf filtruje svá data pomocí externího filtru, byly přidány různé třídy, které usnadňují práci s parametry z těchto externích filtrů. Získání a přípravu dat pro grafy zabezpečují jednotlivé `RestController` soubory, které obsluhují i ​​danou stránku na které jsou tyto grafy.

## Typ grafu

`ChartType` je třída typu `Enum`, která slouží k rychlé a bezpečné identifikaci typu grafu.

Jelikož různé grafy mívají stejný zdroj dat, který je třeba pouze specificky upravit danému typu, je taková rychlá identifikace vhodná.

Třída `ChartType` obsahuje tyto hodnoty:

- `NOT_CHART`, tato hodnota bývá primárně přednastavena. Přidání tohoto typu bylo zapotřebí jelikož v některých případech datatabulka i graf sdíleli stejný zdroj dat a touto hodnotou se dá říci že nejde o graf ale například o datatabulku.
- `LINE`, hodnota reprezentuje graf **čárového** typu
- `BAR_VERTICAL`, hodnota reprezentuje graf **sloupcového** typu, s vertikálními sloupci
- `BAR_HORIZONTAL`, hodnota reprezentuje graf **sloupcového** typu, s horizontálními sloupci
- `PIE_CLASSIC`, hodnota reprezentuje graf **koláčového** typu
- `PIE_DONUT`, hodnota reprezentuje variaci grafu **koláčového** typu, který má ve středu prázdný kruh (není vyplněn)
- `DOUBLE_PIE`, hodnota reprezentuje graf **dvojitého koláčového** typu (vnější a vnitřní graf)
- `WORD_CLOUD`, hodnota reprezentuje graf **oblak slov**, který zobrazuje slova, kde velikost slova reprezentuje jeho frekvenci výskytu (nebo jinou hodnotu) v daném datasetu
- `TABLE`, hodnota reprezentuje jednoduchou **tabulku** s daty (bez pokročilých funkcionalit jako je stránkování, filtrování, atd.)

## Data pro grafy

Formát vrácených dat z BackEnd-u pro grafy závisí na typu grafu, kterému budou tato data přidělena:

- `LINE`, vrácený typ dat je mapa obsahující listy objektů, `Map<String, List<T>>`, kde klíč mapy představuje název `datasetu`, objekt v listu objektů (hodnota prvku mapy) musí obsahovat číselnou proměnnou (osa Y, hodnota) a datumovou proměnnou (osa X, kdy byla hodnota získána)
- `BAR_VERTICAL` / `BAR_HORIZONTAL`, vrácený typ dat je jednoduchý list objektů, `List<T>`, kde objekt musí obsahovat textovou proměnnou (osa Y, kategorie) a číselnou proměnnou (osa X, hodnota k dané kategorii)
- `PIE_CLASSIC` / `PIE_DONUT`, vrácený typ dat je jednoduchý list objektů, `List<T>`, kde objekt musí obsahovat textovou proměnnou (kategorie) a číselnou proměnnou (hodnota k dané kategorii)
- `DOUBLE_PIE`, vrácený typ dat je jednoduchý list objektů, `List<T>`, kde objekt musí obsahovat textovou proměnnou (kategorie) a dvě číselné proměnné (každá hodnota pro jednu kategorii)
- `WORD_CLOUD`, v případě tohoto grafu se data vracejí ve 2 různých formátech:
  - jednoduchý list objektů, `List<T>`, kde objekt obsahuje textovou proměnnou (slovo / frázi) a číselnou proměnnou (frekvence výskytu slova)
  - může být jednoduchý textový řetězec, `String`, sám graf si vyhledá jednotlivá slova a jejich frekvenci výskytu v tomto řetězci
- `TABLE`, vrácený typ dat je jednoduchý list objektů, `List<T>`, kde objekt obsahuje různé textové či číselné hodnoty, které chcete zobrazit

Bližší informace o procesu získání/zpracování dat pro grafy jsou specificky okomentovány v jednotlivých `RestControlleroch`, které tato data zabezpečují.

### Sekce statistiky

Hlavním uživatelem grafů jsou aplikace v sekci Statistiky. Společnou vlastností pro tyto aplikace je, že data pro grafy získáváme specificky pomocí tříd `StatTableDB` nebo `StatNewDB` a vrácená data jsou `listy` s typem `Column`. Protože proměnná `Column` slouží dynamicky pro různé kombinace dat, každá třída musí získat potřebné hodnoty z této proměnné specificky. Například `BrowserRestController` získává hodnotu `visit` jako `Column.getIntColumn3()` ale `CountryRestController` získává stejnou hodnotu jako `Column.getIntColumn2()`.

## Filtrování

Třída `FilterHeaderDto` představuje nejpoužívanější hodnoty externích filtrů, které se používají k filtrování hodnot pro grafy. Protože téměř každá stránka využívající grafy má externí filtr, nejpoužívanější parametry těchto filtrů se shrnuly do této třídy, aby se zvýšila přehlednost a zabránilo se duplicitě kódů.

Kromě konstruktoru, který nastaví základní hodnoty, třída obsahuje také metodu `groupIdToQuery`. Jejím úkolem je vytvořit a nastavit `query` potřebnou ke zpětné kompatibilitě ze zadaného `rootGroupId`. Vytvořená `rootGroupIdQuery` představuje SQL příkaz pro výběr těch záznamů, jejichž `group_id` spadá pod zadané `rootGroupId` jako potomek na kterékoli úrovni (výběr celého pod-stromu web stránek).

## `StatService`

Tato třída byla primárně vytvořena pro snížení duplicitního kódu a zjednodušení zpracování hodnot z externích filtrů. Jelikož každý graf na stránce využívá nějaký externí filtr, byly do této třídy přidány různé metody, které usnadňují zpracování hodnot z externích filtrů nebo práci s nimi. Bližší popis jednotlivých funkcí/metod, jejich vstupy a možné výstupy je přímo v `StatService` třídě ve formě komentářů.

## Barevné schéma grafů

Grafy podporují rozdílná barevná schémata pro úpravu vzhledu grafu. Pro výběr barevného schématu doporučujeme použít pole typu `IMAGE_RADIO`, které musí mít třídu `image-radio-chart-colorset` pro správné nastavení (viz příklad níže)

```java
@Column(name = "color_scheme")
@DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "&nbsp;", tab = "stat", className = "image-radio-horizontal image-radio-chart-colorset")
private String colorScheme;
```

možnosti pro toto pole nastavíte jako

```java
page.addOptions("colorScheme", StatService.getColorSchemeOptions(), "label", "value", true);
```

důležité je právě volání statické metody `getColorSchemeOptions` z pomocné třídy `StatService`. Tato metoda vrátí seznam dostupných barevných schémat is cestou k obrázku, který dané barvy ve schématu zobrazuje

```java
public static final String DEFAULT_COLORSET_NAME = "set3";
public static final List<OptionDto> getColorSchemeOptions() {
    List<OptionDto> optionsMap = new ArrayList<>();
    optionsMap.add(new OptionDto("set1", "set1", "/apps/_common/charts/images/overlapping_circles_set1.png"));
    optionsMap.add(new OptionDto("set2", "set2", "/apps/_common/charts/images/overlapping_circles_set2.png"));
    optionsMap.add(new OptionDto("set3", "set3", "/apps/_common/charts/images/overlapping_circles_set3.png"));
    optionsMap.add(new OptionDto("set4", "set4", "/apps/_common/charts/images/overlapping_circles_set4.png"));
    optionsMap.add(new OptionDto("set5", "set5", "/apps/_common/charts/images/overlapping_circles_set5.png"));
    optionsMap.add(new OptionDto("set_blue", "set_blue", "/apps/_common/charts/images/overlapping_circles_set_blue.png"));
    optionsMap.add(new OptionDto("set_green", "set_green", "/apps/_common/charts/images/overlapping_circles_set_green.png"));
    optionsMap.add(new OptionDto("set_red", "set_red", "/apps/_common/charts/images/overlapping_circles_set_red.png"));
    optionsMap.add(new OptionDto("set_yellow", "set_yellow", "/apps/_common/charts/images/overlapping_circles_set_yellow.png"));
    return optionsMap;
}
```