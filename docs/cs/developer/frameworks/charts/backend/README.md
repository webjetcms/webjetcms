# BackEnd pro práci s grafy

Práce s grafy je výhradně úkolem FrontEndu a BackEnd pouze poskytuje data pro tyto grafy. Protože každý graf filtruje svá data pomocí externího filtru, byly přidány různé třídy, které usnadňují práci s parametry těchto externích filtrů. Získávání a přípravu dat pro grafy zajišťují jednotlivé `RestController` které slouží také stránce, na níž jsou tyto grafy umístěny.

## Typ grafu

`ChartType` je třída typu `Enum`, který slouží k rychlé a bezpečné identifikaci typu grafu.

Vzhledem k tomu, že různé grafy mají stejný zdroj dat, který je třeba pouze specificky přizpůsobit danému typu, je tato rychlá identifikace výhodná.

Třída `ChartType` obsahuje následující hodnoty:
- `NOT_CHART`, tato hodnota je primární předvolbou. Přidání tohoto typu bylo nutné, protože v některých případech datová tabulka a graf sdílely stejný zdroj dat a touto hodnotou lze říci, že se nejedná o graf, ale například o datovou tabulku.
- `PIE`, hodnota představuje graf **Koláč** typ
- `DOUBLE_PIE`, hodnota představuje graf **dvojitá koláčová kůrka** typ (vnější a vnitřní graf)
- `LINE`, hodnota představuje graf **bar** typ
- `BAR`, hodnota představuje graf **Sloupovitý** typ

## Data pro grafy

Formát dat vrácených z BackEnd pro grafy závisí na typu grafu, ke kterému budou data přiřazena:
- `Bar`, vráceným datovým typem je jednoduchý seznam objektů, `java List<T>` kde objekt musí obsahovat textovou proměnnou (osa Y, kategorie) a číselnou proměnnou (osa X, hodnota ke kategorii).
- `Pie`, vráceným datovým typem je jednoduchý seznam objektů, `java List<T>` kde objekt musí obsahovat textovou proměnnou (kategorie) a číselnou proměnnou (hodnota kategorie).
- `DOUBLE_PIE`, vráceným datovým typem je jednoduchý seznam objektů, `java List<T>` kde objekt musí obsahovat textovou proměnnou (kategorie) a dvě číselné proměnné (každá hodnota pro jednu kategorii).
- `Line`, vráceným datovým typem je mapa obsahující listy objektů, `java Map<String, List<T>>`, kde klíč mapy představuje název `datasetu`, objekt v listu objektu (hodnota položky mapy) musí obsahovat číselnou proměnnou (osa Y, hodnota) a proměnnou datum (osa X, kdy byla hodnota získána).

Další informace o procesu získávání/zpracování dat pro grafy jsou konkrétně komentovány v jednotlivých `RestControlleroch` které tyto údaje poskytují.

### Sekce statistiky

Hlavními uživateli grafů jsou aplikace v sekci Statistiky. Společným rysem těchto aplikací je, že data pro grafy získáváme konkrétně pomocí tříd `StatTableDB` nebo `StatNewDB` a vrácená data jsou `Listy` s typem `Column`. Protože proměnná `Column` používá dynamicky pro různé kombinace dat, musí každá třída získat potřebné hodnoty z této proměnné zvlášť. Například `BrowserRestController` získává hodnotu `visit` Stejně jako `Column.getIntColumn3()` Ale `CountryRestController` získá stejnou hodnotu jako `Column.getIntColumn2()`.

## Filtrování

Třída `FilterHeaderDto` představuje nejčastěji používané hodnoty externích filtrů, které se používají k filtrování hodnot pro grafy. Protože téměř každá stránka používající grafy má externí filtr, byly nejčastěji používané parametry těchto filtrů shrnuty do této třídy, aby se zvýšila přehlednost a zabránilo se duplicitě kódu.

Kromě konstruktoru, který nastavuje základní hodnoty, obsahuje třída také metodu `groupIdToQuery`. Jeho úkolem je vytvořit a nastavit `query` vyžadované pro zpětnou kompatibilitu od zadaného `rootGroupId`. Vytvořil `rootGroupIdQuery` je příkaz sql pro výběr těch záznamů, jejichž `group_id` spadá pod uvedenou `rootGroupId` jako potomka na libovolné úrovni (výběr celého podstromu webové stránky).

## `StatService`

Tato třída byla vytvořena především za účelem omezení duplikace kódu a zjednodušení zpracování hodnot z externích filtrů. Protože každý graf na stránce používá nějaký externí filtr, byly do této třídy přidány různé metody, které usnadňují zpracování nebo práci s hodnotami z externích filtrů. Podrobnější popis jednotlivých funkcí/metod, jejich vstupů a možných výstupů je přímo v části `StatService` třídy ve formě komentářů.
