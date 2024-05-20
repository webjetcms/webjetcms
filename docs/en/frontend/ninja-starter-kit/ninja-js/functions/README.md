# Features

| | function | | description | | --------------- | ---------------------- | | init(config) | | initializing the plugin | | | log(param) | logging in the console | | | isTouchDevice() | detecting the device |

## Plugin initialization

In order to use the Ninja plugin, you need to initialize it.

!\&gt; It is necessary to have the scripts needed for the Ninja plugin in the template, which are in `global-functions.min.js`

Plugin initialization:

```javascript
Nina.init();
```

Initialization of the plugin with settings and events used:

```javascript
Nina.init({
	debug: true,
	fireTime: 300,
	onMobileDownBlink: function () {
		// do something...
	},
	onDesktop: function () {
		// do something...
	},
});
```

## Logging in the console

It will list **console.warn** with the specified parameter if it is set when initializing the plugin `debug` at `true`, or if the parameter is set in the URL `ninjaDebug` at `true`.

Ninja.log basically calls console.warn, which is wrapped with a condition that it will only execute if debug mode is enabled.

So you can have a lot of Ninja.logs in your scripts that help you in development and you don't have to delete them, just disable debug mode and the log won't be executed.

!\&gt; console.warm() is also a log, like console.log() or console.info() etc. But it is color-coded. It is yellow by default. This distinguishes between logs that are with the Ninja plugin and standard logs outside of the Ninja plugin.

Example of use:

```javascript
Nina.log("Hello world");
```

## Device detection

Returns true if the device is detected as touch.

Example of use:

```javascript
Nina.isTouchDevice();
```

A code that, when called, checks to see if it is a touch-tone device:

```javascript
var isTouchDevice = false,
	supportsTouch = false;

if ("ontouchstart" in window || window.navigator.msPointerEnabled || "ontouchstart" in document.documentElement) {
	supportsTouch = true;
}

isTouchDevice = Modernizr.touchevents || supportsTouch;

return isTouchDevice;
```
