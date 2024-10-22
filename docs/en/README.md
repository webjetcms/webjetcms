# WebJET CMS 2024

Welcome to the documentation for WebJET CMS version 2024. We recommend to read [list of changes](CHANGELOG-2024.md) a [roadmap](ROADMAP.md).

# List of changes in the latest version

## 2024.0

> Version 2024.0 includes a new version **updates describing the changes**, **cloning structure** integrated with the mirroring function (including the possibility of interlacing), adds the possibility of **Retrieved from** web page, or **the entire folder from the bin**, adds **HTML editor** and the ability to set the editor type directly for the template, **applications** it is possible **enable display only for selected device types** mobile, tablet, PC and of course improves the safety and comfort of work.

### Groundbreaking changes

This version introduces several changes that may not be backwards compatible:
- Discussion/Forum - add new post and approval modified from Struts to Spring, use call `/admin/update/update-2023-18.jsp` For [basic treatment](install/README.md#zmeny-pri-aktualizácii-na-202318). User profile editing cancelled, we recommend using the standard editing component [registered user](redactor/zaheslovana-zona/README.md) (#55649).
- Import from Excel - if you have a special version of import from Excel in your project that calls the URL `/admin/importxls.do` edit the form and URL according to `/admin/spec/import_xls.jsp`. The update script should solve the basics for you `/admin/update/update-2023-18.jsp` (#55905).
- Restoration menu - rebuilt from Struts to Spring, use the update script and verify the menu is displayed on the web page (#55945).

### User interface improvements

- Web pages - Blocks - increased size of the window for inserting blocks, added the ability to directly edit a web page when it is inserted by Dynamic link (#56017).
- Web Page Editor - Increased height of text/header styles selection menu for better overview/selection options (#56017).
- Applications - once again improved window size for applications - both height and width including windows when using PageBuilder. Optimized size of nested datatables. Adjustments in `top.jsp,datatables-wjfunctions.js` (#56013).
- Apps - updated photos of apps and their descriptions in the list of apps in the page editor. Fixed various minor visual bugs in app settings (#55293).
- Datatables - during a long-lasting operation, the connection to the server may be interrupted, in which case the message "An error occurred while communicating with the server. It is possible that the operation is taking too long and will be executed later. Wait and check after a while to see if the operation is executed." to distinguish it from a standard record save error.
- Datatables - added option [import only new records](redactor/datatables/export-import.md#import-dát)if the record by the selected column already exists is left unchanged (#56061).

![](admin/settings/translation-keys/dataTable-import.png)

### Website

- Import/Export - added setting of local folder IDs when creating a template. Addresses an issue when editing a page after importing, where the template is created but does not have the correct folder access set. Non-existing folders are filtered out. We recommend checking the template settings after import (#55993).
- When you create a new web page and upload an image/file/media, the Media folder for that page is already created before saving. It will be created according to the value entered in the Item Name field in the menu (#54953-29).
- When editing one web page, the insert image/link window remembers the last folder even after it is closed and does not switch back to the Media section of that page when it is reopened. This is more practical if you are inserting multiple images from a different folder. You can still switch to the Media section of this page by clicking in the tree structure (#54953-29).
- Added support for ![](redactor/webpages/recover-button.png ":no-zoom"), [Recovering web pages and folders from the Recycle Bin](redactor/webpages/recover.md) (#55937).
- Folder `/files` with a full text file index is moved to the System tab (#54953-30).
- Templates - added the ability to set the Page Editor Type (formerly Inline Structure Editor) for a template, the default is By Template Group, where the Page Editor Type is set by the assigned template group (#56129).

![](frontend/templates/templates-edit.png)

- Editor - added HTML page editor type, where the page editor is displayed directly in HTML mode (HTML code). Allows to edit special page types that may not switch to visual mode due to e.g. special code (#56129).

![](redactor/webpages/editor/html-mode.png)

- Editor - added the ability to set the size/width of the editor also in Standard mode. So you can view the page in width mode for mobile, tablet and computer (#56129).
- Editor - unified Standard/Page Builder mode switching (#56129).
- Ninja - added support for additional browsers to define the minimum supported version in `config.properties` type `minBrowserVersion.crazy\u0020browser=10` (#56129).
- For pages with display disabled, also adjusted page title color to red (#55997).
- Editor - modified Paste as plain text and Paste from Word to open a dialog box to paste text from the clipboard (#56189).

![](redactor/webpages/editor/paste-word.png)

- Editor - improved cleanup of code embedded from Excel - removed cell width and CSS stylesheet class settings (#56189).
- Editor - edited inserting links to email address - incorrect display of http prefix when entering email address (#56189).

### Page Builder

- Page Builder redesigned to use new JavaScript files identical to the ones used when editing a web page (until now, the old ones from version 8 were used). The page code is embedded directly when the page is displayed, no need to make a REST service call. Additional CSS and JavaScript files are embedded using `combine` with one call. The number of HTTP requests was reduced from 42 to 24 (#56017) when displaying a normal page.
- Loading in the editor should be faster, the new version doesn't contain old parts of the code, some objects will be reused.
- In the future, only the Page Builder version will be available in the Web page section, the separate inline editing when displaying a web page will be removed (for script consistency reasons).
- Visibility settings adjusted to Mobile, Tablet, Desktop options in concert with editor width toggle (#56017).
- Added option [generate menu](frontend/page-builder/blocks.md#podpora-menu) For `single page` types of pages. The menu is generated automatically according to the sections in the web page (#56017).

### Spring applications

- Added application name to the settings window instead of the generic name Edit (#55997).
- Spring applications have the added [View tab](custom-apps/appstore/README.md#karta-zobrazenie) for setting the application to display on different devices and the ability to set the HTML output to be cached for faster web page display. If no device is selected the application will always be displayed.

![](custom-apps/appstore/common-settings-tab.png)

### Restaurant menu

- Restaurant menu section (application) converted into date tables [Restaurant menu](redactor/apps/restaurant-menu/README.md) (#55945).

![](redactor/apps/restaurant-menu/menu-app-dialog-meals.png)

### WebJET update

Improved process [WebJET CMS updates](sysadmin/update/README.md), transferred to the new design, added accents to the changelog, display the changelog with highlighted text and images.

![](sysadmin/update/main-page.png)

### Banner system

- Added support for [Display banner for specific device type](redactor/apps/banner/README.md#zobrazenie-banneru-pre-špecificky-typ-zariadenia), i.e. displaying only on a mobile phone, or only on a tablet, for example. So you can display different banners for different devices (#55921).

![](redactor/apps/banner/banner-device-setting-tab.png)

### Forms

- Added the Company field to the list of fields of the logged in user when completing the form (#55961).

### Gallery

- Optimized initialization of the image editor - after the first initialization, the existing instance (#55853) is used.
- Optimized data retrieval - reduced the number of REST service calls from 6 to 1 when displaying the gallery (#56093).

### HTTP headers

- Added header setting option `Content-Language`, [automatic adjustment](admin/settings/response-header/README.md#nastavenie-pre-súbory) headers for files.
- Added support [header settings](admin/settings/response-header/README.md) for an exact URL match using `^/path/subpath/$` and the option to set it for certain suffixes `/path/subpath/*.pdf,*.jpg` (#56109).

![](admin/settings/response-header/editor-wildcard.png)

### Calendar of events

- Added option to create an event type with a space in the name (#56054).
- Added Cache deletion after adding a new event (#56054).

### Cloning structure

- [Cloning structure](redactor/apps/clone-structure/README.md) is integrated with [by mirroring](redactor/apps/docmirroring/README.md) for the possibility of creating translation and linking pages when cloning a language mutation. The language is taken from the source and destination folder settings (#55733).
- After the clone is created, the mirroring of changes is automatically set, if necessary you can cancel it by editing the conf. variable `structureMirroringConfig`. Changes will be transferred between the clone and the original structure.

![](redactor/apps/clone-structure/clone_structure_result.png)

### News

- To [news templates](redactor/apps/news/README.md#šablóna) added objects `$pagesAll` with the whole pagination list (`$pages` contains an abbreviated list) and `$totalPages` with the total number of pages.

### Users of

- User deletion - fixed situation when currently logged in user tries to delete himself (#55917).
- [List of users](admin/users/README.md) - added columns Directory and Site Permissions and Approvals to the table. Due to the nested data in the columns, it is not possible to search and sort by value (#55897).
- User List - added Rights Group view to display admin rights groups in a table with the ability to filter by the selected group (#55601).

### Translation keys

- Added option [import only new](admin/settings/translation-keys/README.md) (not yet existing) keys. Key existence is checked for each language (#56061).

![](admin/settings/translation-keys/dataTable-import.png)

### Redirections

- Path redirects - in a multi-domain installation, only records according to the currently selected domain and records that do not have a domain set (#55957) are displayed.

### Proxy

- Added HTTP header settings `Content-Type` By [answers from proxy](redactor/apps/proxy/README.md) (when using a proxy on a REST service/without embedding it in a web page) (#56054).

### Statistics

- The folder selection filter in the statistics view displays only the folders that the user has permissions to in the list of web pages. On the first visit, the statistics are displayed for the first allowed folder. This allows even editors with limited rights to the tree structure of web pages (#55941) to view the statistics.
- Added the Show Statistics for All Folders permission to allow display of the entire tree structure regardless of the site permissions (#55941).

### Optional fields

- Added field type `UUID` for generating [a unique identifier](frontend/webpages/customfields/README.md#unikátny-identifikátor).

![](frontend/webpages/customfields/webpages-uuid.png)

### Tags

- Tags - the list of available tags is displayed according to the rights to the website sections. Thus you can make tag administration available only for a specific tree section of a web site (or domain). Thus, if a user is only allowed access to the /Newsletter section of the website, only tags that have that section added or tags without display restrictions (#55961) will be displayed.

### Mirroring the structure

- Added the ability to add another folder ID for mirroring later, using [Cloning structure](redactor/apps/clone-structure/README.md) it is possible to add the content of the new language (#55733).
- Added synchronization settings check - if you remove the folder ID from the conf. variable `structureMirroringConfig` pages and folders in the removed structure will no longer be synchronized (#55733).
- Added ability to create in mirrored folders [pages and folders with display turned on](redactor/apps/docmirroring/README.md#priebeh-zrkadlenia) (e.g. during the page development phase), just set the conf. variable `structureMirroringDisabledOnCreate` to the value of `false` (#55733).
- Translator - [For DeepL](admin/setup/translation.md) is possible in the conf. variable `deepl_api_url` set the service API URL. The default value is for the free version, when using `Pro` versions set to `https://api.deepl.com/v2/translate`. Modified the way of authorization against the API to the new version using the HTTP header `Authorize` (#55733).

### Security

- Updated library `logback` to version 1.3.14.
- Vulnerability fixed `CVE-2022-26960` in the library `elfinder`.
- Users - Profile - added information about the quality of the password when changing it (#56077).

### System changes

- Optimized JavaScript file size - library `moment` is loaded with only the necessary languages instead of all available languages (used `MomentLocalesPlugin`). Size reduced by 300kB (#56093).
- Update - from 2023.40-SNAPSHOT onwards, the update server also has packages ready for updating WebJET running in JAR mode, the update server will provide the update file according to the mode type.

**Switching from Struts to Spring**

- WebJET update - redesigned WebJET update [WebJET update](sysadmin/update/README.md) (#55797)
- Discussion/Forum - reworked adding a new post from Struts to Spring, reworked approving (#55649).
- Site rating - redesigned [Rating/Rating](redactor/apps/rating/README.md). Class `RatingDB` substituted for `RatingService` a `RatingBean` For `RatingEntity` (#55729).
- Import XLS - URL for importing XLS files changed from `/admin/importxls.do` at `/admin/import/excel/`, use the example in `/admin/spec/import_xls.jsp` to update your files (#55905).
- Restaurant menu - redesigned at Spring, classes `MenuDB,MealDB,MenuBean,MealBean` substituted for `RestaurantMenuService,RestaurantMenuEntity,RestaurantMenuMealsEntity` (#55945).
- Web pages - redesigned cloning option from struts to Spring [Cloning structure](redactor/apps/clone-structure/README.md) (#55733).
- Web pages - redesigned importing pages from ZIP archive [Importing web pages from a ZIP archive](redactor/webpages/import-export.md#importovanie-web-stránok-zo-zip-archívu) (#55905).
- Web pages - redesigned import of pages from Excel file [Importing web pages from an Excel file](redactor/webpages/import-export.md#import-štruktúry-z-excel-súboru) (#55905).

### Error correction

- Dials - corrected pagination in dials (#56013).
- Datatables - corrected the position of the window from the nested datatable to make it fit entirely in the browser.
- Forms - fixed error displaying the date in Last Submitted in the list of forms on the Oracle database.
- Forms - fixed export of form data and duplicate export file generation (#56141).
- Gallery, File View, File Archive - fixed folder selection in application properties in page (#54953-27).
- Gallery - fixed double slash in gallery URL in some scenarios (#56017).
- Bulk email - added duplicate checking of email addresses regardless of case (#55961).
- Configuration - fixed duplicate display of conf. variable when adding an existing variable (#55733).
- Configuration - fixed import of special characters like `'&#<` and search in such value (#55993).
- Json editor - fixed setting and changing the image in applications using Json editor (e.g. Recommendations, Impressive Presentation, etc.) (#55293).
- Users - corrected display of user rights in editing - also rights by group were displayed checked, not only set ones (#55797).
- Users - Profile - fixed password change error (#56077).
- Restart - fixed Restart function in Settings menu (#54953-30).
- Scripts - fixed inserting scripts into the page - the search for the position name was incorrectly used as a sub-string and the script was inserted even if the position name didn't match exactly (#54953-30).
- `Slider` - jQuery library compatibility fixed (#55293).
- Templates - fixed display of templates that are set to display only for a certain folder and at the same time this folder has an empty domain name (#55993).
- Web pages - added support for approving web pages in no action mode (#55897).
- Web pages - remove approval records left after deleting a folder from the trash (#55897).
- Web pages - added automatic lowercase setting when entering domain (domain is entered with lowercase letters by default) (#55993).
- Web pages - fixed switching to Basic tab after adding new media in new web page (#54953-29).
- Web pages - fixed preview display if Spring is set to JSON responses by default (#56054).
- Web site - domain renaming corrected - The folder system has not changed the domain (#54953-30).
- Web pages - blocks - improved window size setting when inserting Block (#55293).
- File display - fixed file display (domain detection) (#54953-27).
- Fixed bug in sending parameters for external filters, date to value is set to 23:59:59 (#56021).

### For the programmer

- Applications - added option to show application in page [by type of device](custom-apps/appstore/README.md#podmienené-zobrazenie-aplikácie) (mobile, tablet, pc) by adding a parameter `!INCLUDE(/components..., device=mobil)!`. For [Banner system](redactor/apps/banner/README.md) setting is also added to the application settings interface.
- Datatables - added support `LocalDate/LocalDateTime` for data fields, added attribute `alwaysCopyProperties` to `DatatableColumn` for the copy option when editing from [of an existing record](developer/datatables-editor/datatable-columns.md).
- Users - supplemented usage documentation `afterSaveInterceptor` At [user registration](custom-apps/apps/user/README.md) and the option to not send a standard welcome email by overriding the method `shouldSendUserWelcomeEmail` (#54953-28).

![meme](_media/meme/2023-52.jpg)

[Full list of changes \&gt;\&gt;](CHANGELOG-2023.md)
