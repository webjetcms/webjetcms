# Informace o společnosti Webjet

| Metoda | Typ | Popis
| --------------------------------- | -------- | ------------------------------------ |
| ${ninja.webjet.installName}       | *Řetězec* | Název instalace |
| ${ninja.webjet.generatedTime}     | *Řetězec* | Doba generování stránky |
| ${ninja.webjet.pageFunctionsPath} | *Řetězec* | Základní funkce JS |
| ${ninja.webjet.insertJqueryHtml}  | *Řetězec* | Globální knihovna Jquery |
| ${ninja.webjet.insertJqueryFake}  | *Řetězec* | Blokování globální knihovny Jquery |

## Název instalace *Řetězec*

Vrací hodnotu InstallName

```java
${ninja.webjet.installName}
```

## Doba generování stránky *Řetězec*

Vrací čas generování stránky na straně serveru ve formátu `DD:MM:RRRR HH:MM:SS` Např. `27.11.2018 16:18:24`.

```java
${ninja.webjet.generatedTime}
```

## Základní funkce JS *Řetězec*

Vrací cestu k základním globálním funkcím Webjet, které jsou nutné pro správnou funkci webu.Cesta musí být vytvořena v tagu combine nebo script jako odkaz.

```java
${ninja.webjet.pageFunctionsPath}
```

Vygenerovaná cesta:

```url
/components/_common/javascript/page_functions.js.jsp
```

## Globální knihovna Jquery *Řetězec*

Vloží skript s odkazem na aktuální verzi jquery, která je použita ve webovém projektu. Pokud jej tímto způsobem nevložíte, vloží se stejně sám. Tímto způsobem můžete alespoň určit místo, kde se má volání vygenerovat. Pokud nechcete, aby se jquery přidávalo, zavolejte metodu `${ninja.webjet.insertJqueryFake}`. Simuluje to a jquery se nepřipojí.

```java
${ninja.webjet.insertJqueryHtml}
```

Vygenerovaná zpráva:

```html
<script type="text/javascript" src="/components/_common/javascript/jquery.js" ></script>
<script type="text/javascript" src="/components/_common/javascript/page_functions.js.jsp?language=sk" ></script>
<link rel="stylesheet" type="text/css" media="screen" href="/components/form/check_form.css" />
```

## Blokování globální knihovny Jquery *Řetězec*

Simulace vložení verze jquery. Tím se zablokuje automatické vložení aktuální knihovny jquery na web. Pak je nutné ručně přidat globální volání funkcí JS pomocí příkazu `${ninja.webjet.pageFunctionsPath}`.

```java
${ninja.webjet.insertJqueryFake}
```
