# Build, transfer, runtime refresh, and validation

Resolve `<cms>` and `<cke>` from SKILL.md first. Commands below use `cms_root` and `cke_root`, derived from the active CMS checkout and its sibling `../libs-ckeditor4`. Keep these variables available in the shell when running later examples; initialize them again in a new shell.

## Preflight

Inspect `git status --short` in both repositories and read the current CMS build script. The script's relative path expects a sibling named `libs-ckeditor4`. Confirm it resolves to the source being edited. Check that `dev/builder/build-config.js` includes `wjstylescombo: 1` and still includes `stylescombo: 1`.

Keep plugin edits in `<cke>/plugins/wjstylescombo`. The CMS distribution is an output. Both source and distribution changes are needed when delivering a plugin modification; they belong to separate Git repositories.

### Preserve the pre-build state

The CMS transfer script **deletes and recreates its `dist` directory**, and removes obsolete CKEditor source directories under its working directory. It must not run from the CMS root or an arbitrary directory. Before rebuilding, preserve any existing local distribution edits that would be lost and inspect whether they belong in source.

A verified source/distribution drift affected these two CMS files during initial integration:

```text
src/main/webapp/admin/skins/webjet8/ckeditor/dist/plugins/webjet/wj_pixabay.jsp
src/main/webapp/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/component_preview.jsp
```

The CKEditor source copy was older than the CMS copy. Treat this as a known thing to **check**, not a permanent exception list. Compare pre-build content and post-build diffs. If the build introduces an unrelated regression, retain the exact pre-build content of that unrelated file. Do not blindly restore `HEAD`, discard the user's existing changes, or copy unrelated differences back into the source repository without assessing scope.

## Required CKEditor build and transfer

Start this block from any directory inside the active **CMS checkout**. The working directory for `build.sh` is essential because the script records `WJ_PATH=$PWD` and uses relative paths. If the sibling source directory is missing, resolve its location against the build script before proceeding; setting `cke_root` alone does not change the script's source path.

```bash
cms_root="$(git rev-parse --show-toplevel)"
cke_root="$(cd "$cms_root/../libs-ckeditor4" && pwd)"
test -f "$cms_root/src/main/webapp/admin/skins/webjet8/ckeditor/build.sh"
test -f "$cke_root/plugins/wjstylescombo/plugin.js"

cd "$cms_root/src/main/webapp/admin/skins/webjet8/ckeditor"
bash ./build.sh > /tmp/wj-ckeditor-build.log 2>&1
```

This invokes `<cke>/dev/builder/build.sh`, builds `release/ckeditor`, removes unneeded assets, replaces the CMS `dist`, and copies additional WebJET plugin assets. Running only the source builder does **not** complete transfer into the CMS.

Current build requirements:

- CKBuilder 2.4.3 uses an older compiler: plugin JS must retain ES5 syntax. Do not add arrow functions, `let`/`const`, optional chaining, or template literals to these plugin sources.
- CKBuilder needs JDK 15 or older. The CMS wrapper currently selects `$HOME/.sdkman/candidates/java/11.0.30-tem` directly. Verify that installation; do not change the machine's global/default JDK. The CMS Java runtime is separate.
- The wrapper sets `JAVA_TOOL_OPTIONS` to include `-Djava.awt.headless=true`, needed for sprite generation without initializing the macOS GUI toolkit.
- Direct SDKMAN shell initialization previously failed under macOS's old Bash (`${^^}` expansion). The wrapper intentionally selects `JAVA_HOME`/`PATH` without sourcing SDKMAN.
- The builder may check/download its jar from the official CKSource URL. It can use an existing cached jar after a failed update. A missing jar or unsupported compiler is a build failure, not a reason to manually patch `dist`.
- Both build scripts must propagate failures. Do not transfer a partial release after a compiler error or assume a log warning is harmless without checking the exit status.

Inspect the exit status and `/tmp/wj-ckeditor-build.log`. Expected outputs include:

```text
<cms>/src/main/webapp/admin/skins/webjet8/ckeditor/dist/ckeditor.js
<cms>/src/main/webapp/admin/skins/webjet8/ckeditor/dist/lang/*.js
<cms>/src/main/webapp/admin/skins/webjet8/ckeditor/dist/plugins/wjstylescombo/styles.css
```

Plugin JS and translations are bundled; a separate distribution `plugins/wjstylescombo/plugin.js` is not required. Many locale files change because the builder merges translations/fallbacks. Skin CSS may change its generated cache timestamp. Inspect these changes rather than hand editing generated assets.

## Other builds: choose by changed layer

| Changed layer | Needed action |
| --- | --- |
| CKEditor plugin JS, CSS, language files, builder config | Run the CKEditor build/transfer above, then reload/recreate editor instances |
| `<cms>/src/main/webapp/admin/v9/src` administration JS or styles | Run the administration build below; CKEditor build alone does not rebuild it |
| JSP integration/configuration | Normally reload/recreate the editor; check local JSP reload behavior |
| `CssParser`, `CssDto`, `EditorService`, `Constants` | Compile/test Java and reload the running application classes, restarting only the intended local checkout if needed |
| Template `editor.scss` | Compile template CSS while retaining comments; refresh style data by reopening/reloading the page editor |

Administration build:

```bash
cd "$cms_root/src/main/webapp/admin/v9"
npm run dev > /tmp/wj-admin-build.log 2>&1
```

For the Orange template, inspect its current `package.json` and `node_scripts/render-scss.js`. `npm run build:scss` compiles several template stylesheets. Use it when that scope is appropriate. The full `npm run build` also cleans/rebuilds unrelated output; it is not needed for a picker CSS metadata edit.

A narrow editor-only compile used in this checkout is:

```bash
cd "$cms_root/src/main/webapp/templates/orange/orange-web"
node - <<'NODE'
const fs = require('fs');
const sass = require('sass');
const output = sass.compile('src/scss/editor.scss', {
    style: 'expanded',
    loadPaths: ['node_modules']
});
fs.writeFileSync('dist/css/editor.css', output.css);
NODE
```

Reassess this narrow command if the template starts relying on additional pipeline transforms. Preserve editor comments in the non-minified CSS. CSS catalogue definitions alone do not guarantee that equivalent visual CSS is loaded in the published page or in the editor content iframe.

## Local server and browser refresh

Rebuilding assets does not update a CKEditor library already loaded in a browser. Reload the administration page or recreate its editor instances; in PB, the child iframe has its own CKEditor environment. Preserve unsaved user content before reloading. Diagnose stale assets before altering implementation again.

For Java changes, confirm which process belongs to this checkout and preserve its actual database/environment configuration. `app-stop.sh` is scoped using `-Dwebjet.worktreeId=<hash-of-checkout-path>`; other WebJET projects can be running simultaneously. Do not kill all Java/Gradle processes or stop a server merely because it uses a familiar port.

The verified local launch used the following sequence **after checking the active configuration**:

```bash
cd "$cms_root"
./app-stop.sh
webjetDbname=/poolman-local.xml ./gradlew appStartDebug
```

`/poolman-local.xml` is a local example, not a universal database choice. Starting the application may run its normal scheduled jobs; do not restart it for a CSS-only tweak. Keep the server running after verification when the user needs to inspect the result.

## Focused automated checks

Java parser tests:

```bash
cd "$cms_root"
./gradlew test --tests sk.iway.css.CssParserTest
```

Test sources are `src/test/java/sk/iway/css/CssParserTest.java` and `src/test/resources/css/editor-styles.css`. The initial five tests cover all 95 client definitions, optional metadata, compound selectors/title HTML, charset/legacy titles, and explicit definitions after media rules. The in-memory and file constructors share the fallback parser. No substitute database is required.

Browser regression suite:

```bash
cd "$cms_root/src/test/webapp"
CODECEPT_URL='http://iwcm.interway.sk' CODECEPT_SHOW=false npm run all -- tests/webpages/editor-styles.js --steps
```

The initial 11 scenarios use `fixtures/wjstylescombo.html` through `I.mockRoute`, load the **actual deployed CKEditor distribution**, and log in via `login('admin')` so protected assets do not redirect to the login page. They cover classic/inline composition, exclusion, toggling, partial text, mixed selections, objects, caret typing, one-step undo, external ancestor undo/redo, component reset, search, keyboard, modal focus, and viewport bounds. The modal regression also simulates the legacy floating-tools command. Tests use synthetic editor content and do not save a real article.

Run only relevant scenarios for a narrow iteration; run the focused file after shared engine/focus changes. Follow the repository's CodeceptJS skill when adding scenarios: standard `I.*` helpers, condition-based waits, English comments/assertions, and local development target. Respect required environment/tool permissions for browser launch and Gradle caches; a sandbox launch failure is not an application test result.

Useful quick checks:

```bash
node --check "$cke_root/plugins/wjstylescombo/plugin.js"
git -C "$cke_root" diff --check
git -C "$cms_root" -c core.whitespace=cr-at-eol diff --check
```

`CssParser.java` currently uses CRLF. Keep its existing line endings; use the whitespace setting above to avoid reporting every changed line as trailing whitespace. Do not normalize a whole file for an unrelated edit.

## Real integration and visual verification

The fixture is necessary but insufficient for host CSS, mode selectors, PB outlines, and other WebJET plugins. For UI/focus changes, also inspect a real local page at `/admin/v9/webpages/web-pages-list/` with representative content. The initial examples used docid 864; the user's current page may be different. Do not hardcode those article IDs into general regression tests.

- **Classic:** choose “Štandardný” in the Editor selector; content lives in `iframe.cke_wysiwyg_frame`. The toolbar and picker are in the parent administration document.
- **Page Builder:** content and shared toolbar live in `iframe#DTE_Field_data-pageBuilderIframe`. Select an editable element before opening styles. Do not assume the main document's `CKEDITOR.instances.data` is the active PB instance.
- Toolbar: `.cke_button__wjstyles`; panel: `.wj-styles`; search: `.wj-styles__search`; chips: `.wj-styles__chips`; rows: `.wj-styles__item`; target: `.wj-styles__target select`. Observe current IDs rather than relying on generated `cke_N` identifiers.
- Check widths around 1405, 1100, and 767 px and the available **iframe** height. Confirm header/search/chips stay usable, the list scrolls independently, and the mode selector/PB outlines do not cover controls. Restore temporary viewport overrides.
- Select an actual heading, combine size/weight/color as applicable, and check its tag stays unchanged. Switch targets and confirm summary/chips track the correct element. For an ancestor background, verify unrelated text classes survive.
- Verify Escape, outside click, keyboard traversal, search by class/category without accents, disabled incompatible choices, and one undo per applied operation. A count of 95 is a catalog baseline, not a hard-coded plugin limit.
- Capture the rendered result where helpful. Undo test edits or discard them without saving a real article. Confirm the final content state.

## Troubleshooting shortcuts

| Symptom | First checks |
| --- | --- |
| Changes absent in the CMS | Source checkout/build working directory, transferred bundle, browser/iframe cache, active feature flag |
| Old combo or no button | `ckeditor_styleComboEnabled`, exact `FCKConfig.ToolbarSets` item, `extraPlugins`, successful bundled build |
| Only “Other”, missing exclusion | Actual CSS comments retained, template CSS path, server JSON keys, `result.wj`, refreshed `stylesSet` |
| Panel closes after about 300 ms in classic editor | `webjetfloatingtools` command and priority-1 cancellation; inspect host focus behavior |
| Controls disappear behind PB borders or mode selector | `layer()`, toolbar owner document, ancestor z-index, modal stacking context |
| Header or list collapses | Explicit panel height, header flex sizing, list `min-height` and `flex-basis` |
| Ancestor undo fails | Root versus outer element, scope callback, snapshot marker/event priorities, class-delta revision, later edits from another instance |
| Partial selection changes surrounding text | Bookmark boundaries, span splitting/traversal, restored ranges, protected classes |
| Full catalog present but appearance absent | Template visual CSS loading, computed preview context, style class actually defined in runtime CSS |

Report actual checks and failures. Historical passing counts and old screenshots are context, not proof that a new change passed.
