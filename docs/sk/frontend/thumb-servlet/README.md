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

```
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