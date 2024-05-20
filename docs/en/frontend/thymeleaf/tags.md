# Tags WebJET CMS

You can use special WebJET CMS tags in templates.

## Standard attributes

For a list of available attributes when displaying a web page, see [a separate chapter](webjet-objects.md).

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
<body data-th-class="${docDetails.fieldA}">
	<meta name="author" data-th-if="${!#strings.isEmpty(ninja.temp.group.author)}" data-th-content="${ninja.temp.group.author}" />
	<link data-th-href="${ninja.page.url}" rel="canonical" />
	<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css" />

	<article data-iwcm-write="doc_data" />
	<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
</body>
```

Attributes beginning with `data-th-XXX` shall be implemented and replaced `XXX` attribute by the value of the specified code. When prototyping without WebJET CMS running, you can set the attribute to the default value and replace it when running through WebJET using `data-th-` Attribute:

```html
<!-- content atribut sa pri zobrazeni cez WebJET nahradi za data-th-content hodnotu -->
<meta property="og:title" content="WebJET CMS" data-th-content="${ninja.page.seoTitle}" />
<!-- class atribut sa pri zobrazeni cez WebJET nahradi za data-th-class hodnotu -->
<body class="subpage" data-th-class="${docDetails.fieldA}"></body>
```

## Execution of the INCLUDE tag

Web page objects may contain a tag `!INCLUDE(...)!`, it is not possible to use the standard attribute for its execution `data-th-text` but you need to use a special attribute `data-iwcm-write`. Either can be performed [page attribute](webjet-objects.md) or directly entered `!INCLUDE(...)!` Injunction.

Retrieved from **can** remove wrapping `div` element. The behaviour is as follows:
- if the attribute name starts with `doc_` (e.g. `doc_data`) wrapping `div` element **will be preserved** (for better compatibility in prototyping and the possibility of setting attributes as `class` and the like)
- if the value of the attribute `data-iwcm-remove` Is `false` or NOT `tag` wrapping element **will be preserved**
- in other cases the wrapping `div` element **removed** and the code from WebJET is inserted into the page
```html
<!-- zacina na doc_ standardne by header element zostal, ale nastavenim data-iwcm-remove="tag" header element odstranime -->
<header data-iwcm-write="doc_head" data-iwcm-remove="tag" />
<!-- zacina na doc_ standardne sa article element zachova -->
<article data-iwcm-write="doc_data" class="page-body" />
<!-- NEzacina na doc_ standardne sa div element odstrani -->
<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
```

Use `data-iwcm-remove` simplified from the HTML code point of view is performed when setting `true` Call `element[0].outerHTML=data` (that is, the tag on which it is set is also replaced), otherwise it will execute `element.html(data)`, i.e. it replaces the inside of the tag.

**We also recommend** take advantage of the brand property `data-iwcm-write`which **removes the body**, `div` Signs. You can thus **prototype efficiently at html level without running the template in WebJET**:
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

which means that if you view the html template directly in the browser you will see `<article>Lorem ipsum</article>`. When the template is executed in WebJET, everything inside `<article data-iwcm-write="doc_data">` for html code of web page (including wrappers `<div class="container">....`).

## Combining files

To combine JavaScript and CSS files, you can use the attribute `data-iwcm-combine`. It automatically splits the files into JavaScript and CSS by extension and inserts two separate links to `combine.jsp`. You can use any tag, in the example it is `combine`, when displayed it is removed.

```html
<combine
	basePath="|${ninja.temp.basePath}assets/|"
	,
	data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}
${ninja.webjet.pageFunctionsPath}">
	<link href="css/ninja.min.css" rel="stylesheet" type="text/css" />
	<script src="js/jquery-3.6.0.min.js" type="text/javascript"></script>
	<script src="js/jquery.cookie.js" type="text/javascript"></script>
	<script src="js/bootstrap.bundle.min.js" type="text/javascript"></script>
	<script src="js/global-functions.js" type="text/javascript"></script>
	<script src="js/ninja.min.js" type="text/javascript"></script>
</combine>
```

possibly as `PUG` Minutes:

```html
combine( basePath='|${ninja.temp.basePath}dist/|', data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}\n"+ "${ninja.webjet.pageFunctionsPath}" )
<link href="css/ninja.min.css" rel="stylesheet" type="text/css" />
<script src="js/jquery-3.6.0.min.js" type="text/javascript"></script>
<script src="js/jquery.cookie.js" type="text/javascript"></script>
<script src="js/bootstrap.bundle.min.js" type="text/javascript"></script>
<script src="js/global-functions.js" type="text/javascript"></script>
<script src="js/ninja.min.js" type="text/javascript"></script>
```

Note that in the body `combine` are inserted standard `link` a `script` Signs. These are processed and used in the combined entry. The advantage is that you can have it displayed directly `.html` file in the browser and the above scripts and CSS files are inserted into the page by default. Even without executing the page in WebJET, it is functional in the browser.

The combination first inserts the scripts and CSS from the body and then adds those defined in `data-iwcm-combine` Attribute. Insert only specific scripts and CSS that are only used when processing via WebJET CMS.

## Inserting scripts

There is a special tag for inserting scripts from the scripts application `data-iwcm-script`. It receives the script position as a name (e.g. `header`) as defined in the application. The tag uses the tag `div`, but it is removed when displayed.

```html
<div data-iwcm-script="header" />
```

## Embedding jQuery

WebJET typically needs the embedded jQuery library from `/components/_common/javascript/jquery.min.js` and files `/components/_common/javascript/page_functions.js.jsp` a `/components/form/check_form.css`.

But when using npm, you can embed jQuery into `ninja.js` and thus you don't want the library to be duplicated by WebJET when displaying the object `doc_data`. This can be influenced by the following options:
- If `data-iwcm-combine` Contains `${ninja.webjet.pageFunctionsPath}` it is assumed that jQuery is embedded via e.g. `ninja.js` and is no longer automatically inserted.

- When using `data-iwcm-write="doc_data"` attribute can be added `data-iwcm-jquery` with the following values:
	- `false` - jQuery is already embedded, we don't want to automatically embed it with WebJET
	- `true` - jQuery is inserted automatically, but it is checked for duplication and inserted only if it has not been inserted by WebJET yet (it is also inserted `page_functions.js.jsp` a `check_form.css`)
	- `force` - even if WebJET thinks jQuery is embedded (e.g. from `data-iwcm-combine`) it is forced to be inserted again (it is also inserted `page_functions.js.jsp` a `check_form.css`)
We recommend inserting jQuery via the NPM module into ninja.js and then using `combine` s `${ninja.webjet.pageFunctionsPath}`

```html
<combine
	basePath="|${ninja.temp.basePath}|"
	data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}
    ${ninja.webjet.pageFunctionsPath}">
	<script src="js/ninja.js" type="text/javascript"></script>
</combine>
```

`/components/form/check_form.css``5-modules/md-checkform.scss`.
