# Audit

Aplikace Audit je nástroj pro sledování změn v systému. Sekce Audit -> Vyhledávání umožňuje zobrazit a filtrovat jednotlivé zaznamenané hodnoty. Filtrování je možné podle typu auditních záznamů, času, uživatele atp.

![](audit-search.png)

# Typy auditních záznamů

Každý auditní záznam automaticky zaznamená datum a čas, ID přihlášeného uživatele, IP adresu a pokud je povolen reverzní DNS server i jméno počítače. Do textu auditního záznamu se automaticky vkládá jméno uzlu clusteru, URI adresa, doména a hodnota http hlavičky User-Agent.
- `ADMINLOG_NOTIFY` - změna v seznamu notifikací v aplikaci Audit.
- `BANNER` - operace v aplikaci Bannerový systém
- `BASKET` - operace v aplikaci Elektronický obchod
- `CALENDAR` - operace v aplikaci Kalendář událostí
- `CONF_DELETE` - smazání konfigurační proměnné, zaznamená její jméno
- `CONF_UPDATE` - změna nebo přidání konfigurační proměnné (v sekci Nastavení), zaznamená jméno, aktuální hodnotu a novou hodnotu proměnné
- `COOKIE_ACCEPTED` - akceptování cookie na web stránce
- `COOKIE_REJECTED` - zamítnutí použití cookies na web stránce
- `CRON` - zaznamená běh úkolů na pozadí, pokud je zaškrtnuta možnost Auditovat. Ukládá i chyby při vykonávání úkolů (pokud nastanou), v takovém případě zaznamená `Stack Trace`.
- `DATA_DELETING` - zaznamenává provedení mazání dat v Nastavení-Mazání dat. Zaznamená klíč který se v cache smazal, nebo `ALL` pro smazání všeho. Při mazání cache obrázků zaznamená cestu k adresáři. Při mazání persistent cache zaznamená ID záznamu.
- `DMAIL` - aplikace Hromadný email
- `DMAIL_AUTOSENDER` - používá se ve speciální situaci automatického odesílání hromadného emailu
- `DMAIL_BLACKLIST` - změna v Hromadný email->Odhlášené e-maily
- `DMAIL_DOMAINLIMITS` - změna v Hromadný email->Doménové limity
- `EXPORT_WEBJET` - nepoužívá se
- `EXPORT` - operace v aplikaci Export dat (přidání, změna, smazání exportu dat)
- `FILE_CREATE` - vytvoření souboru nebo adresáře, zaznamená cestu
- `FILE_DELETE` - smazání souboru nebo adresáře, zaznamená cestu
- `FILE_EDIT` - přejmenování, nebo editace souboru, zaznamená cestu
- `FILE_SAVE` - uložení souboru, například. při jeho kopírování / přesouvání a podobně. Zaznamená cestu k souboru
- `FILE_UPLOAD` - nahrání souboru do WebJETu, ať už přes klasické nahrání, nebo Drag & Drop. Typicky zaznamenává cestu k nahranému souboru.
- `FORMMAIL` - odeslání formuláře. Zaznamená úspěšné odeslání s hlášením `FormMail formName:` jméno formuláře, seznam příjemců a `referer`. Při neúspěchu zaznamená důvod ne odeslání s hlášením `ERROR: formName:` jméno formuláře, `fail:` důvod ne odeslání. Zaznamenává i detekci spamu hlášením `detectSpam TRUE:` důvod detekce jako spam.
- `FORM_ARCHIVE` - archivace formuláře, zaznamená jméno formuláře
- `FORM_DELETE` - smazání formuláře, zaznamená jméno formuláře a případně ID pokud se jedná o smazání jednoho záznamu
- `FORM_EXPORT` - export formuláře přes záložku Exportovat, aktuálně se nezaznamenává univerzální export přes tlačítka pod tabulkou. Podle tohoto záznamu se určuje datum posledního exportu pro možnost exportu od posledního exportu.
- `FORM_REGEXP` - změna v Formuláře->Regulární výrazy
- `FORM_VIEW` - nepoužívá se
- `FORUM_SAVE` - zaznamená detekci vulgarismu v diskusním fóru
- `FORUM` - operace v aplikaci Diskuse
- `GALLERY` - změny v aplikaci Galerie - vytvoření adresáře, přidání/smazání fotky
- `GDPR_FORMS_DELETE` - aplikace GDPR, mazání starých formulářů
- `GDPR_USERS_DELETE` - aplikace GDPR, mazání starých uživatelů
- `GDPR_BASKET_INVOICES_DELETE` - aplikace GDPR, mazání starých objednávek z elektronického obchodu
- `GDPR_EMAILS_DELETE` - aplikace GDPR, mazání starých emailů
- `GDPR_REGEXP` - aplikace GDPR, správa regulárních výrazů
- `GDPR_DELETE` - aplikace GDPR, nastavení mazání dat
- `GDPR_COOKIES` - aplikace GDPR, správa cookies
- `GROUP` - vytvoření / uložení / smazání adresáře v sekci Web stránky
- `HELPDESK` - nepoužívá se
- `HELP_LAST_SEEN` - používá se k evidenci data zobrazení sekce Co je nového v nápovědě. Při přihlašování se v této sekci hledá nejnovější soubor a porovnává se vůči zaznamenanému datu v Auditu. Pokud existuje novější soubor, zobrazí se po přihlášení vyskakovací okno nápovědy se sekcí Co je nového.
- `IMPORTXLS` - importu Excel souboru, používá se v zákaznických implementacích. Zaznamenává cestu k importovanému souboru a jeho velikost
- `IMPORT_WEBJET` - nepoužívá se
- `INIT` - inicializace WebJETu (start), zaznamenává cestu k adresáři ve kterém byl WebJET na aplikačním serveru spuštěn a číslo verze WebJETu
- `INQUIRY` - operace v aplikaci Anketa
- `INQUIRY` - přidání otázky v aplikaci Anketa, zaznamená text otázky
- `INSERT_SCRIPT` - změna v aplikaci Skripty
- `INVENTORY` - operace v aplikaci Majetek
- `JSPERROR` - chyba při provádění JSP souboru při zobrazení web stránky, zaznamená `Stack Trace` chyby
- `MEDIA` - operace s Médiemi (karta Média ve web stránce).
- `MEDIA_GROUP` - aplikace správa skupin médií.
- `PAGE_DELETE` - smazání, přesun do koše nebo požádání o smazání stránky, zaznamená ID stránky
- `PAGE_UPDATE` - zaznamenává změny ve stránce mimo standardní uložení v editoru - hromadné operace v seznamu stránek
- `PAYMENT_GATEWAY` - volání platební brány v aplikaci Elektronický obchod
- `PEREX_GROUP_CREATE` - vytvoření perex skupiny, zaznamená její jméno
- `PEREX_GROUP_DELETE` - smazání perex skupiny, zaznamená její jméno a ID
- `PEREX_GROUP_UPDATE` - změna perex skupiny, zaznamená její jméno
- `PERSISTENT_CACHE` - změna v Mazání dat->Persistent cache objekty
- `PROP_DELETE` - smazání překladového textu, zaznamená jazyk a klíč
- `PROP_UPDATE` - editace překladového textu (v sekci Nastavení), zaznamená jazyk, klíč a hodnotu
- `PROXY` - operace v aplikaci proxy
- `QA` - operace v aplikaci Otázky a odpovědi
- `REDIRECT_CREATE` - vytvoření nového přesměrování (url nebo domény)
- `REDIRECT_DELETE` - smazání přesměrování (url nebo doména), zaznamená zdroj a pro doménu i cíl přesměrování
- `REDIRECT_UPDATE` - změna přesměrování (url nebo domény), zaznamená zdrojovou a cílovou adresu
- `RUNTIME_ERROR` - zaznamená chybějící šablonu pro zobrazení stránky
- `SAVEDOC` - uložení web stránky v Editoru, zaznamenává i žádosti o schválení. Zaznamenává titulek stránky, ID stránky a ID v historii
- `SENDMAIL` - odeslání emailu (mimo formulářů), zaznamená email odesílatele, příjemce a předmět emailu
- `SE_SITEMAP` - generování souboru `/sitemap.xml`, zaznamená ID adresáře pro který se mapa stránky generuje a obsah hlavičky User-Agent
- `SQLERROR` - databázová chyba, zaznamená SQL chybu, zdroj chyby a `Stack Trace`
- `TEMPLATE_DELETE` - smazání šablony, zaznamená jméno smazané šablony
- `TEMPLATE_INSERT` - vytvoření nové šablony, zaznamená její jméno
- `TEMPLATE_UPDATE` - změna v šabloně, zaznamená seznam změněných polí
- `TEMPLATE_GROUP` - změna ve skupině šablon
- `TIP` - operace v aplikaci Tip dne
- `TOOLTIP` - změna v aplikaci Tooltip
- `UPDATEDB` - provedení změny v databázi přes admin konzoli
- `USER_AUTHORIZE` - autorizace uživatele (schválení přístupu do zaheslované sekce). Zaznamenává ID smazaného uživatele, pokud je známo i jeho `login` a jméno.
- `USER_CHANGE_PASSWORD` - audituje změnu hesla uživatele. Na základě data se počítá interval změny hesla (je-li nastaven)
- `USER_DELETE` - smazání uživatele. Zaznamenává ID smazaného uživatele, pokud je známo i jeho `login` a jméno.
- `USER_EDIT` - zaznamená událost otevření editace uživatele, nejedná se ještě o jeho uložení. Zaznamená ID uživatele, `login` a jméno.
- `USER_GROUP_DELETE` - smazání skupiny uživatelů, zaznamená jméno skupiny a její ID
- `USER_GROUP_INSERT` - vytvoření nové skupiny uživatelů, zaznamená jméno nové skupiny a její typ
- `USER_GROUP_UPDATE` - uložení skupiny uživatelů, zaznamená jméno skupiny a seznam změn
- `USER_INSERT` - vytvoření nového uživatele (nebo nová registrace v zaheslované sekci). Zaznamená ID uživatele, `login` a jméno.
- `USER_LOGOFF` - odhlášení uživatele kliknutím na ikonu odhlášení, zaznamená přihlašovací jméno a informaci o tom, zda se jedná o administrátora nebo registrovaného návštěvníka
- `USER_LOGON` - přihlášení uživatele, zaznamená přihlašovací jméno a informaci o tom, zda se jedná o administrátora nebo registrovaného návštěvníka. Zaznamená i událost zadání neplatného hesla, pokud uživatel není autorizovaný nebo přihlašovací jméno je neznámé
- `USER_PERM_GROUP` - operace se skupinami práv, zaznamená jméno skupiny a při změně seznam změn
- `USER_SAVE` - zaznamenává změny v uživateli v zaheslované sekci (pokud obsahuje formulář pro změnu údajů)
- `USER_UPDATE` - uložení stávajícího uživatele. Zaznamenává aktuální nastavení práv a změny v zadaných údajích
- `WEB_SERVICES` - volání zákaznických `WebServices` (použití záleží na implementaci pro konkrétního zákazníka)
- `XSRF` - XSRF útok na server (nepovolený referer header), zaznamená hodnotu doménového jména z `referer` hlavičky
- `XSS` - XSS útok na server nebo přímé (nepovolené) volání JSP souboru. Zaznamená URL adresu nebo výraz kvůli kterému se útok vyhodnotil (např. nepovolený token v URL, nepovolené HTTP metoda). Zaznamenává také ukradení cookie (změna IP adresy session).

# Speciální formát auditu

V případě potřeby je možné do WebJETu doplnit kód, který audit záznamy bude ukládat do speciálního souboru, nebo posílat na určenou službu. Je třeba nastavit konf. proměnnou `adminlogCustomLogger` na Java třídu, která implementuje třídu `sk.iway.iwcm.AdminlogCustomLogger`. Při každém zápisu se jmenuje metoda `addLog(logType, requestBean, descriptionParam, timestamp)`
