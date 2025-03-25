# Proxy

Aplikace proxy umožňuje do stránky ve WebJETu vložit stránku nebo celou část z jiného web sídla. Podmínkou použití je to, aby při integraci celé části vzdáleného web sídla jej bylo možné identifikovat prefixem URL adresy.

## Nastavení stránky

Pokud nepoužíváte proxy k volání REST služeb, ale výstup vkládáte do vašeho webu je třeba vytvořit Web stránku na které má být vložena část přes aplikaci proxy. Vytvořte ji jako běžnou web stránku, která bude mít **prázdný text**. Pokud vkládáte celou část z jiného web sídla upravte virtuální cestu stránky tak, aby byl na konci znak `*`. Takto bude stránku WebJET zobrazovat pro jakékoli URL začínající na danou adresu. Například `/cobrand/poistenie/*`.

## Nastavení aplikace

Po vytvoření stránky je třeba nastavit parametry mapování adres. Přejděte do menu Aplikace/Proxy. Klepněte na ikonu pro přidání nového záznamu. Zadejte následující položky:
- Název - váš identifikační název proxy (libovolný text)
- Lokální URL adresa - adresa mapování na vašem web sídle, například `/cobrand/poistenie/` (bez koncové `*`). Ve web stránkách vytvořte web stránku s takovou URL adresou, můžete použít znak `*` v URL adrese, například. `/cobrand/poistenie/*` pro vytvoření web stránky, která bude akceptovat všechna URL začínající na zadanou hodnotu.
- Vzdálený server - adresa vzdáleného serveru (bez http prefixu), například `reservations.bookhostels.com`
- Vzdálená URL adresa - URL adresa vzdáleného serveru, například `/custom/index.php`
- Vzdálený port - port na kterém běží vzdálený server, standardně `80`
- Kódování znaků - kódování znaků vzdáleného serveru (např. `windows-1250` nebo `utf-8`)
- Typ proxy - zvolte možnost `ProxyByHttpClient4`, starší verze `ProxyBySocket` nepodporuje všechny možnosti (např. autorizaci).
- Přípony vložené do stránky - seznam přípon, které budou vkládány do web stránky (např. `.htm,.html,.php,.asp,.aspx,.jsp,.do,.action`), ostatní soubory (obrázky, PDF...) budou přímo poslány na výstup. Pokud proxy používáte pro volání REST služby zadejte prázdnou hodnotu, pro takto zadanou hodnotu se odpověď nikdy nebude vkládat do web stránky, odpověď se přepošle přímo na klienta.
- HTML kód oříznutí - začátek - pokud je potřeba přijatý HTML kód oříznout sem zadejte začátek oříznutí, například `<body`
- Ponechat počáteční HTML kód ve výstupu - pokud chcete, aby zadaný začátek oříznutí byl ponechán ve výstupu zapněte tuto možnost. Např. pokud začáteční kód oříznutí je zadán jako `<div id="content` a tento kód potřebujete mít i ve výstupu.
- HTML kód oříznutí - začátek - konec HTML kódu pro oříznutí, například `</body`
- Ponechat koncový HTML kód ve výstupu - podobně jako počáteční kód při zapnutí možnosti vloží do výstupu i zadaný koncový HTML kód.

Do pole Lokální URL adresa je možné zadat více URL adres (každá na nový řádek), které se použijí pro volání proxy, mají následující možnosti:
- `/url-adresa/` - použije se pro stránky začínající na uvedenou URL adresu, například. i pro `/url-adresa/25/`.
- `^/url-adresa/$` - použije se pro stránky s přesně zadanou URL adresu, tedy pouze pro URL adresu `/url-adresa/`.
- `/url-adresa/$` - použije se pro stránky s adresou končící na zadanou hodnotu. pro `/nieco/ine//url-adresa/`.

## Zabezpečení

V kartě zabezpečení je možné aktivovat autorizaci vůči vzdálenému serveru. Při volání REST služby se v HTTP hlavičce `AUTH_USER_CMS` posílá přihlašovací jméno aktuálně přihlášeného uživatele (je-li přihlášen).

Při způsobu autorizace Basic je třeba zadat jméno a heslo, pro autorizaci typu NTLM i Host a Doména.

Pokud potřebujete povolit jen některé HTTP metody, můžete jejich seznam oddělený čárkou zadat do pole Povolené HTTP metody. Pro jiné než povolené metody se vrátí HTTP stav `403`.

## Pokročilé možnosti

Pokud potřebujete ve výstupním HTML kódu ze vzdáleného serveru provést určité úpravy můžete použít připravenou komponentu `proxy.jsp`. Nejprve si udělejte její kopii v adresáři `/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp`. Komponentu vložte do původní stránky pomocí kódu:

`!INCLUDE(/components/INSTALL_NAME/proxy/nazov_proxy/proxy.jsp)!`

Následně můžete v komponentě provést nahrazení HTML kódu, případně provést jiné úpravy. Komponentu můžete použít i pokud potřebujete do stránky kromě výstupu ze vzdáleného serveru vkládat i jiné texty.
