# Generovanie náhľadových obrázkov vo WebJETe

WebJET umožňuje na požiadavku generovať obrázky zadanej veľkosti z ľubovoľného obrázku z priečinkov `/images,/files,/shared,/video,/templates`.

## Základné generovanie

Predstavme si, že máme obrázok (nemusí byť z galérie, je to len ukážka):

`/images/gallery/test-vela-foto/dsc04131.jpg`

![Original Image](original-image.png)

a potrebujeme, aby sme ho mali v max rozmere `200x200` bodov. Pred URL obrázku stačí pridať prefix `/thumb` a pridať URL parametre `w` a `h` s požadovaným rozmer, čiže:

`/thumb/images/gallery/test-vela-foto/dsc04131.jpg?w=200&h=200`

![Thumb Image 200x200](thumb-image.png)

Obrázok reálne môže byť menší ako požadovaných `200x200` bodov, záleží od jeho pomeru strán. V tomto prípade sa vygeneroval ako `200x134` bodov, vždy sa ale zmestí do požadovaného rozmeru.

## Obrázky AVIF

Nahraný obrázok AVIF môžete vložiť do stránky, vybrať ako perexový obrázok a používať v obrázkových poliach, správcovi súborov aj galérii. WebJET načíta jeho rozmery bez dekódovania obrázka. Existujúce JPG, PNG ani WebP sa automaticky nekonvertujú.

Generovanie miniatúr a úpravy AVIF vyžadujú na serveri **ImageMagick s podporou čítania aj zápisu AVIF vrátane priehľadnosti** (s príslušným `libheif` a AV1 kodekom). V konfigurácii `imageMagickDir` nastavte priečinok s príkazom `magick`, prípadne `convert`. Podporu musí zabezpečiť správca servera; WebJET pri spracovaní predpokladá funkčný kodek.

Pred nasadením overte pod účtom, pod ktorým beží WebJET:

- príkaz `magick -list format` (pri ImageMagick 6 `convert -list format`) uvádza pri AVIF čítanie aj zápis,
- prevod priehľadného PNG do AVIF a späť funguje a zachová priehľadnosť,
- proces má právo spustiť ImageMagick a zapisovať do dočasného priečinka Javy aj cieľového priečinka obrázkov.

Napríklad v dočasnom pracovnom priečinku spustite:

```sh
magick -size 8x8 xc:none -fill red -draw "point 4,4" avif-check.avif
magick avif-check.avif avif-check.png
```

Výsledné PNG musí mať priehľadné pozadie a červený bod. Pri ImageMagick 6 nahraďte `magick` príkazom `convert`.

Napríklad miniatúra `/thumb/images/fotografia.avif?w=300&h=200&ip=5` zostane vo formáte AVIF s MIME typom `image/avif`. Zmenšenie, orezanie, otočenie aj uloženie v editore obrázkov zachovávajú priehľadnosť. Predvolené stratové kódovanie môže mierne zmeniť farby a hodnoty polopriehľadných pixelov.

Pri zmenšovaní, orezaní alebo otočení animovaného AVIF vznikne statický obrázok z prvého snímku. Ak sa pri generovaní miniatúry alebo verzie v galérii použije iba kópia menšieho originálu bez zmeny rozmerov, celý súbor vrátane animácie sa zachová pri vypnutom odstraňovaní metadát (`galleryStripExif=false`). Pri zapnutom odstraňovaní metadát sa aj táto kópia prekóduje na statický obrázok. Nahraný originál sa pri generovaní miniatúr nemení; v galérii sa uchová v súbore s prefixom `o_`. Pri úpravách sa používa bežný SDR výstup, bez záruky zachovania HDR a pôvodnej 10/12-bitovej hĺbky. Výslovné uloženie úpravy obrázka nahradí upravovaný súbor statickou verziou. Automatická vodotlač pri nahrávaní (`galleryWatermarkApplyOnUpload`) sa na AVIF originál neaplikuje.

Metóda `GalleryDB.stripExif(filePath)` rešpektuje `galleryStripExif` aj pre AVIF a pri zapnutí odstráni metadáta priamo zo zadaného súboru. Platí to aj pri volaní z vlastného kódu; ak jej odovzdáte originál, prekóduje originál.

Kvalitu a rýchlosť kódovania nastavíte existujúcim mechanizmom [vlastných parametrov ImageMagick](../../redactor/apps/gallery/README.md#vlastné-parametre-imagemagick). Predvolená hodnota `imageMagickCustomParams_avif` je `-quality 80 -define heic:speed=6`. Platia aj špecifickejšie nastavenia pre operáciu, napríklad `imageMagickCustomParams_resize_avif`.

Pri zlyhaní úpravy sa zobrazí spoločná chyba podľa výsledku spracovania, rovnako ako pri ostatných formátoch. Pôvodný AVIF sa nahradí až po úspešnom vytvorení upravenej verzie. Pri neúspešnom generovaní miniatúry sa vráti náhradný obrázok `big_unknown.gif` s HTTP `200`, MIME typom `image/gif` a hlavičkou `Cache-Control: no-store`. Toto správanie platí pre všetky formáty. Chyba sa zapíše do logu; náhradná odpoveď sa necachuje, aby ďalšia požiadavka po odstránení príčiny mohla vygenerovať miniatúru. Už vygenerované platné miniatúry zostávajú dostupné.

AVIF je doplnený do predvolených zoznamov prípon. **Vlastné uložené obmedzenia nahrávania sa nemenia.** Ak používate vlastný zoznam, správca musí doplniť `avif` do príslušných nastavení `FCKConfig.UploadFileTypes[Basic][image]`, `FCKConfig.UploadFileTypes[Default][image]` alebo `xhrFileUploadAllowedExtensions`, prípadne do obmedzení konkrétneho nahrávacieho poľa. Toto rozšírenie nemení formáty prijímané externými AI službami.

## Predvolený obrázok

Predvolene sa pre neexistujúci obrázok pri použití `/thumb` adresy vráti štandardná chyba 404. Ak ale potrebujete pre taký prípad zobraziť predvolený obrázok, je možné použiť konfiguračnú premennú `thumbServletMissingImg`. Do nej je možné do riadkov doplniť meno priečinku a meno súboru, ktorý sa pre tento prípad má použiť. Príklad nastavenia:

```txt
/images/gallery/test/|/images/photo3.jpg
/images/|/images/photo1.jpg
```

Nastavenie podľa uvedeného formátu pre neexistujúce obrázky z priečinka `/images/gallery/test/` a jeho pod-adresárov zobrazí obrázok `/images/photo3.jpg`. Pre volanie obrázka z priečinka `/images/test/podadresar/` sa zobrazí obrázok `/images/photo1.jpg`, pretože najlepšia zhoda bude práve s `/images` adresárom. Pri volaní `/templates/meno/assets/image.jpg` sa zobrazí štandardná chyba 404, keďže v konfiguračnej premennej nie je definovaný žiadny prefix pre tento priečinok.

Nájdený obrázok prejde procesom cez `/thumb`, takže je vygenerovaný v zadanom rozmere z URL parametrov.

## Obmedzenia

Generovanie obrázkov zaťažuje server, je teda chránené SPAM ochranou. Používajú sa nasledovné konf. premenné:

- `spamProtectionTimeout-ThumbServlet` - čas medzi HTTP požiadavkami, nastavené na hodnotu `-2` pre vypnutie, keďže na stránke môže byť viacero obrázkov, ktoré sa generujú naraz.
- `spamProtectionHourlyLimit-ThumbServlet` - maximálny počet vygenerovaných obrázkov z jednej IP adresy za hodinu, predvolene nastavené na hodnotu `300`.
- `cloudCloneAllowedIps` - zoznam začiatkov IP adries oddelených čiarkou pre ktoré sa obmedzenie nebude aplikovať, predvolene prázdne (nepoužije sa).
- `thumbServletAllowedSizes` - zoznam povolených rozmerov súborov pre generovanie obrázka. Generuje sa vo formáte `{width}x{height}ip{ip}{noip}c{color}q{quality}`, napríklad `730x401ip5ncff00ffq90`. Parametre ktoré nie sú zadané nie su použité, napríklad `430x405` alebo `730x404ip5`. Odporúčame použiť režim `learn` na prvotné nastavenie a následne nastaviť režim `check`. Ak používate `ip=1/ip=2` bez `h/w` parametra použite pre povolenie rovnakú hodnotu, napríklad pre `/thumb/images/image.jpg?ip=1&w=610` nastavte `610x610ip1`.
- `thumbServletAllowedSizeMode` - Nastavuje režim povolených rozmerov pre generovanie obrázka. Možné hodnoty:
  - `deny` - zakáže generovanie nových obrázkov ak nie je prihlásený administrátor (existujúce obrázky sa budú zobrazovať, keďže už sú na disku vygenerované)
  - `allow` - povolí všetky rozmery, ani nekontroluje zoznam povolených možností
  - `learn` - pridá hodnotu do zoznamu `thumbServletAllowedSizes` (ak už tam nie je) - režim učenia sa existujúcich hodnôt
  - `check` - povolí len zadané hodnoty, ak je prihlásený administrátor, automaticky pridá nový rozmer do zoznamu
  - `strict` - povolí vygenerovať obrázok len pre zadané hodnoty (kontroluje zoznam povolených hodnôt)

Karta **Miniatúra** vo vlastnostiach obrázka sa riadi nastavením `thumbServletAllowedSizeMode`:

| Režim | Zadané povolené rozmery | Prázdny zoznam |
| --- | --- | --- |
| `strict` | Výber povoleného rozmeru alebo **Nepoužiť miniatúru** | Len **Nepoužiť miniatúru** |
| `learn`, `check`, `allow` | Výber rozmeru, **Nepoužiť miniatúru** alebo **Zadať vlastný rozmer…** | Pôvodné polia na zadanie rozmerov |
| `deny` | Karta je skrytá | Karta je skrytá |

Možnosti sa načítajú z `thumbServletAllowedSizes` (oddelené čiarkou alebo novým riadkom), odstránia sa duplicity a zoradia sa číselne podľa šírky a výšky. Napríklad `96x96ip2` sa zobrazí ako **96 px – Fixná výška (ip2)**. Názov režimu používa rovnaký preklad ako pole **Režim**. Hodnota bez `ip` sa zobrazí ako maximálne rozmery `(ip0)`. Farba pozadia sa zobrazí hexadecimálne; vypnutie bodu záujmu a kvalita sa doplnia do popisu.

Vybraná kombinácia sa uloží do URL obrázka aj CSS triedy `fixedSize`. Kvalita sa zapisuje príponou `-q90`, napríklad `fixedSize-730-401-5-ff00ff-true-q90`. Pri opätovnom otvorení sa vyberie zodpovedajúca možnosť. Ak existujúci rozmer nie je v zozname, v režime `strict` sa zobrazí upozornenie a treba vybrať povolený rozmer alebo **Nepoužiť miniatúru**. V ostatných režimoch sa otvorí vlastné nastavenie s pôvodnými hodnotami. V režime `deny` potvrdenie dialógu zachová pôvodnú miniatúru.

Voľba **Nepoužiť miniatúru** má prázdnu hodnotu a použije pôvodný obrázok: odstráni prefix `/thumb/`, parametre miniatúry a triedu `fixedSize`.

Nastavte najskôr režim `learn` pre naučenie sa existujúcich hodnôt a následne nastavte režim `check` v ktorom sa hodnoty kontrolujú, ale ak je prihlásený administrátor automaticky sa pridá nová hodnota do zoznamu povolených hodnôt. WebJET automaticky pri aktualizácii nastaví režim `learn` a následne po minimálne mesiaci a ďalšom reštarte prepne na režim `check`.

V prípade viac uzlovej inštalácie môžete nastaviť režim `deny` a generovať náhľadové obrázky len na administrátorských uzloch. Rozdiel medzi `deny` a `check` je v tom, že `deny` ani nekontroluje zoznam povolených možností a je teda rýchlejší na spracovanie. Zároveň ak je prihlásený administrátor obrázok je vygenerovaný.

Je potrebné si uvedomiť, že režim `thumbServletAllowedSizeMode` sa kontroluje len v prípade, že požadovaná veľkosť ešte nie je vygenerovaná v priečinku `/WEB-INF/imgcache`, ak súbor už existuje je zobrazený bez ohľadu na nastavenie tejto konfiguračnej premennej. Je to tak z dôvodu výkonu, keďže kontrola povolených možností je náročnejšia.
