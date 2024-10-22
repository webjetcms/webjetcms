# Navigační panel

Navigační panel (navbar / breadcrumb / breadcrumb navigation) zobrazuje na webové stránce cestu, na kterou lze kliknout, k aktuálně zobrazené webové stránce. Kliknutím na názvy adresářů se lze snadno dostat do další úrovně níže. Příklad:

![](navbar.png)

Zobrazení položky na navigačním panelu závisí na nastavení pole Navigační panel na kartě Navigace v adresáři webových stránek. Má následující možnosti:
- Stejně jako menu - zobrazení v navigačním panelu se chová stejně, jako je nastaveno pole zobrazení v menu.
- Zobrazit - položka se zobrazí na navigačním panelu.
- Nezobrazovat - položka se nezobrazí na navigačním panelu (včetně podsložek).

U možnosti zobrazení můžete nastavit možnost zobrazení pro webovou stránku, která je stále zobrazena (obvykle se jedná o poslední položku na navigačním panelu). Tato možnost se nachází rovněž na kartě Navigace a obsahuje tyto možnosti:
- Zobrazit - webová stránka se zobrazí na navigačním panelu.
- Nezobrazovat - webová stránka se nezobrazí na navigačním panelu.

![](groups-dialog.png)

## Použití

Navigační panel je vložen přímo do šablony JSP jako značka:

```html
<iwcm:write name="navbar"/>
```

nebo jej lze vložit přímo do webové stránky jako výraz:

```html
!REQUEST(navbar)!
```

![](editor-dialog.png)
