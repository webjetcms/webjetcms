# Setting debug mode

With the correct settings of the gradle project, it is possible to debug not only Java code, but also JavaScript files. The so-called ```hot-swap``` is supported, i.e. changing the code while the server is running without the need to restart it. However, the standard limitation applies that changes can only be made inside methods (Java does not support ```hot-swap```, which changes the structure - new methods are added, or parameters are changed to existing ones). In this case, it is necessary to restart the web application.

## Application server setup

To run the Tomcat application server within a gradle project, the [gretty](https://gretty-gradle-plugin.github.io/gretty-doc/) extension is used. This supports hot-swap, but it must be configured correctly. The following parameters are important:

- ```reloadOnClassChange = false``` - ​​disables automatic restart of Tomcat when compiling a class, this prevents restarting the application server.
- ```-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005``` - ​​activates the ability to connect the debugger on port 5005.
- ```-Dwebjet.showDocActionAllowedDocids=4,383,390``` - ​​list of page IDs that can be directly opened by entering the docid parameter in the URL without logging into the administration (used to directly open the page in ```Launch Chrome```).

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

In the past, it was also possible to use the ```managedClassReload = true``` parameter, but it uses a library that is not supported in Java 11 and higher. If you have it set in your project, delete it from the gretta configuration.

!>**Warning:** if you have a file open that contains an error (e.g. html, or even JavaScript), the debug mode will not start correctly, or it will disconnect after starting the server. VS Code apparently does not distinguish what type of file the error is in, you must not have a file open when starting with a tab that is displayed in red.

## JavaScript debug settings

In order to debug JavaScript files, you need to launch the browser (Chrome) in a special way, which will open the possibility of connecting to the developer tools. This is set in the file ```.vscode/launch.json``` where you can have the configuration:

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

This will appear in the Debug launch option. The important setting is:

- ```webRoot``` - ​​root folder with source codes.
- ```sourceMapPathOverrides``` - ​​setting source code search against ```.map``` files.

It is important to note that if JavaScript files are compiled, the ```.map``` file also needs to be generated. However, this typically does not have an exact path set in your development environment, so in the ```sourceMapPathOverrides``` configuration, it is possible to set the path replacement/completion to absolute.

In order for ```rspack``` to also generate the ```.map``` file, it is necessary to modify the configuration by setting the ```devtoolModuleFilenameTemplate``` attribute:

```javascript
const config = {
    entry: {
        ...
    },
    output: {
        ...
        devtoolModuleFilenameTemplate: 'file:///[absolute-resource-path]'  // map to source with absolute file path not webpack:// protocol
    },
}
```

If you use ```node``` scripts for generation as in the sample templates, you need to use [exorcist](https://www.npmjs.com/package/exorcist), we have modified the sample templates for correct writing. Any changes are in the ```node_scripts/render-scripts.js``` script, which sets the name of the ```.map``` file.

For example, for the [Bare template](../../../frontend/examples/template-bare/README.md) the path settings are as follows:

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

JavaScript file mapping complicates file combining via ```combine.jsp/<combine data-iwcm-combine.../>```, which in WebJET combines multiple files. You can use the ```combineEnabledRequest=false``` parameter for the page address to disable file combining (or ```combineEnabled=false``` which will also remember the disabling in the session and apply it to other pages).

If a compiled ```ninja.js``` is used (such as in the ```bare``` template, which also contains jQuery and bootstrap) and it is also the first file in the list, then ```combine``` does not generate an opening comment for this file and the line numbers are then relative to the ```.map``` file, so in this case ```combine``` does not need to be turned off.

To start, we recommend adding the parameter ```NO_WJTOOLBAR=true``` so that the inline page editing tool is not added to the page.

If you are using multidomain, don't forget to set the ```-Dwebjet.showDocActionAllowedDocids``` parameter in ```build.gradle``` correctly to be able to open a website by entering its ```docid```.

## Sample VS Code configuration file

In the configuration file ```launch.json``` you can have multiple configurations that you can run as needed. You can have multiple Java configurations and multiple JavaScript. The advantage is that VS Code can run debug mode at the Java code level and also run JavaScript debug at the same time. You can switch between the individual debug modes in the floating step bar.

There are 2 Java configurations in the sample file - one for database connection in file ```poolman.xml``` and one for connection in file ```poolman-local.xml``` (i.e. to your local database). File ```src/main/resources/poolman-local*.xml``` is in ```.gitignore``` and can be set up by each programmer according to their needs. Configuration ```Debug (Attach)``` is intended for additional connection to the debug port after starting the project (or after starting it from the terminal).

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

Note in the configuration sample ```sourceMapPathOverrides```, the sample of mapping NPM modules via webpack in ```Launch Chrome```, the exact sample of mapping files in ```Launch Chrome Bare```, and the mapping from all files in a folder in ```Launch Chrome Creative```. You can combine these options as needed for your project.