# Úrovně logování

Aplikace Úrovně logování umožňuje správu úrovni logování pro jednotlivé java balíčky.

![](audit-log-levels-datatable.png)

První záznam v tabulce je vždy **Hlavní úroveň logování** (základní úroveň).

Využívají se 2 konfigurační proměnné:
- **logLevel**, obsahuje hodnotu úrovně logování pro **Hlavní úroveň logování**
- **logLevels**, obsahuje seznam java balíků s úrovní logování (každý na novém řádku). Např.:

```
sk.iway=DEBUG
sk.iway.iwcm=WARN
org.springframework=WARN
```

Změny nad tabulkou jsou uchovávány lokálně v konstantě. Pokud změny (nastavení) chcete uložit permanentně, v editoru je třeba zvolit možnost **Uložit do databáze**. Při uložení se aktualizují zmíněné konfigurační proměnné v databázi.

# Přidání

Při akci přidání je potřebná hodnota java balíku (package) a úrovně logování. Pokud zadáte již přidaný package, nevytvoří se duplicitní hodnota, ale aktualizuje se již existující.

![](audit-log-levels-editor.png)

# Editace

Akce editace se chová rozdílně pro Hlavní úroveň logování a ostatní logování.

## Hlavní úroveň logování

Při editaci hlavní úrovně můžeme zvolit pouze logování typu NORMAL nebo DEBUG (pro podrobné logování). Pokud v editoru změníte hodnotu `Package`, žádná změna se neprovede. Jelikož hlavní úroveň musí být stále přítomna, změnit lze pouze hodnotu úrovně logování.

## Ostatní logování

Změna úrovně logování se uloží, pokud změníte package, původní logování zmizí a bude nahrazeno tímto novým. Povoleny jsou všechny úrovně logování kromě hodnoty NORMAL.

# Mazání

Všechny packages s úrovní logování lze vymazat kromě **Hlavní úrovně logování**. Při pokusu o vymazání se nic s ní nestane (ani hodnota se nezmění).
