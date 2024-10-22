# Vymazání dat

Aplikace GDPR obsahuje uzel "Odstranit data", který slouží k odstranění starých dat z databáze.

Umožňuje mazání:
- Účty **neaktivní uživatelé** (ti, kteří se nepřihlásili za posledních X dní, základ je nastaven na 730 dní = 2 roky)
- **formuláře** (záznamy starší než X dní, základ nastaven na 730dní = 2roky)
- **objednávky z elektronického obchodu** (záznamy starší než X dní, základna nastavena na 10 let)
- **e-maily** (mazání dat z hromadného odesílání e-mailů, základna nastavena na 186 dní)

![](data-deleting-dataTable.png)

V datové tabulce není možné vytvářet nové záznamy, jejich počet je pevně stanoven funkcí WebJET CMS. Při editaci lze měnit pouze číselnou hodnotu **Období** který udává, za jaké období budou hodnoty při provedení akce zadány. **Odstranění dat** Vymazáno.

![](data-deleting-editor.png)

Všechny akce uživatele WebJET při mazání dat jsou auditovány (typ `GDPR_DELETE_*`) a je možné získat informace o tom, jaké bylo ID operace, kdo, kdy a kolik dat smazal.
