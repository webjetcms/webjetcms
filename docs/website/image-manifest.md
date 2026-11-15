# Pôvod obrázkov

Balík obsahuje 15 statických obrázkov. Aktuálne HTML používa deväť súborov: `opravnenia.png`, `sprava-dokumentov.png`, `headless-web.png` a všetkých šesť log. Tie nahrajte z `sk/images/` do `/images/webjetcms-2026/`.

Zvyšných šesť snímok (`ai-pagebuilder.png`, `ai-asistenti.png`, `pagebuilder.png`, `historia-publikovania.png`, `viackrokovy-formular.png`, `vlastna-aplikacia.png`) zostáva ako voliteľná statická alternatíva k [videám](video-manifest.md). Aktuálne HTML ich nenačítava; nejde o automatický fallback prehrávača.

Deväť snímok je prevzatých bez úprav z dokumentácie; šesť log bez úprav zo súčasného verejného webu. Originálne pomery strán zostávajú zachované. Pri snímkach z dokumentácie bola overená aj binárna zhoda so zdrojom.

| Obrázok | Rozmery | Veľkosť | Zdroj |
| --- | --- | --- | --- |
| [ai-pagebuilder.png](sk/images/ai-pagebuilder.png) | 1440 × 1000 | 234.6 KiB | [Originál](../sk/redactor/ai/datatables/pb-chat-preview.png) |
| [ai-asistenti.png](sk/images/ai-asistenti.png) | 1280 × 850 | 226.0 KiB | [Originál](../sk/redactor/ai/datatables/ckeditor-assistants.png) |
| [pagebuilder.png](sk/images/pagebuilder.png) | 1280 × 760 | 303.5 KiB | [Originál](../sk/redactor/webpages/pagebuilder-structure.png) |
| [historia-publikovania.png](sk/images/historia-publikovania.png) | 1280 × 760 | 150.3 KiB | [Originál](../sk/redactor/webpages/history.png) |
| [opravnenia.png](sk/images/opravnenia.png) | 900 × 492 | 51.5 KiB | [Originál](../sk/admin/users/permissiongroups-editor-dirs.png) |
| [sprava-dokumentov.png](sk/images/sprava-dokumentov.png) | 1400 × 850 | 144.4 KiB | [Originál](../sk/redactor/files/file-archive/datatable.png) |
| [viackrokovy-formular.png](sk/images/viackrokovy-formular.png) | 1360 × 720 | 112.7 KiB | [Originál](../sk/redactor/apps/multistep-form/real-form.png) |
| [vlastna-aplikacia.png](sk/images/vlastna-aplikacia.png) | 1280 × 760 | 252.9 KiB | [Originál](../sk/custom-apps/appstore/appstore.png) |
| [headless-web.png](sk/images/headless-web.png) | 1280 × 720 | 98.9 KiB | [Originál](../sk/frontend/headless/home.png) |
| [logo-mfsr.png](sk/images/logo-mfsr.png) | 398 × 186 | 4.5 KiB | [Originál](https://www.webjetcms.sk/templates/interway/webjet_products/assets/images/references/mfsr.png) |
| [logo-vszp.png](sk/images/logo-vszp.png) | 100 × 186 | 2.7 KiB | [Originál](https://www.webjetcms.sk/templates/interway/webjet_products/assets/images/references/vszp.png) |
| [logo-nn.png](sk/images/logo-nn.png) | 234 × 162 | 10.4 KiB | [Originál](https://www.webjetcms.sk/images/webjet-cms-new/nn_poistovna_logo.png) |
| [logo-vub.png](sk/images/logo-vub.png) | 187 × 186 | 4.3 KiB | [Originál](https://www.webjetcms.sk/templates/interway/webjet_products/assets/images/references/vub-banka.png) |
| [logo-uniqa.png](sk/images/logo-uniqa.png) | 58 × 58 | 0.5 KiB | [Originál](https://www.webjetcms.sk/images/webjetcmsnew/image-2021-02-02-12-53-55-588.png) |
| [logo-tatrabanka.png](sk/images/logo-tatrabanka.png) | 67 × 67 | 4.2 KiB | [Originál](https://www.webjetcms.sk/images/webjetcmsnew/image-2021-02-02-12-53-55-478.png) |

## Použitie

Snímky dokumentujú skutočné rozhranie; ukážkové názvy a obsah v nich zostali zachované. Pri zostávajúcich snímkach sa po kliknutí otvorí pôvodná plná veľkosť. Načítavajú sa odložene. Obrázky neobsahujú doplnené či vygenerované funkcie.

Logá UNIQA a Tatra banky majú malé pôvodné rozlíšenie. HTML ich neškáluje nad prirodzenú veľkosť. Pri budúcej výmene možno použiť kvalitnejšie schválené podklady značiek; nové logá sa teraz nevytvárali.

Náhľady živých noviniek sa načítavajú priamo z `https://docs.webjetcms.sk/json/social-media/images/` podľa JSON. Nezahŕňajú sa do statického balíka, aby sa aktualizovali spolu s novými príspevkami. Ich pomer 16 : 9 používa `object-fit: contain`, takže obsah náhľadu nie je orezaný.
