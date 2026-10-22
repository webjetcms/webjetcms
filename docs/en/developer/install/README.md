# Installation and launch

If you don't have it yet, install [VS Code with recommended dependencies and extensions](https://docs.webjetcms.sk/v8/#/install-config/vscode/setup) - follow only the extension installation part, subsequent chapters ```Pull``` project from SVN and Tomcat setup no longer apply to you.

We use [lombok](https://projectlombok.org) in the project, install the extension into your development environment - on the website, click on the menu item ```Install``` and in the IDEs section, follow the instructions.

## Setting up the Codex AI assistant

When developing with Codex, install the [Matt Pocock skillset](https://github.com/mattpocock/skills). It includes practices for preparing issues, diagnosing bugs, reviewing code, and working with GitHub Issues. Use it in conjunction with the `wj-*` project skills; existing WebJET CMS rules in `AGENTS.md` take precedence, including disallowing agents from creating commits.

### Installing skills

You need Node.js and npm, which is provided by the `npx` command. In the terminal, run:

```shell
npx skills@latest add mattpocock/skills
```

In the installer, select:

1. Required skills including `setup-matt-pocock-skills`. For the label settings below, also include `triage`.
2. Target agent **Codex**.
3. **Installation scope: Global** — skills will be available in all your projects without adding copies of them to the repository.
4. **Installation method: Symlink** — selected agents will use links to install skills together.

If you get the `zsh: command not found: codex` error when trying to install via `codex plugin marketplace add`, use the `npx` command above. You do not need the `codex` command in the terminal for this installation method.

### Repository setup

A global installation makes the skills available to the user. Their setup for a specific repository is done once and shared via files in Git. In this repository, it is already stored in `AGENTS.md` and `docs/agents/` ; once it is obtained, there is no need to repeat the setup on each computer.

For initial setup or configuration changes, open the project in Codex and in **chat, not terminal**, type:

```text
Použi skill setup-matt-pocock-skills pre tento repozitár.
Zachovaj existujúce WebJET pravidlá a zákaz vytvárania commitov.
```

For WebJET CMS, we chose:

1. **GitHub Issues — `webjetcms/webjetcms`** as a place to track issues.
2. Default triage labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human` and `wontfix`.
3. One common vocabulary of terms `GLOSSARY.md` at the root of the project and architectural decisions in `docs/adr/`. These documents are created only when there is a specific need.

Setup first submits a proposal for approval. After confirmation, it adds a section to the existing `AGENTS.md` (or `CLAUDE.md`, if the project uses it), creates the `docs/agents/issue-tracker.md`, `docs/agents/triage-labels.md` and `docs/agents/domain.md` configuration, and adds the missing GitHub tags. To work with GitHub, it needs a logged in `gh` tool and permission to edit repository tags. It leaves the file changes without committing for review by the developer.

Once set up, continue with the [Using AI skills](../guidelines/ai-skills.md) guide, which describes the process from a request or GitHub issue to implementation and review.

## rspack build JS, CSS and PUG files

The administration files are compiled via rspack from the source ```js/scss/pug``` files. For the initial installation, run in a new terminal:

```shell
cd src/main/webapp/admin/v9
npm install
npm run prod
```

To perform a complete reinstallation, use the command (for node ```v17+```, the parameter ```--legacy-peer-deps``` is also required):

```shell
rm -rf node_modules
npm install
```

it will install the necessary libraries, license for `Datatables Editor` and build the production version.

You can then start **dev mode**, in which **rspack automatically tracks changes** in ```js/scss/pug``` files and builds the ```dist``` directory:

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

**You can generate the **production version** via:

```shell
cd src/main/webapp/admin/v9
npm run prod
```

## Build Java classes and run Tomcat

Project compilation:

```shell
gradlew compileJava - kompilacia projektu
```

including restoring dependencies (WebJET from artifactory):

```shell
gradlew compileJava --refresh-dependencies --info
```

Starting/stopping Tomcat, creating a WAR archive:

!>**Warning:** before running gradle appRun, build the dist directory of HTML/CSS files once via the gradle command `npmbuild`, or run the npm run watch command from the src/main/webapp/admin/v9 directory in a separate terminal.

```shell
gradlew appRun
gradlew appStop
gradlew war
```

To view all dependencies of JAR libraries:

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

### Concurrent execution in multiple worktrees

In VS Code, you can select a separate launch profile for each worktree:

| Profile | HTTPS port | HTTP port | Debug port |
| --- | --- | --- | --- |
| `Debug Local DB` | 443 | 80 | 5005 |
| `Debug Local DB 8443` | 8443 | 8080 | 5006 |
| `Debug Local DB 9443` | 9443 | 9080 | 5007 |

In each worktree, start one server and select a different profile. For profile `Debug Local DB 8443`, open the administration at `https://localhost:8443/admin/`.

You can also set the ports via Gradle parameters `-PhttpsPort`, `-PhttpPort` and `-PdebugPort`. Without parameters, the default values ​​are 443, 80 and 5005. For example:

```shell
webjetDbname=/poolman-local.xml ./gradlew appStartDebug -PhttpsPort=8443 -PhttpPort=8080 -PdebugPort=5006
```

On macOS, the VS Code task `appStop` uses the script `./app-stop.sh`. It first calls `./gradlew appStop`, which uses the control port stored in the `build` directory of the given worktree. After successfully sending the request, it waits up to 15 seconds for the server to terminate. If Gradle fails or the server is left running, the script sends the signal `TERM` only to the Java process identified by the identifier of this worktree. If it does not terminate within another 10 seconds, it uses `KILL`.

The task `appKill` (`./app-stop.sh --force`) will directly use `KILL` for the server of the given worktree. The JVM parameter added at startup via the current `build.gradle` is used to identify the process; other worktrees are not stopped. There is no need to re-enter the ports when stopping.

## Setting up the hosts file

WebJET is licensed by domain. For local operation, you need to add the following line to the hosts file (on Windows it is c:\windows\system32\drivers\etc\hosts):

```txt
127.0.0.1   iwcm.interway.sk
```

!>**Warning:** at `Windows` you need to edit the file with admin rights.

After launch, WebJET will be available locally as http://iwcm.interway.sk/admin/.

## Testing

[Playwright](https://github.com/microsoft/playwright/tree/master/docs) and [CodeceptJS](https://codecept.io/basics/) are used for testing.

Initial installation:

```shell
cd src/test/webapp/
npm install
```

Running all tests:

```shell
cd src/test/webapp/
npx codeceptjs run --steps
```
