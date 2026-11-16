# Pôvod obrázkov

Balík obsahuje 24 statických obrázkov: deväť slovenských snímok, deväť anglických snímok a šesť spoločných log. SK HTML používa tri snímky (`opravnenia.png`, `sprava-dokumentov.png`, `headless-web.png`) a všetkých šesť log. EN HTML používa osem snímok a rovnaké logá; `ai-pagebuilder.png` zostáva voliteľnou alternatívou.

V SK verzii zostáva šesť snímok (`ai-pagebuilder.png`, `ai-asistenti.png`, `pagebuilder.png`, `historia-publikovania.png`, `viackrokovy-formular.png`, `vlastna-aplikacia.png`) ako voliteľná statická alternatíva k [videám](video-manifest.md). SK HTML ich nenačítava; nejde o automatický fallback prehrávača. EN verzia namiesto vložených videí používa snímky rozhrania z anglickej dokumentácie.

Osemnásť snímok je prevzatých bez úprav z dokumentácie; šesť log bez úprav zo súčasného verejného webu. Originálne pomery strán zostávajú zachované. Pri všetkých snímkach z dokumentácie bola overená binárna zhoda so zdrojom. Obrázky sa neprekladali prekreslením ani generovaním.

## Adresáre a nahratie na server

| Obsah | Zdroj v balíku | Cieľová cesta na webe |
| --- | --- | --- |
| Slovenské snímky | [sk/images/](sk/images/), deväť súborov bez prefixu `logo-` | `/images/webjetcms-2026/sk/` |
| Anglické snímky | [en/images/](en/images/), deväť súborov | `/images/webjetcms-2026/en/` |
| Spoločné logá | [sk/images/](sk/images/), šesť súborov `logo-*.png` | `/images/webjetcms-2026/` |

EN snímky majú anglické názvy súborov; slovenské názvy v SK sade zostávajú zachované. Jazykové sady oddeľujú priečinky `sk/` a `en/`. EN logá sa neduplikujú: obe jazykové verzie odkazujú na existujúce súbory v koreňovom priečinku obrázkov. Cesty v HTML sú relatívne voči doméne, preto musia byť súbory dostupné na doméne príslušnej jazykovej verzie.

Najprv nahrajte snímky do nových priečinkov `sk/` a `en/` a overte ich dostupnosť. Až potom vložte nové HTML fragmenty do CMS. **Pôvodné snímky v `/images/webjetcms-2026/` na serveri ponechajte**: môžu ich používať už publikované stránky so starším HTML. Presun cesty v tomto balíku neznamená pokyn na ich odstránenie. Tieto zmeny sú pripravené iba v repozitári; na verejnom webe sa nič nemenilo.

## Slovenské snímky a spoločné logá

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

## Anglické snímky

Snímky boli vizuálne skontrolované 16. 11. 2026. Samotná zámena `docs/sk/` za `docs/en/` nestačila pri AI asistentoch a oprávneniach: oba priame náprotivky zobrazujú slovenské ovládanie. Tabuľka uvádza skutočne použité zdroje aj ich rozmery.

| Obrázok | Rozmery | Veľkosť | Zdroj |
| --- | --- | --- | --- |
| [ai-pagebuilder.png](en/images/ai-pagebuilder.png) – voliteľný | 1440 × 1000 | 230.4 KiB | [Náhľad návrhu v AI čete](../en/redactor/ai/datatables/pb-chat-preview.png) |
| [ai-chat.png](en/images/ai-chat.png) | 1440 × 1000 | 154.9 KiB | [AI čet v editore](../en/redactor/ai/chat/chat-editor.png) |
| [pagebuilder.png](en/images/pagebuilder.png) | 1280 × 760 | 302.3 KiB | [Štruktúra PageBuildera](../en/redactor/webpages/pagebuilder-structure.png) |
| [publication-history.png](en/images/publication-history.png) | 1280 × 760 | 159.2 KiB | [História stránky](../en/redactor/webpages/history.png) |
| [user-permissions.png](en/images/user-permissions.png) | 1210 × 704 | 88.3 KiB | [Oprávnenia používateľa](../en/admin/users/users-tab-right-with-admin-section.png) |
| [document-manager.png](en/images/document-manager.png) | 1400 × 850 | 141.7 KiB | [Manažér dokumentov](../en/redactor/files/file-archive/datatable.png) |
| [multi-step-form.png](en/images/multi-step-form.png) | 1360 × 720 | 105.1 KiB | [Viackrokový formulár](../en/redactor/apps/multistep-form/real-form.png) |
| [custom-application.png](en/images/custom-application.png) | 1280 × 760 | 278.9 KiB | [Katalóg aplikácií](../en/custom-apps/appstore/appstore.png) |
| [headless-web.png](en/images/headless-web.png) | 1280 × 720 | 98.9 KiB | [Headless ukážka](../en/frontend/headless/home.png) |

### Náhrady a jazyk ukážkového obsahu

- `ai-chat.png` používa AI čet v editore namiesto [priameho náprotivku](../en/redactor/ai/datatables/ckeditor-assistants.png) so slovenským výberom asistentov. Snímka má anglické ovládanie četu a editora, ale slovenskú ukážkovú konverzáciu a názov stránky. Popis v EN HTML preto hovorí o čete. Rozmery sa oproti SK obrázku menia z 1280 × 850 na **1440 × 1000**.
- `user-permissions.png` používa kartu **Permissions** používateľa namiesto [priameho náprotivku](../en/admin/users/permissiongroups-editor-dirs.png) so slovenským editorom skupiny práv. Zobrazuje práva na priečinky, stránky, nahrávanie súborov a priradenie skupín práv. Jeden názov ukážkovej skupiny zostal slovenský. Rozmery sa menia z 900 × 492 na **1210 × 704**.
- `ai-pagebuilder.png` a `pagebuilder.png` majú anglické hlavné ovládanie, ale slovenské ukážkové zadanie, obsah alebo názov stránky. V zdrojových snímkach zostal aj nepreložený popis veľkosti zariadenia.
- `publication-history.png` a `document-manager.png` majú anglické ovládanie a stĺpce; slovenské sú niektoré názvy ukážkových stránok, dokumentov a súborov.
- `multi-step-form.png` má anglické ovládanie, názvy polí a tlačidlo ďalšieho kroku. Slovenský zostal názov ukážkového formulára a jeho úvodný text v náhľade.
- `custom-application.png` zobrazuje anglický katalóg aplikácií a tlačidlá. Slovenský zostal názov demo komponentu a obsah stránky v pozadí.
- `headless-web.png` má anglický hlavný obsah a ovládanie ukážky; navigácia prevzatá z ukážkového CMS a niektoré názvy ciest sú slovenské. Obrázok je zhodný aj so slovenským podkladom, pretože ide o tú istú demo aplikáciu.

EN sada teda ukazuje existujúce anglické rozhranie, nie úplne anglické ukážkové dáta. Pri budúcom vytváraní screenshotov možno nahradiť demo obsah anglickým priamo v CMS; dodané obrázky zachovávajú pôvodné pixely dokumentácie.

## Použitie

Snímky dokumentujú skutočné rozhranie; ukážkové názvy a obsah v nich zostali zachované. Pri zostávajúcich snímkach sa po kliknutí otvorí pôvodná plná veľkosť. Načítavajú sa odložene. Obrázky neobsahujú doplnené či vygenerované funkcie.

Logá UNIQA a Tatra banky majú malé pôvodné rozlíšenie. HTML ich neškáluje nad prirodzenú veľkosť. Pri budúcej výmene možno použiť kvalitnejšie schválené podklady značiek; nové logá sa teraz nevytvárali.

Náhľady živých noviniek v SK verzii sa načítavajú priamo z `https://docs.webjetcms.sk/json/social-media/images/` podľa JSON. Nezahŕňajú sa do statického balíka, aby sa aktualizovali spolu s novými príspevkami. Ich pomer 16 : 9 používa `object-fit: contain`, takže obsah náhľadu nie je orezaný.
