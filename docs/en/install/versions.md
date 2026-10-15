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

- `2026.0-boot-SNAPSHOT` - ​​development version with Spring Boot 4. Transitioning from `jakarta` version requires [customer project modifications](#changes-when-transitioning-to-spring-boot).
- `2026.18.28-jakarta` - ​​stabilized version 2026.18 with fixes from version 2026.0.28, no daily changes added.
- `2026.0.28-jakarta` - ​​stabilized version 2026.0.28 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
- `2026.0.28` - ​​stabilized version 2026.0.28 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
- `2026.18.25-jakarta` - ​​stabilized version 2026.18 with fixes from version 2026.0.25, no daily changes added.
- `2026.0.25-jakarta` - ​​stabilized version 2026.0.25 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
- `2026.0.25` - ​​stabilized version 2026.0.25 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
- `2026.18-jakarta` - ​​stabilized version 2026.18, no daily changes are added to it.
- `2026.0.18-jakarta` - ​​stabilized version 2026.0.18 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
- `2026.0.18` - ​​stabilized version 2026.0.18 with bug fixes compared to version 2026.0 (without adding improvements from the SNAPSHOT version).
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

## Changes when switching to Spring Boot

Moving from a standalone Spring Framework 7 configuration to Spring Boot 4 changes how you run your application, manage dependencies, and create WAR archives. It's not enough to just change `webjetVersion`. The steps are based on the [basecms migration pull request](https://github.com/webjetcms/basecms/pull/2/files) ; you can find the files to copy in the [release/webjet-2026-boot](https://github.com/webjetcms/basecms/tree/release/webjet-2026-boot) branch.

The prerequisite is a project that is already using the Jakarta version with Spring 7. Java remains at version 17 or later. The reference project uses Spring Boot `4.1.1`, Gradle `8.14` and Tomcat `11.0.25`. If you are migrating from an older version with `javax` packages, also [migrate to the Jakarta version](#changes-when-migrating-to-jakarta-version) first.

### Gradle and dependencies

Before editing `build.gradle`, update the Gradle wrapper to version `8.14`. In the root folder of your project, run:

```sh
./gradlew wrapper --gradle-version 8.14 --distribution-type bin
./gradlew wrapper --gradle-version 8.14 --distribution-type bin
./gradlew --version
```

The first run will set the desired version in `gradle-wrapper.properties`. The second will use Gradle `8.14` and will also update `gradle-wrapper.jar` and the `gradlew` and `gradlew.bat` startup scripts. This two-step procedure is also recommended by the [Gradle documentation](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:upgrading_wrapper). The last command will verify the version used. On Windows, replace `./gradlew` with `gradlew.bat`.

In `build.gradle`, remove the plugin `org.gretty`, the entire block `gretty { ... }`, and the configurations `grettyRunnerTomcat10` and `grettyRunnerTomcat11`, if the project contains them. Also remove the links to the original Gretty tasks, for example the JaCoCo configuration for `appStart`, `appStartDebug`, and `appAfterIntegrationTest`. Incorporate these settings into the existing blocks; keep the other project plugins and custom tasks:

```gradle
plugins {
    id 'java'
    id 'war'
    id 'org.springframework.boot' version '4.1.1'
    id 'io.freefair.lombok' version '8.14'
}

ext {
    webjetVersion = '2026.0-boot-SNAPSHOT'
    tomcatMinimumVersion = '11.0.25'
}

springBoot {
    mainClass = 'sk.iway.iwcm.system.spring.SpringBootStarter'
}

tasks.named('bootJar') { enabled = false }
tasks.named('jar') { enabled = false }
```

There is no need to add a custom startup class with `main()`. WebJET provides it in the library and for the JSP pages the application is still packaged in a WAR archive.

Remove the `springVersion` variable and the manual Spring dependency versions, which are replaced by Spring Boot version management. In `dependencies` add the Boot BOM, which unifies the library versions, and the dependencies for the built-in Tomcat and JSP:

```gradle
dependencies {
    implementation platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    providedCompile platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    providedRuntime platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)

    providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat-runtime'
    providedRuntime('org.apache.tomcat.embed:tomcat-embed-jasper') {
        exclude group: 'org.apache.tomcat', module: 'tomcat-annotations-api'
    }
    constraints {
        ['implementation', 'providedRuntime'].each { configurationName ->
            ['core', 'el', 'jasper', 'websocket'].each { moduleName ->
                add(configurationName, "org.apache.tomcat.embed:tomcat-embed-${moduleName}") {
                    version {
                        require("[${tomcatMinimumVersion},12.0.0)")
                        prefer(tomcatMinimumVersion)
                    }
                }
            }
        }
    }

    implementation("com.webjetcms:webjetcms:${webjetVersion}")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:admin")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:components")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:libs")

    providedCompile 'jakarta.servlet:jakarta.servlet-api'
    providedCompile 'jakarta.servlet.jsp:jakarta.servlet.jsp-api:4.0.0'
    providedCompile 'jakarta.el:jakarta.el-api:6.0.0'
    providedCompile 'jakarta.annotation:jakarta.annotation-api'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

Replace the original separate test dependencies on Spring, JUnit BOM, JUnit Jupiter, Hamcrest, Mockito, and the Jakarta EL test implementation with the `spring-boot-starter-test` and JUnit launcher listed above. Keep the custom libraries needed for customer tests. Also keep the compiler setting `options.compilerArgs += ['-parameters']`.

For both repositories, set up logins with read access to packages via `GPR_USER` and `GPR_API_KEY`, or `gpr.user` and `gpr.api-key` in local `gradle.properties`.

Also download the following parts from the [reference build.gradle](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/build.gradle):

- `bootRun`, calculation `worktreeId`, task `generateCertificate` and binding `bootRun.dependsOn generateCertificate`. They provide local execution, debugging on port `5005`, database connection selection and creation of development HTTPS certificate. In `jvmArgs`, adjust customer values, especially `webjet.smtpServer` and any other `webjet.*` settings.
- Common setup of WAR archives via `tasks.withType(org.gradle.api.tasks.bundling.War).configureEach { ... }` instead of the original `war { ... }` block. This way the packaging rules will be applied to both `war` and `bootWar`. Keep your own project rules and exclude local `poolman-*.xml` ; do not transfer the rules intended to exclude sample basecms files to customer files.
- Block `processResources` excluding `certificates/https-keystore.p12`. The development certificate is generated in `build/certificates` and does not belong to the distribution archive.
- Task `verifyBootWar`, which will create and check both WAR archives including WebJET libraries, logging configuration and library separation `Tomcat`.

Also, when packaging, change the exclusion `**/logback-*.xml` to `**/logback-local*.xml` as described below. If you are using tasks `prepareDataForPublicWar`, `explodeWar` or your own deployment scripts, modify them to work with the output of task `war`. In Gradle, you can get its path via:

```gradle
tasks.named('war').get().archiveFile.get().asFile
```

This avoids referring to the original archive name, which after migration belongs to the `bootWar` output.

### JpaDBConfig and SpringConfig classes

In the customer class `JpaDBConfig`, set your own, unique persistence unit name. Add a constant to the class, for example:

```java
private static final String PERSISTENCE_UNIT_NAME = "basecms";
```

In the `entityManagerFactory()` method, set it on the `LocalContainerEntityManagerFactoryBean` being created, after setting up the JPA adapter:

```java
emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
emf.setPersistenceUnitName(PERSISTENCE_UNIT_NAME);
```

Replace the value `basecms` with the name of your own project. If you have multiple JPA configurations, give each one a different persistence unit name. Keep your own packages in `@EnableJpaRepositories.basePackages` and `emf.setPackagesToScan(...)`. The names `entityManagerFactoryRef` and `transactionManagerRef` must match the corresponding annotations `@Bean` and be unique in the application. In the example, they are `basecmsEntityManager` and `basecmsTransactionManager`, the configuration itself is named `@Configuration("basecms:JpaDBConfig")`.

In the database platform selection, add the branch for PostgreSQL before the default branch for MySQL:

```java
} else if (Constants.DB_TYPE == Constants.DB_PGSQL) {
    properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.PostgreSQL);
} else {
    properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.MySQL);
}
```

Leave your `WebJETPersistenceProvider`, database connection, and EclipseLink settings as they are. A complete example can be found in [JpaDBConfig.java](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/src/main/java/sk/iway/basecms/JpaDBConfig.java).

In the `SpringConfig` class, add an explicit configuration name so that it does not match the WebJET CMS configuration. Instead of `@Configuration` itself, use, for example:

```java
@Configuration("basecmsSpringConfig")
@ComponentScan({
    "sk.iway.basecms",
    "sk.iway.basecms.contact"
})
public class SpringConfig {
}
```

Customize the bean name and scanned packages to your project. Keep existing customer methods and beans in the class.

### Application and web.xml configuration

Copy [application.properties](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/src/main/resources/application.properties) into `src/main/resources`. If you already have it, merge the settings. The file contains the configuration for the embedded Tomcat, JSP, encoding, error pages, and session storage. For local execution, check in particular:

| Setting | Meaning and adjustment in the project |
| --- | --- |
| `server.port=443` | HTTPS port of the embedded Tomcat. |
| `webjet.server.http-redirect.enabled=true`, `webjet.server.http-redirect.port=80` | Enable and port for the concurrent HTTP connector. Despite the name, this is not a blanket redirect to HTTPS; application redirects are still controlled by WebJET CMS settings, such as `adminRequireSSL`. |
| `server.ssl.key-store=${WEBJET_KEYSTORE_PATH}` | The path to the certificate; on `bootRun`, Gradle sets it to a file in `build/certificates`. |
| `server.ssl.key-store-password=${WEBJET_HTTPS_KEYSTORE_PASSWORD:changeit}` | The password for the development certificate. This variable is also used by the `generateCertificate` task. |
| `server.tomcat.max-part-count=1000` | Limit the number of parts in a multipart request, for example when submitting forms and files. |
| `server.servlet.session.store-dir=${user.dir}/work/sessions` | Folder to save sessions when the embedded Tomcat is shut down properly; each instance should have its own folder. |

The ports and TLS of the external Tomcat are still set in its `conf/server.xml`. Add `/work/` and `/logs/` to `.gitignore` so that the operational files are not stored in Git.

Remove `<dispatcher>ERROR</dispatcher>` from the `StripesFilter` mapping and keep `REQUEST`. Also add the MIME mapping of the extension `properties` to `text/plain`. Keep the other custom filters, servlets and mappings; specific changes are in the [web.xml of the reference project](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/src/main/webapp/WEB-INF/web.xml).

### Logging

Rename `src/main/resources/logback.xml` to `src/main/resources/logback-spring.xml`. The content is unchanged in the reference project, so leave your custom appenders, message format, and logging levels as they are. A name with a `-spring` suffix will allow Spring Boot to handle configuration loading; this is also recommended by the [Spring Boot documentation](https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.custom-log-configuration).

In the common WAR archive settings, replace the original rule `rootSpec.exclude('**/logback-*.xml')` with this:

```gradle
rootSpec.exclude('**/logback-local*.xml')
```

The original rule would also exclude the new `logback-spring.xml` and the deployed application would lose the customer logging configuration. If you have other local or test configurations, such as `logback-test.xml`, exclude them separately. The resulting WAR must contain `WEB-INF/classes/logback-spring.xml`.

### Starting and creating a distribution

Set up a local database connection in `src/main/resources/poolman-local.xml` and run the application:

```sh
./gradlew bootRun
```

The default connection file is `/poolman-local.xml` ; you can select a different one with the environment variable `webjetDbname`. By default, the server listens on HTTP port `80`, HTTPS port `443`, and debug port `5005`. You can stop it via `Ctrl+C` or with a script `app-stop.sh` that you copy to the root of your project. On macOS/Linux, keep the script execution permission. On Windows, use `gradlew.bat`.

To create and check the distribution, run:

```sh
./gradlew clean verifyBootWar
```

With the project name `basecms`, the following archives will be created in `build/libs`:

| Archive | Use |
| --- | --- |
| `basecms-plain.war` | Output of the `war` task, intended for deployment to an external Tomcat 11. Does not contain the built-in Tomcat libraries. |
| `basecms.war` | The output of the `bootWar` task, which has the built-in Tomcat libraries separated in `WEB-INF/lib-provided`. It can also be deployed to an external Tomcat. |
| `basecms-public.war` | Public part without administration, created separately with the command `./gradlew buildAllArtifacts` together with `basecms-plain.war`. |

The names are based on `rootProject.name` in `settings.gradle` ; if the project sets its own archive names or version, consider them in the deployment scripts. For normal deployment, use `-plain.war` and keep the existing application context, for example by deploying as `ROOT.war`. Set up the database connection as it exists on the server, as local `poolman-*.xml` are not packaged into the WAR.

!> Direct execution of a packaged WAR via `java -jar` is not supported in this version yet. WebJET CMS needs an unzipped web folder to work with files. Use `bootRun` for development and WAR deployed to an external Tomcat.

After migration, verify error-free startup in the log, administration login, display of JSP pages, customer REST services, database work, and file uploads. The `verifyBootWar` task checks the contents of the archives, it does not replace functional verification of the application.

### Optional files for VS Code and Docker

Download these files based on how you are developing and deploying your project:

- **VS Code:** merge `.vscode/tasks.json` and `.vscode/launch.json` from the reference project. Start tasks use `bootRun` instead of `appStartDebug`, stop on macOS/Linux uses `app-stop.sh`. Check the values ​​of `projectName` according to the Java project in VS Code and keep your own debug configurations.
- **Local Tomcat in Docker:** copy the entire folder `.devcontainer/tomcat/`. In `start-tomcat.sh`, edit `container_name`, `image_name` and `war_file`, which by default points to `build/libs/basecms-plain.war`. Also check `poolman_file` and the customer values ​​in `tomcat_jvm_args`, especially the SMTP server. Set the same container name in the `Docker Tomcat 11 Stop` task in `.vscode/tasks.json`. The script will build the archives and images, insert the database configuration and development certificate, and start Tomcat on ports `80` and `443`. If the database is running on the host machine, use the address `host.docker.internal` in `poolman-local.xml` instead of `localhost`.
- **Development databases:** depending on the database you are using, download the appropriate files from `.devcontainer/db/` and set `WEBJET_DB_PASS`. The procedure is in the [README](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/.devcontainer/db/README.md) there. You only need a separate `docker-compose-rag-pgsql.yml` if you are using a separate PostgreSQL database for RAG. These containers are not a prerequisite for migrating an existing database server.
- **Docker image application:** if you are using an existing solution from the `docker/` folder, download the changes in `docker/Dockerfile` and `docker/docker-compose.yml`. The Dockerfile uses Tomcat 11, the corresponding Redisson adapter, and unpacks `-plain.war` and `-public.war`. Set `ARTIFACT_NAME` to the Gradle project name in both parts of the Dockerfile or via `build.args.ARTIFACT_NAME` in Compose. Edit the image name and the values ​​from `docker/.env.example`, especially `PROJECT_NAME`, database data, ports, and timezone. When downloading this solution for the first time, copy the entire `docker/` folder, as the Dockerfile also uses its Tomcat and database connection configuration.

For an existing database, use a normal start. When installing to a new empty database, `Run SETUP` or `Docker Tomcat 11 SETUP Start` is ready in VS Code. They set the installation mode and require a token with a length of at least 16 characters. When starting manually, use the variables `WEBJET_SETUP_ENABLED=true` and `WEBJET_SETUP_TOKEN`. On `/wjerrorpages/setup/setup`, log in with the name `setup` and the token as the password. When finished, stop the application, turn off SETUP mode, and start it normally.

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