# Funkce

| Funkce | Popis |
| --------------- | ---------------------- |
| init(config) | Inicializace zásuvného modulu |
| log(param) | Protokolování v konzoli |
| isTouchDevice() | detekce zařízení |

## Inicializace zásuvného modulu

Abyste mohli zásuvný modul Ninja používat, musíte jej inicializovat.

!> V šabloně je nutné mít skripty potřebné pro zásuvný modul Ninja, které jsou v části `global-functions.min.js`

Inicializace zásuvného modulu:

```javascript
Nina.init();
```

Inicializace zásuvného modulu s použitými nastaveními a událostmi:

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

## Přihlašování v konzole

Bude obsahovat seznam **console.warn** se zadaným parametrem, pokud je nastaven při inicializaci zásuvného modulu. `debug` na adrese `true`, nebo pokud je parametr nastaven v adrese URL. `ninjaDebug` na adrese `true`.

Ninja.log v podstatě volá console.warn, který je obalený podmínkou, že se provede pouze v případě, že je povolen režim ladění.

Ve svých skriptech tak můžete mít spoustu Ninja.logů, které vám pomáhají při vývoji, a nemusíte je mazat, stačí vypnout režim ladění a logy se nebudou spouštět.

!> console.warm() je také log, stejně jako console.log() nebo console.info() atd. Je však barevně odlišen. Ve výchozím nastavení je žlutý. Tím se rozlišují logy, které jsou s pluginem Ninja, a standardní logy mimo plugin Ninja.

Příklad použití:

```javascript
Nina.log("Hello world");
```

## Detekce zařízení

Vrací hodnotu true, pokud je zařízení detekováno jako dotykové.

Příklad použití:

```javascript
Nina.isTouchDevice();
```

Kód, který při volání kontroluje, zda se jedná o dotykové zařízení:

```javascript
var isTouchDevice = false,
    supportsTouch = false;

if ( ('ontouchstart' in window) || (window.navigator.msPointerEnabled) || ('ontouchstart' in document.documentElement) ) {
    supportsTouch = true;
}

isTouchDevice = Modernizr.touchevents || supportsTouch;

return isTouchDevice;
```
