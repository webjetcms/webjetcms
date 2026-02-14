# Changelog version 2025

## 2025.52/SNAPSHOT

> **WebJET CMS 2025.52/SNAPSHOT** brings an improved version of the tool **Page Builder** for the creation of **complex web pages**. In blocks it is possible **search and filter** based on tags, so you can easily find the appropriate block to insert into the page. New features have been added such as **column division**, **inserting multiple sections at once** a **button to add a new section still displayed** to quickly expand the page content.
>
> Support **PICTURE element** allows you to display **different images according to screen resolution** visitor, thus improving the visual experience on different devices. In addition, it is possible to embed **custom icons** defined in a common SVG file, which brings more flexibility in design.
>
> A new tool for **creation of forms** makes it easy to create **multi-step forms** with the possibility of programmatic validation of individual steps and the possibility of **confirmation of the validity of the email address** using the code sent. This avoids the forms being filled in by various robots.

### Groundbreaking changes

- Updated libraries `commons-lang,displaytag`, more in [section for the programmer](#for-the-programmer) (#58153).
- Changed behaviour of the Blocks icon in Page Builder mode - [text blocks integrated](frontend/page-builder/blocks.md) to the folder `content` similar to the blocks for `section, container, column` (#58165).
- Modified processing **uploading files**, more in `multipart/form-data` section for the programmer[ (#57793-3).](#pre-programátora) Recommended&#x20;
- check the functionality of all form&#x73;**&#x20;due to modifications in their processing, for more information see&#x20;**&#x66;or the programmer[ (#58161).](#pre-programátora) Website

### Added the ability to insert&#x20;

- &#x20;element that displays `PICTURE` image by screen resolution[ visitor. So you can view different images on your mobile phone, tablet or computer (#58141).](frontend/setup/ckeditor.md#picture-element) Added the ability to insert&#x20;

![](frontend/setup/picture-element.png)

- custom icons[ defined in the common SVG file (#58181).](frontend/setup/ckeditor.md#svg-ikony) Added transfer of actual HTML code when switching the editor mode to Standard/HTML/Page Builder. So you can easily edit a Page Builder page in HTML code and view the edits again in Page Builder mode (#58145).

![](frontend/setup/svgicon.png)

- Added Delete Element context menu to easily delete a button, link, paragraph, form, section, etc. Just right click on the element to see the context menu (#osk233).
- Page Builder - modified stylesheet generation when using the pencil tool. Only changed values are generated to the CSS style, they are highlighted in the dialog by blue border of the input field (#58145).
- Page Builder - added calling option&#x20;
- custom JavaScript file[ with support functions for code editing. Added ability to edit settings like selectors for elements, colors, etc (#58141).](frontend/page-builder/blocks.md#podporný-javascript-kód) Page Builder - modified anchor generation for tabs so that the anchor name is generated according to the tab name - originally it was generated non-semantically as&#x20;
- &#x20;(#112).`autotabs-x-y`Page Builder - added option to set column width to&#x20;
- &#x20;for automatic content customization (#114).`auto`Page Builder - added option to prepare&#x20;
- text blocks[ directly to the folder ](frontend/page-builder/blocks.md), they are inserted instead of the original blocks read from web pages from the Templates folder. The web designer prepares them together with other types of Page Builder blocks. Allows quick insertion of frequently used text sections, buttons, etc (#58165 `content`Page Builder - when inserting a new block, the Library tab is defaulted instead of Basic to make it easier to select a block from the prepared list (#58165).
- Page Builder - added the ability to split a column into two parts using the new Split Column function. You invoke it by clicking the + in the yellow bar, selecting Block, and then selecting Split Column in the Basic tab. The function allows you to quickly split a column without having to insert a new column and move the contents (#58165).
- Page Builder - added the ability to insert a block that contains multiple sections or other elements - all sections are marked after inserting (#58173).
- Page Builder - supplemented&#x20;
- Block ID[ to the attribute ](frontend/page-builder/blocks.md#block-id) for the ability to search for block usage in web pages via the administration search (#58193 `data-pb-id`Page Builder - the list of favorite blocks is stored separately for each user, so that everyone can manage their own list of favorite blocks (#58193).
- Page Builder - added a permanently displayed icon to add a new section at the bottom of the page, making it easier to add new sections to the page (#58173).
- Page Builder - modified toolbar design for better visibility on different backgrounds (#58165).

![](redactor/webpages/pagebuilder-plusbutton.png)

- Page Builder - added option&#x20;

![](redactor/webpages/pagebuilder.png)

- filter blocks[ by name and tags (#58173).](frontend/page-builder/blocks.md#názov-a-značky-bloku) Supplemented by&#x20;

![](redactor/webpages/pagebuilder-library.png)

- content change detection[ and notification of unsaved changes when you close the browser window. Changes are detected 5 seconds after the web page is opened. (#112).](redactor/webpages/working-in-editor/README.md#detekcia-zmeny-obsahu-stránky) Added option to set default values for tables in CKEditor via configuration variables, more in&#x20;
- CKEditor settings section[ (#58189).](frontend/setup/ckeditor.md#konfiguračné-premenné) Added the ability to insert&#x20;
- button[ - element ](frontend/setup/ckeditor.md#button). You can so easily insert different action `button` buttons (#58201).`call to action`Style -&#x20;
- choice of style[ defined for the element e.g. ](frontend/examples/template-bare/README.md#list-of-styles-for-editor) or `p.paragraph-green,p.paragraph-red-border,p.paragraph-yellow-background` allows you to set multiple styles at the same time. Repeatedly selecting an already set style will remove that style (#OSK140).`section.test-section,section.test-section-green`Edited text for future publication of the page on&#x20;
- Schedule a page change after this dat&#x65;**, selecting this option will also change the save button to text&#x20;**&#x53;chedul&#x65;**&#x20;for clarity for the user (#58253).**&#x41;dded list of changed fields (#58077) to the web site approval application.
- Applications

![](redactor/webpages/approve/approve-form.png)

### Redesigned application properties settings in the editor from the old code in&#x20;

&#x20;at `JSP` Application. Apps also automatically get the ability to set `Spring` display on devices[. The design is consistent with the rest of the WebJET CMS and data tables (#58073).](custom-apps/appstore/README.md#podmienené-zobrazenie-aplikácie) News
- [Form easily](redactor/apps/news/README.md)

![](redactor/apps/news/editor-dialog.png)

- [Forms](redactor/apps/formsimple/README.md)

![](redactor/apps/formsimple/editor-dialog-items.png)

### A new way to create forms that can include&#x20;

- more steps[ with advanced features. In the list of forms you can create a new form, to which you can then add individual items and possibly multiple steps. The form item tab is visible in the form detail of the Multistep form type (#58161).](redactor/apps/multistep-form/README.md) List of forms - the whole section has been redesigned from technology&#x20;

![](redactor/apps/multistep-form/real-form.png)

- &#x20;to standard `Vue.js` for better integration into WebJET CMS and simplified editing (#58161).`Html + JavaScript`List of forms - enables the creation of a form that is automatically of type&#x20;
- multi-step form[ (#58161).](redactor/apps/multistep-form/README.md) List of forms - allows setting parameters/attributes of all form types directly in the form editor (#58161).
- List of forms - the note field allows you to insert formatted text, so you can better record additional information to the form (#58161).
- Form Detail - added option to display all logged in user data, data is also exported to Excel (#58161).
- Authentication code - added option to submit form after entry&#x20;
- authentication code[ sent to the email address. This way you can better protect your forms from SPAM (#58161).](redactor/apps/form/README.md#nastavenie-potvrdenia-zaslaným-kódom) Redirections

![](redactor/apps/form/form-step-email-verification-2.png)

### Added the ability to end the redirection at a specified time and the ability to enter a note with information about what the redirection is for. Redirects that are no longer temporarily valid are displayed in red (#58105).

- Ecommerce

![](redactor/webpages/redirects/path-editor.png)

### New section&#x20;

- Methods of delivery[, as a separate table, replaces the original configuration of available delivery methods, which was located directly in the application settings ](redactor/apps/eshop/delivery-methods/README.md) e-commerc&#x65;**. It is also possible to set a price for each delivery method, which will be automatically added to the order when the option is selected. Set delivery methods are also automatically reflected in the options when the customer creates an order. Delivery by mail and personal pickup are ready, we plan to add integration to delivery companies in the future (#58061).**&#x53;ecurity

![](redactor/apps/eshop/delivery-methods/datatable.png)

### Added support for enabling only&#x20;

- one active logi&#x6E;**&#x20;per user. You enable the mode by setting the configuration variable&#x20;**&#x20;to the value of `sessionSingleLogon`. A new login will cancel the previously active `true` (#58121).`session`Unsupported library removed&#x20;
- commons-lang[, replaced by a new library ](https://mvnrepository.com/artifact/commons-lang/commons-lang) commons-lang3[, v ](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3) is an update script for editing source code (#58153).`update-2023-18.jsp`Added list&#x20;
- My active logins[ on the administration splash screen, which shows all active logins to administration under your user account and the option to exit them. Also added the ability to send an email to a logged in administrator (#58125).](redactor/admin/welcome.md#moje-aktívne-prihlásenia) Captcha - by setting a configuration variable&#x20;

![](redactor/admin/sessions.png)

- &#x20;to the value of `captchaType` Captcha can be turned off completely. It will not be displayed even if the template of the displayed web page has SPAM protection disabled. In this case, however, it is necessary to correctly check that the SPAM protection of the template is turned off in the eventual processing/verification code of the Captcha response, for forms this check is ensured. You can use the call `none` to verify the mode and disable spam protection (#54273-78).`Captcha.isRequired(component, request)`Updated library for&#x20;
- sending emails[ z ](install/config/README.md#sending-emails) at `com.sun.mail:javax.mail:1.6.2` due to support for new SMTP server authentication mechanisms such as `com.sun.mail:jakarta.mail:1.6.8` and added configuration variable `NTLMv2` to force the use of the authorization mechanism - e.g. set to `smtpAuthMechanism` for enforcement `NTLM` authorisation instead of using `NTLM` Authorization (#58153 `BASIC`Modified exception logging when HTTP connection is interrupted (e.g. when closing the browser, going to another web page, etc.). Such exceptions will not be logged to avoid space occupation errors. This applies to exceptions of the type&#x20;
- &#x20;and exception names defined via the configuration variable `IOExceptio`, by default `clientAbortMessages` (#58153).`response already,connection reset by peer,broken pipe,socket write error`Other minor changes

### Search - modified loading of template list when searching for web pages. All templates are loaded regardless of their availability in folders, so that when editing a web page, the template is not available (#58073).
- HTTP headers - added the ability to set a header longer than 255 characters, for example to set&#x20;
- &#x20;(`Content-Security-Policy`#82[)](https://github.com/webjetcms/webjetcms/issues/82) Configuration - modified method of deleting a configuration variable. After deleting, the original value is automatically set from&#x20;

![](admin/settings/response-header/editor.png)

- to be the same as it will be after the server restart. In the original solution, the variable was just deleted, but its value remained set internally until the server restart (#57849).`Constants`Configuration - added option to set&#x20;
- HTTP header name[ to get the IP address of the visitor via the configuration variable ](sysadmin/pentests/README.md#Configuration) (#58237).`xForwardedForHeader`Security - added the ability to configure blocked file/directory paths via a variable&#x20;
- . By default, URLs that contain the phrase in the name are blocked `pathFilterBlockedPaths`. More can be added as needed (#PR103).`.DS_Store,debug.,config.properties,Thumbs.db,.git,.svn`Tags - modified tags displayed, in case of duplication of values. Comparison is without the influence of diacritics and upper/lower case letters&#x20;
- #115[.](https://github.com/webjetcms/webjetcms/issues/115) Mirroring - added option to display flag image instead of text in&#x20;

![](redactor/webpages/perex-duplicity-values.png)

- page language switcher[ (#54273-79).](redactor/apps/docmirroring/README.md#vytvorenie-odkazu-na-jazykové-mutácie-v-hlavičke-stránky) Change password - added option to set name and email address from which email is sent with link to change password via configuration variables&#x20;
- &#x20;a `passwordResetDefaultSenderEmail` (#58125 `passwordResetDefaultSenderName`Statistics - added summary numbers of views and visits in TOP pages (#PR136).
- News - renamed the Arrange by Priority value to Arrange by Order of Order (Priority) to align with the value in the editor (#57667-16).
- Form easily - added option to set value&#x20;
- &#x20;to insert the form e.g. in the footer of the page (#57667-16).`useFormDocId`News / News templates - moved field&#x20;
- &#x20;from the News app to the News Template to set the property directly in the news template. Original value `contextClasses` from News will still work, but it can no longer be set in the UI (#58245).`contextClasses`Document Manager - added option&#x20;
- edit the metadata of the historical version of the document[ in the document manager (#58241).](redactor/files/file-archive/README.md#úprava-historickej-verzie-dokumentu-v-manažéri) Bulk email - modified auditing of campaign changes. If a group is added it doesn't audit the entire recipient list (it was wasting a lot of records in the audit), only the list of changed groups is written. When manually adding emails, both name and email address (#58249) are still audited.
- Users - when importing, if a column in Excel does not contain a password field, a random password will be generated for new users. If the status Approved user is not specified in Excel, it is set to&#x20;
- &#x20;(#58253 `true`MultiWeb - added domain display in sidebar (#58317-0).
- MultiWeb - added the ability to set the redirect domain to be able to specify&#x20;
- &#x20;prefix (#58317-0 `https://`MultiWeb - added rights control for media groups and tags (#58317-0).
- List of forms - settings&#x20;
- the processor of the forms[, using an autocomplete field that offers a class implementing ](custom-apps/apps/multistep-forms/README.md) (#58313 `FormProcessorInterface`Dialers - added removal of spaces at the beginning and end of the string type field in the dialer data (#OSK233).
- Error correction

### Tags - fixed folder duplication in View for when saving tags, removed folder selection from other domains since tags are already separated by domain (#58121).
- Web pages - fixed hard space insertion after hyphens to apply only to page text and not to attributes or HTML tags (#OSK235).
- Datatables - corrected event processing&#x20;
- &#x20;for selected filter table input fields (#58313).`Enter`Datatables - fixed filtering when multiple&#x20;
- &#x20;tables on the page interacted with each other when filtering (#58313 `serverSide:false`Ecommerce - corrected sending email notification when order status changes (#58313).
- Ecommerce - corrected automatic order status setting after payment change (#58313).
- Documentation

### Updated all screenshots in the Czech version of the documentation (#58113).

- For the programmer

### Free fields - added the ability to specify custom columns for label and value at&#x20;
- links to the dial[. Allows more flexible setting of which property from the code list is used as displayed text and which is used as a stored value (#PR108).](frontend/webpages/customfields/README.md#číselník) Deleted unused files&#x20;
- , if you use them in your project take them from `/admin/spec/gallery_editor_perex_group.jsp,/admin/spec/perex_group.jsp` older versions[ WebJET CMS (#58073).](https://github.com/webjetcms/webjetcms/tree/release/2025.40/src/main/webapp/admin/spec) Slightly modified API in&#x20;
- NewsActionBean[, especially the setting ](../../src/webjet8/java/sk/iway/iwcm/components/news/NewsActionBean.java) which are now of the type `groupIds`. You can use `List<GroupDetails>` for the setting with the value ID field (#58073 `setGroupIds(int[] groupIds)`Fixed ability to insert quotes in application parameters (#58117).
- Prepared containers for all supported database servers in WebJET CMS for easy running in VS Code. They are located in the folder&#x20;
- &#x20;(#58137).`.devcontainer/db`E-commerce - due to changes in the implementation process&#x20;
- delivery method&#x73;**&#x20;the file needs to be modified using an update script&#x20;**&#x20;above the section `update-2023-18.jsp` (#58061).`basket`E-commerce - renamed annotation&#x20;
- &#x20;at `@PaymentMethod` a `@FieldsConfig` at `@PaymentFieldMapAttr` to unify annotations between payments and delivery methods (#58061).`@FieldMapAttr`E-commerce - in the process of implementation&#x20;
- delivery method&#x73;**&#x20;to the file&#x20;**&#x20;There are a few changes that you have to implement manually. These changes are too complex to be added using the update script `order_form.jsp` (#58061).`update-2023-18.jsp`Navigation bar - added option to use custom generator implementation&#x20;
- navigation bar[. Via the configuration variable ](redactor/apps/navbar/README.md) it is possible to set the name of the class implementing `navbarDefaultType` (#PR101).`NavbarInterface`Unsupported library removed&#x20;
- commons-lang[, replaced by a new library ](https://mvnrepository.com/artifact/commons-lang/commons-lang) commons-lang3[, v ](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3) is an update script for editing source code (#58153).`update-2023-18.jsp`Updated library&#x20;
- displaytag[ to version ](https://mvnrepository.com/artifact/com.github.hazendaz/displaytag) (#58153).`2.9.0`Modified file upload processing&#x20;
- . In Spring applications, for the file array, use instead of `multipart/form-data` directly `org.apache.commons.fileupload.FileItem`, which will be set automatically. It is no longer necessary to use a call type `org.springframework.web.multipart.MultipartFile` to retrieve the file. `entity.setDocument(MultipartWrapper.getFileStoredInRequest("document", request))` Warning:**&#x20;all occurrences need to be replaced&#x20;**&#x20;For `CommonsMultipartFile` in your code, also invalidate URL parameters in the Spring application for forced processing. The expression `MultipartFile` replace with `data-th-action="@{${request.getAttribute('ninja').page.urlPath}(\_\_forceParse=1,\_\_setf=1)}"`. You can use `data-th-action="${request.getAttribute('ninja').page.urlPath}"` to update files (#57793-3).`/admin/update/update-2023-18.jsp`Added option to create&#x20;
- project file copies[ Spring application. Just create your own version of the file in the folder ](frontend/customize-apps/README.md) similar to what is used for JSP files. WebJET CMS first looks for the file in the project folder and if it is not found it uses the standard file from `/apps/INSTALL_NAME/` Folder (#58073).`/apps/`Added option to set&#x20;
- name for CSS style[ in CSS file via comment ](frontend/examples/template-bare/README.md). The name appears in the list of styles in the editor (#58209).`/* editor title: Style Name */`Editor - modified dialog for settings&#x20;
- &#x20;\- cancelled color and size settings, `a.btn` only CSS classes are used[ as well as pr ](frontend/setup/ckeditor.md#button) (#57657-16).`button`Data tables - option to display only icons without order for&#x20;
- &#x20;if we add a class to the column `rowReorder` (#58161).`icon-only`Data tables - new options for selecting rows in the table&#x20;
- &#x20;a `toggleSelector`, more in `toggleStyle` data tables section[ (#58161).](developer/datatables/README.md#možnosti-konfigurácie) Data tables - new custom option&#x20;
- render[ functions using annotation ](developer/datatables-editor/datatable-columns.md). Allows you to display composite values from multiple fields in a column and so on (#58161).`@DataTableColumn(...renderFunction = "renderStepName")`Data tables - added option&#x20;
- redirect the user[ to another page after saving the record by calling the method ](developer/datatables/restcontroller.md#redirection-after-saving) (#58161).`setRedirect(String redirect)`Forms - Modified display of the list of forms, deleted class&#x20;
- , replaced by the class `FormAttributesDB`. Forms settings changed from table `FormService` to the table `form_attributes`. We recommend that you check the functionality of all forms on the website (#58161) after the update.`form_settings`Forms - create new table&#x20;
- &#x20;as a replacement for the table `form_settings` where the form properties are stored. Individual attributes (settings) are now stored in separate columns as one record per row. The data has been converted to the new table using `form_attributes` (#58161).`UpdateDatabase.java`Switching to a new table&#x20;
- &#x20;for form properties in `form_settings` files. You need to run the update script `.jsp`, which will adjust the necessary `update-2025-0.jsp` (#58161).`.jsp`List of forms - setting parameters/attributes of all form types redirected from the table&#x20;
- &#x20;to the new table `form_attributes` (#58161).`form_settings`Data tables - added BE support for&#x20;
- where it is possible to change the order of records directly in the data table using drag&drop (#58161).`row-reorder`Events - added event&#x20;
- Updating codes in text[ for the possibility of editing codes in the text of the page type ](developer/backend/events.md#updating-codes-in-the-text) et al (#54273-63).`!CUSTOM_CODE!`Data tables - added&#x20;
- Spring events[ for the ability to edit data in customer installations (#54273-63).](developer/backend/events-datatable.md) Testing

### Supplemented script&#x20;

- rm-same-images.sh[ to remove the same images when creating new screenshots (#58113).](../../src/test/webapp/rm-same-images.sh) 2025.40

![meme](_media/meme/2026-0.jpg ":no-zoom")

## WebJET CMS 2025.40

> **&#x20;brings an integrated&#x20;** &#x41;I Assistant**which makes working with content much easier. Allows you to automatically&#x20;**&#x63;orrect gramma&#x72;**,&#x20;**&#x74;ranslat&#x65;**&#x20;texts, design headlines, summarize articles and generate&#x20;**&#x69;llustrative picture&#x73;**&#x20;directly in the editor. This makes content creation&#x20;**&#x66;aster, more accurate and more creativ&#x65;**&#x20;than ever before.**&#x53;ignificant changes also concern&#x20;
>
> brand&#x73;**&#x20;a&#x20;**&#x6E;ews templates**that have been reworked into&#x20;**&#x73;eparate database table&#x73;**&#x20;with support for separation by domain. This brings higher&#x20;**&#x63;larity, easier administratio&#x6E;**&#x20;and the ability to efficiently customize content for multiple sites. The user environment was&#x20;**&#x6F;ptimised for smaller screen&#x73;**&#x20;- the system automatically adjusts the window display and maximises usable space.**&#x4F;n a technical level, the obsolete Struts Framework has been removed. This makes WebJET CMS more powerful, more stable,&#x20;
>
> Safe&#x72;**&#x20;and ready for further development of modern web solutions.**&#x47;roundbreaking changes

### Removed from&#x20;

- , you need to make an update `Struts Framework` files, Java classes and edit file `JSP`, more in `web.xml` section for the programmer[ (#57789).](#pre-programátora) If you are using Tomcat application server version 9.0.104 and above, you need to&#x20;
- update settings[ parameter ](install/versions.md#changes-when-switching-to-tomcat-90104) at `maxPartCount` element (#54273-70).`<Connector`Tags - split by domains - at startup, a copy of tags for each domain is created (if splitting data by domains is used - configuration variable set&#x20;
- ). Tag IDs for the website and gallery are being updated. You need to manually check the tag IDs for all news apps and other apps that contain tag IDs - the update will try to fix them, but we recommend checking the IDs. See the section for the programmer for more information. (#57837).`enableStaticFilesExternalDir=true`News -&#x20;
- news templates[ converted from a definition via translation keys to a custom database table. When WebJET starts, the records are converted from the original format. They are separated by domain, if they contain a domain alias they are created only in the corresponding domain (#57937).](frontend/templates/news/README.md) Security - stricter control of administration URLs - it is necessary that the URL in the administration has a character at the end&#x20;
- , the incorrect address is `/` or `/admin/v9/webpages/web-pages-list`, correct `/apps/quiz/admin` or `/admin/v9/webpages/web-pages-list/`. It is necessary for the programmer to check the URL definitions in the files `/apps/quiz/admin/` (#57793).`modinfo.properties`AI Assistant

### In today's world, artificial intelligence is all around us and of course, WebJET as a modern content management system does not want to be left behind. That's why we are proud to present the new version of WebJET CMS, where we have integrated&#x20;

advanced AI tools[.](redactor/ai/README.md) These features make it easy to create and edit content - from correcting grammar, to translating text, to designing captions, to generating illustrative images.

![](redactor/ai/datatables/ckeditor-assistants.png)

YouTube video player

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/LhXo7zx7bEc" title="Web pages" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

### AB Testing - added option&#x20;
- show AB version[ according to the status of the logged-in user - the non-logged-in user will see the A version and the logged-in user will see the B version. You activate the mode by setting the configuration variable ](redactor/apps/abtesting/README.md) to the value of `ABTestingForLoggedUser` (#57893).`true`Page Builder
- [ - modified visual to better fit the current WebJET CMS design (#57893).](redactor/webpages/pagebuilder.md) Allowed to display pages containing&#x20;

![](redactor/webpages/pagebuilder-style.png)

- &#x20;in the URL from the system folders, so that such a technical page does not get in the way of your standard web pages (#57657-8 `404.html`Tags - split tag display by the currently selected domain, so you can have tags separately for each domain in WebJET (#57837).
- Cloning structure - added information about the configured translator and how many free characters are left for translation (#57881).
- Structure mirroring - option to delete added&#x20;
- &#x20;values for the selected folder (recursive). Make it easy to undo/redo page mirroring (#57881).`sync_id`Mirroring - adding a new section&#x20;

![](redactor/apps/clone-structure/clone_structure_set_translator.png)

- mirroring[ to track and manage linked folders and pages after mirroring actions (#57941).](redactor/webpages/mirroring/README.md) When selecting an image or video file in the page editor, only the appropriate file types are displayed in the explorer, the others are filtered (#57921).

![](redactor/webpages/mirroring/groups_datatable.png)

- Templates

### New section added&#x20;

- News templates[ to manage and administer newsletter templates (#57937).](frontend/templates/news/README.md) User interface

![](frontend/templates/news/news-temps-datatable.png)

### When using a small monitor (window height less than 760 pixels), the window is automatically displayed full screen and the header and footer are shrunk (the window title is in a smaller font). This will increase the amount of information displayed, which is especially needed in the web page section. This is used for windows using the CSS class&#x20;

- , which are the actual website, photo gallery, image editor and users (#57893).`modal-xl`Added option in the editor to click on the image icon at the beginning of the field to display it in a new tab.

![](redactor/webpages/pagebuilder.png)

- Applications

![](developer/datatables-editor/field-type-elfinder.png)

### Added option to show the application only to logged in/not logged in user. Mode is set in the tab&#x20;

- View application settings[ in the site editor (#57893).](redactor/webpages/working-in-editor/README.md#karta-zobrazenie) Redesigned application properties settings in the editor from the old code in&#x20;

![](custom-apps/appstore/common-settings-tab.png)

&#x20;at `JSP` Application. Apps also automatically get the ability to set `Spring` display on devices[. The design is consistent with the rest of the WebJET CMS and data tables (#57409).](custom-apps/appstore/README.md#podmienené-zobrazenie-aplikácie) Carousel Slider
- [Emoticons](redactor/apps/carousel_slider/README.md)
- [Forum/Discussion](redactor/apps/emoticon/README.md)
- [Questions and answers](redactor/apps/forum/README.md)
- [Users](redactor/apps/qa/README.md)
- [Impressive presentation](redactor/apps/user/README.md)
- [Restaurant menu](redactor/apps/app-impress_slideshow/README.md)
- [Slider](redactor/apps/restaurant-menu/README.md)
- [Slit slider](redactor/apps/slider/README.md)
- [Social icons](redactor/apps/app-slit_slider/README.md)
- [Video](redactor/apps/app-social_icon/README.md)
- [Menu](redactor/apps/video/README.md)

![](redactor/apps/app-slit_slider/editor-items.png)

### If&#x20;

- website menu[ does not have a root folder specified (the value is set to 0), the root folder for the currently displayed web page is automatically used. This is convenient if you are displaying menus in multiple languages where each is a root folder - you don't need to have menus/headers for each language separately, just one common one (#57893).](redactor/apps/menu/README.md) Statistics

### In the sections&#x20;

- traffic[ added summary number of Views, Visits and Number of different users for easy overview of total traffic for the selected period (#57929).](redactor/apps/stat/README.md#návštevnosť) In the sections&#x20;

![](redactor/apps/stat/stats-page.png)

- erroneous pages[ added filtering by bots (applied only to newly recorded data) and sum count in footer. Need to edit the page ](redactor/apps/stat/README.md#erroneous-pages) in your project by adding an object `404.jsp` to call `request` (#58053).`StatDB.addError(statPath, referer, request);`Optional fields

![](redactor/apps/stat/error-page.png)

### Added support for new types&#x20;

- optional fields[:](frontend/webpages/customfields/README.md) Choosing a website folder
  - [ (#57941).](frontend/webpages/customfields/README.md#výber-priečinku-webových-stránok) Choosing a website
  - [ (#57941).](frontend/webpages/customfields/README.md#výber-webovej-stránky) Security

![](frontend/webpages/customfields/webpages-doc-null.png)

Fixed a possible vulnerability in Safari with a special URL pointing to a file archive combined with a nice 404 page (#57657-8).![](frontend/webpages/customfields/webpages-group-null.png)

### Other minor changes

- Change Audit - Search - the Type field is alphabetized (#58093).

### Ecommerce - added option to set&#x20;
- root folder
- &#x20;with the list of products using the configuration variable [if the automatic search by the inserted product list application (#58057) is not satisfied.](redactor/apps/eshop/product-list/README.md) Ecommerce - application for setting up payment methods moved from folder `basketAdminGroupIds` to standard&#x20;
- &#x20;(#58057 `/apps/eshop/admin/payment-methods/`Ecommerce - when an order is deleted, its items and payments are also deleted from the database (#58070).`/apps/basket/admin/payment-methods/`Server monitoring - current values - added database server type (MariaDB, Microsoft SQL, Oracle, PostgreSQL) (#58101).
- Translator - at the translator&#x20;
- &#x20;the handling of returned error messages has been improved, to identify the problem more accurately (#57881).
- Translator - added support for implementing multiple translators and their automatic processing/use (#57881).`DeepL`Translator - added automatic&#x20;
- auditing the number of characters consumed
- &#x20;in every translation. In the audit record type [ shall be entered in the column ](admin/setup/translation.md) records the amount of credits consumed during the translation. The number of available characters is also audited, the result is cached and updated again in 5 minutes at the earliest (#57965).`TRANSLATION`Explorer - optimized loading, fixed duplicate library reading `EntityID` (#57997).
- Error correction`jQuery UI`Data Tables - corrected options setting in the external filter selection menu (#57657-8).

### Cloning structure - fixed validation of specified folder ids and added error message (#57941).
- Gallery - added support for selecting a gallery folder, in the Gallery application in a web page, when using domain aliases, and editing a gallery entry with a domain alias (#57657-11).
- Web pages - fixed displaying the list of pages when displaying folders as tables (#57657-12).
- Charts - fixed displaying a large number of legends in charts, automatically use scrolling in legends (#58093).
- Documentation
- Added documentation for setup and use&#x20;

### two-step verification/authorisation
- &#x20;(#57889).[For the programmer](redactor/admin/logon.md#two-step-verification) Cancelled class&#x20;

### which was used in imports from&#x20;

- &#x20;format in `ImportXLSForm` spec/import\_xls.jsp`XLS`. Technically the class is not needed, just delete the reference in the JSP and modify the form to a standard HTML form (#57789).[Improved update script ](../../src/main/webapp/admin/spec/import_xls.jsp) for File Archive - can update standard changes and add necessary changes to your version&#x20;
- &#x20;and auxiliary classes (#57789).`/admin/update/update-2023-18.jsp`Class `FileArchivatorBean` replaced by an object&#x20;
- , class `org.apache.struts.action.ActionMessage` Replaced by `String` (#57789).`ActionMessages`Cancelled framework `List<String>`, tags&#x20;
- &#x20;substituted for the corresponding `Struts`, beware `<logic:present/iterate/...` For `<iwcm:present/iterate/...`.`<bean:write`In the Java code, due to the removal of `<iwcm:beanWrite` the following changes:
- &#x20;substituted for `Struts` substituted for&#x20;
  - `ActionMessage` Returns `String`
  - `ActionMessages` instead of `List<String>`
  - `BasicLdapLogon.logon` Replaced by `List<String>` Amcharts - added support for specifying a function to transform text in category labels for a chart type `ActionMessages`
  - `org.apache.struts.util.ResponseUtils.filter` (#58093).`sk.iway.iwcm.tags.support.ResponseUtils.filter`
- Amcharts - added support for specifying a function to transform the text in the legend of a chart type `PIE` (#58093).
- Amcharts - added option to hide tooltip when value is `LINE` or&#x20;
- &#x20;in the chart type `null` (#58093).`0`You can use a script to convert both JSP and Java files `LINE`. If you specify a value as the path&#x20;

&#x20;replacement shall also be made in `/admin/update/update-2023-18.jsp` files. The problem is running the project if it contains errors. But you can folder `java` renamed to `../java/*.java` to run a clean WebJET. You can then use the update script. This scans and updates the folder `src/main/java` Also `src/main/java-update`.`../java/*.java`In the file `../java-update/*.java` initialization is no longer required&#x20;

, delete the entire `WEB-INF/web.xml` a section containing `Apache Struts` a `<servlet>` Containing `<servlet-class>org.apache.struts.action.ActionServlet</servlet-class>`.`<servlet-mapping>`Split tags by domain (if the configuration variable is set `<servlet-name>action</servlet-name>`), so that you can easily have separate tags for each domain. When WebJET starts, it copies the existing tags for all defined domains. It will skip tags that are set to display only in a specific folder, where according to the first folder it will set the domain for the tag. Updates the tags for News, that is, for the application&#x20;
- &#x20;where it searches for the expression `enableStaticFilesExternalDir=true` a `/components/news/news-velocity.jsp` where it tries to update the tag ID according to the domain of the web page. The information is written to the history and a record is created in Audit detailing how the `perexGroup` replaced, example:`perexGroupNot`For the first `INCLUDE` tags with ID 625 and 626 were removed because they do not show up in the folder/domain - they were set to show only for a specific folder. In the second&#x20;

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

&#x20;the signs have been changed `INCLUDE` on the newly created `INCLUDE` a `3+645` For `1438+1439`.`794`| perex\_group\_id | perex\_group\_name | domain\_id | available\_groups |
| -------------- | -------------------- | --------- | ---------------- |
| 3 | next perex group | 1 | NULL |
| 645 | deletedPerexGroup | 1 | NULL |
| 794 | calendar-events | 1 | NULL |
| 1438 | next perex group | 83 | NULL |
| 1439 | deletedPerexGroup | 83 | NULL |
| 1440 | calendar-events | 83 | NULL |`1440`Before the update was triggered, only records existed in the database&#x20;

which set&#x20;

. Records `3, 645 a 794` arose during the update for `domain_id=1`.`1438, 1439 a 1440`Data tables - added support for editing `domain_id=83` local JSON data
- &#x20;(#57409).[Data tables - added extension ](developer/datatables-editor/field-datatable.md#local-json-data) Row Reorder
- &#x20;for the ability to arrange the list using the function [ (#57409).](https://datatables.net/extensions/rowreorder/) Datatables - Added setting option `Drag&Drop` Footers for sum of values
- &#x20;(#57929).[Applications - added the ability to use local JSON data to set application items, for example items for ](developer/datatables/README.md#footer-for-sum-of-values) impressive presentation
- &#x20;(#57409).[2025.18](redactor/apps/app-impress_slideshow/README.md) View Full Version&#x20;

![](redactor/apps/app-impress_slideshow/editor-items.png)

## 2025.18

> &#x20;brings a completely redesigned module **E-commerce** with support **payment gateway GoPay** and an improved order list. Application **News calendar** has been separated as **standalone application** and at the same time we have redesigned the settings of several applications in the page editor. **Document Manager** (formerly File Archive) has passed **visual and functional reboot** including new tools for managing, exporting and importing documents.**The system has also been improved&#x20;**&#x42;ulk email
>
> &#x20;with new options for the sender and a more convenient choice of recipients. **Reservations** have gained new opportunities as **overbooking** creating bookings back in time and sending notifications to specific emails for each booking object.**We have optimized the number of files in&#x20;**&#x45;xplore
>
> leading to **faster loading** and added new information to **Server monitoring**.**Groundbreaking changes**News Calendar app separated into a separate app, if you use News Calendar you need to edit the path&#x20;

### &#x20;at&#x20;

- &#x20;(#57409 `/components/calendar/news_calendar.jsp`Modified Spring and JPA initialization, see the programmer's section (#43144) for more information.`/components/news-calendar/news_calendar.jsp`Redesigned backend part of ecommerce application, more in the section for programmer (#57685).
- Data tables
- When the numeric value filter is set to from-to, the field is enlarged to better display the entered value, similar to what the date field does (#57685).

### The File Archive application has been converted into a Spring application. For more information, see the programmer's section (#57317).

- The E-Commerce application was on&#x20;
- &#x20;partly remodeled. For more information see the section for the programmer (#56609).
- Document Manager (File Archive)`BE`List of files

### &#x20;redesigned with the addition of new logic compared to the old version. Read more in&#x20;

- **Archive files** (#57317).[Category Manager](redactor/files/file-archive/README.md) repaired and redesigned. Read more in&#x20;

![](redactor/files/file-archive/datatable_allFiles.png)

- **Category Manager** (#57317).[Product Manager](redactor/files/file-archive/category-manager.md) has been added as a new section. Read more in&#x20;
- **Product Manager** (#57317).[Exporting master files](redactor/files/file-archive/product-manager.md) has been modified to offer wider file export options and improve the clarity of the listings. Read more in&#x20;
- **Exporting master files** (#57317).[Importing master files](redactor/files/file-archive/export-files.md) has been corrected and modified to work with the extended export options. Read more in&#x20;

![](redactor/files/file-archive/export_all.png)

- **Importing master files** (#57317).[Indexing](redactor/files/file-archive/import-files.md) documents in search engines like&#x20;
- **&#x20;modified to not index old/historical versions of documents and documents out of date (HTTP header set&#x20;**). Indexing of these documents can be enabled in the editor in the document manager (#57805).`Google`Applications`X-Robots-Tag=noindex, nofollow`Redesigned application properties settings in the editor from the old code in&#x20;

### &#x20;at&#x20;

&#x20;Application. Apps also automatically get the ability to set `JSP` display on devices`Spring`. The design is consistent with the rest of the WebJET CMS and data tables (#57409).[Survey](custom-apps/appstore/README.md#conditional-application-view) Banner system
- [Date and time, Date and name day](redactor/apps/inquiry/README.md)
- [ - merged into one common application](redactor/apps/banner/README.md)
- [Questionnaires](redactor/apps/app-date/README.md) Bulk e-mail
- [Calendar of events](redactor/apps/quiz/README.md)
- [News calendar](redactor/apps/dmail/form/README.md)
- [Site Map](redactor/apps/calendar/README.md)
- [Media](redactor/apps/news-calendar/README.md)
- [Related sites](redactor/apps/sitemap/README.md)
- [Rating](redactor/webpages/media.md)
- [Reservations](redactor/apps/related-pages/README.md)
- [Accelerated loading of application data in the editor - data is loaded directly from the server, no need to make a REST service call (#57673).](redactor/apps/rating/README.md)
- [Modified visual - application title moved to main window when inserted into the page (instead of the original Application title) to increase the size of the application setup area (#57673).](redactor/apps/reservation/reservation-app/README.md)

![](redactor/apps/dmail/form/editor.png)

- Added application screenshots in Czech language for most applications (#57785).
- Bulk e-mail

![](redactor/apps/menu/editor-dialog.png)

- Moved Web page field

### &#x20;- now located in front of the field&#x20;
- **Subject** so that when you select a page, the subject is automatically filled in according to the name of the selected web page (#57541).**Modifying the order in the Groups tab** - email groups are now shown before user groups (#57541).
- **New options for sender name and email** - if the configuration variables are&#x20;
- **&#x20;a&#x20;** &#x20;set, the following values are used. If they are blank, the system will automatically fill in the name and email of the currently logged in user. (#57541)`dmailDefaultSenderName`With these variables it is possible to set `dmailDefaultSenderEmail` fixed values
  - &#x20;(e.g. company name) for all **Campaigns**, regardless of who is logged in.[Bulk email - optimizing recipient list creation - tab ](redactor/apps/dmail/campaings/README.md) groups

![](redactor/apps/dmail/campaings/editor.png)

- &#x20;moved to the dialog box. After selecting a group of recipients, you can immediately see them in the Recipients tab and can easily edit them, no need to save the email first to view the recipients (#57537).[Unsubscribe - when you directly enter your email to unsubscribe (not by clicking on the link in the email), a confirmation email is sent to the email address you entered. In it you need to click on the unsubscribe link. The original version did not check the validity/ownership of the email address in any way, and it was possible to unsubscribe from someone else's email (#57665).](redactor/apps/dmail/campaings/README.md#pridanie-zo-skupiny) News calendar

![](redactor/apps/dmail/campaings/users.png)

- News Calendar separated as a separate app, originally it was an option in the Calendar app (#57409).

### Displays a calendar linked to the news list with the option to filter news by the selected date in the calendar.

- Server monitoring
- Added table with information about database connections and memory occupied (#54273-61).

![](redactor/apps/news-calendar/news-calendar.png)

### Added information about the version of libraries&#x20;

- &#x20;in the Server Monitoring-Actual Values section (#57793).
- Reservations`Spring (Core, Data, Security)`Support for overbooking

### &#x20;- allows administrators to create multiple reservations&#x20;
- **&#x20;on the same date (#57405).** &#x49;mproved import validation`overbooking` - it is now possible to import&#x20;
- **booking** well into the past, or to create [ data import reservations (#57405).](redactor/apps/reservation/reservations/README.md) Support for adding bookings to the past`overbooking` - allows administrators to create reservations in the past (#57389).
- **To&#x20;** &#x72;eservation objects
- &#x20;a column has been added [Emails for notifications](redactor/apps/reservation/reservation-objects/README.md) which for each valid email entered (separated by a comma) will send an email if the reservation has been added and approved (#57389).**Booking confirmation notifications and other system notifications can be set to the sender's name and email using configuration variables&#x20;**&#x20;(#57389).
- New application added `reservationDefaultSenderName,reservationDefaultSenderEmail` Reservation of days
- , for booking all-day objects for a specific interval using the integrated calendar (#57389).[Gallery](redactor/apps/reservation/day-book-app/README.md) Added support for&#x20;

![](redactor/apps/reservation/day-book-app/app-table_B.png)

### change folder

- &#x20;image, which allows **move image** when editing or duplicating to another folder. This is useful when duplicating, when you can directly set a new folder where you want to duplicate the image. If you specify a folder manually and it doesn't exist, it will automatically be created and its properties will be set according to the nearest existing parent folder (#57885).[Ecommerce](redactor/apps/gallery/README.md#) !>&#x20;

### Warning:

&#x20;due to the database update, the first start of the server may take longer - values for the number of items and price are calculated in the database for faster loading of the order list.**Added card&#x20;**&#x50;ersonal information
- &#x20;to the order list - contains detailed information about **delivery address** as well as **contact information** all in one place (#57685).**Added card&#x20;**&#x4F;ptional fields
- &#x20;to the order list - **optional fields** as needed for implementation (#57685).[Export of order list - columns total price with VAT and number of items (#57685) added.](frontend/webpages/customfields/README.md) Order form - added option to define available list of countries via configuration variable&#x20;
- &#x20;(#57685).
- Modified card data display `basketInvoiceSupportedCountries` Personal data
- &#x20;in the list of orders, their logical division into parts for better overview (#57685).**Columns have been added to the order list&#x20;**&#x4E;umber of items
- , **Price without VAT** a **Price with VAT**. Values are automatically recalculated when order items are changed (#57685).**Added the ability to view the product web page by clicking on the icon in the item list, the product will also be displayed in the Preview tab when opening the item editor (#57685).**&#x49;n the order list, redesigned country selection via the selection field, which offers only countries defined by a constant&#x20;
- &#x20;(#57685).
- New version `basketInvoiceSupportedCountries` configurations of payment methods

![](redactor/apps/eshop/invoice/editor_personal-info.png)

- &#x20;and integration to payment gateways. Data is separated by domain. We have added support for [payment gateway GoPay](redactor/apps/eshop/payment-methods/README.md), which also means accepting payment cards, supporting [, payments via internet banking, ](https://www.gopay.com), `Apple/Google Pay` etc. In addition, payments by bank transfer and cash on delivery are supported. For each type of payment it is also possible to set a price, which will be automatically added to the order when the option is selected. The set payment methods are also automatically reflected in the options when the customer creates an order.`PayPal`New Order List application with a list of orders of the currently logged in user. By clicking on an order, you can view the order detail and download it in PDF format (#56609).`Premium SMS`Other minor changes

![](redactor/apps/eshop/payment-methods/datatable.png)

- Administration search - customized interface&#x20;

### &#x20;a&#x20;

- &#x20;(#57561 `RestController`Explorer - faster loading and lower server load by reducing the number of files/server requests (#56953).`Service` - added support for&#x20;
- hiding parent folders
- `dt-tree-dir-simple` in the displayed tree structure by attribute [ (#57885).](developer/datatables-editor/field-json.md#možnosti-classname) Error correction`data-dt-field-hideRootParents`Bulk email - added duplication of recipient list when duplicating a campaign (#57533).

### Data tables - import - modified logic&#x20;
- Skip erroneous entries
- &#x20;when importing so that generic errors are also handled with this option&#x20;**&#x20;and ensured that the import was completed without interruption. These errors are then displayed to the user via a notification during import (#57405).**&#x46;iles - fixed file/folder size calculation in explorer footer and when showing folder detail (#57669 `Runtime`Navigation - fixed tab navigation in mobile view (#57673).
- Autocomplete - corrected error in type field&#x20;
- , where the first value obtained in the case of&#x20;
- &#x20;was not correct (#57317).`Autocomplete`For the programmer`jstree`!>&#x20;

### Warning:

&#x20;modified Spring and JPA initialization, follow **instructions in the installation section**.[Other changes:](install/versions.md#changes-in-the-transition-to-the-20250-snapshot) Added option to perform&#x20;

additional HTML/JavaScript code

- &#x20;in Spring application with annotation [ by setting the attribute ](custom-apps/appstore/README.md#additional-html-code) (#57409).`@WebjetAppStore`Added field type in datatable editor `customHtml = "/apps/calendar/admin/editor-component.html"` IMAGE\_RADIO
- &#x20;to select one of the options using the image (#57409).[Added field type ](developer/datatables-editor/standard-fields.md#image_radio) For&#x20;
- file upload`UPLOAD` in the datatable editor (#57317).[When initializing ](developer/datatables-editor/field-file-upload.md) nested datatables
- &#x20;added option to edit [ object by specifying a JavaScript function in the attribute ](developer/datatables-editor/field-datatable.md) Annotation (#57317).`columns`Added support for getting sender name and email for various email notifications using `data-dt-field-dt-columns-customize` (#57389).
- Added option to set root folder for `SendMail.getDefaultSenderName(String module, String fallbackName), getDefaultSenderEmail(String module, String fallbackEmail)` field of type JSON
- &#x20;in both ID and path format: [ or ](developer/datatables-editor/field-json.md).`@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")`Running background tasks is only done after complete initialization, including `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "26")` (#43144).
- Added option to set `Spring` all HikariCP properties
- &#x20;(#54273-61).[Added check to see if the database driver supports sequence setup (#54273-61).](install/setup/README.md#vytvorenie-db-schémy) Modified function&#x20;
- if you are listening to change the card we recommend to use event type&#x20;
- where in `WJ.headerTabs` you get the card that was clicked (#56845-20250325 `$('#pills-tabsFilter a[data-wj-toggle="tab"]').on('click', function (e) {`Converted Document Manager (File Archive) to Spring application. If you are using the original version and want to keep it, you need to add back the files `e` a&#x20;
- &#x20;and the necessary classes from `/components/file_archiv/file_archiv.jsp` older version of WebJET CMS`components/file_archiv/editor_component.jsp`.[Document Manager (File Archive) - modified API ](https://github.com/webjetcms/webjetcms/tree/release/2025.0/src/webjet8/java/sk/iway/iwcm/components/file_archiv) Returns&#x20;
- , you can use `FileArchivatorBean.getId()/getReferenceId()/saveAndReturnId()` for including `Long` Values. Delete unused methods, transfer them to your classes if needed. We do not recommend modifying WebJET classes, create new classes of type `getFileArchiveId()` in your project where you add methods. If we have deleted the whole class you are using (e.g. `int`), you can add it directly to your project (#57317).`FileArchivatorProjectDB`Added automatic setting of column filtering to value `FileArchivatorAction`, if the value is&#x20;
- &#x20;(unset) and it is a column that is nested, such as `false` Columns (#57685).`null`Added option `editorFields` of special arrangement
- &#x20;by overwriting the method [ (#57685).](developer/datatables/restcontroller.md#usporiadanie) Added option in annotation `DatatableRestControllerV2.addSpecSort(Map<String, String> params, Pageable pageable)` set attribute&#x20;
- &#x20;which will determine `@DataTableColumn` columns for arrangement`orderProperty`, e.g. [. Convenient for ](developer/datatables/restcontroller.md#Arrangement) classes that can aggregate data from multiple columns (#57685).`orderProperty = "contactLastName,deliverySurName"`For an array type `EditorFields` with set&#x20;
- &#x20;added tree structure of parent folders for better `dt-tree-dir-simple` tree structure display`data-dt-field-root` (before, folders were displayed only from the specified root folder). Added the ability to define a list of folders that will not appear in the tree structure using a configuration variable set to [.](developer/datatables-editor/field-json.md) Selection `data-dt-field-skipFolders` editable field
- &#x20;modified so that when a new record is added, that record is automatically selected in the field (#57757).[Redesigned e-commerce application on ](developer/datatables-editor/field-select-editable.md) parts. Since new classes are already being used, you must:
- make use of the update script `BE` for basic updating of your JSP files
  - as the type is now used `/admin/update/update-2023-18.jsp` instead of&#x20;
  - , you must additionally adjust all comparisons of these values. Type `BigDecimnal` is not compared classically using `float` but by means of `BigDecimal` you need to remove file calls or add back any files that were removed because they were not used`<, =, >`Testing`BigDecimal.compareTo( BigDecimal )`
  - Media - added test for embedding media in a web page if the user does not have the right to all media (#57625).

### Web pages - added test for creating a new page with publishing in the future (#57625).
- Gallery - added watermark test with image comparison, added rights check test (#57625).
- Web pages - added test for optional fields when creating a web page (#57625).
- Allure - jUnit test results added to the common Allure report (#57801).
- 2025.0.52
- Corrected version of the original version 2025.0.

![meme](_media/meme/2025-18.jpg ":no-zoom")

## Security - disabled redirection to external URLs after logging in via the login form.

> Data Tables - Fixed error displaying selection fields when enabling Edit mode in grid view (#57657-16).
- Data tables - corrected saving a new record via keyboard shortcut&#x20;
- &#x20;\- after saving, the returned values are set back to the editor to correctly set the record ID for further editing (#57657-16).
- Forms - modified field name generation to not contain a period (#57657-16).`CTRL+S`Bulk email - modified the buttons to start/stop sending bulk email to be easier to understand&#x20;
- &#x20;a&#x20;
- &#x20;(#54273-81 `play`Bulk email - corrected storage in Oracle database, subject field set as mandatory (#54273-81).`stop`Bulk Email - Domain Limits - fixed loading limits for domains from database (#54273-81).
- Bulk email - fixed user ID setting when adding a group if there are multiple users with the same email (#58217).
- Spam protection - fixed bug checking the time range between form submissions/searches (#57657-16).
- Web pages - fixed tree structure when moving items through&#x20;
- &#x20;in case of descending order setting (#MF-1199).
- Website - fixed folder search in the Trash tab (#58081 `Drag&Drop`Web pages - corrected sending notification of approval/disapproval of a page if there is no approver with notification, added list of changes in various fields of a web page (#58007).
- 2025.0.50
- Corrected version of the original version 2025.0.

## Security - fixed the ability to log in if the password contains diacritics.

> Security - Fixed possible vulnerability in page synchronization (#55193-7).
- Security - added the ability to configure blocked file/directory paths via a variable&#x20;
- . By default, URLs that contain the phrase in the name are blocked&#x20;
- . More can be added as needed (#PR103 `pathFilterBlockedPaths`Banner system - fixed YouTube video banner display (#55193-7).`.DS_Store,debug.,config.properties,Thumbs.db,.git,.svn,/WEB-INF/,./`Data tables - corrected display of advanced export options (#58113).
- Calendar of Events - fixed saving of the description field where HTML code was not allowed to be saved (#58113).
- News - fixed exclusion of main pages for specified root folder (#57657-15).
- Websites - PageBuilder - corrected UI language to the language of the logged-in user (not the language of the website) (#58133).
- Web pages - modified the ability to embed HTML code in the folder and web page name. Enabled embedding secure HTML code in the web page name (
- ) without modifying it, it works in the navigation bar, news and so on. For calling&#x20;
- &#x20;value is returned with the HTML code removed, since it is assumed to be inserted into the `AllowSafeHtmlAttributeConverter` Signs. If you need to get the caption with HTML code you can use the call `${ninja.page.seoTitle}`. The folder is replaced by the `title` for the entity `${ninja.page.seoTitleHtml}`, whereas the sign `/` is used to separate individual folders (#54273-75 `&#47;`Web pages - fixed display of PageBuilder mode set directly in the page template (#57657-15).`/`PDF - corrected duplication of absolute path to&#x20;
- &#x20;folder with fonts at the specified URL in the variable&#x20;
- &#x20;(#58185).`fonts`Oracle/Microsoft SQL - fixed SQL errors and data types in basic tests/tables, used clean databases with `pdfBaseUrl` basic data
- &#x20;(#58185).[2025.0.40](developer/testing/README.md#deleting-the-database) Corrected version of the original version 2025.0.

## !>&#x20;

> Warning:

&#x20;possible change in the behaviour of fields of type&#x20;**&#x20;for bulleted list in data tables (#54273-72).**&#x50;DF - corrected path settings to `quill` fonts folder. You need to specify the full path on the server disk (#57657).
- Updated library `fonts` to version 1.11.0.
- Initialization - added file existence check `Apache Commons BeanUtils` to not write an error on the public nodes of the cluster when starting WebJET (#54273-68).
- Security - added expression control `autoupdate.xml` for URL parameters to prevent an XSS attack (#54273-68).
- Field type `onwebkit` (small HTML editor used in Banners, Gallery...) - fixed duplication&#x20;
- &#x20;element if it contains a CSS class or other attribute (#54273-69).`QUILL`Security - in the annotation `P` allowed attribute insertion&#x20;
- &#x20;For `@AllowSafeHtmlAttributeConverter` a `alt,title` for elements `img` (#54273-69).`class`Security - updated library `a,img,div,span,p,h1,h2,h3,h4,h5,h6,i,b,strong,em` to version&#x20;
- &#x20;(#54273-69).`hibernate-validator`Security - fixed possible vulnerability in AB testing.`6.2.5.Final`Security - corrected unnecessary reading&#x20;
- &#x20;in JSON object&#x20;
- .`dataAsc`Security - reduced amount of text when logging an error `DocBasic` when the web server is overwhelmed/attacked (#BVSSLA-34).
- Administration - added the ability to insert `Session has already been invalidated` additional CSS/JavaScript
- &#x20;file to the administration part, e.g. for custom CSS styles for [Quill field](custom-apps/apps/customize-admin.md) (#54273-69).[Data tables - for Oracle and Microsoft SQL disabled the option to sort by columns containing long text (](developer/datatables-editor/standard-fields.md#quill)) - these database systems do not support ordering when using this data type. The attribute must in&#x20;
- &#x20;have an annotation `ntext/clob`, which disables the ordering option for the specified column for the specified database. For MariaDB and PostgreSQL, ordering is still supported (#54273-70 `Entite`Data Tables - corrected search if you select "Starts at" in one field and "Ends at" in another field, for example (#54273-70).`@Lob`Data Tables / Administration Lookups - special characters (e.g. quotes) allowed for lookups in data tables (#54273-70).
- Forms - hid unnecessary button to create a new record in the list of completed forms (#54273-70).
- Form easy - corrected setting of required fields in form items (#57657-12).
- Web pages - added the ability to insert HTML code into folder names such as&#x20;
- &#x20;\- in the list of web pages, the HTML code is not executed for security reasons, but in applications such as the Menu and Navigation Bar, the HTML code is displayed correctly and executed. An important condition is that the code must contain a closing tag&#x20;
- . The HTML code is also removed from the automatically generated URL. Only safe HTML code allowed in the class `WebJET<sup>TM</sup>` (#54273-70 `</...>`Website - fixed media tab display for old users who did not have media management rights (#57657-10).`AllowSafeHtmlAttributeConverter`Data tables - for fields of type small HTML editor (
- )&#x20;
- modified behaviour for bulleted list`quill` (HTML tag **). The original editor set for this case to&#x20;** &#x20;element attribute `ul` and failed to use directly `li` element instead of `data-list="bullet"` element. The new behaviour uses the correct HTML tag `ul` and removes an unnecessary attribute `ol` (#54273-72 `ul`Gallery - fixed display of perex groups if there are more than 30 in gallery and image editor - displayed as selection box. Fixed loading and saving of groups in image editor (#57657-9).`data-list="bullet"`Explorer - fixed uploading the whole folder with subfolders to&#x20;
- &#x20;via Explorer - correct creation&#x20;
- &#x20;a `/images/gallery` Figures (#57657-11).`o_`Gallery - fixed sharing icon display in gallery type `s_` (#57657-11).
- Gallery - fixed display of folder list when using domain aliases (displaying only folders from currently selected domain) (#57657-11 `PrettyPhoto`Gallery - fixed watermark retrieval for galleries using the domain alias (#57657-11).
- File Upload - modified processing of files uploaded via&#x20;
- . The modified version will allow you to restart the server and then after restoring&#x20;
- &#x20;process the file correctly. Added warning display if the file is of an unauthorized type (#PR75).`/XhrFileUpload`Gallery - cancelled URL setting when displaying photos in gallery type `session` for easier use of the back button in the browser (#57657-12).
- News - Fixed display settings for main pages from folders (#57657-12).`PrettyPhoto`PDF - fixed user logout error when generating PDF if PDF contains images embedded via&#x20;
- &#x20;prefix (#57657-13).
- 2025.0.23`/thumb`Corrected version of the original version 2025.0.

## Error correction

> Data Tables - fixed incorrect display of tabs that should not be displayed when creating a new record (e.g. in templates) (#57533).

### Data tables - added limit of number of records when displaying all. The value is the same as the maximum number of rows for export, it is set in the configuration variable&#x20;
- &#x20;(#57657-2).
- Data tables - corrected number of records on page when page contains navigation tabs (#57725-1 `datatablesExportMaxRows`Data Tables - corrected Duplicate heading instead of Edit when duplicating a record, modified duplicate button icon (#57725-3).
- Data tables - unified name&#x20;
- &#x20;columns from the original&#x20;
- &#x20;to a unified `ID`. For `ID, Id, id` no need to set `ID` attribute, the key is automatically used `DataTableColumnType.ID`. Some translation keys deleted as not needed (#49144)`title`Image editor - when editing an image from a remote server, added notification about the need to download the image to the local server (#57657-2 `datatables.id.js`Web pages - corrected the insertion of the block containing the application (#57657-2).
- Website - supplemented&#x20;
- &#x20;object when inserting an application into a new web page (#57389).
- Web pages - pages in the trash will no longer appear in the Unapproved tab, if the approver clicks on the link in the email an error Page is in trash will appear so that a page that has been deleted in the meantime is not accidentally approved (#54273-62).`ninja`Web pages - approvals - corrected list loading in the Unapproved tab when using the database server&#x20;
- &#x20;(#54273-62).
- Web site - fixed cluster nodes updating when tags change (#57717 `Oracle`Web pages - fixed displaying the list of pages if the user has the right to only selected web pages (#57725-4).
- Web pages - domain switcher added even if no configuration variable is set&#x20;
- &#x20;but only&#x20;
- &#x20;(#57833).`enableStaticFilesExternalDir`Applications - corrected display of the translation keys tab when using the component `multiDomainEnabled` (#54273-57).
- Applications - added support for inserting a new line via keyboard shortcut `editor_component_universal.jsp` into a simple text editor such as the one used in Questions and Answers (#57725-1).
- Dialers - moved dialer selection directly to the data table toolbar (#49144 `SHIFT+ENTER`New - moved section selection directly to the data table toolbar (#49144).
- Login - fixed login error when password expires (#54273-57).
- Login - fixed login in multiweb installation (#54273-57).
- GDPR - corrected display of the Database Cleanup tab on use&#x20;
- &#x20;Database (#54273-57).
- File archive - corrected display of icons in date and time dialog (#54273-57).`Oracle/PostgreSQL`Security - updated library&#x20;
- &#x20;to version&#x20;
- , supplemented by exceptions in `Swagger UI`.`5.20.0`Update - added deletion of unnecessary files when updating an unpacked version (#57657-4).`dependency-check-suppressions.xml`Multiweb - supplemented control&#x20;
- &#x20;domain when a visitor registers for a web site (#57657-4).
- Users - added the ability to also select the Root Folder in the User Rights in the Upload Files to Directories section (54273-60 `ID`Users - modified rights settings - simplified rights settings for administrators and registered users (no longer necessary to select the Users right as well), corrected duplicate entries, modified grouping in the Templates section (#57725-4).
- Explorer - added better reporting on ZIP archive creation error (#56058).
- Statistics - fixed creation of table for click statistics in the temperature map.
- Translator - implementation of intelligent delay for translator&#x20;
- &#x20;as protection against error&#x20;
- which caused a translation outage (#57833).`DeepL`Cloning structure - fixed unwanted interleaving of application implementations `HTTP 429: too many requests`, when automatically translating the page body (#57833).
- Cloning structure - added translation of perex annotation to automatic page translation (#57833 `!INCLUDE(...)!`Explorer - fixed folder and file property settings rights (#57833).
- Server monitoring - fixed report about configuration variable settings for Applications, WEB pages and SQL queries (#57833).
- Introduction - corrected display of the requirement for two-step verification when integrating via&#x20;
- &#x20;(#57833).
- Cloning/mirroring structure - fixed folder URL settings (removing diacritics and spaces) (#57657-7 `IIS`Gallery - missing tags added (#57837).
- Tags - corrected the folder settings of an existing tag in the View For section (#57837).
- Security
- Updated library&#x20;

### &#x20;to version 5.4.1 to fix vulnerabilities&#x20;

- .`Apache POI`2025.0`CVE-2025-31672`In the version&#x20;

## 2025.0

> &#x20;we brought **new administration design** for even better clarity and user experience.**One of the main changes is the transfer of&#x20;**&#x73;econd level menu
>
> &#x20;to **tabs in the page header**, which simplifies navigation. In the website we also **merged folder and website tabs** to keep everything in one place. If the header does not contain tabs, the tables are automatically adjusted and displayed **extra line**.**Please provide feedback via&#x20;**&#x46;eedback form
>
> if you identify when using the new version **any display problem**. You can also add a reminder **screenshot** to help us identify and resolve any deficiencies more quickly.**Thank you for your cooperation and help in improving WebJET CMS!**&#x47;roundbreaking changes
>
> Web pages - inline editing cancelled. The ability to edit the page directly in view mode has been removed as it used an older version of the editor that is no longer supported. As an alternative it is possible to activate&#x20;

### toolbar

- &#x20;displayed in the top right corner of the website. This panel provides quick access to the web page editor, folder or template. You can turn it off or on using the configuration variable [. Once activated, it will start to appear on the web page after entering the Web Pages section in the administration (#57629).](redactor/webpages/editor.md#nástrojový-panel) Login - set for administrators `disableWebJETToolbar` password change request

![](redactor/webpages/webjet-toolbar.png)

- &#x20;once a year. The value can be modified in the configuration variable [, setting it to 0 disables the check (#57629).](sysadmin/pentests/README.md#pravidlá-hesiel) Introduction - added requirement to activate two-factor authentication to increase login security. Prompt is not displayed if authentication is handled via `passwordAdminExpiryDays` or if the translation key is&#x20;
- &#x20;set to empty (#57629).`LDAP`Design`overview.2fa.warning`In the version&#x20;

### 2025.0

&#x20;we brought an improved **administration design** which is clearer and more efficient.**Modified login dialogue** - new background and moving the login dialog to the right side. At&#x20;

**Login** it is possible to use not only the login name but **already have an email address**. **Clearer header** - the name of the current page or section is now displayed directly in the header.![](redactor/admin/logon.png)

**New navigation in the left menu** - under items are no longer part of the left menu, but are displayed&#x20;

**as cards at the top** Pages. **Merged tabs in the Websites section** - Switching folder types and web page types are now displayed in a common section, simplifying navigation. ![](redactor/admin/welcome.png)

**Choosing a domain** has been moved to the bottom of the left menu. **Reorganised menu items**:![](redactor/webpages/domain-select.png)

**SEO** moved to section&#x20;

- **Views**.**GDPR and Scripts** moved to section&#x20;
- **Templates**.**Gallery** is now in the section&#x20;
- **Files**.**Some item names have been modified to better describe their function.**&#x57;eb pages
- Added the ability to set increment order for folders in a configuration variable&#x20;

### &#x20;and web pages in the configuration variable&#x20;

- . The default values are 10 (#57667-0).`sortPriorityIncrementGroup`Testing`sortPriorityIncrementDoc`Standard password for&#x20;

### &#x20;tests are obtained from&#x20;

- &#x20;variable `e2e` (#57629).`ENV`Error correction`CODECEPT_DEFAULT_PASSWORD`Web pages - inserting links to a file in PageBuilder (#57649).

### Web pages - added link information (file type, size) to the Auxiliary caption attribute&#x20;
- &#x20;(#57649).
- Web pages - corrected order of web pages when used `alt` in tree structure (#57657-1).
- Web pages - when duplicating a web page/folder, the value is set `Drag&Drop` in the Order of Arrangement field for inclusion at the end of the list. The value&#x20;
- &#x20;can also be entered manually to obtain a new value for the order of the arrangement (#57657-1 `-1`Websites - importing web pages - fixed media group settings when importing pages containing media. When importing, all Media Groups (even unused ones) are automatically created due to the fact that the media group set for the media application is also translated when importing pages `-1` in the page (which may also contain the media ID of a group outside the imported pages) (#57657-1).
- Firefox - reduced version of the set `/components/media/media.jsp` at&#x20;
- because Firefox puts a significant load on the processor when using newer versions. Optimised CSS style reading `Tabler Icons` (#56393-19).`3.0.1`The rest of the list of changes to the changes is identical to the version `vendor-inline.style.css` 2024.52

.[](CHANGELOG-2024.md)

![meme](_media/meme/2025-0.jpg ":no-zoom")
