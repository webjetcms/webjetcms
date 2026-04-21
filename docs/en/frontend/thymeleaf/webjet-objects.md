# WebJET CMS Attribute List

List of available attributes when viewing a page

## Website

Data of the displayed web page

- **Website ID** - `<div data-iwcm-write="doc_id"/>` - ​​Inserts the website docid, for example: `148`
- **Website Name** - `<div data-iwcm-write="doc_title"/>` - ​​Inserts the name of the website, for example: Web JET CMS
- **Navigation bar text** - `<div data-iwcm-write="doc_navbar"/>` - ​​Website text for the navigation bar (usually the same as the website title - `doc_title`)
- **Web page text** - `<div data-iwcm-write="doc_data"/>` - ​​The actual web page text as entered in the editor
- **Website header** - `<div data-iwcm-write="doc_header"/>` - ​​HTML text with the website header as defined in the virtual template that the website uses
- **Website footer** - `<div data-iwcm-write="doc_footer"/>` - ​​HTML text with the website footer as defined in the virtual template that the website uses
- **Website menu** - `<div data-iwcm-write="doc_menu"/>` - ​​HTML menu code that is entered in the admin section for individual virtual templates
- **Right menu of the website** - `<div data-iwcm-write="doc_right_menu"/>` - ​​HTML code of the right menu, which is entered in the admin section for individual virtual templates
- **HTML code from template** - `<div data-iwcm-write="after_body"/>` - ​​HTML text from the virtual template that the website uses
- **HTML Page Header** - `<div data-iwcm-write="html_head"/>` - ​​HTML code that is entered for the page in the HTML Properties tab as the HTML header code. It may contain code that is inserted into the template in the `<head>` section
- **HTML page code** - `<div data-iwcm-write="html_data"/>` - ​​HTML code that is entered for the page in the HTML Properties tab as Additional HTML code.
- **Link to the main cascading style** - `<div data-iwcm-write="base_css_link"/>` - ​​Link to the cascading style, which is entered in the admin section of virtual templates as the Main CSS style (e.g. `/css/page.css`).
- **Main CSS style link** - `<div data-iwcm-write="base_css_link_noext"/>` - ​​Cascading style link, which is entered in the admin section of virtual templates as the Main CSS style without an extension (e.g. `/css/page`). It can be used to create conditional CSS styles (e.g. `/css/page-dark.css`).
- **Cascading style link** - `<div data-iwcm-write="css_link"/>` - ​​Cascading style link, which is entered in the admin section to virtual templates as a CSS style (e.g. `/css/custom.css`).
- **Cascading style link** - `<div data-iwcm-write="css_link_noext"/>` - ​​Cascading style link, which is entered in the admin section to virtual templates as a CSS style without an extension (e.g. /css/custom). It can be used to create conditional CSS styles (e.g. `/css/page-dark.css`).
- **Navigation bar** - `<div data-iwcm-write="navbar"/>` - ​​Complete navigation bar, e.g. Interway > Products > Web JET. Titles are clickable, links are created using cascading style `navbar`.
- **Date of last website change** - `<div data-iwcm-write="doc_date_created"/>` - ​​The date the website was last changed.
- **Webpage last modified time** - `<div data-iwcm-write="doc_time_created"/>` - ​​The time the web page was last modified.
- **Custom Fields** - `<div data-iwcm-write="field_a"/>` to `<div data-iwcm-write="field_l"/>` - Custom page fields defined in Advanced Properties, Custom Fields tab.
- **Free template objects** - `<div data-iwcm-write="template_object_a"/>` to `<div data-iwcm-write="template_object_d"/>` - Freely usable template objects. They contain the HTML code of the assigned page. They are used when using a header, menu, and footer is not enough.

## Publishing a page

Data entered when editing a web page in the Advanced tab

- **Publishing start date** - `<div data-iwcm-write="doc_publish_start"/>` - ​​Date entered when editing the web page in the Advanced tab as Start.
- **Publishing start time** - `<div data-iwcm-write="doc_publish_start_time"/>` - ​​Time entered when editing the web page in the Advanced tab as Start.
- **Publishing End Date** - `<div data-iwcm-write="doc_publish_end"/>` - ​​Date entered when editing the web page in the Advanced tab as End.
- **Publishing End Time** - `<div data-iwcm-write="doc_publish_end_time"/>` - ​​Time entered when editing the web page in the Advanced tab as End.
- **Event date** - `<div data-iwcm-write="doc_event_date"/>` - ​​Event date entered when editing the website in the Advanced tab. Used, for example, for events where the event date is later than the required publication start date (if we want the information on the page to be displayed from the 1st of the month, but the event the page is reporting on is up to the 10th of the month).
- **Event time** - `<div data-iwcm-write="doc_event_time"/>` - ​​Event time entered when editing the website in the Advanced tab.
- **Web page annotation** - `<div data-iwcm-write="perex_data"/>` - ​​Inserts a page annotation into the HTML template.
- **Formatted web page annotation** - `<div data-iwcm-write="perex_pre"/>` - ​​Inserts a formatted page annotation into the HTML template. If the annotation is entered without HTML characters, the newline characters are converted to `<br>` tags
- **Location** - `<div data-iwcm-write="perex_place"/>` - ​​The location to which the page text refers.
- **Image** - `<div data-iwcm-write="perex_image"/>` - ​​An image that relates to the page annotation.

## Page author

Page author information (the user who last modified the page is considered the author of the page):

- **Website Author ID** - `<div data-iwcm-write="author_id"/>` - ​​ID of the website author (the user who last modified it).
- **Website Author Name** - `<div data-iwcm-write="author_name"/>` - ​​Name of the website author (the user who last modified it).
- **Website author email** - `<div data-iwcm-write="author_email"/>` - ​​Email of the website author (the user who last modified it).

## Folder

Information about the folder in which the page is located:

- **Directory ID** - `<div data-iwcm-write="group_id"/>` - ​​The ID of the directory in which the page is located
- **Directory name** - `<div data-iwcm-write="group_name"/>` - ​​The name of the directory where the page is located
- **Navigation Bar Text** - `<div data-iwcm-write="group_navbar"/>` - ​​Directory text for the navigation bar (usually the same as the directory name)
- **HTML code for the header** - `<div data-iwcm-write="group_htmlhead"/>` - ​​HTML code for the directory header
- **HTML code for the header - recursively obtained** - `<div data-iwcm-write="group_htmlhead_recursive"/>` - ​​HTML code for the directory header recursively obtained - if the content of this field is empty in the current directory, the value is obtained from its parent directory, if it is not entered there either, it proceeds through subdirectories to the main directory
- **Parent ID** - `<div data-iwcm-write="groupParentIds"/>` - ​​Comma-separated IDs of parent directories - based on this, it is possible to determine in the HTML template which language mutation or section we are currently displaying
- **Custom fields** - `<div data-iwcm-write="group_field_a"/>` to `<div data-iwcm-write="group_field_d"/>` - Custom directory fields.

## Page template

Page template data:

- **Template ID** - `<div data-iwcm-write="doc_temp_id"/>` - ​​The template ID that the website uses.
- **Template Name** - `<div data-iwcm-write="doc_temp_name"/>` - ​​The name of the template that the website uses.
- **Main CSS style** - `${ninja.temp.baseCssLink}` - ​​The name of the template that the website uses.
- **Secondary CSS style** - `${ninja.temp.cssLink}` - ​​The name of the template that the website uses.
- **Template object** - `${tempDetails}` - ​​The template object that the website uses.
- **Template Group Object** - `${templatesGroupDetails}` - ​​The template group object that the website uses.

## Ninja template

Examples of using objects from the [Ninja template](http://docs.webjetcms.sk/v8/#/ninja-starter-kit/) (examples are in [pug](../../developer/frameworks/pugjs.md) format). Typically, the Ninja object is used in the template header:

```javascript
meta(http-equiv='X-UA-Compatible' content='IE=edge')
meta(charset="utf-8" data-th-charset='${ninja.temp.charset}')
meta(name='viewport' content='width=device-width, initial-scale=1, shrink-to-fit=no')
meta(http-equiv="Content-type" content="text/html;charset=utf-8" data-th-content='|text/html;charset=${ninja.temp.charset}|')

title(data-th-text='|${docDetails.title} - ${ninja.temp.group.siteName}|') Page Title

meta(name='description' content="WebJET CMS" data-th-content='${ninja.page.seoDescription}')
meta(name='author', content="InterWay" data-th-content='${ninja.temp.group.author}')
meta(name='developer', content="InterWay" data-th-content='${ninja.temp.group.developer}')
meta(name='generator', content="WebJET CMS" data-th-content='${ninja.temp.group.generator}')
meta(name='copyright', content="InterWay" data-th-content='${ninja.temp.group.copyright}')
meta(name='robots', content="noindex" data-th-content='${ninja.page.robots}')

meta(property='og:title' content="WebJET CMS" data-th-content='${ninja.page.seoTitle}')
meta(property='og:description' content="WebJET CMS" data-th-content='${ninja.page.seoDescription}')
meta(property='og:url' content="http://webjetcms.sk" data-th-content='${ninja.page.url}')
meta(property='og:image' content="assets/images/og-webjet.png" data-th-content='|${ninja.page.urlDomain}${ninja.page.seoImage}|')
meta(property='og:site_name' content="WebJET CMS" data-th-content='${ninja.temp.group.siteName}')
meta(property='og:type' content='website')
meta(property='og:locale' content="sk-SK" data-th-content='${ninja.temp.lngIso}')

link(rel='icon' href="assets/images/favicon.ico" data-th-href='|${ninja.temp.basePathImg}favicon.ico|' sizes='any')
link(rel='icon' href="assets/images/icon.svg" data-th-href='|${ninja.temp.basePathImg}icon.svg|' type='image/svg+xml')

link(rel='canonical' data-th-href='${ninja.page.canonical}')
link(data-th-if="${docDetails.tempName == 'Blog template'}" rel='amphtml' data-th-href='|${ninja.page.url}?forceBrowserDetector=amp|')

touchicons(data-th-utext='${ninja.temp.insertTouchIconsHtml}' data-th-remove="tag")
    <link rel="apple-touch-icon-precomposed" href="assets/images/touch-icon.png" />

combine(
basePath='|${ninja.temp.basePath}dist/|',
data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}\n"+
"${ninja.webjet.pageFunctionsPath}"
)
    <link href="css/ninja.min.css" rel="stylesheet" type="text/css"/>
    <script src="js/jquery-3.6.0.min.js" type="text/javascript"></script>
    <script src="js/jquery.cookie.js" type="text/javascript"></script>
    <script src="js/bootstrap.bundle.min.js" type="text/javascript"></script>
    <script src="js/global-functions.js" type="text/javascript"></script>
    <script src="js/ninja.min.js" type="text/javascript"></script>

div(data-iwcm-write="group_htmlhead_recursive")
div(data-iwcm-write="html_head")
div(data-iwcm-script="header")
```

## Available objects

When displaying a page, WebJET inserts the following objects, which you can use directly for the report using ```${objekt.vlastnost}```. The name refers to the documentation, where you can see a list of object properties:

- `request` - ​​object `HttpServletRequest`
- `session` - ​​object `HttpSession`
- [ninja](../../../javadoc/sk/iway/iwcm/doc/ninja/Ninja.html) - additional attributes and functions for [Ninja template](../ninja-starter-kit/README.md)
- [docDetails](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - object of the displayed web page
- [docDetailsOriginal](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - if the login page is displayed, it contains the original (passworded) page
- [pageGroupDetails](../../../javadoc/sk/iway/iwcm/doc/GroupDetails.html) - directory object of the currently displayed web page
- [tempDetails](../../../javadoc/sk/iway/iwcm/doc/TemplateDetails.html) - template object of the currently displayed web page
- [templatesGroupDetails](../../../javadoc/sk/iway/iwcm/doc/TemplatesGroupBean.html) - template group object of the displayed web page

Usage examples:

```html
    <span data-th-text="${docDetails.title}">Titulok stránky</span>
    <body data-th-class="${docDetails.fieldA}">
    <meta name="author" data-th-content="${ninja.temp.group.author}" />
    <link rel="canonical" data-th-href="${ninja.page.canonical}" />
```
