# Video

Pridajte na svoju stránku pútavé video obsahy z YouTube, Vimeo, Facebook alebo nahratých mp4 súborov. S možnosťou nastavenia veľkosti a vlastností prehrávania, môžete zaujať a angažovať svojich návštevníkov.

## Nastavenia aplikácie

### Zdroj videa

V tejto časti je možné vybrať z dostupných zdrojov videa:

- YouTube
- Vimeo
- Facebook
- Video na serveri

![](editor-source.png)

### YouTube

YouTube video parametre:

- **Adresa stránky YouTube**, stačí jednoducho vložiť odkaz na web stránku s videom
- Fixná veľkosť (v bodoch)
  - Šírka
  - Výška
- Responzívna veľkosť (v percentách)
  - Šírka (%)
- Pomer strán videa - výber pomeru strán pre responzívne zobrazenie videa (napr. 16:9, 4:3, 1:1, 21:9, 9:16)
- Prehrať video po načítaní stránky
- Zobraziť názov videa
- Zobraziť YouTube logo
- Zobraziť možnosť prechodu na plnú obrazovku
- Zobraziť ovládacie ikony
- Zobraziť podobné videá po skončení prehrávania

![](editor-youtube.png)

### Vimeo

Vimeo video parametre:

- **Adresa stránky Vimeo**, stačí jednoducho vložiť odkaz na web stránku s videom
- Fixná veľkosť (v bodoch)
  - Šírka
  - Výška
- Responzívna veľkosť (v percentách)
  - Šírka (%)
- Pomer strán videa - výber pomeru strán pre responzívne zobrazenie videa (napr. 16:9, 4:3, 1:1, 21:9, 9:16)
- Prehrať video po načítaní stránky
- Zobraziť názov videa
- Zobraziť autorov text na videu
- Zobraziť možnosť prechodu na plnú obrazovku
- Zobraziť fotku autora na videu
- Povoliť zobrazenie vodoznaku na videu

![](editor-vimeo.png)

### Facebook

Facebook video parametre:

- **Adresa stránky s videom na facebook.com**, stačí jednoducho vložiť odkaz na web stránku s videom
- Fixná veľkosť (v bodoch)
  - Šírka
- Responzívna veľkosť (na celú šírku bloku)
- Pomer strán videa - výber pomeru strán pre responzívne zobrazenie videa (napr. 16:9, 4:3, 1:1, 21:9, 9:16)
- Prehrať video po načítaní stránky
- Zobraziť názov videa
- Zobraziť autorov text na videu
- Zobraziť možnosť prechodu na plnú obrazovku

![](editor-facebook.png)

### Video

Serverové video parametre:

- **Umiestnenie video súboru na serveri**, výber videa pomocou prieskumníka súborov (podporované je aj priame zadanie cesty k súboru)
- Fixná veľkosť (v bodoch)
  - Šírka
  - Výška
- Responzívna veľkosť (v percentách)
  - Šírka (%)
- Pomer strán videa - výber pomeru strán pre responzívne zobrazenie videa (napr. 16:9, 4:3, 1:1, 21:9, 9:16)

![](editor-video.png)

## Konfigurácia

Správa zobrazenia videa sa dá prispôsobiť nasledujúcimi konfiguračnými premennými:

| Premenná | Popis | Predvolená hodnota |
| --- | --- | --- |
| `videoClasses` | Čiarkou oddelený zoznam dostupných pomerov strán v editore. Formát: `prekladový_kľúč:css_triedy` alebo len `css_triedy`. Prvá položka je predvolená hodnota. | 16:9, 4:3, 1:1, 21:9, 9:16 (Bootstrap 4+5 triedy) |
| `videoWrapperClass` | CSS trieda pre obaľovací element videa. Pre Bootstrap použite `embed-responsive`. | `embed-responsive` |
| `videoItemClass` | CSS trieda pre vnútorný `<iframe>` element videa. Pre Bootstrap použite `embed-responsive-item`. | `embed-responsive-item` |

Príklad zmeny na vlastné CSS triedy (bez Bootstrap):

```txt
videoClasses=components.video_player.ratio-16x9:video-wrapper-16x9,components.video_player.ratio-4x3:video-wrapper-4x3
videoWrapperClass=video-wrapper
videoItemClass=video-item
```

Ak potrebujete nastavovať CSS triedu aj pre obaľovač aj pre samotný `iframe` element s videom, je možné rozdeliť CSS triedy znakom `|`, hodnota pred týmto znakom sa použije na obaľovač a za znakom pre `iframe` element. Príklad:

```txt
videoClasses=components.video_player.ratio-16x9:video-wrapper-16x9|video-iframe-16x9,components.video_player.ratio-4x3:video-wrapper-4x3|video-iframe-4x3
videoWrapperClass=video-wrapper
videoItemClass=video-item
```

## Zobrazenie aplikácie

![](video.png)