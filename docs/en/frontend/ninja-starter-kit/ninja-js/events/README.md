# Events

| Event | Equipment |
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

The event is called (with a delay according to the `fireTime` setting) if:
- when loading the page, the device width is within the range for {deviceName}.
- when changing the device width, if the new device width is within the range for {deviceName}.

The event is only called once for each {deviceName}. If the screen size was {deviceName} when the page was loaded and the user resized the window (but still within the range for {deviceName}), the event will not be called a second time. 

```javascript
Nina.init({
    onMobile:function() {
        // do something...
    }
});
```
Breakpoints are taken from CSS styles that are set in `_variables.scss`

- **onDinosaur()** < 576px 
- 575px < **onMobile()** < 768px 
- 767px < **onTablet()** < 992px 
- 991px < **onLaptop()** < 1200px 
- 1199px < **onDesktop()**

## `on{deviceName}Blink()`

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop

The event is called under the same conditions as on{deviceName}() except that this event is called multiple times, not just once. If the user changes the width multiple times and is still in range for {deviceName}, the event is called each time. With the o*{deviceName}() event, it would not be called multiple times. 

```javascript
Nina.init({
    onMobileBlink:function() {
        // do something...
    }
});
```
 
Breakpoints are taken from CSS styles that are set in `_variables.scss`

- **onDinosaurBlink()** < 576px 
- 575px < **onMobileBlink()** < 768px 
- 767px < **onTabletBlink()** < 992px 
- 991px < **onLaptopBlink()** < 1200px 
- 1199px < **onDesktopBlink()**

## `on{deviceName}Up()`

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~

The event is called (with a delay according to the `fireTime` setting) if:
The device width is greater than {deviceName} when loading the page.
when changing the device width, if the new device width is greater than {deviceName}.
The event is only called once for each {deviceName}. For example, if the screen was larger than {deviceName} when the page loaded and the user resized the window (but it is still larger than {deviceName}), the event will not be called a second time. 

```javascript
Nina.init({
    onMobileUp:function() {
        // do something...
    }
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- 575px < **onDinosaurUp()**
- 767px < **onMobileUp()**
- 991px < **onTabletUp()**
- 1199px < **onLaptopUp()**

## `on{deviceName}UpBlink()`

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~

The event is called under the same conditions as on{deviceName}Up() except that this event is called multiple times, not just once. If the user changes the width multiple times and it is still larger than {deviceName}, the event is called each time. With the o*{deviceName}Up() event, it would not be called multiple times. 

```javascript
Nina.init({
    onMobileUpBlink:function() {
        // do something...
    }
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- 575px < **onDinosaurUpBlink()**
- 767px < **onMobileUpBlink()**
- 991px < **onTabletUpBlink()**
- 1199px < **onLaptopUpBlink()**

## `on{deviceName}Down()`

{deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop

The event is called (with a delay according to the `fireTime` setting) if:
The device width is less than {deviceName} when loading the page.
when changing the device width, if the new device width is less than {deviceName}.
The event is only called once for each {deviceName}. For example, if the screen was smaller than {deviceName} when the page loaded and the user resized the window (but it is still smaller than {deviceName}), the event will not be called a second time. 

```javascript
Nina.init({
    onMobileDown:function() {
        // do something...
    }
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- **onMobileDown()** < 576px 
- **onTabletDown()** < 768px 
- **onLaptopDown()** < 992px 
- **onDesktopDown()** < 1200px

## `on{deviceName}DownBlink()`

{deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop

The event is called under the same conditions as on{deviceName}Down() except that this event is called multiple times, not just once. If the user changes the width multiple times and it is still smaller than {deviceName}, the event is called each time. With the o*{deviceName}Down() event, it would not be called multiple times. 

```javascript
Nina.init({
    onMobileDownBlink:function() {
        // do something...
    }
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- **onMobileDownBlink()** < 576px 
- **onTabletDownBlink()** < 768px 
- **onLaptopDownBlink()** < 992px 
- **onDesktopDownBlink()** < 1200px

## `onResize()`

The event is called (with a delay according to the `fireTime` setting) if the screen size changes. 

```javascript
Nina.init({
    onResize:function() {
        // do something...
    }
});
```

## `onResizeWidth()`

The event is called (with a delay according to the `fireTime` setting) if the screen width changes. 

```javascript
Nina.init({
    onResizeWidth:function() {
        // do something...
    }
});
```

## `onResizeHeight()`

The event is called (with a delay according to the `fireTime` setting) if the screen height changes. 

```javascript
Nina.init({
    onResizeHeight:function() {
        // do something...
    }
});
```