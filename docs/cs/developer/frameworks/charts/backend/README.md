# BackEnd pro práci s grafy

Práce s grafy je výhradně práce FrontEndu a BackEnd zajišťuje pouze data pro tyto grafy. Jelikož každý graf filtruje svá data pomocí externího filtru, byly přidány různé třídy, které usnadňují práci s parametry z těchto externích filtrů. Získání a přípravu dat pro grafy zajišťují jednotlivé `RestController` soubory, které obsluhují i danou stránku na které jsou tyto grafy.

## Typ grafu

`ChartType` je třída typu `Enum`, která slouží k rychlé a bezpečné identifikaci typu grafu.

Jelikož různé grafy mívají stejný zdroj dat, který je třeba pouze specificky upravit danému typu, je taková rychlá identifikace vhodná.

Třída `ChartType` obsahuje tyto hodnoty:
- `NOT_CHART`, tato hodnota bývá primárně přednastavena. Přidání tohoto typu bylo zapotřebí jelikož v některých případech datatabulka i graf sdíleli stejný zdroj dat a touto hodnotou se dá říci že nejde o graf ale například o datatabulku.
- `PIE`, hodnota reprezentuje graf **koláčového** typu
- `DOUBLE_PIE`, hodnota reprezentuje graf **dvojitého koláčového** typu (vnější a vnitřní graf)
- `LINE`, hodnota reprezentuje graf **čárového** typu
- `BAR`, hodnota reprezentuje graf **sloupcového** typu

## Data pro grafy

Formát vrácených dat z BackEnd-u pro grafy závisí na typu grafu, kterému budou tato data přidělena :
- `Bar`, vrácený typ dat je jednoduchý list objektů, `java List<T>`, kde objekt musí obsahovat textovou proměnnou(osa Y, kategorie) a číselnou proměnnou (osa X, hodnota k dané kategorii)
- `Pie`, vrácený typ dat je jednoduchý list objektů, `java List<T>`, kde objekt musí obsahovat textovou proměnnou (kategorie) a číselnou proměnnou (hodnota k dané kategorii)
- `DOUBLE_PIE`, vrácený typ dat je jednoduchý list objektů, `java List<T>`, kde objekt musí obsahovat textovou proměnnou(kategorie) a dvě číselné proměnné (každá hodnota pro jednu kategorii)
- `Line`, vrácený typ dat je mapa obsahující listy objektů, `java Map<String, List<T>>`, kde klíč mapy představuje název `datasetu`, objekt v listu objektů (hodnota prvku mapy) musí obsahovat číselnou proměnnou (osa Y, hodnota) a datumovou proměnnou(osa X, kdy byla hodnota získána)

Bližší informace o procesu získání/zpracování dat pro grafy jsou specificky okomentovány v jednotlivých `RestControlleroch`, které tato data zajišťují.

### Sekce statistiky

Hlavním uživatelem grafů jsou aplikace v sekci Statistiky. Společnou vlastností pro tyto aplikace je, že data pro grafy získáváme specificky pomocí tříd `StatTableDB` nebo `StatNewDB` a vrácená data jsou `Listy` s typem `Column`. Jelikož proměnná `Column` slouží dynamicky pro různé kombinace dat, každá třída musí získat potřebné hodnoty z této proměnné specificky. Například `BrowserRestController` získává hodnotu `visit` jak `Column.getIntColumn3()` ale `CountryRestController` získává stejnou hodnotu jako `Column.getIntColumn2()`.

## Filtrování

Třída `FilterHeaderDto` představuje nejpoužívanější hodnoty externích filtrů, které se používají k filtrování hodnot pro grafy. Protože téměř každá stránka využívající grafy má externí filtr, nejpoužívanější parametry těchto filtrů se shrnuly do této třídy, aby se zvýšila přehlednost a zabránilo se duplicitě kódů.

Kromě konstruktoru, který nastaví základní hodnoty, třída obsahuje také metodu `groupIdToQuery`. Jejím úkolem je vytvořit a nastavit `query` potřebnou ke zpětné kompatibilitě ze zadaného `rootGroupId`. Vytvořeno `rootGroupIdQuery` představuje sql příkaz pro výběr těch záznamů, kterých `group_id` spadá pod zadané `rootGroupId` jako potomek na kterékoli úrovni (výběr celého pod-stromu web stránek).

## `StatService`

Tato třída byla primárně vytvořena pro snížení duplicitního kódu a zjednodušení zpracování hodnot z externích filtrů. Jelikož každý graf na stránce využívá nějaký externí filtr, byly do této třídy přidány různé metody, které usnadňují zpracování hodnot z externích filtrů nebo práci s nimi. Bližší popis jednotlivých funkcí/metod, jejich vstupy a možné výstupy je přímo v `StatService` třídě ve formě komentářů.
