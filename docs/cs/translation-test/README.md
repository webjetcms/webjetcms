# Test překladů

Cílem tohoto souboru je testovat překladač a zachování formátování. Tento soubor by měl obsahovat různé typy formátování, jako jsou **tučný text**, *kurziva*, `kód`, a odkazy [Google](https://www.google.com).

Pro testování nastavte v souboru `deepmark.config.mjs` hodnotu `javascript` následovně:

```javascript
    directories: [
        ['sk/translation-test', '$langcode$/translation-test'],
    ]
```

Také by měl obsahovat seznamy, zde je důležité, že za tímto nadpisem **zůstane prázdný řádek**.

- První bod
- Druhý bod
- Třetí bod

Důležité je, aby zachoval strukturu, prázdné řádky a podobně.

## Sidebar problém

Zde smaže prázdný řádek za `point_left`, což způsobí, že se tento odkaz spojí s dalším textem. POZOR: otestujte chování i v `_sidebar.md` souboru, protože zde to někdy funguje správně.

POZOR: také na to, že udělá z `point_left` `point\_left`.

 <div class="sidebar-section">Manuál pro provoz</div>

- [:point\_left: Zpět na Úvod](/?back)
- Bezpečnost
  - [Bezpečnostní testy](../sysadmin/pentests/README.md)
  - [Kontrola zranitelností knihoven](../sysadmin/dependency-check/README.md)
  - [Aktualizace WebJETu](../sysadmin/update/README.md)

## Tabulky

Ukázka tabulky. Zde POZOR na to, aby neudělal z `perex_group_id` `perex\_group\_id` a podobně.

| perex\_group\_id | perex\_group\_name     | domain\_id | available\_groups |
| -------------- | -------------------- | --------- | ---------------- |
| 3              | další perex skupina | 1         | NULL             |
| 645            | deletedPerexGroup    | 1         | NULL             |
| 794            | kalendář-událostí    | 1         | NULL             |
| 1438           | další perex skupina | 83        | NULL             |
| 1439           | deletedPerexGroup    | 83        | NULL             |
| 1440           | kalendář-událostí    | 83        | NULL             |

## HTML kód

V MD souboru může být i HTML kód, například YouTube video. Zde nesmí zapomenout na uvozovky za `allow` atributem.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/XRnwipQ-mH4" title="YouTube video player" frameborder="0" allow="accelerometr; autoplay; clipboard-write; encrypted-media; allowfullscreen></iframe>
</div>

## Changelog

V changelogu máme různé podivné konstrukce.

- Upraveno zpracování **nahrávání souborů** `multipart/form-data`, více v [sekci pro programátora](#test-překladů) (#57793-3).
- Doporučujeme **zkontrolovat funkčnost všech formulářů** z důvodu úprav jejich zpracování, více informací v sekci [pro programátora](#test-překladů) (#58161).

### Webové stránky

- Přidána možnost vkládat `PICTURE` element, který zobrazuje [obrázek podle rozlišení obrazovky](../frontend/setup/ckeditor.md#picture-element) návštěvníka. Můžete tedy zobrazit rozdílné obrázky na mobilním telefonu, tabletu nebo počítači (#58141).

![](../frontend/setup/picture-element.png)

- Přidána možnost vkládat [vlastní ikony](../frontend/setup/ckeditor.md#svg-ikony) definované ve společném SVG souboru (#58181).

![](../frontend/setup/svgicon.png)
