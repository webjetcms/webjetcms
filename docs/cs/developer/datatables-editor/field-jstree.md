# Pole jsTree - stromová struktura

Datové pole jsTree integruje zobrazení stromové struktury v editoru. Stromová struktura umožňuje pomocí výběrového pole `checkbox` označit jednotlivé listy stromové struktury.

Zobrazení je vidět v uživatelích v kartě Práva:

![](field-type-jstree.png)

## Použití anotace

Anotace se používá jako `DataTableColumnType.JSTREE`, přičemž je možné nastavit následující atributy:
- `data-dt-field-jstree-name` - jméno objektu se stromovou strukturou, objekt musí být uložen jako `window.name`

Kompletní příklad anotace:

```java
@DataTableColumn(inputType = DataTableColumnType.JSTREE, title = "components.user.righrs.user_group_rights", tab = "rightsTab", visible = false, editor = {
    @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-jstree-name", value = "jstreePerms") }) })
private String[] enabledItems;
```

Objekt je třeba připravit do modelu a následně nastavit do `window` objektu:

```java
package sk.iway.iwcm.components.users;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

/**
 * Vygeneruje data do modelu pre zobrazenie zoznamu pouzivatelov
 */
@Component
public class UserDetailsListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='users' && event.source.subpage=='user-list'")
    private void setInitalData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            setInitialDataImpl(event.getSource().getModel());
        } catch (Exception ex) {
            Logger.error(UserDetailsListener.class, ex);
        }
    }

    private static void setInitialDataImpl(ModelMap model) throws JsonProcessingException {
        model.addAttribute("jstreePerms", JsonTools.objectToJSON(MenuService.getAllPermissions()));
    }
}
```

a nastavení v pug souboru:

```javascript
window.jstreePerms = [(${jstreePerms})];
```

## Poznámky k implementaci

Pole je implementováno v souboru [field-type-jstree.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-jstree.js).

Generuje HTML kód obsahující vyhledávací pole s možností od/značení všech výběrových polí. Při aktivním vyhledávání se v nástrojové liště vedle vyhledávacího pole zobrazuje i ikona pro zrušení vyhledávání. Vyhledávání se aktivuje buď kliknutím na ikonu lupy, nebo stisknutím `Enter` klávesy.

Tady jsme měli problém v tom, že stisk `Enter` zachytávala vue komponenta `webjet-dte-jstree.vue`, vyřešili jsme to změnou anotace z `<button @click="toggleModals"` na `<button @mouseup="toggleModals"`.

Jméno JSON objektu se stromovou strukturou se získá z data atributu `data-dt-field-jstree-name` a následně se získá samotný JSON objekt voláním `let jstreeJsonData = window[objName];`.

Nastavení zvolených výběrových polí `checkbox` se provede ve funkci `set(conf, val)`. Nejdříve se zruší vyhledávání (aby nezůstalo nastavení mezi zavřením a otevřením okna), odškrtnou se všechna výběrová pole a strom se kompletně otevře voláním `conf._tree.jstree('open_all');`.

Zvolené hodnoty jsou očekávané v poli, které se `iteruje` a voláním `conf._tree.jstree('select_node', v)` se zaškrtne přiřazené výběrové pole podle názvu. Hodnoty je třeba mít unikátní, aby nedošlo k duplikaci ve stránce (jsou to ID elementů stromové struktury), proto doporučujeme použít jedinečný prefix při názvech. V Java objektu jsou přenášeny jako pole. `private String[] enabledItems;`.

Získání zvolených hodnot při odeslání se provede ve funkci `get(conf)` kde je použito API volání `conf._tree.jstree('get_selected')` pro získání pole zvolených hodnot.

Pole je v `index.js` vloženo a inicializováno následujícím kódem:

```javascript
...
import * as fieldTypeJsTree from './field-type-jstree';
...
$.fn.dataTable.Editor.fieldTypes.jsTree = fieldTypeJsTree.typeJsTree();
```

V souboru `_modal.css` jsou CSS styly, ty upravují výchozí CSS styly pro jsTree knihovnu vynucením použití `Font Awesome` ikon.
