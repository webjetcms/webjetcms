# Events

| Event | Equipment | | ------------------------- | ---------------------------------------------------------------- | | | on{deviceName}() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop | | on{deviceName}Blink() | {deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop | | on{deviceName}Up() | {deviceName} Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~ | | on{deviceName}UpBlink() | {deviceName} Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~ | | on{deviceName}Down() | {deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop | | on{deviceName}DownBlink() | {deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop | | | onResize() | | | onResizeWidth() | | | onResizeHeight() | |

## on{deviceName}()

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop

The event is called (with a delay according to the setting `fireTime`) if:
- when the page is loaded, the width of the device is in the range for {deviceName}.
- when changing the width of the device, if the new width of the device is within the range for {deviceName}.
The event is for everyone {deviceName} will call only once. If the screen size was the same when the page was loaded {deviceName} and the user has resized the window (but still within the range for {deviceName}), the event will not be called a second time.

```javascript
Nina.init({
	onMobile: function () {
		// do something...
	},
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- **onDinosaur()** \&lt; 576px
- 575px \&lt; **onMobile()** \&lt; 768px
- 767px \&lt; **onTablet()** \&lt; 992px
- 991px \&lt; **onLaptop()** \&lt; 1200px
- 1199px \&lt; **onDesktop()**

## on{deviceName}Blink()

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / Desktop

The event will be called on the same terms as he{deviceName}() except that this event is called many times, not just once. If the user changes the width multiple times and is still in range for {deviceName}, the event will be called every time. When the event o\*{deviceName}() it would not be called again.

```javascript
Nina.init({
	onMobileBlink: function () {
		// do something...
	},
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- **onDinosaurBlink()** \&lt; 576px
- 575px \&lt; **onMobileBlink()** \&lt; 768px
- 767px \&lt; **onTabletBlink()** \&lt; 992px
- 991px \&lt; **onLaptopBlink()** \&lt; 1200px
- 1199px \&lt; **onDesktopBlink()**

## on{deviceName}Up()

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~

The event is called (with a delay according to the setting `fireTime`) if: when the page is loaded, the width of the device is greater than {deviceName}. when changing the width of the device, if the new width of the device is greater than {deviceName}. The event is for everyone {deviceName} will call only once. For example, if the screen was larger than the page when the page was loaded {deviceName} and the user has resized the window (but it is still larger than {deviceName}), the event will not be called a second time.

```javascript
Nina.init({
	onMobileUp: function () {
		// do something...
	},
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- 575px \&lt; **onDinosaurUp()**
- 767px \&lt; **onMobileUp()**
- 991px \&lt; **onTabletUp()**
- 1199px \&lt; **onLaptopUp()**

## on{deviceName}UpBlink()

{deviceName} : Dinosaur / Mobile / Tablet / Laptop / ~~Desktop~~

The event will be called on the same terms as he{deviceName}Up() except that this event is called many times, not just once. If the user changes the width multiple times and it is still greater than {deviceName}, the event will be called every time. When the event o\*{deviceName}Up() wouldn't call it again.

```javascript
Nina.init({
	onMobileUpBlink: function () {
		// do something...
	},
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- 575px \&lt; **onDinosaurUpBlink()**
- 767px \&lt; **onMobileUpBlink()**
- 991px \&lt; **onTabletUpBlink()**
- 1199px \&lt; **onLaptopUpBlink()**

## on{deviceName}Down()

{deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop

The event is called (with a delay according to the setting `fireTime`) if: when the page is loaded, the width of the device is less than {deviceName}. when changing the width of the device, if the new width of the device is less than {deviceName}. The event is for everyone {deviceName} will call only once. For example, if the screen was smaller than the {deviceName} and the user has resized the window (but it is still smaller than {deviceName}), the event will not be called a second time.

```javascript
Nina.init({
	onMobileDown: function () {
		// do something...
	},
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- **onMobileDown()** \&lt; 576px
- **onTabletDown()** \&lt; 768px
- **onLaptopDown()** \&lt; 992px
- **onDesktopDown()** \&lt; 1200px

## on{deviceName}DownBlink()

{deviceName} : ~~Dinosaur~~ / Mobile / Tablet / Laptop / Desktop

The event will be called on the same terms as he{deviceName}Down() except that this event is called many times, not just once. If the user changes the width multiple times and it is still less than {deviceName}, the event will be called every time. When the event o\*{deviceName}Down() wouldn't call it again.

```javascript
Nina.init({
	onMobileDownBlink: function () {
		// do something...
	},
});
```

Breakpoints are taken from CSS styles that are set in `_variables.scss`

- **onMobileDownBlink()** \&lt; 576px
- **onTabletDownBlink()** \&lt; 768px
- **onLaptopDownBlink()** \&lt; 992px
- **onDesktopDownBlink()** \&lt; 1200px

## onResize()

The event is called (with a delay according to the setting `fireTime`) if the screen size changes.

```javascript
Nina.init({
	onResize: function () {
		// do something...
	},
});
```

## onResizeWidth()

The event is called (with a delay according to the setting `fireTime`) if the screen width changes.

```javascript
Nina.init({
	onResizeWidth: function () {
		// do something...
	},
});
```

## onResizeHeight()

The event is called (with a delay according to the setting `fireTime`) if the screen height changes.

```javascript
Nina.init({
	onResizeHeight: function () {
		// do something...
	},
});
```
