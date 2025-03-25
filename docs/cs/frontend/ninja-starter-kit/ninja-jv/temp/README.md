# Informace o šabloně

| Metoda | Typ | Popis
| ---------------------------------- | -------- | ----------------------------- |
| ${ninja.temp.basePath}             | *String* | Cesta ke kořenovému adresáři šablony |
| ${ninja.temp.basePathCss}          | *String* | Cesta k souborům stylů CSS |
| ${ninja.temp.basePathJs}           | *String* | Cesta ke skriptům JS |
| ${ninja.temp.basePathImg}          | *String* | Cesta k obrázkům |
| ${ninja.temp.lngIso}               | *String* | HTML ISO Language Cod |
| ${ninja.temp.charset}              | *String* | Kódování souborů |
| ${ninja.temp.insertTouchIconsHtml} | *String* | Nastavení dotykových ikon |
| ${ninja.temp.templateFolderName}   | *String* | Název složky šablony |

## Cesta k root adresáři šablony *String*

Cesta k root adresáři šablony vyskládaná z `/templates/ + ${ninja.webjet.installName} + ${ninja.temp.templateFolderName}`.

```java
${ninja.temp.basePath}
```

## Cesta k CSS stylům *String*

Cesta k CSS stylům vyskládaná z `${ninja.temp.basePath} + /assets/css/`.

```java
${ninja.temp.basePathCss}
```

## Cesta k JS skriptům *String*

Cesta k JS skriptům vyskládaná z `${ninja.temp.basePath} + /assets/js/`.

```java
${ninja.temp.basePathJs}
```

## Cesta k obrázkům *String*

Cesta k obrázkům vyskládaná z `${ninja.temp.basePath} + /assets/images/`.

```java
${ninja.temp.basePathImg}
```

## HTML ISO Language Code *String*

HTML ISO Language Code reference ve tvaru například `sk-SK`.

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

## Kódování souboru *String*

Kódování souboru Např. `utf-8`.

```java
${ninja.temp.charset}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta charset="${ninja.temp.charset}">
<meta http-equiv="Content-type" content="text/html;charset=${ninja.temp.charset}" />
```

## Nastavení touch ikon *String*

Vrátí HTML kód s nastavenými touch ikonami. Ikonu je třeba mít uloženou v `/templates/ + ${ninja.temp.basePathImg}` s názvem `touch-icon.png`. Ikona by měla mít velikost `192px x 192px`. Touch ikony jsou nastaveny přes thumb, aby měla ikona přesnou velikost pro každé zařízení.

```java
${ninja.temp.insertTouchIconsHtml}
```

HTML kód, který se vygeneruje:

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

## Název složky šablony *String*

Název složky šablony, defaultně `/ninja-starter-kit/`

```java
${ninja.temp.templateFolderName}
```
