# Restartování

Klikněte na možnost **Restartování** v sekci Nastavení se zobrazí potvrzení o restartování WebJETu. Ve výchozím nastavení se restart provede na serveru, ale záleží na nastavení serveru, zda je restart z webové aplikace povolen. Pokud ne, restart se neprovede.

V konfiguraci `server.xml` aplikační server [Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/config/context.html) je nutné povolit restartování pomocí atributu `reloadable="true"` v prvcích `Context`:

```xml
    <Host name="...">
        <Context reloadable="true" />
    </Host>
```

!> **Varování:** Před restartem si ověřte dostupnost technické podpory hostingu, protože může být nutné restartovat i aplikační server. To však nelze provést přímo z prostředí WebJET.

Opakované restarty mohou také zaplnit paměť aplikačního serveru, což může vyžadovat restartování aplikačního serveru přímo na serveru.
