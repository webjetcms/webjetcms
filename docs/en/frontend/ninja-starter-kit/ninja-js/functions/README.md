# Features

| Function | Description |
| --- | ----- |
| init(config) | Plugin initialization |
| log(param) | Logging in the console |
| isTouchDevice() | Device detection |

## Plugin initialization

To use the Ninja plugin, it needs to be initialized.

!> It is necessary to have the scripts needed for the Ninja plugin linked in the template, which are in `global-functions.min.js`

Initializing the plugin:
```javascript
Nina.init();
```

Initializing the plugin with settings and used events:
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

## Logging in the console

Prints **console.warn** with the specified parameter if `debug` is set to `true` when initializing the plugin, or if the parameter `ninjaDebug` is set to `true` in the URL address.

Ninja.log essentially calls console.warn, which is wrapped with a condition that it will only be executed if debug mode is enabled.

So you can easily have many Ninja.logs in your scripts that help you with development and you don't have to delete them, just turn off debug mode and the log will not be executed.

!> console.warm() is also a log, similar to console.log() or console.info() etc. But it is color-coded. It is yellow by default. This differentiates logs that are with the Ninja plugin and standard logs outside the Ninja plugin.

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

Code that checks if it is a touch device after calling:
```javascript
var isTouchDevice = false,
    supportsTouch = false;

if ( ('ontouchstart' in window) || (window.navigator.msPointerEnabled) || ('ontouchstart' in document.documentElement) ) {
    supportsTouch = true;
}

isTouchDevice = Modernizr.touchevents || supportsTouch;

return isTouchDevice;
```