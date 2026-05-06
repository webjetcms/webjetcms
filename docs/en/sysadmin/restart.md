# Restart

By clicking on the **Restart** option in the Settings section, you will see a confirmation of the WebJET restart. The restart is performed by the system on the server, but it depends on the server settings whether the restart from the web application is allowed. If not, the restart will not be performed.

In the `server.xml` configuration of the [Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/config/context.html) application server, restart must be enabled using the `reloadable="true"` attribute in the `Context` element:

```xml
    <Host name="...">
        <Context reloadable="true" />
    </Host>
```

!> **Warning:** Before restarting, check with your hosting provider's technical support, as it may be necessary to restart the application server. However, this cannot be done directly from the WebJET environment.

Repeated restarts can also fill the application server's memory, which may require a restart of the application server directly on the server.
