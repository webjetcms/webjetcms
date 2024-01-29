# Nastavenia
| Nastavenie | Typ | Prednastavené | Popis |
| --- | ----- | --- | ----- |
| fireTime | *Integer* | 200 | Omeškanie zavolania udalosti |
| debug | *Boolean* | false | Debug režim |


## Omeškanie zavolania udalosti *Integer*
Všetky metódy, ktorých spúšťačom je zmena veľkosti monitoru sa zavolajú s oneskorením podľa nastaveného `fireTime`.
Defaultne je nastavený na 200ms.

Príklad nastavenia:
```javascript
Nina.init({
    fireTime: 300
});
```

## Debug režim *Boolean*
Debug režim, ktorý pri nastavení na `true` povolí vypisovanie logov cez `Nina.log("Hello world");`. Defaultne je nastavený na `false`, vtedy sa logy v konzole nevypíšu.

Príklad zapnutia:
```javascript
Nina.init({
    debug: true
});
```

!> **Pozor:**  Ak je v URL adrese nastavený parameter `NinjaDebug` na `true`, vynúti sa zapnutie aj tohto debug režimu. Viac informácií v časti [Debug režim](/ninja-starter-kit/ninja-jv/debug/?id=debug-režim-boolean).