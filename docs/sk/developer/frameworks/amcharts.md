# Amcharts

Knižnica [amcharts](amcharts.com) sa používa na zobrazenie grafov. Pre WebJET máme zakúpenú komerčnú OEM verziu.

## Použitie

V súbore [app.js](../../../src/main/webapp/admin/v9/src/js/app.js) je pripravená asynchrónna inicializácia amcharts. Tá zabezpečí, že knižnica sa načíta a inicializuje až v prípade potreby.

Po inicializácii vyvolá udalosť ```WJ.initAmcharts.success``` na ```window``` objekte. Na túto udalosť je možné počúvať a následne už použiť objekt ```window.am4core``` cez ktorý je knižnica dostupná.

Príklad použitia:

```javascript
window.initAmcharts().then(module => {

    window.addEventListener("WJ.initAmcharts.success", function(e) {

        // Create chart instance
        //am4core je priamo dostupne, kedze je to window.am4core
        var chart = am4core.create(divId, am4charts.XYChart);

    });
});
```