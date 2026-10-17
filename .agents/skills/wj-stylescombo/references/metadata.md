# CSS metadata contract

This contract is based on WebJET's `src/main/java/sk/iway/css/CssParser.java`, `CssDto.java`, the adapter in `src/main/webapp/admin/v9/src/js/datatables-ckeditor.js`, and `libs-ckeditor4/plugins/wjstylescombo/plugin.js`. When the current implementation differs, use the implementation. The frontend guide is `docs/sk/frontend/examples/template-bare/README.md`, under the style search/group section.

## Syntax and selector limits

Write comments **inside** the CSS rule. Use the exact lowercase prefixes and one field per comment:

```css
div.alert.alert--warning {
    /* editor title: Warning */
    /* editor category: Alerts */
    /* editor group: Alert tone */
    /* editor base: alert */
}
```

Use plain text for newly generated display labels. Existing HTML titles are accepted and shown as plain text by the new picker; preserve them in fill-missing mode. Keep UTF-8 accents. Never translate `editor`, field names, HTML tags, or literal classes.

The safe selector subset is an optional tag or `*` followed by one or more literal classes: `.large`, `*.large`, `p.large`, `div.alert.alert--warning`. The parser is not a CSS AST. Do not assume selector lists, combinators, attributes, IDs, pseudo-classes/elements, CSS escapes, or raw SCSS syntax will produce usable catalog entries. Analyze these constructs in the full styles as evidence, but flag them in the catalog instead of flattening/changing them. A tag-only rule is not a CSS class choice.

SCSS `div.alert { &.alert--warning { ... } }` can emit an eligible compound selector; `div.alert { .alert--warning { ... } }` emits a descendant selector and is not equivalent. Validate the final selector. Nested responsive rules may be readable through the explicit-comment fallback, but metadata does not encode a media condition or turn applicability on/off with viewport width.

The initial parser scan stops at `@media`, `/** Editor end **/`, or `.cssParserEnd`; a fallback discovers eligible rules with literal `/* editor` comments after those markers. It deduplicates by tag and class string. Do not rely on multiple occurrences of a selector having different metadata; later duplicate comments may be ignored. Preserve stop markers and avoid surrounding standalone comments that the limited parser could mistake for part of a selector. For new annotation, use a separate comment for every field rather than a single comment containing several keys.

## Fields and decisions

| Field | Meaning and authoring rule |
| --- | --- |
| `title` | Readable display name. An explicit title takes precedence over a legacy inline comment. Preserve existing labels in fill-missing mode; generate a localized description of the appearance when absent. |
| `category` | Display/search category only. Choose by actual effect or component purpose. Missing categories appear under the plugin's localized Other category. |
| `group` | Mutually exclusive alternatives on compatible targets. Names are compared across the catalog, **not scoped by category**. Reuse exactly the same name for the same behavior; use different names for unrelated properties. |
| `targets` | Optional HTML tags separated by spaces or commas. Intersects the selector's tag restriction; it cannot widen `p.foo` to headings. With `.foo`, it can narrow applicability to several tags. Infer from intended usage, not a generic copied tag list. |
| `base` | Structural classes preserved during toggle/reset. They are also required on the existing target unless a nonempty `requires` overrides that requirement. Use only justified structural classes, not every class in a compound preset. |
| `requires` | Classes that must already exist on the **same target element**, not its ancestor. A nonempty value replaces the default requirement from `base`. Empty or omitted values fall back to `base`; empty does not mean no requirements. Add only when a distinct requirement is needed. |
| `members` | Additional literal variant classes to remove when selecting/clearing the choice or group, including variants present only in markup. Ordinary sibling choices already contribute their removable classes. No dots, selectors, wildcard/prefix patterns, structural classes, or unrelated utilities. Use only when evidence establishes additional variants; do not fill it mechanically. |

Class tokens in `base`, `requires`, and `members` are whitespace/comma-separated names without `.` prefixes. Prefer spaces for all token lists. Do not output empty optional fields as placeholders.

The picker computes removable classes as the selector's classes minus `base`. When applying a choice, it clears compatible definitions' removable classes in the same group and their extra members, while protecting structural classes. A mistaken group or member can therefore remove unrelated appearance. `members` can also affect an ungrouped choice, so omitting `group` does not make speculative members harmless.

A base-only choice has no removable variant. Do not mark a standalone utility as its own base merely to make reset preserve it: this can make the choice unavailable until the class already exists. For component modifiers, confirming the required base is intentional; a modifier is not a command to create a new component.

`span.foo` with no `targets` is the plugin's special inline-text style. `.foo` with `targets: span` does not establish that same inline behavior. Do not change selectors or add redundant targets to span entries during metadata annotation. Existing element tags remain unchanged when a CSS choice is applied.

## Examples based on known declarations

These examples illustrate decisions only when matching declarations and component structure have been verified. Translate display values into the requested language; the examples use English.

If `.text-large` changes font size and `.text-bold` changes weight, both can use category `Typography`, but use groups `Font size` and `Font weight`. If `.mb-small` changes bottom margin and `.pb-small` changes bottom padding, use groups `Bottom margin` and `Bottom padding`. A breakpoint-specific `.mb-md-large` should coexist with the base `.mb-small` and use a distinct group, such as `Bottom margin from medium screens`.

For confirmed alert modifiers on an existing `div.alert`, use the example above and the same `Alert tone` group for `alert--success`, `alert--warning`, etc. The explicit `requires: alert` would be redundant. Add `members` only for verified additional tones not represented by those sibling choices. An independent `alert--full-width` modifier should not share the tone group.

If `.heading-display` is a reusable size style intended for paragraphs and headings:

```css
.heading-display {
    /* editor title: Display heading appearance */
    /* editor category: Typography */
    /* editor group: Font size */
    /* editor targets: p h1 h2 h3 h4 h5 h6 */
}
```

For a deliberate combined preset, do not invent structural requirements or a single-property exclusive group:

```css
p.h4.bold.mb-large {
    /* editor title: Lead paragraph */
    /* editor category: Typography */
}
```

This requires analyzing `.h4`, `.bold`, and `.mb-large` individually. Changing one property later can make the preset cease to match fully; that is expected and is not a reason to force every constituent class into one group.
