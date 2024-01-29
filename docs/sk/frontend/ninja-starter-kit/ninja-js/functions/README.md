# Funkcie

| Funkcia | Opis |
| --- | ----- |
| init(config) | Inicializácia pluginu |
| log(param) | Logovanie v konzole |
| isTouchDevice() | Detegovanie zariadenia |

## Inicialuzácia pluginu

Pre použitie Ninja pluginu je potrebné ho inicializovať.

!> Je potrebné mať v šablóne nalinkované skripty, potrebné pre Ninja plugin, ktoré sú v `global-functions.min.js`

Inicializácia pluginu:
```javascript
Nina.init();
```

Inicializácia pluginu s nastaveniami a použitými udalosťami:
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

## Logovanie v konzole

Vypíše **console.warn** so zadaným parametrom ak je pri inicializácií pluginu nastavený `debug` na `true`, alebo ak je v URL adrese nastavený parameter `ninjaDebug` na `true`.

Ninja.log v podstate zavolá console.warn, ktorý je obalený podmienkou, že sa vykoná len v prípade, ak je zapnutý debug režim.

Čiže môžeš mať vo svojich skriptov pokojne veľa Ninja.logov, ktoré ti pomáhajú pri vývoji a nemusíš ich zmazať, stačí ak vypneš debug režim a log sa nevykoná.

!> console.warm() je tiež log, podobne ako console.log() alebo console.info() atď. Je ale farebne odlíšený. Má štandardne žltú farbu. Tým sa odlíšia logy, ktoré sú s Ninja pluginu a štandradné logy mimo Ninja pluginu.

Príklad použitia:
```javascript
Nina.log("Hello world");
```

## Detegovanie zariadenia

Vráti true ak je zariadenie detekované ako dotykové.

Príklad použitia:
```javascript
Nina.isTouchDevice();
```

Kód, ktorý po zavolaní kontroluje, či ide o dotykové zariadenie:
```javascript
var isTouchDevice = false,
    supportsTouch = false;

if ( ('ontouchstart' in window) || (window.navigator.msPointerEnabled) || ('ontouchstart' in document.documentElement) ) {
    supportsTouch = true;
}

isTouchDevice = Modernizr.touchevents || supportsTouch;

return isTouchDevice;
```