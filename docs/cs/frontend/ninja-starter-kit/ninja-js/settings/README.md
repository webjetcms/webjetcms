# Nastavení

| Nastavení | Typ | Předvolby | Popis |
| ---------- | --------- | ------------- | ---------------------------- |
| fireTime | *Celé číslo* | 200 | Zpoždění volání události |
| debug | *Boolean* | false | Režim ladění |

## Opožděné volání události *Celé číslo*

Všechny metody, které jsou vyvolány změnou velikosti monitoru, jsou volány se zpožděním podle nastavené hodnoty. `fireTime`. Výchozí hodnota je 200 ms.

Příklad nastavení:

```javascript
Nina.init({
    fireTime: 300
});
```

## Režim ladění *Boolean*

Režim ladění, který při nastavení na `true` povolit protokolování prostřednictvím `Nina.log("Hello world");`. Výchozí hodnota je nastavena na `false`, pak se protokoly v konzole nezobrazí.

Příklad zapnutí:

```javascript
Nina.init({
    debug: true
});
```

!> **Varování:** Pokud je parametr nastaven v adrese URL `NinjaDebug` na adrese `true`, je tento režim ladění také vynuceně zapnutý. Další informace naleznete v části [Režim ladění](/ninja-starter-kit/ninja-jv/debug/?id=debug-režim-boolean).
