# Proxy

Aplikácia proxy umožňuje do stránky vo WebJETe vložiť stránku, alebo celú časť z iného web sídla. Podmienkou použitia je to, aby pri integrácii celej časti vzdialeného web sídla ho bolo možné identifikovať prefixom URL adresy.

## Nastavenie stránky

Ak nepoužívate proxy na volanie REST služieb, ale výstup vkladáte do vášho webu je potrebné vytvoriť Web stránku na ktorej má byť vložená časť cez aplikáciu proxy. Vytvorte ju ako bežnú web stránku, ktorá bude mať **prázdny text**. Ak vkladáte celú časť z iného web sídla upravte virtuálnu cestu stránky tak, aby bol na konci znak ```*```. Takto bude stránku WebJET zobrazovať pre akékoľvek URL začínajúce na danú adresu. Napríklad ```/cobrand/poistenie/*```.

## Nastavenie aplikácie

Po vytvorení stránky je potrebné nastaviť parametre mapovania adries. Prejdite do menu Aplikácie/Proxy. Kliknite na ikonu pre pridanie nového záznamu. Zadajte nasledovné položky:

- Názov - váš identifikačný názov proxy (ľubovoľný text)
- Lokálna URL adresa - adresa mapovania na vašom web sídle, napríklad ```/cobrand/poistenie/``` (bez koncovej ```*```). Vo web stránkach vytvorte web stránku s takouto URL adresou, môžete použiť znak `*` v URL adrese, napr. `/cobrand/poistenie/*` pre vytvorenie web stránky, ktorá bude akceptovať všetky URL začínajúce na zadanú hodnotu.
- Vzdialený server - adresa vzdialeného servera (bez http prefixu), napríklad ```reservations.bookhostels.com```
- Vzdialená URL adresa - URL adresa vzdialeného servera, napríklad ```/custom/index.php```
- Vzdialený port - port na ktorom beží vzdialený server, štandardne ```80```
- Kódovanie znakov - kódovanie znakov vzdialeného servera (napr. ```windows-1250``` alebo ```utf-8```)
- Typ proxy - zvoľte možnosť `ProxyByHttpClient4`, staršia verzia `ProxyBySocket` nepodporuje všetky možnosti (napr. autorizáciu).
- Prípony vložené do stránky - zoznam prípon, ktoré budú vkladané do web stránky (napr. ```.htm,.html,.php,.asp,.aspx,.jsp,.do,.action```), ostatné súbory (obrázky, PDF...) budú priamo poslané na výstup. Ak proxy používate pre volanie REST služby zadajte prázdnu hodnotu, pre takto zadanú hodnotu sa odpoveď nikdy nebude vkladať do web stránky, odpoveď sa prepošle priamo na klienta.
- HTML kód orezania - začiatok - ak je potrebné prijatý HTML kód orezať sem zadajte začiatok orezania, napríklad ```<body```
- Ponechať začiatočný HTML kód vo výstupe - ak chcete, aby zadaný začiatok orezania bol ponechaný vo výstupe zapnite túto možnosť. Napr. ak začiatočný kód orezania je zadaný ako `<div id="content` a tento kód potrebujete mať aj vo výstupe.
- HTML kód orezania - začiatok - koniec HTML kódu pre orezanie, napríklad ```</body```
- Ponechať koncový HTML kód vo výstupe - podobne ako začiatočný kód pri zapnutí možnosti vloží do výstupu aj zadaný koncový HTML kód.

Do poľa Lokálna URL adresa je možné zadať viacero URL adries (každá na nový riadok), ktoré sa použijú pre volanie proxy, majú nasledovné možnosti:

- `/url-adresa/` - použije sa pre stránky začínajúce na uvedenú URL adresu, napr. aj pre `/url-adresa/25/`.
- `^/url-adresa/$` - použije sa pre stránky s presne zadanou URL adresu, čiže len pre URL adresu `/url-adresa/`.
- `/url-adresa/$` - použije sa pre stránky s adresou končiacou na zadanú hodnotu, napr. pre `/nieco/ine//url-adresa/`.

## Zabezpečenie

V karte zabezpečenie je možné aktivovať autorizáciu voči vzdialenému serveru. Pri volaní REST služby sa v HTTP hlavičke `AUTH_USER_CMS` posiela prihlasovacie meno aktuálne prihláseného používateľa (ak je prihlásený).

Pri spôsobe autorizácie Basic je potrebné zadať meno a heslo, pre autorizáciu typu NTLM aj Host a Doména.

Ak potrebujete povoliť len niektoré HTTP metódy môžete ich zoznam oddelený čiarkou zadať do poľa Povolené HTTP metódy. Pre iné ako povolené metódy sa vráti HTTP stav `403`.

## Pokročilé možnosti

Ak potrebujete vo výstupnom HTML kóde zo vzdialeného servera vykonať určité úpravy môžete použiť pripravenú komponentu ```proxy.jsp```. Najskôr si spravte jej kópiu v adresári
```/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp```.
Komponentu vložte do pôvodnej stránky pomocou kódu:

```!INCLUDE(/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp)!```

Následne môžete v komponente vykonať nahradenia HTML kódu, prípadne vykonať iné úpravy. Komponentu môžete použiť aj ak potrebujete do stránky okrem výstupu zo vzdialeného servera vkladať aj iné texty.