# WebJET CMS 2026

Welcome to the documentation for WebJET CMS version 2026. We recommend reading the [changelog](CHANGELOG-2026.md) and [roadmap](ROADMAP.md).

## List of changes in version 2026.0

> **WebJET CMS 2026.0** brings an improved version of the **Page Builder** tool for creating **complex websites**. Blocks can be **searched and filtered** based on tags, so you can easily find the right block to insert into your page. New features have been added such as **column splitting**, **inserting multiple sections at once** and **always-on button to add a new section** for quickly expanding your page content.
>
> Support for the **PICTURE element** allows you to display **different images based on the visitor's screen resolution**, improving the visual experience on different devices. Additionally, it is possible to insert **custom icons** defined in a common SVG file, which brings greater design flexibility.
>
> The new **form creation** tool allows you to easily create **multi-step forms** with the option of programmatic validation of individual steps and the option of **confirming the validity of the email address** using the sent code. This way you avoid filling out forms with various bots.

### Groundbreaking changes

- Updated libraries `commons-lang,displaytag`, more in [programmer section](#programmer section) (#58153).
- Changed behavior of Blocks icon in Page Builder mode - [text blocks integrated](frontend/page-builder/blocks.md) to folder `content` similar to blocks for `section, container, column` (#58165).
- Modified **file upload** processing `multipart/form-data`, more in [programmer section](#programmer section) (#57793-3).
- We recommend **checking the functionality of all forms** due to adjustments to their processing, more information in the [for programmers](#for-programmers) section (#58161).

### Websites

- Added the ability to insert a `PICTURE` element that displays [image based on screen resolution](frontend/setup/ckeditor.md#picture-element) of the visitor. So you can display different images on a mobile phone, tablet or computer (#58141).

![](frontend/setup/picture-element.png)

- Added the ability to insert [custom icons](frontend/setup/ckeditor.md#svg-icons) defined in a shared SVG file (#58181).

![](frontend/setup/svgicon.png)

- Added transfer of current HTML code when switching editor mode Standard/HTML/Page Builder. This allows you to easily edit a Page Builder page in HTML code and view the edits again in Page Builder mode (#58145).
- Added context menu Delete Element, with which you can easily delete a button, link, paragraph, form, section, etc. Just right-click on the element to display the context menu (#osk233).
- Page Builder - modified generation of styles when using the pencil tool. Only changed values ​​are generated in the CSS style, they are highlighted in the dialog box by a blue border around the input field (#58145).
- Page Builder - added ability to call [custom JavaScript file](frontend/page-builder/blocks.md#support-javascript-code) with support functions for editing code. Added ability to edit settings like selectors for elements, colors, etc. (#58141).
- Page Builder - modified anchor generation for tabs so that the anchor name is generated according to the tab name - it was originally not generated semantically as `autotabs-x-y` (#112).
- Page Builder - added option to set column width to `auto` for automatic content adjustment (#114).
- Page Builder - added option to prepare [text blocks](frontend/page-builder/blocks.md) directly into the `content` folder, they are inserted instead of the original blocks read from web pages from the Templates folder. The web designer prepares them together with other types of Page Builder blocks. Allows for quick insertion of frequently used text parts, buttons, etc. (#58165).
- Page Builder - when inserting a new block, the default is the Library tab instead of Basic to make it easier to select a block from the prepared list (#58165).
- Page Builder - added the ability to split a column into two parts using the new Split Column function. You can access it by clicking on the + in the yellow bar, selecting the Block option, and then selecting the Split Column option in the Basic tab. The function allows you to quickly split a column without having to insert a new column and move the content (#58165).
- Page Builder - added option to insert a block that contains multiple sections or other elements - all sections will be selected after insertion (#58173).
- Page Builder - added [Block ID](frontend/page-builder/blocks.md#block-id) to the `data-pb-id` attribute to enable searching for block usage on websites via search in the administration (#58193).
- Page Builder - the list of favorite blocks is saved separately for each user, so everyone can manage their own list of favorite blocks (#58193).
- Page Builder - added a persistent icon for adding a new section at the bottom of the page, making it easier to add new sections to a page (#58173).

![](redactor/webpages/pagebuilder-plusbutton.png)

- Page Builder - modified toolbar design for better visibility on different backgrounds (#58165).

![](redactor/webpages/pagebuilder.png)

- Page Builder - added option to [filter blocks](frontend/page-builder/blocks.md#block-name-and-tags) by name and tags (#58173).

![](redactor/webpages/pagebuilder-library.png)

- Added [content change detection](redactor/webpages/working-in-editor/README.md#content-change-detection-page) and notification of unsaved changes when closing the browser window. Changes will start to be detected 5 seconds after opening the web page. (#112).
- Added the ability to set default values ​​for tables in CKEditor via configuration variables, more in [CKEditor settings section](frontend/setup/ckeditor.md#configuration-variables) (#58189).
- Added the option to insert [button](frontend/setup/ckeditor.md#button) - element `button`. You can easily insert various action `call to action` buttons (#58201).
- Style - [style selection](frontend/examples/template-bare/README.md#style-list-for-editor) defined for an element e.g. `p.paragraph-green,p.paragraph-red-border,p.paragraph-yellow-background` or `section.test-section,section.test-section-green` allows you to set multiple styles at the same time. Selecting an already set style again will remove this style (#OSK140).
- Changed the text for publishing a page in the future to **Schedule page change after this date**, when this option is selected, the save button will also change to the text **Schedule** for clearer information for the user (#58253).
- Added a list of changed fields to the website approval request (#58077).

![](redactor/webpages/approve/approve-form.png)

### Applications

Redesigned application properties settings in the editor from the old code in `JSP` to `Spring` applications. Applications automatically also gain the ability to set [display on devices](custom-apps/appstore/README.md#conditional-display-applications). The design is consistent with the rest of the WebJET CMS and data tables (#58073).

- [News] (redactor/apps/news/README.md)

![](redactor/apps/news/editor-dialog.png)

- [Form Simple](redactor/apps/formsimple/README.md)

![](redactor/apps/formsimple/editor-dialog-items.png)

### Forms

- A new way to create forms that can contain [multi-steps](redactor/apps/multistep-form/README.md) with advanced features. In the forms list, you can create a new form, to which you can then add individual items and possibly multiple steps. The form item tab is visible in the form details of the Multi-Step Form type (#58161).

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/XRnwipQ-mH4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

- List of forms - the entire section has been converted from `Vue.js` technology to standard `Html + JavaScript` for better integration into WebJET CMS and easier editing (#58161).
- List of forms - enabled creation of a form that is automatically of type [multistep form](redactor/apps/multistep-form/README.md) (#58161).
- List of forms - enabled setting of parameters/attributes of all form types directly in the form editor (#58161).
- List of forms - the note field allows you to insert formatted text, so you can better record additional information about the form (#58161).
- Form detail - added option to display all data of the logged-in user, data is also exported to Excel (#58161).
- Verification code - added option to submit form only after entering [verification code](redactor/apps/form/README.md#setting-up-confirmation-by-sent-code) sent to email address. This way you can better protect forms from SPAM (#58161).

![](redactor/apps/form/form-step-email-verification-2.png)

### Redirections

- Added the ability to expire a redirect at a specified time and the ability to enter a note with information about what the redirect is for. Redirects that are no longer valid will be displayed in red (#58105).

![](redactor/webpages/redirects/path-editor.png)

### E-commerce

- The new section [Delivery Methods] (redactor/apps/eshop/delivery-methods/README.md), as a separate table, replaces the original configuration of available delivery methods, which was located directly in the settings of the **e-commerce** application. For each delivery method, it is also possible to set a price, which will be automatically added to the order when the option is selected. The set delivery methods will also be automatically reflected in the options when creating an order by the customer. Delivery by mail and personal pickup are ready, in the future we plan to add integration with delivery companies (#58061).

![](redactor/apps/eshop/delivery-methods/datatable.png)

### Safety

- Added support for allowing only **one active login** per user. To enable the mode, set the configuration variable `sessionSingleLogon` to the value `true`. Upon a new login, the previous active `session` will be canceled (#58121).
- Removed unsupported library [commons-lang](https://mvnrepository.com/artifact/commons-lang/commons-lang), replaced with new library [commons-lang3](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3), in `update-2023-18.jsp` there is an update script for editing source codes (#58153).
- Added [My active logins] list (redactor/admin/welcome.md#moje-aktivne-prihlaženia) on the administration home screen, which displays all active logins to the administration under your user account and the option to end them. Also added the option to send an email to the logged in administrator (#58125).

![](editor/admin/sessions.png)

- Captcha - by setting the configuration variable `captchaType` to the value `none`, Captcha can be completely disabled. It will not be displayed even if the template of the displayed web page has SPAM protection disabled. In such a case, however, it is necessary to correctly check the disabling of SPAM protection of the template also in the possible code for processing/verifying the Captcha response, this check is provided for forms. You can use the call `Captcha.isRequired(component, request)` to verify the mode and disable spam protection (#54273-78).
- Updated the [sending emails] library (install/config/README.md#sending-emails) from `com.sun.mail:javax.mail:1.6.2` to `com.sun.mail:jakarta.mail:1.6.8` to support new SMTP server authentication mechanisms such as `NTLMv2` and added a configuration variable `smtpAuthMechanism` to force the use of an authorization mechanism - set it to `NTLM` for example to force `NTLM` authorization instead of `BASIC` authorization (#58153).
- Modified logging of exceptions when HTTP connection is interrupted (e.g. when closing the browser, going to another web page, etc.). Such exceptions are not written to the log to avoid a space allocation error. This applies to exceptions of type `IOExceptio` and exception names defined via the configuration variable `clientAbortMessages`, by default `response already,connection reset by peer,broken pipe,socket write error` (#58153).

### Other minor changes

- Search - modified loading of the template list when searching for web pages. All templates are loaded regardless of their availability in folders, so that a template is not available when editing a web page (#58073).
- HTTP headers - added option to set header longer than 255 characters, for example for setting `Content-Security-Policy` ([#82](https://github.com/webjetcms/webjetcms/issues/82))

![](admin/settings/response-header/editor.png)

- Configuration - modified way to delete a configuration variable. After deletion, the original value from `Constants` is automatically set to the same value as it will be after a server restart. In the original solution, the variable was only deleted, but its value remained internally set until the server restart (#57849).
- Configuration - added option to set [HTTP header name](sysadmin/pentests/README.md#configuration) to get visitor's IP address via configuration variable `xForwardedForHeader` (#58237).
- Security - added the ability to configure blocked file/directory paths via the `pathFilterBlockedPaths` variable. By default, URLs that contain the term `.DS_Store,debug.,config.properties,Thumbs.db,.git,.svn` in the name are blocked. It is possible to add more as needed (#PR103).
- Tags - modified displayed tags, in case of duplicate values. Comparison is without the influence of diacritics and upper/lower case [#115](https://github.com/webjetcms/webjetcms/issues/115).

![](redactor/webpages/perex-duplicity-values.png)

- Mirroring - added option to display flag image instead of text in [page language switcher](redactor/apps/docmirroring/README.md#creating-a-link-to-language-mutations-in-the-page-header) (#54273-79).
- Password change - added option to set the name and email address from which the email with a link to change the password is sent via configuration variables `passwordResetDefaultSenderEmail` and `passwordResetDefaultSenderName` (#58125).
- Statistics - added summary numbers of views and visits to TOP pages (#PR136).
- News - renamed value sort by priority to sort by Sort order (priority) to align with value in editor (#57667-16).
- Easy form - added option to set the value `useFormDocId` for inserting the form, e.g. in the footer of the page (#57667-16).
- News / News Templates - moved field `contextClasses` from News application to News Template so that the property is set directly in the news template. The original value `contextClasses` from News will still work, but it can no longer be set in the user interface (#58245).
- Document Manager - added option [edit historical document version metadata](redactor/files/file-archive/README.md#edit-historical-document-version-in-manager) in the document manager (#58241).
- Bulk email - adjusted auditing of changes in the campaign. If a group is added, the entire list of recipients is not audited (there were unnecessarily many records in the audit), only the list of changed groups is written. When manually adding emails, both the name and email address are still audited (#58249).
- Users - when importing, if the column in Excel does not contain a password field, a random password will be generated for new users. If the Approved User status is not specified in Excel, it will be set to the value `true` (#58253).
- MultiWeb - added domain display in the sidebar (#58317-0).
- MultiWeb - added option to set redirection domain to allow entering `https://` prefix (#58317-0).
- MultiWeb - added rights control for media groups and brands (#58317-0).
- List of forms - setting up [form handler](custom-apps/apps/multistep-forms/README.md), using the autocomplete field provided by classes implementing `FormProcessorInterface` (#58313).
- Codebooks - added removal of spaces at the beginning and end of string fields in codebook data (#OSK233).

### Bug fixes

- Tags - fixed folder duplication in Show for when saving a tag, removed folder selection from other domains as tags are already separated by domain (#58121).
- Web pages - fixed inserting a hard space after hyphens so that it only applies to the page text and not to attributes or HTML tags (#OSK235).
- Datatables - fixed handling of the `Enter` event for selected input fields of table filters (#58313).
- Datatables - fixed filtering where multiple `serverSide:false` tables on a page were affecting each other when filtering (#58313).
- E-commerce - fixed sending email notifications when changing order status (#58313).
- E-commerce - fixed automatic setting of order status after changing payments (#58313).

### Documentation

- Updated all screenshots in the Czech version of the documentation (#58113).

### For the programmer

- Free fields - added the ability to specify custom columns for label and value when [linking to a codebook](frontend/webpages/customfields/README.md#codebook). Allows for more flexible settings of which property from the codebook will be used as the displayed text and which as the stored value (#PR108).
- Deleted unused files `/admin/spec/gallery_editor_perex_group.jsp,/admin/spec/perex_group.jsp`, if you use them in your project, take them from [older version](https://github.com/webjetcms/webjetcms/tree/release/2025.40/src/main/webapp/admin/spec) WebJET CMS (#58073).
- Slightly modified API in [NewsActionBean](../../src/main/java/sk/iway/iwcm/components/news/NewsActionBean.java), mainly `groupIds` settings which are now of type `List<GroupDetails>`. You can use `setGroupIds(int[] groupIds)` for settings with an ID field of values ​​(#58073).
- Fixed the ability to insert quotes into application parameters (#58117).
- Ready-made containers for all supported database servers in WebJET CMS for easy launch in VS Code. They are located in the `.devcontainer/db` folder (#58137).
- E-commerce - due to changes in the implementation process of **delivery methods**, it is necessary to edit the file using the update script `update-2023-18.jsp` above the section `basket` (#58061).
- E-commerce - renamed annotation `@PaymentMethod` to `@FieldsConfig` and `@PaymentFieldMapAttr` to `@FieldMapAttr` to unify annotations between payments and delivery methods (#58061).
- E-commerce - During the **delivery methods** implementation process, several changes were added to the `order_form.jsp` file that you must implement manually. These changes are too complex to be implemented using the `update-2023-18.jsp` update script (#58061).
- Navigation bar - added the ability to use your own implementation of the [navigation bar] generator (redactor/apps/navbar/README.md). The name of the class implementing `navbarDefaultType` can be set via the configuration variable `NavbarInterface` (#PR101).
- Removed unsupported library [commons-lang](https://mvnrepository.com/artifact/commons-lang/commons-lang), replaced with new library [commons-lang3](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3), in `update-2023-18.jsp` there is an update script for editing source codes (#58153).
- Updated the [displaytag](https://mvnrepository.com/artifact/com.github.hazendaz/displaytag) library to version `2.9.0` (#58153).
- Fixed handling of `multipart/form-data` file uploads. In Spring applications, instead of `org.apache.commons.fileupload.FileItem`, use `org.springframework.web.multipart.MultipartFile` directly for the file field, which will be set automatically. It is no longer necessary to use a call like `entity.setDocument(MultipartWrapper.getFileStoredInRequest("document", request))` to get the file. **Warning:** you need to replace all occurrences of `CommonsMultipartFile` with `MultipartFile` in your code, and also remove URL parameters in the Spring application for forced processing. Replace the expression `data-th-action="@{${request.getAttribute('ninja').page.urlPath}(\_\_forceParse=1,\_\_setf=1)}"` with `data-th-action="${request.getAttribute('ninja').page.urlPath}"`. You can use `/admin/update/update-2023-18.jsp` to update files (#57793-3).
- Added the ability to create [project copies of files](frontend/customize-apps/README.md) for Spring applications. Just create your own version of the file in the `/apps/INSTALL_NAME/` folder, similar to what is used for JSP files. WebJET CMS first looks for the file in the project folder and if it is not found, it uses the standard file from the `/apps/` folder (#58073).
- Added the option to set [CSS style name](frontend/examples/template-bare/README.md) in the CSS file via the `/* editor title: Style Name */` comment. The name will be displayed in the style list in the editor (#58209).
- Editor - modified dialog for `a.btn` settings - canceled color and size settings, [only CSS classes are used](frontend/setup/ckeditor.md#button) same as for `button` (#57657-16).
- Data tables - option to display only the icon without a sequence for `rowReorder` if we add the class `icon-only` to the given column (#58161).
- Data tables - new options for selecting rows in the `toggleSelector` and `toggleStyle` tables, more in the [data tables section](developer/datatables/README.md#configuration-options) (#58161).
- Datatables - new custom [render](developer/datatables-editor/datatable-columns.md) function option using the `@DataTableColumn(...renderFunction = "renderStepName")` annotation. This will allow you to display composite values ​​from multiple fields in a column, etc. (#58161).
- Data tables - added option to [redirect user](developer/datatables/restcontroller.md#redirect-after-save) to another page after saving a record by calling the `setRedirect(String redirect)` method (#58161).
- Forms - Modified form list display, class `FormAttributesDB` removed, replaced with class `FormService`. Form settings changed from table `form_attributes` to table `form_settings`. We recommend verifying the functionality of all forms on the website after the update (#58161).
- Forms - creating a new table `form_settings` to replace the table `form_attributes`, where form properties are stored. Individual attributes (settings) are now stored in separate columns as one record per row. The data was converted to the new table using `UpdateDatabase.java` (#58161).
- Switch to a new table `form_settings` for form properties in `.jsp` files. You need to run the update script `update-2025-0.jsp`, which will edit the necessary `.jsp` (#58161).
- List of forms - setting parameters/attributes of all form types redirected from table `form_attributes` to new table `form_settings` (#58161).
- Data tables - added BE support for `row-reorder`, where it is possible to change the order of records directly in the data table using drag&drop (#58161).
- Events - added event [Update codes in text](developer/backend/events.md#update-codes-in-text) for the ability to edit codes in the text of the page like `!CUSTOM_CODE!` and the like (#54273-63).
- Data tables - added [Spring events](developer/backend/events-datatable.md) for data editing in customer installations (#54273-63).

### Testing

- Added script [rm-same-images.sh](../../src/test/webapp/rm-same-images.sh) to remove identical images when creating new screenshots (#58113).

![meme](_media/meme/2026-0.jpg ":no-zoom")