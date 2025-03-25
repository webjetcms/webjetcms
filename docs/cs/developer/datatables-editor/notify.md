# Notifikace

Při práci s editorem je z REST služby možné poslat notifikaci, která se zobrazí uživateli.

![](notify.png)

Aktuálně je podporováno zobrazení notifikace při následujících operacích:
- `/all` - získání všech záznamů
- `/{id}` - získání záznamu do editoru (při použití možnosti `fetchOnCreate/fetchOnEdit`)
- `/editor` - uložení záznamu v editoru
- `/action/{action}` - provedení [serverové akce](../datatables/README.md#tlačítko-pro-provedení-serverové-akce) v tabulce

Notifikaci přidáte jednoduše ve vaší REST službě:

```java
addNotify(new NotifyBean(prop.getText("text.warning"), prop.getText("editor.notify.checkHistory"), NotifyBean.NotifyType.WARNING, 15000));

NotifyBean notify = new NotifyBean(prop.getText("editor.approve.notifyTitle"), getProp().getText("editor.approveRequestGet")+": "+notifyText, NotifyBean.NotifyType.INFO, 60000);
addNotify(notify);
```

konstruktor [NotifyBean](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyBean.html) třídy má následující parametry:
- `String title` - titulek notifikace
- `String text` - text notifikace
- `NotifyType type` - typ notifikace (`SUCCESS, INFO, WARNING, ERROR`)
- `long timeout` - počet ms pro automatické zavření notifikace, nebo 0 pro vypnutí automatického zavření

## Přidání tlačítka

Do notifikace je možné přidat tlačítko pro provedení akce (např. načtení poslední stránky z historie). Pomocí API metody [NotifyBean.addButton](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyBean.html) můžete přidat tlačítko (objekt typu [NotifyButton](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyButton.html)). Po kliknutí na tlačítko se provede zadaná JavaScript funkce (vkládá se jako atribut `onlick` na tlačítku). Definování volané funkce musíte implementovat přímo v dané stránce.

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

Notifikace jsou během běhu REST služby ukládány do `ThreadLocal` objektu `ThreadBean`, aby je bylo možné přidat kdykoli. Následně jsou generovány do výstupního JSON objektu:
- pro `DatatableResponse` je seznam notifikací přidán jako `if(hasNotify()) response.setNotify(getThreadData().getNotify());`
- pro vrácenou entitu do `BaseEditorFields` objektu jako `addNotifyToEditorFields(T entity);`, aktuálně jen při volání `getOne`

### Frontend

Zobrazení notifikace je implementováno přímo v `index.js` na 2 místech:
- `EDITOR.on('submitSuccess', function (e, json, data, action)` - volané při uložení v editoru, notifikace se získají z `json.notify`
- `_executeAction(action, ids)` - volané při provedení serverové akce v tabulce, notifikace se získají z `json.notify`
- `refreshRow(id, callback)` - toto volání je použito při `fetchOnCreate/fetchOnEdit`, oznámení se získají z `json.editorFields.notify` objektu

Samotné zobrazení notifikace je implementováno ve funkci `showNotify(notifyList)` která pro každý prvek seznamu oznámení volá zobrazení přes [WJ.notify](../frameworks/webjetjs.md#notifikace)
