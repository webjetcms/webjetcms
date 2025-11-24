# ROADMAP

List of planned versions of WebJET CMS. The versions are numbered as `rok.týždeň` which is also the expected time of availability of that version. Tasks marked with a ticket number are already in progress, tasks marked with a `[x]` are already implemented, the tasks marked with the `+` were added after the preparation `roadmapy` and affect the delivery of other tasks.

Please note: this is a roadmap, new tasks may be added to the version depending on needs and capacity, and some may be added to a later version.

Explanation of the pictograms used:
- [ ] the task is on schedule
- [ ] if it has already started working on the task (#ticket-id)
- [x] the job is done
- [ ] +if it starts on a character `+` the task was added to the list after the initial creation of the schedule, such tasks are typically requested by customers and will affect the schedule if implemented. This means that some other tasks may not be implemented in a given version due to lack of time.
- [ ] ~~crossed out~~ task moved to another version, or solved in a different way, or cancelled. The description always includes the version number where it was moved or the reason for cancellation.

## 2025

- [ ] Switch to `Jakarta EE` - change Java packages from `javax.servlet` at `jakarta.servlet`, prepare migration script (#57793).
- [ ] Switching to Tomcat 11+ application server (#57793).
- [ ] Switch to `Spring` Version 7 (#57793).
- [ ] Introduce into the project the obligation to use `SonarLint` and formatting code via `.editorconfig` or `Spotless` - Example https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/build.gradle.
- [x] Primary use of the GitHub repository for development.
- [x] Cancel artifact generation on old `iwmsp.sk` repository, artifacts will only be available through [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms).
- [x] Updating the library `pd4ml` to the new version, individual sites will have to purchase the license separately, it will no longer be provided by WebJET CMS.
- [ ] Migrating the most used applications to Spring version using Thymeleaf templates.
- [x] Cancellation `Apache Struts` framework, replacement `logic:present,logic:iterate,bean:write` either for `JSTL` variant, or the implementation of similar functionality into `iwcm:present,iwcm:iterate,iwcm:beanrwite` (#57789).
- [ ] Moving JSP files, Java classes and JavaScript libraries of old version 8 to `obsolete` spring archive, which will not be a standard part of WebJET CMS. It can be used on old projects where all customer applications have not yet been updated to `Spring` versions, but without support and updates from WebJET CMS.
- [ ] Statistics - click map - restore functionality, solve responsiveness problem (separate registration by window width).
- [x] Statistics - ability to filter bots for statistics of failed pages (#58053).
- [ ] Log files - filter by installation name.
- [ ] `quill` - add the ability to set menu items including colors.
- [ ] Applications - option to purchase the application for the OpenSource version (#55825).
- [ ] Option to execute Thymeleaf code in the header/footer and maybe in the body of the web page.
- [ ] Security - add generation support `nonce` For `Content-Security-Policy` header, see e.g. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.
- [ ] Forms - add the ability to call a Java class for form validation.
- [x] Tags - filter by current domain to be the same as in other sections (#57837).
- [ ] Import users - if no password is entered, generate one (for new users), if no status is sent `available` set to `true`.
- [ ] In tests, somehow automated to check for the occurrence of `I\.waitForText\('.*?', '.*?'\);` a `I\.waitForText\(".*?", ".*?"\);` which are incorrect waits without a defined time, will cause automated tests to get stuck.
- [ ] Add an app to redirect the main page to `/sk/` or `/en/` by browser language.
- [ ] Modify the deletion of the configuration so that when it is deleted, it is set to the original value defined in `Constants` (#57849).
- [x] Gallery - when duplicating an image, enable the "Folder" change to allow us to duplicate images to a folder other than the current folder (#57885).
- [ ] Bulk email - audit changes in user groups.
- [x] Archive files - rebuild into data tables (#57317).
- [ ] Optional fields - add the option to select multiple items to link to the dial.
- [x] E-commerce - integration to the payment gateway `GoPay` (#56609).
- [ ] Add authorization option via `OAuth2`, the possibility to use `mock` server https://github.com/navikt/mock-oauth2-server or https://github.com/patientsknowbest/fake-oauth2-server (#56665).
- [ ] Authorisation via `SAML` - integrate the library [Spring SAML](https://spring.io/projects/spring-security-saml) for the possibility of authentication against `ADFS/SAML` server.
- [x] Reservations - new app for all-day reservations (#57389).
- [ ] Applications - convert the application settings dialog in the web page editor from the old JSP to a data table (#57409).
- [x] Bulk email - optimizing recipient list creation (#57537).
- [ ] +Background Tasks - option to manually run the task on `node` that has the task set, now it will run on `node` where the user is logged in.
- [ ] +Forms - disable `GET` call to `FormMail/FormMailAjax`.
- [ ] +Ecommerce - add to email `JSON-LD` Data https://schema.seznam.cz/objednavky/dokumentace/.
- [ ] +Dial, Blog, News - edit so that the selection of the type of dial or folder for news is on the left, similar to the gallery/web pages. They don't have to be tabs then but all displayed at once.
- [ ] +Translation Keys - show tree structure of translation keys for better orientation.
- [ ] +Add support for logging into administration via [PassKeys](https://passkeys.dev/docs/tools-libraries/libraries/) (#56665).
- [ ] +Photobank - add option to set file name before downloading from photobank, automatically set by search term.
- [ ] +Gallery - if I set the perex image to an image in the gallery, download from the photo library, and rename the image to an existing one in the database (if the file is deleted from the disk) it will be created in `gallery` duplicate record in the table. Additionally, other images are not renamed `o_,s_`. The thing to remember is that theoretically I can rename any, it should detect that I'm in the gallery and rename all versions.
- [x] +Configuration - add setting option `Hikari` via `poolman.xml/ENV` such as `spring.datasource.hikari.idle-timeout=30000, spring.datasource.hikari.max-lifetime=1800000, spring.datasource.hikari.connection-timeout=30000` (#54273-61).
- [ ] Datatable - fix counting of selected rows after their deletion.
- [ ] +Search Engine - set folder properties (indexing, rights) according to the parent (recursively) on first opening, if the folder has no settings in the database.
- [ ] +For a field of type `DataTableColumnType.JSON` specifically `className = "dt-tree-page-null"` add the option to define a root folder.
- [ ] +Move all `Converter` classes such as. `DocDetailsNotNullConverter` into a separate `package` where there will be no other entities to make this `Converter` can also be used in projects where a separate `JPA`.
- [ ] +Add the option to set the HTTP header name for `x-forwarded-for` and determine which of the IP addresses to use (first VS last).
- [ ] +Tables - add the option to arrange by multiple columns by clicking with the key `SHIFT`.
- [ ] +Tables - add option `hideOnDuplicate` for cards in the editor, not forgetting also the class `DataTableTab` so that it can also be set in the annotation.
- [ ] +Add the option to move to the parent folder in the Media of this page.
- [ ] +Configuration - if `cluster` add the option to set the variable only for the current node (do not save it to the database). Originally this was handled by the selection field `applyToAllClusterNodes`.
- [ ] +Enumerators - add the ability to define a field type for the string as we have in the optional fields.
- [x] +Statistics - add the total number of Views and Visits to the traffic section for an easy overview of the total traffic for the selected period (the sum of the numbers in the table). It could be added `footerCallback` - https://datatables.net/examples/advanced\_init/footer\_callback.html (#57929).
- [x] +Mirror structure - add delete option `sync_id` values for the selected folder (recursive). In order to easily cancel/reset the synchronization. Also, there is a problem that I clone `SK,DE,EN` then we'll shut down `DE` and I just want to have `EN` but the mirroring still applies changes even to the `DE` Folder (#57561).
- [ ] +Add verification to GitHub tests `autoupdate` for all supported database servers - i.e. initialize an empty database and verify all `autoupdate` and verify that they pass without error. Treat as a separate pipeline.
- [ ] Gallery - allow to change the path of the gallery (folder) and set everything related to it.
- [ ] +Configuration - add column group with value `modules` configuration variable (selection field, can have multiple values). Add the option to display also unconfigured variables (i.e. complete list). Create a tool to generate all variables by groups/modules into `md` file in the documentation for an overview of all variables.
- [ ] +If I have an unsaved page with the title Cleaning and there is already another page with the title Cleaning, the newly uploaded images will be uploaded to the Cleaning folder before saving it `upratovanie`. But when the page is saved, it gets the URL cleaning-2.html and the other images are already uploaded to the folder `upratovanie-2`. It is necessary to edit the code in `getPageUploadSubDir` so that instead of using the page title directly, it tries to get the URL for the new page and then uses that.
- [ ] +HTTP headers - extend the maximum HTTP header value size to more than 255 characters, for `Content-Security-Policy` it's undersized (#PR83).
- [x] +Integration of AI tools, helpers, assistants (#57997).
- [ ] +In profile editing, the API key is not displayed after it is generated, notifications are not transferred to the parent window.
- [ ] +Forms - modify form protection so that it is not used `document.write`.
- [ ] +Add the option to set the type `textarea` as in AI assistants and with line numbers, e.g. in scripts or elsewhere where code writing is expected.
- [ ] +News - move field `contextClasses` from the news app to the news template. Field set as `hidden` to remain functional (can be set somewhere), if empty use the value from the template. So both options must work, maybe combine both values into one list.

## 2024

- [x] Transition to Java 17 - WebJET from version 2024.0 will require Java version 17 (#54425)
- [x] Replacing a set of icons from `FontAwesome` for the orchard `Tabler Icons` (#56397).
- [x] Publishing JAR files to `Maven Central` (#43144).
- [x] New version of the Shopping Cart app/`eshop`, sample version of website, API integration to online billing systems, updated integrations to payment gateways (#56329,56385,56325).
- [x] Translation keys - option to import only non-existing keys (#56061).
- [ ] ~~`quill` - add the ability to set menu items including colors.~~~
- [ ] ~~Applications - option to purchase the application for OpenSource version (#55825).~~
- [x] Web pages - if conf. variable is enabled `syncGroupAndWebpageTitle` and one page is set as the main page for multiple folders, disable the renaming of the folder name according to the main page. Plus when the main page is in a different folder, also don't rename (#56477).
- [x] Audit - redo Changed Pages and Waiting to Publish to Datatables (#56165).
- [x] Blog - administration `bloggerov` rebuild to datatables (#56169).
- [x] Blog - comments - integrate to Discussion section, add `bloggerom` rights also on the Discussion section (#56173).
- [x] AB testing - rework to DT, use options from `news` application, make conf. variables settings according to the prefix (#56177).
- [x] Event Calendar - unapproved and event recommendation - redo to DT, use code for event list (#56181).
- [x] Editor - when you create a link to an email it automatically adds http in the window, although the link eventually inserts correctly with `mailto:` prefix (#56189).
- [x] Questionnaires - rebuild application into datatables (#55949).
- [x] +PostgreSQL - add database support (#56305).
- [x] Form easy - add options like standard forms (especially setting redirect after submit), option to send form to user (for checking) (#56481).
- [x]+Form easily - add support for wysiwyg fields (#56481).
- [x] +Delete unused and replace little used libraries (#56265).
- [x] +MultiWeb - verify functionality, add necessary rights (#56421, #56405, #56425).
- [x] +Bulk Email - add one-click unsubscribe support (#56409).
- [ ] ~~+Ability to execute Thymeleaf code in the header/footer and maybe also in the body of the web page.~~
- [x] +Replace database pool with `HikariCP` (#56821).
- [x] +Logging levels - convert to datatable (#56833).
- [x] +DBPool - transition from `Apache DBCP` at `HikariCP` (#56821).
- [x] Documentation - English translation (#56237,#56249,#56773).
- [x] +Documentation - Czech translation (#57033).
- [x] +A/B Testing - disable for browsers/bots, indicate version for Ninja object (#56677).
- [x] +Datatables - option to skip a row when importing when it is incorrect (#56465).
- [x] +Redirects - optimize getting redirects without getting the redirect code from the database a second time (#53469).
- [ ] ~~+Security - add generation support `nonce` For `Content-Security-Policy` header, see e.g. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.~~
- [x] +Gallery - add the option to resize the image.
- [ ] ~~+Forms - add option to call Java class for form validation.~~
- [x] +Indexing files - add the date of the last file change (#57177) to Perex-Start of publishing.
- [x] +Translation keys - to REST service `/rest/properties/` add the ability to filter keys by conf. variable, so that it is not possible to publicly retrieve all keys from WebJET CMS (#57202).
- [x] +Web pages - audit successful time publishing of web page, possibility to send notification to author of web page (#57173).
- [x] +Display license expiration information 2 months before expiration on the splash screen (#57169).
- [ ] ~~+Tags - filter by current domain to be the same as in other sections.~~
- [x] +Audit - add HTTP header `Referrer` to identify the original page from which the call was made (#57565).
- [ ] ~~+Import users - if no password is entered, generate one (for new users), if no status is sent `available` set to `true`.~~
- [ ] ~~+In tests, somehow automatically check for the occurrence of `I\.waitForText\('.*?', '.*?'\);` a `I\.waitForText\(".*?", ".*?"\);` which are incorrect waits without a defined time, will cause automated tests to crash.~~~
- [ ] ~~+Add an application to redirect the main page to `/sk/` or `/en/` by browser language.~~~
- [ ] ~~+Modify the deletion of the configuration so that when it is deleted, it is set to the original value defined in `Constants`.~~
- [x] +Gallery - add a URL field for the source of the image with the option to specify the address from which we got the image, automatically set when using the photo bank.
- [ ] ~~+Gallery - when duplicating an image, enable the "Folder" change to be able to duplicate images to a folder other than the current folder.~~
- [ ] ~~+Bulk email - audit changes in user groups.~~
- [ ] ~~+Archive files - rebuild into data tables (#57317).~~
- [ ] ~~+Optional fields - add the option to select multiple items for linking to the dial.~~
- [x] +Search in administration - rebuild into data table (#57333).
- [x] +Annotation - addition to the original statistics display functionality (#57337).
- [x] +Tags - add possibility to define name in multiple languages, add optional fields (#57273,#57449)
- [x] +Applications - convert application settings dialog in web page editor from old JSP to data table (#57157,#57161,#57409).
- [x] +Web pages, gallery - add the ability to search in tree structure (#57265,#57437)

## 2023.52 / 2024.0

> Version 2023.52/2024.0 is aimed at improving security, transitioning from library `Struts` at `Spring`.

- [x] Update - redesign, add option to update using spring packages (#55797).
- [x] Cloning Structure - remake to use Mirroring Structure with translation option (#55733).
- [x] Rating - Remodel to Spring (#55729).
- [x] Forum - remake to Spring (#55649).
- [x] Users - add column rights group to datatable (#55601).
- [ ] Gallery - new type for displaying a small number of photos with expansion, possibility to switch between all photos in the article (#55349).
- [ ] ~~Add authorization option via `OAuth2`, the possibility to use `mock` server https://github.com/navikt/mock-oauth2-server or https://github.com/patientsknowbest/fake-oauth2-server (#56665).~~~
- [ ] ~~Authorization via `SAML` - integrate the library [Spring SAML](https://spring.io/projects/spring-security-saml) for the possibility of authentication against `ADFS/SAML` server.~~
- [x] Web pages - add the option to restore a deleted folder so that the attribute is set correctly for web pages `available` (#55937).
- [ ] Explorer - edit to new design, update JS code elfinder (#55849).
- [x] Web pages - remember the last directory when creating links in the dialog and use it directly when adding another link (#54953-29).
- [x] Event calendar - if an event is entered in October for November when the time is shifted, it is displayed with the time shifted by one hour (#56857).
- [ ] modify the messaging between administrators from a popup window to a better user interface.
- [x] Applications - improve description of each application, update application images (#55293).
- [x] Photo gallery - optimize the amount of data loaded (#56093).
- [x] Photo gallery - image editor is initialized every time the window is opened, edit to use an existing editor (#55853).
- [x] Datatables - add Duplicate testing (#56849) to the automated test.
- [ ] Banner - add the possibility to use templates to display a banner, e.g. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template\_literals.
- [ ] Check how SEO extensions for WordPress work (e.g. https://yoast.com/beginners-guide-yoast-seo/) and propose a similar solution for WebJET CMS. Integrate something like https://github.com/thinhlam/seo-defect-checker into the editor (#56853).
- [ ] Add some data attribute to the blocks in PageBuilder with the block name and then display it in the block properties, so that it can be identified which block it is.
- [ ] On the splash screen, implement the todo mini app.
- [ ] DT - after adding a record, repage to the page where the record is located (typically the last page)
- [x] Editor - add option to display HTML editor for selected web pages, which will not be switchable to WYSIWYG mode. Used on special pages integrating different JavaScript applications or special components. Suitable editor is [Ace-code](https://www.npmjs.com/package/ace-code) (#56129).
- [x] Fulltext - folder `files` in the index web pages move to the System tab (54953-30).
- [x] +Users - prevent self-deleting (#55917).
- [x] +News - add folder rights check and show only the folders to which the user has rights (#56661).
- [x] +Applications - option to conditionally display applications by device type, implemented generically, configurable via UI for Banner (#55921).
- [x] +Statistics - limit the displayed statistics to folders that the user has the right to (#55941).
- [x] +Restore menu - rebuild the application into datatables (#55945).
- [x] +Redirects - separate redirects according to the selected domain (#55957).
- [x] +Perex groups - separate administration of perex groups according to user rights on the website (#55961).
- [x] +Banner - conditional display by mobile/tablet/desktop device (#55997).
- [x] +Page Builder - generate anchors/top menu based on inserted sections (#56017).
- [x] +Optional fields - add field type `UUID` for generating identifiers (#55953).
- [x] +Import web pages - rebuild to Spring (#55905).
- [x] +Server Monitoring - CPU - `oshi-core` not working on Windows 11 (#55865).
- [x] +Headers - option to define the file extension for which the header will be set (#56109).

## 2023.40

> Version 2023.40 modifies the user interface based on new UX suggestions, improves usability of datatables.

- [x] Migration to a higher version of Java with `LTS` support (Java 11 or ideally up to Java 17) (#54425).
- [x] Banner - add the possibility to set the directory structure of the pages in which the banner will be displayed and the possibility to define exceptions where it will not be displayed (#55285).
- [x] Headers - add a new application that allows to define any HTTP header by URL (#55165).
- [ ] ~~(move to 2024.0)modify messaging between admins from popup to better UI~~
- [ ] ~~(move to 2024.0)Applications - improve description of each application, update application images (#55293).~~
- [ ] ~~(move to 2024.0)Photo gallery - optimize the amount of data loaded~~
- [ ] ~~(move to 2024.0)Photo gallery - image editor is initialized every time the window is opened, edit to use existing editor (#55853)~~
- [ ] ~~(move to 2024.0)Datatables - add Duplicate testing to automated test.~~
- [ ] ~~(move to 2024.0)Banner - add the possibility to use templates for displaying banner, e.g. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template\_literals.~~
- [ ] ~~~(move to 2024.0)Check how SEO extensions for WordPress work (e.g. https://yoast.com/beginners-guide-yoast-seo/) and propose a similar solution for WebJET CMS. Integrate something like https://github.com/thinhlam/seo-defect-checker into the editor.~~
- [ ] ~~(move to 2024.0)Add some data attribute to the blocks in PageBuilder with the block name and then display it in the block properties so that it can be identified which block it is.~~
- [x] Dictionaries - there is no attribute in the export that the record is deleted and then after the import the record is activated (#55541).
- [ ] ~~(move to 2024.0)On the splash screen implement the todo mini application.~~
- [ ] ~~(move to 2024.0)DT - after adding a record, repage to the page where the record is located (typically the last page)~~
- [ ] ~~~(move to 2024.0)Editor - add option to display HTML editor for selected web pages, which will not be switchable to WYSIWYG mode. Used on special pages integrating various JavaScript applications or special components. Suitable editor is [Ace-code](https://www.npmjs.com/package/ace-code).~~
- [x] Publishing OpenSource/Community version of WebJET CMS on `github.com` (#55789,#55773,#55829,#55749).
- [x] +Datatabases - in web pages/gallery when setting different column ratio, buttons are displayed badly (#54953-22).
- [x] +Translations - add to Structure mirroring the possibility to translate also the page text via [DeppL](https://www.deepl.com/docs-api/html/) (#55709).
- [ ] ~~~(move to 2024.0)+Authorization via `SAML` - integrate the library [Spring SAML](https://spring.io/projects/spring-security-saml) for the possibility of authentication against `ADFS/SAML` server.~~
- [ ] ~~(move to 2024.0)+Transition from Struts framework to Spring - gradually rewrite code of all classes to Spring controllers (#55389,#55469,#55489,#55493,#55469,#55501,#55609,#55613,#55617,#55701).~~
- [x] +Rebuild the news list administration into a new design and datatable, using as much code as possible from the existing list of web pages (#55441).
- [x] +Update library `amcharts` for charts on the newer version 5 (#55405).
- [x] +Redesign the News app (#55441).
- [x] +Add development support via DevContainers (#55345).
- [x] +Server Monitoring - rebuild Application, Web Pages and SQL Queries sections into new datatables (#55497).
- [x] +Web site - approval - rebuild from `Struts` at `Spring`, to supplement the tests (#55493).
- [x] +SEO - rebuild on repositories and datatables (#55537).
- [x] +User Authorization - the original option from v8 (#55545) is missing in the administration.
- [x] +DT - record search by ID, better solution for finding a web page by docid when there are many pages in the list (#55581).
- [x] +Discussion - redo administration to datatables (#55501).
- [x] +Server Monitoring - redo Application/Website/SQL query monitoring to datatables/new graphs (#55497).
- [x] +Proxy - add support for calling REST services without embedding the output in the web page (#55689).
- [x] +Banner - add support for video files including content banner (#55817).
- [x] +Translation keys - possibility to import only one language without affecting translations in other languages (import only the specified column from Excel) (#55697).
- [x] +Media - add support for optional fields (#55685).

## 2023.18

> Version 2023.18 changes the requirement for running the solution to Java version 17. This is a major change and requires a change to the servers as well. For this reason it is implemented right at the beginning of the year.

- [ ] ~~~(move to 2023.40)Migration to a higher version of Java with `LTS` support (Java 11 or ideally up to Java 17) (#54425)~~~.
- [x] Integrate version 8 code into project, remove duplicate pages, remove option to switch to version 8, keep only new versions of pages (#54993).
- [x] Update CK Editor to a newer version (#55093).
- [x] Forms - add the archive form function (#54993).
- [ ] ~~(move to 2023.40)Banner - add option to set directory structure of pages where banner will be displayed and option to define exceptions where it will not be displayed.~~
- [x] Search - `Lucene` - add language indexing based on page language, so that only pages in the specified template language would be included in the index (#54273-34).
- [ ] ~~(move to 2023.40)Headers - add new application that allows to define any HTTP header by URL (#55165).~~
- [x] Page attributes - added option to set page attributes in the editor (#55145).
- [x] Templates - add option to merge templates (#55021).
- [ ] +Audit - add annotation to the primary key if it is different than the ID, specifically for editing translation keys the value is not audited `key` But `id` which is irrelevant.
- [x] +Gallery - The multidomain structure was used `/images/DOMENA/gallery` which is now not visible in the gallery. It needs to be added there somehow (#54953-4).
- [x] +Translations - possibility to dynamically define supported languages in templates, translation keys etc (#MR332).
- [x] +Security - server logs are available within the audit privilege, this needs to be separated into a separate privilege (#54953-5).
- [x] +Improve Oracle DB server support, test entity functionality (#54953-6).
- [x] +Web pages - when duplicating a web page, the attached media should also be duplicated (#54953-6).
- [x] +Web pages/gallery - add option to set aspect ratio for tree structure and table display (#54953-7).
- [x] +Translation keys - change editing to a tabular view where the columns contain the individual languages for the ability to edit translations in all languages at once (#55001).
- [x] +Banner - rebuild banner display statistics into new design (#54989).
- [x] +Translation keys - add checking the number of translation languages so that it doesn't fall on `fieldK` (#MR344).
- [x] +Digitizer - redesign to remove the dependency on the old version of datatables.net (#55009).
- [ ] +Fix bug in editor e.g. in gallery app - when scrolling window by grabbing the title bar App scrolls badly and then keeps tracking the cursor. You then need to click down by the OK button to turn off window scrolling.
- [x] +Proxy - redesign (#55025).

## 2022.52 / 2023.0

> Version 2022.52 is a long term list of tasks that did not have time in the more detailed 2022 plan.

- [ ] ~~~cancelled for now: upload videos using WebJET CMS in an automated way - https://dev.to/yannbertrand/automated-screen-recording-with-javascript-18he or https://www.macrorecorder.com~~
- [x] add test statistics via module `Allure` - https://codecept.io/plugins/#allure (#54437).
- [x] add `code coverage` report, e.g. via JaCoCo - https://docs.gradle.org/current/userguide/jacoco\_plugin.html (#54909).
- [ ] ~~(move to 2023.40) modify messaging between admins from popup to better UI~~
- [ ] ~~(move to 2023.40) Applications - improve description of each application, update application images.~~
- [x] Gallery - add physical file name change (including generated `s_` a `o_` images) after changing the File Name attribute in the editor (#39751-52).
- [x] +Datatabases - remember the number of records per page for each datatable (#39751-50).
- [ ] +`Command Palette` - add a command palette with integrated search like VS Code - https://trevorsullivan.net/2019/09/18/frictionless-user-experiences-add-a-command-palette-to-your-react-application/
- [ ] ~~(move to 2023.40) +photo gallery - optimize the amount of data loaded~~
- [ ] ~~(move to 2023.40) +photo gallery - image editor is initialized every time the window is opened, edit to use an existing editor~~
- [ ] ~~(move to 2023.40) +Datatatables - add testing of Duplicate function to automated test.~~
- [ ] ~~(move to 2023.40) +Banner - add option to use templates for banner display, e.g. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template\_literals.~~
- [ ] ~~(move to 2023.40) +Verify how SEO extensions for WordPress work (e.g. https://yoast.com/beginners-guide-yoast-seo/) and propose a similar solution for WebJET CMS.~~
- [ ] ~~(move to 2023.40) +Add some data attribute to the blocks in PageBuilder with the block name and then display it in the block properties so that it can be identified which block it is.~~
- [x] Consider using [HotSwapAgent](http://hotswapagent.org/) for faster development without the need for restarts (solved by rebuilding `build.gradle` for standard HotSwap support).
- [ ] ~~(move to 2023.40)+Numerals - there is no attribute in the export that the record is deleted and then after import the record is activated.~~
- [ ] ~~(move to 2023.40)On the splash screen implement the todo mini app.~~
- [ ] ~~(move to 2023.40)+DT - after adding a record to paginate to the page where the record is located (typically the last page)~~
- [x] +Complete translations of texts from pug files (e.g. gallery.pug), look for e.g. the letter á or similar, which could be found in untranslated texts.
- [x]+Add the option to keep the editor window open after saving (see https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open, closeOnComplete, https://editor.datatables.net/reference/api/submit()). Implemented with a keyboard shortcut `CTRL+s/CMD+s` (#54273-26).
- [x] `Combine` / delete data - when deleting all data in the cache to transfer correctly also to `Cluster` and there also delete as well, the actual `ClusterRefresher` deletes only the cache, not `DocDB/GroupsDB` and does not set a new time for `combime` (#54673).
- [x] +REST - add the possibility to define API keys (tokens) used instead of generated CSRF tokens for REST calls by external services (#54941).
- [ ] ~~(move to 2023.18) +Headers - add a new application that allows you to define any HTTP header by URL.~~
- [x] +Scripts - add `autocomplete` on the Script placement in template field (#54941).
- [x] +Link Check - edit the original version of Link Check to datatable and link to the list of web pages (#54697).
- [x] +Reservations and reservation objects - redo administration on datatable and Spring (#54701).
- [x] +Anketa - redo administration on datatable and Spring (#54705).
- [x] +Page Attributes - redo administration on datatable and Spring (#54709).
- [x] +Templates - add a tab with a list of websites using the template (#54693).
- [x] +Statistics - redesign to new design and charts (#54497, #54585, #54897).
- [x] +Search - `Lucene` - add Czech and English language search support (#54273-34).
- [x] +Solve problem with FontAwesome versions in administration and web pages (#39751-51).
- [x] +Banner system - add the possibility to display the campaign banner according to the URL parameter (#MR296).
- [x] +Gallery - expand editor options, add superscript/subscript, option to insert HTML code, color settings. Improve support against old editor/html code (#54857-4).
- [x] +Gallery - adjust the number of photos to the size of the window, remember the set size of images in the table (#54857-4, #market-245).
- [x] +Web pages - displaying perex groups as multi-selection field with search instead of standard `checkbox` fields (if there are many perex groups) (#market-245).
- [x] +Form easily - add field type `select` - standard selection box, add the option to enter a different value and display text (#MR298, #39751-45).
- [x] +Web pages - add display of folders as a data table with bulk operations (#54797).
- [x] +Security - add option to set HTTP header `Access-Control-Allow-Origin` (#39751-44).
- [x] +Banner system - add the ability to set client, control display rights (#38751-52).
- [x] +Banner system - add possibility to set optional fields, refactor `BannerEntity` a `BannerBean` into a single object so that the new fields can be used in the JSP component (#39751-52).

## 2022.40

> Version 2022.40 is aimed at improving security.

- [x] Users - add password history check when saving a user, because now it allows it, but you can't log in with it (#54293).
- [x] User Groups - add a tab with a list of users in the rights group (#54493).
- [x] Update NPM modules - https://www.npmjs.com/package/npm-check-updates or https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version (#54721).
- [x] Change `hash` the function of passwords on `bcrypt` (or something more modern - https://cheatsheetseries.owasp.org/cheatsheets/Password\_Storage\_Cheat\_Sheet.html) (#54573).
- [x] Add/verify edit rights control for configuration variables (edit all variables function) (#54749).
- [x] Validations - add better validations on fields (e.g. url address, domain) with descriptive error messages and edit `DatatableExceptionHandlerV2` to be able to use translation keys directly from WebJET CMS with additional data (like min/max value and other macros in translation key) (#54597).
- [x] Logging - transition from `log4j` to standard `slf4j` - solved by switching to `logback` ().
- [x] Logging - add a system to temporarily write logs to RAM memory for the ability to view the latest logs via administration if the logs are not accessible on the file system. Possibility to get also logs from cluster nodes e.g. via temporary write to data `conf` Tables (#54269).
- [x] Add update and database checks as in WebJET 8 + ~~opening the new version help~~ (there is a link to the changelog in the introduction) (#54457) to the home page.
- [x] Modify the visual of the non-compliant password and password changes on WebJET 9 (at least replace the background image) (#54573).
- [x] Security - add administration access control - currently only REST service calls are controlled, not calls `Thymeleaf` site (#54649)
- [x] Datatable - add the name of the item to the delete dialog, it is necessary to think about the situation when multiple rows are selected (#54753).
- [ ] ~~~(move to 2023.18) +Migration to a higher version of Java with `LTS` support (Java 11 or ideally up to Java 17) (#54425).~~~
- [ ] ~~(move to 2023.18) +Forms - add archive form function.~~
- [x] +Web pages - add preview function before saving (#54557).
- [x] +Web site - add option `drag&drop` pages to the directory structure (#54513).
- [x] +Prepare a sample application for the programmer in the administration where it will be possible to select some options in the form and upload a file that will be processed by the server. Then the result (#54449) will be displayed.
- [x] +Prepare a sample template using Thymeleaf and possibly PUG files with integration to PageBuilder (#54345).
- [x] +Redesign the Bulk Email-Sent Emails application to the new design (#54377).
- [x] +Add CZ and EN translations (#53805).
- [x] +Add a tab to edit a user group with a list of users in that group, edit the list of web pages for server paging (#54993).
- [ ] ~~(move to 2023.18) +Banner - add option to set directory structure of pages where banner will be displayed and option to define exceptions where it will not be displayed.~~
- [x] +Add remembering the ordering method for each datatable (#54513-22).~~
- [x] +Add X to close the window and to the maximize icon.
- [ ] ~~(move to 2022.52) +Templates - add tab with list of web pages using template (#54693).~~
- [x] +Web pages - multiple folders can be selected with CTRL, but not all folders can be deleted this way (only the first one is deleted) (#54797).
- [x] +Datatabases - dynamically determine the number of columns in the table so that there is no empty space, which evokes that there are no more records (the user does not notice that there is pagination) (#54273-26).
- [x] +Datatabases - add the ability to use multiselect in the datatable and in the application in the editor (#54273-25).
- [x] +Events calendar - redesign the app (#54473).
- [x] +Web pages - add the ability to display folder ID and order of arrangement in the page tree structure (#54513-1).
- [x] +Web pages - add option to save page as A/B test (#54513-2).
- [x] +Formulas - add export option since last export, selected records, according to current filter (#54513-3)
- [x] +Generate `POM` file with dependencies/libraries directly from the gradle version, instead of editing the original WJ8 version (#54673).
- [ ] ~~~(move to 2022.52) +Scripts - add `autocomplete` in the Script location in template field.~~~
- [ ] ~~(move to 2022.52) +Link Check - edit the original version of Link Check to datatable and link to the list of web pages (#54697).~~
- [ ] ~~(move to 2022.52) +Reservations and reservation objects - redo administration on datatable and Spring (#54701).~~
- [ ] ~~(move to 2022.52) +Anketa - redo administration on datatable and Spring (#54705).~~
- [ ] ~~(move to 2022.52) +Attributes page - redo administration on datatable and Spring (#54709).~~
- [x] +Scrolling editor window (#54513-21).
- [x] +Remember the order of the columns in the table for each table (#54513-22)
- [x] +Remember the last opened folder in the list of web pages (#39751-45)

## 2022.18

> Version 2022.18 focuses on usability improvements.

- [x] editor - display the page in inline editing (if the inline editor is set to Page Builder) (#54349).
- [x] DT in client pagination marking all lines pretends to mark on one page but deletes everything on the other pages (#54281).
- [x] Add a button to notify that there is a version in progress to open (#54357).
- [x] Add column with icons to display web page (#54257).
- [x] Remake DocID/web page to ID in web pages (#53805).
- [x] Home - add application to the right column with the list of Favorite URLs (#54177)
- [x] Introduction - add a simple form to send comments to WJ (#54181)
- [x] Home - make icons in the first block (visits, forms...) clickable (#53805)
- [x] Add option to set two-factor authorization by administrator (#54429)
- [x] For WJ Spring components map `editor_component`. It's currently looking for jsp in `INCLUDE` and for these components there is e.g. `sk.iway.xxx.component.SiteBrowserComponent` and so he doesn't find it and returns `appstore` (#54333).
- [x] Web pages - add option to change domain after entering `docid` if the page is in another domain and automatically switch the bookmark to System or Trash if it is in this branch (#54397).
- [x] Translation keys - add button to restore keys from `properties` file, restore keys after deleting a record and not display delete error if the key does not exist (because it is from properties file - id greater than 1000000) (#54401).
- [x] DT Editor - add notification when leaving the page (#54413).
- [x] Optional fields - add the option to select a directory (#54433).
- [ ] ~~+(move to 2022.52) DT - after adding a record, repage to the page where the record is located (typically the last page)~~
- [x] +Tune version for Firefox, add automated testing for Firefox as well (#54437).
- [x] +After switching to the WJ8 version, the links to add redirects (and maybe others) are missing (#53805).
- [x] +Add icon to WJ8 interface to switch to V9 version (#53805).
- [ ] ~~+(move to 2022.52) Add translations of texts from pug files (e.g. gallery.pug), look for e.g. the letter á or similar, which could be found in untranslated texts.~~
- [x] +When importing the configuration with update by name it falls on error, it would be advisable to replace it with the original import with the preview of changes.
- [ ] ~~+ (move to 2022.36) Prepare a sample application for the programmer to use in the administration where it will be possible to select some options in the form and upload a file that will be processed by the server. Then the result (#54449) will be displayed.~~~
- [x] +Update Java libraries to the current version without vulnerabilities.
- [x] +Add option to use `Thymeleaf` templates for `@WebjetComponent` (#54273).
- [x] +Establish an instance with a public git repository (#54285).
- [ ] ~~+(move to 2022.52) Add option to keep editor window open after saving (see https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open).~~
- [x] +In the menu of the template section show template editing items directly on the first level (#54345)
- [x] +Add option to enter `placeholder` in the Form Easy application (#54381).
- [x] +Verify running WebJET CMS over Oracle and Microsoft SQL (#54457).

## 2021.52

> Version 2021.52 is aimed at improving the work with web pages and the editor.

### Web pages

- [x] editor - added info icons for each field with explanation of their meaning (#54253)
- [x] editor - complete TODO items in the web page editing in the Access tab - set up navigation bar and sitemap display (data model extension) (#54205)
- [x] editor - modified URL field according to the original UI design (only the end part is displayed, with the option to toggle the display of the whole URL) (#54237)
- [x] editor - show page approval information (after saving, when it should be approved) (#54129)
- [x] editor - view work in progress (#54129)
- [x] ~~editor - add option Save and Save As~~ - replaced by Duplicate Datatables property
- [x] web pages - display web pages also from subdirectories (#52435)
- [x] Web pages - directories - show domain entry field only for root folders, automatically set domain when saving root folder (#54249)
- [x] Web pages - add information about editing the current web page by another editor (#54193)
- [x] Web pages - verify/add permissions control for add/edit/delete, shortened menus and hidden directories in administration (#54257).
- [ ] ~~tree structure - support of keyboard shortcuts for creating/editing a new item in the tree structure~~ - will not be implemented, after UX consultation only buttons will be used everywhere
- [ ] ~~tree structure - context menu - added context menu in tree structure (after considering the suitability from UI/UX point of view context menu vs toolbar icons)~~ - will not be implemented, after UX consultation only buttons will be used everywhere
- [x] +edit cell - read data from the server before editing to get `editorFields` Data. In the user list, you cannot otherwise edit e.g. the name (because the password is missing). Also some headings (Enter admin section) are displayed incorrectly (#54109).
- [x] +generic solution of the problem with `domain_id` (#53913)
- [x] +adding an ID field to each datatable as used for managing web pages (#53913)
- [x] +edit the Q&A application to the WebJET 2021 design (#53913)
- [x] +improved usability on mobile devices (#53573, #54133)
- [x] +add the option to save the web page as a work in progress using the checkbox (#54161)
- [x] +Change font to `Open Sans` (#53689).
- [x] +Integrate automatic translation of texts via https://www.deepl.com/translator (e.g. for mirroring structure, translation texts, etc.) (MR146).
- [x] +Web pages - if the page is in more than one directory, it is not possible to edit the page in the secondary directory (the data of the original page is returned and the data by docid/ID line is not paired).
- [x] +Modify the design of version 8 to resemble version 2021 (minimum menu behavior) (#54233).
- [x] +Add deployment format for update via Settings->WebJET Update (#54225).
- [x] +Complete deletion of old conf. variables `statDistinctUsers-` a `statSessions-` in mode `auto` clustra, add nodes display to Monitoring->Current values (#54453).

### Applications in a new design

- [x] +Questions & Answers - edit to new design (#53913).
- [x] +Banner system - modify to new design (#53917).
- [x] +Bulk Email - Domain Limits - edit to new design (#54153).
- [x] +Export data - modify to new design (#54149).
- [x] +Tooltip - edit to new design (#53909).

### Datatables

- [x] +Add the ability to specify the record ID in any table, similar to what is done for web pages.

### Export-import
- [x] +Export-import - fix the order of columns relative to the header (visible in the user list, but can be elsewhere) (#54097).
- [x] +Export-import - resolve columns with the same name but different data (e.g. there are 2 addresses in the user list, which is not reflected in excel column names) (#54097).

### Security

- [x] +Update NPM modules - https://www.npmjs.com/package/npm-check-updates or https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version.
- [x] +Tables - add automatic column condition setting to REST Controller `domain_id` (#53913).
- [x] +Modify the way Java classes are compiled so that everything is completely rebuilt at once before deployment. Thus, there cannot be a compatibility problem with modified classes.
- [x] +Add security check in administration in fields where HTML code can be inserted (use `org.owasp.html.PolicyFactory`) (#53931).

### Other modifications

- [x] +Version for mobile devices - optimize the interface, datatables and editor for use on mobile devices.

## 2021.40

> The goal of version 2021.40 is to migrate the user editing and GDPR module to the Datatables Editor. We will also continue to work on improving the usability of working with web pages - the Drag & Drop option and the contextual menu in the tree structure.

### New features

- [x] users - converted to Datatables Editor (#46873)
- [x] GDPR application - rebuilt in Datatables Editor (#53881, #53905)

### Web pages

- [x] editor - free fields - possibility to set field types `selectbox`, `autocomplete`, select image/page URL (#53757)
- [x] tree structure - drag & drop support for web pages and directories (#52396)
- [x] +automatically create `System` a folder for the domain and display pages from it, including subfolders (#53685)
- [x] +automatic deletion of title New web page when clicked in the box and transfer to the navigation bar (#53693)
- [x] +adjust folder list loading for template groups - show all folders not only by installation name for easier use of templates (#53693)
- [x] +add perex group editing (#53701)
- [x] +add edit rights control for translation keys (edit all keys function) (#53757)
- [x] +when renaming the domain, also change the domain prefix of the configuration variables and translation keys (#market-107)
- [x] +if the tree structure contains max 2 directories show them expanded (#53805)
- [x] +when setting the parent directory, automatically expand the root directory (#53805)
- [x] +when creating a domain `System` directories also automatically create subdirectories for headers, footers and menus (#53805)
- [x] +web pages - add icon to display the page (#53753)

### Datatables

- [x] +Edit cell for editor containing multiple sheets (#53585).

### General

- [x] gallery application - modify editing of tree structure items from VUE component to datatables editor (as for web pages) (#53561)
- [x] rewrite datatables definition from PUG files to Java annotations (#48430)
- [x] +design and document a way to create customer applications in the new WebJET 2021 design (#54045)

## 2021.13

> The goal of version 2021.13 is to improve the usability of working with web pages and the possibility of using them in customer projects. At the same time, the development of the new features of WebJET 8 will be completed and the What's New in Version 8.8 section will be written.

### Web pages

- [x] editor - improve visibility of Editor's Note field (#53131)
- [x] editor - view and manage media (Media tab) (#52351, #52462)
- [x] editor - display page history (#53385)
- [x] website - tab functionality Last modified and Pending (#53493)
- [x] web pages - import/export - adding the original import and export of pages in XML zip to the web pages toolbar (#53497)
- [x] tree structure - sometimes clicking the edit icon is not done (after previously closing the dialog via cancel) (#51487)
- [x] tree structure - completing TODO entries in the directory edit in the Template and Access tab (#51487)
- [x] tree structure - modify the icons displayed in the tree structure based on UI/UX comments (#52396)
- [x] tree structure - view scheduled versions and directory history (#53489)
- [x] tree structure - added icon to restore tree structure (#52396)
- [x] tree structure - added view `docid/groupid` and display order (by setting) (#53429, #53565)
- [x] tree structure - System and Basket tabs functionality (#53137)
- [x] +optimize the speed of displaying the list of web pages (#53513)

### Datatables editor

- [x] after closing a dialog and opening another one, error messages remain displayed (#52639)
- [x] after closing the dialog and reopening, the content remains scrolled down/in its original place (#53119)
- [x] added field type datatable to allow embedding a nested datatable in the editor (e.g. for media, page history, etc.) (#52351)
- [x] addition of displaying error message on token/user logout error (#53119)
- [x] +adding autocomplete option for text fields (#52999)
- [x] +display note field for editor via notifications (#53131)
- [x] +initialization of nested datatables only after navigating to the given sheet (#53425)
- [x] +integration of file selection via elfinder (#52609)

### Other

- [x] +Configuration - view planned versions and change history (#53089)
- [x] +add the ability to translate text in JavaScript files (#53128)
- [x] +Read data of nested datatable only after clicking on the sheet where it is located (#53425)
- [x] +check the connection to the server and display an error message in case of connection error or security (CSRF) token error (#53119)
- [x] +update the codeceptjs test framework to the current version (#52444)
- [x] +divide documentation into programming, user, installation, etc. (#52384)
- [x] +remembering the settings of the displayed columns in the datatable for each user separately (#53545)
- [x] +new login screen visual (#53617)

### General

- [x] improving integration into customer projects based on first tests (#52996, #53589)
- [x] maintain session with server, show logout progress bar as in version 8 (#53119)
- [ ] writing the section What's new for WebJET 8.8
- [x] +modification of documentation to format `docsify` and move to http://docs.webjetcms.sk/ (#52384)
- [x] +updating the codeceptjs test framework to version 3.0.4 (#52444)

<!-- deepmark-ignore-start -->
<script type="text/javascript">
setTimeout(function() {
    for (var node of document.querySelectorAll("input[type=checkbox]")) {
        node.removeAttribute("disabled");
        // Prevent click events
        node.addEventListener('click', function(event) {
            event.preventDefault();
        });
    }
}, 100);
</script>
<!-- deepmark-ignore-end -->
