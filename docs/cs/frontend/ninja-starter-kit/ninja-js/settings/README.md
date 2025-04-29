# Nastavení

| Nastavení | Typ | Přednastaveno | Popis |
 | ---------- | --------- | ------------- | ---------------------------- |
 | fireTime | *Integer* | 200 | Zpoždění zavolání události |
 | debug | *Boolean* | false | Debug režim |

## Zpoždění zavolání události *Integer*

Všechny metody, jejichž spouštěčem je změna velikosti monitoru se zavolají se zpožděním podle nastaveného `fireTime`. Defaultně je nastaven na 200ms.

Příklad nastavení:

```javascript
Nina.init({
    fireTime: 300
});
```

## Debug režim *Boolean*

Debug režim, který při nastavení na `true` povolí vypisování logů přes `Nina.log("Hello world");`. Defaultně je nastaven na `false`, tehdy se logy v konzole nevypíší.

Příklad zapnutí:

```javascript
Nina.init({
    debug: true
});
```

!> **Upozornění:** Pokud je v URL adrese nastaven parametr `NinjaDebug` na `true`, vynutí se zapnutí i tohoto debug režimu. Více informací v části [Debug režim](/ninja-starter-kit/ninja-jv/debug/?id=debug-režim-boolean).
