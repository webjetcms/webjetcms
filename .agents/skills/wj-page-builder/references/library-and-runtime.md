# Block Library and Template Integration

Sources: `docs/sk/frontend/page-builder/blocks.md` and `settings.md`.

## Location and Fragment Type

The documented web path is
`/templates/INSTALL_NAME/skupina_sablon/menosablony/pagebuilder`.
Resolve the actual template library from the repository; do not create literal
placeholder directories. In this repository public template assets live under
`src/main/webapp/templates/`.

Each block type requires a category subdirectory, for example
`pagebuilder/section/Features/pricing.html`. Do not put HTML directly in the type
directory.

| Directory | Expected fragment |
| --- | --- |
| `section/<category>/` | Complete section with its containers, rows, and columns |
| `container/<category>/` | Container and its content; no surrounding section |
| `column/<category>/` | Column and its content; no surrounding section/container/row |
| `content/<category>/` | Text/button/content fragment inserted with the Blocks icon and yellow insertion line |

Library files are inserted as HTML copies. There is no native inclusion of one
library file into another, and changing a file does not update existing inserted
copies. If a section and a standalone container share markup, reuse the template's
existing Pug build when available; the emitted HTML must remain self-contained.
Do not introduce a build system merely to deduplicate one block.

## Picker Metadata and Previews

A UTF-8 `pagebuilder.properties` file in the category can set:

```properties
title=Features
icon=fa fa-cubes
tags=Features, Pricing
title.pricing=Pricing plans
```

The `title.<basename>` key identifies the HTML block without its extension.
Language-specific files such as `pagebuilder_en.properties` are supported.
Preserve existing category metadata. For Pug templates, check that
`build-pug.js` copies PB `.properties` files alongside generated blocks.

A same-basename preview such as `pricing.jpg` appears in the picker when present.
Use a screenshot of the actual styled block or follow the template's existing
preview workflow. The documented legacy generator is
`/components/grideditor/phantom/generator.jsp`; it requires PhantomJS and
`grideditorPhantomjsPath`. Do not assume it is installed or configure it merely
to deliver an HTML fragment. If no preview was generated, say so.

## Images

Use `fixedSize-w-h-ip[-color][-true]` when the image's dimensions/crop must survive
replacement. Supply width and height in pixels and an interest-point mode 1–5.
Optional background color is hex without `#` or a `c` prefix; trailing `true`
disables the interest point. Examples:

- `fixedSize-160-160-5`
- `fixedSize-700-400-3-ff0000`
- `fixedSize-700-400-1-true`
- `fixedSize-700-400-3-ff0000-true`

Keep the initial image URL/thumbnail parameters consistent with the class, and
provide appropriate `alt` text. These classes control editor image replacement;
they are not generic CSS sizing utilities. Clicking images with `fixedSize-*`,
`w-100`, or `autoimg` opens their image properties directly. A URL containing
`placeholder` or `stock` opens the page's Media folder during image selection.

## Limited Template Evaluation and IDs

Block insertion does not execute arbitrary Thymeleaf. Supported attributes are
`data-iwcm-write`, `data-iwcm-remove`, `data-th-src`, and `data-th-href`.
Supported Ninja substitutions are `${ninja.temp.basePath}`,
`${ninja.temp.basePathAssets}`, `${ninja.temp.basePathCss}`,
`${ninja.temp.basePathJs}`, `${ninja.temp.basePathPlugins}`, and
`${ninja.temp.basePathImg}`. For example:

```html
<img src="./assets/images/logo.png"
     data-th-src="${ninja.temp.basePathImg}logo.png" alt="Company logo">
```

Do not rely on `th:each`, arbitrary expressions, or server-side fragment includes
to create the inserted block. Preserve supported application directives when
refactoring; inspect an existing target-template example before adding one.

`__ID__` becomes a timestamp-derived value at library insertion. Use a distinct
descriptive prefix for each independently identified element, keeping matching
references consistent, for example `id="plans__ID__"` and
`data-bs-target="#plans__ID__"`. This is not a fix for subsequent DOM duplication.

PB adds `data-pb-id` to the inserted root to identify the source block; a library
value is a Base64-encoded block path. Preserve it in existing page HTML, but let
PB generate it for a new library file. Basic blocks use `pb-basic-<type>.<index>`.

## Configuration and Hooks When Needed

Enable the PageBuilder editor type in the template group or template.
`inlineEditingEnabled=true` is needed for frontend inline editing;
`bootstrapColumns` sets grid size and `pageBuilderPrefix` sets the default `pb`
prefix (changing it also requires compatible PB CSS). These settings are not
prerequisites to edit a library file.

Custom editor support belongs in
`/components/INSTALL_NAME/admin/pagesupport-custom.js`, loaded after
`pagesupport.js`. Inspect the corresponding existing file under
`src/main/webapp/components/` before changing it. Use the documented hooks only
when the requested block or refactor needs them:

- `pbCustomOptions(options)`: palette/options.
- `pbCustomSettings(me)`: grid selectors, content wrapper, valid column prefixes,
  or `me.grid.duplicable` for existing repeated-element selectors.
- `pbScreenSizePrefix(me)` and `pbGetWindowSize(name)`: custom responsive behavior.
- `pbBuildTabMenu(me, tabMenu)`: style dialog customization.
- `afterPasteFromWordCallback(html, editor)` and `wysiwygGetCallback(html, conf)`:
  return cleaned HTML. The latter can run repeatedly, not only on final save.

Window events include `WJ.PageBuilder.loaded`, `instanceReady`, `gridChanged`,
`styleChange`, `newElementAdded`, `elementDuplicated`, and `elementMoved`, all with
the `WJ.PageBuilder.` prefix. Initializers must tolerate repeated events;
`instanceReady` can recur after row movement/duplication. A custom duplicable
selector does not add `pb-duplicable` to saved HTML; preserve its original hooks.

The editor adds `body.is-edit-mode`. Scope editor-only visibility or link
deactivation to it. Headers/footers are hidden in the PB editor; avoid relying
on them to render or initialize an editable content block.
