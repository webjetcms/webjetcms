# Page Builder

Page Builder is a special page editing mode. In this mode, the entire page is not edited, but only selected parts of it. Page Builder separates the editing of text/images and the page structure. This prevents you from accidentally deleting structural elements of a web page when editing its text.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/ieaNWY57Exc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

The mode needs to be activated, blocks prepared and templates set up, the procedure is in the [web designer] section (../../frontend/page-builder/README.md).

When you set the Page Builder option for a template, the Page Builder mode will load when you open a web page in the editor.

![](pagebuilder.png)

If necessary, the window has the option to switch the editor to standard mode. Switching from PageBuilder mode to Standard mode is remembered until the page is refreshed again. Any other PageBuilder page will then also be displayed in standard mode. You must switch back to PageBuilder mode by toggling the selection box or refreshing the entire page.

!>**Warning:** The content of the editors is not synchronized while you are editing. Both will load the same content only when the window is opened. So you cannot start making changes in Page Builder and then switch to the standard editor to make further changes there.

## Basic work

When using Page Builder, you create a website from pre-made blocks. Hovering over a block will highlight individual sections, where:

- The blue color represents a section - the main building block, usually the entire width of the screen.
- The red color represents a container - a block designed to insert columns, it is usually narrower than a section for better readability of texts on the page.
- The green color represents a column - it typically contains editable text, images, or applications.

![](pagebuilder.png)

Each highlighted section displays a gear icon, clicking on the gear will display a toolbar. This contains options:

- Style - allows you to set advanced block style/properties such as background image, colors, alignment, indentation, etc.
- Column width - sets the column width, it is possible to set different widths for different devices (phone, tablet, computer).
- Move block - allows you to move a block to another location on the page. After clicking the icon, options for where the block can be moved will be displayed.
- Duplicate - duplicates the selected block.
- Favorites - adds the block to the list of favorite blocks.
- Delete - deletes the selected block.

![](pagebuilder-style.png)

Clicking on the gear wheel will also display the + icons for inserting a new block. Clicking on the appropriate location will display the block selection. It contains the following tabs:

- Basic - simple blocks of various sizes.
- Library - blocks created for your website.
- Favorites - blocks that you have marked as favorites.

![](pagebuilder-library.png)

In the library tab, you can search for blocks by name, or filter blocks by tags. You can define these in the `pagebuilder.properties` file when [creating blocks](../../frontend/page-builder/blocks.md#block-name-and-tags) for your website.

An `+` icon is displayed at the bottom of the page to make it easier to add a new section.

![](pagebuilder-plusbutton.png)

## Setting column widths

The editor allows you to set column widths according to the selected device. In the toolbar next to the editor type switch, there is an option to set the device size (width).

![](pagebuilder-switcher.png)

- Desktop - is intended for a width greater than/equal to 1200 points (sets the CSS class ```col-xl```).
- Tablet - is designed for a width of 768-1199 points (sets the CSS class ```col-md```)
- Mobile - is intended for widths less than 768 points (sets CSS class ```col-```)

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/aru-B1vxReo" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Column Splitting

You can access the Split Column function by clicking on `+` in the yellow bar and selecting the Block option. Then, in the Basic tab, select the Split Column option. The function allows you to quickly split a column without having to insert a new column and move the content. It will allow you to insert new complex blocks, e.g. into a long text column.