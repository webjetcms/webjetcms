# Funkce

| Funkce | Popis |
| --------------- | ---------------------- |
| init(config) | Inicializace pluginu |
| log(param) | Logování v konzole |
| isTouchDevice() | Detekování zařízení |

## Inicializace pluginu

Pro použití Ninja pluginu je třeba jej inicializovat.

!> Je třeba mít v šabloně nalinkované skripty, potřebné pro Ninja plugin, které jsou v `global-functions.min.js`

Inicializace pluginu:

```javascript
Nina.init();
```

Inicializace pluginu s nastaveními a použitými událostmi:

```javascript
Nina.init({
    debug: true,
    fireTime: 300,
    onMobileDownBlink:function() {
        // do something...
    },
    onDesktop:function() {
        // do something...
    }
});
```

## Logování v konzole

Vypíše **console.warn** se zadaným parametrem je-li při inicializaci pluginu nastaven `debug` na `true`, nebo je-li v URL adrese nastaven parametr `ninjaDebug` na `true`.

Ninja.log v podstatě zavolá console.warn, který je obalen podmínkou, že se provede pouze v případě, je-li zapnut debug režim.

Čili můžeš mít ve svých skriptů klidně hodně Ninja.logů, které ti pomáhají při vývoji a nemusíš je smazat, stačí pokud vypneš debug režim a log se neprovede.

!> console.warm() je také log, podobně jako console.log() nebo console.info() atp. Je ale barevně odlišen. Má standardně žlutou barvu. Tím se odliší logy, které jsou s Ninja pluginu a standartní logy mimo Ninja pluginu.

Příklad použití:

```javascript
Nina.log("Hello world");
```

## Detekování zařízení

Vrátí true pokud je zařízení detekováno jako dotykové.

Příklad použití:

```javascript
Nina.isTouchDevice();
```

Kód, který po zavolání kontroluje, zda se jedná o dotykové zařízení:

```javascript
var isTouchDevice = false,
    supportsTouch = false;

if ( ('ontouchstart' in window) || (window.navigator.msPointerEnabled) || ('ontouchstart' in document.documentElement) ) {
    supportsTouch = true;
}

isTouchDevice = Modernizr.touchevents || supportsTouch;

return isTouchDevice;
```
