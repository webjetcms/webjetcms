# WebJET CMS 2025

Welcome to the documentation for WebJET CMS version 2025. We recommend to read [list of changes](CHANGELOG-2025.md) a [roadmap](ROADMAP.md).

## List of changes in version 2025.18

> View Full Version **2025.18** brings a completely redesigned module **E-commerce** with support **payment gateway GoPay** and an improved order list. Application **News calendar** has been separated as **standalone application** and at the same time we have redesigned the settings of several applications in the page editor. **Document Manager** (formerly File Archive) has passed **visual and functional reboot** including new tools for managing, exporting and importing documents.
>
> The system has also been improved **Bulk email** with new options for the sender and a more convenient choice of recipients. **Reservations** have gained new opportunities as **overbooking** creating bookings back in time and sending notifications to specific emails for each booking object.
>
> We have optimized the number of files in **Explore** leading to **faster loading** and added new information to **Server monitoring**.

### Groundbreaking changes

- News Calendar app separated into a separate app, if you use News Calendar you need to edit the path `/components/calendar/news_calendar.jsp` at `/components/news-calendar/news_calendar.jsp` (#57409).
- Modified Spring and JPA initialization, see the programmer's section (#43144) for more information.
- Redesigned backend part of ecommerce application, more in the section for programmer (#57685).

### Data tables

- When the numeric value filter is set to from-to, the field is enlarged to better display the entered value, similar to what the date field does (#57685).
- The File Archive application has been converted into a Spring application. For more information, see the programmer's section (#57317).
- The E-Commerce application was on `BE` partly remodeled. For more information see the section for the programmer (#56609).

### Document Manager (File Archive)

- **List of files** redesigned with the addition of new logic compared to the old version. Read more in [Archive files](redactor/files/file-archive/README.md) (#57317).

![](redactor/files/file-archive/datatable_allFiles.png)

- **Category Manager** repaired and redesigned. Read more in [Category Manager](redactor/files/file-archive/category-manager.md) (#57317).
- **Product Manager** has been added as a new section. Read more in [Product Manager](redactor/files/file-archive/product-manager.md) (#57317).
- **Exporting master files** has been modified to offer wider file export options and improve the clarity of the listings. Read more in [Exporting master files](redactor/files/file-archive/export-files.md) (#57317).

![](redactor/files/file-archive/export_all.png)

- **Importing master files** has been corrected and modified to work with the extended export options. Read more in [Importing master files](redactor/files/file-archive/import-files.md) (#57317).
- **Indexing** documents in search engines like `Google` modified to not index old/historical versions of documents and documents out of date (HTTP header set `X-Robots-Tag=noindex, nofollow`). Indexing of these documents can be enabled in the editor in the document manager (#57805).

### Applications

Redesigned application properties settings in the editor from the old code in `JSP` at `Spring` Application. Apps also automatically get the ability to set [display on devices](custom-apps/appstore/README.md#conditional-application-view). The design is consistent with the rest of the WebJET CMS and data tables (#57409).
- [Survey](redactor/apps/inquiry/README.md)
- [Banner system](redactor/apps/banner/README.md)
- [Date and time, Date and name day](redactor/apps/app-date/README.md) - merged into one common application
- [Questionnaires](redactor/apps/quiz/README.md)
- [Bulk e-mail](redactor/apps/dmail/form/README.md)
- [Calendar of events](redactor/apps/calendar/README.md)
- [News calendar](redactor/apps/news-calendar/README.md)
- [Site Map](redactor/apps/sitemap/README.md)
- [Media](redactor/webpages/media.md)
- [Related sites](redactor/apps/related-pages/README.md)
- [Rating](redactor/apps/rating/README.md)
- [Reservations](redactor/apps/reservation/reservation-app/README.md)

![](redactor/apps/dmail/form/editor.png)

- Accelerated loading of application data in the editor - data is loaded directly from the server, no need to make a REST service call (#57673).
- Modified visual - application title moved to main window when inserted into the page (instead of the original Application title) to increase the size of the application setup area (#57673).

![](redactor/apps/menu/editor-dialog.png)

- Added application screenshots in Czech language for most applications (#57785).

### Bulk e-mail
- **Moved Web page field** - now located in front of the field **Subject** so that when you select a page, the subject is automatically filled in according to the name of the selected web page (#57541).
- **Modifying the order in the Groups tab** - email groups are now shown before user groups (#57541).
- **New options for sender name and email** - if the configuration variables are `dmailDefaultSenderName` a `dmailDefaultSenderEmail` set, the following values are used. If they are blank, the system will automatically fill in the name and email of the currently logged in user. (#57541)
  - With these variables it is possible to set **fixed values** (e.g. company name) for all [Campaigns](redactor/apps/dmail/campaings/README.md), regardless of who is logged in.

![](redactor/apps/dmail/campaings/editor.png)

- Bulk email - optimizing recipient list creation - tab [groups](redactor/apps/dmail/campaings/README.md#adding-from-a-group) moved to the dialog box. After selecting a group of recipients, you can immediately see them in the Recipients tab and can easily edit them, no need to save the email first to view the recipients (#57537).

![](redactor/apps/dmail/campaings/users.png)

- Unsubscribe - when you directly enter your email to unsubscribe (not by clicking on the link in the email), a confirmation email is sent to the email address you entered. In it you need to click on the unsubscribe link. The original version did not check the validity/ownership of the email address in any way, and it was possible to unsubscribe from someone else's email (#57665).

### News calendar

- News Calendar separated as a separate app, originally it was an option in the Calendar app (#57409).
- Displays a calendar linked to the news list with the option to filter news by the selected date in the calendar.

![](redactor/apps/news-calendar/news-calendar.png)

### Server monitoring

- Added table with information about database connections and memory occupied (#54273-61).
- Added information about the version of libraries `Spring (Core, Data, Security)` in the Server Monitoring-Actual Values section (#57793).

### Reservations

- **Support for overbooking** - allows administrators to create multiple reservations `overbooking` on the same date (#57405).
- **Improved import validation** - it is now possible to import [booking](redactor/apps/reservation/reservations/README.md) well into the past, or to create `overbooking` data import reservations (#57405).
- **Support for adding bookings to the past** - allows administrators to create reservations in the past (#57389).
- To [reservation objects](redactor/apps/reservation/reservation-objects/README.md) a column has been added **Emails for notifications** which for each valid email entered (separated by a comma) will send an email if the reservation has been added and approved (#57389).
- Booking confirmation notifications and other system notifications can be set to the sender's name and email using configuration variables `reservationDefaultSenderName,reservationDefaultSenderEmail` (#57389).
- New application added [Reservation of days](redactor/apps/reservation/day-book-app/README.md), for booking all-day objects for a specific interval using the integrated calendar (#57389).

![](redactor/apps/reservation/day-book-app/app-table_B.png)

### Ecommerce

!> **Warning:** due to the database update, the first start of the server may take longer - values for the number of items and price are calculated in the database for faster loading of the order list.
- Added card **Personal information** to the order list - contains detailed information about **delivery address** as well as **contact information** all in one place (#57685).
- Added card **Optional fields** to the order list - [optional fields](frontend/webpages/customfields/README.md) as needed for implementation (#57685).
- Export of order list - columns total price with VAT and number of items (#57685) added.
- Order form - added option to define available list of countries via configuration variable `basketInvoiceSupportedCountries` (#57685).
- Modified card data display **Personal data** in the list of orders, their logical division into parts for better overview (#57685).
- Columns have been added to the order list **Number of items**, **Price without VAT** a **Price with VAT**. Values are automatically recalculated when order items are changed (#57685).
- Added the ability to view the product web page by clicking on the icon in the item list, the product will also be displayed in the Preview tab when opening the item editor (#57685).
- In the order list, redesigned country selection via the selection field, which offers only countries defined by a constant `basketInvoiceSupportedCountries` (#57685).

![](redactor/apps/eshop/invoice/editor_personal-info.png)

- New version [configurations of payment methods](redactor/apps/eshop/payment-methods/README.md) and integration to payment gateways. Data is separated by domain. We have added support for [payment gateway GoPay](https://www.gopay.com), which also means accepting payment cards, supporting `Apple/Google Pay`, payments via internet banking, `PayPal`, `Premium SMS` etc. In addition, payments by bank transfer and cash on delivery are supported. For each type of payment it is also possible to set a price, which will be automatically added to the order when the option is selected. The set payment methods are also automatically reflected in the options when the customer creates an order.

![](redactor/apps/eshop/payment-methods/datatable.png)

- New Order List application with a list of orders of the currently logged in user. By clicking on an order, you can view the order detail and download it in PDF format (#56609).

### Other minor changes

- Administration search - customized interface `RestController` a `Service` (#57561).
- Explorer - faster loading and lower server load by reducing the number of files/server requests (#56953).

### Error correction

- Bulk email - added duplication of recipient list when duplicating a campaign (#57533).
- Data tables - import - modified logic **Skip erroneous entries** when importing so that generic errors are also handled with this option `Runtime` and ensured that the import was completed without interruption. These errors are then displayed to the user via a notification during import (#57405).
- Files - fixed file/folder size calculation in explorer footer and when showing folder detail (#57669).
- Navigation - fixed tab navigation in mobile view (#57673).
- Autocomplete - corrected error in type field `Autocomplete`, where the first value obtained in the case of `jstree` was not correct (#57317).

### For the programmer

!> **Warning:** modified Spring and JPA initialization, follow [instructions in the installation section](install/versions.md#changes-in-the-transition-to-the-20250-snapshot).

Other changes:
- Added option to perform [additional HTML/JavaScript code](custom-apps/appstore/README.md#additional-html-code) in Spring application with annotation `@WebjetAppStore` by setting the attribute `customHtml = "/apps/calendar/admin/editor-component.html"` (#57409).
- Added field type in datatable editor [IMAGE\_RADIO](developer/datatables-editor/standard-fields.md#image_radio) to select one of the options using the image (#57409).
- Added field type `UPLOAD` For [file upload](developer/datatables-editor/field-file-upload.md) in the datatable editor (#57317).
- When initializing [nested datatables](developer/datatables-editor/field-datatable.md) added option to edit `columns` object by specifying a JavaScript function in the attribute `data-dt-field-dt-columns-customize` Annotation (#57317).
- Added support for getting sender name and email for various email notifications using `SendMail.getDefaultSenderName(String module, String fallbackName), getDefaultSenderEmail(String module, String fallbackEmail)` (#57389).
- Added option to set root folder for [field of type JSON](developer/datatables-editor/field-json.md) in both ID and path format: `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")` or `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "26")`.
- Running background tasks is only done after complete initialization, including `Spring` (#43144).
- Added option to set [all HikariCP properties](install/setup/README.md#creating-a-db-schema) (#54273-61).
- Added check to see if the database driver supports sequence setup (#54273-61).
- Modified function `WJ.headerTabs` if you are listening to change the card we recommend to use event type `$('#pills-tabsFilter a[data-wj-toggle="tab"]').on('click', function (e) {` where in `e` you get the card that was clicked (#56845-20250325).
- Converted Document Manager (File Archive) to Spring application. If you are using the original version and want to keep it, you need to add back the files `/components/file_archiv/file_archiv.jsp` a `components/file_archiv/editor_component.jsp` and the necessary classes from [older version of WebJET CMS](https://github.com/webjetcms/webjetcms/tree/release/2025.0/src/webjet8/java/sk/iway/iwcm/components/file_archiv).
- Document Manager (File Archive) - modified API `FileArchivatorBean.getId()/getReferenceId()/saveAndReturnId()` Returns `Long`, you can use `getFileArchiveId()` for including `int` Values. Delete unused methods, transfer them to your classes if needed. We do not recommend modifying WebJET classes, create new classes of type `FileArchivatorProjectDB` in your project where you add methods. If we have deleted the whole class you are using (e.g. `FileArchivatorAction`), you can add it directly to your project (#57317).
- Added automatic setting of column filtering to value `false`, if the value is `null` (unset) and it is a column that is nested, such as `editorFields` Columns (#57685).
- Added option [of special arrangement](developer/datatables/restcontroller.md#Arrangement) by overwriting the method `DatatableRestControllerV2.addSpecSort(Map<String, String> params, Pageable pageable)` (#57685).
- Added option in annotation `@DataTableColumn` set attribute `orderProperty` which will determine [columns for arrangement](developer/datatables/restcontroller.md#Arrangement), e.g. `orderProperty = "contactLastName,deliverySurName"`. Convenient for `EditorFields` classes that can aggregate data from multiple columns (#57685).
- For an array type `dt-tree-dir-simple` with set `data-dt-field-root` added tree structure of parent folders for better [tree structure display](developer/datatables-editor/field-json.md) (before, folders were displayed only from the specified root folder). Added the ability to define a list of folders that will not appear in the tree structure using a configuration variable set to `data-dt-field-skipFolders`.
- Selection [editable field](developer/datatables-editor/field-select-editable.md) modified so that when a new record is added, that record is automatically selected in the field (#57757).
- Redesigned e-commerce application on `BE` parts. Since new classes are already being used, you must:
  - make use of the update script `/admin/update/update-2023-18.jsp` for basic updating of your JSP files
  - as the type is now used `BigDecimnal` instead of `float`, you must additionally adjust all comparisons of these values. Type `BigDecimal` is not compared classically using `<, =, >` but by means of `BigDecimal.compareTo( BigDecimal )`
  - you need to remove file calls or add back any files that were removed because they were not used

### Testing

- Media - added test for embedding media in a web page if the user does not have the right to all media (#57625).
- Web pages - added test for creating a new page with publishing in the future (#57625).
- Gallery - added watermark test with image comparison, added rights check test (#57625).
- Web pages - added test for optional fields when creating a web page (#57625).
- Allure - jUnit test results added to the common Allure report (#57801).

![meme](_media/meme/2025-18.jpg ":no-zoom")
