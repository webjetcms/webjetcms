# Události

| Událost | Zařízení |
| ------------------------- | ---------------------------------------------------------------- |
| na{deviceName}() | {deviceName} : Dinosaurus / Mobilní telefon / Tablet / Notebook / Stolní počítač |
| na{deviceName}Blink() | {deviceName} : Dinosaurus / Mobilní telefon / Tablet / Notebook / Stolní počítač |
| na{deviceName}Up() | {deviceName} : Dinosaurus / Mobilní / Tablet / Notebook / ~~Desktop~~ |
| na{deviceName}UpBlink() | {deviceName} : Dinosaurus / Mobilní / Tablet / Notebook / ~~Desktop~~ |
| na{deviceName}Down() | {deviceName} : ~~Dinosaur~~ / Mobilní / Tablet / Notebook / Stolní počítač |
| na{deviceName}DownBlink() | {deviceName} : ~~Dinosaur~~ / Mobilní / Tablet / Notebook / Stolní počítač |
| onResize() |
| onResizeWidth() | |
| onResizeHeight() | |

## `on{deviceName}()`

{deviceName} : Dinosaurus / Mobilní telefon / Tablet / Notebook / Stolní počítač

Událost je vyvolána (se zpožděním podle nastavení `fireTime`), pokud:
- při načítání stránky je šířka zařízení v rozsahu pro {deviceName}.
- při změně šířky zařízení, pokud je nová šířka zařízení v rozmezí šířky pro {deviceName}.

Akce je určena pro všechny {deviceName} zavolá pouze jednou. Pokud byla velikost obrazovky při načtení stránky stejná. {deviceName} a uživatel změnil velikost okna (ale stále v rámci rozsahu pro {deviceName}), událost nebude podruhé vyvolána.

```javascript
Nina.init({
    onMobile:function() {
        // do something...
    }
});
```

Body zlomu se přebírají ze stylů CSS nastavených v položce `_variables.scss`

- **onDinosaur()** < 576px
- 575px < **onMobile()** < 768px
- 767px < **onTablet()** < 992px
- 991px < **onLaptop()** < 1200px
- 1199px < **onDesktop()**

## `on{deviceName}Blink()`

{deviceName} : Dinosaurus / Mobilní telefon / Tablet / Notebook / Stolní počítač

Akce bude svolána za stejných podmínek jako on.{deviceName}() s tím rozdílem, že tato událost je volána vícekrát, nikoli pouze jednou. Pokud uživatel změní šířku vícekrát a je stále v dosahu pro funkci {deviceName}, bude událost vyvolána pokaždé. Když se událost o\*{deviceName}() by se již neozval.

```javascript
Nina.init({
    onMobileBlink:function() {
        // do something...
    }
});
```

Body zlomu se přebírají ze stylů CSS nastavených v položce `_variables.scss`

- **onDinosaurBlink()** < 576px
- 575px < **onMobileBlink()** < 768px
- 767px < **onTabletBlink()** < 992px
- 991px < **onLaptopBlink()** < 1200px
- 1199px < **onDesktopBlink()**

## `on{deviceName}Up()`

{deviceName} : Dinosaurus / Mobilní / Tablet / Notebook / ~~Desktop~~

Událost je vyvolána (se zpožděním podle nastavení `fireTime`), pokud: při načítání stránky je šířka zařízení větší než {deviceName}. při změně šířky zařízení, pokud je nová šířka zařízení větší než {deviceName}. Akce je určena pro všechny {deviceName} zavolá pouze jednou. Například pokud byla obrazovka při načítání stránky větší než stránka. {deviceName} a uživatel změnil velikost okna (ale stále je větší než velikost okna {deviceName}), událost nebude podruhé vyvolána.

```javascript
Nina.init({
    onMobileUp:function() {
        // do something...
    }
});
```

Body zlomu se přebírají ze stylů CSS nastavených v položce `_variables.scss`

- 575px < **onDinosaurUp()**
- 767px < **onMobileUp()**
- 991px < **onTabletUp()**
- 1199px < **onLaptopUp()**

## `on{deviceName}UpBlink()`

{deviceName} : Dinosaurus / Mobilní / Tablet / Notebook / ~~Desktop~~

Akce bude svolána za stejných podmínek jako on.{deviceName}Up() s tím rozdílem, že tato událost je volána mnohokrát, nikoli pouze jednou. Pokud uživatel změní šířku vícekrát a ta je stále větší než {deviceName}, bude událost vyvolána pokaždé. Když se událost o\*{deviceName}Up() by ji znovu nevolal.

```javascript
Nina.init({
    onMobileUpBlink:function() {
        // do something...
    }
});
```

Body zlomu se přebírají ze stylů CSS nastavených v položce `_variables.scss`

- 575px < **onDinosaurUpBlink()**
- 767px < **onMobileUpBlink()**
- 991px < **onTabletUpBlink()**
- 1199px < **onLaptopUpBlink()**

## `on{deviceName}Down()`

{deviceName} : ~~Dinosaur~~ / Mobilní / Tablet / Notebook / Stolní počítač

Událost je vyvolána (se zpožděním podle nastavení `fireTime`), pokud: při načítání stránky je šířka zařízení menší než {deviceName}. při změně šířky zařízení, pokud je nová šířka zařízení menší než {deviceName}. Akce je určena pro všechny {deviceName} zavolá pouze jednou. Například pokud by obrazovka byla menší než obrazovka {deviceName} a uživatel změnil velikost okna (ale stále je menší než velikost okna {deviceName}), událost nebude podruhé vyvolána.

```javascript
Nina.init({
    onMobileDown:function() {
        // do something...
    }
});
```

Body zlomu se přebírají ze stylů CSS nastavených v položce `_variables.scss`

- **onMobileDown()** < 576px
- **onTabletDown()** < 768px
- **onLaptopDown()** < 992px
- **onDesktopDown()** < 1200px

## `on{deviceName}DownBlink()`

{deviceName} : ~~Dinosaur~~ / Mobilní / Tablet / Notebook / Stolní počítač

Akce bude svolána za stejných podmínek jako on.{deviceName}Down() s tím rozdílem, že tato událost je volána mnohokrát, nikoli pouze jednou. Pokud uživatel několikrát změní šířku a ta je stále menší než {deviceName}, bude událost vyvolána pokaždé. Když se událost o\*{deviceName}Down() by ji znovu nevolal.

```javascript
Nina.init({
    onMobileDownBlink:function() {
        // do something...
    }
});
```

Body zlomu se přebírají ze stylů CSS nastavených v položce `_variables.scss`

- **onMobileDownBlink()** < 576px
- **onTabletDownBlink()** < 768px
- **onLaptopDownBlink()** < 992px
- **onDesktopDownBlink()** < 1200px

## `onResize()`

Událost je vyvolána (se zpožděním podle nastavení `fireTime`), pokud se změní velikost obrazovky.

```javascript
Nina.init({
    onResize:function() {
        // do something...
    }
});
```

## `onResizeWidth()`

Událost je vyvolána (se zpožděním podle nastavení `fireTime`), pokud se změní šířka obrazovky.

```javascript
Nina.init({
    onResizeWidth:function() {
        // do something...
    }
});
```

## `onResizeHeight()`

Událost je vyvolána (se zpožděním podle nastavení `fireTime`), pokud se změní výška obrazovky.

```javascript
Nina.init({
    onResizeHeight:function() {
        // do something...
    }
});
```
