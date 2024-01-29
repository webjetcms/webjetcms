# Externá konfigurácia

WebJET sa štandardne konfiguruje priamo v administrácii v sekcii Nastavenia/Konfigurácia. Niekedy je ale potrebné konfigurovať premenné rozdielne pre jednotlivé uzly clustra, alebo prenášať konfiguráciu z externých premenných (napr. rozdielnych pre PROD a TEST servre).

WebJET podporuje nastavenie konfiguračných premenných aj z `Java System Properties` aj z premenných prostredia `Enviroment Variables`.

## Systémové premenné

Systémove premenné sa nastavujú pre každý bežiaci Java proces (aplikačný server). Môžu byť **rozdielne pre každý aplikačný server bežiaci vrámci jedného operačného systému**.

Pre prebratie hodnoty systémovej premennej ```System.getProperty``` je potrebné meno premennej prefixovať výrazom ```webjet.```. Premenné dostanete do WebJETu nastavením parametra -D java procesu.

Na Linuxových OS zvyčajne upravíte súbor ```/etc/conf.d/tomcat_XXX``` alebo ```/etc/default/tomcat_XXX``` kde pridáte požadovanú premennú ako:

```sh
#WebJET: vypnutie dmail sendera (aby sa neposielali mailu duplicitne)
JAVA_OPTS="$JAVA_OPTS -Dwebjet.disableDMailSender=true"
#WebJET: ID nodu clustra
JAVA_OPTS="$JAVA_OPTS -DwebjetNodeId=2"
```

Uvedený zápis nastaví konfiguračnú premennú ```disableDMailSender``` na hodnotu ```false```.

**POZNÁMKA:** Všimnite si nastavenie ID uzla clustra priamo nastavením premennej ```webjetNodeId```, ktorá nepoužíva prefix ```webjet.```. webjetNodeId nie je konfiguračná premenná, ale inštrukciu na nastavenie konfiguračných premenných:

- `clusterMyNodeName` - na hodnotu ```nodeX```
- `pkeyGenOffset` - na hodnotu X

kde ```X``` je hodnota nastavená cez ```webjetNodeId```.

Vo Windows sa nastavujú systémové premenné v programe ```Configure Tomcat``` v záložke ```Java```. Vyššie uvedené premenné nastavíte pridaním na koniec textovej oblasti ```Java Options```:

```
-Dwebjet.disableDMailSender=true
-DwebjetNodeId=2
```

## Premenné prostredia (enviroment variables)

Premenné prostredia sú zvyčajne nastavené na úrovni operačného systému a **sú spoločné pre všetky aplikačné server bežiace na danom operačnom systéme**.

Nastavujú sa s prefixom ```webjet_``` keďže meno premennej prostredia nemôže obsahovať znak bodka. V java kóde sa preberajú ako ```System.getenv```. Na Linux OS v `shell` nastavíte premenné ako:

```sh
#pre csh pouzite namiesto export setnev
export webjet_disableDMailSender=true
export webjetNodeId=2
```

**Premenné prostredia odporúčame používať pri kontajnerizácii**, pretože je možné ich štandardným spôsobom nastavovať spúšťaným kontajnerom.

## Context parameter

Vhodné miesto na nastavenie premennej je aj ```<Parameter``` v ```<Context``` elemente v server.xml pre Tomcat. Výhoda je, že premennú môžete nastavovať pre každý host samostatne. Nastavujú sa s prefixom ```webjet_```

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

## Externé databázové spojenie

Databázové spojenie sa vo WebJETe nastavuje v súbore ```WEB-INF/classes/poolman.xml```. Niekedy je vhodné meniť parametre databázového spojenia podľa prostredia (PROD, TEST), alebo bezpečnosť nepovoľuje mať databázové heslo zapísané v súbore.

WebJET podporuje nastavenie parametrov databázového spojenia zo systémových premenných/premenných prostredia. Súbor `poolman.xml` musí existovať, ale hodnoty môžu byť prázdne:

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

Jednotlivé hodnoty môžete následne nastaviť cez systémové premenné alebo premenné prostredia. Podporované sú nasledovné premenné:

- `webjetDbDriver`
- `webjetDbUserName`
- `webjetDbPassword`
- `webjetDbUrl`
- `webjetDbMinimumSize`
- `webjetDbMaximumSize`

preberú sa len hodnoty, ktoré sú nastavené. Môžete teda kombinovať nastavenie `minumumSize` v `poolman.xml` a následne premennú `webjetDbMinimumSize` nemusíte nastaviť cez premennú prostredia.

**TIP:** pre nastavenie premennej v `JAVA_OPTS` nezabudnite menu premennej pridať parameter `-D`.

Ak potrebujete definovať viaceré databázové spojenia pre iné spojenie ako `iwcm` možné je použiť príponu v mene premennej s hodnotou ```_dbname```, čiže napríklad ```webjetDbUserName_ip_data_jpa```.

### Použitie iného súboru

WebJET umožňuje použiť aj iný `poolman.xml` súbor ako predvolený. Stačí pomocou `JAVA_OPTS` nastaviť parameter ```-DwebjetPoolmanPath=/poolman-local.xml``` s cestou k inému poolman.xml súboru. Zadané meno sa hľadá najskôr priamo ako súbor na disku (zadaný ako absolútna cesta), ak sa nenájde tak ako súbor v adresári ```WEB-INF/classes```.

Nastavenie ```-DwebjetPoolmanPath=/poolman-local.xml``` je globálne pre celý Tomcat, WebJET ale súbor použije len ak existuje, inak použije štandardný ```poolman.xml```.

Ak potrebujete nastaviť iný súbor len pre jeden `Host` Tomcat, môžete použiť Context parameter ```webjetDbname``` v súbore tomcat/server.xml. Ak obsahuje cestu končiacu na .xml nepoužije sa ako meno databázového spojenia, ale ako meno súboru:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="/poolman-acc.xml" override="true"/>
  </Context>
```

### Prepínanie konfigurácie medzi prostrediami

Ak potrebujete prepínať databázové spojenie podľa prostredia môžete v `poolman.xml` definovať viaceré databázové spojenia. Primárne `iwcm` spojenie môže byť nastavené na dev databázu, zároveň pridáte nové spojenie pomenované ```acc``` pre spojenie na `ACC` prostredie.

Následne na `ACC` prostredí v konfigurácii Tomcat v `server.xml` môžete WebJET nastaviť tak, aby namiesto `iwcm` spojenia použil `acc` spojenie nastavením Context parametra ```webjetDbname```:

```xml
  <Context reloadable="false" path="" docBase="/www/tomcat/webapps/webjet/" allowLinking="true" swallowOutput="true">
        <Parameter name="webjetDbname" value="acc" override="true"/>
  </Context>
```