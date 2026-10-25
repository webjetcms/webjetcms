# Generování náhledových obrázků ve WebJETu

WebJET umožňuje na požadavek generovat obrázky zadané velikosti z libovolného obrázku ze složek `/images,/files,/shared,/video,/templates`.

## Základní generování

Představme si, že máme obrázek (nemusí být z galerie, je to jen ukázka):

`/images/gallery/test-vela-foto/dsc04131.jpg`

![Original Image](original-image.png)

a potřebujeme, abychom ho měli v max. rozměru `200x200` bodů. Před URL obrázku stačí přidat prefix `/thumb` a přidat URL parametry `w` a `h` s požadovaným rozměr, tedy:

`/thumb/images/gallery/test-vela-foto/dsc04131.jpg?w=200&h=200`

![Thumb Image 200x200](thumb-image.png)

Obrázek reálně může být menší než požadovaných `200x200` bodů, záleží na jeho poměru stran. V tomto případě se vygeneroval jako `200x134` bodů, vždy se ale vejde do požadovaného rozměru.

## Obrázky AVIF

Generování miniatur a úpravy AVIF vyžadují na serveru **ImageMagick s podporou čtení i zápisu AVIF včetně průhlednosti** (s příslušným `libheif` a AV1 kodekem). V konfiguraci `imageMagickDir` nastavte složku s příkazem `magick`, případně `convert`. Podporu musí zajistit správce serveru. WebJET při zpracování předpokládá funkční kodek.

Před nasazením ověřte pod účtem, pod kterým běží WebJET:

- příkaz `magick -list format` (u ImageMagick 6 `convert -list format`) uvádí u AVIF čtení i zápis,
- převod průhledného PNG do AVIF a zpět funguje a zachová průhlednost,
- proces má právo spustit ImageMagick a zapisovat do dočasné složky Javy i cílové složky obrázků.

Při zmenšování, oříznutí nebo otočení animovaného AVIF vznikne statický obrázek z prvního snímku. Při úpravách se používá běžný SDR výstup, bez záruky zachování HDR a původní 10/12bitové hloubky. Automatický vodoznak při nahrávání (`galleryWatermarkApplyOnUpload`) se aplikuje i na originál AVIF; při jejím vložení se nezaručuje zachování animace.

Kvalitu a rychlost kódování nastavíte stávajícím mechanismem [vlastních parametrů ImageMagick](../../redactor/apps/gallery/README.md#vlastní-parametry-imagemagick). Výchozí hodnota `imageMagickCustomParams_avif` je `-quality 80 -define heic:speed=6`. Platí také specifičtější nastavení pro operaci, například `imageMagickCustomParams_resize_avif`.

## Výchozí obrázek

Ve výchozím nastavení se pro neexistující obrázek při použití `/thumb` adresy vrátí standardní chyba 404. Pokud ale potřebujete pro takový případ zobrazit výchozí obrázek, je možné použít konfigurační proměnnou `thumbServletMissingImg`. Do ní lze do řádků doplnit jméno složky a jméno souboru, který se pro tento případ má použít. Příklad nastavení:

```txt
/images/gallery/test/|/images/photo3.jpg
/images/|/images/photo1.jpg
```

Nastavení podle uvedeného formátu pro neexistující obrázky ze složky `/images/gallery/test/` a jeho pod-adresářů zobrazí obrázek `/images/photo3.jpg`. Pro volání obrázku ze složky `/images/test/podadresar/` se zobrazí obrázek `/images/photo1.jpg`, protože nejlepší shoda bude právě s `/images` adresářem. Při volání `/templates/meno/assets/image.jpg` se zobrazí standardní chyba 404, protože v konfigurační proměnné není definován žádný prefix pro tuto složku.

Nalezený obrázek projde procesem přes `/thumb`, takže je vygenerován v zadaném rozměru z URL parametrů.

## Omezení

Generování obrázků zatěžuje server, je tedy chráněno SPAM ochranou. Používají se následující konf. proměnné:

- `spamProtectionTimeout-ThumbServlet` - ​​čas mezi HTTP požadavky, nastavené na hodnotu `-2` pro vypnutí, protože na stránce může být více obrázků, které se generují najednou.
- `spamProtectionHourlyLimit-ThumbServlet` - ​​maximální počet vygenerovaných obrázků z jedné IP adresy za hodinu, ve výchozím nastavení na hodnotu `300`.
- `cloudCloneAllowedIps` - ​​seznam začátků IP adres oddělených čárkou pro které se omezení nebude aplikovat, ve výchozím stavu prázdné (nepoužije se).
- `thumbServletAllowedSizes` - ​​seznam povolených rozměrů souborů pro generování obrázku. Generuje se ve formátu `{width}x{height}ip{ip}{noip}c{color}q{quality}`, například `730x401ip5ncff00ffq90`. Parametry, které nejsou zadány nejsou použity, například `430x405` nebo `730x404ip5`. Doporučujeme použít režim `learn` pro prvotní nastavení a následně nastavit režim `check`. Pokud používáte `ip=1/ip=2` bez `h/w` parametru použijte pro povolení stejnou hodnotu, například pro `/thumb/images/image.jpg?ip=1&w=610` nastavte `610x610ip1`.
- `thumbServletAllowedSizeMode` - ​​Nastavuje režim povolených rozměrů pro generování obrázku. Možné hodnoty:
  - `deny` - ​​zakáže generování nových obrázků pokud není přihlášen administrátor (existující obrázky se budou zobrazovat, protože již jsou na disku vygenerovány)
  - `allow` - ​​povolí všechny rozměry, ani nekontroluje seznam povolených možností
  - `learn` - ​​přidá hodnotu do seznamu `thumbServletAllowedSizes` (pokud již tam není) - režim učení se stávajících hodnot
  - `check` - ​​povolí pouze zadané hodnoty, pokud je přihlášen administrátor, automaticky přidá nový rozměr do seznamu
  - `strict` - ​​povolí vygenerovat obrázek pouze pro zadané hodnoty (kontroluje seznam povolených hodnot)

Karta **Miniatura** ve vlastnostech obrázku se řídí nastavením `thumbServletAllowedSizeMode`:

| Režim | Zadané povolené rozměry | Prázdný seznam |
| --- | --- | --- |
| `strict` | Výběr povoleného rozměru nebo **Nepoužít miniaturu** | Jen **Nepoužít miniaturu** |
| `learn`, `check`, `allow` | Výběr rozměru, **Nepoužít miniaturu** nebo **Zadat vlastní rozměr…** | Původní pole pro zadání rozměrů |
| `deny` | Karta je skryta | Karta je skryta |

Možnosti se načtou z `thumbServletAllowedSizes` (oddělené čárkou nebo novým řádkem), odstraní se duplicity a seřadí se číselně podle šířky a výšky. Například `96x96ip2` se zobrazí jako **96 px – Fixní výška (ip2)**. Název režimu používá stejný překlad jako pole **Režim**. Hodnota bez `ip` se zobrazí jako maximální rozměry `(ip0)`. Barva pozadí se zobrazí hexadecimálně; vypnutí bodu zájmu a kvalita se doplní do popisu.

Vybraná kombinace se uloží do URL obrázku i CSS třídy `fixedSize`. Kvalita se zapisuje příponou `-q90`, například `fixedSize-730-401-5-ff00ff-true-q90`. Při opětovném otevření se vybere odpovídající možnost. Pokud existující rozměr není v seznamu, v režimu `strict` se zobrazí upozornění a je třeba vybrat povolený rozměr nebo **Nepoužít miniaturu**. V ostatních režimech se otevře vlastní nastavení s původními hodnotami. V režimu `deny` potvrzení dialogu zachová původní miniaturu.

Volba **Nepoužít miniaturu** má prázdnou hodnotu a použije původní obrázek: odstraní prefix `/thumb/`, parametry miniatury a třídu `fixedSize`.

Nastavte nejprve režim `learn` pro naučení se existujících hodnot a následně nastavte režim `check` ve kterém se hodnoty kontrolují, ale pokud je přihlášen administrátor automaticky se přidá nová hodnota do seznamu povolených hodnot. WebJET automaticky při aktualizaci nastaví režim `learn` a následně po minimálně měsíci a dalším restartu přepne na režim `check`.

V případě více uzlové instalace můžete nastavit režim `deny` a generovat náhledové obrázky pouze na administrátorských uzlech. Rozdíl mezi `deny` a `check` je v tom, že `deny` ani nekontroluje seznam povolených možností a je tedy rychlejší ke zpracování. Zároveň je-li přihlášen administrátor obrázek je vygenerován.

Je třeba si uvědomit, že režim `thumbServletAllowedSizeMode` se kontroluje pouze v případě, že požadovaná velikost ještě není vygenerována ve složce `/WEB-INF/imgcache`, pokud soubor již existuje je zobrazen bez ohledu na nastavení této konfigurační proměnné. Je tomu tak z důvodu výkonu, jelikož kontrola povolených možností je náročnější.
