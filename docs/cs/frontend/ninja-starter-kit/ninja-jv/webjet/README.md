# Informace o Webjetu

| Metoda | Typ | Popis |
| --------------------------------- | -------- | ------------------------------------ |
| ${ninja.webjet.installName}       | *String* | Installname |
| ${ninja.webjet.generatedTime}     | *String* | Čas vygenerování stránky |
| ${ninja.webjet.pageFunctionsPath} | *String* | Základní JS funkce |
| ${ninja.webjet.insertJqueryHtml}  | *String* | Globální Jquery knihovna |
| ${ninja.webjet.insertJqueryFake}  | *String* | Blokování globální Jquery knihovny |

## Installname *String*

Vrátí hodnotu InstallName

```java
${ninja.webjet.installName}
```

## Čas vygenerování stránky *String*

Vrátí čas vygenerování stránky na serverové straně ve formátu `DD:MM:RRRR HH:MM:SS` Např. `27.11.2018 16:18:24`.

```java
${ninja.webjet.generatedTime}
```

## Základní JS funkce *String*

Vrátí cestu k základním globálním funkcím webjetu, které jsou potřebné pro správné fungování webu. cestu je třeba vygenerovat do combine, nebo do script tagu jako link.

```java
${ninja.webjet.pageFunctionsPath}
```

Cesta, která se vygeneruje:

```url
/components/_common/javascript/page_functions.js.jsp
```

## Globální Jquery knihovna *String*

Vloží skript s linkem na aktuální verzi jquery, která se ve webjetu používá. Pokud ji nevložíš tímto způsobem, vloží se stejně sama. Tímto způsobem alespoň umíš určit vísto, kam se má volání vygenerovat. Pokud nechceš aby se jquery přidalo, zavolej metodu `${ninja.webjet.insertJqueryFake}`. Ona to nasimuluje a jquery se nepřidá.

```java
${ninja.webjet.insertJqueryHtml}
```

Odkaz, který se vygeneruje:

```html
<script type="text/javascript" src="/components/_common/javascript/jquery.js" ></script>
<script type="text/javascript" src="/components/_common/javascript/page_functions.js.jsp?language=sk" ></script>
<link rel="stylesheet" type="text/css" media="screen" href="/components/form/check_form.css" />
```

## Blokování globální Jquery knihovny *String*

Nasimulování vložení jquery verze. Tím se blokuje automatické vložení aktuální jquery knihovny na web. Je třeba pak ručně přidat volání globálních JS funkcí pomocí `${ninja.webjet.pageFunctionsPath}`.

```java
${ninja.webjet.insertJqueryFake}
```
