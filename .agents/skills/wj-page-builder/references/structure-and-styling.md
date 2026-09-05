# Structure, Styling, and Repeated Elements

Source: `docs/sk/frontend/page-builder/settings.md` and `blocks.md`.
Implementation checks: `src/main/webapp/admin/webpages/page-builder/scripts/ninja-page-builder.js.jsp`
(`set_settings`, `mark_*`, `wrap_column_content`, `create_style_element`).

## Recognition and Editing

These are the default conventions, not a prohibition on documented custom grids
or intermediate wrappers used by widgets.

| Element or class | Meaning and authoring rule |
| --- | --- |
| `section` | Blue section tools. Keep at the top level of page content. |
| `pb-not-section` | Exclude a section from PB section recognition. |
| `div.container`, `div.pb-custom-container` | Red container tools; the custom marker makes a nonstandard wrapper a container. Nested containers are supported. |
| `pb-not-container` | Keep a layout wrapper out of PB container recognition; often used around tabs or an accordion. |
| `div.row` | Bootstrap layout wrapper. Normally has no independent PB editing/styling tools. |
| `div` with `col-*` or `pb-col-*` | Green column tools and automatic CKEditor content, unless excluded. |
| `pb-col`, `pb-col-auto` | Column markers for custom layouts; no column-width control. Preserve/provide the template's actual layout CSS. |
| `pb-not-column` | Exclude an otherwise matching column from PB column recognition. |
| `div.column-content` | Persistent inner content wrapper; PB creates it if absent. Column visual styles apply here. |
| `pb-editable` | Explicit CKEditor region outside a normal editable column. Does not add PB styling tools. |
| `pb-not-editable` | Suppress editing/marking of the marked node; intended to protect noneditable structures. See the nested-widget exception below. |
| `pb-always-mark` | Retain the green toolbar on a `pb-not-editable` column, without making the outer column a CKEditor region. |
| `pb-duplicable` | Orange move/duplicate/delete tools on repeated content elements or an explicitly marked row. |

The current default container selector includes
`div[class^="container"]:not(.pb-not-container)`. Put `container` or
`container-fluid` first in the class attribute, or use `pb-custom-container` for
an intentional custom container. Use `div` for recognized rows and columns;
putting `col-12` on an `article` does not match the default column selector.

The docs describe `pb-not-editable` as disabling a subtree, but separately support
nested containers inside a `pb-not-editable pb-always-mark` column. Container
discovery traverses descendants, so do not generalize this class into a guaranteed
subtree lock. Use the documented structural-column pattern for nested widgets and
verify the inner leaf editors. Duplicable elements under `pb-not-editable` are
excluded even in this pattern.

## Default Nesting

Use `section > container > row > column > column-content` for ordinary blocks.
Multiple containers per section, rows per container, and columns per row are
normal. Text outside columns needs its own `pb-editable` region if users must
edit it. An extra card wrapper inside an ordinary column belongs to that one
editor; it does not automatically create independent layout controls.

For nested layout, protect the outer structural column and create independent
inner containers/rows/leaf columns, as shown in
[interactive-blocks.md](interactive-blocks.md). Keep rows and nested widgets out
of an ordinary column's CKEditor region. The documentation's lack of shared block
includes refers to composing library files, not a ban on nested HTML containers.

## CSS and Widths

| Purpose | Convention |
| --- | --- |
| Section style namespace | `pb-style-section-<name>` on `section` |
| Container style namespace | `pb-style-container-<name>` on the container |
| Column style namespace | `pb-style-column-<name>` on the outer column; target its direct `column-content` child for the visual box |
| Phone, below 768px | `col-N` |
| Tablet, 768–1199px | `col-md-N` |
| Desktop, at least 1200px | `col-xl-N` |

`N` is a width in the configured grid, normally 1–12. For example,
`col-12 col-md-6 col-xl-4` gives one, two, then three cards per row.
Use a coherent width for each intended device; an all-device full-width column
can simply use `col-12`. `pb-col-12` provides PB recognition for a custom grid;
do not assume its responsive widths are implemented by Bootstrap on the frontend.

The style prefixes are naming conventions, not prebuilt themes. Reuse existing
CSS utilities or implement scoped template CSS for new names. Do not invent
undocumented `pb-*` spacing/color utilities or add custom site styles to the
administration's PageBuilder stylesheet.

The COLUMN example in `settings.md` uses `pb-style-content-person-26`, although
the accompanying rule and `set_settings().tag.column_style_prefix` both specify
`pb-style-column`. Use `pb-style-column-*` for new column style names. Preserve
existing `pb-style-content-*` hooks if a target template actually styles them.

This minimal column fragment shows the separation; adapt its content and styling
to the target rather than imposing this design on every block:

```html
<div class="col-12 col-md-6 col-xl-4 pb-style-column-review">
  <div class="column-content">
    <blockquote>
      <p>Example customer feedback.</p>
    </blockquote>
    <p><strong>Example customer</strong></p>
  </div>
</div>
```

```css
.pb-style-column-review > .column-content {
  padding: 1.5rem;
  border: 1px solid currentColor;
  border-radius: 0.5rem;
}
```

Do not rely on `.col-* > h3` or `.col-* > .card`: the inserted wrapper changes
those relationships. During refactoring, update affected selectors together with
the HTML and avoid wrapping an existing `column-content` again.

## Choose the Repetition Unit

- Use regular column tools for pricing plans or testimonial cards when each unit
  should have its own width, styling, and editable content.
- Use `pb-custom-container` for independent tabs, accordion items, or other units
  containing their own rows and columns.
- Use `pb-duplicable` on a repeated non-void element inside column content, such as
  an `li`, when it needs move/duplicate/delete within its existing list.
- Use `row pb-duplicable` to manipulate an entire row including all its columns.
  Row contents remain editable; the row does not gain a styling dialog.

Duplicable targets must also be marked and have the same tag, type (row versus
ordinary content element), and direct parent. An `li` cannot move into a different
`ul`, and a row cannot move into another container. A single marked row can be
duplicated; moving needs another eligible sibling. Only the outermost of nested
duplicable elements gets controls. Void elements such as `img` are unsupported;
mark an appropriate enclosing content element instead.

Duplication preserves attributes, including `id`. Do not promise automatic unique
IDs for arbitrary clones. Library insertion can replace `__ID__`, but later
duplication of the resulting DOM does not repeat that substitution. Avoid IDs
when unnecessary; verify or implement the specific widget's ID/target repair when
it needs them. Tabs and accordions have their own support described in
[interactive-blocks.md](interactive-blocks.md).

## Saved HTML Versus Editor Decorations

Author semantic PB classes above, not generated toolbar/highlighter markup,
`contenteditable`, CKEditor instance attributes, or runtime classes such as
`pb-grid-element`, `pb-column__content`, and `pb-duplicable-element`.
When cleaning exported HTML, preserve meaningful content, `column-content`,
existing PB style data, and `data-pb-id`; do not blanket-delete every `pb-*` class
or `data-*` attribute.
