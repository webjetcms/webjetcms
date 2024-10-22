# Nastavenie debug režimu

S korektným nastavením gradle projektu je možné debugovať nielen Java kód, ale aj JavaScript súbory. Podporovaný je tzv. ```hot-swap```, čiže výmena kódu za behu servera bez potreby jeho reštartu. Platí ale štandardné obmedzenie, že zmeny je možné vykonať len vnútri metód (Java nepodporuje ```hot-swap``` pri ktorom sa mení štruktúra - pridajú sa nové metódy, alebo existujúcim sa zmenia parametre). V takom prípade je potrebné vykonať reštart web aplikácie.

## Nastavenie aplikačného servera

Pre spustenie aplikačného servera Tomcat vrámci gradle projektu sa používa rozšírenie [gretty](https://gretty-gradle-plugin.github.io/gretty-doc/). To podporuje hot-swap, musí byť ale korektne nastavené. Dôležité sú nasledovné parametre:

- ```reloadOnClassChange = false``` - vypne automatický reštart Tomcat pri skompilovaní triedy, to zamedzí reštartovaniu aplikačného servera.
- ```-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005``` - aktivuje možnosť pripojenia debugger na porte 5005.
- ```-Dwebjet.showDocActionAllowedDocids=4,383,390``` - zoznam ID stránok, ktoré je možné priamo otvoriť zadaním docid parametra v URL bez prihlásenia do administrácie (používa sa na priame otvorenie stránky v ```Launch Chrome```).

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

V minulosti bolo možné použiť aj parameter ```managedClassReload = true```, ten ale používa knižnicu, ktorá nemá podporu v Java 11 a viac, ak ho v projekte máte nastavené, zmažte ho z konfigurácie gretty.

!>**Upozornenie:** ak máte otvorený súbor, ktorý obsahuje chybu (napr. html, alebo aj JavaScript) nespustí sa debug režim korektne, respektíve po spustení servera sa odpojí. VS Code zjavne nerozlišuje v akom type súboru je chyba, nesmiete mať pri spustení otvorený súbor, ktorého karta je zobrazená červenou farbou.

## Nastavenie pre JavaScript debug

Aby bolo možné debugovať JavaScript súbory je potrebné špeciálnym spôsobom spustiť prehliadač (Chrome), ktorý otvorí možnosť pripojenia sa na vývojárske nástroje. To sa nastavuje v súbore ```.vscode/launch.json``` v ktorom môžete mať konfiguráciu:

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

Tá sa objaví v možnosti spustenia Debug. Dôležité je nastavenie:

- ```webRoot``` - koreňový priečinok so zdrojovými kódmi.
- ```sourceMapPathOverrides``` - nastavenie hľadania zdrojových kódov voči ```.map``` súborom.

Tu je dôležité si uvedomiť, že ak sú JavaScript súbory kompilované, je potrebné aby sa generoval aj ```.map``` súbor. Ten ale typicky nemá presne nastavenú cestu vo vašom vývojovom prostredí, preto v konfigurácii ```sourceMapPathOverrides``` je možné nastaviť nahradenie/doplnenie cesty na absolútnu.

Aby ```webpack``` generoval aj ```.map``` súbor je potrebné upraviť konfiguráciu nastavením atribútu ```devtoolModuleFilenameTemplate```:

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

Ak sa používajú ```node``` skripty pre generovanie ako je v ukážkových šablónach je potrebné použiť [exorcist](https://www.npmjs.com/package/exorcist), ukážkové šablóny sme upravili pre korektný zápis. Prípadné zmeny sú v skripte ```node_scripts/render-scripts.js``` v ktorom sa nastavuje názov ```.map``` súboru.

Napríklad pre [šablónu Bare](../../../frontend/examples/template-bare/README.md) je nastavenie ciest nasledovné:

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

Mapovanie JavaScript súborov komplikuje kombinovanie súborov cez ```combine.jsp/<combine data-iwcm-combine.../>```, ktoré vo WebJETe spája viacero súborov. Pre adresu stránky môžete použiť parameter ```combineEnabledRequest=false``` pre vypnutie kombinovania súborov (prípadne ```combineEnabled=false``` ktoré si naviac vypnutie zapamätá v session a použije ho aj na ďalšie stránky).

Ak sa používa skompilovaný ```ninja.js``` (ako napr. v šablóne ```bare```, obsahujúci aj jQuery a bootstrap) a zároveň je to prvý súbor v zozname, tak ```combine``` pre tento súbor negeneruje úvodný komentár a čísla riadkov potom sedia voči ```.map``` súboru, čiže pre tento prípad ```combine``` nie je potrebné vypnúť.

Pre spustenie odporúčame pridať aj parameter ```NO_WJTOOLBAR=true``` aby sa do stránky nepridával nástroj na inline editáciu stránky.

Ak používate multidomain nezabudnite správne nastaviť parameter ```-Dwebjet.showDocActionAllowedDocids``` v ```build.gradle``` pre možnosť otvorenia web stránky zadaním jej ```docid```.

## Ukážkový súbor konfigurácie VS Code

V konfiguračnom súbore ```launch.json``` môžete mať viacero konfigurácií, ktoré môžete spúšťať podľa potreby. Môžete mať viaceré Java konfigurácie a viaceré JavaScript. Výhoda je, že VS Code vie spustiť debug režim na úrovni Java kódu a zároveň s ním spustiť aj JavaScript debug. V plávajúcej lište krokovania sa viete prepínať medzi jednotlivými debug režimami.

V ukážkovom súbore sú 2 Java konfigurácie - jedna pre databázové pripojenie v súbore ```poolman.xml``` a jedno na pripojenie v súbore ```poolman-local.xml``` (čiže napr. do vašej lokálnej databázy). Súbor ```src/main/resources/poolman-local*.xml``` je v ```.gitignore``` a môže ho mať každý programátor nastavený podľa svojich potrieb. Konfigurácia ```Debug (Attach)``` je určená na dodatočné pripojenie sa k debug portu po spustení projektu (alebo po jeho spustení z terminálu).

Okrem toho sú tam ukážky spustenia JavaScript debug pre rôzne priečinky (šablóny).

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

Všimnite si v ukážke konfigurácie ```sourceMapPathOverrides``` ukážku mapovania NPM modulov cez webpack v ```Launch Chrome```, presnú ukážku mapovania súborov v ```Launch Chrome Bare``` a mapovanie s všetkých súborov v priečinku v ```Launch Chrome Creative```. Tieto možnosti môžete podľa potreby kombinovať ako potrebujete vo vašom projekte.