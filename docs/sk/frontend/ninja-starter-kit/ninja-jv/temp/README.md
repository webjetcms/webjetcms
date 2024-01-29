# Informácie o šablóne

| Metóda | Typ | Popis |
| --- | --- | --- |
| ${ninja.temp.basePath} | *String* | Cesta k root adresáru šablóny |
| ${ninja.temp.basePathCss} | *String* | Cesta k CSS štýlom |
| ${ninja.temp.basePathJs} | *String* | Cesta k JS skriptom |
| ${ninja.temp.basePathImg} | *String* | Cesta k obrázkom |
| ${ninja.temp.lngIso} | *String* | HTML ISO Language Cod |
| ${ninja.temp.charset} | *String* | Kódovanie súboru |
| ${ninja.temp.insertTouchIconsHtml} | *String* | Nastavenie touch ikon |
| ${ninja.temp.templateFolderName} | *String* | Názov priečinka šablóny |

## Cesta k root adresáru šablóny *String*
Cesta k root adresáru šablóny vyskladaná z `/templates/ + ${ninja.webjet.installName} + ${ninja.temp.templateFolderName}`.

```java
${ninja.temp.basePath}
```

## Cesta k CSS štýlom *String*
Cesta k CSS štýlom vyskladaná z `${ninja.temp.basePath} + /assets/css/`.

```java
${ninja.temp.basePathCss}
```

## Cesta k JS skriptom *String*
Cesta k JS skriptom vyskladaná z `${ninja.temp.basePath} + /assets/js/`.

```java
${ninja.temp.basePathJs}
```

## Cesta k obrázkom *String*
Cesta k obrázkom vyskladaná z `${ninja.temp.basePath} + /assets/images/`.

```java
${ninja.temp.basePathImg}
```

## HTML ISO Language Code *String*
HTML ISO Language Code referencia v tvare napríklad `sk-SK`.

```java
${ninja.temp.lngIso}
```

Použité v :ghost:<code>head.jsp</code>
```html
<meta property="og:locale" content="${ninja.temp.lngIso}" />
```

Použité v :ghost:<code>html-attributes.jsp</code>
```html
<html lang="${ninja.temp.lngIso}" >
```

## Kódovanie súboru *String*
Kódovanie súboru napr. `utf-8`.

```java
${ninja.temp.charset}
```

Použité v :ghost:<code>head.jsp</code>
```html
<meta charset="${ninja.temp.charset}">
<meta http-equiv="Content-type" content="text/html;charset=${ninja.temp.charset}" />
```

## Nastavenie touch ikon *String*
Vráti HTML kód s nastavenými touch ikonami. Ikonu je potrebné mať uloženú v `/templates/ + ${ninja.temp.basePathImg}` s názvom `touch-icon.png`. Ikona by mala mať veľkosť `192px x 192px`. Touch ikony sú nastavené cez thumb, aby mala ikona presnú veľkosť pre každé zariadenie.

```java
${ninja.temp.insertTouchIconsHtml}
```

HTML kód, ktorý sa vygeneruje:
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

## Názov priečinka šablóny *String*
Názov priečinka šablóny, defaultne `/ninja-starter-kit/`

```java
${ninja.temp.templateFolderName} 
```