# Events

| Event | Device |
| ------------------------- | ---------------------------------------------------------------- |
| on{deviceName}() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop |
| on{deviceName}Blink() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop |
| on{deviceName}Up() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~ |
| on{deviceName}UpBlink() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~ |
| on{deviceName}Down() | {deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop |
| on{deviceName}DownBlink() | {deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop |
| onResize() | |
| onResizeWidth() | |
| onResizeHeight() | |

## `on{deviceName}()`

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop

The event is called (with a delay according to the setting `fireTime`) if:
- when the page is loaded, the width of the device is in the range for {deviceName}.
- when changing the width of the device, if the new width of the device is within the range for {deviceName}.

The event is for everyone {deviceName} will call only once. If the screen size was the same when the page was loaded {deviceName} and the user has resized the window (but still within the range for {deviceName}), the event will not be called a second time.

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

The event will be called on the same terms as he{deviceName}() except that this event is called many times, not just once. If the user changes the width multiple times and is still in range for {deviceName}, the event will be called every time. When the event o\*{deviceName}() it would not be called again.

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

The event is called (with a delay according to the setting `fireTime`) if: when the page is loaded, the width of the device is greater than {deviceName}. when changing the width of the device, if the new width of the device is greater than {deviceName}. The event is for everyone {deviceName} will call only once. For example, if the screen was larger than the page when the page was loaded {deviceName} and the user has resized the window (but it is still larger than {deviceName}), the event will not be called a second time.

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

The event will be called on the same terms as he{deviceName}Up() except that this event is called many times, not just once. If the user changes the width multiple times and it is still greater than {deviceName}, the event will be called every time. When the event o\*{deviceName}Up() wouldn't call it again.

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

The event is called (with a delay according to the setting `fireTime`) if: when the page is loaded, the width of the device is less than {deviceName}. when changing the width of the device, if the new width of the device is less than {deviceName}. The event is for everyone {deviceName} will call only once. For example, if the screen was smaller than the {deviceName} and the user has resized the window (but it is still smaller than {deviceName}), the event will not be called a second time.

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

The event will be called on the same terms as he{deviceName}Down() except that this event is called many times, not just once. If the user changes the width multiple times and it is still less than {deviceName}, the event will be called every time. When the event o\*{deviceName}Down() wouldn't call it again.

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

The event is called (with a delay according to the setting `fireTime`) if the screen size changes.

```javascript
Nina.init({
    onResize:function() {
        // do something...
    }
});
```

## `onResizeWidth()`

The event is called (with a delay according to the setting `fireTime`) if the screen width changes.

```javascript
Nina.init({
    onResizeWidth:function() {
        // do something...
    }
});
```

## `onResizeHeight()`

The event is called (with a delay according to the setting `fireTime`) if the screen height changes.

```javascript
Nina.init({
    onResizeHeight:function() {
        // do something...
    }
});
```
