# Nastavenie vodotlače

Vkladanie vodotlače vyžaduje nasledovné nastavenie servera:

- Na serveri dostupná knižnica [ImageMagick](https://imagemagick.org/script/download.php) konkrétne príkazy ```convert``` a ```composite```.
- Nastavená konfiguračná premenná ```imageMagickDir``` na adresár s inštaláciou ```ImageMagick``` (typicky ```/usr/bin```).
- Povolené vkladanie vodotlače nastavením konfiguračnej premennej ```galleryEnableWatermarking``` na hodnotu ```true```.

Až po splnení nastavení servera sa začne vkladať nastavená vodotlač do obrázka.

![](watermark-applied.png)

Podporované sú nasledovné formáty obrázkov vodotlače:

- ```png``` - obrázok je vložený do nahratého obrázku bez zmeny veľkosti, je preto potrebné vhodne nastaviť veľkosť vodotlače (uvedomte si, že fotogaléria obsahuje malý aj veľký obrázok a do oboch bude vložená vodotlač rovnakej veľkosti).
- ```svg``` - vodotlač je do obrázku vkladaná s korektným škálovaním veľkosti podľa veľkosti nahratého obrázku. Veľkosť vodotlače sa nastavuje v konfiguračnej premennej ```galleryWatermarkSvgSizePercent``` (štandardne 5 percent) a ```galleryWatermarkSvgMinHeight``` (štandardne 30 bodov). Veľkosť vodotlače sa teda automaticky prispôsobí veľkosti obrázku. Ukážková [svg vodotlač](watermark.svg).

!>**Upozornenie:** uvedomte si, že v galérii sa vodotlač nevkladá do originálneho obrázku, je to tak z dôvodu možnosti pregenerovania existujúcich obrázkov (napr. zmena veľkosti, alebo zmena vodotlače). Originálny obrázok je dostupný s prefixom ```o_``` a teda je možné ho takto verejne získať. Ak nutne potrebujete mať vodotlač vo všetkých obrázkoch je potrebné nastaviť automatické aplikovanie vodotlače po nahratí obrázka nastavením konf. premennej `galleryWatermarkApplyOnUpload` na hodnotu `true`.

ImageMagick má štandardne zakázanú prácu s SVG súbormi, je preto potrebné upraviť súbor `/etc/ImageMagick-7/policy.xml` v ktorom je potrebné pridať riadky:

```xml
    <policy domain="module" rights="read|write" pattern="SVG"/>
    <policy domain="coder" rights="read|write" pattern="SVG" />
```

a vymazať hodnotu `SVG` z `<policy domain="module" rights="write" pattern="{MSL,MVG,PS,URL,XPS}"/>`.

## Automatické aplikovanie vodotlače po nahratí obrázku

Automatické vkladanie vodotlače do fotiek pri ich nahratí do WebJETu zapnete nastavením konfiguračnej premennej ```galleryWatermarkApplyOnUpload``` na ```true```. Vodotlač sa vkladá do nahratej fotky, aby nenastávala nasledovne duplicita jej vloženia pri zmene veľkosti fotky a podobne.

Výnimky, pre ktoré sa vodotlač pri nahratí súboru neaplikuje je možné definovať v konfiguračnej premennej ```galleryWatermarkApplyOnUploadExceptions``` (štandardne nastavené na `logo,nowatermark,system`) kde sú výrazy, ktoré keď sa nájdu v ceste k obrázku (meno obrázku alebo názov adresára), tak sa vodotlač neaplikuje.

Vodotlač sa pri tomto režime vkladá rovnaká pre celý web (nie je možné špecifikovať rôzne vodotlače), pre multidomain inštaláciu je možné nastaviť obrázok pre každú doménu. Nastavuje sa v konfiguračnej premennej ```galleryWatermarkApplyOnUploadDir``` (predvolene /templates/{INSTALL_NAME}/assets/watermark/) - adresár kde sú umiestnené obrázky pre automatickú vodotlač pri nahratí obrázku. Názov obrázku musí byť ```default.png```, pri multidomain je možnosť mať pre každú doménu iný, v tvare ```domena.png``` (napr. ```www.interway.sk.png```).

Pozíciu vodotlače nastavujete v konfiguračnej premennej ```galleryWatermarkGravity``` (prednastavené na Center). Možnosti podľa svetových strán v angličtine: ```NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast```. Priesvitnosť vodotlače môžete nastaviť v premennej ```galleryWatermarkSaturation``` - nastavuje transparentnosť vodotlače vo výslednom obrázku. Číslo 0-100, 0 znamená úplnú priesvitnosť, 100 nepriesvitnosť.

## Možné konfiguračné premenné

- ```galleryEnableWatermarking``` (predvolene true) - Vypne / zapne vodotlač pre obrázky. Vodotlač môže výrazne spomaľovať veľké importy obrázkov kvôli rekurzívnemu hľadaniu nastavenia vodotlače.
- ```galleryWatermarkSaturation``` (predvolene 70) - nastavuje transparentnosť vodotlače vo výslednom obrázku. Číslo 0-100, 0 znamená úplnú priesvitnosť, 100 nepriesvitnosť.
- ```galleryWatermarkGravity``` (predvolene Center) - Pozícia vodotlače vo výslednom obrázku. Možnosti podľa svetových strán v angličtine: ```NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast```.
- ```galleryWatermarkApplyOnUpload``` (predvolene false) - Po nastavení na ```true``` aktivuje automatické aplikovanie vodotlače pri nahratí obrázkov, vodotlač sa teda aplikuje aj na originálne obrázky v galérii.
- ```galleryWatermarkApplyOnUploadDir``` (predvolene /templates/{INSTALL_NAME}/assets/watermark/) - Adresár kde sú umiestnené obrázky pre automatickú vodotlač pri nahratí obrázku. Názov obrázku musí byť ```default.png```, pri multidomain je možnosť mať pre každú doménu iný, v tvare ```domena.png``` (napr. ```www.interway.sk.png```).


