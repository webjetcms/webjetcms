# Restartovat

Klepnutím na možnost **Restartovat** v sekci Nastavení se vám zobrazí potvrzení restartu WebJETu. Restart se systémově provede na serveru, avšak záleží na nastavení serveru, zda je restart z web aplikace povolen. Pokud ne, restart se neprovede.

V konfiguraci `server.xml` aplikačního serveru [Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/config/context.html) je třeba mít povolen restart pomocí atributu `reloadable="true"` v elementu `Context`:

```xml
    <Host name="...">
        <Context reloadable="true" />
    </Host>
```

!> **Upozornění:** Před restartem si ověřte dostupnost technické podpory vašeho hostingu, protože může být nutné restartovat i aplikační server. To však nelze provést přímo z prostředí WebJETu.

Opakovaným restartem se může také zaplnit paměť aplikačního serveru, což může vyžadovat restart aplikačního serveru přímo na serveru.
