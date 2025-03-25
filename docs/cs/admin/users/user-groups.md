# Skupiny uživatelů

Sekce **Skupiny uživatelů** zobrazuje seznam skupin, do kterých můžete kategorizovat návštěvníky/uživatele/příjemce hromadného emailu.

Systémově rozlišujeme dva typy skupin:
- **Přístupů k zaheslované sekci web sídla** - skupina pro přístupová práva k neveřejným sekcím web sídla
- **Přihlášení k hromadnému e-mailu** - skupina pro distribuční seznam pro posílání hromadných emailů

Tabulka nabízí rychlé akce nad skupinami pomocí tlačítek:
- ![](user-groups-addGroupToAll.png ":no-zoom"), **Přidat zvolenou skupinu všem uživatelům** - zvolené skupiny budou přidány všem stávajícím uživatelům.
- ![](user-groups-removeGroupFromAll.png ":no-zoom"), **Odebrat zvolenou skupinu všem uživatelům** - zvolené skupiny budou odebrány všem stávajícím uživatelům.
- ![](user-groups-removeUsersFromGroup.png ":no-zoom"), **Smazat všechny uživatele zvolené skupiny** - uživatelé kteří mají alespoň jednu ze zvolené skupiny budou vymazáni.

![](user-groups-datatable.png)

## Editor

V dialogovém okně editoru pro definování skupiny máte následující možnosti:
- **Název** - jednoznačný název skupiny uživatelů.
- **Typ skupiny** - typ skupiny, buď přístupová práva pro skupinu pro kontrolu práv, nebo skupinu pro hromadný email.
- **Poznámka** - libovolná poznámka.
- **Vyžaduje schválení** - pokud je zaškrtnuto tak po zaregistrování uživatele do této skupiny se nejprve zašle email ke schválení registrace administrátorovi. Uživatel se vytvoří v databázi, nebude mít ale zvolené pole Schválen a nebude se moci přihlásit
- **Povolit přidávání/odebírání ze skupiny samotným uživatelům** - pokud je nastaveno, tak skupinu si bude moci přidat/odebrat sám uživatel. Obvykle se to nastavuje pro e-mail skupiny, aby sám uživatel si mohl nastavit do jakých distribučních seznamů bude přihlášen. Pro skupiny s přístupovými právy ponechte nezaškrtnuté.
- **Vyžadovat potvrzení e-mailové adresy** - pokud je zvoleno bude při registraci uživatele přes registrační formulář zaslán odkaz na potvrzení emailové adresy. Teprve po kliknutí na odkaz v email zprávě bude uživatel do skupiny přiřazen.
- **ID stránky s textem emailu** - výběr stránky s textem emailu, který se uživateli zašle po jeho schválení.
- **Sleva z ceny v %** - procentuální sleva pro uživatele v této skupině. Sleva pro uživatele může být využita v různých aplikacích jako jsou například [Rezervace](../../redactor/apps/reservation/reservations/README.md).

![](user-groups-editor.png)
