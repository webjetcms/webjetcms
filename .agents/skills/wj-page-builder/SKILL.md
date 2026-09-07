---
name: wj-page-builder
description: "Create, modify, review, or refactor WebJET CMS PageBuilder (PB) HTML blocks, such as testimonials, pricing, and reusable page sections. Use for PageBuilder nesting, editable regions, CSS class conventions, responsive columns, block libraries, tabs, accordions, and duplicable elements. Do not use for unrelated HTML styling or AppStore backend implementation."
---

# WebJET CMS PageBuilder Blocks

Produce HTML that editors can safely edit, style, move, and duplicate in
PageBuilder. PB manages layout; separate CKEditor instances manage leaf content.
A visually correct block is incomplete if editing its text can destroy its layout.

## Establish the Target

- Infer the target template, source file, and block type from the request and
  nearby files. For a self-contained feature such as customer testimonials or a
  pricing section, default to a `section` block unless the insertion context calls
  for `container`, `column`, or `content`.
- Inspect the target template's existing blocks, CSS/SCSS, Bootstrap version, and
  any `pagesupport-custom.js`. Reuse its frontend design conventions; the
  administration UI's tokens and Bootstrap version do not define the public site.
- When a file is requested but several template libraries are plausible, ask for
  the target before writing into one. A standalone HTML request can be answered
  with a fragment and an explicit block type without inventing an installation.
- Edit the maintained source, including Pug when the template generates HTML.
  Preserve requested copy, links, applications, design, and existing CSS hooks
  during refactoring. Do not invent customer endorsements or real pricing claims;
  label example content when actual copy was not provided.

## Essential Structure

Read [structure-and-styling.md](references/structure-and-styling.md) when creating
or refactoring HTML. It defines the class contract and the duplication rules.

The default layout is:

```text
section
  div.container
    div.row
      div.col-12.col-md-6.col-xl-4
        div.column-content
          h3, p, ul, images, links, other content markup
```

- Keep section blocks at the page content's top level. Use columns for ordinary
  repeated cards and nested containers for composite widgets. Do not nest an
  entire section block inside a column to reuse it.
- Ordinary columns are CKEditor regions. Never put bare text directly in a
  column; use at least a paragraph. PB inserts `div.column-content` when missing.
  If providing this wrapper explicitly, put all editable column content inside
  it and avoid adding a second wrapper during refactoring.
- Do not nest CKEditor regions inside another editable region. Use
  `pb-editable` for text outside ordinary editable columns, not on every heading
  or paragraph inside them.
- A structural column containing nested containers uses
  `pb-not-editable pb-always-mark`: the outer column retains its tools while its
  inner leaf columns remain separate editing regions. Read
  [interactive-blocks.md](references/interactive-blocks.md) before building this
  pattern; `pb-not-editable` is not a general-purpose wrapper for editable content.
- Use `col-*`, `col-md-*`, and `col-xl-*` widths for phone, tablet, and desktop
  respectively. Default to a 12-column grid and the `pb` prefix unless the target
  configuration overrides them. PB marker classes alone do not supply a site's
  responsive layout CSS.
- Style sections, containers, and column content. Keep width classes on the
  outer column; place its visual box styling on `column-content`. Account for
  this wrapper in CSS selectors even when PB creates it later.

## Task-Specific References

- For tabs, accordions, nested widgets, or generated menus, read
  [interactive-blocks.md](references/interactive-blocks.md).
- For adding files to the block picker, previews, metadata, images, Thymeleaf
  substitution, or template hooks, read
  [library-and-runtime.md](references/library-and-runtime.md).
- Canonical local documentation:
  [overview](../../../docs/sk/frontend/page-builder/README.md),
  [blocks](../../../docs/sk/frontend/page-builder/blocks.md), and
  [settings](../../../docs/sk/frontend/page-builder/settings.md).
  All source paths in the references are relative to the repository root.
  Recheck the relevant documentation and implementation if the target overrides
  the defaults or behavior is unclear; do not copy an inconsistent example blindly.

## Verify the Result

Review the authored HTML as an editor would use it:

- The fragment root matches its insertion type; structural nodes remain outside
  CKEditor, and every intended text region is editable.
- Column content is wrapped correctly; spacing, borders, and backgrounds still
  work after PB initialization and the template's normal CSS build.
- Responsive widths are coherent at below 768px, 768–1199px, and 1200px or above,
  adjusted for any verified project overrides.
- Each repeated unit uses the appropriate column, container, or `pb-duplicable`
  controls. Duplicating it does not break IDs, targets, or nested editing regions.
- Refactoring preserves content, attributes needed by CSS/JS, application
  directives, image behavior, and meaningful accessibility attributes.

When a runnable editor is available, verify insertion, text/image editing,
moving, duplication, and save/reopen for the behavior affected by the change.
For interactive blocks, also verify the published frontend with its actual
Bootstrap assets. Otherwise report static verification and the remaining browser
checks honestly. Return the changed files or requested fragment, its block type,
any CSS/runtime dependencies, and material assumptions.
