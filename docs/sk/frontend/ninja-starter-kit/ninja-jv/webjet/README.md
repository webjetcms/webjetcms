# Informácie o Webjete
| Metóda | Typ | Popis |
| --- | --- | --- |
| ${ninja.webjet.installName} | *String* | Installname |
| ${ninja.webjet.generatedTime} | *String* | Čas vygenerovania stránky |
| ${ninja.webjet.pageFunctionsPath} | *String* | Základné JS funkcie |
| ${ninja.webjet.insertJqueryHtml} | *String* | Globálna Jquery knižnica  |
| ${ninja.webjet.insertJqueryFake} | *String* | Blokovanie globálnej Jquery knižnice |

## Installname *String*
Vráti hodnotu InstallName

```java
${ninja.webjet.installName}
```

## Čas vygenerovania stránky *String*
Vráti čas vygenerovania stránky na serverovej strane vo formáte `DD:MM:RRRR HH:MM:SS` napr. `27.11.2018 16:18:24`.

```java
${ninja.webjet.generatedTime}
```

## Základné JS funkcie *String*
Vráti cestu k základným globálnym funkciam webjetu, ktoré sú potrebné pre správne fungovanie webu. cestu je potrebné vygenerovať do combine-u, alebo do script tagu ako link.

```java
${ninja.webjet.pageFunctionsPath}
```

Cesta, ktorá sa vygeneruje:
```url
/components/_common/javascript/page_functions.js.jsp
```

## Globálna Jquery knižnica *String*
Vloží skript s linkom na aktuálnu verziu jquery, ktorá sa vo webjete používa. Ak ju nevložíš týmto spôsobom, vloží sa aj tak sama. Týmto spôsobom aspoň vieš určiť viesto, kam sa má volanie vygenerovať.
Ak nechceš aby sa jquery pridalo, zavolaj metódu `${ninja.webjet.insertJqueryFake}`. Ona to nasimuluje a jquery sa nepridá.

```java
${ninja.webjet.insertJqueryHtml}
```

Odkaz, ktorý sa vygeneruje:
```html
<script type="text/javascript" src="/components/_common/javascript/jquery.js" ></script>
<script type="text/javascript" src="/components/_common/javascript/page_functions.js.jsp?language=sk" ></script>
<link rel="stylesheet" type="text/css" media="screen" href="/components/form/check_form.css" />
```

## Blokovanie globálnej Jquery knižnice *String*
Nasimulovanie vloženia jquery verzie. Týmo sa blokne automatické vloženie aktuálnej jquery knižnice na web. Je potrebné potom ručne pridať volanie globálnych JS funkcií pomocou `${ninja.webjet.pageFunctionsPath}`.

```java
${ninja.webjet.insertJqueryFake}
```
