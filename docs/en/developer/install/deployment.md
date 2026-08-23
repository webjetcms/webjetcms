# Deployment of the build on the artifactory/maven server

Deployment of jar archives for use in client projects is based on preparing jar archives compatible with the original WebJET 8 structure.

## Before creating a build

Before creating a build, the following steps need to be manually performed/checked:

- prepare a description of the changes in the file ```docs/CHANGELOG.md```
- edit file ```docs/README.md``` - ​​add the latest version from the changelog to the top
- edit translation key ```admin.overview.changelog``` with the summary of changes of the current version displayed under the welcome text on the start screen
- edit ```src/main/webapp/admin/v9/json/wjnews.LANG.json``` - ​​add summary and link to changelog of latest version

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
- ```finalwar``` - ​​creates a structure ```build/updatezip/finalwar``` and an archive ```build/updatezip/webjetcms.war``` with applications packaged as JAR files
- ```createUpdateZip``` - ​​creates ```build/updatezip/artifacts/archive.zip``` for updating an old unpacked installation without JAR packaging
- ```createUpdateZipJar``` - ​​creates ```build/updatezip/artifacts/archive-jar.zip``` for installation using JAR packaging; runs after preparing artifacts
- ```prepareAllJars``` - ​​prepares all JAR files and ```POM``` file for publishing to repositories
- ```deployGithub``` - ​​prepares artifacts and publishes SNAPSHOT version to [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502/versions)
- ```deployMavenCentral``` - ​​after confirming the version, prepares artifacts and signatures, creates a publishing ZIP file and sends it to [Maven Central](https://repo1.maven.org/maven2/com/webjetcms/webjetcms/)

How to generate a new version:

```shell
# nezabudnite vypnúť bežiaci npm watch a Tomcat
cd ant

# iba lokálna príprava JAR a POM súborov
ant prepareAllJars

# publikovanie SNAPSHOT verzie do GitHub Packages
ant deployGithub

# publikovanie verzie do Maven Central
ant deployMavenCentral
```

The ```deployGithub``` and ```deployMavenCentral``` publish targets will automatically launch ```prepareAllJars```. Update ZIP archives can be created separately:

```shell
cd ant
ant createUpdateZip
ant prepareAllJars createUpdateZipJar
```

## Java and AspectJ compilation

Java source codes are compiled via Gradle during the ```setup``` task. The ```io.freefair.aspectj.post-compile-weaving``` plugin first lets ```javac``` and annotation processors like Lombok and MapStruct generate classes and then processes them using `AspectJ weaving` before creating the WAR archive. Separate compilation via Ant/AJC is no longer necessary.

## Use in client projects

In client projects, just set the appropriate version in build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}
```

We have experimentally verified the basic functionality on projects with MariaDB, Microsoft SQL and Oracle DB.
