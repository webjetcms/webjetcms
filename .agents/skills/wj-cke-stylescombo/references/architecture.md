# Architecture and editing guide

Paths prefixed `<cms>` and `<cke>` refer to the working copies resolved in SKILL.md. This is a verified implementation map from 2026-10-16, not a promise that future code remains identical.

## Source map

| Path | Responsibility |
| --- | --- |
| `<cke>/plugins/wjstylescombo/plugin.js` | Picker model, selection, class operations, toolbar command, panel DOM, summary, focus, positioning, undo integration |
| `<cke>/plugins/wjstylescombo/styles.css` | Toolbar button, fixed panel, header/chips, scrolling list, samples, keyboard focus, responsive rules |
| `<cke>/plugins/wjstylescombo/lang/{en,sk,cs}.js` | User-facing labels and element names; keep language keys aligned |
| `<cke>/plugins/wjstylescombo/README.md` | Developer overview; update when changing the plugin's contract |
| `<cke>/dev/builder/build-config.js` | Includes both `stylescombo` and `wjstylescombo` |
| `<cke>/plugins/{stylescombo,richcombo}/plugin.js` | Legacy reference; richcombo closes after selection, so configuration alone does not implement the new interaction |
| `<cke>/plugins/webjetfloatingtools/plugin.js` | Existing floating toolbar; delayed commands can steal focus from the picker |
| `<cms>/src/main/java/sk/iway/css/{CssParser,CssDto}.java` | Read CSS selectors, names, and optional metadata |
| `<cms>/src/main/java/sk/iway/iwcm/editor/service/EditorService.java` | `getCssListJson`, CSS path discovery, JSON keys |
| `<cms>/src/main/webapp/admin/v9/src/js/datatables-ckeditor.js` | `setStyleComboList`: translate server definitions into CKEditor styles and fire `stylesSet` |
| `<cms>/src/main/webapp/admin/skins/webjet8/ckeditor/config.jsp` | Plugin activation, toolbar replacement, `format_tags` |
| `<cms>/src/main/java/sk/iway/iwcm/Constants.java` | `ckeditor_styleComboEnabled`, `FCKConfig.ToolbarSets[...]`, other editor settings |
| `<cms>/src/main/webapp/admin/inline/inline_page_toolbar_v9.jsp` | Inline instances/shared toolbar, style population, explicit ancestor boundary callback |
| `<cms>/docs/sk/frontend/examples/template-bare/README.md` | Frontend documentation under “Vyhľadávanie a skupiny štýlov” |

When debugging integration, inspect the live configuration as well as these files. `ckeditor_styleComboEnabled` defaults to `true`; `false` retains the legacy `Styles` toolbar item. Current replacement is `'Styles'` → `'WJStyles'`, without automatically adding `Format`. The plugin requires `button,undo,format`, but loading `format` does not itself place its button in a configured toolbar.

## Data flow

1. Template source SCSS compiles to `editor.css` with editor comments retained.
2. `EditorService.getCssPath` resolves the page template's CSS and nearby `editor.css`. `getCssListJson` skips Bootstrap/Font Awesome, prefers an available non-minified sibling of `.min.css`, and uses custom-path resolution.
3. `CssParser`/`CssDto` produce `tag`, `class`, `title`, `category`, `group`, `targets`, `base`, `members`, and `requires`.
4. `DatatablesCkEditor.setStyleComboList` prepends seven semantic definitions (`p`, `h1`–`h6`), creates CSS style definitions, copies metadata into `result.wj`, updates the instance's definitions, and fires `stylesSet`.
5. `Picker.setStyles` keeps definitions with `attributes.class`; semantic-only definitions are not CSS choices. The initial Orange catalog therefore has **95 CSS choices**, not 102 CSS choices. Seven HTML formats are a separate capability.

Preserve `wj.tag`: the CMS adapter expands a wildcard selector into an `element` array for CKEditor, while the picker uses the original tag to decide applicability. Names fall back to the existing tag/class representation if `editor title:` is absent. `plain()` strips title HTML and decodes entities for labels, summaries, and search. Render labels with text, not template HTML inside the administration UI.

The parser is a limited selector parser, not a complete CSS AST. It handles simple tag/class combinations, compound classes, charset declarations, and an explicit editor-comment fallback after media/end markers. Preserve legacy title comments when adding metadata. Tests cover multiline metadata, media fallback, title HTML, and deduplication. Extend the parser deliberately if the task introduces combinators or other unsupported selectors.

## Metadata contract

Place comments **inside the CSS rule**, following the existing literal spelling:

```css
div.alert.warning {
    /* editor title: Warning */
    /* editor category: Alerts */
    /* editor group: Alert tone */
    /* editor base: alert */
    /* editor requires: alert */
    /* editor members: info success error */
}
.hero {
    /* editor title: Hero heading */
    /* editor category: Typography */
    /* editor group: Heading size */
    /* editor targets: h1 h2 h3 h4 h5 h6 */
}
```

| Key | Meaning |
| --- | --- |
| `title` | Display name; existing HTML titles remain valid input but become plain picker labels |
| `category` | Display/search organization only; missing category becomes localized “Other” |
| `group` | Mutually exclusive choices on the same applicable target; use names unique to the property, not merely to a category |
| `targets` | Optional whitespace/comma-separated tags narrowing the selector; a wildcard selector can target several block types |
| `base` | Structural classes preserved during toggle/reset; by default also required on the existing target |
| `requires` | Nonempty explicit required classes independent of `base`; current empty/missing value falls back to `base` |
| `members` | Additional literal class names belonging to the exclusive group; no wildcard/prefix expansion |

`changes = classes minus base`. Exclusion combines compatible definitions' changes and members, and protects structural classes. Reset removes known variants, preserving declared base and unknown classes. Do not clear `className` wholesale, infer exclusions from arbitrary prefixes, or assume every class in a composite selector is removable.

Class bundles without a group remain independent presets (for example Perex). Changing one of their properties can make a bundle no longer fully active. A base-only definition has no removable variant; do not mistake it for permission to delete component structure or create a new component.

For the Orange development template:

- Source: `<cms>/src/main/webapp/templates/orange/orange-web/src/scss/editor.scss`.
- Generated catalog: the same template's `dist/css/editor.css`.
- Reviewable test fixture: `<cms>/src/test/resources/css/editor-styles.css`.
- Initial categories: Typografia, Farba textu, Odsadenie, Pozadie, Sekcie, Zoznamy, Tabuľky, Tlačidlá a odkazy, Bannery, Upozornenia, Obrázky.
- Client files may be ignored by the CMS repository. Verify with `git check-ignore`; retain the catalog in the client source repository as appropriate. Keep the fixture aligned when intentionally changing the representative catalog.
- At initial integration, deployed CSS and source SCSS differed. Compare them before assuming a client rebuild is harmless; never reconstruct or overwrite source SCSS from CSS as a routine metadata edit.

## Picker model and class application

There is one `Picker` per CKEditor instance, exposed as `editor.wjstylescombo`, and one active panel per loaded CKEditor scope. Do not use a global `CKEDITOR.currentInstance` to choose the mutation target.

| Functions/state | Purpose |
| --- | --- |
| `capture`, `ranges`, `starts`, `contexts`, `selectedElement` | Snapshot selection and determine current elements; ranges are native DOM ranges converted from CKEditor ranges |
| `matches`, `targets`, `ancestors`, `scope` | Applicability and an explicitly chosen ancestor; ordinary text selection does not implicitly target a section |
| `state`, `active` | `true`/`false`/`mixed`; summary/chips deduplicate equivalent class sets |
| `conflicts`, `protectedClasses`, `changeElement` | Replace a group on its target, preserve unrelated and structural classes, and preserve the HTML tag |
| `changeText` | Partial text selection using bookmarks and span boundary splitting; collapsed caret delegates new inline formatting to native CKEditor style handling |
| `apply`, `restore`, `resetComponent` | Restore editor selection, apply/remove, snapshot once before/after, fire change, restore panel focus |
| `create`, `render`, `renderList`, `sample` | DOM controls, active/inherited styles, filtering, preview samples |
| `updateSummary`, `position`, `layer` | Closed summary measurement and panel placement |

A selected mixed choice applies to all eligible targets; an active choice toggles off. A chip explicitly removes its choice even when mixed. “Not set” clears an exclusive group. Selections containing unsupported element types do not authorize changing those elements.

For partial text changes, preserve text and unrelated markup outside the selection. During span splitting, retain the original outer ancestor before calling `splitElement`; traversing the newly split structure incorrectly previously caused a loop. Do not inject custom zero-width fillers for caret formatting. Verify that `getData()` contains no saved filler or picker markup.

Current ACF handling occurs when definitions enter `setStyles`: custom filters are checked, automatic filters receive allowed styles. Changes use class operations rather than blindly delegating every bundle to native `applyStyle`. If changing targets or filter behavior, test restricted ACF explicitly; the normal fixture uses `allowedContent: true` and does not establish complete restricted-filter coverage.

## Ancestors and undo

Normal scope ends at the editable root. Page Builder supplies `editor.config.wjstylescombo_scope = function() { return pageDiv[0]; };` from the inline toolbar integration. This trusted callback must return the relevant content container containing that editor, not the entire admin document. Ancestor controls use the localized element name and existing classes. Generic wildcard rules should not expose every wrapper as a useful ancestor.

Native undo snapshots capture editable content, but not necessarily root attributes or ancestors outside an inline editable. The picker therefore maintains `external` class deltas and a `revision`:

- `getSnapshot` at priority 20 appends `<!--wjstyles:N-->` by modifying **`event.data`**. Returning a string from that handler is insufficient.
- `loadSnapshot` at priority 1 removes the marker before CKEditor consumes the snapshot and replays root/ancestor class deltas.
- `getData()` must never expose the marker.
- Undo/redo compares the watched group classes before replay. If another inline editor changed that group, preserve the later change and emit `wjstylescomboUndoConflict` instead of overwriting it.
- History is per editor instance; do not claim globally shared undo across all Page Builder blocks.

## UI constraints and known integration traps

- The button is `WJStyles`, its command is `wjstylescombo`, and its DOM class is `.cke_button__wjstyles`. Opening uses `canUndo: false` and `editorFocus: false`.
- The panel uses `role="dialog"`, is registered with `editor.focusManager`, and lives in the **toolbar button's owner document**, inside its closest modal/dialog when present. Classic content is in a separate editable iframe; Page Builder's shared toolbar is inside the PB iframe.
- `webjetfloatingtools` schedules a `showFloatingTools` command roughly 300 ms after other commands, which can steal focus and close the picker. The picker cancels `*FloatingTools` commands while open in `beforeCommandExec` at priority 1, before command focus/snapshot behavior. Preserve or replace this protection deliberately; a minimal CKEditor fixture alone misses the original integration failure.
- Z-index must clear both editor mode controls and PB outlines. Current `layer()` starts at `max(baseFloatZIndex, 101100)`, inspects toolbar ancestors, then adds 100. Observed host layers include 101005 for the mode selector and 102000/102001 for PB toolbar/outlines. These are observed values, not immutable platform constants.
- Panel width is up to 480 px, with viewport margins. Height is explicitly bounded around 160–640 px and can flip above the button. Keep header/chips/search separate from the scrolling list. A flex list with only `max-height` on its parent previously collapsed the intended list space.
- Closed labels are comma-separated, retain whole names that fit, and append `+N`. Measure rendered width; do not use a fixed character count. Long first labels truncate. Full text remains in the hover title/focus tooltip and panel chips.
- Search ignores case/diacritics and matches label, selector, and category. Filter changes reset list scroll; active chips remain visible. “All styles” reveals disabled incompatible choices with target information.
- Labels stay uniform in size. `sample()` computes color/background/typography from a temporary `data-cke-temp` element in the content document, removes it, and caches the preview. It is not a full rendering of context-dependent components; test inherited/background colors if changing previews.
- Tab navigates controls, arrows/Home/End navigate choices, Escape closes and returns toolbar focus. Outside interaction closes. Guard selection capture while the user types in panel controls. Clean up listeners/panel on destroy; close on source/read-only transitions.
- UI CSS must stay scoped under `.wj-styles`/the toolbar button; client CSS may otherwise affect controls in inline mode. Keep WebJET typography, blue selection, visible focus, and localized SK/CS/EN labels.
