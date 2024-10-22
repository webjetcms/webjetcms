# Amcharts

Knihovna [amcharts](amcharts.com) se používá k zobrazení grafů. Pro WebJET jsme zakoupili komerční OEM verzi.

## Použití

V souboru [app.js](../../../src/main/webapp/admin/v9/src/js/app.js) asynchronní inicializace amcharts je připravena. Tím je zajištěno, že se knihovna načte a inicializuje pouze tehdy, když je to potřeba.

Po inicializaci vyvolá událost `WJ.initAmcharts.success` na adrese `window` zařízení. Tuto událost je možné vyslechnout a poté již použít objekt `window.am4core` prostřednictvím kterého je knihovna přístupná.

Příklad použití:

```javascript
window.initAmcharts().then(module => {

    window.addEventListener("WJ.initAmcharts.success", function(e) {

        // Create chart instance
        //am4core je priamo dostupne, kedze je to window.am4core
        var chart = am4core.create(divId, am4charts.XYChart);

    });
});
```
