# Nastavení vodoznaku

Vkládání vodoznaku vyžaduje následující nastavení serveru:
- Na serveru dostupná knihovna [ImageMagick](https://imagemagick.org/script/download.php) konkrétní příkazy `convert` a `composite`.
- Nastavená konfigurační proměnná `imageMagickDir` na adresář s instalací `ImageMagick` (typicky `/usr/bin`).
- Povolené vkládání vodoznaku nastavením konfigurační proměnné `galleryEnableWatermarking` na hodnotu `true`.

Teprve po splnění nastavení serveru se začne vkládat nastavený vodoznak do obrázku.

![](watermark-applied.png)

Podporovány jsou následující formáty obrázků vodoznaku:
- `png` - obrázek je vložen do nahraného obrázku beze změny velikosti, je proto třeba vhodně nastavit velikost vodoznaku (uvědomte si, že fotogalerie obsahuje malý i velký obrázek a do obou bude vložen vodoznak stejné velikosti).
- `svg` - vodoznak je do obrázku vkládán s korektním škálováním velikosti podle velikosti nahraného obrázku. Velikost vodoznaku se nastavuje v konfigurační proměnné `galleryWatermarkSvgSizePercent` (standardně 5 procent) a `galleryWatermarkSvgMinHeight` (standardně 30 bodů). Velikost vodoznaku se tedy automaticky přizpůsobí velikosti obrázku. Ukázkový [svg vodoznak](watermark.svg).

!>**Upozornění:** uvědomte si, že v galerii se vodoznak nevkládá do originálního obrázku, je to tak z důvodu možnosti přegenerování stávajících obrázků (např. změna velikosti nebo změna vodoznaku). Originální obrázek je dostupný s prefixem `o_` a tedy je možné jej takto veřejně získat. Pokud nutně potřebujete mít vodoznak ve všech obrázcích je třeba nastavit automatické aplikování vodoznaku po nahrání obrázku nastavením konf. proměnné `galleryWatermarkApplyOnUpload` na hodnotu `true`.

## Automatické aplikování vodoznaku po nahrání obrázku

Automatické vkládání vodoznaku do fotek při jejich nahrání do WebJETu zapnete nastavením konfigurační proměnné `galleryWatermarkApplyOnUpload` na `true`. Vodoznak se vkládá do nahrané fotky, aby nenastávala následovně duplicita jeho vložení při změně velikosti fotky a podobně.

Výjimky, pro které se vodoznak při nahrání souboru neaplikuje lze definovat v konfigurační proměnné `galleryWatermarkApplyOnUploadExceptions` (standardně nastaveno na logo,nowatermark,system) kde jsou výrazy, které když se najdou v cestě k obrázku (jméno obrázku nebo název adresáře), tak se vodoznak neaplikuje.

Vodoznak se při tomto režimu vkládá stejný pro celý web (nelze specifikovat různé vodoznaky), pro multidomain instalaci lze nastavit obrázek pro každou doménu. Nastavuje se v konfigurační proměnné `galleryWatermarkApplyOnUploadDir` (výchozí /templates/{INSTALL_NAME}/assets/watermark/) - adresář kde jsou umístěny obrázky pro automatický vodotisk při nahrání obrázku. Název obrázku musí být `default.png`, u multidomain je možnost mít pro každou doménu jiný, ve tvaru `domena.png` (Např. `www.interway.sk.png`).

Pozici vodoznaku nastavujete v konfigurační proměnné `galleryWatermarkGravity` (přednastaveno na Center). Možnosti podle světových stran v angličtině: `NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast`. Průsvitnost vodoznaku můžete nastavit v proměnné `galleryWatermarkSaturation` - nastavuje transparentnost vodotisku ve výsledném obrázku. Číslo 0-100, 0 znamená úplnou průsvitnost, 100 neprůsvitnost.

## Možné konfigurační proměnné

- `galleryEnableWatermarking` (výchozí true) - Vypne / zapne vodoznak pro obrázky. Vodoznak může výrazně zpomalovat velké importy obrázků kvůli rekurzivnímu hledání nastavení vodoznaku.
- `galleryWatermarkSaturation` (výchozí 70) - nastavuje transparentnost vodoznaku ve výsledném obrázku. Číslo 0-100, 0 znamená úplnou průsvitnost, 100 neprůsvitnost.
- `galleryWatermarkGravity` (výchozí Center) - Pozice vodoznaku ve výsledném obrázku. Možnosti podle světových stran v angličtině: `NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast`.
- `galleryWatermarkApplyOnUpload` (výchozí false) - Po nastavení na `true` aktivuje automatické aplikování vodotisku při nahrání obrázků, vodoznak se tedy aplikuje i na originální obrázky v galerii.
- `galleryWatermarkApplyOnUploadDir` (výchozí /templates/{INSTALL_NAME}/assets/watermark/) - Adresář kde jsou umístěny obrázky pro automatický vodotisk při nahrání obrázku. Název obrázku musí být `default.png`, u multidomain je možnost mít pro každou doménu jiný, ve tvaru `domena.png` (Např. `www.interway.sk.png`).
