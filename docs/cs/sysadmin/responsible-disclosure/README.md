# Zodpovědné oznamování zranitelností

Bezpečnost WebJET CMS bereme velmi vážně. Pokud jste objevili bezpečnostní zranitelnost, prosíme vás, abyste ji nahlásili zodpovědným způsobem a umožnili nám ji opravit dříve, než bude zveřejněna.

**Prosíme, nenahlašujte bezpečnostní zranitelnosti prostřednictvím veřejných GitHub issues.**

## Jak nahlásit zranitelnost

Bezpečnostní zranitelnosti nahlaste prostřednictvím našeho formuláře pro zodpovědné oznamování:

**[Zodpovědné oznamování zranitelností](https://www.interway.sk/sluzby-riesenia/zodpovedne-oznamovanie-zranitelnosti/)**

## Co uvést v hlášení

Při nahlašování zranitelnosti prosíme uveďte:

- Typ zranitelnosti (např. SQL injection, XSS, obcházení autentizace)
- Cesty k souborům souvisejícím se zranitelností
- Umístění dotčeného zdrojového kódu (tag/větev/commit nebo přímá URL adresa)
- Kroky k reprodukci problému
- Proof-of-concept nebo exploit kód (pokud je to možné)
- Dopad zranitelnosti a jak by ji útočník mohl zneužít

## Co můžete očekávat

- Potvrdíme přijetí vašeho hlášení
- Prověříme a potvrdíme problém
- Opravíme zranitelnost co nejdříve v závislosti na její složitosti
- Upozorníme vás, když bude zranitelnost opravena

## Nastavení na GitHub

Na GitHub je možné aktivovat bezpečnostní politiku repozitáře. Soubor `.github/SECURITY.md` je zobrazen na záložce **Security** repozitáře a GitHub automaticky odkazuje na tento soubor při nahlašování problémů. Doporučujeme:

1. Ověřit, že soubor `.github/SECURITY.md` je přítomen v repozitáři.
2. Na GitHub přejít do **Settings → Code security and analysis** a zapnout **Private vulnerability reporting** pro umožnění soukromého nahlašování zranitelností přímo přes GitHub.

## Podporované verze

Poskytujeme bezpečnostní aktualizace pro nejnovější stabilní vydání WebJET CMS. Doporučujeme vždy používat nejnovější verzi.

## Bezpečnostní dokumentace

Pro více informací o bezpečnostní konfiguraci a hardening WebJET CMS viz:

- [Bezpečnostní testy](/sysadmin/pentests/README.md)
- [Kontrola zranitelností knihoven](/sysadmin/dependency-check/README.md)
- [Aktualizace WebJETu](/sysadmin/update/README.md)
