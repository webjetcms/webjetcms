# WebJET CMS Brands

You can use special WebJET CMS tags in templates.

## Standard attributes

The list of available attributes when viewing a web page can be found in [a separate chapter](webjet-objects.md).

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
<body data-th-class="${docDetails.fieldA}">
<meta name="author" data-th-if="${!#strings.isEmpty(ninja.temp.group.author)}" data-th-content="${ninja.temp.group.author}" />
<link data-th-href="${ninja.page.canonical}" rel="canonical" />
<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css"/>

<article data-iwcm-write="doc_data"/>
<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
```

Attributes starting with ```data-th-XXX``` will be executed and replace the ```XXX``` attribute with the value of the specified code. When prototyping without running WebJET CMS, you can set the attribute to the default value and replace it with the ```data-th-``` attribute when running through WebJET:

```html
<!-- content atribut sa pri zobrazeni cez WebJET nahradi za data-th-content hodnotu -->
<meta property="og:title" content="WebJET CMS" data-th-content='${ninja.page.seoTitle}'/>
<!-- class atribut sa pri zobrazeni cez WebJET nahradi za data-th-class hodnotu -->
<body class="subpage" data-th-class="${docDetails.fieldA}">
```

## Implementing the INCLUDE tag

Web page objects can contain the ```!INCLUDE(...)!``` tag, to execute it, it is not possible to use the standard ```data-th-text``` attribute, but it is necessary to use the special ```data-iwcm-write``` attribute. It is possible to execute either the [page attribute](webjet-objects.md) or a directly entered ```!INCLUDE(...)!``` command.

The execution **may** remove the wrapping ```div``` element. The behavior is as follows:

- if the attribute name starts with ```doc_``` (e.g. ```doc_data```) the wrapping ```div``` element **is preserved** (for better compatibility when prototyping and the ability to set attributes like ```class``` and the like)
- if the value of the attribute ```data-iwcm-remove``` is ```false``` or NOT ```tag``` the wrapper element **is preserved**
- in other cases, the wrapping ```div``` element is **removed** and the code from WebJET is inserted into the page

```html
<!-- zacina na doc_ standardne by header element zostal, ale nastavenim data-iwcm-remove="tag" header element odstranime -->
<header data-iwcm-write="doc_head" data-iwcm-remove="tag" />
<!-- zacina na doc_ standardne sa article element zachova -->
<article data-iwcm-write="doc_data" class="page-body"/>
<!-- NEzacina na doc_ standardne sa div element odstrani -->
<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
```

Using ```data-iwcm-remove```, simplified from an HTML code perspective, when ```true``` is set, it calls ```element[0].outerHTML=data``` (which also replaces the tag on which it is set), otherwise it executes ```element.html(data)```, which replaces the inside of the tag.

**We also recommend** using the ```data-iwcm-write``` tag property, which **removes the body** of the ```div``` tag. This way you can **effectively prototype at the html level without running the template in WebJET**:

```html
<article data-iwcm-write="doc_data">
    <!-- content - toto je len ukazka pre html sablonu, vo WebJETe sa toto cele nahradi za obsah stranky -->
    <div class="container">
        <div class="row">
            <div class="col-md-12">
                <p>Lorem ipsum</p>
            </div>
        </div>
    </div>
</article>
```

which means that if you view the html template directly in a browser you will see ```<article>Lorem ipsum</article>```. When the template is executed in WebJET everything inside ```<article data-iwcm-write="doc_data">``` will be replaced with the html code of the web page (including the wrappers ```<div class="container">....```).

## Combining files

To combine JavaScript and CSS files, you can use the ```data-iwcm-combine``` attribute. This automatically splits the files into JavaScript and CSS based on the extension and inserts two separate links to ```combine.jsp```. You can use any tag, in the example it is ```combine```, it will be removed when displayed.

```html
<combine
basePath='|${ninja.temp.basePath}assets/|',
data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}
${ninja.webjet.pageFunctionsPath}"
>
    <link href="css/ninja.min.css" rel="stylesheet" type="text/css"/>
    <script src="js/jquery-3.6.0.min.js" type="text/javascript"></script>
    <script src="js/jquery.cookie.js" type="text/javascript"></script>
    <script src="js/bootstrap.bundle.min.js" type="text/javascript"></script>
    <script src="js/global-functions.js" type="text/javascript"></script>
    <script src="js/ninja.min.js" type="text/javascript"></script>
</combine>
```

or as ```PUG``` notation:

```html
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
```

Note that the standard ```link``` and ```script``` tags are inserted in the body of ```combine```. These are processed and used in the combined notation. The advantage is that you can have the ```.html``` file displayed directly in the browser and the listed scripts and CSS files are inserted into the page by default. Even without executing the page in WebJET, it is functional in the browser.

The combination will first insert scripts and CSS from the body and then add those defined in the ```data-iwcm-combine``` attribute. Insert only specific scripts and CSS there that are only used when processed through WebJET CMS.

The `basePath` attribute sets the root folder for individual files so you don't have to type the full path. For addresses starting with `/templates/` or `/components/`, the value in `basePath` is not added.

## Script embedding

For inserting scripts from the scripts application, there is a special tag ```data-iwcm-script```. It is given the script position as a name (e.g. ```header```) as defined in the application. The tag uses the tag ```div```, but it is removed when displayed.

```html
<div data-iwcm-script="header"/>
```

## jQuery embedding

WebJET typically needs the embedded jQuery library from ```/components/_common/javascript/jquery.min.js``` and the files ```/components/_common/javascript/page_functions.js.jsp``` and ```/components/form/check_form.css``` to display a page.

However, when using npm, you can include jQuery in ```ninja.js``` and thus do not want WebJET to duplicately include the library when displaying the ```doc_data``` object. This can be influenced by the following options:

- If ```data-iwcm-combine``` contains ```${ninja.webjet.pageFunctionsPath}```, it is assumed that jQuery is inserted via, for example, ```ninja.js``` and will no longer be inserted automatically.
- When using ```data-iwcm-write="doc_data"```, it is possible to add the attribute ```data-iwcm-jquery``` with the following values:
  - ```false``` - ​​jQuery is already included, we don't want to automatically include it with WebJET
  - ```true``` - ​​jQuery is inserted automatically, but it is checked for duplication and is inserted only if it has not been inserted by WebJET yet (```page_functions.js.jsp``` and ```check_form.css``` are also inserted)
  - ```force``` - ​​even if WebJET thinks jQuery is embedded (e.g. from ```data-iwcm-combine```) it will be forced to be embedded again (```page_functions.js.jsp``` and ```check_form.css``` will also be embedded)

We recommend loading jQuery via the NPM module into ninja.js and then using ```combine``` with ```${ninja.webjet.pageFunctionsPath}``` to notify WebJET that jQuery is loaded:

```html
<combine
    basePath="|${ninja.temp.basePath}|"
    data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}
    ${ninja.webjet.pageFunctionsPath}"
>
    <script src="js/ninja.js" type="text/javascript"></script>
</combine>
```

You need to put the CSS for ```/components/form/check_form.css``` in your scss (download the content in your browser from the given URL). We recommend putting it in ```5-modules/md-checkform.scss```.