# Prerequisites and versions

The current version of WebJET CMS requires `Java 17` and `Tomcat 11`.

The base project in gradle format can be found on [github webjetcms/basecms](https://github.com/webjetcms/basecms).

In gradle projects, just specify the version in build.gradle:

```gradle
ext {
    webjetVersion = "2026.0";
}
```

Currently, the following versions of WebJET exist:

- `2026.18.25-jakarta` - ​​stabilized version 2026.18 with fixes from version 2026.0.25, no daily changes added.
- `2026.0.25-jakarta` - ​​stabilized version 2025.0.25 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
- `2026.0.25` - ​​stabilized version 2025.0.25 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
- `2026.18-jakarta` - ​​stabilized version 2026.18, no daily changes are added to it.
- `2026.0.18-jakarta` - ​​stabilized version 2025.0.18 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
- `2026.0.18` - ​​stabilized version 2025.0.18 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
- `2026.0-jakarta-SNAPSHOT` - ​​regularly updated version from the main repository using `Jakarta namespace`. Requires Tomcat 11, available as [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2026.0-jakarta-SNAPSHOT)
- `2026.0-SNAPSHOT` - ​​updated version from `hotfix/2026.0` with fixes from version `2026.0` for Tomcat9/Java 17.
- `2026.0-jakarta` - ​​stabilized version 2026.0 for the Tomcat 11 application server using `Jakarta namespace`, no daily changes are added to it.
- `2026.0` - ​​stabilized version 2026.0, no daily changes are added to it.
- `2025.0-jakarta-SNAPSHOT` - ​​stable version 2025.52 using `Jakarta namespace`. Requires Tomcat 10/11, available as [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-jakarta-SNAPSHOT)
- `2025.0-SNAPSHOT` - ​​stabilized version 2025.52, available as [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-SNAPSHOT)
- `2025.0.52` - ​​stabilized version 2025.0.52 with bug fixes compared to version 2025.0 (without adding improvements from the SNAPSHOT version).
- `2025.0.50` - ​​stabilized version 2025.0.50 with bug fixes compared to version 2025.0 (without adding improvements from the SNAPSHOT version).
- `2025.40-jakarta` - ​​stabilized version 2025.40 for the Tomcat 10/11 application server using `Jakarta namespace`, no daily changes are added to it.
- `2025.40` - ​​stabilized version 2025.40, no daily changes are added to it.
- `2025.0.40` - ​​stabilized version 2025.0.40 with bug fixes compared to version 2025.0 (without adding improvements from the SNAPSHOT version).
- `2025.18` - ​​stabilized version 2025.18, no daily changes are added to it.
- `2025.0.23` - ​​stabilized version 2025.0.23 with bug fixes compared to version 2025.0 (without adding improvements from the SNAPSHOT version).
- `2025.0` - ​​stabilized version 2025.0, no daily changes are added to it.
- `2024.52` - ​​stabilized version 2024.52, no daily changes are added to it.
- `2024.0.52` - ​​stabilized version 2024.0.52 with bug fixes compared to version 2024.0 (without adding improvements from the SNAPSHOT version).
- `2024.0.47` - ​​stabilized version 2024.0.47 with bug fixes compared to version 2024.0 (without adding improvements from the SNAPSHOT version).
- `2024.40` - ​​stabilized version 2024.40, no daily changes are added to it.
- `2024.0-SNAPSHOT` - ​​regularly updated version from the master repository version 2024.0 compiled with Java version 17.
- `2024.18` - ​​stabilized version 2024.18, no daily changes are added to it.
- `2024.0.34` - ​​stabilized version 2024.0.34 with bug fixes compared to version 2024.0 (without adding improvements from the SNAPSHOT version).
- `2024.0.21` - ​​stabilized version 2024.0.21 with bug fixes compared to version 2024.0 (without adding improvements from the SNAPSHOT version).
- `2024.0.17` - ​​stabilized version 2024.0.17 with bug fixes compared to version 2024.0 (without adding improvements from the SNAPSHOT version).
- `2024.0.9` - ​​stabilized version 2024.0.9 with bug fixes compared to version 2024.0 (without adding improvements from the SNAPSHOT version).
- `2024.0` - ​​stabilized version 2024.0 (technically identical to 2023.52-java17), no daily changes added, compiled with Java version 17.
- `2023.52-java17` - ​​stabilized version 2023.52, no daily changes added, compiled with Java version 17.
- `2023.52` - ​​stabilized version 2023.52, no daily changes are added to it.
- `2023.40-SNAPSHOT-java17` - ​​regularly updated version from the master repository version 2023.40 compiled with Java version 17.
- `2023.40-SNAPSHOT` - ​​regularly updated version from the master repository version 2023.40.
- `2023.40` - ​​stabilized version 2023.40, no daily changes are added to it.
- `2023.18-SNAPSHOT-java17` - ​​regularly updated version from the master repository version 2023.18 compiled with Java version 17.
- `2023.18-SNAPSHOT` - ​​regularly updated version from the master repository version 2023.18.
- `2023.18` - ​​stabilized version 2023.18, no daily changes are added to it.
- `2023.0-SNAPSHOT` - ​​regularly updated version from the master repository version 2023.0, due to API changes this version ends before the release of version 2023.18 to avoid unexpected API changes in projects.
- `2023.0` - ​​stabilized version 2023.0, no daily changes are added to it.
- `2022.0-SNAPSHOT` - ​​regularly updated version from the master repository.
- `2022.52` - ​​stabilized version 2022.52, no daily changes are added to it.
- `2022.40` - ​​stabilized version 2022.40, no daily changes are added to it.
- `2022.18` - ​​stabilized version 2022.18, no daily changes are added to it.
- `2022.0` - ​​stabilized version 2022.0, no daily changes are added to it.
- `2021.0-SNAPSHOT` - ​​regularly updated version from the master repository.
- `2021.52` - ​​stabilized version 2021.52, no daily changes are added to it.
- `2021.40` - ​​stabilized version 2021.40, no daily changes are added to it.
- `2021.13` - ​​stabilized version 2021.13, no daily changes are added to it.

The following applies to version numbers:

- `YEAR.0.x` - ​​a correction version, **no new features**, during the year, errors found in WebJET CMS are corrected. Used libraries are updated only within the `minor` version, if necessary. If a library correction requires a change in the `major` version, it cannot be incorporated into this version, as this may carry the risk of changes in `API`.
- `YEAR.0-SNAPSHOT` - ​​development version which **contains new features** and bug fixes from version `YEAR.0.x`.
- `YEAR.WEEK` - ​​**stabilized version** from a given week that is created from the `SNAPSHOT` version after successful multiple testing. Fixes for other bugs will be incorporated into the next version, not a fix `YEAR.WEEK.x` but a new `YEAR.WEEK` with a new number will be created. In the event of an error in such a version, it is therefore necessary to count on a transition to the next stable version `YEAR.WEEK` or to the `YEAR.0-SNAPSHOT` version.

The `YEAR.0.x` version is therefore not fundamentally changed, it contains bug fixes (if the fix does not require a fundamental change). It is suitable for use by a customer who wants to have a stable version of WebJET without adding new features during the year.

At the same time, the `YEAR.0.x` version may not be the most secure. If it is necessary to update a library used in WebJET and it contains major changes, we cannot make this change in the `YEAR.0.x` version, because compatibility would be broken.

So `YEAR.0.x` is **the most stable** in terms of changes and `YEAR.0-SNAPSHOT` is **the most secure** in terms of vulnerabilities.

## Changes when migrating to Tomcat 9.0.104+

In [Tomcat since version 9.0.104](https://tomcat.apache.org/tomcat-9.0-doc/config/http.html) the parameter count check for `multipart` HTTP request has been changed. Therefore, it is necessary to set/increase the parameter `maxPartCount` on the `<Connector` element with the file `tomcat/conf/server.xml` to a value of at least 100, example:

```xml
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443"
               maxPartCount="1000"
               URIEncoding="UTF-8"
               useBodyEncodingForURI="true" relaxedQueryChars="^{}[]|&quot;"
    />
```

## Changes when switching to the Jakarta version

Version intended for `jakarta namespace`, requires Tomcat 11 application server, uses Spring version 7. Breaking changes:

- URLs - Spring has implemented exact matches for URLs, if a REST service defines a URL with a trailing slash, it must be used as such. There is a difference between URLs `/admin/rest/service` and `/admin/rest/service/`.
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

To search in the code, you can use the search in files `*Repository.java` and search for the regular expression `\(.*List[^)]*\)`, `\(.*Long\[\][^)]*\)`, `\(.*Integer\[\][^)]*\)`. We recommend executing the code, displaying an error in the log, and using the generated SQL to `Query` value. The only problem is the type checking, where `EclipseLink` cannot identify that it is supposed to check an array/list and not directly a data type.

In `build.gradle` you need to update the `gretty` configuration and add the `options.compilerArgs += ['-parameters']` compilation setting:

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

add an exception for `log4j-core` to the `configurations` element:

```gradle
configurations {
    all*.exclude group: 'org.slf4j', module: 'slf4j-log4j12'
    all*.exclude group: 'org.slf4j', module: 'jcl104-over-slf4j' //je nahradene novsim jcl-over-slf4j
    all*.exclude group: 'commons-logging', module: 'commons-logging'
    all*.exclude group: 'log4j', module: 'log4j'
    all*.exclude group: 'org.apache.logging.log4j', module: 'log4j-core'
```

## Changes when moving to 2025.0-SNAPSHOT

Version `2025.0-SNAPSHOT` is available via [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-SNAPSHOT), so you need to add the configuration to your `build.gradle` file:

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

Unfortunately, GitHub Packages are not publicly available, so you need to set the credentials `gpr.user` and `gpr.api-key` in the `gradle.properties` file or via the `ENV` variables. We will provide you with the credentials upon request.

!> **Warning:** modified Spring and JPA initialization:

- JPA entities are not initialized automatically in `package sk.iway.INSTALL-NAME`, a gradual transition to Spring DATA is expected. If you need to initialize `@Entity`, set the configuration variable `jpaAddPackages` to the required value - for example `sk.iway.INSTALL-NAME`. Only classes containing the annotation `@Entity` or `@Converter` are initialized.
- In `web.xml`, the initialization of `Apache Struts` is no longer needed, delete the entire `<servlet>` section containing `<servlet-class>org.apache.struts.action.ActionServlet</servlet-class>` and `<servlet-mapping>` containing `<servlet-name>action</servlet-name>`.
- Adjusted Spring initialization order - WebJET class initialization is performed before customer classes `SpringConfig`.
- Modified initialization `Swagger` - ​​if the configuration variable `swaggerEnabled` is not set to the value `true` or a Java class scan is not performed at startup.

## Changes when moving to 2024.0-SNAPSHOT

Similar to the [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) version, you need to add the `implementation("sk.iway:webjet:${webjetVersion}:libs")` section to the `dependencies` block:

```gradle
dependencies {
    implementation("sk.iway:webjet:${webjetVersion}")
    implementation("sk.iway:webjet:${webjetVersion}:admin")
    implementation("sk.iway:webjet:${webjetVersion}:components")
    implementation("sk.iway:webjet:${webjetVersion}:libs")
}
```

The following libraries have been removed, which are not used in the standard installation, if your project needs them, add them to your `build.gradle` file:

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

In the `configurations` section, delete the expressions:

```gradle
all*.exclude group: 'xml-apis', module: 'xml-apis'
all*.exclude group: 'javax.xml.stream', module: 'stax-api'
```

## Changes when switching to GitHub/Maven Central version

- In [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) the package name is changed from `sk.iway` to `com.webjetcms` and a `libs` part is added, which combines all the original `sk.iway` dependencies of type `struts,daisydiff,jtidy`. In `build.gradle`, edit:

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

## Changes when migrating to 2023.18-SNAPSHOT/2023.40

- In your project, delete the file `src/main/webapp/WEB-INF/struts-config.xml` to use the current file from WebJET (from the jar file).

## Changes when moving to Java 17

When upgrading to Java version 17, you need to make a few changes to your project. The [basecms](https://github.com/webjetcms/basecms/tree/release/webjet-2023-18-java17) project has `branch`, ```release/webjet-2023-18-java17``` ready with sample edits. A complete list of changes can be seen in [this commit](https://github.com/webjetcms/basecms/commit/e4b9cf6f0a88fd6f0b0cc6c57b28e7a3ec924535).

The simplified procedure is as follows:

Updating `gradle-wrapper` to version 8 (from the original 6), we recommend first updating to version 7 and then to 8 (upgrading directly to version 8 may result in an error):

```sh
gradlew.bat wrapper --gradle-version 7.4.2
gradlew.bat wrapper --gradle-version 8.1.1
```

After updating gradle, you will not be able to compile the project, you also need to edit the ```build.gradle``` file, in which several ```plugins``` are also updated. An important change is the transition of WebJET CMS in version ```java17``` to the standard version ```eclipselink``` with the WebJET primary key generator setting.

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

We then recommend restarting your development environment, or in the case of VS Code, performing the ```Java: Clean Java Language Server Workspace``` action to completely delete temporary files.

If you are running multiple WebJET installations on a single Tomcat server, it is possible that older versions will not be fully compatible with Java 17. For errors like:

```txt
[ERROR] ContextLoader - Context initialization failed <java.lang.IllegalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf>java.lang.Ill
egalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf
...
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make protected final java.lang.Class java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain) throws java.lang.ClassFormatError accessible: module java.base does not "opens java.lang" to unnamed module @4c6e3350
```

set the following for Tomcat ```JAVA_OPTS```:

```txt
JAVA_OPTS="$JAVA_OPTS --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"
```

### Updating from Java version 8

If you were running Tomcat with Java version 8, you may encounter problems with missing libraries (they are also required for Java 11). If you see the error ```java.lang.NoClassDefFoundError: javax/activation/DataSource``` in the log:

```txt
java.util.concurrent.ExecutionException: org.apache.catalina.LifecycleException: Failed to start component [StandardEngine[Catalina].StandardHost[...].StandardContext[]]
    ...
    Caused by: java.lang.NoClassDefFoundError: javax/activation/DataSource
```

It is necessary to copy the libraries from [this ZIP archive](lib-java11.zip) to each WebJET CMS installation into the ```WEB-INF/lib``` folder and delete the files (if they exist):

```txt
jaxb-api-2.1.jar
jaxb-runtime-3.0.0-M2.jar
```

If you were using WebJET version `8.0-8.6` - ​​older than `08/2019`, or you will see the following error at startup:

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

It is necessary to update the `eclipselink` library and the Java initialization class, download the update archive [jpa-wj82.zip](jpa-wj82.zip) and extract it in the root folder of the web application. Overwrite the `eclipselink.jar` and `WebJETJavaSECMPInitializer.class` files.

## Changes in the 2023.18 update

Version ```2023.18``` changes the API and the way distribution archives are generated. The main API changes are the use of generic objects of type ```List/Map``` instead of specific implementations ```ArrayList/Hashtable```. Therefore, you need to recompile your classes and modify your JSP files.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/sfu5b_S7Q8Q" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

In ```build.gradle``` it is necessary to delete the part:

```
    implementation("sk.iway:webjet:${webjetVersion}:struts")
    implementation("sk.iway:webjet:${webjetVersion}:daisydiff")
    implementation("sk.iway:webjet:${webjetVersion}:jtidy")
```

Also check the file `src/main/resources/logback.xml` where you need to adjust the date and time format in `ConsoleAppender` (deleted `,SSS`, if you need to log hundredths of a second, use `.SSS`):

```xml
    <pattern>[%X{installName}][%c{1}][%p][%X{userId}] %d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
```

To simplify the upgrade, you can use the ```/admin/update/update-2023-18.jsp``` script to check and fix JSP files. Custom Java classes need to be recompiled and errors fixed due to API changes.

Several Java classes and packages and the corresponding JSP files have been cleaned/deleted. To support the deleted parts in projects, it is necessary to use either the corresponding WebJET NET product or transfer them to the project from the version 8 source code.

More information can be found in the [changelog](../CHANGELOG-2023.md#removing-dependencies-on-version-8).

## Changes since version 8.8

In ```build.gradle```, compared to version 8.8, it is necessary to delete the following expressions:

```gradle
    compile("sk.iway:webjet:${webjetVersion}:swagger-ui")

    compile 'taglibs:standard:1.1.2'
    compile 'javax.servlet:jstl:1.2'

    providedCompile 'org.slf4j:slf4j-log4j12:1.7.25'

    exclude group: 'org.slf4j', module: 'log4j-over-slf4j'
```

and in the ```configurations``` section, edit the exceptions as follows:

```gradle
configurations {
    all*.exclude group: 'org.slf4j', module: 'slf4j-log4j12'
    all*.exclude group: 'org.slf4j', module: 'jcl104-over-slf4j' //je nahradene novsim jcl-over-slf4j
    all*.exclude group: 'commons-logging', module: 'commons-logging'
    all*.exclude group: 'log4j', module: 'log4j'
    all*.exclude group: 'org.apache.logging.log4j', module: 'log4j-core'

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

To move from ```log4j``` to ```logback```, delete the file ```src/main/resources/log4j.properties``` and add the file ```src/main/resources/logback.xml```:

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

If you need to modify the ```WAR``` archive for deployment, you can use the following tips:

**Another web.xml file**

If you need to deploy a different ```web.xml``` file than you use during development, you can use the options offered by the [gradle war](https://docs.gradle.org/current/userguide/war_plugin.html) task in ```build.gradle```:

```
war {
    ....
    webXml = file('src/someWeb.xml') // copies a file to WEB-INF/web.xml
}
```

**Modified logging**

WebJET writes log files using [slf4j/logback](https://logback.qos.ch/manual/configuration.html). This also allows you to search for an additional configuration file, e.g. ```logback-test.xml```, which will be used primarily (if it exists). In it you can have logging set up for development. In the file ```logback.xml``` you will have the settings for deployment. When creating the ```WAR``` archive, you omit the additional ```logback-test.xml``` in ```build.gradle```:

```
war {
    ....
    rootSpec.exclude('**/logback-*.xml')
}
```

## Additional project setup options

### Setting up social login

If you use social login (e.g. Facebook) in your project, you need to add the ```socialauth``` library to your gradle project. It is not included in the distribution by default because it is rarely used and contains a potential vulnerability. You add the library in the ```build.gradle``` file:

```gradle
// https://mvnrepository.com/artifact/org.brickred/socialauth
implementation group: 'org.brickred', name: 'socialauth', version: '4.15'
```

### Separate web.xml file for deployment

If you need a different web.xml file for deployment versions on environments than for standard development, you can use the [war task](https://docs.gradle.org/current/userguide/war_plugin.html) settings in ```build.gradle``` where you can attach a different ```web.xml``` file:

```
war {
    zip64 = true
    webXml = file('src/web-azure.xml')
}
```

## Changes to the database schema

When you enable version 2021, new columns are added to several tables:

- ```_properties_``` - ​​added column ```update_date```, column ```id``` set as ```autoincrement```
- ```crontab``` - ​​added task_name column
- ```_conf_prepared_``` - ​​set column ```date_prepared``` for the ability to insert ```NULL``` value.

### MariaDB - utf8mb4 and InnoDB support

WebJET uses ```storage engine InnoDB``` by default, this is set in the configuration variable ```mariaDbDefaultEngine```, which in WebJET 8 has the value ```MyISAM``` and in the new WebJET the value ```InnoDB```. When using ```InnoDB```, it is possible to use the charset ```utf8mb4``` with full support for ```uft8``` encoding (emoticons). By default, ```MyISAM``` tables use the encoding ```utf8```, which in MySQL/MariaDB is only 3 bytes and therefore does not support all characters (various emoticons).

To create a new database schema and user, you can use the following SQL commands:

```sql
CREATE DATABASE xxx_web DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
CREATE USER xxx_web IDENTIFIED BY 'gJ0gzNJSMwWIv4Fg';
GRANT ALL PRIVILEGES ON xxx_web.* TO `xxx_web`@`%`;
FLUSH PRIVILEGES;
```

## Rollback changes

Changes to the database schema are backward compatible with version 8.8, by default there is no need to revert the schema changes. However, if you need to revert the changes, use the following SQL commands:

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