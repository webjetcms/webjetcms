# WebJET CMS 2024

Vítejte v dokumentaci k WebJET CMS verze 2024. Doporučujeme přečíst si [seznam změn](CHANGELOG-2024.md) a [roadmap](ROADMAP.md).

# Seznam změn v poslední verzi

## 2025.0

> Ve verzi **2025.0** jsme přinesli **nový design administrace** pro ještě lepší přehlednost a uživatelský komfort.
>
> Jednou z hlavních změn je přesunutí **druhé úrovně menu** do **karet v hlavičce stránky**, čímž se zjednodušila navigace. Ve webových stránkách jsme také **sloučily karty složek a webových stránek**, abyste měli vše přehledně na jednom místě. Pokud hlavička neobsahuje karty, tabulky se automaticky přizpůsobí a zobrazí **řádek navíc**.
>
> Prosíme vás o zpětnou vazbu prostřednictvím **formuláře Zpětná vazba**, pokud při používání nové verze identifikujete **jakýkoli problém se zobrazením**. Připomínku můžete doplnit io **fotku obrazovky**, což nám pomůže rychleji identifikovat a vyřešit případné nedostatky.
>
> Děkujeme za spolupráci a pomoc při vylepšování WebJET CMS!

## Průlomové změny

- Web stránky - zrušena inline editace. Možnost přímé editace stránky v režimu jejího zobrazení byla odstraněna, jelikož využívala starší verzi editoru, která již není podporována. Jako alternativu lze aktivovat nástrojový panel zobrazovaný v pravém horním rohu webové stránky. Tento panel umožňuje rychlý přístup k editoru web stránky, složky nebo šablony. Můžete vypnout nebo zapnout pomocí konfigurační proměnné `disableWebJETToolbar`. Po aktivaci se začne zobrazovat na webové stránce po vstupu do sekce Webové stránky v administraci (#57629).
- Přihlášení - pro administrátory nastavena [požadavek na změnu hesla](sysadmin/pentests/README.md#pravidla-hesel) jednou za rok. Hodnotu lze upravit v konfigurační proměnné `passwordAdminExpiryDays`, nastavením na hodnotu 0 se kontrola vypne (#57629).
- Úvod - přidán požadavek na aktivaci dvoustupňového ověřování pro zvýšení bezpečnosti přihlašovacích údajů. Výzva se nezobrazuje, pokud je ověřování řešeno přes `LDAP` nebo je-li překladový klíč `overview.2fa.warning` nastaven na prázdnou hodnotu (#57629).

### Design

Ve verzi **2025.0** jsme přinesli vylepšený **design administrace**, který je přehlednější a efektivnější.

- **Upravený přihlašovací dialog** – nové pozadí a přesunutí přihlašovacího dialogu na pravou stranu. Na **přihlášení** je možné použít nejen přihlašovací jméno ale **už i email adresu**. ![](redactor/admin/logon.png)
- **Přehlednější hlavička** – název aktuální stránky nebo sekce se nyní zobrazuje přímo v hlavičce.
- **Nová navigace v levém menu** – pod položky již nejsou součástí levého menu, ale zobrazují se **jako karty v horní části** stránky. ![](redactor/admin/welcome.png)
- **Sloučené karty v sekci Webové stránky** – přepínání typů složky a typů webových stránek se nyní zobrazují ve společné části, čímž se zjednodušila navigace. **Výběr domény** byl přesunut na spodní část levého menu. ![](redactor/webpages/domain-select.png)
- **Přeorganizované menu položky**:
  - **SEO** přesunuty do sekce **Přehledy**.
  - **GDPR a Skripty** přesunuty do sekce **Šablony**.
  - **Galerie** je nyní v sekci **Soubory**.
  - Některé názvy položek byly upraveny, aby lépe vystihovaly jejich funkci.

Zbývající seznam změn změn je shodný s verzí [2024.52](CHANGELOG-2024.md).

## Web stránky

- Přidána možnost nastavit inkrement pořadí uspořádání pro složky v konfigurační proměnné `sortPriorityIncrementGroup` a web stránky v konfigurační proměnné `sortPriorityIncrementDoc`. Výchozí hodnoty jsou 10 (#57667-0).

### Testování

- Standardní heslo pro `e2e` testy se získá z `ENV` proměnné `CODECEPT_DEFAULT_PASSWORD` (#57629).

### Oprava chyb

- Webové stránky - vkládání odkazů na soubor v PageBuilder (#57649).
- Webové stránky - doplněné informace o odkazu (typ souboru, velikost) do atributu Pomocný titulek `alt` (#57649).
- Webové stránky - opravené nastavení pořadí uspořádání web stránek při použití `Drag&Drop` ve stromové struktuře (#57657-1).
- Webové stránky - při duplikování webové stránky/složka se nastaví hodnota `-1` do pole Pořadí uspořádání pro zařazení na konec seznamu. Hodnotu `-1` můžete také zadat ručně pro získání nové hodnoty pořadí uspořádání (#57657-1).
- Webové stránky - import webových stránek - opraveno nastavení média skupin při importu stránek obsahujících média. Při importu se automaticky vytvoří všechna Média skupiny (i nepoužívaná) z důvodu, že se při importu stránek překládá i média skupina nastavená pro média aplikaci `/components/media/media.jsp` ve stránce (ta může obsahovat i ID média skupiny mimo importované stránky) (#57657-1).
- Firefox - snížená verze sady `Tabler Icons` na `3.0.1`, protože Firefox při použití novějších verzí výrazně zatěžuje procesor. Optimalizované čtení CSS stylu `vendor-inline.style.css` (#56393-19).

![meme](_media/meme/2025-0.jpg ":no-zoom")
