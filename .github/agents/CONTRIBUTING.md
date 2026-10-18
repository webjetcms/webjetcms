# WebJET CMS: Working Guide for Agents

[Index](AGENTS.md) · [Topic navigation](AGENTS-NAV.md)

These commands and paths describe the core checkout. For a customer project, inspect its build, scripts, database profiles and instructions first. Framework source or core test fixtures may be absent there.

## Start a task

1. Read the project's root instructions, inspect `git status` and the current branch, and preserve existing user changes.
2. Read the [knowledge index](AGENTS.md), then the relevant topic and available task-specific skill.
3. Locate the current implementation and its callers, configuration, translations and tests. Treat prose and old comments as guidance to verify against code.
4. Make the smallest complete change, run the relevant checks, and report the result and any verification gaps.

In WebJET core, agents must not create Git commits on any branch. Leave changes uncommitted for the user. Follow the existing feature/hotfix branch; create a separate branch only when needed for the task. Root `AGENTS.md` uses `feature/<id>-<description>` and `hotfix/<id>-<description>`. The repository's [contribution guide](../../CONTRIBUTING.md) describes PR targets: features target `main`, hotfixes target their corresponding `hotfix/YEAR.0-main` branch. Do not rebase, merge or publish as a routine documentation or coding step.

## Build and run the core

- Use Java 17 and the checked-in Gradle wrapper. Java compilation includes Lombok/MapStruct annotation processing and AspectJ post-compile weaving.
- Configure a local database profile under `src/main/resources/` using the existing Poolman examples. `poolman-local.xml` is local configuration, not a file to overwrite with shared credentials.
- Gradle resolves the AI artifacts from GitHub Packages; `GPR_USER` and `GPR_API_KEY` (or the corresponding Gradle properties) may be required. Keep credentials out of tracked files and output.
- Install the admin frontend dependencies in `src/main/webapp/admin/v9` when needed. Its `postinstall.sh` installs the bundled DataTables Editor ZIP and prepares vendor assets. Do not skip this lifecycle step or replace the licensed Editor package with a different build to work around setup failures.
- Build the administration assets before running the application. Keep `npm run watch` in a separate terminal when changing JS/SCSS/Pug.

Run from the core repository root unless a directory is specified:

| Purpose | Command |
| --- | --- |
| Compile Java | `./gradlew compileJava` |
| Install admin dependencies | `npm --prefix src/main/webapp/admin/v9 install` |
| Build admin production assets | `./gradlew npmbuild` or `npm --prefix src/main/webapp/admin/v9 run prod` |
| Watch frontend sources | `./gradlew npmwatch` or `npm --prefix src/main/webapp/admin/v9 run watch` |
| Start with a configured local DB | `webjetDbname=/poolman-local.xml ./gradlew appStartDebug` |
| Stop this checkout's server | `./gradlew appStop` |
| Package the standard client WAR | `./gradlew war` |

The startup example uses POSIX environment syntax. Windows task variants are in `.vscode/tasks.json`. Gretty defaults to HTTP 80, HTTPS 443 and debugger 5005; use `-PhttpPort=8080 -PhttpsPort=8443 -PdebugPort=5006` for another local instance. Match the URL and hostname to the chosen profile and local domain setup. See [installation](../../docs/sk/developer/install/README.md), but use current build files when old documentation examples disagree.

## Java tests

The Gradle `test` task uses JUnit Platform/Jupiter (JUnit 6), Spring Test and Mockito. Tests include isolated unit tests and tests requiring initialized WebJET services or a database; inspect the selected class before running it.

```sh
./gradlew test --tests 'sk.iway.iwcm.JsonToolsTest'
./gradlew test -DwebjetDbname=/poolman-local.xml --tests 'fully.qualified.TestClass'
```

The second command is a template: replace the class with the relevant existing test. Tests default to `/poolman-local.xml`; unlike Gretty startup, the Gradle test task reads the **system property** `webjetDbname`. Its language/country defaults are `sk`/`SK`.

- `src/test/java/sk/iway/iwcm/test/BaseWebjetTest.java` initializes constants, a mock servlet context and Stripes. It does not by itself create a full Spring/JPA application context. Reuse the appropriate neighboring test configuration.
- Test JPA persistence through the actual repository and database, including a reload where persistence behavior is under test. Do not substitute H2 or assume an in-memory entity proves a mapping works.
- `.github/workflows/gradle.yml` provisions MariaDB, imports `.github/workflows/blank_web_autotest.sql` and runs `./gradlew cleanTest test`. A local full run needs equivalent prerequisites; do not import the fixture into an existing database as an incidental test step.
- Reports are under `build/reports/tests/test/`. Compilation or an isolated unit test passing is not proof that database or browser behavior passed.

## Browser tests

`src/test/webapp` has a separate npm installation. Its `codecept.conf.js` configures CodeceptJS/Playwright, auto-login and page helpers such as `DT`, `DTE`, `DataTables`, `Document` and `Browser`. The configuration does not start the CMS server.

```sh
cd src/test/webapp
npm install
CODECEPT_URL='http://iwcm.interway.sk' CODECEPT_SHOW=false npx codeceptjs run tests/path/to/test.js --steps
```

Replace the test path and URL with the intended test file and running local/test instance. Verify the hostname resolves to that instance. Supply the test credentials/environment expected by the configuration; do not copy secrets into test files. `CODECEPT_BROWSER`, `CODECEPT_LNG` and `CODECEPT_RESTART` select browser, UI language and restart behavior. Without an explicit URL, the configuration uses `https://` plus `CODECEPT_DEFAULT_DOMAIN_NAME`; some npm scripts also specify their own target. Inspect the script before running it.

Use `wj-codeceptjs` for new scenarios and `wj-codeceptjs-check` for rerunning reported failures when installed. Existing tests and helpers take precedence over invented selectors, sleeps or a parallel browser harness. Results and Allure artifacts go to `build/test/`.

## Skills and knowledge validation

Changes to `.github/agents/` are part of the skills distribution. These checks need Node.js (the skills CI uses Node 22), not a running CMS or database:

```sh
node --test .github/scripts/skills.test.mjs
node .github/scripts/export-skills.mjs --output /tmp/webjet-skills-review
```

Choose a new output directory; the exporter refuses an existing destination. It reads working-tree contents of Git-tracked files, validates metadata and Markdown links, and produces `source.json` plus the distribution. Newly created files must be tracked to be included. No publishing is needed to validate the export.

The exporter rewrites relative links to bundled references and tracked core files. Keep links relative in the maintained source; use code-formatted core paths for locations that may not exist in customer checkouts. Do not edit generated reference copies.

To keep a skill only in the core repository, add its exact directory name to `.github/skills-distribution/excluded-skills.txt`, one name per line. Blank lines and lines starting with `#` are ignored; glob patterns and unknown skill names are rejected. The exporter omits the entire skill directory and its manifest entry. Required skills `wj-project-context` and `wj-setup` cannot be excluded. The existing workflow watches this file and removes previously published excluded skills during synchronization.

## Documentation and completion

- Update relevant product/developer documentation for behavior changes. Core Slovak documentation is in `docs/sk/`; follow the existing translation workflow for the other languages.
- For core PRs, follow `.github/copilot-instructions.md` and the root contribution guide for the current Slovak changelog and matching roadmap item. Do not invent a PR number. These release requirements do not apply automatically to customer work.
- For frontend work, edit the source under `admin/v9/src/` or `views/`, build it and inspect the relevant UI. Do not hand-edit generated bundles to fix source behavior.
- In core, `.gitignore` and the WAR configuration distinguish local/demo templates and components from distributed files. In customer projects, similarly named templates may be production code; do not classify them from their names alone.
- Summarize the concrete change, checks actually run and remaining limitations. Leave Git commits to the user.
