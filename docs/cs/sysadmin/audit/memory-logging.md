# Poslední logy

Aplikace je určena k zobrazení posledních logů, v případě, že nemáte přístup k logům na souborovém systému. Zobrazuje logy, které procházejí přes log framework (čili používají třídu `Logger`), nezobrazí logy zapsané přímo přes `System.out` nebo `System.err`.

![](memory-logging.png)

Podporuje cluster, je tak možné vyžádat i poslední logy z jiného uzlu clusteru. V kartě `Stack Trace` se nachází výpis zásobníku (obsah se ale zobrazí pouze pro chybové záznamy, pro standardní log úrovně je prázdný).

## Konfigurační možnosti/nastavení:
- `loggingInMemoryEnabled` - nastavením na `true/false` povolíte nebo zakážete ukládání logů do paměti.
- `loggingInMemoryQueueSize` - maximální počet logů zapsaných do paměti (výchozí 200). Upozorňujeme, že všechna data jsou načtena najednou do tabulky a z důvodu přenosu `stack trace` mohou být velké. Nedoporučujeme tuto proměnnou nastavovat na extrémně vysokou hodnotu.

Pro správné fungování musí být `logger` nastaven iv souboru `logback.xml`. Ve výchozím nastavení je takto nastaven, pokud jste ale soubor měnili, je třeba doplnit `IN_MEMORY appender` a přidat jeho volání pro `root` element.

```xml
    ...
    <appender name="IN_MEMORY" class="sk.iway.iwcm.system.logging.InMemoryLoggerAppender" />

    <root level="ERROR">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="IN_MEMORY" />
    </root>
    ...
```

## Implementační detaily

- `sk.iway.iwcm.system.logging.InMemoryLoggerAppender` - `appender` pro `logback`, který zajišťuje odeslání logů do `InMemoryLoggingDB`
- `sk.iway.iwcm.system.logging.InMemoryLoggingDB` - třída zajišťuje zápis a získání logů z a do `queue`, načítání logů na clusteru
- `sk.iway.iwcm.system.logging.InMemoryLoggingEvent` - model pro log event
- `sk.iway.iwcm.system.logging.InMemoryLoggerRestController` - controller pro výpis logů do DataTable
