# Všeobecná nastavení

Níže naleznete seznam obecných nastavení pomocí kterých je možné změnit chování primárně web stránek. Jednotlivé konfigurační proměnné se nastavují v sekci [Konfigurace](../../admin/setup/configuration/README.md).

Hodnoty doporučujeme nastavit na začátku vytváření web sídla a následně již neměnit, jelikož mohou ovlivnit chování již vytvořených web stránek.

## URL adresy

Chování URL adres web stránek lze nastavit následujícími konfiguračními proměnnými:
- `virtualPathLastSlash` ve výchozím nastavení `true` - nastavuje možnost posledního `/` pro URL adresy **hlavní stránky**. Při nastavení na `true` bude URL adresa stránky Produkty vytvořena jako `/products/`, při nastavení na `false` jak `/products`.
- `editorPageExtension` ve výchozím nastavení `.html` - nastavuje příponu pro **další stránky ve složce**. Při nastavení na `.html` bude URL adresa stránky `iPhone` vytvořena jako `/products/iphone.html`, při nastavení na `/` bude vytvořena jako `/products/iphone/`.

Po změně hodnot je třeba provést restart aplikačního serveru.
