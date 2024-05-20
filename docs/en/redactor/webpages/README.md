# Website

## Basic controls

### Choosing a domain

For multi-domain web sites, the domain selection is displayed at the top. In the tree structure of the site, only folders with the selected domain and folders that do not have a domain set are displayed.

![](domain-select.png)

### Viewing the System and Trash folders

The special folders System (contains pages needed for the template such as header and footer) and Trash (contains deleted pages) appear in the System or Trash tab.

The display in the System tab depends on the WebJET configuration:
- displays the contents of the folder by default `/System` (global folder for all domains)
- if the mode is on **Local Folder System** (set configuration variable `templatesUseDomainLocalSystemFolder` at `true`) and a local System folder exists for the currently selected domain, the contents of the folder will be displayed
- if Folder System search mode is on **recursive in tree structure** (set configuration variable `templatesUseRecursiveSystemFolder` at `true`) displays the folder structure containing the System folder
In addition to the Folder System, this tab also displays the folder `/files` with a full-text index for searching files (if full-text search is active). This folder contains the text extracted from the files, and the text is used in the file search.

The Trash tab shows the contents of the folder `/System/Kôš`.

These folders do not appear in the Folders tab (they are filtered), but if for some reason you need to see the exact tree structure without filtering, click on the Folders tab with the `shift`. In this case, filtering is turned off and all folders in the currently selected domain and folders that do not have a domain set are displayed.

![](system-folder.png)

### Remembering the last folder you opened

The list of web pages remembers the last folder opened in a single login, and when you return to the list of web pages, the folder is reopened. Folder memory is deleted when switching domains or when entering an address `/admin/v9/webpages/web-pages-list/?groupid=0`, i.e. the address of the page with the parameter `groupid=0`.

At the same time, when browsing the tree structure, the address of the page is displayed in the browser address bar with the parameter `groupid`, which represents the folder ID. When the page is refreshed or a link is sent, the folder structure is opened according to the ID in the address bar. On the home page, you can add a bookmark to the Bookmarks block [add page address with folder ID](https://youtu.be/G5Ts04jSMX8) and create a link to the nested folder structure on the home page.

### Web site tabs

The following tabs can be viewed in the right section:
- `Web stránky` - displays a standard list of web pages in the selected folder in a tree structure.
- `Naposledy upravené` - shows a list of your most recently edited pages.
- `Čakajúce na schválenie` - if you approve changes to web pages, the pages that are awaiting your approval will appear in this tab.
- `Priečinky` - switches the view from a list of web pages to a list of folders. Clicking on a folder in the tree structure displays the selected folder and its subfolders. If you select multiple folders in the tree structure (e.g. by pressing the CTRL key), the selected folders are displayed. The tabular view of folders allows you to, for example, perform bulk operations on folders (e.g., change the template), use the Edit Cell function, or the Duplicate function. **Notice**: you must first enable folder view in [tree structure view settings](#nastavenie-zobrazenia-stromovej-štruktúry).

![](../../_media/changelog/2021q1/2021-13-awaiting-approve.png)

### View pages also from subdirectories

If necessary, you can also view web pages from subdirectories by toggling the **View pages also from subdirectories** in the datatable header. When you switch to the mode of displaying pages from subdirectories, the pages from the currently selected directory are displayed in the tree structure, including its subdirectories. You can click on another directory in the tree structure, which again causes the pages from the selected directory and its subdirectories to be displayed.

In the table settings, you can turn on the display of the column **Parent folder** in which you will see the directory in which the page is located.

![](recursive-list.png)

### Recovering web pages and directories from the Recycle Bin

The list of web pages also offers a special icon ![](recover-button.png ":no-zoom") to restore a web page or an entire folder from the Recycle Bin. These icons only appear in specific circumstances. For a more detailed explanation of the logic, see [Recovering web pages and folders from the Recycle Bin](./recover.md)

### Special icons

The Data table in the page list contains the following special icons:
- <i class="far fa-eye fa-btn" role="presentation" /> - Show page - after selecting one or more lines and clicking on the icon, the selected web page will open in a new window/tab.
- <i class="fas fa-restroom fa-btn" role="presentation" /> - Save as AB test - creates a B version of the page for [AB testing](../apps/abtesting/README.md).
- <i class="far fa-chart-line fa-btn" role="presentation" /> - Page Statistics - displays [traffic](../apps/stat/README.md) marked web page.
- <i class="far fa-link-slash fa-btn" role="presentation" /> - Check for links and blank pages - checks [validity of links](linkcheck.md) in the pages in the current folder and subfolders, displays pages that have no text entered.
- ![](icon-recursive.png ":no-zoom") - Show web pages from subdirectories - switch the switch to the on position to show web pages from subdirectories in the table

### Icons and colours in the tree structure and page list

The following types of icons and colors can be displayed in the folder and page tree structure:
- <i class="fas fa-folder" role="presentation" /> full folder icon = folder is displayed in the menu
- <i class="far fa-folder" role="presentation" /> empty folder icon = not shown in the menu
- <i class="fas fa-map-marker-alt" role="presentation" /> the page is displayed in the menu
- <i class="far fa-map-marker-alt-slash" role="presentation" /> the page is not displayed in the menu
- <i class="fas fa-folder-times" role="presentation" /> you don't have edit/delete permissions on the folder, you won't even see any list of web pages in that folder (even if the folder actually contains web pages). This is used when you only have permissions on any of the subfolders.
- <i class="fas fa-lock" role="presentation" /> lock = only available for logged in visitor
- <span style="color: #FF4B58">red colour</span> = unavailable to the public (internal directory) or page with display disabled
- <i class="fas fa-star" />, **bold font** = main page of the directory
- <i class="fas fa-external-link-alt" /> out arrow = page is redirected
- <i class="fas fa-eye-slash" /> crossed out eye = page not searchable
- <i class="fas fa-restroom" /> B variant of the application page [AB testing](../apps/abtesting/README.md)

## Setting the tree structure view

If necessary, you can click on the icon in the tree structure <i class="far fa-wrench" /> Settings to display the settings dialog box:
- `ID` - It will also display the directory ID in the form #ID before the name. This display is useful if you need to manually enter a folder ID into an application, or if you are migrating pages between environments and need to quickly check the settings of embedded applications.
- `Poradie usporiadania` - After the name, display the order of the arrangement in the form (order).
- `Web Stránky` - It also displays web pages in the tree structure. **Warning:** reduces performance and data loading speed. We recommend enabling this option only if you need to move web pages using the `Drag&Drop`.
- `Priečinky stromovej štruktúry ako tabuľku` - Displays the Folders tab in the datatable. Allows you to use datatable features such as bulk operations, duplicate, cell editing, etc. with tree structure folders.
- `Pomer šírky stĺpcov strom:tabuľka` - Sets the ratio of the column widths of the displayed tree structure and datatable for better use of monitor width. The default ratio is 4:8. Warning: some ratios and inappropriate monitor size may cause the toolbar/buttons to display incorrectly.

![](jstree-settings.png)
