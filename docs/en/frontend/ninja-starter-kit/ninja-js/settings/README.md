# Settings

| Settings | Type | Presets | Description | | ---------- | --------- | ------------- | ---------------------------- | | fireTime | *Integer* | 200 | Event call delay | | debug | *Boolean* | false | Debug mode |

## Delayed event call *Integer*

All methods that are triggered by a monitor resize are called with a delay according to the set `fireTime`. Default is set to 200ms.

Example setup:

```javascript
Nina.init({
	fireTime: 300,
});
```

## Debug mode *Boolean*

Debug mode, which when set to `true` enable logging via `Nina.log("Hello world");`. Default is set to `false`, then the logs in the console will not be displayed.

Example of switching on:

```javascript
Nina.init({
	debug: true,
});
```

!\&gt; **Attention:** If the parameter is set in the URL `NinjaDebug` at `true`, this debug mode is also forced on. For more information, see [Debug mode](/ninja-starter-kit/ninja-jv/debug/?id=debug-režim-boolean).
