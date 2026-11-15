# Aktuálny web a hranice HTML výstupu

Overené 15. 11. 2026 na verejnom webe. Tento dokument je technický podklad, nie hotový obsah na publikovanie.

## Rozsah

Používateľ žiada nahradiť obsah vnútri `article.ly-article` na úvodnej stránke, v Benefitoch, Funkciách a Aplikáciách. Dizajn zostáva existujúci. Stránky Služby, Aktuálna verzia a Licencie sú mimo rozsahu. Nové cesty pre vývojárov a agentúry možno odporučiť; menu je mimo článku a samotné vloženie fragmentu ho nezmení.

Výstupná štruktúra:

- `docs/website/*.md`: výskum, obsahová stratégia, rozhodnutia, mapovanie zdrojov a pokyny na vloženie.
- `docs/website/sk/*.html`: výsledné slovenské fragmenty bez `html`, `head`, `body` a bez ďalšieho obalu `article`.
- `docs/website/sk/images/`: použité dokumentačné obrázky a prípadné odvodené varianty s evidovaným pôvodom.

Finálne fragmenty a vybrané obrázky sú uložené podľa tejto štruktúry. Používateľ potvrdil aj stránky Pre vývojárov a Pre agentúry, vynechanie licenčných kariet na úvode a doplnenie živých noviniek. Zmeny zostávajú necommitnuté.

## Overený technický základ

Verejný skript [bootstrap.bundle.min.js](https://www.webjetcms.sk/templates/webjet_products/assets/js/plugins/bootstrap.bundle.min.js) obsahuje hlavičku **Bootstrap v4.1.3**. Pri príprave fragmentov nepoužívať utility vyžadujúce Bootstrap 5.

Šablóna má `body.cms`, potom `main.ly-content-wrapper > div.ly-content > article.ly-article#blindBlock-article`. Hlavné menu a pätička sú mimo článku. H1 je vo všetkých štyroch skúmaných stránkach súčasťou článku, preto ho má obsahovať aj nový fragment.

Hlavný CSS balík načítava `/templates/webjet_products/assets/css/shame.css` a `/templates/interway/webjet_products/assets/css/ninja.min.css` cez `/components/_common/combine.jsp`. Používa písma Neo Sans a Open Sans, existujúce farby CMS a zaoblené tlačidlá. Nie je dôvod pridávať ďalší framework, font ani knižnicu.

| Existujúca trieda | Použitie | Poznámka pre nové texty |
| --- | --- | --- |
| `md-banner` | Úvod s H1 a perexom | Perex má vlastné obmedzenie šírky; dlhý text presunúť do ďalšej sekcie. |
| `md-intro` | Textová sekcia s nadpisom | Zachovať typografiu a existujúce odstupy. |
| `md-cta-list`, `md-cta-list__wrapper__list` | Zoznam benefitov | Existujúce odrážky používajú Font Awesome; netreba novú sadu ikon. |
| `md-top-features`, `md-top-features__item`, `md-top-features__wrapper` | Striedanie obrázka a textu na Benefitoch | Pôvodne veľké ilustrácie, absolútne polohovanie a pevná výška na desktope. Dokumentačné screenshoty potrebujú vizuálne overenie vrátane mobilu. |
| `md-top-features__item--right` | Obrátené poradie obrazu a textu | Využiť pôvodný vizuálny princíp. |
| `md-logo-showcase`, `logo-container` | Referencie | Existujúci pás log je použiteľný. Doplnené mená klientov nemajú naznačovať používanie všetkých nových funkcií. |
| `btn btn-cta`, `btn btn-primary` | Výzva na akciu | Obyčajný odkaz je spoľahlivejší než kopírovanie interného formulára alebo `javascript:void(0)`. |
| `row`, `col-md-*`, `img-fluid`, `table-responsive` | Bootstrap 4 rozloženie | Použiť len podľa potreby, bez celkového prerábania dizajnu. |

Nevyhnutné nové CSS má byť vložené v `style` priamo vo fragmente a obmedzené na vlastný jednoznačný obal/triedy. Globálne pravidlá pre `h2`, `p`, `img`, `.row` alebo `.btn` by mohli meniť zvyšok webu.

## Obsahový audit

| Stránka | Aktuálny stav | Dôsledok pre prepis |
| --- | --- | --- |
| [Úvod](https://www.webjetcms.sk/) | Všeobecný bezpečný redakčný systém, staršie snímky administrácie, zoznamy výhod, logá, blok verzie a licenčné karty. | Konkretizovať publikum, AI prácu a redakčné riadenie. Rozlíšiť nové produktové texty od detailov verzie/licencií. |
| [Benefity](https://www.webjetcms.sk/benefity/) | Striedané texty a ilustrácie. Bezpečnosť vysvetľuje malým rozšírením produktu. | Nepreberať sľub, že neexistujú automatizované útoky. Opísať doložené oprávnenia, schvaľovanie a audit. |
| [Funkcie](https://www.webjetcms.sk/funkcie/) | Akordeóny s ďalšími vnorenými záložkami. Všeobecné sľuby súladu s legislatívou. | Zoskupiť schopnosti podľa práce používateľa; konkrétne nástroje neprezentovať ako záruku súladu celého nasadenia. |
| [Aplikácie](https://www.webjetcms.sk/aplikacie/) | Dlhá tabuľka previazaná s dostupnosťou v licenciách. | Vysvetliť praktické použitie aplikácií. Dostupnosť v licenciách nevymýšľať ani nesynchronizovať bez zadania. |

Úvod obsahuje aj skrytý kontaktný formulár s triedou `pg-contact--hidden`. Nové CTA môže odkazovať na existujúcu [kontaktnú stránku](https://www.webjetcms.sk/kontakt/). Tá dnes obsahuje formulár s poľom Požiadavka, nie verejne overený rezervačný kalendár. Text o dohodnutí ukážky preto nesmie sľúbiť okamžitú rezerváciu alebo konkrétny čas odpovede.

## Referencie zo zadania

Používateľ uvádza Ministerstvo financií SR, Všeobecnú zdravotnú poisťovňu, Ministerstvo obrany SR, Úrad geodézie, kartografie a katastra SR, Ministerstvo práce, sociálnych vecí a rodiny SR, UNIQA, NN, Tatra banku, VÚB banku a Orange. Existujúca hlavná stránka už zobrazuje logá MF SR, VšZP, NN, Eustream, VÚB, UNIQA a Tatra banky.

Tieto informácie možno použiť ako referencie produktu podľa zadania. Nie sú dokladom nameraných úspor, výsledkov projektov, klientskych citácií ani používania AI konkrétnym klientom. Ak sa nenájde existujúce vhodné logo, názov organizácie môže zostať textový.

## Kontrola pri finálnom odovzdaní

Overiť kompletnosť fragmentov, jediný H1 na stránku, zmysluplné poradie nadpisov, unikátne ID, alt texty a existenciu obrázkov. Odkazy na existujúce stránky majú zachovať ich verejné URL. Pre nové stránky treba uviesť navrhovanú URL a krok založenia v CMS.

Náhľad otestovať v pôvodnej šablóne s jej CSS na desktope aj mobile. Predmetom kontroly je zalamovanie, čitateľnosť, neorezané snímky a ovládateľné odkazy. Plný CMS build ani Java testy nemajú pre samotný obsahový balík význam.
