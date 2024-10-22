# Proxy

Aplikace proxy umožňuje vložit stránku nebo celou sekci z jiného webu do stránky WebJET. Podmínkou použití je, že při začlenění celé části vzdálené webové stránky ji lze identifikovat pomocí prefixu URL.

## Nastavení stránky

Pokud nepoužíváte proxy server k volání služeb REST, ale vkládáte výstup do svého webu, je třeba vytvořit webovou stránku, na kterou se část vloží prostřednictvím aplikace proxy serveru. Vytvořte ji jako běžnou webovou stránku, která bude mít **prázdný text**. Pokud vkládáte celou sekci z jiného webu, upravte virtuální cestu ke stránce tak, aby znak na konci byl `*`. Tím se zobrazí stránka WebJET pro jakoukoli adresu URL začínající touto adresou. Například, `/cobrand/poistenie/*`.

## Nastavení aplikace

Po vytvoření stránky je třeba nastavit parametry mapování adres. Přejděte do nabídky Aplikace/Proxy. Klepnutím na ikonu přidejte novou položku. Zadejte následující údaje:
- Jméno - vaše identifikační jméno zástupce (volný text)
- Místní adresa URL - mapovací adresa na vašich webových stránkách, například `/cobrand/poistenie/` (bez konce) `*`). Na webových stránkách vytvořte webovou stránku s takovou adresou URL, můžete použít znak `*` v adrese URL, např. `/cobrand/poistenie/*` vytvořit webovou stránku, která bude přijímat všechny adresy URL začínající zadanou hodnotou.
- Vzdálený server - adresa vzdáleného serveru (bez předpony http), např. `reservations.bookhostels.com`
- Vzdálená adresa URL - adresa URL vzdáleného serveru, např. `/custom/index.php`
- Vzdálený port - port, na kterém je ve výchozím nastavení spuštěn vzdálený server. `80`
- Kódování znaků - kódování znaků vzdáleného serveru (např. `windows-1250` nebo `utf-8`)
- Typ proxy serveru - vyberte možnost `ProxyByHttpClient4`, starší verze `ProxyBySocket` nepodporuje všechny možnosti (např. autorizaci).
- Rozšíření vložená do stránky - seznam rozšíření, která budou vložena do webové stránky (např. `.htm,.html,.php,.asp,.aspx,.jsp,.do,.action`), ostatní soubory (obrázky, PDF...) budou odeslány přímo na výstup. Pokud pro volání služby REST použijete proxy server, zadejte prázdnou hodnotu, pro tuto hodnotu nebude odpověď nikdy vložena do webové stránky, odpověď bude předána přímo klientovi.
- HTML trim kód - začátek - pokud je třeba oříznout přijatý HTML kód, zadejte zde začátek ořezu, např. `<body`
- Zachovat ve výstupu začátek kódu HTML - pokud chcete, aby byl ve výstupu zachován zadaný začátek výřezu, povolte tuto možnost. Např. pokud je počáteční kód výstřižku zadán jako `<div id="content` a tento kód musí být uveden i ve výstupu.
- Kód HTML pro výstřižky - začátek - konec kódu HTML pro výstřižky, například `</body`
- Ponechání koncového kódu HTML ve výstupu - podobně jako u počátečního kódu, pokud je tato možnost povolena, vloží se do výstupu také zadaný koncový kód HTML.

Do pole Místní adresa URL lze zadat více adres URL (každou na novém řádku), které budou použity pro volání proxy serveru, mají následující možnosti:
- `/url-adresa/` - bude použita pro stránky začínající zadanou adresou URL, např. také pro `/url-adresa/25/`.
- `^/url-adresa/$` - se používá pro stránky s přesně zadanou adresou URL, tj. pouze pro adresu URL `/url-adresa/`.
- `/url-adresa/$` - se používá pro stránky s adresou končící na zadanou hodnotu, např. pro `/nieco/ine//url-adresa/`.

## Zabezpečení

Na kartě zabezpečení je možné aktivovat autorizaci vůči vzdálenému serveru. Při volání služby REST se zobrazí hlavička HTTP `AUTH_USER_CMS` odešle přihlašovací jméno aktuálně přihlášeného uživatele (je-li přihlášen).

Pro základní metodu autorizace je nutné zadat jméno a heslo a pro autorizaci NTLM je nutné zadat také Hostitele a Doménu.

Pokud potřebujete povolit pouze některé metody HTTP, můžete do pole Povolené metody HTTP zadat jejich seznam oddělený čárkou. Pro jiné metody než povolené bude vrácen stav HTTP `403`.

## Rozšířené možnosti

Pokud potřebujete provést nějaké úpravy ve výstupním kódu HTML ze vzdáleného serveru, můžete použít připravenou komponentu. `proxy.jsp`. Nejprve vytvořte jeho kopii v adresáři `/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp`. Vložte komponentu do původní stránky pomocí kódu:

`!INCLUDE(/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp)!`

Poté můžete v komponentě nahradit kód HTML nebo provést jiné úpravy. Komponentu můžete použít také v případě, že potřebujete do stránky vložit jiné texty než výstup ze vzdáleného serveru.
