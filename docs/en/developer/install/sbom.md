# SBOM generation

SBOM (*Software Bill of Materials*) is a machine-readable list of software components and their dependencies. For WebJET CMS, you can generate it on demand from Gradle dependencies and npm lockfile administration. The output also includes transitive dependencies, i.e. libraries that use directly declared libraries.

The generator creates one common file in **CycloneDX 1.6 JSON** format. It serves as a basis for component registration and CRA analysis. The scope is limited to these two sources; before submitting the entire product to SBOM, the libraries listed below must be added outside of package management.

## Launch

You need a build environment for WebJET CMS: Java 17, Apache Ant, Node.js at least 20.18.0, npm at least 9, and Git. Ant runs the Gradle wrapper from the repository.

From the root of the repository, run:

```shell
ant -f ant/build.xml sbom
```

The version is automatically taken from the `artifact.version` property defined in the `define-artifact-properties` target in `ant/build.xml`. It is therefore the same as the version used when preparing distribution artifacts. It does not need to be entered or maintained in another place.

From the `ant` folder, just run `ant sbom`. On Windows, the target is `gradlew.bat`. Generation does not run CMS, update `build.properties`, compile, test, or build frontend files.

If you need a one-time different report label, use the Ant property:

```shell
ant -f ant/build.xml -Dartifact.version=2026.0 sbom
```

This value indicates a report, it does not switch Git branch or dependency versions.

On first run, access to dependency repositories, Gradle Plugin Portal and npm registry is required. The Java part uses repository accesses set in `build.gradle`, including `GPR_USER` and `GPR_API_KEY` or the corresponding Gradle properties for WebJET AI. The access data does not belong to SBOM.

The generator uses the [CycloneDX Gradle plugin](https://github.com/CycloneDX/cyclonedx-gradle-plugin) version `3.5.1` and the [CycloneDX npm](https://github.com/CycloneDX/cyclonedx-node-npm) version `6.0.1`. The Ant target loads the configuration `ant/sbom.init.gradle`, passes it the artifact version, and runs the Gradle task `sbom`. The Gradle plugin is only loaded when this init script is used. The npm tool is downloaded via `npm exec` to the npm cache; it does not change `package.json`, `package-lock.json`, or install admin dependencies.

If both the tools and dependencies are already in the local cache, you can use:

```shell
ant -f ant/build.xml -Dsbom.offline=true sbom
```

The option applies to both Gradle and npm tool acquisition. Missing cache contents will cause an error.

## Output

The resulting file to be saved to the repository is **`ant/sbom/webjetcms.cdx.json`**. Target `sbom` copies it from `build/reports/sbom/webjetcms.cdx.json` after successful generation and validation. It is also updated when `ant sbom` is manually run.

It contains both Java and npm components, with the administration package included as a WebJET CMS dependency. Transitive dependency relationships and metadata of both generators are also preserved.

In the `build/reports/sbom/` folder, there are auxiliary exports for checking the origin of the data:

| File | Contents |
| --- | --- |
| `webjetcms-java.cdx.json` | Resolved Java dependencies from `distributionRuntimeClasspath`, their versions, mutual bindings, checksums and available metadata. |
| `webjetcms-admin.cdx.json` | Administration dependencies from `src/main/webapp/admin/v9/package-lock.json` including transitive, optional and peer dependencies, without `devDependencies`. |

Both the common and Java reports use the WebJET CMS version from Ant property `artifact.version`. The npm report preserves the root package identity from `package.json` ; its `1.0.0` is the administration package version, not the CMS version. The CMS version is also listed in `metadata.properties` under the name `webjet:releaseVersion` in both npm and common reports.

The script checks the consistency of dependency declarations in `package.json` and the root part of the lockfile. If there is an error resolving Java or npm dependencies, it will end with an error. A successful conclusion of `BUILD SUCCESSFUL` means that both the auxiliary and the joint report have passed the CycloneDX schema validation and the check for uniqueness of identifiers and the existence of referenced components. The joint file will be replaced only after successful validation.

The common report has a value of `compositions` because the manifest inventory does not cover the entire distributed product.

The next run will overwrite the files. After a failed run, do not use the contents of the output folder as a new report: it may contain older or only partially restored output.

## Automatic update during Maven deployment

Target `deployGithub` first runs `prepareAllJars`, then `sbom`, and then publishes to GitHub Packages. SBOM is therefore generated after building Maven artifacts in the same working directory. Preparing update ZIP files via `createUpdateZip` does not trigger this step.

When publishing, the same report is attached as a Maven artifact with classifier `cyclonedx` and type `json`, i.e. `webjetcms-<verzia>-cyclonedx.json`. A generation, validation, copy or publishing error will cause the target `deployGithub` to fail.

The existing GitLab job `deploy-snapshot` can keep its calls to `ant deployGithub` and then `ant gitPush`. The target `gitPush` will include `ant/sbom/webjetcms.cdx.json` in the existing build commit along with `src/main/webapp/WEB-INF/build.properties`. Neither `ant sbom` nor `ant deployGithub` by themselves create a Git commit.

When `ant -Dcompress=true deployMavenCentral` is subsequently manually run, the existing `ant/sbom/webjetcms.cdx.json` from the previous `deployGithub` for the same version is used. The SBOM is not regenerated. After preparing the JAR files, it is copied between the artifacts and added to the publishing ZIP as `webjetcms-<verzia>-cyclonedx.json` along with the GPG signature and MD5 and SHA-1 checksums. If the source SBOM is missing or signing fails, publishing stops.

Each branch maintains a single file with the latest saved SBOM; older versions remain in the Git history. The branch `release/2026.18` will contain a report from the build saved in that branch. The specific release is identified by the corresponding commit or release tag. The report contains the build time and a new UUID, so it will change even if you build again with the same dependencies.

## Scope and limitations

- **Java distribution:** `distributionRuntimeClasspath` is used, so the report does not include development `webjet-ai-local`, separate test configurations, or build tools. The configuration also includes APIs provided by the container, such as the Servlet/JSP API. These are deducted by `war` during packaging via `providedRuntime`, so the report is not an exact list of files in `WEB-INF/lib`. A dependency declared in `implementation` will still be included, even if its name contains `test`.
- **npm before build:** the source is the lockfile, not the contents of `node_modules` or the finished JS/CSS files. The list may contain components that will not make it into the resulting bundle. Excluding `devDependencies` is based on project declarations; it does not check whether part of any development package made it into the distributed files.
- **Changes via postinstall:** `postinstall.sh` installs DataTables Editor from a local ZIP. Lockfile records npm package `datatables.net-editor`, whose version and checksum may not match the subsequently inserted licensed code. The latter should be recorded according to the actually delivered ZIP.
- **Manually included libraries:** CKEditor, older JS libraries in the administration, component libraries, and custom extensions outside of the listed manifests require separate inventory. Also, the contents of shaded JAR files, native libraries, and other included packages must be verified.
- **Local JAR files:** Libraries from `libs` that Gradle resolves as dependencies are also included with checksums. If they do not have a Maven POM, they may be missing vendor, license, or internal dependencies. The `metadata is unavailable` warning does not mean that the component was omitted from the report.
- **Deployment Environment:** Tomcat, JDK, operating system, database server, or container image content are not inventoried by these two sources.

SBOM is an inventory, not a vulnerability scan result or confirmation of CRA compliance. Availability of license data depends on the metadata of individual packages.

## Use for a specific release

Generate a report when building a release from the same checkout and with the same dependency resolution. `build.gradle` also contains dynamic versions, such as `7.0.+`, so a later run on the same commit may pick up a newer version of the library. The release number parameter alone will not restore the original resolution.

Archive the joint report and auxiliary exports together with the distribution package, its checksum, Git commit, and information about any local changes. For repeatable releases, it is also necessary to keep accurate resolved versions of dependencies, for example using Gradle dependency locking. Before marking the report as the SBOM of the entire distribution, complete the inventory of libraries outside of Gradle and npm and compare it with the supplied files.
