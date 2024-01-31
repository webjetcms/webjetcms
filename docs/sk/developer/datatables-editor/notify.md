# Notifikácie

Pri práci s editorom je z REST služby možné poslať notifikáciu, ktorá sa zobrazí používateľovi.

![](notify.png)

Aktuálne je podporované zobrazenie notifikácie pri nasledovných operáciách:

- ```/all``` - získanie všetkých záznamov
- ```/{id}``` - získanie záznamu do editora (pri použití možnosti ```fetchOnCreate/fetchOnEdit```)
- ```/editor``` - uloženie záznamu v editore
- ```/action/{action}``` - vykonanie [serverovej akcie](../datatables/README.md#tlačidlo-pre-vykonanie-serverovej-akcie) v tabuľke

Notifikáciu pridáte jednoducho vo vašej REST službe:

```java
addNotify(new NotifyBean(prop.getText("text.warning"), prop.getText("editor.notify.checkHistory"), NotifyBean.NotifyType.WARNING, 15000));

NotifyBean notify = new NotifyBean(prop.getText("editor.approve.notifyTitle"), getProp().getText("editor.approveRequestGet")+": "+notifyText, NotifyBean.NotifyType.INFO, 60000);
addNotify(notify);
```

konštruktor [NotifyBean](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyBean.html) triedy má nasledovné parametre:

- ```String title``` - titulok notifikácie
- ```String text``` - text notifikácie
- ```NotifyType type``` - typ notifikácie (```SUCCESS, INFO, WARNING, ERROR```)
- ```long timeout``` - počet ms pre automatické zatvorenie notifikácie, alebo 0 pre vypnutie automatického zatvorenia

## Pridanie tlačidla

Do notifikácie je možné pridať tlačidlo na vykonanie akcie (napr. načítanie poslednej stránky z histórie). Pomocou API metódy [NotifyBean.addButton](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyBean.html) môžete pridať tlačidlo (objekt typu [NotifyButton](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyButton.html)). Po kliknutí na tlačidlo sa vykoná zadaná JavaScript funkcia (vkladá sa ako atribút ```onlick``` na tlačidle). Definovanie volanej funkcie musíte implementovať priamo v danej stránke.

Príklad:

```java
NotifyBean notify = new NotifyBean(prop.getText("text.warning"), prop.getText("editor.notify.checkHistory"), NotifyBean.NotifyType.WARNING, 15000);
notify.addButton(new NotifyButton(
    prop.getText("editor.notify.editFromHistory"), //text tlacidla
    "btn btn-primary", //CSS trieda
    "far fa-pencil", //FontAwesome ikona
    "editFromHistory("+history.get(0).getDocId()+", "+history.get(0).getHistoryId()+")") //onclick funkcia
);
addNotify(notify);
```

## Implementačné detaily

### Backend

Notifikácie sú počas behu REST služby ukladané do ```ThreadLocal``` objektu ```ThreadBean```, aby ich bolo možné pridať kedykoľvek. Následne sú generované do výstupného JSON objektu:

- pre ```DatatableResponse``` je zoznam notifikácií pridaný ako ```if(hasNotify()) response.setNotify(getThreadData().getNotify());```
- pre vrátenú entitu do ```BaseEditorFields``` objektu ako ```addNotifyToEditorFields(T entity);```, aktuálne len pri volaní ```getOne```

### Frontend

Zobrazenie notifikácie je implementované priamo v ```index.js``` na 2 miestach:

- ```EDITOR.on('submitSuccess', function (e, json, data, action)``` - volané pri uložení v editore, notifikácie sa získajú z ```json.notify```
- ```_executeAction(action, ids)``` - volané pri vykonaní serverovej akcie v tabuľke, notifikácie sa získajú z ```json.notify```
- ```refreshRow(id, callback)``` - toto volanie je použité pri ```fetchOnCreate/fetchOnEdit```, notifikácie sa získajú z ```json.editorFields.notify``` objektu

Samotné zobrazenie notifikácie je implementované vo funkcii ```showNotify(notifyList)``` ktorá pre každý prvok zoznamu notifikácie volá zobrazenie cez [WJ.notify](../frameworks/webjetjs.md#notifikácie)

