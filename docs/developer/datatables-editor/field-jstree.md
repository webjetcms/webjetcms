# Pole jsTree - stromová štruktúra

Dátové pole jsTree integruje zobrazenie stromovej štruktúry v editore. Stromová štruktúra umožňuje pomocou výberového poľa ```checkbox``` označiť jednotlivé listy stromovej štruktúry.

Zobrazenie je vidno v používateľoch v karte Práva:

![](field-type-jstree.png)

## Použitie anotácie

Anotácia sa používa ako ```DataTableColumnType.JSTREE```, pričom je možné nastaviť nasledovné atribúty:

- ```data-dt-field-jstree-name``` - meno objektu so stromovou štruktúrou, objekt musí byť uložený ako ```window.name```

Kompletný príklad anotácie:

```java
@DataTableColumn(inputType = DataTableColumnType.JSTREE, title = "components.user.righrs.user_group_rights", tab = "rightsTab", visible = false, editor = {
    @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-jstree-name", value = "jstreePerms") }) })
private String[] enabledItems;
```

Objekt je potrebné pripraviť do modelu a následne nastaviť do ```window``` objektu:

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

a nastavenie v pug súbore:

```javascript
window.jstreePerms = [(${jstreePerms})];
```

## Poznámky k implementácii

Pole je implementované v súbore [field-type-jstree.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-jstree.js).

Generuje HTML kód obsahujúci vyhľadávacie pole s možnosťou od/značenia všetkých výberových polí. Pri aktívnom vyhľadávaní sa v nástrojovej lište vedľa vyhľadávacieho poľa zobrazuje aj ikona pre zrušenie vyhľadávania. Vyhľadávanie sa aktivuje buď kliknutím na ikonu lupy, alebo stlačením ```Enter``` klávesy.

Tu sme mali problém v tom, že stlačenie ```Enter``` zachytávala vue komponenta ```webjet-dte-jstree.vue```, vyriešili sme to zmenou anotácie z ```<button @click="toggleModals"``` na ```<button @mouseup="toggleModals"```.

Meno JSON objektu so stromovou štruktúrou sa získa z data atribútu ```data-dt-field-jstree-name``` a následne sa získa samotný JSON objekt volaním ```let jstreeJsonData = window[objName];```.

Nastavenie zvolených výberových polí ```checkbox``` sa vykoná vo funkcii ```set(conf, val)```. Najskôr sa zruší vyhľadávanie (aby nezostalo nastavenie medzi zatvorením a otvorením okna), od-škrtnú sa všetky výberové polia a strom sa kompletne otvorí volaním ```conf._tree.jstree('open_all');```.

Zvolené hodnoty sú očakávané v poli, ktoré sa ```iteruje``` a volaním ```conf._tree.jstree('select_node', v)``` sa zaškrtne priradené výberové pole podľa názvu. Hodnoty je potrebné mať unikátne, aby nedošlo k duplikácii v stránke (sú to ID elementov stromovej štruktúry), preto odporúčame použiť jedinečný prefix pri názvoch. V Java objekte sú prenášané ako pole, napr. ```private String[] enabledItems;```.

Získanie zvolených hodnôt pri odoslaní sa vykoná vo funkcii ```get(conf)``` kde je použité API volanie ```conf._tree.jstree('get_selected')``` pre získanie poľa zvolených hodnôt.

Pole je v ```index.js``` vložené a inicializované nasledovným kódom:

```javascript
...
import * as fieldTypeJsTree from './field-type-jstree';
...
$.fn.dataTable.Editor.fieldTypes.jsTree = fieldTypeJsTree.typeJsTree();
```

V súbore ```_modal.css``` sú CSS štýly, tie upravujú predvolené CSS štýly pre jsTree knižnicu vynútením použitia ```Font Awesome``` ikon.