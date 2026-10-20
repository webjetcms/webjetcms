# Deployment of the build on the artifactory/maven server

Deployment of jar archives for use in client projects is based on preparing jar archives compatible with the original WebJET 8 structure.

## Before creating a build

Before creating a build, the following steps need to be manually performed/checked:

- prepare a description of the changes in the file ```docs/CHANGELOG.md```
- edit file ```docs/README.md``` - ​​add the latest version from the changelog to the top
- edit translation key ```admin.overview.changelog``` with the summary of changes of the current version displayed under the welcome text on the start screen

If the version changes, update it in:

- `ant/build.xml`

from there it will also be transferred to `build.properties` to display the version in the administration.

## ANT task

[Build file](../../../../ant/build.xml) contains multiple targets. You can display their current list and description with the command ```ant -f ant/build.xml -p```. A separate target ```deploy``` does not exist; use one of the following targets depending on the output type or target repository:

- ```setup``` - ​​restores dependencies, compiles the project and generates ```WAR``` archive, JavaDoc and source files processed through Delombok
- ```expandwar``` - ​​runs ```setup``` and unpacks the generated ```WAR``` archive into the ```build/updatezip/WebContent``` directory
- ```define-artifact-properties``` - ​​defines properties for generating artifacts; the version of the generated artifact is set in the ```artifact.version``` property
- ```makejars``` - ​​prepares JAR archives with classes, source files, JavaDoc documentation and contents of directories ```/admin``` and ```/components```
- ```makepom``` - ​​generates ```POM``` Gradle file by task ```writePom``` based on dependencies defined in ```build.gradle```
- ```sbom``` - ​​generates [common CycloneDX SBOM](sbom.md) Java and npm dependencies with version from ```artifact.version``` and saves it to ```ant/sbom/webjetcms.cdx.json```, without building the distribution
- ```finalwar``` - ​​creates a structure ```build/updatezip/finalwar``` and an archive ```build/updatezip/webjetcms.war``` with applications packaged as JAR files
- ```createUpdateZip``` - ​​creates ```build/updatezip/artifacts/archive.zip``` for updating an old unpacked installation without JAR packaging
- ```createUpdateZipJar``` - ​​creates ```build/updatezip/artifacts/archive-jar.zip``` for installation using JAR packaging; runs after preparing artifacts
- ```prepareAllJars``` - ​​prepares all JAR files and ```POM``` file for publishing to repositories
- ```deployGithub``` - ​​prepares artifacts, generates SBOM and publishes SNAPSHOT version including SBOM with classifier ```cyclonedx``` to [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502/versions)
- ```deployMavenCentral``` - ​​after the version is confirmed, it prepares the artifacts, attaches the existing SBOM from ```ant/sbom/webjetcms.cdx.json``` and creates signatures. It sends the publishing ZIP file to [Maven Central](https://repo1.maven.org/maven2/com/webjetcms/webjetcms/). The SBOM is not regenerated; the report from the previous ```deployGithub``` for the same version is used.

How to generate a new version:

```shell
# nezabudnite vypnúť bežiaci npm watch a Tomcat
cd ant

# iba lokálna príprava JAR a POM súborov
ant prepareAllJars

# publikovanie SNAPSHOT verzie do GitHub Packages
ant deployGithub

# publikovanie rovnakej verzie do Maven Central s existujúcim SBOM
ant -Dcompress=true deployMavenCentral
```

The ```deployGithub``` and ```deployMavenCentral``` publish targets will automatically launch ```prepareAllJars```. Update ZIP archives can be created separately:

```shell
cd ant
ant createUpdateZip
ant prepareAllJars createUpdateZipJar
```

## Java and AspectJ compilation

Java source codes are compiled via Gradle during the ```setup``` task. The ```io.freefair.aspectj.post-compile-weaving``` plugin first lets ```javac``` and annotation processors like Lombok and MapStruct generate classes and then processes them using `AspectJ weaving` before creating the WAR archive. Separate compilation via Ant/AJC is no longer necessary.

## List of news on the docs server

The public files `wjnews.sk.json`, `wjnews.cs.json` and `wjnews.en.json` are created by the [central generator](social-media-feed.md) directly on the docs server. The target `rsyncToLicenseServer` only transfers the update archives to the license server; it does not copy any JSON files from `admin/v9/json/` to the docs server. Thus, the WebJET CMS deployment does not overwrite the generated news.

Run the generator via an automated task on the docs server. The public URL is preserved as `https://docs.webjetcms.sk/json/wjnews.sk.json`, for other languages ​​only the language code is changed to `cs` or `en`. The news of the installed version remains part of the translation key `admin.overview.changelog` and the dashboard combines them with this list.

The `sk.iway.aceintegration.socialmedia` package is deployed to the docs server in the `build/updatezip/webjetcms.war` archive created by the `finalwar` target, which supplements the classes from the `aceintegration` package. Custom JARs and update ZIP archives do not include this package due to existing filters in the Ant build. Generated JSON files, previews, and private cache are excluded from the package; they are created only when the generator is run on the server.

## Use in client projects

In client projects, just set the appropriate version in build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}
```

We have experimentally verified the basic functionality on projects with MariaDB, Microsoft SQL and Oracle DB.
