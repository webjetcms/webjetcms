# Obecná nastavení

Níže naleznete seznam obecných nastavení, kterými lze změnit chování primárního webu. Jednotlivé konfigurační proměnné se nastavují v [Konfigurace](../../admin/setup/configuration/README.md).

Doporučujeme nastavit tyto hodnoty na začátku vytváření webové stránky a neměnit je později, protože mohou ovlivnit chování již vytvořených webových stránek.

## Adresy URL

Chování adres URL webových stránek lze nastavit pomocí následujících konfiguračních proměnných:
- `virtualPathLastSlash` Výchozí `true` - nastaví možnost posledního `/` pro adresy URL **hlavní stránka**. Při nastavení na `true` adresa URL stránky Produkty bude vytvořena jako `/products/`, pokud je nastavena na hodnotu `false` Stejně jako `/products`.
- `editorPageExtension` Výchozí `.html` - nastaví příponu pro **další stránky ve složce**. Při nastavení na `.html` bude adresa URL stránky `iPhone` vytvořené jako `/products/iphone.html`, pokud je nastavena na hodnotu `/` bude vytvořen jako `/products/iphone/`.

Po změně hodnot je nutné restartovat aplikační server.
