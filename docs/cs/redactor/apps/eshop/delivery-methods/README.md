# Způsoby doručení

Aplikace Způsoby doručení umožňuje nastavit podporované způsoby doručení pro jednotlivé země.

![](datatable.png)

## Konfigurace způsobu doručení

Každý způsob doručení má minimálně tato pole:
- **Způsob doručení**, nezměnitelná hodnota reprezentující typ způsobu doručení. Podporované způsoby doručení jsou definovány programátorsky.
- **Název**, název zobrazený zákazníkovi během objednávky. Můžete zadat překladový klíč. Pokud ponecháte prázdné, použije se způsob doručení.
- **Podporované země**, výběr země (nebo více zemí), pro kterou bude tento způsob doručení dostupný. Seznam podporovaných zemí lze nastavit konfigurační přeměnou `basketInvoiceSupportedCountries`.
- **Cena bez DPH**, hodnota představující cenu doručení bez DPH
- **DPH `[%]`**, hodnota představující sazbu DPH v procentech
- **Cena s DPH**, hodnota představující cenu doručení s DPH (vypočtená automaticky na základě ceny bez DPH a sazby DPH)
- **Pořadí uspořádání**, číselná hodnota k uspořádání způsobu doručení v elektronickém obchodě.

![](editor.png)

Editor **může obsahovat i další pole**, v závislosti na implementaci konkrétního způsobu doručení.

!> Volbu způsobu doručení při vytváření nového záznamu, provádíte pomocí rozbalovacího seznamu v levo horním rohu stránky.

## Validace způsobu doručení

Musíte zvolit alespoň jednu zemi, pro kterou bude tento způsob doručení dostupný.

I při vytváření/úpravě záznamu způsobu doručení nezadáte hodnotu do pole **Cena bez DPH** nebo **DPH `[%]`**, automatický se doplní hodnota 0, čili doručení bude zdarma.

## Nový typ způsobu doručení

Zadefinování nového způsobu (typu) doručení je možné naprogramováním `BackeEnd` funkcionality. Bližší informace [pro programátora](../../../../custom-apps/apps/eshop/delivery-methods/README.md).
