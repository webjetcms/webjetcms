# Externí konfigurace

WebJET je ve výchozím nastavení konfigurován přímo v administraci v sekci Nastavení/Konfigurace. Někdy je však nutné konfigurovat proměnné pro jednotlivé uzly clusteru odlišně nebo přenést konfiguraci z externích proměnných (např. odlišně pro servery PROD a TEST).

WebJET také podporuje nastavení konfiguračních proměnných z `Java System Properties` také z proměnných prostředí `Enviroment Variables`.

## Systémové proměnné

Systémové proměnné jsou nastaveny pro každý spuštěný proces Java (aplikační server). Mohou být **pro každý aplikační server spuštěný v rámci jednoho operačního systému jinak**.

Převzetí hodnoty systémové proměnné `System.getProperty` je nutné název proměnné předřadit výrazu `webjet.`. Proměnné se do WebJETu dostanou nastavením parametru -D procesu java.

V operačních systémech Linux obvykle upravíte soubor `/etc/conf.d/tomcat_XXX` nebo `/etc/default/tomcat_XXX` kde přidáte požadovanou proměnnou jako:

```sh
#WebJET: vypnutie dmail sendera (aby sa neposielali mailu duplicitne)
JAVA_OPTS="$JAVA_OPTS -Dwebjet.disableDMailSender=true"
#WebJET: ID nodu clustra
JAVA_OPTS="$JAVA_OPTS -DwebjetNodeId=2"
```

Výše uvedená položka nastavuje konfigurační proměnnou `disableDMailSender` na hodnotu `false`.

**KOMENTÁŘ:** Všimněte si, že ID uzlu clusteru se nastavuje přímo pomocí proměnné `webjetNodeId` který nepoužívá předponu `webjet.`. webjetNodeId není konfigurační proměnná, ale pokyn k nastavení konfiguračních proměnných:
- `clusterMyNodeName` - na hodnotu `nodeX`
- `pkeyGenOffset` - na hodnotu X

Kde: `X` je hodnota nastavená pomocí `webjetNodeId`.

V systému Windows se systémové proměnné nastavují v programu `Configure Tomcat` na kartě `Java`. Výše uvedené proměnné můžete nastavit tak, že je přidáte na konec textové oblasti. `Java Options`:

```
-Dwebjet.disableDMailSender=true
-DwebjetNodeId=2
```

## Proměnné prostředí

Proměnné prostředí se obvykle nastavují na úrovni operačního systému a **jsou společné pro všechny aplikační servery běžící v daném operačním systému**.

Nastavují se pomocí předpony `webjet_` protože název proměnné prostředí nemůže obsahovat znak tečky. V java kódu se berou jako `System.getenv`. V operačním systému Linux v `shell` nastavit proměnné jako:

```sh
#pre csh pouzite namiesto export setnev
export webjet_disableDMailSender=true
export webjetNodeId=2
```

**Při kontejnerizaci doporučujeme používat proměnné prostředí**, protože je lze nastavit standardním způsobem pomocí spuštěného kontejneru.

## Kontextový parametr

Vhodným místem pro nastavení proměnné je také `<Parameter` v `<Context` v souboru server.xml pro Tomcat. Výhodou je, že proměnnou můžete nastavit pro každého hostitele zvlášť. Nastavují se pomocí předpony `webjet_`

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

## Prázdná hodnota

Při zadávání hodnoty prostřednictvím `-Dwebjet.` nebo `ENV` se použije pouze v případě, že není prázdná. Chcete-li zadat prázdnou hodnotu, zadejte znak `-`, bude nahrazena prázdnou hodnotou.

## Připojení k externí databázi

Připojení k databázi se nastavuje v systému WebJET v souboru `WEB-INF/classes/poolman.xml`. Někdy je vhodné změnit parametry připojení k databázi podle prostředí (PROD, TEST) nebo zabezpečení neumožňuje zapsat heslo k databázi do souboru.

WebJET podporuje nastavení parametrů připojení k databázi ze systémových proměnných/profesních proměnných. Soubor `poolman.xml` musí existovat, ale hodnoty mohou být prázdné:

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

Jednotlivé hodnoty pak můžete nastavit pomocí systémových proměnných nebo proměnných prostředí. Podporovány jsou následující proměnné:
- `webjetDbDriver`
- `webjetDbUserName`
- `webjetDbPassword`
- `webjetDbUrl`
- `webjetDbMinimumSize`
- `webjetDbMaximumSize`

stáhnou se pouze nastavené hodnoty. Můžete tedy kombinovat nastavení `minumumSize` v `poolman.xml` a pak proměnná `webjetDbMinimumSize` nemusíte ji nastavovat pomocí proměnné prostředí.

**TIP:** pro nastavení proměnné v `JAVA_OPTS` nezapomeňte přidat parametr do nabídky proměnných `-D`.

Pokud potřebujete definovat více databázových připojení pro jiné připojení než `iwcm` je možné v názvu proměnné použít příponu s hodnotou `_dbname` to je například `webjetDbUserName_ip_data_jpa`.

### Použití jiného souboru

WebJET také umožňuje používat další `poolman.xml` jako výchozí. Stačí použít `JAVA_OPTS` nastavit parametr `-DwebjetPoolmanPath=/poolman-local.xml` s cestou k jinému souboru poolman.xml. Zadaný název je nejprve vyhledán přímo jako soubor na disku (zadaný jako absolutní cesta), pokud není nalezen jako soubor v adresáři `WEB-INF/classes`.

Nastavení `-DwebjetPoolmanPath=/poolman-local.xml` je globální pro celý Tomcat, ale WebJET použije tento soubor pouze v případě, že existuje, jinak použije standardní soubor. `poolman.xml`.

Pokud potřebujete nastavit jiný soubor pouze pro jeden. `Host` Tomcat, můžete použít parametr Kontext. `webjetDbname` v souboru tomcat/server.xml. Pokud obsahuje cestu končící na .xml, nepoužije se jako název připojení k databázi, ale jako název souboru:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="/poolman-acc.xml" override="true"/>
  </Context>
```

### Přepínání konfigurace mezi prostředími

Pokud potřebujete přepnout připojení k databázi podle prostředí, můžete v položce `poolman.xml` definovat více databázových připojení. Primární `iwcm` lze nastavit připojení k databázi dev a zároveň přidat nové připojení s názvem `acc` pro připojení k `ACC` prostředí.

Následně na `ACC` prostředí v konfiguraci Tomcatu v `server.xml` můžete nakonfigurovat WebJET tak, aby místo `iwcm` použitá připojení `acc` připojení nastavením parametru Context `webjetDbname`:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="acc" override="true"/>
  </Context>
```
