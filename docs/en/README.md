# WebJET CMS 2025

Welcome to the documentation for WebJET CMS version 2025. We recommend to read [list of changes](CHANGELOG-2025.md) a [roadmap](ROADMAP.md).

## List of changes in version 2025.40

> **WebJET CMS 2025.40** brings an integrated **AI Assistant** which makes working with content much easier. Allows you to automatically **correct grammar**, **translate** texts, design headlines, summarize articles and generate **illustrative pictures** directly in the editor. This makes content creation **faster, more accurate and more creative** than ever before.
>
> Significant changes also concern **brands** a **news templates** that have been reworked into **separate database tables** with support for separation by domain. This brings higher **clarity, easier administration** and the ability to efficiently customize content for multiple sites. The user environment was **optimised for smaller screens** - the system automatically adjusts the window display and maximises usable space.
>
> On a technical level, the obsolete Struts Framework has been removed. This makes WebJET CMS more powerful, more stable, **Safer** and ready for further development of modern web solutions.

### Groundbreaking changes

- Removed from `Struts Framework`, you need to make an update `JSP` files, Java classes and edit file `web.xml`, more in [section for the programmer](#for-the-programmer) (#57789).
- If you are using Tomcat application server version 9.0.104 and above, you need to [update settings](install/versions.md#changes-when-switching-to-tomcat-90104) parameter `maxPartCount` at `<Connector` element (#54273-70).
- Tags - split by domains - at startup, a copy of tags for each domain is created (if splitting data by domains is used - configuration variable set `enableStaticFilesExternalDir=true`). Tag IDs for the website and gallery are being updated. You need to manually check the tag IDs for all news apps and other apps that contain tag IDs - the update will try to fix them, but we recommend checking the IDs. See the section for the programmer for more information. (#57837).
- News - [news templates](frontend/templates/news/README.md) converted from a definition via translation keys to a custom database table. When WebJET starts, the records are converted from the original format. They are separated by domain, if they contain a domain alias they are created only in the corresponding domain (#57937).
- Security - stricter control of administration URLs - it is necessary that the URL in the administration has a character at the end `/`, the incorrect address is `/admin/v9/webpages/web-pages-list` or `/apps/quiz/admin`, correct `/admin/v9/webpages/web-pages-list/` or `/apps/quiz/admin/`. It is necessary for the programmer to check the URL definitions in the files `modinfo.properties` (#57793).

### AI Assistant

In today's world, artificial intelligence is all around us and of course, WebJET as a modern content management system does not want to be left behind. That's why we are proud to present the new version of WebJET CMS, where we have integrated [advanced AI tools](redactor/ai/README.md).

![](redactor/ai/datatables/ckeditor-assistants.png)

These features make it easy to create and edit content - from correcting grammar, to translating text, to designing captions, to generating illustrative images.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/LhXo7zx7bEc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

### Web pages

- AB Testing - added option [show AB version](redactor/apps/abtesting/README.md) according to the status of the logged-in user - the non-logged-in user will see the A version and the logged-in user will see the B version. You activate the mode by setting the configuration variable `ABTestingForLoggedUser` to the value of `true` (#57893).
- [Page Builder](redactor/webpages/pagebuilder.md) - modified visual to better fit the current WebJET CMS design (#57893).

![](redactor/webpages/pagebuilder-style.png)

- Allowed to display pages containing `404.html` in the URL from the system folders, so that such a technical page does not get in the way of your standard web pages (#57657-8).
- Tags - split tag display by the currently selected domain, so you can have tags separately for each domain in WebJET (#57837).
- Cloning structure - added information about the configured translator and how many free characters are left for translation (#57881).
- Structure mirroring - option to delete added `sync_id` values for the selected folder (recursive). Make it easy to undo/redo page mirroring (#57881).

![](redactor/apps/clone-structure/clone_structure_set_translator.png)

- Mirroring - adding a new section [mirroring](redactor/webpages/mirroring/README.md) to track and manage linked folders and pages after mirroring actions (#57941).

![](redactor/webpages/mirroring/groups_datatable.png)

- When selecting an image or video file in the page editor, only the appropriate file types are displayed in the explorer, the others are filtered (#57921).

### Templates

- New section added [News templates](frontend/templates/news/README.md) to manage and administer newsletter templates (#57937).

![](frontend/templates/news/news-temps-datatable.png)

### User interface

- When using a small monitor (window height less than 760 pixels), the window is automatically displayed full screen and the header and footer are shrunk (the window title is in a smaller font). This will increase the amount of information displayed, which is especially needed in the web page section. This is used for windows using the CSS class `modal-xl`, which are the actual website, photo gallery, image editor and users (#57893).

![](redactor/webpages/pagebuilder.png)

- Added option in the editor to click on the image icon at the beginning of the field to display it in a new tab.

![](developer/datatables-editor/field-type-elfinder.png)

### Applications

- Added option to show the application only to logged in/not logged in user. Mode is set in the tab [View application settings](redactor/webpages/working-in-editor/README.md#tab-view) in the site editor (#57893).

![](custom-apps/appstore/common-settings-tab.png)

Redesigned application properties settings in the editor from the old code in `JSP` at `Spring` Application. Apps also automatically get the ability to set [display on devices](custom-apps/appstore/README.md#conditional-application-view). The design is consistent with the rest of the WebJET CMS and data tables (#57409).
- [Carousel Slider](redactor/apps/carousel_slider/README.md)
- [Emoticons](redactor/apps/emoticon/README.md)
- [Forum/Discussion](redactor/apps/forum/README.md)
- [Questions and answers](redactor/apps/qa/README.md)
- [Users](redactor/apps/user/README.md)
- [Impressive presentation](redactor/apps/app-impress_slideshow/README.md)
- [Restaurant menu](redactor/apps/restaurant-menu/README.md)
- [Slider](redactor/apps/slider/README.md)
- [Slit slider](redactor/apps/app-slit_slider/README.md)
- [Social icons](redactor/apps/app-social_icon/README.md)
- [Video](redactor/apps/video/README.md)

![](redactor/apps/app-slit_slider/editor-items.png)

### Menu

- If [website menu](redactor/apps/menu/README.md) does not have a root folder specified (the value is set to 0), the root folder for the currently displayed web page is automatically used. This is convenient if you are displaying menus in multiple languages where each is a root folder - you don't need to have menus/headers for each language separately, just one common one (#57893).

### Statistics

- In the sections [traffic](redactor/apps/stat/README.md#traffic) added summary number of Views, Visits and Number of different users for easy overview of total traffic for the selected period (#57929).

![](redactor/apps/stat/stats-page.png)

- In the sections [erroneous pages](redactor/apps/stat/README.md#erroneous-pages) added filtering by bots (applied only to newly recorded data) and sum count in footer. Need to edit the page `404.jsp` in your project by adding an object `request` to call `StatDB.addError(statPath, referer, request);` (#58053).

![](redactor/apps/stat/error-page.png)

### Optional fields

- Added support for new types [optional fields](frontend/webpages/customfields/README.md):
  - [Choosing a website folder](frontend/webpages/customfields/README.md#selecting-a-website-folder) (#57941).
  - [Choosing a website](frontend/webpages/customfields/README.md#choice-of-website) (#57941).

![](frontend/webpages/customfields/webpages-doc-null.png)

Security![](frontend/webpages/customfields/webpages-group-null.png)

### Fixed a possible vulnerability in Safari with a special URL pointing to a file archive combined with a nice 404 page (#57657-8).
- Other minor changes

### Change Audit - Search - the Type field is alphabetized (#58093).
- Ecommerce - added option to set&#x20;
- root folder[ with the list of products using the configuration variable ](redactor/apps/eshop/product-list/README.md) if the automatic search by the inserted product list application (#58057) is not satisfied.`basketAdminGroupIds`Ecommerce - application for setting up payment methods moved from folder&#x20;
- &#x20;to standard `/apps/eshop/admin/payment-methods/` (#58057 `/apps/basket/admin/payment-methods/`Ecommerce - when an order is deleted, its items and payments are also deleted from the database (#58070).
- Server monitoring - current values - added database server type (MariaDB, Microsoft SQL, Oracle, PostgreSQL) (#58101).
- Translator - at the translator&#x20;
- &#x20;the handling of returned error messages has been improved, to identify the problem more accurately (#57881 `DeepL`Translator - added support for implementing multiple translators and their automatic processing/use (#57881).
- Translator - added automatic&#x20;
- auditing the number of characters consumed[ in every translation. In the audit record type ](admin/setup/translation.md) shall be entered in the column `TRANSLATION` records the amount of credits consumed during the translation. The number of available characters is also audited, the result is cached and updated again in 5 minutes at the earliest (#57965).`EntityID`Explorer - optimized loading, fixed duplicate library reading&#x20;
- &#x20;(#57997).`jQuery UI`Error correction

### Data Tables - corrected options setting in the external filter selection menu (#57657-8).
- Cloning structure - fixed validation of specified folder ids and added error message (#57941).
- Gallery - added support for selecting a gallery folder, in the Gallery application in a web page, when using domain aliases, and editing a gallery entry with a domain alias (#57657-11).
- Web pages - fixed displaying the list of pages when displaying folders as tables (#57657-12).
- Charts - fixed displaying a large number of legends in charts, automatically use scrolling in legends (#58093).
- Documentation

### Added documentation for setup and use&#x20;

- two-step verification/authorisation[ (#57889).](redactor/admin/logon.md#dvojstupňové-overovanie) For the programmer

### Cancelled class&#x20;

- which was used in imports from `ImportXLSForm` format in `XLS` spec/import\_xls.jsp[. Technically the class is not needed, just delete the reference in the JSP and modify the form to a standard HTML form (#57789).](../../src/main/webapp/admin/spec/import_xls.jsp) Improved update script&#x20;
- &#x20;for File Archive - can update standard changes and add necessary changes to your version `/admin/update/update-2023-18.jsp` and auxiliary classes (#57789).`FileArchivatorBean`Class&#x20;
- &#x20;replaced by an object `org.apache.struts.action.ActionMessage`, class `String` Replaced by `ActionMessages` (#57789).`List<String>`Cancelled framework&#x20;
- , tags `Struts` substituted for the corresponding `<logic:present/iterate/...`, beware `<iwcm:present/iterate/...` For `<bean:write`.`<iwcm:beanWrite`In the Java code, due to the removal of&#x20;
- &#x20;the following changes:`Struts` substituted for&#x20;
  - `ActionMessage` substituted for `String`
  - `ActionMessages` Returns `List<String>`
  - `BasicLdapLogon.logon` instead of `List<String>` Replaced by `ActionMessages`
  - `org.apache.struts.util.ResponseUtils.filter` Amcharts - added support for specifying a function to transform text in category labels for a chart type `sk.iway.iwcm.tags.support.ResponseUtils.filter`
- &#x20;(#58093).`PIE`Amcharts - added support for specifying a function to transform the text in the legend of a chart type&#x20;
- &#x20;(#58093).`LINE`Amcharts - added option to hide tooltip when value is&#x20;
- &#x20;or `null` in the chart type `0` (#58093).`LINE`You can use a script to convert both JSP and Java files&#x20;

. If you specify a value as the path `/admin/update/update-2023-18.jsp` replacement shall also be made in `java` files. The problem is running the project if it contains errors. But you can folder `../java/*.java` renamed to `src/main/java` to run a clean WebJET. You can then use the update script. This scans and updates the folder `src/main/java-update` Also `../java/*.java`.`../java-update/*.java`In the file&#x20;

&#x20;initialization is no longer required `WEB-INF/web.xml`, delete the entire `Apache Struts` a section containing `<servlet>` a `<servlet-class>org.apache.struts.action.ActionServlet</servlet-class>` Containing `<servlet-mapping>`.`<servlet-name>action</servlet-name>`Split tags by domain (if the configuration variable is set&#x20;
- ), so that you can easily have separate tags for each domain. When WebJET starts, it copies the existing tags for all defined domains. It will skip tags that are set to display only in a specific folder, where according to the first folder it will set the domain for the tag. Updates the tags for News, that is, for the application `enableStaticFilesExternalDir=true` where it searches for the expression `/components/news/news-velocity.jsp` a `perexGroup` where it tries to update the tag ID according to the domain of the web page. The information is written to the history and a record is created in Audit detailing how the `perexGroupNot` replaced, example:`INCLUDE`For the first&#x20;

```txt
UPDATE:
id: 76897

news-velocity.jsp - update perexGroups+perexGroupsNot for domainId, old code::
INCLUDE(/components/news/news-velocity.jsp, groupIds="24", alsoSubGroups="false", publishType="new", order="date", ascending="false", paging="false", pageSize="1", offset="0", perexNotRequired="false", loadData="false", checkDuplicity="true", contextClasses="", cacheMinutes="0", template="news.template.dlazdica-3", perexGroup="625", perexGroupNot="626")
new code:
INCLUDE(/components/news/news-velocity.jsp, groupIds="24", alsoSubGroups="false", publishType="new", order="date", ascending="false", paging="false", pageSize="1", offset="0", perexNotRequired="false", loadData="false", checkDuplicity="true", contextClasses="", cacheMinutes="0", template="news.template.dlazdica-3", perexGroup="", perexGroupNot="")

INCLUDE(/components/news/news-velocity.jsp, groupIds="24", alsoSubGroups="false", publishType="new", order="date", ascending="false", paging="false", pageSize="1", offset="0", perexNotRequired="false", loadData="false", checkDuplicity="true", contextClasses="", cacheMinutes="0", template="news.template.dlazdica-3", perexGroup="3+645", perexGroupNot="794")
new code:
INCLUDE(/components/news/news-velocity.jsp, groupIds="24", alsoSubGroups="false", publishType="new", order="date", ascending="false", paging="false", pageSize="1", offset="0", perexNotRequired="false", loadData="false", checkDuplicity="true", contextClasses="", cacheMinutes="0", template="news.template.dlazdica-3", perexGroup="1438+1439", perexGroupNot="1440")
```

&#x20;tags with ID 625 and 626 were removed because they do not show up in the folder/domain - they were set to show only for a specific folder. In the second `INCLUDE` the signs have been changed `INCLUDE` on the newly created `3+645` a `1438+1439` For `794`.`1440`| perex\_group\_id | perex\_group\_name | domain\_id | available\_groups |
| -------------- | -------------------- | --------- | ---------------- |
| 3 | next perex group | 1 | NULL |
| 645 | deletedPerexGroup | 1 | NULL |
| 794 | calendar-events | 1 | NULL |
| 1438 | next perex group | 83 | NULL |
| 1439 | deletedPerexGroup | 83 | NULL |
| 1440 | calendar-events | 83 | NULL |

Before the update was triggered, only records existed in the database&#x20;

which set `3, 645 a 794`. Records `domain_id=1` arose during the update for `1438, 1439 a 1440`.`domain_id=83`Data tables - added support for editing&#x20;
- local JSON data[ (#57409).](developer/datatables-editor/field-datatable.md#lokálne-json-dáta) Data tables - added extension&#x20;
- Row Reorder[ for the ability to arrange the list using the function ](https://datatables.net/extensions/rowreorder/) (#57409).`Drag&Drop`Datatables - Added setting option&#x20;
- Footers for sum of values[ (#57929).](developer/datatables/README.md#pätička-pre-súčet-hodnôt) Applications - added the ability to use local JSON data to set application items, for example items for&#x20;
- impressive presentation[ (#57409).](redactor/apps/app-impress_slideshow/README.md)

![](redactor/apps/app-impress_slideshow/editor-items.png)

![meme](_media/meme/2025-40.jpg ":no-zoom")
