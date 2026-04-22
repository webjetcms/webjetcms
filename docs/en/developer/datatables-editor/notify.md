# Notifications

When working with the editor, it is possible to send a notification from the REST service that will be displayed to the user.

![](notify.png)

Currently, displaying notifications is supported for the following operations:

- ```/all``` - ​​get all records
- ```/{id}``` - ​​getting a record into the editor (when using the ```fetchOnCreate/fetchOnEdit``` option)
- ```/editor``` - ​​save record in editor
- ```/action/{action}``` - ​​execute [server action](../datatables/README.md#button-for-execute-server-action) in the table

You can easily add a notification in your REST service:

```java
addNotify(new NotifyBean(prop.getText("text.warning"), prop.getText("editor.notify.checkHistory"), NotifyBean.NotifyType.WARNING, 15000));

NotifyBean notify = new NotifyBean(prop.getText("editor.approve.notifyTitle"), getProp().getText("editor.approveRequestGet")+": "+notifyText, NotifyBean.NotifyType.INFO, 60000);
addNotify(notify);
```

The constructor of the [NotifyBean](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyBean.html) class has the following parameters:

- ```String title``` - ​​notification title
- ```String text``` - ​​notification text
- ```NotifyType type``` - ​​notification type (```SUCCESS, INFO, WARNING, ERROR```)
- ```long timeout``` - ​​number of ms for automatic closing of the notification, or 0 to disable automatic closing

## Adding a button

It is possible to add a button to a notification to perform an action (e.g. loading the last page from the history). Using the API method [NotifyBean.addButton](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyBean.html) you can add a button (object of type [NotifyButton](../../../javadoc/sk/iway/iwcm/system/datatable/NotifyButton.html)). After clicking the button, the specified JavaScript function is executed (it is inserted as the ```onlick``` attribute on the button). You must implement the definition of the called function directly in the given page.

Example:

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

## Implementation details

### Backend

Notifications are stored in the ```ThreadLocal``` object ```ThreadBean``` during the REST service runtime so that they can be added at any time. They are then generated into the output JSON object:

- for ```DatatableResponse``` the notification list is added as ```if(hasNotify()) response.setNotify(getThreadData().getNotify());```
- for the returned entity to the ```BaseEditorFields``` object as ```addNotifyToEditorFields(T entity);```, currently only when calling ```getOne```

### Frontend

Notification display is implemented directly in ```index.js``` in 2 places:

- ```EDITOR.on('submitSuccess', function (e, json, data, action)``` - ​​called when saving in the editor, notifications are obtained from ```json.notify```
- ```_executeAction(action, ids)``` - ​​called when a server action is performed on a table, notifications are obtained from ```json.notify```
- ```refreshRow(id, callback)``` - ​​this call is used for ```fetchOnCreate/fetchOnEdit```, notifications are obtained from the ```json.editorFields.notify``` object

The notification display itself is implemented in the function ```showNotify(notifyList)``` which calls the display via [WJ.notify](../frameworks/webjetjs.md#notifications) for each element of the notification list.

