# Assumptions and versions

The current version of WebJET CMS requires `Java 17` a `Tomcat 11`.

The basic project in gradle format can be found at [githube webjetcms/basecms](https://github.com/webjetcms/basecms).

In gradle projects, just specify the version in build.gradle:

```gradle
ext {
    webjetVersion = "2026.0";
}
```

Currently there are the following versions of WebJET:
- `2026.0-jakarta-SNAPSHOT` - regularly updated version from the main repository using `Jakarta namespace`. Requires Tomcat 11, available as [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2026.0-jakarta-SNAPSHOT)
- `2026.0-jakarta` - stabilized version 2026.0 for Tomcat 11 application server using `Jakarta namespace`, no daily shifts are added to it.
- `2026.0` - Stabilized version 2026.0, no daily changes are added.
- `2025.0-jakarta-SNAPSHOT` - stabilized version 2025.52 using `Jakarta namespace`. Requires Tomcat 10/11, available as [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-jakarta-SNAPSHOT)
- `2025.0-SNAPSHOT` - stabilized version 2025.52, available as [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-SNAPSHOT)
- `2025.0.52` - Stabilized version 2025.0.52 with bug fixes against version 2025.0 (without adding enhancements from SNAPSHOT version).
- `2025.0.50` - Stabilized version 2025.0.50 with bug fixes against version 2025.0 (without adding enhancements from SNAPSHOT version).
- `2025.40-jakarta` - stabilized version 2025.40 for Tomcat 10/11 application server using `Jakarta namespace`, no daily shifts are added to it.
- `2025.40` - Stabilized version 2025.40, no daily changes are added.
- `2025.0.40` - Stabilized version 2025.0.40 with bug fixes against version 2025.0 (without adding enhancements from SNAPSHOT version).
- `2025.18` - Stabilized version 2025.18, no daily changes are added.
- `2025.0.23` - Stabilized version 2025.0.23 with bug fixes against version 2025.0 (without adding enhancements from SNAPSHOT version).
- `2025.0` - Stabilized version 2025.0, no daily changes are added.
- `2024.52` - Stabilized version 2024.52, no daily changes are added.
- `2024.0.52` - Stabilized version 2024.0.52 with bug fixes against version 2024.0 (without adding enhancements from SNAPSHOT version).
- `2024.0.47` - Stabilized version 2024.0.47 with bug fixes against version 2024.0 (without adding enhancements from SNAPSHOT version).
- `2024.40` - Stabilized version 2024.40, no daily changes are added.
- `2024.0-SNAPSHOT` - regularly updated version from the master repository version 2024.0 compiled with Java version 17.
- `2024.18` - Stabilized version 2024.18, no daily changes are added.
- `2024.0.34` - Stabilized version 2024.0.34 with bug fixes against version 2024.0 (without adding enhancements from SNAPSHOT version).
- `2024.0.21` - Stabilized version 2024.0.21 with bug fixes against version 2024.0 (without adding enhancements from SNAPSHOT version).
- `2024.0.17` - Stabilized version 2024.0.17 with bug fixes against version 2024.0 (without adding enhancements from SNAPSHOT version).
- `2024.0.9` - Stabilized version 2024.0.9 with bug fixes against version 2024.0 (without adding enhancements from SNAPSHOT version).
- `2024.0` - Stabilized version 2024.0 (technically identical to 2023.52-java17), no daily changes added, compiled with Java version 17.
- `2023.52-java17` - Stabilized version 2023.52, no daily changes added, compiled with Java version 17.
- `2023.52` - Stabilized version 2023.52, no daily changes are added.
- `2023.40-SNAPSHOT-java17` - regularly updated version from the master repository version 2023.40 compiled with Java version 17.
- `2023.40-SNAPSHOT` - regularly updated version from the master repository version 2023.40.
- `2023.40` - Stabilized version 2023.40, no daily changes are added.
- `2023.18-SNAPSHOT-java17` - regularly updated version from the master repository version 2023.18 compiled with Java version 17.
- `2023.18-SNAPSHOT` - regularly updated version from the master repository version 2023.18.
- `2023.18` - Stabilized version 2023.18, no daily changes are added.
- `2023.0-SNAPSHOT` - regularly updated version from the master repository version 2023.0, due to API changes this version ends before the release of version 2023.18 to avoid unexpected API changes in projects.
- `2023.0` - Stabilized version 2023.0, no daily changes are added.
- `2022.0-SNAPSHOT` - regularly updated version from the master repository.
- `2022.52` - stabilized version 2022.52, no daily changes are added.
- `2022.40` - stabilized version 2022.40, no daily changes are added.
- `2022.18` - stabilized version 2022.18, no daily changes are added.
- `2022.0` - Stabilized version 2022.0, no daily changes are added.
- `2021.0-SNAPSHOT` - regularly updated version from the master repository.
- `2021.52` - Stabilized version 2021.52, no daily changes are added.
- `2021.40` - Stabilized version 2021.40, no daily changes are added.
- `2021.13` - Stabilized version 2021.13, no daily changes are added.

For version numbers:
- `YEAR.0.x` - Corrected version, **no new properties are added to it**, during the year it corrects the bugs found in WebJET CMS. The libraries used are updated only within `minor` Versions. If a library patch requires a change in `major` version cannot be incorporated into this version as it may carry the risk of changes in `API`.
- `YEAR.0-SNAPSHOT` - development version which **includes new features** and bug fixes from version `YEAR.0.x`.
- `YEAR.WEEK` - **stabilized version** from that week which is created from `SNAPSHOT` versions after successful multiple testing. Fixes for other bugs will be incorporated into the next version, no patch will be created `YEAR.WEEK.x` but the new `YEAR.WEEK` with a new number. In case of a bug in such a version, it is therefore necessary to count on moving to the next stable version `YEAR.WEEK` or to `YEAR.0-SNAPSHOT` version.

View Full Version `YEAR.0.x` is therefore not fundamentally changed, it contains corrections of errors (unless the correction requires a fundamental change). It is suitable for use by a customer who wants to have a stable version of WebJET without adding new features during the year.

But at the same time, the version does not have to be `YEAR.0.x` the safest. If the library used in WebJET needs to be updated and it contains major changes, we cannot make this change in `YEAR.0.x` version, as it would break compatibility.

It is therefore true that `YEAR.0.x` Is **most stable** in terms of changes and `YEAR.0-SNAPSHOT` Is **safest** in terms of vulnerabilities.

## Changes when moving to Tomcat 9.0.104+

V [Tomcat from version 9.0.104](https://tomcat.apache.org/tomcat-9.0-doc/config/http.html) the control of the number of parameters is changed at `multipart` HTTP request. It is therefore necessary to set/increase the parameter `maxPartCount` at `<Connector` elements with file `tomcat/conf/server.xml` to a value of at least 100, example:

```xml
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443"
               maxPartCount="1000"
               URIEncoding="UTF-8"
               useBodyEncodingForURI="true" relaxedQueryChars="^{}[]|&quot;"
    />
```

## Changes when switching to Jakarta version

Version for `jakarta namespace`, requires Tomcat 11 application server, uses Spring version 7. Breakthrough changes:
- URLs - Spring has introduced exact matches for URLs, if a REST service defines a URL with a slash at the end, it must be used as such. There is a difference in the URL `/admin/rest/service` a `/admin/rest/service/`.
- In Spring DATA repositories for `IN/NOTIN query` it is necessary to add `@Query`, otherwise the SQL will not be created correctly, example:

```java
  //old
  Page<DocDetails> findAllByGroupIdIn(int[] groupIds, Pageable pageable);
  List<UserDetailsEntity> findAllByIdIn(List<Long> ids);

  //new - add @Query and @Param to correctly create JPQL query for Eclipselink
  @Query("SELECT d FROM DocDetails d WHERE d.groupId IN :groupIds")
  Page<DocDetails> findAllByGroupIdIn(@Param("groupIds") int[] groupIds, Pageable pageable);

  @Query(value = "SELECT u FROM UserDetailsEntity u WHERE u.id IN :ids")
  List<UserDetailsEntity> findAllByIdIn(@Param("ids") List<Long> ids);
```

To search the code, you can use the file search `*Repository.java` and search for a regular expression `\(.*List[^)]*\)`, `\(.*Long[\][^)]*\)`, `\(.*Integer[\][^)]*\)`. We recommend to execute the code, display the error in the log and use the generated SQL to `Query` Values. The only problem is the type checking, where `EclipseLink` can't identify that it should check the field/list and not directly the data type.

V `build.gradle` need to be updated `gretty` configuration and add compilation settings `options.compilerArgs += ['-parameters']`:

```gradle
plugins {
    id 'org.gretty' version "5.0.1"
}

configurations {
    grettyRunnerTomcat11 {
    }
}

gretty {
    servletContainer = 'tomcat11'
}

tasks.withType(JavaCompile) {
    //prevent warning messages during compile
    options.compilerArgs += ['-Xlint:none']
    //needed for Spring
    options.compilerArgs += ['-parameters']
}
```

## Changes in the transition to 2025.0-SNAPSHOT

View Full Version `2025.0-SNAPSHOT` is available via [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-SNAPSHOT), you therefore need to add the configuration to your `build.gradle` file:

```gradle
repositories {
    mavenCentral()
    maven {
        url "https://pd4ml.tech/maven2/"
    }
    maven {
        name = "github"
        url = uri("https://maven.pkg.github.com/webjetcms/webjetcms")
        credentials {
            //define in gradle.properties or as environment variables
            username = project.findProperty("gpr.user") ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.api-key") ?: System.getenv("GPR_API_KEY")
        }
    }
    flatDir {
       dirs 'libs'
   }
}
```

Unfortunately GitHub Packages are not publicly available, so you need to set up a login `gpr.user` a `gpr.api-key` in the file `gradle.properties` or via `ENV` Variables. We will provide login details on request.

!> **Warning:** modified initialization of Spring and JPA:
- JPA entities are in `package sk.iway.INSTALL-NAME` do not initialize automatically, a gradual transition to Spring DATA is assumed. If you need `@Entity` initialize set configuration variable `jpaAddPackages` to the necessary value - for example `sk.iway.INSTALL-NAME`. Only classes containing an annotation are initialized `@Entity` or `@Converter`.
- In `web.xml` initialization is no longer required `Apache Struts`, delete the entire `<servlet>` a section containing `<servlet-class>org.apache.struts.action.ActionServlet</servlet-class>` a `<servlet-mapping>` Containing `<servlet-name>action</servlet-name>`.
- Modified Spring initialization order - initialization of WebJET classes is done before customer classes `SpringConfig`.
- Modified initialization `Swagger` - if the configuration variable is not set `swaggerEnabled` to the value of `true` nor does it perform a scan of Java classes at startup.

## Changes in the transition to 2024.0-SNAPSHOT

Similarly for [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) version needs to be added to the `dependencies` block part `implementation("sk.iway:webjet:${webjetVersion}:libs")`:

```gradle
dependencies {
    implementation("sk.iway:webjet:${webjetVersion}")
    implementation("sk.iway:webjet:${webjetVersion}:admin")
    implementation("sk.iway:webjet:${webjetVersion}:components")
    implementation("sk.iway:webjet:${webjetVersion}:libs")
}
```

The following libraries that are not used in the standard installation have been deleted, if your project needs them, please add them to your `build.gradle` file:

```gradle
dependencies {
    implementation("com.amazonaws:aws-java-sdk-core:1.12.+")
    implementation("com.amazonaws:aws-java-sdk-ses:1.12.+")
    implementation("bsf:bsf:2.4.0")
    implementation("commons-validator:commons-validator:1.3.1")
    implementation("taglibs:datetime:1.0.1")
    implementation("net.htmlparser.jericho:jericho-html:3.1")
    implementation("joda-time:joda-time:2.10.13")
    implementation("io.bit3:jsass:5.1.1")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("org.mcavallo:opencloud:0.3")
    implementation("org.springframework:spring-messaging:${springVersion}")
    implementation("net.sf.uadetector:uadetector-core:0.9.22")
    implementation("net.sf.uadetector:uadetector-resources:2014.10")
    implementation("cryptix:cryptix:3.2.0")
    implementation("org.springframework:spring-messaging:${springVersion}")
    implementation("com.google.protobuf:protobuf-java:3.21.7")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.apache.taglibs:taglibs-standard-spec:1.2.5")
    implementation("org.apache.taglibs:taglibs-standard-impl:1.2.5")
    implementation('com.mchange:c3p0:0.9.5.5')
    implementation("xerces:xercesImpl:2.12.2")
    implementation 'jakarta.xml.bind:jakarta.xml.bind-api:2.3.3'
    implementation 'com.sun.xml.bind:jaxb-ri:2.3.3'
}
```

In the section `configurations` delete the expressions:

```gradle
all*.exclude group: 'xml-apis', module: 'xml-apis'
all*.exclude group: 'javax.xml.stream', module: 'stax-api'
```

## Changes when moving to GitHub/Maven Central version

- V [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) the name of the packages is changed from `sk.iway` at `com.webjetcms` and a section is added `libs` which combines all the original `sk.iway` type dependencies `struts,daisydiff,jtidy`. V `build.gradle` edit:

```gradle
repositories {
    mavenCentral()
    maven {
        url "https://pd4ml.tech/maven2/"
    }
    flatDir {
       dirs 'libs'
   }
}

ext {
    webjetVersion = "2024.0.3";
}

dependencies {
    implementation("com.webjetcms:webjetcms:${webjetVersion}")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:admin")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:components")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:libs")
    ...
}
```

## Changes in the transition to 2023.18-SNAPSHOT/2023.40
- In your project, delete the file `src/main/webapp/WEB-INF/struts-config.xml` to use the current WebJET file (from the jar file).

## Changes when moving to Java 17

There are a few changes that need to be made to your project when moving to Java version 17. Project [basecms](https://github.com/webjetcms/basecms/tree/release/webjet-2023-18-java17) has prepared `branch`, `release/webjet-2023-18-java17` with a sample finish. V [this commit](https://github.com/webjetcms/basecms/commit/e4b9cf6f0a88fd6f0b0cc6c57b28e7a3ec924535) the complete list of changes is visible.

The simplified procedure is as follows:

Update `gradle-wrapper` to version 8 (from the original version 6), we recommend upgrading to version 7 first and then to version 8 (upgrading directly to version 8 may result in an error):

```sh
gradlew.bat wrapper --gradle-version 7.4.2
gradlew.bat wrapper --gradle-version 8.1.1
```

After updating the gradle, you will not be able to compile the project, you also need to edit the file `build.gradle`, in which a number of `plugins`. An important change is the transition of WebJET CMS in version `java17` to the standard version `eclipselink` with the WebJET primary key generator setup.

```groovy
plugins {
    ...
    //aktualizacia verzii pluginov
    id 'org.gretty' version "3.1.1"
    id "io.freefair.lombok" version "8.0.1"
    id "org.owasp.dependencycheck" version "8.2.1"
    ...
}

ext {
    //aktualizovana verzia WebJETu
    webjetVersion = "2023.18-SNAPSHOT-java17";
}

dependencies {
    //celu sekciu modules ZMAZAT
    modules {
        module("org.apache.struts:struts-core") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.apache.struts:struts-taglib") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.eclipse.persistence:org.eclipse.persistence.moxy") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.eclipse.persistence:org.eclipse.persistence.core") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.eclipse.persistence:org.eclipse.persistence.asm") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.eclipse.persistence:org.eclipse.persistence.sdo") { replacedBy("sk.iway:webjet", "mame integrovane") }
    }

    //zmazat pri prechode na verziu 2023.18+
    implementation("sk.iway:webjet:${webjetVersion}:struts")
    implementation("sk.iway:webjet:${webjetVersion}:daisydiff")
    implementation("sk.iway:webjet:${webjetVersion}:jtidy")
}

java {
    //nastavenie verzie Java na 17
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

lombok {
    //nastavenie verzie lombok
    version = "1.18.28"
}

gretty {
    //zakomentovat/zmazat managedClassReload = true - nie je mozne pouzit s Java > 8
    //managedClassReload = true
}
```

Then we recommend to restart your development environment, in case of VS Code perform the action `Java: Clean Java Language Server Workspace` to completely delete temporary files.

If you are running multiple installations of WebJET on a single Tomcat server, it is possible that older versions may not be fully compatible with Java 17:

```txt
[ERROR] ContextLoader - Context initialization failed <java.lang.IllegalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf>java.lang.Ill
egalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf
...
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make protected final java.lang.Class java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain) throws java.lang.ClassFormatError accessible: module java.base does not "opens java.lang" to unnamed module @4c6e3350
```

set the following for Tomcat `JAVA_OPTS`:

```txt
JAVA_OPTS="$JAVA_OPTS --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"
```

### Update from Java version 8

If you were still running Tomcat with Java version 8 you may have problems with missing libraries (these are also needed for Java 11). If you get an error in the log `java.lang.NoClassDefFoundError: javax/activation/DataSource`:

```txt
java.util.concurrent.ExecutionException: org.apache.catalina.LifecycleException: Failed to start component [StandardEngine[Catalina].StandardHost[...].StandardContext[]]
    ...
    Caused by: java.lang.NoClassDefFoundError: javax/activation/DataSource
```

it is necessary to add to each installation of WebJET CMS to the folder `WEB-INF/lib` copied libraries from [of this ZIP archive](lib-java11.zip) and delete files (if any):

```txt
jaxb-api-2.1.jar
jaxb-runtime-3.0.0-M2.jar
```

If you have been using the WebJET version `8.0-8.6` - older than `08/2019`, or you get the following error on startup:

```txt
[10.09 13:48:16 {vubintra} {JpaTools}] JPA: adding class: sk.iway.spirit.model.Media
[10.09 13:48:16 {vubintra} {JpaTools}] JPA: adding class: sk.iway.iwcm.io.FileHistoryBean
[10.09 13:48:16 {vubintra} {WebJETJavaSECMPInitializer}] initPersistenceUnits[iwcm], beans=82
[10.09 13:48:16 {vubintra}]  [FAIL]
java.lang.NullPointerException
	at org.eclipse.persistence.internal.jpa.EntityManagerSetupImpl.predeploy(EntityManagerSetupImpl.java:2027)
	at org.eclipse.persistence.internal.jpa.deployment.JPAInitializer.callPredeploy(JPAInitializer.java:100)
	at sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer.initPersistenceUnits(WebJETJavaSECMPInitializer.java:307)
	at sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer.initialize(WebJETJavaSECMPInitializer.java:134)
	at sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer.getJavaSECMPInitializer(WebJETJavaSECMPInitializer.java:95)
	at sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer.getJavaSECMPInitializer(WebJETJavaSECMPInitializer.java:61)
	at sk.iway.iwcm.system.jpa.WebJETPersistenceProvider.createEntityManagerFactory(WebJETPersistenceProvider.java:32)
```

the library needs to be updated `eclipselink` and the Java initialization class, download the update archive [jpa-wj82.zip](jpa-wj82.zip) and unzip it in the root folder of the web application. Overwrite the file `eclipselink.jar` a `WebJETJavaSECMPInitializer.class`.

## Changes on update to 2023.18

View Full Version `2023.18` changes the API and the way distribution archives are generated. The main changes to the API are in the use of generic objects of type `List/Map` instead of specific implementations `ArrayList/Hashtable`. Therefore, you need to recompile your classes and modify the JSP files.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/sfu5b_S7Q8Q" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

V `build.gradle` it is necessary to delete the part:

```
    implementation("sk.iway:webjet:${webjetVersion}:struts")
    implementation("sk.iway:webjet:${webjetVersion}:daisydiff")
    implementation("sk.iway:webjet:${webjetVersion}:jtidy")
```

Also check the file `src/main/resources/logback.xml` in which the date and time format in `ConsoleAppender` (deleted `,SSS`, if you need to log even hundredths of a second use `.SSS`):

```xml
    <pattern>[%X{installName}][%c{1}][%p][%X{userId}] %d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
```

To simplify the update, you can use the script `/admin/update/update-2023-18.jsp` for checking and repairing JSP files. Customer Java classes need to be recompiled and bugs need to be fixed due to API changes.

Several Java classes and packages and the corresponding JSP file are cleaned/deleted. To support the deleted parts in projects, you need to either use the appropriate WebJET NET product or bring them into the project from the version 8 source code.

More information is in [the list of changes](../CHANGELOG-2023.md#removing-dependency-on-version-8).

## Changes compared to version 8.8

V `build.gradle` it is necessary to delete expressions compared to version 8.8:

```gradle
    compile("sk.iway:webjet:${webjetVersion}:swagger-ui")

    compile 'taglibs:standard:1.1.2'
    compile 'javax.servlet:jstl:1.2'

    providedCompile 'org.slf4j:slf4j-log4j12:1.7.25'

    exclude group: 'org.slf4j', module: 'log4j-over-slf4j'
```

and in the section `configurations` modify the exceptions as follows:

```gradle
configurations {
    all*.exclude group: 'org.slf4j', module: 'slf4j-log4j12'
    all*.exclude group: 'org.slf4j', module: 'jcl104-over-slf4j' //je nahradene novsim jcl-over-slf4j
    all*.exclude group: 'commons-logging', module: 'commons-logging'
    all*.exclude group: 'log4j', module: 'log4j'

    //javax.xml.stream:stax-api:1.0-2 -> stax:stax-api:1.0.1
    all*.exclude group: 'javax.xml.stream', module: 'stax-api'

    grettyRunnerTomcat85 {

    }
    grettyRunnerTomcat9 {
        // gretty pouziva staru verziu commons-io, ktora koliduje s nasou
        // https://mvnrepository.com/artifact/commons-io/commons-io
        exclude group: 'commons-io', module: 'commons-io'
    }
}
```

Due to the transition from `log4j` at `logback` delete file `src/main/resources/log4j.properties` and add the file `src/main/resources/logback.xml`:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[%X{installName}][%c{1}][%p][%X{userId}] %d{yyyy-MM-dd HH:mm:ss,SSS} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="IN_MEMORY" class="sk.iway.iwcm.system.logging.InMemoryLoggerAppender" />

    <root level="ERROR">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="IN_MEMORY" />
    </root>

    <logger level="INFO" name="sk.iway"/>

    <!--
    !!! Nastavovat priamo tu len ak je to nevyhnutne potrebne !!!
    Odporucame nastavit logovacie levely v localconf.jsp cez konstantu:
    Constants.setString("logLevels", "sk.iway=DEBUG,org.springframework=DEBUG");

    # Spring logging
    <logger level="DEBUG" name="org.springframework"/>

    # SQL logging
    <logger level="TRACE" name="org.eclipse.persistence"/>
    <logger level="DEBUG" name="org.hibernate"/>
    <logger level="TRACE" name="org.hibernate.type.descriptor.sql.BasicBinder"/>

    # Hibernate logging options (INFO only shows startup messages)
    <logger level="INFO" name="org.hibernate"/>

    # Log JDBC bind parameter runtime arguments
    <logger level="TRACE" name="org.hibernate.type"/>
    -->
</configuration>
```

## Editing the generated archive

If you need to adjust for deployment `WAR` archive, you can use the following tips:

**Other web.xml file**

If you need to deploy a different `web.xml` file as you use during development you can take advantage of the options offered by [gradle war](https://docs.gradle.org/current/userguide/war_plugin.html) role in `build.gradle`:

```
war {
    ....
    webXml = file('src/someWeb.xml') // copies a file to WEB-INF/web.xml
}
```

**Modified logging**

WebJET writes log files using [slf4j/logback](https://logback.qos.ch/manual/configuration.html). This allows you to search for an additional configuration file, e.g. `logback-test.xml` to be used primarily (if any). In it you can have logging set up for development. In the file `logback.xml` you will be set for deployment. When creating `WAR` archive additional `logback-test.xml` v `build.gradle` you skip:

```
war {
    ....
    rootSpec.exclude('**/logback-*.xml')
}
```

## Additional project setup options

### Setting up social login

If you use social login (e.g. Facebook) in your project, you need to add a library to the project gradle `socialauth`. It is not included in the distribution by default because it is rarely used and also contains a potential vulnerability. You add the library in a file `build.gradle`:

```gradle
// https://mvnrepository.com/artifact/org.brickred/socialauth
implementation group: 'org.brickred', name: 'socialauth', version: '4.15'
```

### Separate web.xml file for deployment

If you need a different web.xml file for version deployment to environments than for standard development, you can use the settings options [tasks of war](https://docs.gradle.org/current/userguide/war_plugin.html) v `build.gradle` where it is possible to attach a different `web.xml` File:

```
war {
    zip64 = true
    webXml = file('src/web-azure.xml')
}
```

## Changes to the database schema

New columns are added to several tables when version 2021 is enabled:
- `_properties_` - column added `update_date`, column `id` set as `autoincrement`
- `crontab` - added task\_name column
- `_conf_prepared_` - set column `date_prepared` for the possibility to insert `NULL` Value.

### MariaDB - utf8mb4 and InnoDB support

WebJET is used by default `storage engine InnoDB`, this is set in the configuration variable `mariaDbDefaultEngine`, which in WebJET 8 has the value `MyISAM` and in the new WebJET value `InnoDB`. When using `InnoDB` it is possible to use the charset `utf8mb4` with full support `uft8` coding (emoticons). By default `MyISAM` encoding is used in the tables `utf8`, which in MySQL/MariaDB is only 3 bytes and thus does not support all characters (different emoticons).

You can use the following SQL statements to create a new database schema and user:

```sql
CREATE DATABASE xxx_web DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
CREATE USER xxx_web IDENTIFIED BY 'gJ0gzNJSMwWIv4Fg';
GRANT ALL PRIVILEGES ON xxx_web.* TO `xxx_web`@`%`;
FLUSH PRIVILEGES;
```

## Rollback of changes

Changes to the database schema are backwards compatible with version 8.8, by default there is no need to revert schema changes. However, if you need to revert the changes, use the following SQL statements:

MySQL / Microsoft SQL:

```sql
ALTER TABLE _properties_ DROP COLUMN update_date;
ALTER TABLE _properties_ DROP COLUMN id;
DELETE FROM _db_ WHERE note='13.5.2020 [pgajdos] Pridanie stlpca update_date do _properties_ tabulky';

ALTER TABLE crontab DROP COLUMN task_name;
DELETE FROM _db_ WHERE note='08.07.2020 [pgajdos] Pridanie stlpca task_name do tabulky crontab, umiestnenie stlpca na druhe miesto';
DELETE FROM _db_ WHERE note='09.07.2020 [pgajdos] Zapisanie popisov cronjobov do stlpca task_name, pre tabulku crontab';


ALTER TABLE documents DROP COLUMN temp_field_a_docid;
ALTER TABLE documents DROP COLUMN temp_field_b_docid;
ALTER TABLE documents DROP COLUMN temp_field_c_docid;
ALTER TABLE documents DROP COLUMN temp_field_d_docid;
DELETE FROM _db_ WHERE note='14.12.2021 [sivan] Pridanie stlpcov temp_field_a_docid ... temp_field_d_docid do tabuľky documents';

ALTER TABLE documents_history DROP COLUMN temp_field_a_docid;
ALTER TABLE documents_history DROP COLUMN temp_field_b_docid;
ALTER TABLE documents_history DROP COLUMN temp_field_c_docid;
ALTER TABLE documents_history DROP COLUMN temp_field_d_docid;
DELETE FROM _db_ WHERE note='14.12.2021 [sivan] Pridanie stlpcov temp_field_a_docid ... temp_field_d_docid do tabuľky documents_history';

ALTER TABLE documents DROP COLUMN show_in_navbar;
ALTER TABLE documents DROP COLUMN show_in_sitemap;
ALTER TABLE documents DROP COLUMN logged_show_in_menu;
ALTER TABLE documents DROP COLUMN logged_show_in_navbar;
ALTER TABLE documents DROP COLUMN logged_show_in_sitemap;
DELETE FROM _db_ WHERE note='14.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_menu, logged_show_in_navbar, logged_show_in_sitemap) do tabulky documents';

ALTER TABLE documents_history DROP COLUMN show_in_navbar;
ALTER TABLE documents_history DROP COLUMN show_in_sitemap;
ALTER TABLE documents_history DROP COLUMN logged_show_in_menu;
ALTER TABLE documents_history DROP COLUMN logged_show_in_navbar;
ALTER TABLE documents_history DROP COLUMN logged_show_in_sitemap;
DELETE FROM _db_ WHERE note='18.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_menu, logged_show_in_navbar, logged_show_in_sitemap) do tabulky documents_history';

ALTER TABLE groups DROP COLUMN show_in_navbar;
ALTER TABLE groups DROP COLUMN show_in_sitemap;
ALTER TABLE groups DROP COLUMN logged_show_in_navbar;
ALTER TABLE groups DROP COLUMN logged_show_in_sitemap;
DELETE FROM _db_ WHERE note='28.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_navbar, logged_show_in_sitemap) do tabulky groups.';

ALTER TABLE groups_scheduler DROP COLUMN show_in_navbar;
ALTER TABLE groups_scheduler DROP COLUMN show_in_sitemap;
ALTER TABLE groups_scheduler DROP COLUMN logged_show_in_navbar;
ALTER TABLE groups_scheduler DROP COLUMN logged_show_in_sitemap;
DELETE FROM _db_ WHERE note='28.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_navbar, logged_show_in_sitemap) do tabulky groups_scheduler.';
```

Oracle:

```sql
DROP TRIGGER T_webjet_properties;
DROP SEQUENCE S_webjet_properties;
ALTER TABLE webjet_properties DROP COLUMN update_date;
ALTER TABLE webjet_properties DROP COLUMN id;
DELETE FROM webjet_db WHERE note='13.5.2020 [pgajdos] Pridanie stlpca update_date do _properties_ tabulky';

ALTER TABLE crontab DROP COLUMN task_name;
DELETE FROM webjet_db WHERE note='08.07.2020 [pgajdos] Pridanie stlpca task_name do tabulky crontab, umiestnenie stlpca na druhe miesto';
DELETE FROM webjet_db WHERE note='09.07.2020 [pgajdos] Zapisanie popisov cronjobov do stlpca task_name, pre tabulku crontab';
```
