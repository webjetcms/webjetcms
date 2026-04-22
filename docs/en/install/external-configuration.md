# External configuration

WebJET is configured directly in the administration in the Settings/Configuration section by default. However, sometimes it is necessary to configure variables differently for individual cluster nodes, or to transfer the configuration from external variables (e.g. different for PROD and TEST servers).

WebJET supports setting configuration variables from both `Java System Properties` and `Enviroment Variables` environment variables.

## System variables

System variables are set for each running Java process (application server). They can be **different for each application server running within the same operating system**.

To retrieve the value of the system variable ```System.getProperty```, it is necessary to prefix the variable name with the expression ```webjet.```. You can get the variables into WebJET by setting the -D parameter of the java process.

On Linux OSes, you usually edit the file ```/etc/conf.d/tomcat_XXX``` or ```/etc/default/tomcat_XXX``` where you add the desired variable like:

```sh
#WebJET: vypnutie dmail sendera (aby sa neposielali mailu duplicitne)
JAVA_OPTS="$JAVA_OPTS -Dwebjet.disableDMailSender=true"
#WebJET: ID nodu clustra
JAVA_OPTS="$JAVA_OPTS -DwebjetNodeId=2"
```

The above statement sets the configuration variable ```disableDMailSender``` to the value ```false```.

**NOTE:** Note the cluster node ID is set directly by setting the ```webjetNodeId``` variable, which does not use the ```webjet.``` prefix. webjetNodeId is not a configuration variable, but an instruction to set configuration variables:

- `clusterMyNodeName` - ​​to the value ```nodeX```
- `pkeyGenOffset` - ​​to the value X

where ```X``` is the value set via ```webjetNodeId```.

In Windows, system variables are set in the ```Configure Tomcat``` program in the ```Java``` tab. You can set the above variables by adding ```Java Options``` to the end of the text area:

```
-Dwebjet.disableDMailSender=true
-DwebjetNodeId=2
```

## Environment variables

Environment variables are usually set at the operating system level and **are common to all application servers running on a given operating system**.

They are set with the prefix ```webjet_``` since the environment variable name cannot contain a period. In Java code they are passed as ```System.getenv```. On Linux OS in `shell` you set the variables as:

```sh
#pre csh pouzite namiesto export setnev
export webjet_disableDMailSender=true
export webjetNodeId=2
```

**We recommend using environment variables during containerization** because they can be set in a standard way by the running container.

## Context parameter

A good place to set the variable is also ```<Parameter``` in the ```<Context``` element in server.xml for Tomcat. The advantage is that you can set the variable for each host separately. They are set with the prefix ```webjet_```

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

## Empty value

When entering a value via `-Dwebjet.` or `ENV`, the value will only be used if it is not empty. To be able to enter an empty value, enter the character `-`, which will be replaced by an empty value.

## External database connection

The database connection in WebJET is set in the file ```WEB-INF/classes/poolman.xml```. Sometimes it is appropriate to change the database connection parameters according to the environment (PROD, TEST), or security does not allow having the database password written in the file.

WebJET supports setting database connection parameters from system/environment variables. The file `poolman.xml` must exist, but the values ​​can be empty:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<poolman>
 <datasource>
      <dbname>iwcm</dbname>
      <driver>oracle.jdbc.driver.OracleDriver</driver>
      <url></url>
      <username></username>
      <password></password>
      <minimumSize></minimumSize>
      <maximumSize></maximumSize>
  </datasource>
</poolman>
```

You can then set individual values ​​via system variables or environment variables. The following variables are supported:

- `webjetDbDriver`
- `webjetDbUserName`
- `webjetDbPassword`
- `webjetDbUrl`
- `webjetDbMinimumSize`
- `webjetDbMaximumSize`

only the values ​​that are set will be taken over. So you can combine the settings `minumumSize` in `poolman.xml` and then you don't have to set the variable `webjetDbMinimumSize` via an environment variable.

**TIP:** to set a variable in `JAVA_OPTS`, don't forget to add the parameter `-D` to the variable menu.

If you need to define multiple database connections for a connection other than `iwcm`, you can use a suffix in the variable name with the value ```_dbname```, for example ```webjetDbUserName_ip_data_jpa```.

### Using a different file

WebJET allows you to use another `poolman.xml` file as the default. Just use `JAVA_OPTS` to set the ```-DwebjetPoolmanPath=/poolman-local.xml``` parameter with the path to another poolman.xml file. The specified name is first searched directly as a file on disk (specified as an absolute path), if it is not found then as a file in the ```WEB-INF/classes``` directory.

The ```-DwebjetPoolmanPath=/poolman-local.xml``` setting is global for the entire Tomcat, but WebJET will only use the file if it exists, otherwise it will use the default ```poolman.xml```.

If you need to set up a different file for just one `Host` Tomcat, you can use the Context parameter ```webjetDbname``` in the tomcat/server.xml file. If it contains a path ending in .xml, it will not be used as a database connection name, but as a file name:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="/poolman-acc.xml" override="true"/>
  </Context>
```

### Switching configuration between environments

If you need to switch database connections based on your environment, you can define multiple database connections in `poolman.xml`. The primary `iwcm` connection can be set to the dev database, while adding a new connection named ```acc``` to connect to the `ACC` environment.

Subsequently, on the `ACC` environment in the Tomcat configuration in `server.xml`, you can set WebJET to use the `acc` connection instead of the `iwcm` connection by setting the Context parameter ```webjetDbname```:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="acc" override="true"/>
  </Context>
```