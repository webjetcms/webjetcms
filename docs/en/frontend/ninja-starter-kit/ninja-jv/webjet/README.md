# Information about Webjet

| Method | Type | Description |
| --- | --- | --- |
| ${ninja.webjet.installName} | *String* | Install name |
| ${ninja.webjet.generatedTime} | *String* | Page generation time |
| ${ninja.webjet.pageFunctionsPath} | *String* | Basic JS functions |
| ${ninja.webjet.insertJqueryHtml} | *String* | Global Jquery library  |
| ${ninja.webjet.insertJqueryFake} | *String* | Blocking the global Jquery library |

## Install name *String*

Returns the InstallName value.

```java
${ninja.webjet.installName}
```

## Page generation time *String*

Returns the time the page was generated on the server side in the format `DD:MM:RRRR HH:MM:SS` e.g. `27.11.2018 16:18:24`.

```java
${ninja.webjet.generatedTime}
```

## Basic JS functions *String*

Returns the path to the basic global webjet functions that are necessary for the correct functioning of the website. The path needs to be generated in combine or in a script tag as a link.

```java
${ninja.webjet.pageFunctionsPath}
```

The path that will be generated:
```url
/components/_common/javascript/page_functions.js.jsp
```

## Global Jquery library *String*

It inserts a script with a link to the current version of jQuery that is being used in the website. If you don't insert it this way, it will insert itself anyway. This way you can at least specify where the call should be generated.
If you don't want jQuery to be added, call the `${ninja.webjet.insertJqueryFake}` method. It will simulate it and jQuery will not be added.

```java
${ninja.webjet.insertJqueryHtml}
```

The link that will be generated:
```html
<script type="text/javascript" src="/components/_common/javascript/jquery.js" ></script>
<script type="text/javascript" src="/components/_common/javascript/page_functions.js.jsp?language=sk" ></script>
<link rel="stylesheet" type="text/css" media="screen" href="/components/form/check_form.css" />
```

## Blocking the global Jquery library *String*

Simulate the insertion of a jQuery version. This blocks the automatic insertion of the current jQuery library into the website. It is then necessary to manually add the call to global JS functions using `${ninja.webjet.pageFunctionsPath}`.

```java
${ninja.webjet.insertJqueryFake}
```
