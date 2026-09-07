# Tabs, Accordions, and Nested Widgets

Sources: `docs/sk/frontend/page-builder/blocks.md` and
`src/main/webapp/admin/webpages/page-builder/scripts/pagesupport.js`
(`pbGenerateTabs`, `pbGenerateAccordion`, `pbAutoMenu`).

## Tabs

- Use an empty `ul.nav.nav-tabs.pb-autotabs`; support code generates the navigation
  from `.tab-pane` containers. Do not make generated navigation a CKEditor region.
- The outer `div.container` uses `pb-not-container`; each pane uses
  `pb-custom-container`, with its own row and editable columns.
- A pane's nonempty `title` attribute takes precedence. For editor-friendly names,
  omit it and use `div.pb-tab-title` containing an `h3` or `p` in an editable column.
- IDs derive from titles, or from `data-title` on `.tab-pane` when supplied.
  Ensure names/overrides are unique across the page; generation from a title is
  not a collision-proof ID allocator.
- Navigation refreshes after structural events and every five seconds. Generated
  navigation is saved with the page. The saved active pane is the one visible to
  visitors; provide one initial `active show` pane and check the state before save.

Duplicating a pane or the whole widget can repeat its titles and `data-title`
values. Before saving, give the copied panes unique titles or unique `data-title`
values. If the requested workflow must preserve labels and work immediately after
duplication, use the template's custom support hook to refresh the copied panes'
`data-title` values and regenerate navigation on `WJ.PageBuilder.elementDuplicated`.
Verify event ordering with `pagesupport.js` so generation uses the new values.
Updating only `id` is insufficient because the generator overwrites it; `__ID__`
only helps initial library insertion. Do not claim duplication safety for a static
fragment that has no such handling.

## A Tab Block Nested in an Accordion

For a library `column` block that inserts an entire tab widget inside an accordion
item's row, use this structure. The protected outer column has its own toolbar;
each pane is a nested container with independent leaf editors.

```html
<div class="col-12 pb-not-editable pb-always-mark">
  <div class="tabsBox">
    <ul class="nav nav-tabs pb-autotabs"></ul>
  </div>
  <div class="tab-content">
    <div class="tab-pane fade active show pb-custom-container">
      <div class="row">
        <div class="col-12 pb-tab-title"><h3>Overview</h3></div>
        <div class="col-12"><p>Overview content.</p></div>
      </div>
    </div>
    <div class="tab-pane fade pb-custom-container">
      <div class="row">
        <div class="col-12 pb-tab-title"><h3>Details</h3></div>
        <div class="col-12"><p>Details content.</p></div>
      </div>
    </div>
  </div>
</div>
```

For a standalone `section` version, place the tabs navigation and pane group
inside `section > div.container.pb-not-container`; omit the protected outer
column. Do not surround either form with an extra `pb-editable` wrapper. PB
re-marks the grid when inserting a column whose HTML contains `container`, which
allows the inner pane toolbars to appear.

## Accordion

Use `div.container.pb-not-container.pb-autoaccordion` around the items. Each
item is a `div.card.pb-custom-container` with a header and collapsible body.
Keep the header text explicitly editable and put body text in leaf columns.

The documented card-based structure is:

```text
section
  div.container.pb-not-container.pb-autoaccordion
    h2.pb-editable
    div.card.pb-custom-container
      div.card-header
        .accordionLink (collapse trigger)
          .pb-editable (item title)
      div.collapse
        div.card-body
          div.row
            div.col-12 (body content or nested-widget column)
```

Adapt the trigger markup and Bootstrap attributes to the target template;
Bootstrap 4 uses `data-toggle`, Bootstrap 5 uses `data-bs-toggle`.

Support code generates/refreshes header IDs, collapse IDs, and trigger targets
after structural changes, including duplication. It also recognizes
`md-accordion-item`/`md-accordion-header` and
`accordion-item`/`accordion-header`, with `.accordion-button` or `.accordionLink`
triggers. Preserve those hooks when adapting a design. Support code's attribute
generation is not a guarantee of complete accessibility or Bootstrap-version
compatibility: verify keyboard activation, expanded state, ARIA ID references,
and collapse behavior in the actual frontend. Avoid hardcoding per-item IDs that
conflict with generated values.

## Generated Section Menu

`ul.pb-automenu` is rebuilt from the page's `section` elements. Exclude the menu's
own section (and any unwanted section) with `pb-not-automenu`. Label priority is
`.section-title`, then `h1`, then the section's `title` attribute. A missing section
ID is generated from its position. Structural changes update menu entries; text
updates can take five seconds. Preserve intentional stable section IDs for links.
Do not treat generated menu items as independently maintained content.

## Runtime Boundaries

PageBuilder's `pagesupport.js` performs editor-side generation and keeps generated
markup in the saved page. The frontend still needs the template's Bootstrap JS/CSS
and any actual widget dependencies. An uninserted fragment with empty tab
navigation is not a complete standalone browser demo; preview it through PB or
explicitly provide equivalent preview initialization. Do not copy the entire
admin support script into every block.
