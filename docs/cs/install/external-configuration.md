# Externí konfigurace

WebJET se standardně konfiguruje přímo v administraci v sekci Nastavení/Konfigurace. Někdy je ale třeba konfigurovat proměnné rozdílné pro jednotlivé uzly clusteru, nebo přenášet konfiguraci z externích proměnných (např. rozdílných pro PROD a TEST servery).

WebJET podporuje nastavení konfiguračních proměnných iz `Java System Properties` iz proměnných prostředí `Enviroment Variables`.

## Systémové proměnné

Systémové proměnné se nastavují pro každý běžící Java proces (aplikační server). Mohou být **rozdílně pro každý aplikační server běžící v rámci jednoho operačního systému**.

Pro převzetí hodnoty systémové proměnné `System.getProperty` je třeba jméno proměnné přefixovat výrazem `webjet.`. Proměnné dostanete do WebJETu nastavením parametru -D java procesu.

Na Linuxových OS obvykle upravíte soubor `/etc/conf.d/tomcat_XXX` nebo `/etc/default/tomcat_XXX` kde přidáte požadovanou proměnnou jako:

```sh
#WebJET: vypnutie dmail sendera (aby sa neposielali mailu duplicitne)
JAVA_OPTS="$JAVA_OPTS -Dwebjet.disableDMailSender=true"
#WebJET: ID nodu clustra
JAVA_OPTS="$JAVA_OPTS -DwebjetNodeId=2"
```

Uvedený zápis nastaví konfigurační proměnnou `disableDMailSender` na hodnotu `false`.

**POZNÁMKA:** Všimněte si nastavení ID uzlu clustra přímo nastavením proměnné `webjetNodeId`, která nepoužívá prefix `webjet.`. webjetNodeId není konfigurační proměnná, ale instrukce pro nastavení konfiguračních proměnných:
- `clusterMyNodeName` - na hodnotu `nodeX`
- `pkeyGenOffset` - na hodnotu X

kde `X` je hodnota nastavena přes `webjetNodeId`.

Ve Windows se nastavují systémové proměnné v programu `Configure Tomcat` v záložce `Java`. Výše uvedené proměnné nastavíte přidáním na konec textové oblasti `Java Options`:

```
-Dwebjet.disableDMailSender=true
-DwebjetNodeId=2
```

## Proměnná prostředí (enviroment variables)

Proměnná prostředí jsou obvykle nastavena na úrovni operačního systému a **jsou společné pro všechny aplikační server běžící na daném operačním systému**.

Nastavují se s prefixem `webjet_` vzhledem k tomu, že jméno proměnné prostředí nemůže obsahovat znak tečka. V java kódu se přebírají jako `System.getenv`. Na Linux OS v `shell` nastavíte proměnné jako:

```sh
#pre csh pouzite namiesto export setnev
export webjet_disableDMailSender=true
export webjetNodeId=2
```

**Proměnné prostředí doporučujeme používat při kontejnerizaci**, protože je možné je standardním způsobem nastavovat spouštěným kontejnerem.

## Context parametr

Vhodné místo pro nastavení proměnné je také `<Parameter` v `<Context` elementu v server.xml pro Tomcat. Výhoda je, že proměnnou můžete nastavovat pro každý host samostatně. Nastavují se s prefixem `webjet_`

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

Při zadání hodnoty přes `-Dwebjet.` nebo `ENV` se použije hodnota, jen pokud není prázdná. Aby bylo možné zadat i prázdnou hodnotu zadejte znak `-`, tento bude nahrazen za prázdnou hodnotu.

## Externí databázové spojení

Databázové spojení se ve WebJETu nastavuje v souboru `WEB-INF/classes/poolman.xml`. Někdy je vhodné měnit parametry databázového spojení podle prostředí (PROD, TEST), nebo bezpečnost nepovoluje mít databázové heslo zapsané v souboru.

WebJET podporuje nastavení parametrů databázového spojení ze systémových proměnných/proměnných prostředí. Soubor `poolman.xml` musí existovat, ale hodnoty mohou být prázdné:

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

Jednotlivé hodnoty můžete následně nastavit přes systémová proměnná nebo proměnná prostředí. Podporovány jsou následující proměnné:
- `webjetDbDriver`
- `webjetDbUserName`
- `webjetDbPassword`
- `webjetDbUrl`
- `webjetDbMinimumSize`
- `webjetDbMaximumSize`

přeberou se pouze hodnoty, které jsou nastaveny. Můžete tedy kombinovat nastavení `minumumSize` v `poolman.xml` a následně proměnnou `webjetDbMinimumSize` nemusíte nastavit přes proměnnou prostředí.

**TIP:** pro nastavení proměnné v `JAVA_OPTS` nezapomeňte menu proměnné přidat parametr `-D`.

Pokud potřebujete definovat více databázových spojení pro jiné spojení než `iwcm` lze použít příponu v měně proměnné s hodnotou `_dbname`, čili například `webjetDbUserName_ip_data_jpa`.

### Použití jiného souboru

WebJET umožňuje použít i jiný `poolman.xml` soubor jako výchozí. Stačí pomocí `JAVA_OPTS` nastavit parametr `-DwebjetPoolmanPath=/poolman-local.xml` s cestou k jinému poolman.xml souboru. Zadané jméno se hledá nejprve přímo jako soubor na disku (zadaný jako absolutní cesta), pokud se nenajde tak jako soubor v adresáři `WEB-INF/classes`.

Nastavení `-DwebjetPoolmanPath=/poolman-local.xml` je globální pro celý Tomcat, WebJET ale soubor použije jen pokud existuje, jinak použije standardní `poolman.xml`.

Pokud potřebujete nastavit jiný soubor jen pro jeden `Host` Tomcat, můžete použít Context parametr `webjetDbname` v souboru tomcat/server.xml. Obsahuje-li cestu končící .xml, nepoužije se jako jméno databázového spojení, ale jako jméno souboru:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="/poolman-acc.xml" override="true"/>
  </Context>
```

### Přepínání konfigurace mezi prostředími

Pokud potřebujete přepínat databázové spojení podle prostředí můžete v `poolman.xml` definovat více databázových spojení. Primární `iwcm` spojení může být nastaveno na dev databázi, zároveň přidáte nové spojení pojmenované `acc` pro spojení na `ACC` prostředí.

Následně na `ACC` prostředí v konfiguraci Tomcat v `server.xml` můžete WebJET nastavit tak, aby místo `iwcm` spojení použil `acc` spojení nastavením Context parametru `webjetDbname`:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="acc" override="true"/>
  </Context>
```
