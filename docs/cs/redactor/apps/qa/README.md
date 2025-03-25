# Otázky a odpovědi

Aplikace Otázky a odpovědi, umožňuje do stránky vložit časté dotazy a odpovědi. Je možné je kategorizovat podle skupiny. Nová otázka přijde na zadaný email.

## Parametry aplikace

- Vložit
  - Otázky s odpověďmi
  - Formulář pro zadání otázky
- Skupina otázek - zadejte název skupiny, ze které se vyberou příspěvky pro zobrazení na stránce
- Počet otázek na stránce (zobrazí se pokud je zvoleno: Otázky s odpověďmi)
- Email pro zasílání otázek (zobrazí se pokud je zvoleno: Formulář pro zadání otázky)

Otázky jsou řazeny do skupin, kterou zadáváte při vkládání aplikace. Pokud potřebujete udělat ještě podskupiny, můžete modifikovat soubor `/components/qa/qa-ask.jsp`, kde nahradíte skryté pole `categoryName` za výběrové menu, pomocí kterého budou návštěvníci moci vybrat podskupinu.

![](webform.png)

## Administrace

Přehled všech příspěvků do sekce Otázky a odpovědi si můžete prohlédnout zvolením položky Otázky a odpovědi v sekci Aplikace. Červenou barvou jsou zobrazeny dotazy, které nemají zadanou odpověď.

![](admin.png)

Klepnutím na dotaz, se vám zobrazí formulář s parametry otázky a odpovědi. Otázku s odpovědí lze kromě zobrazení na stránce i odeslat emailem na adresu zadavatele dotazy. Pokud zadavatel neoznačil možnost **Souhlasím se zveřejněním otázky na webové stránce**, odpověď lze pouze zaslat na jeho email (možnost Zobrazovat na web stránce nechte nezvolenou). Provedené změny potvrdíte kliknutím na Uložit.

![](admin-edit.png)

V případě, že zadáte "Odpověď do e-mailu" a zvolíte možnost **Zobrazovat na web stránce**, tak se text z "Odpověď do e-mailu" automaticky zkopíruje do "Odpověď na stránku".

Aplikace podporuje nastavení [volitelných polí](../../../frontend/webpages/customfields/README.md).
