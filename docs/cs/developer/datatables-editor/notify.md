# Oznámení

Při práci s editorem je možné ze služby REST odeslat oznámení, které se zobrazí uživateli.

![](notify.png)

V současné době jsou podporovány následující operace pro zobrazení oznámení:
- `/all` - získání všech záznamů
- `/{id}` - získání záznamu do editoru (při použití příkazu `fetchOnCreate/fetchOnEdit`)
- `/editor` - uložení záznamu v editoru
- `/action/{action}` - provedení [akce serveru](../datatables/README.md#tlačítko-pro-provedení-akce-serveru) v tabulce

Stačí přidat oznámení do služby REST:

```java
addNotify(new NotifyBean(prop.getText("text.warning"), prop.getText("editor.notify.checkHistory"), NotifyBean.NotifyType.WARNING, 15000));

NotifyBean notify = new NotifyBean(prop.getText("editor.approve.notifyTitle"), getProp().getText("editor.approveRequestGet")+": "+notifyText, NotifyBean.NotifyType.INFO, 60000);
addNotify(notify);
```

konstruktor [NotifyBean](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyBean.html) třída má následující parametry:
- `String title` - titulek oznámení
- `String text` - text oznámení
- `NotifyType type` - typ oznámení (`SUCCESS, INFO, WARNING, ERROR`)
- `long timeout` - počet ms pro automatické zavření oznámení nebo 0 pro vypnutí automatického zavírání.

## Přidání tlačítka

Do oznámení lze přidat tlačítko pro provedení akce (např. načtení poslední stránky z historie). Použití metody API [NotifyBean.addButton](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyBean.html) můžete přidat tlačítko (typ objektu [NotifyButton](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyButton.html)). Po kliknutí na tlačítko se spustí zadaná funkce JavaScriptu (je vložena jako atribut `onlick` na tlačítku). Definici volané funkce musíte implementovat přímo ve stránce.

Příklad:

```java
NotifyBean notify = new NotifyBean(prop.getText("text.warning"), prop.getText("editor.notify.checkHistory"), NotifyBean.NotifyType.WARNING, 15000);
notify.addButton(new NotifyButton(
    prop.getText("editor.notify.editFromHistory"), //text tlacidla
    "btn btn-primary", //CSS trieda
    "ti ti-pencil", //TablerIcons ikona
    "editFromHistory("+history.get(0).getDocId()+", "+history.get(0).getHistoryId()+")") //onclick funkcia
);
addNotify(notify);
```

## Implementační detaily

### Backend

Oznámení se ukládají do služby REST za jejího běhu. `ThreadLocal` objekt `ThreadBean` aby je bylo možné kdykoli přidat. Poté se vygenerují do výstupního objektu JSON:
- Pro `DatatableResponse` seznam oznámení je přidán jako `if(hasNotify()) response.setNotify(getThreadData().getNotify());`
- pro vrácenou entitu na `BaseEditorFields` objekt jako `addNotifyToEditorFields(T entity);`, aktuální pouze při volání `getOne`

### Frontend

Zobrazení oznámení je implementováno přímo v `index.js` na 2 místech:
- `EDITOR.on('submitSuccess', function (e, json, data, action)` - volán při ukládání v editoru, oznámení se načítají z adresáře `json.notify`
- `_executeAction(action, ids)` - zavolá, když je v tabulce provedena akce serveru, oznámení jsou získávána z. `json.notify`
- `refreshRow(id, callback)` - toto volání se používá, když `fetchOnCreate/fetchOnEdit`, oznámení jsou získávána z `json.editorFields.notify` objekt

Samotné zobrazení oznámení je implementováno ve funkci `showNotify(notifyList)` který pro každý prvek seznamu oznámení vyvolá zobrazení prostřednictvím [WJ.notify](../frameworks/webjetjs.md#oznámení)
