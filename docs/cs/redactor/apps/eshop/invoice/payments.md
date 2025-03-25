# Platby

Vnořená tabulka Platby v detailu objednávky poskytuje přehled plateb ke konkrétní objednávce. Nad touto tabulkou jsou možné všechny operace s daty.

Požadovaná částka objednávky nemusí být zaplacena najednou, ale může být zaplacena v jednotlivých platbách, což je důvod proč jsou platby vedeny v samostatné tabulce.

![](editor_payments.png)

## Přidání/úprava plateb

![](editor_payments_editor.png)

Okno editoru plateb obsahuje pouze několik parametrů.

**Datum**, automatické nastavené aktuální datum, kdy je platba vytvářena. Tuto hodnotu nelze změnit a při úpravě zůstává nezměněna.

**Způsob platby**, poskytuje na výběr následující možnosti.

![](editor_payments_editor_paymentMethods.png)

**Suma**, částka platby k objednávce. Zadaná částka má svá omezení.

Zadaná částka nemůže být menší než 0.01.

![](editor_payments_editor_minPayment.png)

Součet všech plateb nebo jedna samostatná platba nesmí být větší než celková částka objednávky k úhradě.

![](editor_payments_editor_maxPayment.png)

## Patička tabulky

Patička tabulky obsahuje užitečnou informaci o tom, kolik z celkové částky objednávky je již reálně uhrazeno a také to, jaká je ta celková cena objednávky. Uživatel má tak přehled, kolik ještě musí zaplatit nebo ať už je vše zaplaceno.

![](editor_payments_footer_a.png)

Informace v patičce se automaticky mění v čase při každé úpravě platby, nebo [položky](./items.md#patička-tabulky).
