# Reštartovať
Kliknutím na možnosť **Reštartovať** v sekcii Nastavenia sa vám zobrazí potvrdenie reštartu WebJETu. Reštart sa systémovo vykoná na serveri, avšak záleží od nastavenia servera, či je reštart z web aplikácie povolený. Ak nie, reštart sa nevykoná.

V konfigurácii `server.xml` aplikačného servera [Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/config/context.html) je potrebné mať povolený reštart pomocou atribútu `reloadable="true"` v elemente `Context`:

```xml
    <Host name="...">
        <Context reloadable="true" />
    </Host>
```

!> **Upozornenie:** Pred reštartom si overte dostupnosť technickej podpory vášho hostingu, pretože môže byť potrebné reštartovať aj aplikačný server. To však nie je možné vykonať priamo z prostredia WebJETu.

Opakovaným reštartom sa môže tiež zaplniť pamäť aplikačného servera, čo môže vyžadovať reštart aplikačného servera priamo na serveri.
