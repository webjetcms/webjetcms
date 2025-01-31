# Restart

Click on the option **Restart** in the Settings section, you will see a confirmation to restart WebJET. The reboot will be performed on the server by default, but it depends on the server settings whether the reboot from the web application is enabled. If not, the restart will not be performed.

In the configuration `server.xml` application server [Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/config/context.html) it is necessary to have restart enabled using the attribute `reloadable="true"` in the elements `Context`:

```xml
    <Host name="...">
        <Context reloadable="true" />
    </Host>
```

!> **Warning:** Before restarting, check the availability of your hosting's technical support, as the application server may need to be restarted as well. However, this cannot be done directly from the WebJET environment.

Repeated restarts can also fill the application server's memory, which may require restarting the application server directly on the server.
