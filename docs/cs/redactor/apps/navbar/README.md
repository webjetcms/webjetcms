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
    public String getNavbar(int groupId, int docId, HttpServletRequest request) {
        return "Custom navbar for group " + groupId + " doc " + docId;
    }

    @Override
    public String getNavbarForNonDefaultDoc(sk.iway.iwcm.doc.DocDetails docDetails, String navbar, HttpServletRequest request) {
        return navbar + ", Custom navbar for non-default doc " + docDetails.getDocId();
    }

}
```

### Nastavení

Po vytvoření vlastní implementace je třeba nastavit konfigurační proměnnou `navbarDefaultType` na plný název třídy (včetně package):

```txt
navbarDefaultType=com.example.custom.CustomNavbar
```

Tato konfigurace se nastavuje v **Nastavení > Konfigurace** v administraci WebJET.

### Standardní implementace

WebJET obsahuje tři standardní implementace:
- [NavbarStandard](../../../../../src/main/java/sk/iway/iwcm/doc/NavbarStandard.java) - standardní textová navigace (hodnota `normal` nebo prázdná)
- [NavbarRDF](../../../../../src/main/java/sk/iway/iwcm/doc/NavbarRDF.java) - navigace ve formátu `RDF` (hodnota `rdf`)
- [NavbarSchemaOrg](../../../../../src/main/java/sk/iway/iwcm/doc/NavbarSchemaOrg.java) - navigace ve formátu `Schema.org` (hodnota `schema.org`)

### Poznámky

- Pokud konfigurační proměnná `navbarDefaultType` obsahuje název třídy (ne standardní hodnoty `normal`, `rdf`, `schema.org`), WebJET se pokusí načíst tuto třídu a použít ji.
- Pokud třída neexistuje nebo neimplementuje `NavbarInterface`, použije se standardní implementace.
- Vlastní třída musí mít veřejný konstruktor bez parametrů.
