# ROADMAP

List of planned versions of WebJET CMS. Versions are numbered as ```rok.týždeň``` which is also the expected availability time of the given version. Tasks marked with a ticket number are already in the solution, tasks marked with ```[x]``` are already implemented, tasks marked with ```+``` were added after the preparation of ```roadmapy``` and affect the delivery of other tasks.

Warning: this is a plan, depending on needs and capacities, new tasks may be added to the version and some may be postponed to a later version.

Explanation of the pictograms used:

- [ ] task is scheduled
- [ ] if it has an assigned ticket ID at the end, we have already started working on the task (#ticket-id)
- [x] task is complete
- [ ] +if it starts with ```+``` the task was added to the list after the initial plan was created, such tasks are typically requested by customers and if they are implemented they will affect the schedule. This means that some other tasks may not be implemented in the given version due to lack of time.
- [ ] ~~crossed out~~ task moved to another version, or solved in a different way, or canceled. The description always includes the version number it was moved to or the reason for cancellation.

## 2026

- [x] Semantic search - use AI to improve search with `RAG` (#211).
- [ ] `Headless` CMS - prepare REST interfaces for using WebJET CMS in `headless` mode.
- [ ] Migration to `Spring Boot` project.
- [x] Testing - add accessibility testing using the [codeceptjs-a11y-helper](https://github.com/kobenguyent/codeceptjs-a11y-helper) extension (#58389).
- [ ] Rights - add the option to set read-only rights and optionally set only allowed IDs for editing.
- [x] Forms - add option to set total attachment size for form, now can only be set per file (#58517).
- [ ] Statistics - adjust write to `seo_bots` through `StatWriteBuffer` for fewer conflicts under high load and cluster database.
- [x] Scripts - add option to set whether the script should also be inserted in the page editor or not (#58349).
- [x] Forms - add a group of selection and checkbox fields `radio/checkbox` connected to the number list, similar to what we have for the `select` field (#58517).
- [ ] Delete files associated with a web page when I delete it - but ask the user in advance if they want to delete the files. Check if they are not being used somewhere else.
- [ ] Data deletion - add the option to delete pages and folders from the trash. Also resolve the option to run data deletion as an automated task.
- [ ] Unused files - make it possible to get a list of unused files - they are not used anywhere on the page, not even in media, etc. There was an API for this `FileTools.getDirFileUsage(currentDir, request)`. Add as a tab to the folder properties in explorer.

## 2025

- [x] Transition to `Jakarta EE` - ​​change Java packages from `javax.servlet` to `jakarta.servlet`, prepare migration script (#57793).
- [x] Migration to Tomcat 11+ application server (#57793).
- [x] Transition to `Spring` version 7 (#57793).
- [ ] Introduce into the project the obligation to use `SonarLint` and format the code via `.editorconfig` or `Spotless` - example https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/build.gradle.
- [x] Primary use of GitHub repository for development.
- [x] Canceling artifact generation on the old `iwmsp.sk` repository, artifacts will now only be available via [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms).
- [x] Updating the `pd4ml` library to a new version, individual websites will have to obtain a license separately, WebJET CMS will no longer be provided.
- [ ] Migration of the most used applications to the Spring version using Thymeleaf templates.
- [x] Discontinue `Apache Struts` framework, replace `logic:present,logic:iterate,bean:write` with either `JSTL` variant, or implement similar functionality into `iwcm:present,iwcm:iterate,iwcm:beanrwite` (#57789).
- [ ] Moving JSP files, Java classes and JavaScript libraries of the old version 8 to the `obsolete` jar archive, which will not be a standard part of WebJET CMS. It can be used on old projects where all customer applications have not yet been updated to the `Spring` version, but without support and updates from WebJET CMS.
- [ ] Statistics - click map - functionality restored, responsiveness problem resolved (separate records based on window width).
- [x] Statistics - ability to filter bots for error page statistics (#58053).
- [ ] Log files - filter by installation name.
- [ ] `quill` - ​​add the ability to set menu items including colors.
- [ ] Applications - option to purchase the application for the OpenSource version (#55825).
- [ ] Possibility to execute Thymeleaf code in the header/footer and possibly also in the body of the web page.
- [ ] Security - add support for generating `nonce` for `Content-Security-Policy` header, see e.g. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.
- [x] Forms - add the ability to call a Java class for form validation (#58161).
- [x] Tags - filter by current domain to be the same as in other sections (#57837).
- [x] User import - if no password is entered, generate one (for new users), if not sent, set status `authorized` to `true` (#58253).
- [ ] In tests, somehow automatically check for the occurrence of `I\.waitForText\('.*?', '.*?'\);` and `I\.waitForText\(".*?", ".*?"\);`, which are incorrect waits without a defined time, will cause automated tests to crash.
- [x] Add an application to redirect the main page to `/sk/` or `/en/` depending on the browser language. (#58477)
- [x] Modify configuration deletion so that when deleted, it is set to the original value defined in `Constants` (#57849).
- [x] Gallery - when duplicating an image, allow changing the "Folder" so that we can duplicate images to a folder other than the current one (#57885).
- [x] Mass email - audit changes in user groups (#58249).
- [x] File archive - convert to data tables (#57317).
- [ ] Optional fields - add the ability to select multiple items for connection to the dialer.
- [x] E-commerce - integration with payment gateway `GoPay` (#56609).
- [x] Add authorization option via `OAuth2`, option to use `mock` server https://github.com/navikt/mock-oauth2-server or https://github.com/patientsknowbest/fake-oauth2-server (#56665).
- [ ] Authorization via ```SAML``` - ​​integrate the [Spring SAML](https://spring.io/projects/spring-security-saml) library for authentication against the ```ADFS/SAML``` server.
- [x] Reservations - new app for all-day reservations (#57389).
- [x] Applications - convert the application settings dialog in the web page editor from the old JSP to a data table (#57409).
- [x] Mass email - optimization of recipient list creation (#57537).
- [ ] +Background tasks - the option to manually run the task on `node`, which the task is set to, will now run on `node` where the user is logged in.
- [ ] +Forms - disable `GET` call to `FormMail/FormMailAjax`.
- [ ] +Electronic store - add `JSON-LD` data to the email https://schema.seznam.cz/objednavky/dokumentace/.
- [ ] + Dialer, Blog, News - adjust so that the dialer or news folder type selection is on the left, similar to the gallery/websites. Then the cards don't have to be displayed all at once.
- [ ] +Translation keys - display a tree structure of translation keys for better orientation.
- [x] +Add support for logging into administration via [PassKeys](https://passkeys.dev/docs/tools-libraries/libraries/) (#58369).
- [ ] +Photo bank - add the option to set the file name before downloading from the photo bank, automatically set according to the search term.
- [ ] +Gallery - if I set the perex image to an image in the gallery, download it from the photo bank, and rename the image to an existing one in the database (if the file is deleted from the disk), a duplicate entry will be created in the `gallery` table. Moreover, the other images `o_,s_` will not be renamed. It should be remembered that theoretically I can rename any one, it should detect that I am in the gallery and rename all versions.
- [x] +Configuration - add option to set `Hikari` via `poolman.xml/ENV` such as `spring.datasource.hikari.idle-timeout=30000, spring.datasource.hikari.max-lifetime=1800000, spring.datasource.hikari.connection-timeout=30000` (#54273-61).
- [ ] Datatable - fix counting of marked rows after deleting them.
- [ ] +Explorer - set folder properties (indexing, rights) according to parent (recursively) when first opened, if the folder does not have settings in the database.
- [ ] +For fields of type `DataTableColumnType.JSON`, specifically `className = "dt-tree-page-null"`, add the option to define the root folder.
- [ ] +Move all `Converter` classes such as `DocDetailsNotNullConverter` to a separate `package` where there will be no other entities so that this `Converter` can also be used in projects where a separate `JPA` is used.
- [x] +Add option to set HTTP header name for `x-forwarded-for` and specify which of the IP addresses to use (first VS last) (#58237).
- [ ] +Datatables - add the ability to sort by multiple columns by clicking with the `SHIFT` key.
- [ ] +Datatables - add the `hideOnDuplicate` option for cards in the editor, don't forget the `DataTableTab` class so that it can also be set in the annotation.
- [ ] +Explorer - add the option to move to the parent folder in the Media of this page.
- [ ] +Configuration - in case of `cluster` installation, add the option to set the variable only for the current node (do not save it to the database). Originally, this was solved by the `applyToAllClusterNodes` selection field.
- [ ] +Numbers - add the ability to define the field type for a string like we have in optional fields.
- [x] +Statistics - add a summary number of Views and Visits to the traffic section for an easy overview of the total traffic for the selected period (sum of the numbers in the table). It could be added `footerCallback` - ​​https://datatables.net/examples/advanced_init/footer_callback.html (#57929).
- [x] +Structure mirroring - add option to delete `sync_id` values ​​for selected folder (recursively). To make it easy to cancel/reset synchronization. Also there is a problem that I clone `SK,DE,EN` then disable `DE` and want to have only `EN` but mirroring still applies changes to `DE` folder (#57561).
- [ ] +Add `autoupdate` verification to GitHub tests for all supported database servers - i.e. initialize an empty database and verify all `autoupdate` and verify that they pass without error. Make it a separate pipeline.
- [x] Gallery - allow changing the gallery path (folder) and set everything related to it (#58433).
- [ ] +Configuration - add the group column with the value `modules` of the configuration variable (select field, can have multiple values). Add the option to display unset variables (i.e. complete list). Create a tool to generate all variables by groups/modules into the `md` file in the documentation for an overview of all variables.
- [x] +If I have an unsaved page with the title Cleaning and there is already another page called Cleaning, then the newly uploaded images will be uploaded to the `upratovanie` folder before saving it. But when the page is saved, it will get the URL cleaning-2.html and the other images will be uploaded to the `upratovanie-2` folder. You need to modify the code in `getPageUploadSubDir` so that instead of using the page title directly, it will try to get the URL for the new page and then use that (#58361).
- [x] +HTTP headers - extend the maximum size of the HTTP header value to more than 255 characters, for `Content-Security-Policy` it is an insufficient size (#PR83, #58129).
- [x] +Integration of AI tools, helpers, assistants (#57997).
- [ ] +The API key will not be displayed in the profile editor after it is generated, notifications will not be transferred to the parent window.
- [ ] +Forms - adjust form protection so that `document.write` is not used.
- [ ] +Add the option to set the type `textarea` as in AI assistants also with line numbers, e.g. in scripts or elsewhere where writing code is expected.
- [x] +News - move field `contextClasses` from news application to news template. Set field as `hidden` to remain functional (it may be set somewhere), if empty use value from template. So both options must work, maybe combine both values ​​into one list (#58245).
- [ ] +Nested data tables - set the number of records in `auto` mode according to the size of the nested data table area.
- [ ] +The send email later function uses `sendMailSaveEmailPath`, which cannot save the file according to the current domain, think about a solution. Maybe this is because emails are sent in the background where the domain may not be known.

## 2024

- [x] Migration to Java 17 - WebJET from version 2024.0 will require Java version 17. (#54425)
- [x] Changed icon set from `FontAwesome` to `Tabler Icons` (#56397).
- [x] Publishing JAR files to `Maven Central` (#43144).
- [x] New version of the Shopping Cart application/`eshop`, demo version of the website, integration via API to online billing systems, updated integrations to payment gateways (#56329,56385,56325).
- [x] Translation keys - option to import only non-existent keys (#56061).
- [ ] ~~`quill` - ​​add the ability to set menu items including colors.~~
- [ ] ~~Applications - option to purchase the application for the OpenSource version (#55825).~~
- [x] Web pages - if config variable `syncGroupAndWebpageTitle` is enabled and one page is set as the main page for multiple folders, disable renaming of the folder name according to the main page. Plus, if the main page is in another folder, also do not rename (#56477).
- [x] Audit - redo Changed pages and Waiting to publish to data tables (#56165).
- [x] Blog - administration `bloggerov` convert to data tables (#56169).
- [x] Blog - comments - integrate into Discussion section, add `bloggerom` rights to Discussion section as well (#56173).
- [x] AB testing - conversion to DT, use the options from the `news` application, set config variables according to the specified prefix (#56177).
- [x] Event calendar - unapproved and recommended events - convert to DT, use code for event list (#56181).
- [x] Editor - when creating an email link, it automatically adds http in the window, even though the link is eventually inserted correctly with the `mailto:` prefix (#56189).
- [x] Questionnaires - convert the application to data tables (#55949).
- [x] +PostgreSQL - add database support (#56305).
- [x] Easy form - add options like standard forms (mainly redirection settings after submission), option to send the form to the user (for review) (#56481).
- [x] +Form easy - add support for wysiwyg fields (#56481).
- [x] +Delete unused and replace rarely used libraries (#56265).
- [x] +MultiWeb - verify functionality, add necessary rights (#56421, #56405, #56425).
- [x] +Bulk email - add one-click unsubscribe support (#56409).
- [ ] ~~+Option to execute Thymeleaf code in the header/footer and possibly in the body of the web page.~~
- [x] +Replace database pool with `HikariCP` (#56821).
- [x] +Logging levels - convert to datatable (#56833).
- [x] +DBPool - transition from `Apache DBCP` to `HikariCP` (#56821).
- [x] Documentation - translation into English (#56237,#56249,#56773).
- [x] +Documentation - translation into Czech (#57033).
- [x] +A/B Testing - disable for search engines/bots, indicate version for Ninja object (#56677).
- [x] +Datatables - option to skip a row during import if it is incorrect (#56465).
- [x] +Redirects - optimize getting redirects without secondary getting the redirect code from the database (#53469).
- [ ] ~~+Security - add support for generating `nonce` for `Content-Security-Policy` header, see e.g. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.~~
- [x] +Gallery - add the option to change the image size.
- [ ] ~~+Forms - add the ability to call a Java class for form validation.~~
- [x] +File indexing - add to Perex-Start of publication the date of the last file change (#57177).
- [x] +Translation keys - add the ability to filter keys by conf. variable to the REST service `/rest/properties/` to prevent it from publicly obtaining all keys from WebJET CMS (#57202).
- [x] +Websites - audit successful time publishing of a website, option to send notification to the website author (#57173).
- [x] +Display license validity information 2 months before its expiration on the splash screen (#57169).
- [ ] ~~+Tags - filter by current domain to make it the same as in other sections.~~
- [x] +Audit - add HTTP header `Referrer` to identify the original page from which the call was made (#57565).
- [ ] ~~+Import users - if no password is entered, generate one (for new users), if not, the status `available` is sent, set to `true`.~~
- [ ] ~~+In tests, somehow automatically check for the occurrence of `I\.waitForText\('.*?', '.*?'\);` and `I\.waitForText\(".*?", ".*?"\);` which are incorrect waits without a defined time, will cause automated tests to crash.~~
- [ ] ~~+Add an application to redirect the main page to `/sk/` or `/en/` depending on the browser language.~~
- [ ] ~~+Modify configuration deletion so that when deleted, it is set to the original value defined in `Constants`.~~
- [x] +Gallery - add the URL address of the image source field with the option to enter the address from which we obtained the image, automatically set when using a photo bank.
- [ ] ~~+Gallery - when duplicating an image, allow changing the "Folder" so that we can duplicate images to a folder other than the current one.~~
- [ ] ~~+Bulk email - audit changes in user groups.~~
- [ ] ~~+File archive - convert to data tables (#57317).~~
- [ ] ~~+Optional fields - add the ability to select multiple items for connection to the dialer.~~
- [x] +Search in administration - converted to data table (#57333).
- [x] +Survey - addition of the original functionality of displaying statistics (#57337).
- [x] +Tags - add the option to define the name in multiple languages, add optional fields (#57273,#57449)
- [x] +Applications - convert the application settings dialog in the web page editor from the old JSP to a data table (#57157,#57161,#57409).
- [x] +Websites, gallery - add the ability to search in the tree structure (#57265,#57437)

## 2023.52 / 2024.0

> Version 2023.52 / 2024.0 is focused on improving security, moving from the `Struts` library to `Spring`.

- [x] Update - rework to a new design, add the option to update using jar packages (#55797).
- [x] Structure cloning - rework to use Structure mirroring with translation capability (#55733).
- [x] Rating - rework to Spring (#55729).
- [x] Forum - convert to Spring (#55649).
- [x] Users - add rights group column to data table (#55601).
- [ ] Gallery - new type for displaying a small number of photos with expansion, possibility to switch between all photos in the article (#55349).
- [ ] ~~Add authorization option via `OAuth2`, option to use `mock` server https://github.com/navikt/mock-oauth2-server or https://github.com/patientsknowbest/fake-oauth2-server (#56665).~~
- [ ] ~~Authorization via ```SAML``` - ​​integrate the [Spring SAML](https://spring.io/projects/spring-security-saml) library to enable authentication against the ```ADFS/SAML``` server.~~
- [x] Web pages - add the ability to restore a deleted folder so that the `available` attribute is correctly set for web pages (#55937).
- [ ] Explorer - adjust to new design, update JS code elfinder (#55849).
- [x] Web pages - when creating links in the dialog box, remember the last directory and use it directly when adding another link (#54953-29).
- [x] Event calendar - if an event is entered in October for November when the time shift occurs, it is displayed with the time shifted by one hour (#56857).
- [ ] adjust messaging between administrators from a pop-up window for a better user interface.
- [x] Apps - improve description of each app, update app images (#55293).
- [x] Photo gallery - optimize the amount of data loaded (#56093).
- [x] Photo gallery - image editor is initialized every time the window is opened, fix to use an existing editor (#55853).
- [x] Datatables - add testing of the Duplicate function to the automated test (#56849).
- [ ] Banner - add the ability to use templates to display the banner, e.g. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals.
- [ ] Check how SEO extensions for WordPress work (e.g. https://yoast.com/beginners-guide-yoast-seo/) and propose a similar solution for WebJET CMS. Integrate something like https://github.com/thinhlam/seo-defect-checker into the editor (#56853).
- [ ] Add a data attribute with the block name to blocks in PageBuilder and then display it in the block properties so that it can be identified later on what block it is.
- [ ] Implement a mini todo app on the home screen.
- [ ] DT - after adding a record, re-paginate to the page where the record is located (typically the last page)
- [x] Editor - add the option to display an HTML editor for selected web pages, which cannot be switched to WYSIWYG mode. Used for special pages integrating various JavaScript applications or special components. A suitable editor is [Ace-code](https://www.npmjs.com/package/ace-code) (#56129).
- [x] Fulltext - move folder `files` in indexed web pages to System tab (54953-30).
- [x] +Users - prevent self-deletion (#55917).
- [x] +News - add folder rights check and display only those for which the user has rights (#56661).
- [x] +Applications - option to conditionally display applications by device type, implement generically, configurable via UI for Banner (#55921).
- [x] +Statistics - limit displayed statistics to only folders to which the user has rights (#55941).
- [x] +Restaurant menu - convert application to data tables (#55945).
- [x] +Redirects - separate redirects by selected domain (#55957).
- [x] +Perex groups - separate administration of perex groups according to user rights on the website (#55961).
- [x] +Banner - conditional display based on device mobile/tablet/desktop (#55997).
- [x] +Page Builder - generating anchors/top menu based on inserted sections (#56017).
- [x] +Optional fields - add a field of type `UUID` for generating identifiers (#55953).
- [x] +Import web pages - convert to Spring (#55905).
- [x] +Server Monitoring - CPU - `oshi-core` does not work on Windows 11 (#55865).
- [x] +Headers - option to define the file extension for which the header will be set (#56109).

## 2023.40

> Version 2023.40 modifies the user interface based on new UX designs, improves the usability of data tables.

- [x] Migrate to a higher version of Java with ```LTS``` support (Java 11 or ideally up to Java 17) (#54425).
- [x] Banner - add the option to set the directory structure of the pages in which the banner will be displayed and the option to define exceptions where it will not be displayed (#55285).
- [x] Headers - add a new application that allows you to define any HTTP header by URL (#55165).
- [ ] ~~(move to 2024.0)edit messaging between administrators from popup window to better user interface~~
- [ ] ~~(Move to 2024.0)Apps - improve description of each app, update app images (#55293).~~
- [ ] ~~(move to 2024.0)Photo gallery - optimize the amount of data loaded~~
- [ ] ~~(moved to 2024.0)Photo gallery - image editor is initialized every time the window is opened, modify to use the existing editor (#55853)~~
- [ ] ~~(moved to 2024.0)Datatables - add testing of the Duplicate function to the automated test.~~
- [ ] ~~(moved to 2024.0)Banner - add the ability to use templates to display the banner, e.g. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals.~~
- [ ] ~~(moved to 2024.0)Check how SEO extensions for WordPress work (e.g. https://yoast.com/beginners-guide-yoast-seo/) and propose a similar solution for WebJET CMS. Integrate something like https://github.com/thinhlam/seo-defect-checker into the editor.~~
- [ ] ~~(moved to 2024.0)Add a data attribute with the block name to blocks in PageBuilder and then display it in the block properties so that it can be identified later what block it is.~~
- [x] Number books - there is no attribute in the export that the record is deleted and then after import the record is activated (#55541).
- [ ] ~~(Move to 2024.0)Implement a mini todo app on the home screen.~~
- [ ] ~~(move to 2024.0)DT - after adding a record, re-paginate to the page where the record is located (typically the last page)~~
- [ ] ~~(moved to 2024.0)Editor - add the option to display an HTML editor for selected web pages, which cannot be switched to WYSIWYG mode. Used for special pages integrating various JavaScript applications or special components. A suitable editor is [Ace-code](https://www.npmjs.com/package/ace-code).~~
- [x] Publishing OpenSource/Community version of WebJET CMS on ```github.com``` (#55789,#55773,#55829,#55749).
- [x] +Datatables - buttons are displayed incorrectly in web pages/galleries when setting a different column ratio (#54953-22).
- [x] +Translations - add to Structure Mirroring the option to translate page text via [DeppL](https://www.deepl.com/docs-api/html/) (#55709).
- [ ] ~~(moved to 2024.0)+Authorization via ```SAML``` - ​​integrate the [Spring SAML](https://spring.io/projects/spring-security-saml) library for authentication against the ```ADFS/SAML``` server.~~
- [ ] ~~(moved to 2024.0)+Transition from Struts framework to Spring - gradually rewrite the code of all classes to Spring controllers (#55389,#55469,#55489,#55493,#55469,#55501,#55609,#55613,#55617,#55701).~~
- [x] +Redesign the news list administration to a new design and data table, using as much code as possible from the existing website list (#55441).
- [x] +Update the `amcharts` library for charts to newer version 5 (#55405).
- [x] +Redesign the News app to a new design (#55441).
- [x] +Add development support via DevContainers (#55345).
- [x] +Server Monitoring - rework the Applications, Web Pages and SQL Queries sections into new data tables (#55497).
- [x] +Websites - approval - rewrite from `Struts` to `Spring`, add tests (#55493).
- [x] +SEO - convert to repositories and datatables (#55537).
- [x] +User authorization - the original option from v8 is missing in the administration (#55545).
- [x] +DT - search for a record by ID, a better solution for searching for a web page by docid when there are many pages in the list (#55581).
- [x] +Discussion - convert administration to datatables (#55501).
- [x] +Server Monitoring - rework monitoring of Applications/Websites/SQL queries into datatables/new graphs (#55497).
- [x] +Proxy - add support for calling REST services without embedding the output into the web page (#55689).
- [x] +Banner - add support for video files including content banner (#55817).
- [x] +Translation keys - option to import only one language without affecting translations in other languages ​​(import only the specified column from Excel) (#55697).
- [x] +Media - add optional fields support (#55685).

## 2023.18

> Version 2023.18 changes the requirement for running the solution to Java version 17. This is a fundamental change and also requires a change on the servers. For this reason, it is implemented right at the beginning of the year.

- [ ] ~~(moved to 2023.40)Migration to a higher version of Java with ```LTS``` support (Java 11 or ideally up to Java 17) (#54425)~~.
- [x] Integrate version 8 code into the project, remove duplicate pages, remove the option to switch to version 8, keep only new versions of pages (#54993).
- [x] Updated CK Editor to a newer version (#55093).
- [x] Forms - add archive form function (#54993).
- [ ] ~~(moved to 2023.40)Banner - add the option to set the directory structure of the pages in which the banner will be displayed and the option to define exceptions where it will not be displayed.~~
- [x] Search - ```Lucene``` - ​​add language indexing based on the page language, so only pages in the specified template language would be indexed (#54273-34).
- [ ] ~~(moved to 2023.40)Headers - add a new application that allows you to define any HTTP header by URL (#55165).~~
- [x] Page attributes - added option to set page attributes in the editor (#55145).
- [x] Templates - add template merging option (#55021).
- [ ] +Audit - add annotation to primary key if it is different from ID, specifically for editing translation keys, the value ```key``` is not audited but ```id``` which is irrelevant.
- [x] +Gallery - The multidomain used the ```/images/DOMENA/gallery``` structure, which is now not visible in the gallery. It needs to be added there somehow (#54953-4).
- [x] +Translations - possibility of dynamic definition of supported languages ​​in templates, translation keys, etc. (#MR332).
- [x] +Security - server logs are available within the audit right, it needs to be separated into a separate right (#54953-5).
- [x] +Improve Oracle DB server support, test entity functionality (#54953-6).
- [x] +Websites - when duplicating a website, attached media should also be duplicated (#54953-6).
- [x] +Websites/gallery - add the ability to set the aspect ratio for displaying the tree structure and table (#54953-7).
- [x] +Translation keys - change editing to a table view where the columns contain individual languages ​​to allow editing translations in all languages ​​at once (#55001).
- [x] +Banner - redesign banner display statistics to a new design (#54989).
- [x] +Translation keys - add check for the number of translation languages ​​so that it does not fall on ```fieldK``` (#MR344).
- [x] +Císelník - rework to a new design to remove the dependency on the old version of datatables.net (#55009).
- [ ] +Fix a bug in the editor, e.g. in the gallery application - when moving the window by grabbing the application title, it moves incorrectly and then constantly follows the cursor. You then need to click at the bottom of the OK button to turn off window scrolling.
- [x] +Proxy - rework to new design (#55025).

## 2022.52 / 2023.0

> Version 2022.52 represents a long-term list of tasks that did not make it into the more detailed 2022 plan.

- [ ] ~~cancelled for now: record videos of using WebJET CMS in an automated way - https://dev.to/yannbertrand/automated-screen-recording-with-javascript-18he or https://www.macrorecorder.com~~
- [x] add test statistics via the ```Allure``` module - https://codecept.io/plugins/#allure (#54437).
- [x] add ```code coverage``` report, e.g. via JaCoCo - https://docs.gradle.org/current/userguide/jacoco_plugin.html (#54909).
- [ ] ~~(move to 2023.40) adjust messaging between administrators from popup window to better user interface~~
- [ ] ~~(move to 2023.40) Apps - improve description of each app, update app images.~~
- [x] Gallery - add physical file name change (including generated `s_` and `o_` images) after changing Filename attribute in editor (#39751-52).
- [x] +Datatables - remember the number of records per page for each datatable (#39751-50).
- [ ] +```Command Palette``` - ​​add a command palette with integrated search similar to VS Code - https://trevorsullivan.net/2019/09/18/frictionless-user-experiences-add-a-command-palette-to-your-react-application/
- [ ] ~~(move to 2023.40) +photo gallery - optimize the amount of data loaded~~
- [ ] ~~(move to 2023.40) +photo gallery - image editor is initialized every time the window is opened, modify to use an existing editor~~
- [ ] ~~(moved to 2023.40) +Datatables - add testing of the Duplicate function to the automated test.~~
- [ ] ~~(moved to 2023.40) +Banner - add the ability to use templates to display the banner, e.g. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals.~~
- [ ] ~~(moved to 2023.40) +Check how SEO extensions for WordPress work (e.g. https://yoast.com/beginners-guide-yoast-seo/) and propose a similar solution for WebJET CMS.~~
- [ ] ~~(moved to 2023.40) +Add a data attribute with the block name to blocks in PageBuilder and then display it in the block properties so that it can be identified later what block it is.~~
- [x] Consider using [HotSwapAgent](http://hotswapagent.org/) for faster development without the need for reboots (solved by refactoring ```build.gradle``` to support standard HotSwap).
- [ ] ~~(move to 2023.40)+Numbers - there is no attribute in the export that the record is deleted and then after import the record is activated.~~
- [ ] ~~(Moved to 2023.40)Implement a mini todo app on the home screen.~~
- [ ] ~~(move to 2023.40)+DT - after adding a record to page to the page where the record is located (typically the last page)~~
- [x] +Add translations of texts from pug files (e.g. `gallery.pug`), search for e.g. the letter á or similar, which could be found in untranslated texts.
- [x] +Add the option to keep the editor window open after saving (see https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open, closeOnComplete, https://editor.datatables.net/reference/api/submit()). Implemented using the ```CTRL+s/CMD+s``` keyboard shortcut (#54273-26).
- [x] ```Combine``` / data deletion - when deleting all data in the cache, transfer it correctly to ```Cluster``` and delete it there as well, currently ```ClusterRefresher``` only deletes the cache, not ```DocDB/GroupsDB``` and does not set a new time for ```combime``` (#54673).
- [x] +REST - add the ability to define API keys (tokens) used instead of generated CSRF tokens for REST calls by external services (#54941).
- [ ] ~~(moved to 2023.18) +Headers - add a new application that allows you to define any HTTP header by URL address.~~
- [x] +Scripts - add ```autocomplete``` to Script Location field in template (#54941).
- [x] +Link check - edit the original version of Check links to the data table and link to the list of web pages (#54697).
- [x] +Reservations and reservation objects - convert administration to datatable and Spring (#54701).
- [x] +Survey - convert administration to datatable and Spring (#54705).
- [x] +Page attributes - convert administration to datatable and Spring (#54709).
- [x] +Templates - add a tab with a list of websites using the template (#54693).
- [x] +Statistics - rework to new design and graphs (#54497, #54585, #54897).
- [x] +Search - ```Lucene``` - ​​add search support in Czech and English (#54273-34).
- [x] +Fix an issue with FontAwesome versions in administration and websites (#39751-51).
- [x] +Banner system - add the option to display a campaign banner according to the URL parameter (#MR296).
- [x] +Gallery - expand editor options, add superscript/subscript, ability to insert HTML code, color settings. Improve support for old editor/html code (#54857-4).
- [x] +Gallery - adjust the number of photos to the window size, remember the set image size in the table (#54857-4, #market-245).
- [x] +Websites - display perex groups as a multi-select field with search instead of standard ```checkbox``` fields (if there are many perex groups) (#market-245).
- [x] +Form easy - add field type ```select``` - ​​standard selection field, add option to enter a different value and displayed text (#MR298, #39751-45).
- [x] +Web pages - add display of folders as a data table with the possibility of bulk operations (#54797).
- [x] +Security - add option to set HTTP header ```Access-Control-Allow-Origin``` (#39751-44).
- [x] +Banner system - add the option to set the client, check rights when viewing (#38751-52).
- [x] +Banner system - add the option to set optional fields, refactor ```BannerEntity``` and ```BannerBean``` into a single object so that the new fields can also be used in the JSP component (#39751-52).

## 2022.40

> Version 2022.40 is focused on improving security.

- [x] Users - add password check from history when saving a user, because now it allows it, but you can't log in with it (#54293).
- [x] User groups - add a tab with a list of users in a rights group (#54493).
- [x] Update NPM modules - https://www.npmjs.com/package/npm-check-updates or https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version (#54721).
- [x] Change ```hash``` password function to ```bcrypt``` (or something more modern - https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) (#54573).
- [x] Add/verify rights check for editing configuration variables (edit all variables function) (#54749).
- [x] Validations - add better validations for fields (e.g. url address, domain) with descriptive error messages and modify ```DatatableExceptionHandlerV2``` so that it can use translation keys directly from WebJET CMS with additional data (such as min/max value and other macros in the translation key) (#54597).
- [x] Logging - transition from ```log4j``` to standard ```slf4j``` - resolved by transitioning to ```logback``` ().
- [x] Logging - add a system for temporarily writing logs to RAM memory for the ability to view the latest logs via administration if there is no access to the logs on the file system. Possibility to also get logs from cluster nodes, e.g. via temporary writing to the data of the ```conf``` table (#54269).
- [x] Add update and database checks to the home page like in WebJET 8 + ~~opening help for new version~~ (there is a link to the change list in the introduction) (#54457).
- [x] Adjust the visual of the non-compliant password and password changes on WebJET 9 (at least change the background image) (#54573).
- [x] Security - add access control to administration - currently only REST service calls are checked, not ```Thymeleaf``` page calls (#54649)
- [x] Datatable - add item name to the delete dialog box, it is necessary to consider the situation when multiple rows are selected (#54753).
- [ ] ~~(moved to 2023.18) +Migration to a higher version of Java with ```LTS``` support (Java 11 or ideally up to Java 17) (#54425).~~
- [ ] ~~(moved to 2023.18) +Forms - add the function to archive a form.~~
- [x] +Web pages - add page preview function before saving (#54557).
- [x] +Websites - add ```drag&drop``` pages option to directory structure (#54513).
- [x] +Prepare a sample application for the programmer in the administration where it will be possible to select some options in the form and upload a file that will be processed by the server. The result will then be displayed (#54449).
- [x] +Prepare a sample template using Thymeleaf and possibly PUG files with PageBuilder integration (#54345).
- [x] +Redesign the Bulk Email-Sent Emails application to the new design (#54377).
- [x] +Add CZ and EN translations (#53805).
- [x] +Add a tab with a list of users in a given group to the user group editing, adjust the list of web pages for server paging (#54993).
- [ ] ~~(moved to 2023.18) +Banner - add the option to set the directory structure of the pages in which the banner will be displayed and the option to define exceptions where it will not be displayed.~~
- [x] +Add remembering the arrangement method for each data table (#54513-22).~~
- [x] +Add X to close window and to maximize icon.
- [ ] ~~(move to 2022.52) +Templates - add a tab with a list of websites using the template (#54693).~~
- [x] +Web sites - folders can be selected multiple times with CTRL, but they cannot all be deleted this way (only the first one will be deleted) (#54797).
- [x] +Datatables - dynamically determine the number of table columns so that there is no empty space left, which evokes that there are no more records (the user will not notice that there is pagination) (#54273-26).
- [x] +Datatables - add the ability to use multiselect in the datatable and in the application in the editor (#54273-25).
- [x] +Event Calendar - redesign the application to a new design (#54473).
- [x] +Websites - add the option to display folder ID and sorting order in the page tree structure (#54513-1).
- [x] +Websites - add option to save page as A/B test (#54513-2).
- [x] +Forms - add export option from last export, selected records, according to current filter (#54513-3)
- [x] +Generate ```POM``` file with dependencies/libraries directly from gradle version, instead of editing in original WJ8 version (#54673).
- [ ] ~~(move to 2022.52) +Scripts - add ```autocomplete``` to the Script Location field in the template.~~
- [ ] ~~(moved to 2022.52) +Link check - edit original version Link check to datatable and link to list of websites (#54697).~~
- [ ] ~~(moved to 2022.52) +Reservations and reservation objects - convert administration to datatable and Spring (#54701).~~
- [ ] ~~(moved to 2022.52) +Survey - convert administration to datatable and Spring (#54705).~~
- [ ] ~~(moved to 2022.52) +Page attributes - convert administration to datatable and Spring (#54709).~~
- [x] +Ability to scroll the editor window (#54513-21).
- [x] +Remember table column order for each table (#54513-22)
- [x] +Remember last opened folder in website list (#39751-45)

## 2022.18

> Version 2022.18 focuses on usability improvements.

- [x] editor - display page in inline editing (if inline editor is set to Page Builder) (#54349).
- [x] DT in client pagination, marking all lines appears as marking on one page but deletes everything on other pages (#54281).
- [x] Add button to notification that there is a work in progress version to open (#54357).
- [x] Add a column with icons to display the web page (#54257).
- [x] Convert DocID/website to ID in websites (#53805).
- [x] Home - add application to right column with list of Favorite URLs (#54177)
- [x] Introduction - add a simple form to send a comment to WJ (#54181)
- [x] Introduction - enable clicking on icons in the first block (visits, forms...) (#53805)
- [x] Add option to set up two-factor authentication by administrator (#54429)
- [x] For WJ Spring components map ```editor_component```. Currently, jsp is looking for ```INCLUDE``` and for these components there is e.g. ```sk.iway.xxx.component.SiteBrowserComponent``` and so it doesn't find it and returns ```appstore``` (#54333).
- [x] Websites - add the option to change the domain after entering ```docid``` if the page is located in a different domain and automatically switch the bookmark to System or Trash if it is located in this branch (#54397).
- [x] Translation keys - add a button to restore keys from the ```properties``` file, restore keys after deleting a record and do not display a deletion error if the key does not exist (because it is from the properties file - id greater than 1000000) (#54401).
- [x] DT Editor - add notification when leaving the page (#54413).
- [x] Optional fields - add option to select directory (#54433).
- [ ] ~~+(move to 2022.52) DT - after adding a record, re-paginate to the page where the record is located (typically the last page)~~
- [x] +Tweak Firefox version, add automated testing for Firefox as well (#54437).
- [x] +After switching to WJ8 version, the links to add redirection (and maybe others) are missing (#53805).
- [x] +Add an icon to switch to V9 version in the WJ8 interface (#53805).
- [ ] ~~+(move to 2022.52) Add translations of texts from pug files (e.g. gallery.pug), search for e.g. the letter á or similar, which could be found in untranslated texts.~~
- [x] +When importing a configuration with an update by name, it fails with an error, it would be advisable to replace it with the original import with a preview of the changes.
- [ ] ~~+ (moved to 2022.36) Prepare a sample application for the programmer in the administration where it will be possible to select some options in the form and upload a file that will be processed by the server. The result will then be displayed (#54449).~~
- [x] +Update Java libraries to the current version without vulnerabilities.
- [x] +Add option to use ```Thymeleaf``` templates for ```@WebjetComponent``` (#54273).
- [x] +Set up an instance with a public git repository (#54285).
- [ ] ~~+(move to 2022.52) Add option to keep editor window open after saving (see https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open).~~
- [x] +In the template section menu, display template editing items directly on the first level (#54345)
- [x] +Add the option to enter ```placeholder``` in the Easy Form application (#54381).
- [x] +Verify running WebJET CMS on Oracle and Microsoft SQL (#54457).

## 2021.52

> Version 2021.52 is focused on improving work with websites and the editor.

### Website

- [x] editor - added info icons for individual fields with an explanation of their meaning (#54253)
- [x] editor - completion of TODO items in web page editing in the Access tab - setting the display of the navigation bar and site map (data model extension) (#54205)
- [x] editor - modified URL address field according to the original UI design (only the end part is displayed with the option to switch to displaying the entire URL) (#54237)
- [x] editor - display page approval information (after saving, when it is to be approved) (#54129)
- [x] editor - display of the working version of the page (#54129)
- [x] ~~editor - add Save and Save As option~~ - replaced by Duplicate datatables property
- [x] web pages - display web pages also from subdirectories (#52435)
- [x] Web pages - directories - show domain entry field only for root folders, automatically set domain when saving root folder (#54249)
- [x] Websites - add information about editing the current website by another editor (#54193)
- [x] Websites - verify/add control of rights to add/edit/delete, short menus and hidden directories in administration (#54257).
- [ ] ~~tree structure - support for keyboard shortcuts for creating/editing a new item in the tree structure~~ - will not be implemented, after UX consultation only buttons will be used everywhere
- [ ] ~~tree structure - context menu - added context menu in the tree structure (after considering the suitability from a UI/UX perspective, context menu vs icons in the toolbar)~~ - will not be implemented, after UX consultation only buttons will be used everywhere
- [x] +edit cell - load data from server before editing to get ```editorFields``` data. Otherwise, you cannot edit e.g. name in the user list (because password is missing). Some headings are also displayed incorrectly (Enter admin section) (#54109).
- [x] +generic solution to the problem with ```domain_id``` (#53913)
- [x] +adding the ID field to each data table as used for website management (#53913)
- [x] +edit Questions and Answers app to WebJET 2021 design (#53913)
- [x] +improved usability on mobile devices (#53573, #54133)
- [x] +add option to save web page as a work in progress using checkbox (#54161)
- [x] +Change font to ```Open Sans``` (#53689).
- [x] +Integrate automatic text translation via https://www.deepl.com/translator (e.g. for structure mirroring, translation texts, etc.) (MR146).
- [x] +Web pages - if a page is in multiple directories, it is not possible to edit the page in the secondary directory (the data of the original page is returned and the data is not matched by docid/row ID).
- [x] +Adjust version 8 design to resemble version 2021 (minimal menu behavior) (#54233).
- [x] +Add deployment in format for update via Settings->WebJET Update (#54225).
- [x] +Add deletion of old config variables ```statDistinctUsers-``` and ```statSessions-``` in ```auto``` cluster mode, add display of nodes in Monitoring->Current values ​​(#54453).

### Applications in a new design

- [x] +Questions and Answers - adjust to new design (#53913).
- [x] +Banner system - adjust to new design (#53917).
- [x] +Bulk Email - Domain Limits - adjust to new design (#54153).
- [x] +Export data - adjust to new design (#54149).
- [x] +Tooltip - adjust to new design (#53909).

### Data tables

- [x] +Add the ability to enter a record ID in any table, similar to how it is for web pages.

### Export-import

- [x] +Export-import - fix column order relative to header (visible in user list, but may be elsewhere) (#54097).
- [x] +Export-import - resolve columns with the same name but different data (e.g. there are 2 addresses in the user list, which is not reflected in the Excel column names) (#54097).

### Safety

- [x] +Update NPM modules - https://www.npmjs.com/package/npm-check-updates or https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version.
- [x] +Datatables - add automatic setting of column condition ```domain_id``` to REST Controller (#53913).
- [x] +Modify the way Java classes are compiled so that everything is completely rebuilt at once before deployment. This means that there can be no compatibility issues with modified classes.
- [x] +Add security check in administration in fields where HTML code can be inserted (use ```org.owasp.html.PolicyFactory```) (#53931).

### Other adjustments

- [x] +Mobile version - optimize the interface, data tables and editor for use on mobile devices.

## 2021.40

> The goal of version 2021.40 is to migrate user editing and the GDPR module to the Datatables Editor. We will also continue to work on improving the usability of working with websites - Drag & Drop and context menus in the tree structure.

### New features

- [x] users - converted to Datatables Editor (#46873)
- [x] GDPR application - converted to Datatables Editor (#53881, #53905)

### Website

- [x] editor - free fields - option to set field types ```selectbox```, ```autocomplete```, select image/page URL (#53757)
- [x] tree structure - support for drag & drop of web pages and directories (#52396)
- [x] +automatically create ```System``` folder for domain and display pages from it including subfolders (#53685)
- [x] +automatically delete the New website title when clicking in the field and transfer it to the navigation bar (#53693)
- [x] +fix loading folder list for template groups - show all folders not just by installation name for easier template usage (#53693)
- [x] +add editing of perex groups (#53701)
- [x] +add rights check for editing translation keys (function edit all keys) (#53757)
- [x] +when renaming a domain, also change the domain prefix of configuration variables and translation keys (#market-107)
- [x] +if the tree structure contains max 2 directories, display them expanded (#53805)
- [x] +automatically expand root directory when setting parent directory (#53805)
- [x] +when creating a domain ```System``` directory, automatically create subdirectories for headers, footers and menus (#53805)
- [x] +websites - add icon to display the page (#53753)

### Datatables

- [x] +Cell editing for editor containing multiple sheets (#53585).

### General

- [x] gallery application - change editing of tree structure items from VUE component to datatables editor (like on web pages) (#53561)
- [x] rewrite datatables definition from PUG files to Java annotations (#48430)
- [x] +design and document a way to create customer applications in the new WebJET 2021 design (#54045)

## 2021.13

> The goal of version 2021.13 is to improve the usability of working with websites and the possibility of using them in customer projects. At the same time, the development of new features of WebJET 8 will be completed and the section What's new in version 8.8 will be written.

### Website

- [x] editor - improve visibility of Editor's Note field (#53131)
- [x] editor - display and manage media (Media tab) (#52351, #52462)
- [x] editor - view page history (#53385)
- [x] websites - enable Last edited and Pending approval tabs (#53493)
- [x] website - import/export - adding the original import and export of pages in XML zip to the website toolbar (#53497)
- [x] tree structure - sometimes clicking on the edit icon is not executed (after previously closing the dialog via cancel) (#51487)
- [x] tree structure - completion of TODO items in directory editing in the Template and Access tab (#51487)
- [x] tree structure - adjustment of displayed icons in the tree structure based on UI/UX feedback (#52396)
- [x] tree structure - display planned versions and directory history (#53489)
- [x] tree structure - added icon for restoring tree structure (#52396)
- [x] tree structure - added ```docid/groupid``` display and display order (according to settings) (#53429, #53565)
- [x] tree structure - enabling System and Recycle Bin tabs (#53137)
- [x] +optimize the speed of displaying the list of websites (#53513)

### Datatables editor

- [x] error messages remain displayed after closing a dialog and opening another (#52639)
- [x] after closing the dialog and opening it, the content remains shifted down/in its original location (#53119)
- [x] added field type datatable for the possibility of inserting a nested datatable into the editor (e.g. for media, page history, etc.) (#52351)
- [x] added error message display for token/user logout error (#53119)
- [x] +add autocomplete option for text fields (#52999)
- [x] +show editor note field via notifications (#53131)
- [x] +initialization of nested data tables only after switching to a given sheet (#53425)
- [x] +file selection integration via elfinder (#52609)

### Other

- [x] +Configuration - view planned versions and change history (#53089)
- [x] +add the ability to translate texts in JavaScript files (#53128)
- [x] +Load data from a nested datasheet only after clicking on the sheet where it is located (#53425)
- [x] +check connection to server and display error message in case of connection error or security token (CSRF) error (#53119)
- [x] +update codeceptjs testing framework to current version (#52444)
- [x] +split documentation into programmer, user, installation, etc. (#52384)
- [x] +remembering the settings of displayed columns in the data table for each user separately (#53545)
- [x] +new login screen visual (#53617)

### General

- [x] improved integration into customer projects based on first tests (#52996, #53589)
- [x] maintaining session with server, displaying logout progress bar as in version 8 (#53119)
- [ ] writing the What's New for WebJET 8.8 section
- [x] +editing the documentation to ```docsify``` format and moving it to the server http://docs.webjetcms.sk/ (#52384)
- [x] +updated codeceptjs testing framework to version 3.0.4 (#52444)

<!-- spellcheck-off -->
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