# Template information

| Method | Type | Description |
| ---------------------------------- | -------- | ----------------------------- |
| ${ninja.temp.basePath}             | *String* | Path to the root directory of the template |
| ${ninja.temp.basePathCss}          | *String* | The way to CSS stylesheets |
| ${ninja.temp.basePathJs}           | *String* | The way to JS scripts |
| ${ninja.temp.basePathImg}          | *String* | The way to images |
| ${ninja.temp.lngIso}               | *String* | HTML ISO Language Cod |
| ${ninja.temp.charset}              | *String* | File encoding |
| ${ninja.temp.insertTouchIconsHtml} | *String* | Setting touch icons |
| ${ninja.temp.templateFolderName}   | *String* | Template folder name |

## Path to the root directory of the template *String*

The path to the root directory of the template consisting of `/templates/ + ${ninja.webjet.installName} + ${ninja.temp.templateFolderName}`.

```java
${ninja.temp.basePath}
```

## The path to CSS styles *String*

The path to CSS stylesheets consists of `${ninja.temp.basePath} + /assets/css/`.

```java
${ninja.temp.basePathCss}
```

## The path to JS scripts *String*

Path to JS scripts composed of `${ninja.temp.basePath} + /assets/js/`.

```java
${ninja.temp.basePathJs}
```

## The way to the pictures *String*

The path to the pictures made up of `${ninja.temp.basePath} + /assets/images/`.

```java
${ninja.temp.basePathImg}
```

## HTML ISO Language Code *String*

HTML ISO Language Code reference in the form for example `sk-SK`.

```java
${ninja.temp.lngIso}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:locale" content="${ninja.temp.lngIso}" />
```

Used in :ghost:<code>html-attributes.jsp</code>

```html
<html lang="${ninja.temp.lngIso}" >
```

## File encoding *String*

File encoding e.g. `utf-8`.

```java
${ninja.temp.charset}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta charset="${ninja.temp.charset}">
<meta http-equiv="Content-type" content="text/html;charset=${ninja.temp.charset}" />
```

## Setting touch icons *String*

Returns HTML code with touch icons set. The icon needs to be saved in `/templates/ + ${ninja.temp.basePathImg}` called `touch-icon.png`. The icon should be the size of `192px x 192px`. Touch icons are set via thumb to ensure the icon is the exact size for each device.

```java
${ninja.temp.insertTouchIconsHtml}
```

The HTML code that is generated:

```html
<link rel="apple-touch-icon-precomposed" href="/thumb/templates/interway/ninja-starter-kit/assets/images/touch-icon.png?w=0&h=0&ip=5" />
<link rel="apple-touch-icon-precomposed" sizes="72x72" href="/thumb/templates/interway/ninja-starter-kit/assets/images/touch-icon.png?w=72&h=72&ip=5" />
<link rel="apple-touch-icon-precomposed" sizes="76x76" href="/thumb/templates/interway/ninja-starter-kit/assets/images/touch-icon.png?w=76&h=76&ip=5" />
<link rel="apple-touch-icon-precomposed" sizes="114x114" href="/thumb/templates/interway/ninja-starter-kit/assets/images/touch-icon.png?w=114&h=114&ip=5" />
<link rel="apple-touch-icon-precomposed" sizes="120x120" href="/thumb/templates/interway/ninja-starter-kit/assets/images/touch-icon.png?w=120&h=120&ip=5" />
<link rel="apple-touch-icon-precomposed" sizes="144x144" href="/thumb/templates/interway/ninja-starter-kit/assets/images/touch-icon.png?w=144&h=144&ip=5" />
<link rel="apple-touch-icon-precomposed" sizes="152x152" href="/thumb/templates/interway/ninja-starter-kit/assets/images/touch-icon.png?w=152&h=152&ip=5" />
<link rel="apple-touch-icon-precomposed" sizes="180x180" href="/thumb/templates/interway/ninja-starter-kit/assets/images/touch-icon.png?w=180&h=180&ip=5" />
<link rel="icon" sizes="192x192" href="/thumb/templates/interway/ninja-starter-kit/assets/images/touch-icon.png?w=192&h=192&ip=5" />
```

## Template folder name *String*

Template folder name, default `/ninja-starter-kit/`

```java
${ninja.temp.templateFolderName}
```
