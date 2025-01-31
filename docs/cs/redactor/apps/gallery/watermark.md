# Nastavení vodoznaku

Vložení vodoznaku vyžaduje následující nastavení serveru:
- Knihovna dostupná na serveru [ImageMagick](https://imagemagick.org/script/download.php) specifické příkazy `convert` a `composite`.
- Nastavení konfigurační proměnné `imageMagickDir` do instalačního adresáře `ImageMagick` (typicky `/usr/bin`).
- Povolení vkládání vodoznaku nastavením konfigurační proměnné `galleryEnableWatermarking` na hodnotu `true`.

Teprve po splnění nastavení serveru se do obrázku začne vkládat nastavený vodoznak.

![](watermark-applied.png)

Podporovány jsou následující formáty obrázků s vodoznakem:
- `png` - obrázek je vložen do nahraného obrázku bez změny velikosti, takže je třeba nastavit vhodnou velikost vodoznaku (všimněte si, že fotogalerie obsahuje malý i velký obrázek a do obou bude vložen vodoznak stejné velikosti).
- `svg` - vodoznak je vložen do obrázku se správným měřítkem velikosti podle velikosti nahraného obrázku. Velikost vodoznaku se nastavuje v konfigurační proměnné `galleryWatermarkSvgSizePercent` (standardně 5 %) a `galleryWatermarkSvgMinHeight` (standardně 30 bodů). Velikost vodoznaku se proto automaticky přizpůsobí velikosti obrázku. Ukázka [vodoznak svg](watermark.svg).

!>**Varování:** Všimněte si, že v galerii není vodoznak vložen do původního obrázku, a to z důvodu možnosti opětovného generování stávajících obrázků (např. změna velikosti nebo vodoznaku). Původní obrázek je k dispozici s předponou `o_` a lze je tedy tímto způsobem veřejně získat. Pokud nutně potřebujete mít vodoznak na všech obrázcích, musíte nastavit automatické použití vodoznaku po nahrání obrázku nastavením proměnné conf. `galleryWatermarkApplyOnUpload` na hodnotu `true`.

## Automatické použití vodoznaku po nahrání obrázku

Nastavením konfigurační proměnné můžete povolit automatické označování fotografií vodoznakem při jejich nahrávání do WebJETu. `galleryWatermarkApplyOnUpload` na adrese `true`. Vodoznak je vložen do nahrané fotografie, aby se zabránilo duplicitnímu vložení při změně velikosti fotografie apod.

Výjimky, pro které se vodoznak při nahrávání souboru nepoužije, lze definovat v konfigurační proměnné. `galleryWatermarkApplyOnUploadExceptions` (ve výchozím nastavení nastaveno na logo,nowatermark,system), kde jsou výrazy, které se při nalezení v cestě k obrázku (název obrázku nebo adresáře) nepoužijí.

Vodoznak je v tomto režimu stejný pro celý web (není možné zadat různé vodoznaky), v případě instalace na více domén je možné nastavit obrázek pro každou doménu. Nastavuje se v konfigurační proměnné `galleryWatermarkApplyOnUploadDir` (výchozí /templates/{INSTALL_NAME}/assets/watermark/) - adresář, kam se umisťují obrázky pro automatické označení vodoznakem při nahrávání obrázku. Název obrázku musí být `default.png`, u multidomén je možné mít pro každou doménu jiný ve tvaru `domena.png` (např. `www.interway.sk.png`).

Polohu vodoznaku nastavíte v konfigurační proměnné `galleryWatermarkGravity` (přednastaveno na střed). Možnosti podle světových stran v angličtině: `NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast`. Průsvitnost tlaku vody můžete nastavit v proměnné `galleryWatermarkSaturation` - nastaví průhlednost vodoznaku ve výsledném obrázku. Číslo 0-100, 0 znamená plnou průhlednost, 100 znamená neprůhlednost.

## Možné konfigurační proměnné

- `galleryEnableWatermarking` (ve výchozím nastavení true) - Vypne/zapne vodoznak pro obrázky. Vodoznak může výrazně zpomalit import velkých obrázků kvůli rekurzivnímu vyhledávání nastavení vodoznaku.
- `galleryWatermarkSaturation` (výchozí 70) - nastavuje průhlednost vodoznaku ve výsledném obrázku. Číslo 0-100, 0 znamená plnou průhlednost, 100 znamená neprůhlednost.
- `galleryWatermarkGravity` (výchozí Střed) - Poloha vodoznaku ve výsledném obrázku. Možnosti podle světových stran v angličtině: `NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast`.
- `galleryWatermarkApplyOnUpload` (ve výchozím nastavení false) - Při nastavení na `true` aktivuje automatickou aplikaci vodoznaku při nahrávání obrázků, takže vodoznak se použije i na původní obrázky v galerii.
- `galleryWatermarkApplyOnUploadDir` (výchozí /templates/{INSTALL_NAME}/assets/watermark/) - Adresář, kam se umisťují obrázky pro automatické označení vodoznakem při nahrávání obrázku. Název obrázku musí být `default.png`, u multidomén je možné mít pro každou doménu jiný ve tvaru `domena.png` (např. `www.interway.sk.png`).
