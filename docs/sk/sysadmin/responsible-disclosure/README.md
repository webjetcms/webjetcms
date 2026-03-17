# Zodpovedné oznamovanie zraniteľností

Bezpečnosť WebJET CMS berieme veľmi vážne. Ak ste objavili bezpečnostnú zraniteľnosť, prosíme vás, aby ste ju nahlásili zodpovedným spôsobom a umožnili nám ju opraviť skôr, ako bude zverejnená.

**Prosíme, nenahlasujte bezpečnostné zraniteľnosti prostredníctvom verejných GitHub issues.**

## Ako nahlásiť zraniteľnosť

Bezpečnostné zraniteľnosti nahláste prostredníctvom nášho formulára pre zodpovedné oznamovanie:

**[Zodpovedné oznamovanie zraniteľností](https://www.interway.sk/sluzby-riesenia/zodpovedne-oznamovanie-zranitelnosti/)**

## Čo uviesť v hlásení

Pri nahlasovaní zraniteľnosti prosíme uveďte:

- Typ zraniteľnosti (napr. SQL injection, XSS, obchádzanie autentifikácie)
- Cesty k súborom súvisiacim so zraniteľnosťou
- Umiestnenie dotknutého zdrojového kódu (tag/vetva/commit alebo priama URL adresa)
- Kroky na reprodukciu problému
- Proof-of-concept alebo exploit kód (ak je to možné)
- Dopad zraniteľnosti a ako by ju útočník mohol zneužiť

## Čo môžete očakávať

- Potvrdíme prijatie vášho hlásenia
- Preveríme a potvrdíme problém
- Opravíme zraniteľnosť čo najskôr v závislosti od jej zložitosti
- Upozorníme vás, keď bude zraniteľnosť opravená

## Nastavenie na GitHub

Na GitHub je možné aktivovať bezpečnostnú politiku repozitára. Súbor `.github/SECURITY.md` je zobrazený na záložke **Security** repozitára a GitHub automaticky odkazuje na tento súbor pri hlásení problémov. Odporúčame:

1. Overiť, že súbor `.github/SECURITY.md` je prítomný v repozitári.
2. Na GitHub prejsť do **Settings → Code security and analysis** a zapnúť **Private vulnerability reporting** pre umožnenie súkromného hlásenia zraniteľností priamo cez GitHub.

## Podporované verzie

Poskytujeme bezpečnostné aktualizácie pre najnovšie stabilné vydanie WebJET CMS. Odporúčame vždy používať najnovšiu verziu.

## Bezpečnostná dokumentácia

Pre viac informácií o bezpečnostnej konfigurácii a hardening WebJET CMS pozrite:

- [Bezpečnostné testy](/sysadmin/pentests/README.md)
- [Kontrola zraniteľností knižníc](/sysadmin/dependency-check/README.md)
- [Aktualizácia WebJETu](/sysadmin/update/README.md)
