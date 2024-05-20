# Amcharts

Library [amcharts](amcharts.com) is used to display graphs. For WebJET we have purchased a commercial OEM version.

## Use

In the file [app.js](../../../src/main/webapp/admin/v9/src/js/app.js) asynchronous initialization of amcharts is ready. This ensures that the library is loaded and initialized only when needed.

After initialization, it fires an event `WJ.initAmcharts.success` at `window` facility. It is possible to listen to this event and then already use the object `window.am4core` through which the library is accessible.

Example of use:

```javascript
window.initAmcharts().then((module) => {
	window.addEventListener("WJ.initAmcharts.success", function (e) {
		// Create chart instance
		//am4core je priamo dostupne, kedze je to window.am4core
		var chart = am4core.create(divId, am4charts.XYChart);
	});
});
```
