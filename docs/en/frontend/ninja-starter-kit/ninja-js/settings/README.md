# Settings

| Setting | Type | Preset | Description |
| --- | ----- | --- | ----- |
| fireTime | *Integer* | 200 | Event call delay |
| debug | *Boolean* | false | Debug mode |

## Event call delay *Integer*

All methods triggered by a change in monitor size will be called with a delay according to the set `fireTime`.
By default it is set to 200ms.

Example of setting:
```javascript
Nina.init({
    fireTime: 300
});
```

## Debug mode *Boolean*

Debug mode, which when set to `true` will enable logging via `Nina.log("Hello world");`. By default it is set to `false`, in which case logs will not be written to the console.

Example of switching on:
```javascript
Nina.init({
    debug: true
});
```

!> **Warning:** If the parameter `NinjaDebug` is set to `true` in the URL, this debug mode will also be forced to be enabled. More information in the [Debug mode](/ninja-starter-kit/ninja-jv/debug/?id=debug-mode-boolean) section.