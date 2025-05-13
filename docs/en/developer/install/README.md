# Installation and start-up

If you don't already have it, install [VS Code with recommended dependencies and extensions](https://docs.webjetcms.sk/v8/#/install-config/vscode/setup) - follow only the installation part of the extensions, the following chapters `Pull` project from SVN and the Tomcat setup no longer applies to you.

In the project we use [lombok](https://projectlombok.org), install the extension in your development environment - on the web page click on the menu item `Install` and follow the instructions in the IDEs section.

## Webpack build JS, CSS and PUG files

Files for administration are compiled via webpack from source `js/scss/pug` files. For the initial installation, start in a new terminal:

```shell
cd src/main/webapp/admin/v9
npm install
npm run prod
```

complete reinstallation is done with the command (for node `v17+` a parameter is also needed `--legacy-peer-deps`):

```shell
rm -rf node_modules
npm install
```

the latter will install the necessary libraries, license the `Datatables Editor` and compiles the production version.

You can then start **dev mode** whereby automatically **webpack tracks changes** v `js/scss/pug` files and builds `dist` Directory:

```shell
cd src/main/webapp/admin/v9
npm run watch
```

OR you can use `gradle task`:

```shell
#kontinualny watch zmien v suboroch a buildovanie dist adresara
gradlew npmwatch
#alebo jednorazovy build
gradlew npmbuild
```

**Production version** you generate via:

```shell
cd src/main/webapp/admin/v9
npm run prod
```

## Build Java classes and start Tomcat

Compilation of the project:

```shell
gradlew compileJava - kompilacia projektu
```

including refresh dependencies (WebJET from artifactory):

```shell
gradlew compileJava --refresh-dependencies --info
```

Start/stop Tomcat, create WAR archive:

!>**Warning:** before running gradle appRun, build the dist directory of HTML/CSS files once via gradle npmbuild, or run npm run watch from src/main/webapp/admin/v9 in a separate terminal.

```shell
gradlew appRun
gradlew appStop
gradlew war
```

View all JAR library dependencies:

```shell
gradlew -q dependencies --configuration runtimeClasspath
```

Gradle wrapper update

```shell
./gradlew wrapper --gradle-version 6.9.2
```

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/ZHb8714HXNY" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Setting up a hosts file

WebJET is licensed by domain. To work locally, you need to add a line to the hosts file (on windows it is c:\windows\system32\drivers\etc\hosts):

```
127.0.0.1   iwcm.interway.sk
```

!>**Warning:** at `Windows` you need to edit the file with admin rights.

WebJET will be available locally as http://iwcm.interway.sk/admin/ when launched.

## Testing

For testing is used [Playwright](https://github.com/microsoft/playwright/tree/master/docs) a [CodeceptJS](https://codecept.io/basics/).

Initial installation:

```shell
cd src/test/webapp/
npm install
```

Run all tests:

```shell
cd src/test/webapp/
npx codeceptjs run --steps
```
