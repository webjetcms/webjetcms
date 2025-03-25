# Seznam uživatelů

## Backend

Backend je implementován v package `sk.iway.iwcm.components.users`, REST controller je ve třídě `UserDetailsController`.

### Třídy pro generování seznamu práv

V kartě Práva se nachází stromová struktura ve jstree zobrazující jednotlivá práva. Seznam všech práv se generuje v `MenuService.getAllPermissions()`. Použití třídy MenuService není šťastné, jelikož ale sdílí několik metod pro úpravu práv z WebJET verze 8 do verze 2021 zdálo se nám jednodušší použít ji i pro seznam práv.

První úroveň reprezentují sekce stejné jako jsou v menu, druhou jednotlivé moduly a třetí práva v rámci modulů (hodnoty `leftSubmenuXItemKey` v `modinfo.properties`).

Specifické je generování CSS tříd `permgroup` a `permgroup-ID ` podle skupin práv, pomocí kterých se zobrazují barevné kruhy u jednotlivých práv obsažených ve skupině práv.

Pokud se má zobrazit i 3. úroveň ve stromové struktuře (modul obsahuje pod práva) je upravena položka na 2. úrovni - k jejímu ID je doplněna přípona `-leaf` aby položka byla jedinečná a zároveň je doplněna se stejným názvem i do 3. úrovně menu.

Data pro stromovou strukturu práv jsou do modelu vložena v `UserDetailsListener` jak `model.addAttribute("jstreePerms", JsonTools.objectToJSON(MenuService.getAllPermissions()));`.

Práva, která uživatel má přiřazena se přenášejí v `String[] UserDetailsEditorFields.enabledItems`. Zde je seznam povolených práv uživatele. Technicky ale WebJET z historických důvodů ukládá do databáze jen nepovolená (zakázaná) práva. V metodách `fromUserDetailsEntity` a `toUserDetailsEntity` se seznam práv invertuje.

## Frontend

Zobrazení seznamu uživatelů je v souboru [user-list.pug](../../../../src/main/webapp/admin/v9/views/pages/users/user-list.pug). Obsahuje specifické JS funkce pro zobrazení barevných kruhů spárovaných se skupinami práv. Pomocí kruhů se v jednotlivých právech zobrazuje skupina práv, která dané jednotlivé právo obsahuje.

![](../../datatables-editor/field-type-jstree.png)

Události a kruhy skupin práv se inicializují při prvním otevření okna přes funkci `usersDatatable.EDITOR.on('opened', function (e, type, action)`. Pomocí proměnné `permGroupsColorBinded` se zajistí inicializace jen při prvním otevření.

V proměnné `niceColors` je seznam barev kruhů (aby se podle pořadí skupin práv zobrazily ve stejných barvách), použity byly barvy z Finderu v MacOS. Je-li skupin práv více než velikost pole generují se další barvy náhodně voláním funkce `randomColor`.

Nejprve se prochází seznam skupin práv voláním `$(".DTE_Field_Name_editorFields\\.permGroups input").each(function(index)`. Ze vstupního pole se získá ID skupiny a z přiřazeného `label` elementu její jméno. V kruzích se zobrazuje pro lepší přehled první písmeno ze jména skupiny. Zároveň se generuje definice CSS stylů podle ID skupiny práv, která se následně vloží do `head` elementu.

Po získání seznamu skupin práv se do každého LI elementu jsTree doplní kruhy podle CSS stylů voláním `$("#DTE_Field_editorFields-enabledItems li.permgroup").each(function(index)`. CSS styly jsou pro každý element stromové struktury přidány na backende v `MenuService.getAllPermissions()`, kde každý LI element obsahuje CSS třídy `permgroup permgroup-ID`. Přechodem podle CSS třídy `permgroup` se vygeneruje dovnitř LI elementu HTML kód s barevnými kruhy skupin práv.

Klepnutí na výběrové pole skupiny práv je zpracováno v `$(".DTE_Field_Name_editorFields\\.permGroups").on("click", "input", function() {` a způsobí přidání CSS třídy `permgroup-ID-checked` na `body` elemente. To následně způsobí plné zabarvení kruhu reprezentující skupinu práv.
