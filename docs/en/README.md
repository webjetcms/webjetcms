# WebJET CMS 2026

Welcome to the documentation for WebJET CMS version 2026. We recommend reading the [changelog](CHANGELOG-2026.md) and [roadmap](ROADMAP.md).

## List of changes in version 2026.18

> WebJET CMS 2026.18 brings **folder change approval** with support for multi-level approval and **accessibility testing** integrated directly into automated tests.
>
> In the page editor, you can now **maximize dialogs** when setting up applications, inserting images, or links. Application settings also gain new options for accessibility attributes (aria) and **custom CSS styles** for display.
>
> In the area of ​​security, **support for login via OAuth2/Keycloak/Google/Facebook** and **Access Keys** (PassKey/WebAuthn) have been added, along with improvements for more stable operation in a cluster environment.

!>**Warning:** Version intended for `jakarta namespace`, requires Tomcat 11 application server, uses Spring version 7. Before upgrading [check requirements](install/versions.md#changes-when-upgrading-to-jakarta-version).

### Groundbreaking changes

- Tomcat 11 application server is required, version 10 is no longer supported (#58385).
- Removed legacy applications `/components/adresar/editor_component.jsp` and `/components/sharing_icons/editor_component.jsp` which were no longer used. If you want to continue using them, please download them from [GitHub](https://github.com/webjetcms/webjetcms/tree/hotfix/2026.0-main/src/main/webapp/components) (#57409).

### Websites

- Approval - added [approving folder changes](redactor/webpages/approve/README.md) including approving folder creation, editing and deletion. The approver will receive an email with an overview of the changes and the option to approve or reject the change. Multi-level approval is also supported (#58405).

![](redactor/webpages/approve/approve-group-page.png)

- Folder approval - in the `Na schválenie` tab, sub-tabs `Dokumenty` and `Priečinky` were added for separate display of pages and folders awaiting approval (#58405).
- Folder approval - added display of a column with approval/rejection date and approver name in folder change history (#58405).
- Applications - added option to set [application display styles](redactor/webpages/working-in-editor/README.md#karta-zobrazenie). You can set, for example, the application's indentation on the page, width or different display styles, as well as information for the reader for visually impaired visitors (#osk418).

![](custom-apps/appstore/common-settings-tab.png)

- Page editor - a tab with advanced settings such as ID, title, description for readers (aria-label), and the like has been added to the [Link and Button] dialog box (redactor/webpages/working-in-editor/README.md#buttons) (#osk115).
- Ninja - added field `canonical` as [optional Q field](frontend/ninja-starter-kit/ninja-jv/page/README.md#page-information) to set the canonical URL of the page. If the field is empty, the page URL will be used. You will get the value in the template as `${ninja.page.canonical}`. Adds the parameter `page` to the URL, if it exists, to display the correct page in the news list (#OSK149, #54273-88).
- Page Builder - fixed moving the window for inserting blocks, fixed overlapping of editor mode selection, incorporated UX comments (#58353).
- Unified use of the `ImageMagick` tool for changing image sizes between the gallery and `/thumb servlet` (#osk396).
- Added support for inserting images in `webp` format, including resizing using the native library `libwebp` via `ImageIO` (#osk396).
- Added configuration variables `imageMagickCustomParams*` for [setting custom parameters](redactor/apps/gallery/README.md#custom-parameters-imagemagick) `ImageMagick` operations by operation type and image format (#osk396).
- Applications - added option to **maximize and minimize** window for **inserting applications, images, links** etc. into the page (#57409).
- Added support for automatically restoring all linked (mirrored) pages and folders from the trash when one of them has been restored (#osk423).
- Folder - added the ability to set the HTML code of a new page from the local System/Templates folder, originally the list of pages was read according to the configuration variable `tempGroupId` (#57409).
- Modified [optional fields] nomenclature (frontend/ninja-starter-kit/ninja-jv/page/README.md) for SEO values ​​(#228).
- Added an icon to move the [cursor to a hard-to-reach place](redactor/webpages/working-in-editor/README.md#inserting-text-in-hard-to-reach-places), such as behind the last SVG icon in a line and the like (#osk105).

![](redactor/webpages/working-in-editor/wjmagicline-append.png)

### Applications

Redesigned application properties settings in the editor from the old code in `JSP` to `Spring` applications. Applications automatically also get the ability to set [display on devices](custom-apps/appstore/README.md#conditional-display-applications). The design is consistent with the rest of the WebJET CMS and data tables (#57409).

- [Pre-made blocks (HTMLBox)](redactor/apps/htmlbox/README.md)

![](redactor/apps/htmlbox/editor-block.png)

- [Inquiry made easy](redactor/apps/inquiry/inquiry-simple.md)

![](redactor/apps/inquiry/inquiry-simple-tab-basic.png)

- [Map](redactor/apps/map/README.md)

![](redactor/apps/map/map-editor.png)

- [Testimonials](redactor/apps/app-testimonials/README.md)

![](redactor/apps/app-testimonials/editor-style.png)

- [Pre-built blocks](redactor/apps/htmlbox/README.md)

![](redactor/apps/htmlbox/editor-block.png)

- Video - added option to set [CSS classes for video aspect ratio](redactor/apps/video/README.md#configuration), options displayed in the application are set via configuration variables `videoClasses`, `videoWrapperClass` and `videoItemClass` (#osk496).

### Forms

- Added [Statistics] tab (redactor/apps/multistep-form/stat.md) to display form responses in the form of graphs (#58333).

![](redactor/apps/multistep-form/stat-section.png)

- Added **Completion Duration** column for multi-step forms, which shows how long it took the user to complete the form (time from viewing to submission) (#58333).

### Gallery

- Added the ability to move gallery folders directly in the editor by changing the parent folder. For details, see [structure management](redactor/apps/gallery/structure.md) (#58433).
- Improved handling of moving gallery folders to prevent invalid links. For more information, see [moving a Gallery folder](redactor/apps/gallery/structure.md#moving-gallery-folder) (#58433).
- Added support for filtering gallery folders by current and original/filename of the gallery folder in the tree structure (#58445).
- Added support for [displaying the original/file name of the folder](redactor/apps/gallery/structure.md#setting-up-the-tree-structure-display) of galleries in the tree structure. The folder name on disk may be different from the nice name specified in the gallery properties (#58445).

![](redactor/apps/gallery/jstree-settings.png)

### Document manager

- Added display of **Global Id** value for documents (#58357).
- Added the ability to select documents to display in the application using their `globalId` value in the nested table, more in the [Tab - Selected documents] section (redactor/files/file-archive/file-archive-app.md#tab---selected-documents) (#58357).

### Accessibility

- Added [automated accessibility testing](developer/testing/a11y.md) (a11y / WCAG) using `axe-core` integrated into CodeceptJS. Tests cover WCAG 2.0/2.1/2.2 AA levels (#58389).
- Improved accessibility / fixed WCAG errors on the pages:
  - Login to administration (#58389-2).
  - Introduction / item menu / header (#58389-3).
  - Improved color contrast in error messages and notifications (#58389-4).
  - Data tables, editor (#58389-4).
- Extended a11y tests with new scenarios for file manager, monitoring, statistics, file upload, user management and website. The `a11y.check()` method supports the `context` parameter to limit the inspection to a specific part of the page, including nested `iframe` elements (#58389-5).

### Other minor changes

- Gallery - added support for transparency in `png/webp/gif` images when resizing them, if [ImageMagick](redactor/apps/gallery/README.md#possible-configuration-variables) (#osk396).
- GitHub pipeline - after `merge pull request`, a social media post is automatically generated using LLM (GitHub Copilot / Google Gemini) and added as a comment to the given `pull request` along with any screenshots from the documentation (#177).
- Scripts - added option to set whether a script should be inserted in the page editor in PageBuilder mode (#58349).
- Scripts - added attribute [Insert Order](redactor/apps/insert-script/README.md) to set the order of inserting scripts within the same position. The default value for existing scripts is 10, for a new script it is automatically set to the highest value + 10 (#osk387).
- Page editor - fixed specifying the folder for uploading images/files for a new/not yet saved web page with a duplicate name: the folder now corresponds to the actual URL address of the page including the extension `-2`, `-3` etc (#58361).
- Structure mirroring - added option to generate [links to language mutations](redactor/apps/docmirroring/README.md#hreflang-attribute-setting) in the page header using the `hreflang.jsp` application, links contain the `hreflang` attribute for better SEO optimization of language versions (#58357).
- Tree structure - added automatic scrolling to the currently selected element in the tree structure (#58433).
- Applications - added the ability to hide fields/tabs in the application editor using the `appHideFields` configuration variable. For more information, see [Hiding fields/tabs](custom-apps/appstore/README.md#hiding-fields-tabs) (#58433).
- `imageradio` - ​​added option to set field type `imageradio` as `disabled` (#58333).
- The design of all charts throughout the project has been redesigned from dark to light mode for better design consistency (#58333).

![](developer/frameworks/charts/frontend/line-chart.png)

- Application `Tooltip` renamed to Help (#205).
- Traffic statistics - modified filtering by folder. The user can select any folder even from other domains if they do not have limited folder rights or if they have the right **Show statistics for all folders** (#58453).
- Users - modified the ability to see all folders in the **Directory and Page Rights** setting if the administrator has the **Manage Administrators** right. Folders are displayed even if the administrator himself has limited folder rights (#58453).

### Bug fixes

- `imageradio` - ​​fixed display of field type `imageradio` in datasheet editor (#58333).
- Websites - fixed restoring a page that was created via mirroring and did not have a historical record that could be used for restoration (#208).
- Web pages - when restoring a page from the trash, the historical version is used to preserve its original state before deletion (#208).
- Button - fixed the ability to set button properties which is disabled (attribute `disabled=disabled`) (#209).
- Photo gallery - fixed folder search (#58433).
- Files - added preservation of `.min.js` and `.min.css` in the file name when uploading to `/files,/images` folders (#213).

### Performance

- Modified `PkeyGenerator` for cluster mode `auto`. Method `allocate` wrapped in transaction (`setAutoCommit(false)`) with retry at `deadlock`. Improved block acquisition also in cluster mode `auto`, reducing the number of database accesses and the risk of `deadlock`. Incrementing and reading the value of `pkey_generator` is done in a single atomic SQL query, which prevents the allocation of the same block to multiple cluster nodes. Database-specific SQL statements are used (#213):

| Database | SQL command | Minimum version |
| --- | --- | --- |
| MySQL | `LAST_INSERT_ID(expr)` | 3.23+ |
| MariaDB | `LAST_INSERT_ID(expr)` | 5.1+ (all GAs) |
| Microsoft SQL Server | `UPDATE ... OUTPUT inserted` | 2005+ |
| PostgreSQL | `UPDATE ... RETURNING` | 8.2+ |
| Oracle | `RETURNING value INTO` | 10g+ |

- `SeoManager` and `ClusterRefresher` tolerate database `deadlock` during `UPDATE/DELETE` operations without error log (#213).
- Cluster mode `auto` - ​​added configuration variable `clusterAutoRandomDelay` for [random start delay](install/config/README.md#auto-mode) of some tasks to reduce the risk of concurrent database conflicts between cluster nodes (#213).

### Safety

- Added file `.github/SECURITY.md` with [responsible vulnerability disclosure](sysadmin/responsible-disclosure/README.md) (#187).
- Updated libraries `AspectJ, Eclipselink, slf4j, GoPay` (#57793).
- Version `SpringSecurity` upgraded to version 7 (#56665).
- Added the option to log in via [OAuth2/Keycloak/Google/Facebook...](install/oauth2/oauth2.md) (#56665).

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/q8xs3qDq-G4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

- Removed unused libraries `lodash,pdfmake`, updated list `dependency-check-suppressions`, fixed initial installation (#204).
- Fixed invalid password handling during API/BASIC authentication after transitioning to `Spring Security 7` so that the `password encoder` error does not turn into an internal exception and the request is correctly rejected (#58369).
- Added support for logging into the administration via [Access keys](redactor/admin/logon.md#use-access-key) `PassKey/WebAuthn` (#58369).

![](redactor/admin/passkey-logon.png)

- Users - if you enter the character `*` in the password field when creating or editing a new user, a new secure password will be generated and displayed in a notification (#58369).
- [Permission Groups](admin/users/perm-groups.md) - added the option to set the flag **Access to all web directory** and **Access to all file system folders**, which will overwrite the accumulated rights from other groups upon login and provide the user with unlimited access to web pages or files (#osk422).
- Updated library `Datatables.net/Editor` from version `2.2.2/2.3.2` to `2.3.7/2.5.2` (#206).

### Documentation

- Modified automatic translator to use Google Translator API v3 (#58301).
- English version of documentation re-translated (#58301).

### For the programmer

- AI - new `AI skill` to fix A11Y/WCAG errors, just use the `/wj-accessibility` tool.
- Updated dependencies to minimum requirements for Tomcat 11 (Tomcat 10 is no longer supported). `Stripes` validations - modified execution of EL expressions from removed `jakarta.servlet.jsp.el` to `jakarta.el` for compatibility with `jakarta.servlet.jsp-api:4.0.0` (#58385).
- Updated the way API documentation is displayed to the [OpenAPI 3.0](https://www.openapis.org/) standard. The documentation is available at `/admin/swagger-ui/index.html` for users with administrator editing rights (#57793).
- Administration - changed `build` administration files from `webpack` to `rspack`, which is significantly faster (#206).
- Administration - unified generation of `PUG` templates for `watch` and `prod`, removed unused `npm` build dependencies and historical `webpack` scripts (#206).
- Administration - added automatic refresh of the open page at `npm run watch` after changing `JS/CSS/PUG` files (#206).
- Administration - added script `npm run analyze` with HTML report of the size of used libraries (#206).
- Datatables - added a new event type `DatatableColumnsEvent`, which can be listened to and dynamically modified before table initialization. Learn more in the [DatatableColumnsEvent event](developer/backend/events-datatable.md#datatableColumnsEvent event) (#58433).
- Added `Jackson v3` library, some JSON objects may not serialize correctly unless they have the correct `Java Bean` name (e.g. `setcookieId` without the capital `C`, or `set__rowNum__`). The best solution is to set the variable name correctly, or use the `@JsonProperty("__rowNum__")` type annotation on `getter/setter` too (#58369).
- Gallery - modified ImageMagick library call, changed API for its call to `ImageTools.executeImageMagick(...)` (#osk396).
- Charts - the tool/library [chart-tool.js](../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js) for working with `amcharts` charts has been updated, new functionalities, new charts and improved logic (#58333).
- Charts - added new class/library [stats-by-charts.js](../../src/main/webapp/apps/_common/charts/stats-by-charts.js) for quickly creating entire sections of statistics using [chart-tool.js](../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js) for creating charts (#58333).
- Header tabs - added sub-tab support via `WJ.headerSubTabs()` function for nested tabs in the Disapproved list in websites (#58405).
- Removed `@Temporal` annotation on date columns in database entities, recommended solution is to use `java.time.*` types for new entities. Eclipselink/JPA seems to correctly detect `Date` type as date and time and the annotation is not needed. We recommend checking the behavior of date fields after the update (#57793).
- The `PageListHolder/MutableSortDefinition` class is `Deprecated` in Spring 7, as a direct replacement you can use our `PagedListHolder/SortDefinition` implementation from package `sk.iway.iwcm.system.datatable` (#57793).
- Websites - restore from trash - added [event publishing](developer/backend/events.md) `ON_RECOVER` and `AFTER_RECOVER` for restoring pages and folders from trash (#161).
- Websites - added the ability to edit the tabs in the [Style when using PageBuilder](frontend/page-builder/blocks.md#support-javascript-code) window by calling the `window.pbBuildTabMenu` function. This allows you to display only relevant tabs and block settings for the customer (#58345).
- Websites - added option to call [custom function for cleaning HTML code](frontend/page-builder/blocks.md#supporting-javascript-code) after inserting from `Microsoft Office` or when retrieving HTML code (#OSK49).
- `WebjetEvent` – added the ability to set a user of type `Identity` directly into the event. Suitable for processing events where a user is needed, but `context` or `request` is not available (#OSK423).
- `WJ.openIframeModal` - ​​added the ability to move the dialog box by grabbing the header (drag & drop), maximize/minimize the window and define custom buttons in the footer via the `buttons` parameter (#58405).

![meme](_media/meme/2026-18.jpg ":no-zoom")