# Test prekladov

Cieľom tohto súboru je testovať prekladač a zachovanie formátovania. Tento súbor by mal obsahovat rôzne typy formátovania, ako sú **tučný text**, *kurzíva*, `kód`, a odkazy [Google](https://www.google.com).

Pre testovanie nastavte v súbore `deepmark.config.mjs` hodnotu `javascript` nasledovne:

```javascript
    directories: [
        ['sk/translation-test', '$langcode$/translation-test'],
    ]
```

Taktiež by mal obsahovať zoznamy, tu je dôležité, že za týmto nadpisom **zostane prázdny riadok**.

- Prvý bod
- Druhý bod
- Tretí bod

Dôležité je, aby zachoval štruktúru, prázdne riadky a podobne.

## Sidebar problém

Tu zmaže prázdny riadok za `point_left`, čo spôsobí, že sa tento odkaz spojí s ďalším textom. POZOR: otestujte správanie aj v `_sidebar.md` súbore, pretože tu to niekedy funguje správne.

POZOR: tiež na to, že spraví z `point_left` `point\_left`.

<div class="sidebar-section">Manuál pre prevádzku</div>

- [:point_left: Späť na Úvod](/?back)

- Bezpečnosť
  - [Bezpečnostné testy](../sysadmin/pentests/README.md)
  - [Kontrola zraniteľností knižníc](../sysadmin/dependency-check/README.md)
  - [Aktualizácia WebJETu](../sysadmin/update/README.md)

## Tabuľky

Ukážka tabuľky. Tu POZOR na to, aby nespravil z `perex_group_id` `perex\_group\_id` a podobne.

| perex_group_id | perex_group_name      | domain_id | available_groups |
|----------------|-----------------------|-----------|------------------|
| 3              | ďalšia perex skupina  | 1         | NULL             |
| 645            | deletedPerexGroup     | 1         | NULL             |
| 794            | kalendár-udalostí     | 1         | NULL             |
| 1438           | ďalšia perex skupina  | 83        | NULL             |
| 1439           | deletedPerexGroup     | 83        | NULL             |
| 1440           | kalendár-udalostí     | 83        | NULL             |

## HTML kód

V MD súbore môže byť aj HTML kód, napríklad YouTube video. Tu nesmie zabudnúť na úvodzovky za `allow` atribútom.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/XRnwipQ-mH4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Changelog

V changelogu máme rôzne podivné konštrukcie.

- Upravené spracovanie **nahrávania súborov** `multipart/form-data`, viac v [sekcii pre programátora](#test-prekladov) (#57793-3).
- Odporúčame **skontrolovať funkčnosť všetkých formulárov** z dôvodu úprav ich spracovania, viac informácií v sekcii [pre programátora](#test-prekladov) (#58161).

### Webové stránky

- Pridaná možnosť vkladať `PICTURE` element, ktorý zobrazuje [obrázok podľa rozlíšenia obrazovky](../frontend/setup/ckeditor.md#picture-element) návštevníka. Môžete teda zobraziť rozdielne obrázky na mobilnom telefóne, tablete alebo počítači (#58141).

![](../frontend/setup/picture-element.png)

- Pridaná možnosť vkladať [vlastné ikony](../frontend/setup/ckeditor.md#svg-ikony) definované v spoločnom SVG súbore (#58181).

![](../frontend/setup/svgicon.png)