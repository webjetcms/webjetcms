# Page Builder

Page Builder is a special page editing mode. In this mode the whole page is not edited but only selected parts of it. Page Builder separates text/image editing and page structure editing. This prevents you from accidentally deleting structural elements of a web page while editing its text.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/ieaNWY57Exc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

The mode needs to be activated, blocks need to be prepared and templates need to be set up, the procedure is in the section for [web designer](../../frontend/page-builder/README.md).

When you set the option to use Page Builder for a template, Page Builder mode is loaded when you open a web page in the editor.

![](pagebuilder.png)

If necessary, there is an option in the window to switch the editor to standard mode. Switching from PageBuilder mode to Standard mode is remembered until the page is refreshed again. Also another PageBuilder page will subsequently be displayed in standard mode. You must switch back to PageBuilder mode by toggling the selection box, or by refreshing the entire page.

!>**Warning:** the content of the editors is not synchronized during your edits. They both load the same content only when the window is opened. So you can't start making changes in Page Builder and then switch to the standard editor and make more changes there.

## Basic work

When you use Page Builder, you create a web page from pre-made blocks. Moving the cursor over the block will highlight the individual sections where:
- The blue color represents a section - the main building block, usually the full width of the screen.
- The red color represents a container - a block designed for inserting columns, it is usually narrower than a section for better readability of the texts on the page.
- The green color represents a column - this already contains typically editable text, images or applications.

![](pagebuilder.png)

Each highlighted part displays a gear icon, click on the gear to display the toolbar. This contains options:
- Style - allows to set advanced block style/properties such as background image, color, alignment, indentation, etc.
- Column width - sets the column width, different widths can be set for different devices (phone, tablet, computer).
- Move block - allows you to move a block to another place in the page, after clicking on the icon you will see options where the block can be moved.
- Duplicate - duplicates the selected block.
- Favorites - adds the block to the list of favorite blocks.
- Delete - deletes the selected block.

![](pagebuilder-style.png)

Clicking on the gear will also display the + icons for inserting a new block. Clicking on the appropriate location will display the block selection. This contains the tabs:
- Basic - simple blocks of different sizes.
- Library - blocks created for your web page.
- Favorites - blocks that you have marked as favorites.

![](pagebuilder-library.png)

## Setting the width of the columns

The editor allows you to adjust the column widths according to the selected device. In the toolbar, next to the editor type toggle, there is an option to set the size (width) of the device.

![](pagebuilder-switcher.png)

- Desktop - is for width greater/equal to 1200 pixels (sets the CSS class `col-xl`).
- Tablet - is designed for width 768-1199 pixels (sets CSS class `col-md`)
- Mobile - is for width less than 768 pixels (sets CSS class `col-`)

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/aru-B1vxReo" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>
