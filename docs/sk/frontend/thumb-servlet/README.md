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
- `thumbServletAllowedSizes` - zoznam povolených rozmerov súborov pre generovanie obrázka. Generuje sa vo formáte `{width}x{height}ip{ip}{noip}c{color}q{quality}`, napríklad `730x401ip5ncff00ffq90`. Parametre ktoré nie sú zadané nie su použité, napríklad `430x405` alebo `730x404ip5`. Odporúčame použiť režim `learn` na prvotné nastavenie a následne nastaviť režim `check`.
- `thumbServletAllowedSizeMode` - Nastavuje režim povolených rozmerov pre generovanie obrázka. Možné hodnoty:
  - `deny` - zakáže generovanie nových obrázkov ak nie je prihlásený administrátor (existujúce obrázky sa budú zobrazovať, keďže už sú na disku vygenerované)
  - `allow` - povolí všetky rozmery, ani nekontroluje zoznam povolených možností
  - `learn` - pridá hodnotu do zoznamu `thumbServletAllowedSizes` (ak už tam nie je) - režim učenia sa existujúcich hodnôt
  - `check` - povolí len zadané hodnoty, ak je prihlásený administrátor, automaticky pridá nový rozmer do zoznamu
  - `strict` - povolí vygenerovať obrázok len pre zadané hodnoty (kontroluje zoznam povolených hodnôt)

Nastavte najskôr režim `learn` pre naučenie sa existujúcich hodnôt a následne nastavte režim `check` v ktorom sa hodnoty kontrolujú, ale ak je prihlásený administrátor automaticky sa pridá nová hodnota do zoznamu povolených hodnôt. WebJET automaticky pri aktualizácii nastaví režim `learn` a následne po minimálne mesiaci a ďalšom reštarte prepne na režim `check`.

V prípade viac uzlovej inštalácie môžete nastaviť režim `deny` a generovať náhľadové obrázky len na administrátorských uzloch. Rozdiel medzi `deny` a `check` je v tom, že `deny` ani nekontroluje zoznam povolených možností a je teda rýchlejší na spracovanie. Zároveň ak je prihlásený administrátor obrázok je vygenerovaný.

Je potrebné si uvedomiť, že režim `thumbServletAllowedSizeMode` sa kontroluje len v prípade, že požadovaná veľkosť ešte nie je vygenerovaná v priečinku `/WEB-INF/imgcache`, ak súbor už existuje je zobrazený bez ohľadu na nastavenie tejto konfiguračnej premennej. Je to tak z dôvodu výkonu, keďže kontrola povolených možností je náročnejšia.