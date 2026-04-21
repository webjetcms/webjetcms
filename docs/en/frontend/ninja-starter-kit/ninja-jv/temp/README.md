# Template information

| Method | Type | Description |
| --- | --- | --- |
| ${ninja.temp.basePath} | *String* | Path to the template root directory |
| ${ninja.temp.basePathCss} | *String* | The path to CSS styles |
| ${ninja.temp.basePathJs} | *String* | The path to JS scripts |
| ${ninja.temp.basePathImg} | *String* | Path to images |
| ${ninja.temp.lngIso} | *String* | HTML ISO Language Code |
| ${ninja.temp.charset} | *String* | File encoding |
| ${ninja.temp.insertTouchIconsHtml} | *String* | Touch icon settings |
| ${ninja.temp.templateFolderName} | *String* | Template folder name |

## Template root directory path *String*

The path to the template root directory is compiled from `/templates/ + ${ninja.webjet.installName} + ${ninja.temp.templateFolderName}`.

```java
${ninja.temp.basePath}
```

## Path to CSS styles *String*

The path to CSS styles is compiled from `${ninja.temp.basePath} + /assets/css/`.

```java
${ninja.temp.basePathCss}
```

## Path to JS scripts *String*

The path to JS scripts is compiled from `${ninja.temp.basePath} + /assets/js/`.

```java
${ninja.temp.basePathJs}
```

## Image path *String*

Image path compiled from `${ninja.temp.basePath} + /assets/images/`.

```java
${ninja.temp.basePathImg}
```

## HTML ISO Language Code *String*

HTML ISO Language Code reference in the form, for example, `sk-SK`.

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

File encoding, e.g. `utf-8`.

```java
${ninja.temp.charset}
```

Used in :ghost:<code>head.jsp</code>
```html
<meta charset="${ninja.temp.charset}">
<meta http-equiv="Content-type" content="text/html;charset=${ninja.temp.charset}" />
```

## Touch icon settings *String*

Returns HTML code with touch icons set. The icon must be stored in `/templates/ + ${ninja.temp.basePathImg}` with the name `touch-icon.png`. The icon should be sized `192px x 192px`. Touch icons are set via thumb so that the icon is the exact size for each device.

```java
${ninja.temp.insertTouchIconsHtml}
```

The HTML code that will be generated:
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