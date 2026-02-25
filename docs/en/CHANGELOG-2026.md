# Changelog version 2026

## 2026.0-SNAPSHOT

> Development version updated from the main repository.

!>**Warning:** Version for `jakarta namespace`, requires Tomcat 10/11 application server, uses Spring version 7. Before the upgrade [check the requirements](install/versions.md#changes-when-switching-to-jakarta-version).

### Security

- Updated libraries `AspectJ, Eclipselink, slf4j, GoPay` (#57793).
- View Full Version `SpringSecurity` upgraded to version 7 (#56665).
- Added authorization option via [OAuth2/Keycloak/Google/Facebook...](install/oauth2/oauth2.md) (#56665).

### For the programmer

- Updated the way API documentation is displayed to standard [OpenAPI 3.0](https://www.openapis.org/). The documentation is available at `/admin/swagger-ui/index.html` for users who have admin editing rights (#57793).
- Class `PageListHolder/MutableSortDefinition` is in Spring 7 `Deprecated`, you can use our implementation as a direct replacement `PagedListHolder/SortDefinition` with package `sk.iway.iwcm.system.datatable` (#57793).
- Removed annotation `@Temporal` on date columns in database entities, the recommended solution for new entities is to use `java.time.*` types. Eclipselink/JPA seems to correctly detect the type `Date` as date and time and annotation is not necessary. We recommend checking the behavior of the date fields after the update (#57793).
- Website - recovery from the trash - completed [publishing events](developer/backend/events.md) a `ON_RECOVER` to recover pages and folders from the Recycle Bin (#161).`AFTER_RECOVER`2026.0.x

## Corrected version of the original version 2026.0.

> Banner System - fixed loading of group list in Microsoft SQL.
- Gallery - Fixed saving gallery settings for a folder on disk (without a record in the database) in Oracle DB.
- Bulk email - fixed transfer of recipients when duplicating a campaign in Oracle DB (#54273-82).
- Bulk email - fixed replacing external links that contain multiple URL parameters in an email (#54273-83).
- 2026.0

## WebJET CMS 2026.0

> **&#x20;brings an improved version of the tool&#x20;** &#x50;age Builde&#x72;**&#x20;for the creation of&#x20;**&#x63;omplex web page&#x73;**. In blocks it is possible&#x20;**&#x73;earch and filte&#x72;**&#x20;based on tags, so you can easily find the appropriate block to insert into the page. New features have been added such as&#x20;**&#x63;olumn divisio&#x6E;**,&#x20;**&#x69;nserting multiple sections at onc&#x65;**&#x20;a&#x20;**&#x62;utton to add a new section still displaye&#x64;**&#x20;to quickly expand the page content.**&#x53;upport&#x20;
>
> PICTURE elemen&#x74;**&#x20;allows you to display&#x20;**&#x64;ifferent images according to screen resolutio&#x6E;**&#x20;visitor, thus improving the visual experience on different devices. In addition, it is possible to embed&#x20;**&#x63;ustom icon&#x73;**&#x20;defined in a common SVG file, which brings more flexibility in design.**&#x41; new tool for&#x20;
>
> creation of form&#x73;**&#x20;makes it easy to create&#x20;**&#x6D;ulti-step form&#x73;**&#x20;with the possibility of programmatic validation of individual steps and the possibility of&#x20;**&#x63;onfirmation of the validity of the email addres&#x73;**&#x20;using the code sent. This avoids the forms being filled in by various robots.**!> Warning: version&#x20;

2026. &#x30;**&#x20;is the latest version for the Tomcat 9 application server using&#x20;**. All newer versions from `javax namespace` will be determined `2026.18` just fo&#x72;**&#x20;application server&#x20;**&#x54;omcat 10/1&#x31;**&#x20;using&#x20;**. Change from `Jakarta namespace` at `javax` is a consequence of the transfer of `jakarta namespace` specifications from Oracle to `Java EE` Open Source/Eclipse Foundation**where the project continues under the name&#x20;**. This change requires an update of the application server, as Tomcat 10 and later versions are already using exclusively `Jakarta EE` for all `jakarta namespace` API. For a current list of available versions, see `Java EE` installation section[.](install/versions.md) Groundbreaking changes

### Updated libraries&#x20;

- , more in `commons-lang,displaytag` section for the programmer[ (#58153).](#pre-programátora) Changed behaviour of the Blocks icon in Page Builder mode -&#x20;
- text blocks integrated[ to the folder ](frontend/page-builder/blocks.md) similar to the blocks for `content` (#58165).`section, container, column`Modified processing&#x20;
- uploading file&#x73;**, more in&#x20;**&#x73;ection for the programmer`multipart/form-data` (#57793-3).[Recommended ](#for-the-programmer) check the functionality of all forms
- &#x20;due to modifications in their processing, for more information see **for the programmer** (#58161).[Website](#for-the-programmer) Added the ability to insert&#x20;

### &#x20;element that displays&#x20;

- image by screen resolution`PICTURE` visitor. So you can view different images on your mobile phone, tablet or computer (#58141).[Added the ability to insert ](frontend/setup/ckeditor.md#picture-element) custom icons

![](frontend/setup/picture-element.png)

- &#x20;defined in the common SVG file (#58181).[Added transfer of actual HTML code when switching the editor mode to Standard/HTML/Page Builder. So you can easily edit a Page Builder page in HTML code and view the edits again in Page Builder mode (#58145).](frontend/setup/ckeditor.md#svg-ikony) Added Delete Element context menu to easily delete a button, link, paragraph, form, section, etc. Just right click on the element to see the context menu (#osk233).

![](frontend/setup/svgicon.png)

- Page Builder - modified stylesheet generation when using the pencil tool. Only changed values are generated to the CSS style, they are highlighted in the dialog by blue border of the input field (#58145).
- Page Builder - added calling option&#x20;
- custom JavaScript file
- &#x20;with support functions for code editing. Added ability to edit settings like selectors for elements, colors, etc (#58141).[Page Builder - modified anchor generation for tabs so that the anchor name is generated according to the tab name - originally it was generated non-semantically as ](frontend/page-builder/blocks.md#supporting-javascript-code) (#112).
- Page Builder - added option to set column width to `autotabs-x-y` for automatic content customization (#114).
- Page Builder - added option to prepare `auto` text blocks
- &#x20;directly to the folder [, they are inserted instead of the original blocks read from web pages from the Templates folder. The web designer prepares them together with other types of Page Builder blocks. Allows quick insertion of frequently used text sections, buttons, etc (#58165).](frontend/page-builder/blocks.md) Page Builder - when inserting a new block, the Library tab is defaulted instead of Basic to make it easier to select a block from the prepared list (#58165 `content`Page Builder - added the ability to split a column into two parts using the new Split Column function. You invoke it by clicking the + in the yellow bar, selecting Block, and then selecting Split Column in the Basic tab. The function allows you to quickly split a column without having to insert a new column and move the contents (#58165).
- Page Builder - added the ability to insert a block that contains multiple sections or other elements - all sections are marked after inserting (#58173).
- Page Builder - supplemented&#x20;
- Block ID
- &#x20;to the attribute [ for the ability to search for block usage in web pages via the administration search (#58193).](frontend/page-builder/blocks.md#id-bloku) Page Builder - the list of favorite blocks is stored separately for each user, so that everyone can manage their own list of favorite blocks (#58193 `data-pb-id`Page Builder - added a permanently displayed icon to add a new section at the bottom of the page, making it easier to add new sections to the page (#58173).
- Page Builder - modified toolbar design for better visibility on different backgrounds (#58165).
- Page Builder - added option&#x20;

![](redactor/webpages/pagebuilder-plusbutton.png)

- filter blocks

![](redactor/webpages/pagebuilder.png)

- &#x20;by name and tags (#58173).[Supplemented by ](frontend/page-builder/blocks.md#name-and-block-marks) content change detection

![](redactor/webpages/pagebuilder-library.png)

- &#x20;and notification of unsaved changes when you close the browser window. Changes are detected 5 seconds after the web page is opened. (#112).[Added option to set default values for tables in CKEditor via configuration variables, more in ](redactor/webpages/working-in-editor/README.md#detection-of-page-content-changes) CKEditor settings section
- &#x20;(#58189).[Added the ability to insert ](frontend/setup/ckeditor.md#configuration-variables) button
- &#x20;\- element [. You can so easily insert different action ](frontend/setup/ckeditor.md#button) buttons (#58201).`button`Style - `call to action` choice of style
- &#x20;defined for the element e.g. [ or ](frontend/examples/template-bare/README.md#list-of-styles-for-editor) allows you to set multiple styles at the same time. Repeatedly selecting an already set style will remove that style (#OSK140).`p.paragraph-green,p.paragraph-red-border,p.paragraph-yellow-background`Edited text for future publication of the page on `section.test-section,section.test-section-green` Schedule a page change after this date
- , selecting this option will also change the save button to text **Schedule** for clarity for the user (#58253).**Added list of changed fields (#58077) to the web site approval application.**&#x41;pplications
- Redesigned application properties settings in the editor from the old code in&#x20;

![](redactor/webpages/approve/approve-form.png)

### &#x20;at&#x20;

&#x20;Application. Apps also automatically get the ability to set `JSP` display on devices`Spring`. The design is consistent with the rest of the WebJET CMS and data tables (#58073).[News](custom-apps/appstore/README.md#conditional-application-view) Form easily
- [Forms](redactor/apps/news/README.md)

![](redactor/apps/news/editor-dialog.png)

- [A new way to create forms that can include ](redactor/apps/formsimple/README.md)

![](redactor/apps/formsimple/editor-dialog-items.png)

### more steps

- &#x20;with advanced features. In the list of forms you can create a new form, to which you can then add individual items and possibly multiple steps. The form item tab is visible in the form detail of the Multistep form type (#58161).[YouTube video player](redactor/apps/multistep-form/README.md) List of forms - the whole section has been redesigned from technology&#x20;

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/XRnwipQ-mH4" title=" to standard " frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

- &#x20;for better integration into WebJET CMS and simplified editing (#58161).`Vue.js`List of forms - enables the creation of a form that is automatically of type `Html + JavaScript` multi-step form
- &#x20;(#58161).[List of forms - allows setting parameters/attributes of all form types directly in the form editor (#58161).](redactor/apps/multistep-form/README.md) List of forms - the note field allows you to insert formatted text, so you can better record additional information to the form (#58161).
- Form Detail - added option to display all logged in user data, data is also exported to Excel (#58161).
- Authentication code - added option to submit form after entry&#x20;
- authentication code
- &#x20;sent to the email address. This way you can better protect your forms from SPAM (#58161).[Redirections](redactor/apps/form/README.md#setting-the-confirmation-by-the-sent-code) Added the ability to end the redirection at a specified time and the ability to enter a note with information about what the redirection is for. Redirects that are no longer temporarily valid are displayed in red (#58105).

![](redactor/apps/form/form-step-email-verification-2.png)

### Ecommerce

- New section&#x20;

![](redactor/webpages/redirects/path-editor.png)

### Methods of delivery

- , as a separate table, replaces the original configuration of available delivery methods, which was located directly in the application settings [e-commerce](redactor/apps/eshop/delivery-methods/README.md). It is also possible to set a price for each delivery method, which will be automatically added to the order when the option is selected. Set delivery methods are also automatically reflected in the options when the customer creates an order. Delivery by mail and personal pickup are ready, we plan to add integration to delivery companies in the future (#58061).**Security**Added support for enabling only&#x20;

![](redactor/apps/eshop/delivery-methods/datatable.png)

### one active login

- &#x20;per user. You enable the mode by setting the configuration variable&#x20;**&#x20;to the value of&#x20;**. A new login will cancel the previously active `sessionSingleLogon` (#58121).`true`Unsupported library removed `session` commons-lang
- , replaced by a new library [commons-lang3](https://mvnrepository.com/artifact/commons-lang/commons-lang), v [ is an update script for editing source code (#58153).](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3) Added list `update-2023-18.jsp` My active logins
- &#x20;on the administration splash screen, which shows all active logins to administration under your user account and the option to exit them. Also added the ability to send an email to a logged in administrator (#58125).[Captcha - by setting a configuration variable ](redactor/admin/welcome.md#my-active-logins) to the value of&#x20;

![](redactor/admin/sessions.png)

- &#x20;Captcha can be turned off completely. It will not be displayed even if the template of the displayed web page has SPAM protection disabled. In this case, however, it is necessary to correctly check that the SPAM protection of the template is turned off in the eventual processing/verification code of the Captcha response, for forms this check is ensured. You can use the call `captchaType` to verify the mode and disable spam protection (#54273-78).`none`Updated library for `Captcha.isRequired(component, request)` sending emails
- &#x20;z [ at ](install/config/README.md#sending-emails) due to support for new SMTP server authentication mechanisms such as `com.sun.mail:javax.mail:1.6.2` and added configuration variable `com.sun.mail:jakarta.mail:1.6.8` to force the use of the authorization mechanism - e.g. set to `NTLMv2` for enforcement `smtpAuthMechanism` authorisation instead of using `NTLM` Authorization (#58153 `NTLM`Modified exception logging when HTTP connection is interrupted (e.g. when closing the browser, going to another web page, etc.). Such exceptions will not be logged to avoid space occupation errors. This applies to exceptions of the type `BASIC` and exception names defined via the configuration variable&#x20;
- , by default `IOExceptio` (#58153).`clientAbortMessages`Other minor changes`response already,connection reset by peer,broken pipe,socket write error`Search - modified loading of template list when searching for web pages. All templates are loaded regardless of their availability in folders, so that when editing a web page, the template is not available (#58073).

### HTTP headers - added the ability to set a header longer than 255 characters, for example to set&#x20;
- &#x20;(
- #82`Content-Security-Policy`)[Configuration - modified method of deleting a configuration variable. After deleting, the original value is automatically set from ](https://github.com/webjetcms/webjetcms/issues/82) to be the same as it will be after the server restart. In the original solution, the variable was just deleted, but its value remained set internally until the server restart (#57849).

![](admin/settings/response-header/editor.png)

- Configuration - added option to set `Constants` HTTP header name
- &#x20;to get the IP address of the visitor via the configuration variable [ (#58237).](sysadmin/pentests/README.md#konfigurácia) Security - added the ability to configure blocked file/directory paths via a variable `xForwardedForHeader`. By default, URLs that contain the phrase in the name are blocked&#x20;
- . More can be added as needed (#PR103).`pathFilterBlockedPaths`Tags - modified tags displayed, in case of duplication of values. Comparison is without the influence of diacritics and upper/lower case letters `.DS_Store,debug.,config.properties,Thumbs.db,.git,.svn` #115
- .[Mirroring - added option to display flag image instead of text in ](https://github.com/webjetcms/webjetcms/issues/115) page language switcher

![](redactor/webpages/perex-duplicity-values.png)

- &#x20;(#54273-79).[Change password - added option to set name and email address from which email is sent with link to change password via configuration variables ](redactor/apps/docmirroring/README.md#creating-a-link-to-language-mutations-in-the-page-header) a&#x20;
- &#x20;(#58125 `passwordResetDefaultSenderEmail`Statistics - added summary numbers of views and visits in TOP pages (#PR136).`passwordResetDefaultSenderName`News - renamed the Arrange by Priority value to Arrange by Order of Order (Priority) to align with the value in the editor (#57667-16).
- Form easily - added option to set value&#x20;
- &#x20;to insert the form e.g. in the footer of the page (#57667-16).
- News / News templates - moved field `useFormDocId` from the News app to the News Template to set the property directly in the news template. Original value&#x20;
- &#x20;from News will still work, but it can no longer be set in the UI (#58245).`contextClasses`Document Manager - added option `contextClasses` edit the metadata of the historical version of the document
- &#x20;in the document manager (#58241).[Bulk email - modified auditing of campaign changes. If a group is added it doesn't audit the entire recipient list (it was wasting a lot of records in the audit), only the list of changed groups is written. When manually adding emails, both name and email address (#58249) are still audited.](redactor/files/file-archive/README.md#úprava-historickej-verzie-dokumentu-v-manažéri) Users - when importing, if a column in Excel does not contain a password field, a random password will be generated for new users. If the status Approved user is not specified in Excel, it is set to&#x20;
- &#x20;(#58253).
- MultiWeb - added domain display in sidebar (#58317-0).`true`MultiWeb - added the ability to set the redirect domain to be able to specify&#x20;
- &#x20;prefix (#58317-0).
- MultiWeb - added rights control for media groups and tags (#58317-0).`https://`List of forms - settings&#x20;
- the processor of the forms
- , using an autocomplete field that offers a class implementing [ (#58313).](custom-apps/apps/multistep-forms/README.md) Dialers - added removal of spaces at the beginning and end of the string type field in the dialer data (#OSK233).`FormProcessorInterface`Error correction
- Tags - fixed folder duplication in View for when saving tags, removed folder selection from other domains since tags are already separated by domain (#58121).

### Web pages - fixed hard space insertion after hyphens to apply only to page text and not to attributes or HTML tags (#OSK235).
- Datatables - corrected event processing&#x20;
- &#x20;for selected filter table input fields (#58313).
- Datatables - fixed filtering when multiple `Enter` tables on the page interacted with each other when filtering (#58313).
- Ecommerce - corrected sending email notification when order status changes (#58313 `serverSide:false`Ecommerce - corrected automatic order status setting after payment change (#58313).
- Documentation
- Updated all screenshots in the Czech version of the documentation (#58113).

### For the programmer

- Free fields - added the ability to specify custom columns for label and value at&#x20;

### links to the dial

- . Allows more flexible setting of which property from the code list is used as displayed text and which is used as a stored value (#PR108).[Deleted unused files ](frontend/webpages/customfields/README.md#Dialcode), if you use them in your project take them from&#x20;
- older versions`/admin/spec/gallery_editor_perex_group.jsp,/admin/spec/perex_group.jsp` WebJET CMS (#58073).[Slightly modified API in ](https://github.com/webjetcms/webjetcms/tree/release/2025.40/src/main/webapp/admin/spec) NewsActionBean
- , especially the setting [ which are now of the type ](../../src/main/java/sk/iway/iwcm/components/news/NewsActionBean.java). You can use `groupIds` for the setting with the value ID field (#58073 `List<GroupDetails>`Fixed ability to insert quotes in application parameters (#58117).`setGroupIds(int[] groupIds)`Prepared containers for all supported database servers in WebJET CMS for easy running in VS Code. They are located in the folder&#x20;
- &#x20;(#58137).
- E-commerce - due to changes in the implementation process `.devcontainer/db` delivery methods
- &#x20;the file needs to be modified using an update script&#x20;**&#x20;above the section&#x20;**&#x20;(#58061).`update-2023-18.jsp`E-commerce - renamed annotation `basket` at&#x20;
- &#x20;a `@PaymentMethod` at `@FieldsConfig` to unify annotations between payments and delivery methods (#58061).`@PaymentFieldMapAttr`E-commerce - in the process of implementation `@FieldMapAttr` delivery methods
- &#x20;to the file&#x20;**&#x20;There are a few changes that you have to implement manually. These changes are too complex to be added using the update script&#x20;**&#x20;(#58061).`order_form.jsp`Navigation bar - added option to use custom generator implementation `update-2023-18.jsp` navigation bar
- . Via the configuration variable [ it is possible to set the name of the class implementing ](redactor/apps/navbar/README.md) (#PR101).`navbarDefaultType`Unsupported library removed `NavbarInterface` commons-lang
- , replaced by a new library [commons-lang3](https://mvnrepository.com/artifact/commons-lang/commons-lang), v [ is an update script for editing source code (#58153).](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3) Updated library `update-2023-18.jsp` displaytag
- &#x20;to version [ (#58153).](https://mvnrepository.com/artifact/com.github.hazendaz/displaytag) Modified file upload processing `2.9.0`. In Spring applications, for the file array, use instead of&#x20;
- &#x20;directly `multipart/form-data`, which will be set automatically. It is no longer necessary to use a call type `org.apache.commons.fileupload.FileItem` to retrieve the file. `org.springframework.web.multipart.MultipartFile` Warning:`entity.setDocument(MultipartWrapper.getFileStoredInRequest("document", request))` all occurrences need to be replaced&#x20;**&#x20;For&#x20;**&#x20;in your code, also invalidate URL parameters in the Spring application for forced processing. The expression `CommonsMultipartFile` replace with `MultipartFile`. You can use `data-th-action="@{${request.getAttribute('ninja').page.urlPath}(\_\_forceParse=1,\_\_setf=1)}"` to update files (#57793-3).`data-th-action="${request.getAttribute('ninja').page.urlPath}"`Added option to create `/admin/update/update-2023-18.jsp` project file copies
- &#x20;Spring application. Just create your own version of the file in the folder [ similar to what is used for JSP files. WebJET CMS first looks for the file in the project folder and if it is not found it uses the standard file from ](frontend/customize-apps/README.md) Folder (#58073).`/apps/INSTALL_NAME/`Added option to set `/apps/` name for CSS style
- &#x20;in CSS file via comment [. The name appears in the list of styles in the editor (#58209).](frontend/examples/template-bare/README.md) Editor - modified dialog for settings `/* editor title: Style Name */` - cancelled color and size settings,&#x20;
- only CSS classes are used`a.btn` as well as pr [ (#57657-16).](frontend/setup/ckeditor.md#tlačidlo) Data tables - option to display only icons without order for `button` if we add a class to the column&#x20;
- &#x20;(#58161).`rowReorder`Data tables - new options for selecting rows in the table `icon-only` a&#x20;
- , more in `toggleSelector` data tables section`toggleStyle` (#58161).[Data tables - new custom option ](developer/datatables/README.md#configuration-options) render
- &#x20;functions using annotation [. Allows you to display composite values from multiple fields in a column and so on (#58161).](developer/datatables-editor/datatable-columns.md) Data tables - added option `@DataTableColumn(...renderFunction = "renderStepName")` redirect the user
- &#x20;to another page after saving the record by calling the method [ (#58161).](developer/datatables/restcontroller.md#presmerovanie-po-uložení) Forms - Modified display of the list of forms, deleted class `setRedirect(String redirect)`, replaced by the class&#x20;
- . Forms settings changed from table `FormAttributesDB` to the table `FormService`. We recommend that you check the functionality of all forms on the website (#58161) after the update.`form_attributes`Forms - create new table `form_settings` as a replacement for the table&#x20;
- where the form properties are stored. Individual attributes (settings) are now stored in separate columns as one record per row. The data has been converted to the new table using `form_settings` (#58161).`form_attributes`Switching to a new table `UpdateDatabase.java` for form properties in&#x20;
- &#x20;files. You need to run the update script `form_settings`, which will adjust the necessary `.jsp` (#58161).`update-2025-0.jsp`List of forms - setting parameters/attributes of all form types redirected from the table `.jsp` to the new table&#x20;
- &#x20;(#58161).`form_attributes`Data tables - added BE support for `form_settings` where it is possible to change the order of records directly in the data table using drag&drop (#58161).
- Events - added event `row-reorder` Updating codes in text
- &#x20;for the possibility of editing codes in the text of the page type [ et al (#54273-63).](developer/backend/events.md#aktualizácia-kódov-v-texte) Data tables - added `!CUSTOM_CODE!` Spring events
- &#x20;for the ability to edit data in customer installations (#54273-63).[Testing](developer/backend/events-datatable.md) Supplemented script&#x20;

### rm-same-images.sh
- &#x20;to remove the same images when creating new screenshots (#58113).[](../../src/test/webapp/rm-same-images.sh)

![meme](_media/meme/2026-0.jpg ":no-zoom")
