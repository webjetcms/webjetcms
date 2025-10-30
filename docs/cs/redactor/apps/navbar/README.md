# Navigační lišta

Navigační lišta (navbar/breadcrumb/drobková navigace) zobrazuje ve web stránce klikatelnou cestu k aktuálně zobrazené web stránce. Na názvy adresářů lze kliknout a dostat se jednoduše o úroveň níže. Příklad:

![](navbar.png)

Zobrazení položky v navigační liště je závislé na nastavení pole Navigační lišta v kartě Navigace adresáře web stránek. Má následující možnosti:
- Stejně jako menu - zobrazení v navigační liště se chová stejně jako je nastaveno pole pro zobrazení v menu.
- Zobrazit - položka se v navigační liště zobrazí.
- Nezobrazit - položka se v navigační liště nezobrazí (a to včetně podsložek).

Při možnosti zobrazit můžete ještě zobrazené webové stránce nastavit možnost zobrazení (typicky se jedná o poslední položku v navigační liště). Ta je rovněž v kartě Navigace a obsahuje možnosti:
- Zobrazit - web stránka se v navigační liště zobrazí.
- Nezobrazit - web stránka se v navigační liště nezobrazí.

![](groups-dialog.png)

## Použití

Navigační lišta se vkládá přímo do JSP šablony jako značka:

```html
<iwcm:write name="navbar"/>
```

nebo je možné ji vložit přímo do web stránky jako výraz:

```html
!REQUEST(navbar)!
```

![](editor-dialog.png)

## Vlastní implementace navigační lišty

Pro některé projekty může být nutné vytvořit vlastní implementaci navigační lišty s odlišným formátováním nebo strukturou. WebJET umožňuje definovat vlastní třídu pro generování navigační lišty.

### Vytvoření vlastní implementace

Vlastní implementace musí implementovat rozhraní `sk.iway.iwcm.doc.NavbarInterface`:

```java
package com.example.custom;

import javax.servlet.http.HttpServletRequest;
import sk.iway.iwcm.doc.NavbarInterface;

public class CustomNavbar implements NavbarInterface {
    
    @Override
    public String getNavbarRDF(int groupId, int docId, HttpServletRequest request) {
        // Vlastní implementace pro RDF formát
        return "<div class=\"custom-rdf\">...</div>";
    }

    @Override
    public String getNavbarSchema(int groupId, int docId, HttpServletRequest request) {
        // Vlastní implementace pro Schema.org formát
        return "<ol class=\"custom-schema\">...</ol>";
    }

    @Override
    public String getNavbar(int groupId, int docId, HttpServletRequest request) {
        // Vlastní implementace pro standardní formát
        return "Vlastní navigace...";
    }
}
```

### Nastavení

Po vytvoření vlastní implementace je nutné nastavit konfigurační proměnnou `navbarDefaultType` na plný název třídy (včetně package):

```
navbarDefaultType=com.example.custom.CustomNavbar
```

Tato konfigurace se nastavuje v **Nastavení > Konfigurace** v administraci WebJET.

### Poznámky

- Pokud konfigurační proměnná `navbarDefaultType` obsahuje název třídy (ne standardní hodnoty `normal`, `rdf`, `schema.org`), WebJET se pokusí načíst tuto třídu a použít ji.
- Pokud třída neexistuje nebo neimplementuje `NavbarInterface`, použije se standardní implementace.
- Vlastní třída musí mít veřejný konstruktor bez parametrů.

