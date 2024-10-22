# Seznam uživatelů

## Backend

Backend je implementován v balíčku `sk.iway.iwcm.components.users`, řadič REST je ve třídě `UserDetailsController`.

### Třídy pro generování seznamu práv

Karta Práva obsahuje stromovou strukturu v jstree zobrazující jednotlivá práva. Seznam všech práv je generován v `MenuService.getAllPermissions()`. Použití třídy MenuService není šťastné, ale protože sdílí několik metod pro úpravu práv z verze WebJET 8 na verzi 2021, bylo pro nás jednodušší použít ji i pro seznam práv.

První úroveň představují stejné sekce jako v menu, druhou jednotlivé moduly a třetí práva v rámci modulů (hodnoty `leftSubmenuXItemKey` v `modinfo.properties`).

Specifické je generování tříd CSS `permgroup` a `permgroup-ID ` podle skupiny práv, která zobrazuje barevné kroužky u každého práva obsaženého ve skupině práv.

Pokud se má zobrazit 3. úroveň stromové struktury (modul obsahuje dílčí práva), položka na 2. úrovni se upraví - k jejímu ID se přidá přípona. `-leaf` aby byla položka jedinečná a zároveň byla přidána se stejným názvem do 3. úrovně nabídky.

Údaje pro stromovou strukturu práv se do modelu vkládají v položce `UserDetailsListener` Stejně jako `model.addAttribute("jstreePerms", JsonTools.objectToJSON(MenuService.getAllPermissions()));`.

Práva přidělená uživateli se přenášejí v. `String[] UserDetailsEditorFields.enabledItems`. Zde je seznam povolených uživatelských práv. Technicky však WebJET ukládá do databáze pouze nepovolená (zakázaná) práva, a to z historických důvodů. V metodách `fromUserDetailsEntity` a `toUserDetailsEntity` seznam práv je obrácený.

## Frontend

Zobrazení seznamu uživatelů je v souboru [user-list.pug](../../../../src/main/webapp/admin/v9/views/pages/users/user-list.pug). Obsahuje specifické funkce JS pro zobrazení barevných kruhů spárovaných se skupinami práv. Kruhy se používají k zobrazení skupiny práv, která obsahuje jednotlivé právo.

![](../../datatables-editor/field-type-jstree.png)

Okruhy skupin událostí a práv jsou inicializovány při prvním otevření okna pomocí příkazu `usersDatatable.EDITOR.on('opened', function (e, type, action)`. Pomocí proměnné `permGroupsColorBinded` inicializace je zajištěna pouze při prvním otevření.

V proměnné `niceColors` je seznam barev kruhů (pro zobrazení stejných barev podle pořadí skupin práv), byly použity barvy z Finderu v systému MacOS. Pokud je skupin práv více, než je velikost pole, jsou další barvy generovány náhodně voláním funkce `randomColor`.

Nejprve se seznam skupin práv prochází voláním `$(".DTE_Field_Name_editorFields\\.permGroups input").each(function(index)`. ID skupiny se získá ze vstupního pole a přiřazeného `label` prvek jejího jména. První písmeno názvu skupiny je pro lepší přehlednost zobrazeno v kroužcích. Současně se podle ID skupiny práv vygeneruje definice souboru stylů CSS, která se vloží do prvku `head` prvek.

Po získání seznamu skupin práv jsou ke každému prvku LI jsTree přidány kruhy podle stylů CSS voláním `$("#DTE_Field_editorFields-enabledItems li.permgroup").each(function(index)`. Styly CSS jsou přidány pro každý prvek stromové struktury na backendu v části `MenuService.getAllPermissions()` kde každý prvek LI obsahuje třídy CSS `permgroup permgroup-ID`. Přechod podle třídy CSS `permgroup` Kód HTML se generuje uvnitř prvku LI s barevnými kroužky skupin práv.

Kliknutí na políčko pro výběr skupiny práv se provádí v režimu `$(".DTE_Field_Name_editorFields\\.permGroups").on("click", "input", function() {` a způsobí přidání třídy CSS `permgroup-ID-checked` na adrese `body` prvek. To následně způsobí, že se kruh představující skupinu práv plně vybarví.
