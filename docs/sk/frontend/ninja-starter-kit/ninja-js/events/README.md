# Udalosti
| Udalosť | Zariadenie |
| --- | ----- |
| on{deviceName}() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop |
| on{deviceName}Blink() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop |
| on{deviceName}Up() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~ |
| on{deviceName}UpBlink() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~ |
| on{deviceName}Down() | {deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop |
| on{deviceName}DownBlink() | {deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop |
| onResize() |  |
| onResizeWidth() |  |
| onResizeHeight() |  |

## `on{deviceName}()`
{deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop

Udalosť sa zavolá (s omeškaním podľa nastavenia `fireTime`) ak:
- pri načítaní stránky je šírka zariadenia v rozmedzí pre {deviceName}.
- pri zmene šírky zariadenia, ak je nová šírka zariadenia v rozmedzí pre {deviceName}.

Udalosť sa pre každý {deviceName} zavolá len raz. Ak bola pri načítaní stránky obrazovka o veľkosti {deviceName} a používateľ zmenil veľkosť okna (ale stále v rozmedzí pre {deviceName}), tak sa udalosť už druhý krát nezavolá. 

```javascript
Nina.init({
    onMobile:function() {
        // do something...
    }
});
```
Breakpointy sa preberajú z CSS štýlov, ktoré sú nastavené vo `_variables.scss`
- **onDinosaur()** < 576px 
- 575px < **onMobile()** < 768px 
- 767px < **onTablet()** < 992px 
- 991px < **onLaptop()** < 1200px 
- 1199px < **onDesktop()**

## `on{deviceName}Blink()`
{deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop

Udalosť sa zavolá za rovnakých podmienok ako on{deviceName}() až na to, že táto udalosť sa volá mnoho krát, nie len raz. Ak užívateľ zmení viac krát šírku a stále je v rozmezí pre {deviceName}, udalosť sa zakaždým zavolá. Pri udalosti o*{deviceName}() by sa to už viac krát nezavolalo. 

```javascript
Nina.init({
    onMobileBlink:function() {
        // do something...
    }
});
```
 
Breakpointy sa preberajú z CSS štýlov, ktoré sú nastavené vo `_variables.scss`
- **onDinosaurBlink()** < 576px 
- 575px < **onMobileBlink()** < 768px 
- 767px < **onTabletBlink()** < 992px 
- 991px < **onLaptopBlink()** < 1200px 
- 1199px < **onDesktopBlink()**

## `on{deviceName}Up()`
{deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~

Udalosť sa zavolá (s omeškaním podľa nastavenia `fireTime`) ak:
pri načítaní stránky je šírka zariadenia väčšia ako {deviceName}.
pri zmene šírky zariadenia, ak je nová šírka zariadenia väčšia ako {deviceName}.
Udalosť sa pre každý {deviceName} zavolá len raz. Ak napríklad bola pri načítaní stránky obrazovka väčšia ako {deviceName} a používateľ zmenil veľkosť okna (ale stále je väčšia ako {deviceName}), tak sa udalosť už druhý krát nezavolá. 

```javascript
Nina.init({
    onMobileUp:function() {
        // do something...
    }
});
```

Breakpointy sa preberajú z CSS štýlov, ktoré sú nastavené vo `_variables.scss`
- 575px < **onDinosaurUp()**
- 767px < **onMobileUp()**
- 991px < **onTabletUp()**
- 1199px < **onLaptopUp()**

## `on{deviceName}UpBlink()`
{deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~

Udalosť sa zavolá za rovnakých podmienok ako on{deviceName}Up() až na to, že táto udalosť sa volá mnoho krát, nie len raz. Ak užívateľ zmení viac krát šírku a stále je väčšia ako {deviceName}, udalosť sa zakaždým zavolá. Pri udalosti o*{deviceName}Up() by sa to už viac krát nezavolalo. 

```javascript
Nina.init({
    onMobileUpBlink:function() {
        // do something...
    }
});
```

Breakpointy sa preberajú z CSS štýlov, ktoré sú nastavené vo `_variables.scss`
- 575px < **onDinosaurUpBlink()**
- 767px < **onMobileUpBlink()**
- 991px < **onTabletUpBlink()**
- 1199px < **onLaptopUpBlink()**

## `on{deviceName}Down()`
{deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop

Udalosť sa zavolá (s omeškaním podľa nastavenia `fireTime`) ak:
pri načítaní stránky je šírka zariadenia menšia ako {deviceName}.
pri zmene šírky zariadenia, ak je nová šírka zariadenia menšia ako {deviceName}.
Udalosť sa pre každý {deviceName} zavolá len raz. Ak napríklad bola pri načítaní stránky obrazovka menšia ako {deviceName} a používateľ zmenil veľkosť okna (ale stále je menšia ako {deviceName}), tak sa udalosť už druhý krát nezavolá. 

```javascript
Nina.init({
    onMobileDown:function() {
        // do something...
    }
});
```

Breakpointy sa preberajú z CSS štýlov, ktoré sú nastavené vo `_variables.scss`
- **onMobileDown()** < 576px 
- **onTabletDown()** < 768px 
- **onLaptopDown()** < 992px 
- **onDesktopDown()** < 1200px

## `on{deviceName}DownBlink()`
{deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop

Udalosť sa zavolá za rovnakých podmienok ako on{deviceName}Down() až na to, že táto udalosť sa volá mnoho krát, nie len raz. Ak užívateľ zmení viac krát šírku a stále je menšia ako {deviceName}, udalosť sa zakaždým zavolá. Pri udalosti o*{deviceName}Down() by sa to už viac krát nezavolalo. 

```javascript
Nina.init({
    onMobileDownBlink:function() {
        // do something...
    }
});
```

Breakpointy sa preberajú z CSS štýlov, ktoré sú nastavené vo `_variables.scss`
- **onMobileDownBlink()** < 576px 
- **onTabletDownBlink()** < 768px 
- **onLaptopDownBlink()** < 992px 
- **onDesktopDownBlink()** < 1200px

## `onResize()`
Udalosť sa zavolá (s omeškaním podľa nastavenia `fireTime`) ak sa zmení veľkosť obrazovky. 

```javascript
Nina.init({
    onResize:function() {
        // do something...
    }
});
```

## `onResizeWidth()`
Udalosť sa zavolá (s omeškaním podľa nastavenia `fireTime`) ak sa zmení šírka obrazovky. 

```javascript
Nina.init({
    onResizeWidth:function() {
        // do something...
    }
});
```

## `onResizeHeight()`
Udalosť sa zavolá (s omeškaním podľa nastavenia `fireTime`) ak sa zmení výška obrazovky. 

```javascript
Nina.init({
    onResizeHeight:function() {
        // do something...
    }
});
```