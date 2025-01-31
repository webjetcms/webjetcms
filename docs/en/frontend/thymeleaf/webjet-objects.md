# List of WebJET CMS attributes

List of available attributes when displaying a page

## Web page

Displayed web page data

- **Website ID** - `<div data-iwcm-write="doc_id"/>` - Inserts the docid of a web page, for example: `148`
- **Name of the website** - `<div data-iwcm-write="doc_title"/>` - Inserts the name of the web page, for example, Web JET CMS
- **Navigation bar text** - `<div data-iwcm-write="doc_navbar"/>` - Web page text for the navigation bar (usually the same as the title of the web page - `doc_title`)
- **Web page text** - `<div data-iwcm-write="doc_data"/>` - The actual text of the web page as entered in the editor
- **Header of the web page** - `<div data-iwcm-write="doc_header"/>` - HTML text with the web page header as defined in the virtual template used by the web page
- **Footer web page** - `<div data-iwcm-write="doc_footer"/>` - HTML text with the footer of the web page as defined in the virtual template used by the web page
- **Website menu** - `<div data-iwcm-write="doc_menu"/>` - HTML code of the menu, which is entered in the admin section of each virtual template
- **Right menu of the web page** - `<div data-iwcm-write="doc_right_menu"/>` - HTML code of the right menu, which is entered in the admin section of each virtual template
- **HTML code from template** - `<div data-iwcm-write="after_body"/>` - HTML text from the virtual template used by the web page
- **HTML page header** - `<div data-iwcm-write="html_head"/>` - The HTML code that is entered for the page in the HTML Properties tab as the HTML header code. It can contain code that is inserted into the template in the `<head>`
- **HTML code of the page** - `<div data-iwcm-write="html_data"/>` - HTML code that is entered for the page in the HTML Properties tab as Additional HTML code.
- **Line to the main cascade style** - `<div data-iwcm-write="base_css_link"/>` - A link to a cascading style that is specified in the admin section of virtual templates as a Main CSS style (e.g. `/css/page.css`).
- **Line to the main cascade style** - `<div data-iwcm-write="base_css_link_noext"/>` - A link to a cascading style that is specified in the admin section of virtual templates as a Main CSS style without suffix (e.g. `/css/page`). It can be used to create conditional css styles (e.g. `/css/page-dark.css`).
- **Cascade style line** - `<div data-iwcm-write="css_link"/>` - A link to a cascading style that is specified in the admin section of virtual templates as a CSS style (e.g. `/css/custom.css`).
- **Cascade style line** - `<div data-iwcm-write="css_link_noext"/>` - A link to a cascading style that is specified in the admin section of virtual templates as a CSS style without a suffix (e.g. /css/custom). It can be used to create conditional css styles (e.g. `/css/page-dark.css`).
- **Navigation bar** - `<div data-iwcm-write="navbar"/>` - Complete navigation bar, e.g. Interway > Products > Web JET. Titles are clickable, links are created using cascading style `navbar`.
- **Date of last change to the website** - `<div data-iwcm-write="doc_date_created"/>` - The date when the web page was last changed.
- **Time of last web page change** - `<div data-iwcm-write="doc_time_created"/>` - The time when the web page was last changed.
- **Custom fields** - `<div data-iwcm-write="field_a"/>` to `<div data-iwcm-write="field_l"/>` - Custom page fields defined in Advanced Properties, Custom Fields tab.
- **Free template objects** - `<div data-iwcm-write="template_object_a"/>` to `<div data-iwcm-write="template_object_d"/>` - Freely usable template objects. They contain the HTML code of the associated page. They are used when the use of header, menu and footer is not enough.

## Publishing the page

Data entered when editing a web page in the Advanced tab

- **Date of start of publication** - `<div data-iwcm-write="doc_publish_start"/>` - The date entered when editing the web page in the Advanced tab as Start.
- **Start time of publication** - `<div data-iwcm-write="doc_publish_start_time"/>` - The time entered when editing a web page in the Advanced tab as Start.
- **Date of end of publication** - `<div data-iwcm-write="doc_publish_end"/>` - The date entered when editing the web page in the Advanced tab as End.
- **Time of end of publication** - `<div data-iwcm-write="doc_publish_end_time"/>` - The time specified when editing a web page in the Advanced tab as End.
- **Date of the event** - `<div data-iwcm-write="doc_event_date"/>` - The date of the event entered when editing the web page in the Advanced tab. It is used, for example, for events where the event date is later than the desired publishing start date (if we want the information to be displayed on the page from the 1st of the month, but the event the page informs about is only on the 10th of the month).
- **Event time** - `<div data-iwcm-write="doc_event_time"/>` - The time of the event specified when editing the web page in the Advanced tab.
- **Perex (annotation) website** - `<div data-iwcm-write="perex_data"/>` - Inserts the page annotation into the HTML template.
- **Formatted perex (annotation) of the web page** - `<div data-iwcm-write="perex_pre"/>` - Inserts a formatted page annotation into the HTML template. If the annotation is specified without HTML characters, the characters of the newline are converted to tags `<br>`
- **Place** - `<div data-iwcm-write="perex_place"/>` - The place to which the page text refers.
- **Image** - `<div data-iwcm-write="perex_image"/>` - Image related to the annotation of the page.

## Author of the page

Page author details (the user who last changed the page is considered the page author):
- **ID of the author of the web page** - `<div data-iwcm-write="author_id"/>` - The ID of the author of the web page (the user who last changed it).
- **Name of the author of the web page** - `<div data-iwcm-write="author_name"/>` - The name of the author of the web page (the user who last changed it).
- **Email of the author of the website** - `<div data-iwcm-write="author_email"/>` - Email of the author of the web page (the user who last changed it).

## Folder

Details of the folder in which the page is located:
- **Directory ID** - `<div data-iwcm-write="group_id"/>` - The ID of the directory in which the page is located
- **Directory name** - `<div data-iwcm-write="group_name"/>` - The name of the directory in which the page is located
- **Navigation bar text** - `<div data-iwcm-write="group_navbar"/>` - Directory text for the navigation bar (usually the same as the directory name)
- **HTML header code** - `<div data-iwcm-write="group_htmlhead"/>` - HTML code for the directory header
- **HTML header code - recursively retrieved** - `<div data-iwcm-write="group_htmlhead_recursive"/>` - HTML code to the directory header recursively retrieved - if the content of this field is empty for the current directory, the value is retrieved from its parent directory, if it is not specified there either, it is passed on through subdirectories to the main directory
- **Parent ID** - `<div data-iwcm-write="groupParentIds"/>` - Comma separated ID of parent directories - according to this it is possible to determine in HTML template which language or section is currently displayed
- **Custom fields** - `<div data-iwcm-write="group_field_a"/>` to `<div data-iwcm-write="group_field_d"/>` - Custom directory fields.

## Page template

Page template details:
- **Template ID** - `<div data-iwcm-write="doc_temp_id"/>` - The ID of the template that the web page uses.
- **Template name** - `<div data-iwcm-write="doc_temp_name"/>` - The name of the template that the web page uses.
- **Main CSS style** - `${ninja.temp.baseCssLink}` - The name of the template that the web page uses.
- **Secondary CSS style** - `${ninja.temp.cssLink}` - The name of the template that the web page uses.
- **Template object** - `${tempDetails}` - The template object that the web page uses.
- **Template group object** - `${templatesGroupDetails}` - The template group object that the web page uses.

## Ninja template

Examples of using objects from [Ninja templates](http://docs.webjetcms.sk/v8/#/ninja-starter-kit/) (examples are in [pug](../../developer/frameworks/pugjs.md) format). Typically, the Ninja object is used in the template header:

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

link(rel='canonical' data-th-href='${ninja.page.url}')
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

When displaying the page WebJET inserts the following objects, you can use these directly for listing using `${objekt.vlastnost}`. The name refers to the documentation where the list of properties of the object can be seen:
- `request` - object `HttpServletRequest`
- `session` - object `HttpSession`
- [ninja](../../../javadoc/sk/iway/iwcm/doc/ninja/Ninja.html) - additional attributes and features for [Ninja template](../ninja-starter-kit/README.md)
- [docDetails](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - the object of the displayed web page
- [docDetailsOriginal](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - if the login page displayed contains the original (passworded) page
- [pageGroupDetails](../../../javadoc/sk/iway/iwcm/doc/GroupDetails.html) - the directory object of the currently displayed web page
- [tempDetails](../../../javadoc/sk/iway/iwcm/doc/TemplateDetails.html) - template object of the currently displayed web page
- [templatesGroupDetails](../../../javadoc/sk/iway/iwcm/doc/TemplatesGroupBean.html) - the template group object of the displayed web page

Examples of use:

```html
    <span data-th-text="${docDetails.title}">Titulok stránky</span>
    <body data-th-class="${docDetails.fieldA}">
    <meta name="author" data-th-content="${ninja.temp.group.author}" />
    <link rel="canonical" data-th-href="${ninja.page.url}" />
```
