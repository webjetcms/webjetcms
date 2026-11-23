---
name: wj-stylescombo
description: "Add or regenerate WebJET wjstylescombo metadata comments in a template's editor.css or editor.scss, using the full CSS/SCSS to understand each style. Use for localized style names, categories, exclusive groups, and component constraints, either across the whole catalog or only where metadata is missing. Supports source SCSS and compiled DIST CSS in any combination. For plugin implementation or UI changes, use wj-cke-stylescombo."
---

# WebJET style catalog metadata

Annotate the selected style catalog for the CKEditor `wjstylescombo` picker. Determine what each style actually does from the template's complete stylesheets. The deliverable is metadata in the requested catalog and, when applicable, its source/generated counterpart. Keep CSS behavior and the catalog's selection of styles unchanged.

## Inputs and modes

Resolve these inputs from the request and available project context. Ask together for information that cannot be resolved; do not require the user to repeat supplied paths or preferences.

| Input | Resolution |
| --- | --- |
| Catalog to edit | A project path or file link to CSS or SCSS, commonly `editor.css` or `editor.scss`. Resolve relative paths from the project root. Ask if there is no identifiable target or several equally plausible targets. |
| Complete styles | One or more CSS files, SCSS entrypoints, or a stylesheet directory defining the actual appearance. This may be the catalog itself when it contains real declarations. If omitted, discover the matching template's styles as described below. |
| Language | Use the requested language for new titles, categories, and groups, e.g. Slovak, Czech, or English. Otherwise reuse the catalog's consistent language; if none is evident, ask before writing labels. Metadata keywords, HTML tags, and class names stay unchanged. |
| Mode | **Regenerate all** when explicitly requested; otherwise **fill missing**. State the resolved mode and language before editing. |

**Fill missing:** inspect every eligible entry, including partially annotated ones. Keep all existing metadata values and meaningful legacy titles, including their language and HTML. Add absent or empty fields only where justified. An absent optional `category-icon`, `group`, `targets`, `base`, `requires`, or `members` is not automatically a defect. Reuse existing group/category names for the same concept; changing just one group label can break mutual exclusion. If the requested language conflicts with existing names, report this and clarify any necessary migration rather than creating parallel translated groups. Report incorrect existing metadata without silently rewriting it. Repeating this mode with unchanged inputs should make no further edits.

**Regenerate all:** reassess and replace supported `editor ...:` metadata for all eligible catalog entries in the requested language, removing obsolete optional fields. Preserve selectors, declarations, rule order, unrelated comments, and deliberate presets. Existing titles and constraints are evidence of intent, but may be corrected when supported by the CSS or component usage. Do not invent a new catalog from every rule in the complete stylesheet. When replacing a legacy title, preserve its original comment and add an explicit `editor title:` override.

User-facing metadata values follow the requested language even when ordinary source-code comments use English. This does not authorize translating unrelated comments.

## Resolve source and complete styles

Read applicable repository instructions and inspect existing changes. Keep files uncommitted. The paths below are project-relative, not tied to a particular checkout name.

1. Identify the template root containing the supplied catalog. Inspect its `package.json`, relevant build scripts, stylesheet imports, source-map references, and output mapping. Search ignored client-template files when necessary, excluding dependency/build caches from broad searches. Include vendor styles only when imports or actual class usage make them relevant.
2. Keep the **catalog being edited** separate from the **styles used as evidence**. Accept all four combinations: CSS/CSS, CSS/SCSS, SCSS/CSS, and SCSS/SCSS. A source `editor.scss` with `dist/css/ninja.css` as evidence is valid; do not require matching extensions or a particular full-stylesheet filename.
3. If complete styles were omitted, locate the template's actual SCSS entrypoints and/or matching `dist`, `build`, or assets CSS. Follow imports and linked stylesheets. Prefer readable unminified CSS when it represents the same build. Do not substitute another template's CSS merely because it has similarly named classes. Ask for the full stylesheet if relevant definitions remain unavailable.
4. For SCSS evidence, resolve nesting, `&`, `@use`, `@forward`, `@import`, variables, mixins, maps/loops, interpolation, and `@extend` as needed. Use existing tooling to compile to a temporary output when static reading is insufficient. For compiled evidence, follow custom properties, media queries, and relevant imported styles. Inspect both source and output if they disagree; do not assume that DIST is current.
5. When the catalog is SCSS, annotate the source rule or generating construct and identify the CSS output WebJET reads. When the supplied CSS is a confirmed generated file with available source, annotate the owning source and regenerate the requested CSS, explaining the resolved paths. Honor an explicit CSS-only instruction. Hand-maintained CSS, or supplied CSS with no available source, can be edited directly; report regeneration risk only when it is actually generated. Do not reconstruct SCSS from DIST.

For the Orange example, the usual catalog is `src/main/webapp/templates/orange/orange-web/src/scss/editor.scss`, its output is `dist/css/editor.css` under the same template root, and full styles are in `src/scss/ninja.scss` / `dist/css/ninja.css`. Verify this mapping from the current build scripts; it is an example, not a requirement for other templates.

## Analyze before assigning metadata

Read [metadata.md](references/metadata.md) before editing. It describes the parser's accepted syntax and the behavior of each field. For an unfamiliar checkout, verify the contract in `src/main/java/sk/iway/css/CssParser.java`, `CssDto.java`, and, if needed, `../libs-ckeditor4/plugins/wjstylescombo/plugin.js`. This task does not require modifying or rebuilding CKEditor.

For each eligible catalog selector, establish:

- Its tag, complete class bundle, existing metadata, and effective appearance. Look up every constituent class: a preset such as `p.h4.bold.mb-large` may have no exact compound rule in the full CSS.
- The declarations and conditions responsible for that appearance, including specificity, order, `!important`, inherited values, custom properties, breakpoints, and state selectors. Names such as `primary`, `large`, or `normal` are clues, not proof of color, size, or weight.
- Whether it changes a single property, a component variant, or several independent properties. Where CSS alone cannot establish required structure, inspect the relevant template/PageBuilder markup or component documentation. Distinguish classes on the styled element from classes on its ancestors.
- Which classes are structural prerequisites and which are removable variants. Check siblings and existing metadata before assigning constraints or exclusivity.

Use concise labels that describe the visible result. For example, a class that changes `margin-bottom` belongs to spacing, not typography merely because it is applied to a heading. A heading appearance class does not change the HTML heading level. Avoid claiming a fixed color or pixel size when themes or breakpoints change it.

Choose a compact, consistent category vocabulary suited to this template and language. Possible concepts include Typography, Text color, Spacing, Backgrounds, Lists, Tables, Buttons and links, Alerts, and Images; do not create unused categories or copy Orange-specific mappings into another template. Categorize a multi-property component variant by the component's purpose.

Use the optional `/* editor category-icon: ti-border-none */` comment when a specific category icon is requested or the automatic icon does not convey its meaning. Otherwise keep automatic selection from the category name. Inspect the whole category before adding it: the first valid explicit icon in compiled CSS order wins, even if its style is filtered out. Prefer one declaration per category. In fill-missing mode, preserve the existing category-wide choice; do not add an earlier declaration that overrides it. See [metadata.md](references/metadata.md) for accepted icon syntax.

Assign a group only to alternatives that should replace one another on the same element. Keep independent dimensions apart: font size versus weight, text versus background color, margin versus padding, top versus bottom spacing, and base versus breakpoint-specific spacing. Responsive utilities that intentionally coexist must not clear each other. Sharing one property does not necessarily make two multi-property presets interchangeable; independent bundles normally remain without a group.

If behavior cannot be established, continue with supported entries and leave the uncertain metadata unchanged. Ask a targeted question or report the exact selectors and missing evidence. Do not guess destructive `base`, `requires`, or `members` values to complete the catalog.

## Edit and verify

- Add one block comment per supported field inside the owning rule, using the exact `/* editor key: value */` spelling. Preserve meaningful legacy title comments and their position in fill-missing mode. Add an explicit title when there is no existing meaningful label; a new explicit title copied from a legacy title must retain its value.
- Keep all selectors, declarations, at-rules, unrelated comments, and source constructs intact. Do not broaden selectors, add new catalog entries, import full styles into a definitions-only catalog, or change plugin/parser code as part of annotation. Flag unsupported selectors separately.
- For generated/nested SCSS, verify metadata on the compiled selectors, not just the source text. A comment in a parent block must not accidentally label all descendants or every loop-generated variant identically.
- Inspect the template build before running it. Prefer a narrow catalog compilation using the installed toolchain and its existing import paths/postprocessing. Avoid full clean builds that rewrite unrelated output. Preserve literal `/* editor ... */` comments and otherwise empty catalog rules in the CSS WebJET reads. `//` SCSS comments disappear; `/*! ... */` is not a compatible substitute for this parser.
- Before replacing existing DIST output, check for source/output drift, preferably by compiling the unmodified source to a temporary file. Compare ordered selectors, declarations, and at-rule context independently of metadata/formatting. If regeneration would overwrite unrelated changes, preserve that output and report the mismatch; do not overwrite it as a metadata update. If compilation is unavailable, keep the source edits and clearly state that generated CSS remains pending.
- Review the final diff against the pre-edit state. Validate that changes are limited to intended metadata and expected generated output. Check localized labels, category-icon validity and precedence in compiled CSS order, exact reuse of groups, compatible targets, preserved structural classes, and literal exclusion members across the whole catalog. Check that fill-missing did not change existing values or introduce duplicates.
- Where the existing parser/test harness is available, parse the resulting CSS with the actual `CssParser` and inspect the selectors and metadata, including rules after media/end markers. Otherwise inspect the accepted selector subset and emitted comments, and report that runtime parsing was not run. A source-only inspection does not prove compiled comments survived. This metadata task does not require broad application tests or saving a page in the CMS.

Report changed source/output paths, analyzed full styles, language/mode, counts of updated and unresolved entries, and build/parser checks actually performed. Mention ignored client files when relevant to delivery. Do not claim the catalog is ready in the running editor if CSS generation or parsing remains unverified.
