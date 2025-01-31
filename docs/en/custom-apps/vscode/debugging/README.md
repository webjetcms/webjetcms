# Setting debug mode

With the correct gradle project setup, it is possible to debug not only Java code, but also JavaScript files. Podporovaný je tzv. `hot-swap`, i.e. changing the code while the server is running without restarting it. However, the standard restriction that changes can only be made inside methods (Java does not support `hot-swap` where the structure is changed - new methods are added or existing ones have their parameters changed). In this case, it is necessary to restart the web application.

## Setting up an application server

To run the Tomcat application server within a gradle project, the extension is used [gretty](https://gretty-gradle-plugin.github.io/gretty-doc/). It supports hot-swap, but must be set up correctly. The following parameters are important:
- `reloadOnClassChange = false` - disables the automatic restart of Tomcat when the class is compiled, this prevents the application server from restarting.
- `-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005` - activates the ability to connect the debugger on port 5005.
- `-Dwebjet.showDocActionAllowedDocids=4,383,390` - list of page IDs that can be opened directly by entering the docid parameter in the URL without logging into the administration (used to open the page directly in `Launch Chrome`).

```gradle
gretty {
    servletContainer = 'tomcat9'
    contextPath = ''
    httpPort = 80
    //hard mod spusti WJ rovno z src/main/webapp co zrychli start (netreba kopirovat JSP subory niekde inde)
    inplaceMode = 'hard'
    //scanovania pre hotswap
    scanInterval = 1
    scanner = 'jdk'
    scanDir "${projectDir}/build/classes/java/main"
    fastReload = true
    //chceme aby sa tomcat nereloadol po zmene triedy, v IDE je potrebne kliknut na Hot Code Replace pre aplikovanie zmeny v triede
    reloadOnClassChange = false
    debugSuspend = false
    //JVM parametre
    jvmArgs = [
        //odporucane premenne pre Tomcat
        '-Dsun.net.client.defaultConnectTimeout=300000',
        '-Dsun.net.client.defaultReadTimeout=300000',
        '-Dfile.encoding=utf-8',
        '-Duser.language=sk',
        '-Duser.country=SK',
        //povolenie remote debug na porte 5005
        '-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005',
        //Uprava konfiguracie pre WebJET, tymto sa prepisuju hodnoty z databazy v Ovladaci panel->Konfiguracia
        '-Dwebjet.smtpServer=smtp.interway.sk',
        "-DwebjetDbname=${System.env.webjetDbname}",
        //zoznam docid ktore je mozne priamo otvorit bez prihlasenia pre Launch Chrome debug rezim
        "-Dwebjet.showDocActionAllowedDocids=4,383,390"
    ]
}
```

In the past it was also possible to use the parameter `managedClassReload = true`, but it uses a library that is not supported in Java 11 and above, if you have it set in your project, delete it from the gretty configuration.

!>**Warning:** if you have a file open that contains an error (e.g. html, or even JavaScript) the debug mode will not start correctly, or it will disconnect after the server starts. Apparently VS Code does not distinguish what type of file the error is in, you must not have a file open at startup whose tab is shown in red.

## Settings for JavaScript debug

Aby bolo možné debugovať JavaScript súbory je potrebné špeciálnym spôsobom spustiť prehliadač (Chrome), ktorý otvorí možnosť pripojenia sa na vývojárske nástroje. This is set in the file `.vscode/launch.json` in which you can have a configuration:

```json
{
    "trace": true,
    "name": "Launch Chrome",
    "request": "launch",
    "type": "chrome",
    "url": "http://iwcm.interway.sk/admin/",
    "webRoot": "${workspaceRoot}/src/main/webapp",
    "sourceMaps": true,
    "disableNetworkCache": true,

    // we have multiple js source folders, so some source maps are still generated with webpack protocol links. Don't know why?
    "sourceMapPathOverrides": {  // if you override this, you MUST provide all defaults again
        "webpack:///./~/*": "${webRoot}/node_modules/*",  // a default
        "webpack:///./*":   "${webRoot}/src/js/*",        // unsure how/why webpack generates ./links.js
        "webpack:///../*": "${webRoot}/src/js/*",         // unsure how/why webpack generates ../links.js
        "webpack:///*":     "*"                           // a default, catch everything else
    }
}
```

This will appear in the Debug launch option. The important thing is the setting:
- `webRoot` - root folder with source codes.
- `sourceMapPathOverrides` - setting source code search against `.map` file.

Here it is important to note that if JavaScript files are compiled, it is necessary to generate `.map` File. However, it typically does not have a precise path in your development environment, so in the configuration `sourceMapPathOverrides` it is possible to set the path replacement/replenishment to absolute.

To `webpack` also generated `.map` the file needs to be configured by setting the attribute `devtoolModuleFilenameTemplate`:

```
module.exports = {
    entry: {
        ...
    },
    output: {
        ...
        devtoolModuleFilenameTemplate: 'file:///[absolute-resource-path]'  // map to source with absolute file path not webpack:// protocol
    },
}
```

If they are used `node` scripts for generating as in the sample templates need to be used [exorcist](https://www.npmjs.com/package/exorcist), we have edited the sample templates for correct entry. Possible changes are in the script `node_scripts/render-scripts.js` in which the name is set `.map` file.

For example, for [Bare template](../../../frontend/examples/template-bare/README.md) the path settings are as follows:

```json
{
    "trace": true,
    "name": "Launch Chrome Bare",
    "request": "launch",
    "type": "chrome",
    "url": "http://iwcm.interway.sk/showdoc.do?docid=383&NO_WJTOOLBAR=true",
    "webRoot": "${workspaceRoot}/src/main/webapp/",
    "sourceMaps": true,
    "disableNetworkCache": true,
    "sourceMapPathOverrides": {
        "src/js/ninja.js": "${webRoot}/templates/bare/bootstrap-bare/src/js/ninja.js",
        "src/js/global-functions.js": "${webRoot}/templates/bare/bootstrap-bare/src/js/global-functions.js",
    }
}
```

JavaScript file mapping makes combining files via `combine.jsp/<combine data-iwcm-combine.../>`, which links multiple files in WebJET. For the page address you can use the parameter `combineEnabledRequest=false` to disable combining files (or `combineEnabled=false` which also remembers the shutdown in the session and uses it for other pages).

If a compiled `ninja.js` (as e.g. in the template `bare`, including jQuery and bootstrap) and it's also the first file in the list, so `combine` does not generate an initial comment for this file, and the line numbers then match against `.map` file, so for this case `combine` there is no need to turn it off.

To start, we recommend to add a parameter `NO_WJTOOLBAR=true` not to add an inline editing tool to the page.

If you are using multidomain remember to set the parameter correctly `-Dwebjet.showDocActionAllowedDocids` v `build.gradle` to open a web page by typing its `docid`.

## VS Code sample configuration file

In the configuration file `launch.json` you can have multiple configurations that you can run as needed. You can have multiple Java configurations and multiple JavaScript configurations. The advantage is that VS Code can run debug mode at the Java code level and run JavaScript debug at the same time. You can switch between the different debug modes in the floating step bar.

There are 2 Java configurations in the sample file - one for the database connection in the file `poolman.xml` and one for the connection in the file `poolman-local.xml` (e.g. to your local database). File `src/main/resources/poolman-local*.xml` is in `.gitignore` and can be configured by each programmer according to their needs. Configuration `Debug (Attach)` is intended for additional connection to the debug port after starting the project (or after starting it from the terminal).

In addition, there are examples of running JavaScript debug for different folders (templates).

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005,
            "preLaunchTask": "appStart",
            "postDebugTask": "appKill",
            "timeout": 240000,
            "internalConsoleOptions": "neverOpen"
        },
        {
            "type": "java",
            "name": "Debug Local DB",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005,
            "preLaunchTask": "appStartLocalDB",
            "postDebugTask": "appKill",
            "timeout": 240000,
            "internalConsoleOptions": "neverOpen"
        },
        {
            "type": "java",
            "name": "Debug (Attach)",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005,
            "timeout": 240000,
            "internalConsoleOptions": "neverOpen"
        },
        {
            "trace": true,
            "name": "Launch Chrome",
            "request": "launch",
            "type": "chrome",
            "url": "http://iwcm.interway.sk/showdoc.do?docid=4&NO_WJTOOLBAR=true&combineEnabledRequest=false",
            "webRoot": "${workspaceRoot}/src/main/webapp",
            "sourceMaps": true,
            "disableNetworkCache": true,

            // we have multiple js source folders, so some source maps are still generated with webpack protocol links. Don't know why?
            "sourceMapPathOverrides": {  // if you override this, you MUST provide all defaults again
                "webpack:///./~/*": "${webRoot}/node_modules/*",  // a default
                "webpack:///./*":   "${webRoot}/src/js/*",        // unsure how/why webpack generates ./links.js
                "webpack:///../*": "${webRoot}/src/js/*",         // unsure how/why webpack generates ../links.js
                "webpack:///*":     "*"                           // a default, catch everything else
            }
        },
        {
            "trace": true,
            "name": "Launch Chrome Bare",
            "request": "launch",
            "type": "chrome",
            "url": "http://iwcm.interway.sk/showdoc.do?docid=383&NO_WJTOOLBAR=true",
            "webRoot": "${workspaceRoot}/src/main/webapp/",
            "sourceMaps": true,
            "disableNetworkCache": true,
            "sourceMapPathOverrides": {
                "src/js/ninja.js": "${webRoot}/templates/bare/bootstrap-bare/src/js/ninja.js",
                "src/js/global-functions.js": "${webRoot}/templates/bare/bootstrap-bare/src/js/global-functions.js",
            }
        },
        {
            "trace": true,
            "name": "Launch Chrome Creative",
            "request": "launch",
            "type": "chrome",
            "url": "http://iwcm.interway.sk/showdoc.do?docid=390&NO_WJTOOLBAR=true",
            "webRoot": "${workspaceRoot}/src/main/webapp/",
            "sourceMaps": true,
            "disableNetworkCache": true,
            "sourceMapPathOverrides": {
                "src/js/*": "${webRoot}/templates/creative/bootstrap-creative/src/js/*",
            }
        }
    ]
}
```

Note in the sample configuration `sourceMapPathOverrides` demonstration of mapping NPM modules via webpack in `Launch Chrome`, an accurate demonstration of file mapping in `Launch Chrome Bare` and mapping with all the files in the folder in `Launch Chrome Creative`. You can combine these options as needed in your project.
