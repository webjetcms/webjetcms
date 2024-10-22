# Informace o šabloně

| Metoda | Typ | Popis
| ---------------------------------- | -------- | ----------------------------- |
| ${ninja.temp.basePath}             | *Řetězec* | Cesta ke kořenovému adresáři šablony |
| ${ninja.temp.basePathCss}          | *Řetězec* | Cesta k souborům stylů CSS |
| ${ninja.temp.basePathJs}           | *Řetězec* | Cesta ke skriptům JS |
| ${ninja.temp.basePathImg}          | *Řetězec* | Cesta k obrázkům |
| ${ninja.temp.lngIso}               | *Řetězec* | HTML ISO Language Cod |
| ${ninja.temp.charset}              | *Řetězec* | Kódování souborů |
| ${ninja.temp.insertTouchIconsHtml} | *Řetězec* | Nastavení dotykových ikon |
| ${ninja.temp.templateFolderName}   | *Řetězec* | Název složky šablony |

## Cesta ke kořenovému adresáři šablony *Řetězec*

Cesta ke kořenovému adresáři šablony, který se skládá z `/templates/ + ${ninja.webjet.installName} + ${ninja.temp.templateFolderName}`.

```java
${ninja.temp.basePath}
```

## Cesta ke stylům CSS *Řetězec*

Cesta k souborům stylů CSS se skládá z následujících položek `${ninja.temp.basePath} + /assets/css/`.

```java
${ninja.temp.basePathCss}
```

## Cesta ke skriptům JS *Řetězec*

Cesta ke skriptům JS složeným z `${ninja.temp.basePath} + /assets/js/`.

```java
${ninja.temp.basePathJs}
```

## Cesta k obrázkům *Řetězec*

Cesta k obrázkům se skládá z `${ninja.temp.basePath} + /assets/images/`.

```java
${ninja.temp.basePathImg}
```

## Kód jazyka HTML ISO *Řetězec*

Odkaz na kód jazyka HTML ISO například ve tvaru `sk-SK`.

```java
${ninja.temp.lngIso}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta property="og:locale" content="${ninja.temp.lngIso}" />
```

Používá se v :ghost:<code>html-attributes.jsp</code>

```html
<html lang="${ninja.temp.lngIso}" >
```

## Kódování souborů *Řetězec*

Kódování souborů, např. `utf-8`.

```java
${ninja.temp.charset}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta charset="${ninja.temp.charset}">
<meta http-equiv="Content-type" content="text/html;charset=${ninja.temp.charset}" />
```

## Nastavení dotykových ikon *Řetězec*

Vrátí kód HTML se sadou dotykových ikon. Ikona musí být uložena v `/templates/ + ${ninja.temp.basePathImg}` s názvem `touch-icon.png`. Ikona by měla mít velikost `192px x 192px`. Dotykové ikony se nastavují pomocí palce, aby byla zajištěna přesná velikost ikony pro každé zařízení.

```java
${ninja.temp.insertTouchIconsHtml}
```

Vygenerovaný kód HTML:

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

## Název složky šablony *Řetězec*

Název složky šablony, výchozí `/ninja-starter-kit/`

```java
${ninja.temp.templateFolderName}
```
