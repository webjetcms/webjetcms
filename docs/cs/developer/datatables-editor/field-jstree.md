# Array jsTree - stromová struktura

Datové pole jsTree integruje zobrazení stromové struktury v editoru. Stromová struktura umožňuje používat výběrové pole `checkbox` označit jednotlivé listy stromové struktury.

Zobrazení je viditelné u uživatelů na kartě Práva:

![](field-type-jstree.png)

## Použití anotace

Anotace se používá jako `DataTableColumnType.JSTREE` a lze nastavit následující atributy:
- `data-dt-field-jstree-name` - název objektu se stromovou strukturou, musí být objekt uložen jako `window.name`

Příklad úplné anotace:

```java
@DataTableColumn(inputType = DataTableColumnType.JSTREE, title = "components.user.righrs.user_group_rights", tab = "rightsTab", visible = false, editor = {
    @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-jstree-name", value = "jstreePerms") }) })
private String[] enabledItems;
```

Objekt je třeba v modelu připravit a poté nastavit na hodnotu `window` objekt:

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

a nastavení v souboru pug:

```javascript
window.jstreePerms = [(${jstreePerms})];
```

## Poznámky k implementaci

Pole je implementováno v souboru [field-type-jstree.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-jstree.js).

Vygeneruje kód HTML obsahující vyhledávací pole s možností výběru/nevýběru všech výběrových polí. Když je vyhledávání aktivní, zobrazí se na panelu nástrojů vedle vyhledávacího pole také ikona pro zrušení vyhledávání. Vyhledávání se aktivuje buď kliknutím na ikonu lupy, nebo stisknutím tlačítka `Enter` klíče.

Zde jsme měli problém v tom, že naléhavé `Enter` zachycené komponentou vue `webjet-dte-jstree.vue`, vyřešili jsme to změnou anotace z `<button @click="toggleModals"` na adrese `<button @mouseup="toggleModals"`.

Název objektu JSON se stromovou strukturou se získá z atributu data `data-dt-field-jstree-name` a samotný objekt JSON se načte voláním `let jstreeJsonData = window[objName];`.

Nastavení vybraných výběrových polí `checkbox` se vykonává jako `set(conf, val)`. Nejprve se zruší vyhledávání (aby mezi zavřením a otevřením okna nezůstalo žádné nastavení), zruší se zaškrtnutí všech polí pro výběr a strom se zcela otevře voláním `conf._tree.jstree('open_all');`.

Očekává se, že vybrané hodnoty v poli budou `iteruje` a voláním `conf._tree.jstree('select_node', v)` je zaškrtnuto přiřazené výběrové pole podle názvu. Hodnoty musí být jedinečné, aby nedocházelo k duplicitám na stránce (jedná se o ID prvků stromové struktury), proto doporučujeme používat pro názvy jedinečnou předponu. V objektu Java se předávají jako pole, např. `private String[] enabledItems;`.

Získání vybraných hodnot při odeslání se provádí ve funkci `get(conf)` kde se používá volání API `conf._tree.jstree('get_selected')` získat pole vybraných hodnot.

Pole je v `index.js` vložen a inicializován následujícím kódem:

```javascript
...
import * as fieldTypeJsTree from './field-type-jstree';
...
$.fn.dataTable.Editor.fieldTypes.jsTree = fieldTypeJsTree.typeJsTree();
```

V souboru `_modal.css` jsou soubory stylů CSS, upravují výchozí soubory stylů CSS pro knihovnu jsTree tím, že vynucují použití `Font Awesome` Ikony.
