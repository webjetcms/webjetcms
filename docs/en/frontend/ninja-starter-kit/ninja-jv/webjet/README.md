# Information about Webjet

| Method | Type | Description | | | --------------------------------- | -------- | ------------------------------------ | | ${ninja.webjet.installName}       | *String* | Installname | ${ninja.webjet.generatedTime}     | *String* | Page generation time | | ${ninja.webjet.pageFunctionsPath} | *String* | Basic JS Functions | | ${ninja.webjet.insertJqueryHtml}  | *String* | Global Jquery Library | | ${ninja.webjet.insertJqueryFake}  | *String* | Blocking the global Jquery library |

## Installname *String*

Returns the value of InstallName

```java
${ninja.webjet.installName}
```

## Page generation time *String*

Returns the server-side page generation time in the format `DD:MM:RRRR HH:MM:SS` e.g. `27.11.2018 16:18:24`.

```java
${ninja.webjet.generatedTime}
```

## Basic JS functions *String*

Returns the path to the basic global webjet functions that are required for the site to function properly.The path must be generated in a combine or script tag as a link.

```java
${ninja.webjet.pageFunctionsPath}
```

The path that is generated:

```url
/components/_common/javascript/page_functions.js.jsp
```

## Global Jquery library *String*

Inserts a script with a link to the current version of jquery that is used in the webject. If you don't insert it this way, it will insert itself anyway. At least this way you can specify the location where the call should be generated. If you don't want jquery to be added, call the method `${ninja.webjet.insertJqueryFake}`. She simulates it and jquery doesn't join.

```java
${ninja.webjet.insertJqueryHtml}
```

The message that is generated:

```html
<script type="text/javascript" src="/components/_common/javascript/jquery.js"></script>
<script type="text/javascript" src="/components/_common/javascript/page_functions.js.jsp?language=sk"></script>
<link rel="stylesheet" type="text/css" media="screen" href="/components/form/check_form.css" />
```

## Blocking the global Jquery library *String*

`${ninja.webjet.pageFunctionsPath}`.

```java
${ninja.webjet.insertJqueryFake}
```
