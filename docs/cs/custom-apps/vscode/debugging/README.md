# Nastavení debug režimu

S korektním nastavením gradle projektu lze debugovat nejen Java kód, ale také JavaScript soubory. Podporován je tzn. `hot-swap`, neboli výměna kódu za běhu serveru bez potřeby jeho restartu. Platí ale standardní omezení, že změny lze provést pouze uvnitř metod (Java nepodporuje `hot-swap` při kterém se mění struktura - přidají se nové metody, nebo stávajícím se změní parametry). V takovém případě je třeba provést restart web aplikace.

## Nastavení aplikačního serveru

Pro spuštění aplikačního serveru Tomcat v rámci gradle projektu se používá rozšíření [gretty](https://gretty-gradle-plugin.github.io/gretty-doc/). To podporuje hot-swap, musí být ale korektně nastaveno. Důležité jsou následující parametry:
- `reloadOnClassChange = false` - vypne automatický restart Tomcat při zkompilování třídy, to zamezí restartování aplikačního serveru.
- `-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005` - aktivuje možnost připojení debugger na portu 5005.
- `-Dwebjet.showDocActionAllowedDocids=4,383,390` - seznam ID stránek, které lze přímo otevřít zadáním docid parametru v URL bez přihlášení do administrace (používá se pro přímé otevření stránky v `Launch Chrome`).

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

V minulosti bylo možné použít i parametr `managedClassReload = true`, ten ale používá knihovnu, která nemá podporu v Java 11 a více, pokud jej v projektu máte nastaveno, smažte jej z konfigurace gretty.

!>**Upozornění:** pokud máte otevřený soubor, který obsahuje chybu (např. html, nebo i JavaScript) nespustí se debug režim korektně, respektive po spuštění serveru se odpojí. VS Code zjevně nerozlišuje v jakém typu souboru je chyba, nesmíte mít při spuštění otevřený soubor, jehož karta je zobrazena červenou barvou.

## Nastavení pro JavaScript debug

Aby bylo možné debugovat JavaScript soubory je třeba speciálním způsobem spustit prohlížeč (Chrome), který otevře možnost připojení se na vývojářské nástroje. To se nastavuje v souboru `.vscode/launch.json` ve kterém můžete mít konfiguraci:

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

Ta se objeví v možnosti spuštění Debug. Důležité je nastavení:
- `webRoot` - kořenová složka se zdrojovými kódy.
- `sourceMapPathOverrides` - nastavení hledání zdrojových kódů vůči `.map` souborem.

Zde je důležité si uvědomit, že pokud jsou JavaScript soubory kompilované, je třeba aby se generoval i `.map` soubor. Ten ale typicky nemá přesně nastavenou cestu ve vašem vývojovém prostředí, proto v konfiguraci `sourceMapPathOverrides` je možné nastavit nahrazení/doplnění cesty na absolutní.

Aby `webpack` generoval také `.map` soubor je třeba upravit konfiguraci nastavením atributu `devtoolModuleFilenameTemplate`:

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

Pokud se používají `node` skripty pro generování jako je v ukázkových šablonách je třeba použít [exorcist](https://www.npmjs.com/package/exorcist), ukázkové šablony jsme upravili pro korektní zápis. Případné změny jsou ve skriptu `node_scripts/render-scripts.js` ve kterém se nastavuje název `.map` souboru.

Například pro [šablonu Bare](../../../frontend/examples/template-bare/README.md) je nastavení cest následující:

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

Mapování JavaScript souborů komplikuje kombinování souborů přes `combine.jsp/<combine data-iwcm-combine.../>`, které ve WebJETu spojuje více souborů. Pro adresu stránky můžete použít parametr `combineEnabledRequest=false` pro vypnutí kombinování souborů (případně `combineEnabled=false` které si navíc vypnutí zapamatuje v session a použije jej i na další stránky).

Pokud se používá zkompilovaný `ninja.js` (jako např. v šabloně `bare`, obsahující i jQuery a bootstrap) a zároveň je to první soubor v seznamu, tak `combine` pro tento soubor negeneruje úvodní komentář a čísla řádků pak sedí vůči `.map` souboru, tedy pro tento případ `combine` není třeba vypnout.

Pro spuštění doporučujeme přidat i parametr `NO_WJTOOLBAR=true` aby se do stránky nepřidával nástroj pro inline editaci stránky.

Pokud používáte multidomain nezapomeňte správně nastavit parametr `-Dwebjet.showDocActionAllowedDocids` v `build.gradle` pro možnost otevření web stránky zadáním jí `docid`.

## Ukázkový soubor konfigurace VS Code

V konfiguračním souboru `launch.json` můžete mít několik konfigurací, které můžete spouštět podle potřeby. Můžete mít více Java konfigurací a více JavaScript. Výhoda je, že VS Code umí spustit debug režim na úrovni Java kódu a zároveň s ním spustit i JavaScript debug. V plovoucí liště krokování se umíte přepínat mezi jednotlivými debug režimy.

V ukázkovém souboru jsou 2 Java konfigurace - jedna pro databázové připojení v souboru `poolman.xml` a jedno pro připojení v souboru `poolman-local.xml` (tj. např. do vaší lokální databáze). Soubor `src/main/resources/poolman-local*.xml` je v `.gitignore` a může jej mít každý programátor nastavený podle svých potřeb. Konfigurace `Debug (Attach)` je určena k dodatečnému připojení se k debug portu po spuštění projektu (nebo po jeho spuštění z terminálu).

Kromě toho jsou tam ukázky spuštění JavaScript debug pro různé složky (šablony).

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

Všimněte si v ukázce konfigurace `sourceMapPathOverrides` ukázku mapování NPM modulů přes webpack v `Launch Chrome`, přesnou ukázku mapování souborů v `Launch Chrome Bare` a mapování s všech souborů ve složce v `Launch Chrome Creative`. Tyto možnosti můžete podle potřeby kombinovat, jak potřebujete ve vašem projektu.
