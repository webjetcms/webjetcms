# External configuration

WebJET is configured by default directly in the administration in the Settings/Configuration section. However, sometimes it is necessary to configure variables differently for individual cluster nodes, or to transfer configuration from external variables (e.g. different for PROD and TEST servers).

WebJET also supports setting configuration variables from `Java System Properties` also from environment variables `Enviroment Variables`.

## System variables

System variables are set for each running Java process (application server). They can be **differently for each application server running within a single operating system**.

To take the value of the system variable `System.getProperty` it is necessary to prefix the variable name with the expression `webjet.`. You get the variables into WebJET by setting the -D parameter of the java process.

On Linux OSes you usually edit the file `/etc/conf.d/tomcat_XXX` or `/etc/default/tomcat_XXX` where you add the desired variable as:

```sh
#WebJET: vypnutie dmail sendera (aby sa neposielali mailu duplicitne)
JAVA_OPTS="$JAVA_OPTS -Dwebjet.disableDMailSender=true"
#WebJET: ID nodu clustra
JAVA_OPTS="$JAVA_OPTS -DwebjetNodeId=2"
```

The above entry sets the configuration variable `disableDMailSender` to the value of `false`.

**COMMENT:** Note the setting of the cluster node ID directly by setting the variable `webjetNodeId`that does not use a prefix `webjet.`. webjetNodeId is not a configuration variable, but an instruction to set configuration variables:
- `clusterMyNodeName` - to the value of `nodeX`
- `pkeyGenOffset` - to the value of X
Where `X` is the value set via `webjetNodeId`.

In Windows, system variables are set in the program `Configure Tomcat` in the tab `Java`. You can set the above variables by adding them to the end of the text area `Java Options`:

```
-Dwebjet.disableDMailSender=true
-DwebjetNodeId=2
```

## Enviroment variables

Environment variables are usually set at the operating system level and **are common to all application servers running on a given operating system**.

They are set with a prefix `webjet_` since the name of an environment variable cannot contain the period character. In java code, they are taken as `System.getenv`. On Linux OS in `shell` set variables like:

```sh
#pre csh pouzite namiesto export setnev
export webjet_disableDMailSender=true
export webjetNodeId=2
```

**We recommend using environment variables when containerizing**, because they can be set by the standard way by a triggered container.
## Context parameter

A suitable place to set the variable is also `<Parameter` v `<Context` element in server.xml for Tomcat. The advantage is that you can set the variable for each host separately. They are set with the prefix `webjet_`

```xml
      <Host name="meno.domena.sk" debug="0" appBase="webapps2" unpackWARs="false" autoDeploy="false">
        <Alias>alias.domena.sk</Alias>
        <Context path="" docBase="../webapps/webjet" reloadable="true" debug="0" swallowOutput="true">
            <Resources allowLinking="true" />
            <Parameter name="webjet_meno-premennej" value="hodnota" override="true"/>
        </Context>
        <Valve className="org.apache.catalina.valves.RemoteIpValve"
                remoteIpHeader="x-forwarded-for" protocolHeader="x-forwarded-proto" />
      </Host>
```

## External database connection

The database connection is set in WebJET in the file `WEB-INF/classes/poolman.xml`. Sometimes it is advisable to change the database connection parameters according to the environment (PROD, TEST), or security does not allow to have the database password written in the file.

WebJET supports setting database connection parameters from system/environment variables. File `poolman.xml` must exist, but the values can be empty:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<poolman>
 <datasource>
      <dbname>iwcm</dbname>
      <jndiName>jndi-iwcm</jndiName>
      <driver>oracle.jdbc.driver.OracleDriver</driver>
      <url></url>
      <username></username>
      <password></password>
      <minimumSize></minimumSize>
      <maximumSize></maximumSize>
  </datasource>
</poolman>
```

You can then set individual values via system or environment variables. The following variables are supported:
- `webjetDbDriver`
- `webjetDbUserName`
- `webjetDbPassword`
- `webjetDbUrl`
- `webjetDbMinimumSize`
- `webjetDbMaximumSize`
only the values that are set are downloaded. So you can combine the settings `minumumSize` v `poolman.xml` and then the variable `webjetDbMinimumSize` you don't have to set it via an environment variable.

**TIP:** for setting the variable v `JAVA_OPTS` don't forget to add a parameter to the variable menu `-D`.
If you need to define multiple database connections for a connection other than `iwcm` it is possible to use a suffix in the variable name with the value `_dbname`that is, for example `webjetDbUserName_ip_data_jpa`.

### Using another file

WebJET also allows you to use other `poolman.xml` file as default. Just use `JAVA_OPTS` set parameter `-DwebjetPoolmanPath=/poolman-local.xml` with the path to another poolman.xml file. The specified name is first searched for directly as a file on disk (specified as an absolute path), if not found as a file in the directory `WEB-INF/classes`.

Settings `-DwebjetPoolmanPath=/poolman-local.xml` is global for all of Tomcat, but WebJET will only use the file if it exists, otherwise it will use the standard `poolman.xml`.

If you need to set a different file for just one `Host` Tomcat, you can use the Context parameter `webjetDbname` in the tomcat/server.xml file. If it contains a path ending in .xml it is not used as the database connection name, but as the filename:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="/poolman-acc.xml" override="true"/>
  </Context>
```

### Switching configuration between environments

If you need to switch the database connection according to the environment you can in `poolman.xml` define multiple database connections. Primary `iwcm` connection can be set to dev database, at the same time you add a new connection named `acc` for connection to `ACC` environment.

Subsequently on `ACC` environment in the Tomcat configuration in `server.xml` you can configure WebJET so that instead of `iwcm` connections used `acc` connection by setting the Context parameter `webjetDbname`:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="acc" override="true"/>
  </Context>
```
