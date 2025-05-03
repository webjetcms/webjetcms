# Události

| Událost | Zařízení |
 | ------------------------- | ---------------------------------------------------------------- |
 | on{deviceName}() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop |
 | on{deviceName}Blink() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop |
 | on{deviceName}Up() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~ |
 | on{deviceName}UpBlink() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~ |
 | on{deviceName}Down() | {deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop |
 | on{deviceName}DownBlink() | {deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop |
| onResize() |                                                                  |
| onResizeWidth() |                                                                  |
| onResizeHeight() |                                                                  |

## `on{deviceName}()`

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop

Událost se zavolá (se zpožděním podle nastavení `fireTime`) pokud:
- při načítání stránky je šířka zařízení v rozmezí pro {deviceName}.
- při změně šířky zařízení, je-li nová šířka zařízení v rozmezí pro {deviceName}.

Událost se pro každý {deviceName} zavolá jen jednou. Pokud byla při načítání stránky obrazovka o velikosti {deviceName} a uživatel změnil velikost okna (ale stále v rozmezí pro {deviceName}), tak se událost již podruhé nezavolá.

```javascript
Nina.init({
    onMobile:function() {
        // do something...
    }
});
```

Breakpointy se přebírají z CSS stylů, které jsou nastaveny ve `_variables.scss`

- **onDinosaur()** \< 576px
- 575px \< **onMobile()** \< 768px
- 767px \< **onTablet()** \< 992px
- 991px \< **onLaptop()** \< 1200px
- 1199px \< **onDesktop()**

## `on{deviceName}Blink()`

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop

Událost se zavolá za stejných podmínek jako on{deviceName}() až na to, že tato událost se jmenuje mnohokrát, ne jen jednou. Pokud uživatel změní vícekrát šířku a stále je v rozmezí pro {deviceName}, událost se pokaždé zavolá. Při události o\*{deviceName}() by se to již vícekrát nezavolalo.

```javascript
Nina.init({
    onMobileBlink:function() {
        // do something...
    }
});
```

Breakpointy se přebírají z CSS stylů, které jsou nastaveny ve `_variables.scss`

- **onDinosaurBlink()** \< 576px
- 575px \< **onMobileBlink()** \< 768px
- 767px \< **onTabletBlink()** \< 992px
- 991px \< **onLaptopBlink()** \< 1200px
- 1199px \< **onDesktopBlink()**

## `on{deviceName}Up()`

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~

Událost se zavolá (se zpožděním podle nastavení `fireTime`) pokud: při načítání stránky je šířka zařízení větší než {deviceName}. při změně šířky zařízení, je-li nová šířka zařízení větší než {deviceName}. Událost se pro každý {deviceName} zavolá jen jednou. Pokud například byla při načítání stránky obrazovka větší než {deviceName} a uživatel změnil velikost okna (ale stále je větší než {deviceName}), tak se událost již podruhé nezavolá.

```javascript
Nina.init({
    onMobileUp:function() {
        // do something...
    }
});
```

Breakpointy se přebírají z CSS stylů, které jsou nastaveny ve `_variables.scss`

- 575px \< **onDinosaurUp()**
- 767px \< **onMobileUp()**
- 991px \< **onTabletUp()**
- 1199px \< **onLaptopUp()**

## `on{deviceName}UpBlink()`

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~

Událost se zavolá za stejných podmínek jako on{deviceName}Up() až na to, že tato událost se jmenuje mnohokrát, ne jen jednou. Pokud uživatel změní vícekrát šířku a stále je větší než {deviceName}, událost se pokaždé zavolá. Při události o\*{deviceName}Up() by se to již vícekrát nezavolalo.

```javascript
Nina.init({
    onMobileUpBlink:function() {
        // do something...
    }
});
```

Breakpointy se přebírají z CSS stylů, které jsou nastaveny ve `_variables.scss`

- 575px \< **onDinosaurUpBlink()**
- 767px \< **onMobileUpBlink()**
- 991px \< **onTabletUpBlink()**
- 1199px \< **onLaptopUpBlink()**

## `on{deviceName}Down()`

{deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop

Událost se zavolá (se zpožděním podle nastavení `fireTime`) pokud: při načítání stránky je šířka zařízení menší než {deviceName}. při změně šířky zařízení, je-li nová šířka zařízení menší než {deviceName}. Událost se pro každý {deviceName} zavolá jen jednou. Pokud například byla při načítání stránky obrazovka menší než {deviceName} a uživatel změnil velikost okna (ale stále je menší než {deviceName}), tak se událost již podruhé nezavolá.

```javascript
Nina.init({
    onMobileDown:function() {
        // do something...
    }
});
```

Breakpointy se přebírají z CSS stylů, které jsou nastaveny ve `_variables.scss`

- **onMobileDown()** \< 576px
- **onTabletDown()** \< 768px
- **onLaptopDown()** \< 992px
- **onDesktopDown()** \< 1200px

## `on{deviceName}DownBlink()`

{deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop

Událost se zavolá za stejných podmínek jako on{deviceName}Down() až na to, že tato událost se jmenuje mnohokrát, ne jen jednou. Pokud uživatel změní vícekrát šířku a stále je menší než {deviceName}, událost se pokaždé zavolá. Při události o\*{deviceName}Down() by se to už vícekrát nezavolalo.

```javascript
Nina.init({
    onMobileDownBlink:function() {
        // do something...
    }
});
```

Breakpointy se přebírají z CSS stylů, které jsou nastaveny ve `_variables.scss`

- **onMobileDownBlink()** \< 576px
- **onTabletDownBlink()** \< 768px
- **onLaptopDownBlink()** \< 992px
- **onDesktopDownBlink()** \< 1200px

## `onResize()`

Událost se zavolá (se zpožděním podle nastavení `fireTime`) pokud se změní velikost obrazovky.

```javascript
Nina.init({
    onResize:function() {
        // do something...
    }
});
```

## `onResizeWidth()`

Událost se zavolá (se zpožděním podle nastavení `fireTime`) pokud se změní šířka obrazovky.

```javascript
Nina.init({
    onResizeWidth:function() {
        // do something...
    }
});
```

## `onResizeHeight()`

Událost se zavolá (se zpožděním podle nastavení `fireTime`) pokud se změní výška obrazovky.

```javascript
Nina.init({
    onResizeHeight:function() {
        // do something...
    }
});
```
