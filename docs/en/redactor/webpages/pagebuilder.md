# Page Builder

Page Builder is a special page editing mode. It lets you edit selected parts of a page and separates text and image editing from the page structure. This prevents you from accidentally deleting structural elements while editing text.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/ieaNWY57Exc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

The mode must be enabled, blocks prepared and templates configured. See the [web designer guide](../../frontend/page-builder/README.md).

When a template uses Page Builder, this mode loads when you open a page in the editor.

![](pagebuilder.png)

Use the **Editor** selector to switch between **Page Builder**, **Standard** and **HTML**. The selected mode is remembered for other Page Builder pages you open. Select **Page Builder** to resume building a page from blocks.

Switching modes transfers the current content to the selected editor. You can edit HTML and then continue in Page Builder. Save the page changes with **Save**.

## Basic work

Build your page from prepared blocks. Moving the pointer over content displays a faint, translucent outline. It shows the boundaries of the block you can click; green outlines help you find editable text.

Click text to select its block and start typing immediately. The selected block has a stronger outline and appears in the path in the top toolbar. Moving the pointer to another block does not change the selection. While typing, the selection outline becomes subtler and the hover outline disappears.

![Selected column on the left and a faint hover outline on the right](pagebuilder-hover.png)

By default, each color identifies a block type:

- Blue represents a section, the main building block, usually spanning the screen width.
- Pink represents a container, which contains rows of columns and is usually narrower than a section for readability.
- Gray represents a row, which arranges columns within a container.
- Green represents a column or standalone editable text. Columns typically contain text, images or applications.
- Orange represents a duplicable item, such as a list item.

![](pagebuilder.png)

The selected block's tools are in a fixed toolbar below CKEditor. A clickable path, such as **Section › Container › Row › Column**, lets you select a parent without finding its outline in the content. On narrow screens, use the button next to the current block type to open the parent path. The toolbar contains:

- Structure - opens the page's block tree.
- Add block (`+`) - shows section, container and column insertion points directly on the page, without selecting a block first.
- Column width - displays the current width, such as `3 / 12`, and opens width controls for the selected device size.
- Duplicate beside - inserts a copy immediately after the selected block and selects it.
- More actions (`…`) - opens additional operations for the selected block.
- Outlines (eye or layers icon) - cycles through outline modes.

Depending on the selected block type, **More actions** includes:

- Style - background, colors, alignment, spacing and other properties.
- Insert block before / Insert block after - opens the library for the same block type on the selected side.
- Move up / Move down - moves the block one position in the order.
- Move / Duplicate - shows available destinations on the page; click to move the block or insert a copy there. Press **Escape** to cancel destination selection.
- Add to favorites - saves the block to your personal favorites in the library.
- Delete - deletes the selected block.

Only supported operations appear. An ordinary row is used for navigation; duplicable rows and items support moving, duplication and deletion. Standalone editable text has no structure editing tools. Duplicable elements can only move between compatible siblings under the same parent.

## Style settings

Open **More actions → Style** to display the compact properties window. Its title, such as **Column style**, and the text below identify the edited block. **Locate on page** scrolls to that block and briefly highlights it. Drag the header to move the window.

![](pagebuilder-style.png)

Properties are organized into expandable groups: **Identification**, **Background**, **Visibility**, **Offset**, **Align**, **Dimensions**, **Borders**, **Radius**, **Shadow**, **Linked styles** and **Z-index**. **Identification**, with the ID, CSS classes and title, opens by default. Your web designer can customize the groups and their order for the template.

Several groups can stay open together. Collapsing a group preserves its pending values. The header and bottom buttons remain accessible while you scroll the properties. Select a background image with the button beside its address. For spacing, borders and corner radii, you can link all four values or pairs; linked values follow the first field in their group.

![Spacing settings with linked values](pagebuilder-style-settings.png)

Changes are previewed on the page as you edit. **Save** in this window confirms the style in the page being edited; use the editor's main **Save** button to save the whole page afterward. **Cancel**, the header's close icon or **Escape** close the window and revert unconfirmed style changes. **Reset** removes the block's custom style so that the template styles apply again.

## Inserting blocks

Add a block without first selecting a section or column:

1. Click **Add block** (`+`) in the top toolbar. The tools and block path are temporarily replaced by a blue hint with insertion instructions and a **Finish · Esc** button.
2. Click a plus at the required insertion point. Blue strips insert sections, pink strips insert containers and green plus buttons insert columns. Points appear before the first block, between neighboring blocks and after the last block. Strip labels and button descriptions identify the type and position.
3. Click the required block card in the library. The library offers the appropriate type and reminds you of the destination at the top. Clicking inserts the block immediately, without another confirmation. Insertion mode ends, the new block is selected and you can edit its content.

![Insertion points for sections, containers and columns](pagebuilder-insert.png)

You can also open the library for a selected block through **More actions → Insert block before / Insert block after**. It contains these tabs:

- Basic - simple blocks of different sizes.
- Library - blocks prepared for your website.
- Favorites - blocks you have marked as favorites.

![](pagebuilder-library.png)

The library opens in a narrow window over the page. The header shows, for example, **Insert section**, with the insertion destination below. Drag the header to move it. Close it with the icon in the top right corner or **Escape**.

On the **Library** tab, blocks are grouped into categories with block counts. Click a category to expand it; only one stays open at a time. Cards show a name and a preview with its original aspect ratio. Click the preview or name to insert the block.

**Find a block…** filters by name and can be combined with one tag. Filtering preserves an open matching category or opens the first category with results. Category counts reflect the active filter; tag counts show the total number of blocks with that tag. **All** clears only the tag and preserves the search text. If nothing matches, **Clear filters** resets both text and tag. Search and tags remain accessible while you scroll the results.

![Searching for blocks with a tag filter](pagebuilder-library-filter.png)

Your web designer defines names and tags in `pagebuilder.properties` when [creating blocks](../../frontend/page-builder/blocks.md#block-name-and-tags). They belong to the template's library and do not necessarily change with the administration language. On the **Favorites** tab, use the button beside a block's name to remove it; removal requires confirmation.

A `+` icon at the bottom of the page provides quick access to adding another section.

![](pagebuilder-plusbutton.png)

Entering insertion mode smoothly opens temporary gaps without scrolling visible destinations away. Column widths stay unchanged: plus buttons appear in gutters, between wrapped rows or over the content when space is limited. Selection and hover outlines disappear while choosing a destination.

Closing the library returns focus to the selected plus. End insertion mode with **Finish · Esc** or **Escape**. The gaps collapse smoothly, the regular toolbar returns and focus moves back to `+`. Clicking content ends the mode and selects that block. If your system uses reduced motion, opening and closing happen immediately. Insertion helpers are neither saved nor shown in the preview.

## Page structure and outline modes

**Structure** opens a block tree with names derived from the content. Search by name or type. Clicking an item selects the block, scrolls to its position and expands its closed branch. Clicking again leaves the branch open; the arrow beside the name expands or collapses it without changing the selection.

The selected item has a thin outline in its type's color, matching the page outline. The panel opens over the content and does not change the page width or wrapping. On narrow screens it closes after selection.

![Page tree with a selected column](pagebuilder-structure.png)

Hidden blocks have a **Hidden** label. Selecting them does not change their visibility or the active page tab. The tree adds no names or identifiers to the HTML and does not support dragging blocks to reorder them. Use the toolbar actions to move blocks.

Use the up and down arrows to navigate the tree, and right and left arrows to expand or collapse branches. **Enter** or Space selects an item and expands its closed branch. **Escape** closes an open menu or panel, ends width editing, or cancels move or insertion destination selection.

The eye button cycles through three modes:

- **Selected block outline** (default) - highlights the selection and shows a faint outline for another block under the pointer.
- **No outlines** (crossed-out eye) - hides both selection and hover outlines.
- **Full active block hierarchy** (layers icon) - also outlines the parent section, container and row. Hover outlines show the hierarchy with fainter lines. Shared ancestors are not highlighted twice.

The browser remembers your choice when you reopen the editor. Switching modes does not change the toolbar, Structure panel or content wrapping. Helper outlines and controls do not appear in **Preview** or on the saved page.

## Setting column widths

Set different column widths for mobile, tablet and desktop:

1. Select a device size using the icons beside **Editor**.
2. Click a column and choose **Column width**, such as `3 / 12`, in the toolbar. A blue hint replaces the tools and path, as in insertion mode. It identifies the size being edited, such as **MD — Tablet (768–1199 px)**.
3. Use the arrows inside each column to change its width. The value is the number of grid units; in the default 12-unit grid, `3 / 12` is a quarter and `12 / 12` is full width. The `auto` value allows automatic sizing.
4. End the mode with **Finish · Esc** or **Escape**. The regular toolbar and your previous outline setting return.

Green outlines identify all columns in the container whose widths you can adjust, even if regular outlines are hidden. Controls with a light green background occupy temporary space inside each column above its content. In narrow columns, the value appears above the arrows. Exiting the mode releases that space.

![Width controls inside columns with the blue mode hint](pagebuilder-width.png)

You can switch devices while editing widths. The controls and hint update, and subsequent changes apply to the selected device.

![](pagebuilder-switcher.png)

The abbreviation beside the width identifies the device size being edited. Defaults are:

| Device | Label | Preview width | CSS class |
| --- | --- | --- | --- |
| Desktop | `XL` | 1200 px and above | `col-xl-` |
| Tablet | `MD` | 768–1199 px | `col-md-` |
| Mobile | no abbreviation | below 768 px | `col-` |

Templates can define custom labels and breakpoints. The hint then shows the template's label, such as `SM`, without the default ranges listed above.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/aru-B1vxReo" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Column splitting

**Split column** belongs to the blocks inserted into text:

1. Place the text cursor at the split point.
2. Click **Blocks** in the CKEditor toolbar.
3. In the library, open **Basic** and choose **Split column**.

The content is split into two columns at the cursor. You do not need to create another column and move the text manually. Use **Blocks** for prepared text fragments too; the Page Builder toolbar's `+` adds sections, containers and columns.
