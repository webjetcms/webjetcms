# Nastavenie vodoznaku

Vkladanie vodoznaku vyžaduje nasledovné nastavenie servera:

- Na serveri dostupná knižnica [ImageMagick](https://imagemagick.org/script/download.php) konkrétne príkazy ```convert``` a ```composite```.
- Nastavená konfiguračná premenná ```imageMagickDir``` na adresár s inštaláciou ```ImageMagick``` (typicky ```/usr/bin```).
- Povolené vkladanie vodoznaku nastavením konfiguračnej premennej ```galleryEnableWatermarking``` na hodnotu ```true```.

Až po splnení nastavení servera sa začne vkladať nastavený vodoznak do obrázka.

![](watermark-applied.png)

Podporované sú nasledovné formáty obrázkov vodoznaku:

- ```png``` - obrázok je vložený do nahratého obrázku bez zmeny veľkosti, je preto potrebné vhodne nastaviť veľkosť vodoznaku (uvedomte si, že fotogaléria obsahuje malý aj veľký obrázok a do oboch bude vložený vodoznak rovnakej veľkosti).
- ```svg``` - vodoznak je do obrázku vkladaný s korektným škálovaním veľkosti podľa veľkosti nahratého obrázku. Veľkosť vodoznaku sa nastavuje v konfiguračnej premennej ```galleryWatermarkSvgSizePercent``` (štandardne 5 percent) a ```galleryWatermarkSvgMinHeight``` (štandardne 30 bodov). Veľkosť vodoznaku sa teda automaticky prispôsobí veľkosti obrázku. Ukážkový [svg vodoznak](watermark.svg).

!>**Upozornenie:** uvedomte si, že v galérii sa vodoznak nevkladá do originálneho obrázku, je to tak z dôvodu možnosti pregenerovania existujúcich obrázkov (napr. zmena veľkosti, alebo zmena vodoznaku). Originálny obrázok je dostupný s prefixom ```o_``` a teda je možné ho takto verejne získať. Ak nutne potrebujete mať vodoznak vo všetkých obrázkoch je potrebné nastaviť automatické aplikovanie vodoznaku po nahratí obrázka nastavením konf. premennej `galleryWatermarkApplyOnUpload` na hodnotu `true`.

## Automatické aplikovanie vodoznaku po nahratí obrázku

Automatické vkladanie vodoznaku do fotiek pri ich nahratí do WebJETu zapnete nastavením konfiguračnej premennej ```galleryWatermarkApplyOnUpload``` na ```true```. Vodoznak sa vkladá do nahratej fotky, aby nenastávala nasledovne duplicita jeho vloženia pri zmene veľkosti fotky a podobne.

Výnimky, pre ktoré sa vodoznak pri nahratí súboru neaplikuje je možné definovať v konfiguračnej premennej ```galleryWatermarkApplyOnUploadExceptions``` (štandardne nastavené na logo,nowatermark,system) kde sú výrazy, ktoré keď sa nájdu v ceste k obrázku (meno obrázku alebo názov adresára), tak sa vodoznak neaplikuje.

Vodoznak sa pri tomto režime vkladá rovnaký pre celý web (nie je možné špecifikovať rôzne vodoznaky), pre multidomain inštaláciu je možné nastaviť obrázok pre každú doménu. Nastavuje sa v konfiguračnej premennej ```galleryWatermarkApplyOnUploadDir``` (predvolene /templates/{INSTALL_NAME}/assets/watermark/) - adresár kde sú umiestnené obrázky pre automatickú vodotlač pri nahratí obrázku. Názov obrázku musí byť ```default.png```, pri multidomain je možnosť mať pre každú doménu iný, v tvare ```domena.png``` (napr. ```www.interway.sk.png```).

Pozíciu vodoznaku nastavujete v konfiguračnej premennej ```galleryWatermarkGravity``` (prednastavené na Center). Možnosti podľa svetových strán v angličtine: ```NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast```. Priesvitnosť vodotlače môžete nastaviť v premennej ```galleryWatermarkSaturation``` - nastavuje transparentnosť vodotlače vo výslednom obrázku. Číslo 0-100, 0 znamená úplnú priesvitnosť, 100 nepriesvitnosť.

## Možné konfiguračné premenné

- ```galleryEnableWatermarking``` (predvolene true) - Vypne / zapne vodotlač pre obrázky. Vodotlač môže výrazne spomaľovať veľké importy obrázkov kvôli rekurzívnemu hľadaniu nastavenia vodotlače.
- ```galleryWatermarkSaturation``` (predvolene 70) - nastavuje transparentnosť vodotlače vo výslednom obrázku. Číslo 0-100, 0 znamená úplnú priesvitnosť, 100 nepriesvitnosť.
- ```galleryWatermarkGravity``` (predvolene Center) - Pozícia vodotlače vo výslednom obrázku. Možnosti podľa svetových strán v angličtine: ```NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast```.
- ```galleryWatermarkApplyOnUpload``` (predvolene false) - Po nastavení na ```true``` aktivuje automatické aplikovanie vodotlače pri nahratí obrázkov, vodoznak sa teda aplikuje aj na originálne obrázky v galérii.
- ```galleryWatermarkApplyOnUploadDir``` (predvolene /templates/{INSTALL_NAME}/assets/watermark/) - Adresár kde sú umiestnené obrázky pre automatickú vodotlač pri nahratí obrázku. Názov obrázku musí byť ```default.png```, pri multidomain je možnosť mať pre každú doménu iný, v tvare ```domena.png``` (napr. ```www.interway.sk.png```).


