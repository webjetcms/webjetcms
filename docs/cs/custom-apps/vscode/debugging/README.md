# Nastavení režimu ladění

Při správném nastavení projektu gradle je možné ladit nejen kód v jazyce Java, ale také soubory v jazyce JavaScript. Podporován je tzv. `hot-swap`, tj. změna kódu za běhu serveru bez jeho restartování. Standardní omezení, podle kterého lze změny provádět pouze uvnitř metod (Java nepodporuje `hot-swap` kde se mění struktura - přidávají se nové metody nebo se mění parametry stávajících metod). V takovém případě je nutné webovou aplikaci restartovat.

## Nastavení aplikačního serveru

Pro spuštění aplikačního serveru Tomcat v rámci projektu gradle se používá rozšíření [gretty](https://gretty-gradle-plugin.github.io/gretty-doc/). Podporuje hot-swap, ale musí být správně nastaven. Důležité jsou následující parametry:
- `reloadOnClassChange = false` - zakáže automatický restart Tomcatu při kompilaci třídy, čímž zabrání restartu aplikačního serveru.
- `-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005` - aktivuje možnost připojení ladicího programu na portu 5005.
- `-Dwebjet.showDocActionAllowedDocids=4,383,390` - seznam ID stránek, které lze otevřít přímo zadáním parametru docid v adrese URL bez přihlášení do administrace (slouží k otevření stránky přímo v prohlížeči). `Launch Chrome`).

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

V minulosti bylo také možné použít parametr `managedClassReload = true`, ale používá knihovnu, která není podporována v Javě 11 a vyšší, pokud ji máte v projektu nastavenou, odstraňte ji z konfigurace gretty.

**Varování:** pokud máte otevřený soubor, který obsahuje chybu (např. html nebo dokonce JavaScript), režim ladění se nespustí správně nebo se po spuštění serveru odpojí. VS Code zřejmě nerozlišuje, v jakém typu souboru se chyba nachází, nesmíte mít při spuštění otevřený soubor, jehož karta se zobrazuje červeně.

## Nastavení pro ladění JavaScriptu

Abyste mohli ladit soubory JavaScriptu, musíte spustit prohlížeč (Chrome) speciálním způsobem, který otevře možnost připojení k vývojářským nástrojům. To se nastavuje v souboru `.vscode/launch.json` ve kterém můžete mít konfiguraci:

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

Zobrazí se v možnosti spuštění Ladění. Důležité je nastavení:
- `webRoot` - kořenovou složku se zdrojovými kódy.
- `sourceMapPathOverrides` - nastavení vyhledávání zdrojového kódu proti `.map` soubor.

Zde je důležité poznamenat, že pokud jsou soubory JavaScript kompilovány, je nutné vygenerovat `.map` Soubor. Ve vašem vývojovém prostředí však obvykle nemá přesnou cestu, takže v konfiguraci `sourceMapPathOverrides` je možné nastavit nahrazení/doplnění cesty na absolutní.

Na `webpack` také vygeneroval `.map` soubor je třeba nakonfigurovat nastavením atributu `devtoolModuleFilenameTemplate`:

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

Pokud se používají `node` je třeba použít skripty pro generování jako ve vzorových šablonách. [exorcista](https://www.npmjs.com/package/exorcist), upravili jsme vzorové šablony pro správné zadání. Možné změny jsou ve skriptu `node_scripts/render-scripts.js` ve kterém je nastaven název `.map` soubor.

Například pro [Holá šablona](../../../frontend/examples/template-bare/README.md) nastavení cesty jsou následující:

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

Mapování souborů v JavaScriptu umožňuje kombinovat soubory prostřednictvím `combine.jsp/<combine data-iwcm-combine.../>`, který propojuje více souborů v aplikaci WebJET. Pro adresu stránky můžete použít parametr `combineEnabledRequest=false` zakázat kombinování souborů (nebo `combineEnabled=false` který si také pamatuje vypnutí v relaci a používá ho pro další stránky).

Pokud je zkompilovaný `ninja.js` (jako např. v šabloně `bare`, včetně jQuery a bootstrapu) a je to také první soubor v seznamu, takže `combine` nevytváří pro tento soubor počáteční komentář a čísla řádků se pak shodují s čísly `.map` soubor, takže v tomto případě `combine` není třeba ji vypínat.

Pro začátek doporučujeme přidat parametr `NO_WJTOOLBAR=true` nepřidávat na stránku nástroj pro řádkové úpravy.

Pokud používáte multidoménu, nezapomeňte správně nastavit parametr `-Dwebjet.showDocActionAllowedDocids` v `build.gradle` otevřít webovou stránku zadáním jejího `docid`.

## Ukázka konfiguračního souboru VS Code

V konfiguračním souboru `launch.json` můžete mít více konfigurací, které můžete spouštět podle potřeby. Můžete mít více konfigurací Java a více konfigurací JavaScript. Výhodou je, že VS Code může spustit režim ladění na úrovni kódu Java a současně spustit ladění JavaScriptu. Mezi různými režimy ladění můžete přepínat v plovoucím krokovém panelu.

V ukázkovém souboru jsou 2 konfigurace jazyka Java - jedna pro připojení k databázi v souboru `poolman.xml` a jeden pro připojení v souboru `poolman-local.xml` (např. do místní databáze). Soubor `src/main/resources/poolman-local*.xml` je v `.gitignore` a každý programátor si je může nakonfigurovat podle svých potřeb. Konfigurace `Debug (Attach)` je určen pro dodatečné připojení k ladicímu portu po spuštění projektu (nebo po jeho spuštění z terminálu).

Kromě toho jsou zde příklady spuštění ladění JavaScriptu pro různé složky (šablony).

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

Poznámka ve vzorové konfiguraci `sourceMapPathOverrides` ukázka mapování modulů NPM přes webpack v aplikaci `Launch Chrome`, přesná ukázka mapování souborů v programu `Launch Chrome Bare` a mapování se všemi soubory ve složce ve složce `Launch Chrome Creative`. Tyto možnosti můžete v projektu kombinovat podle potřeby.
