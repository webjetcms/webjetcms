# WebJET Events

WebJET uses Spring to publish and listen to events. A basic description can be found at [baeldung](https://www.baeldung.com/spring-events). Both synchronous and asynchronous events are supported.

Events are typically implemented generically using the `WebjetEvent` class, which is a universal event container. It contains the following attributes:

- `source` - ​​event source object (e.g. `GroupDetails`, `DocDetails`)
- `eventType` - ​​[event type](#event-types)
- `clazz` - ​​class name and package for [event filtering](#listening-events) when listening

## Event types

For standard operations, event types are implemented in the `enum` class `WebjetEventType`, the following types are currently available:

- `ON_START` - ​​called at the beginning of the method, at this point you can modify the object's data via a synchronous event
- `AFTER_SAVE` - ​​called after saving an object, you have access to an already saved object
- `ON_DELETE` - ​​called before deleting an object
- `AFTER_DELETE` - ​​called after deleting an object
- `ON_XHR_FILE_UPLOAD` - ​​called after uploading a file via URL address `/XhrFileUpload`
- `ON_END` - ​​called at the end of the method, used when no saving is performed (i.e. `AFTER_SAVE` is not called), but only some action
- `ON_RECOVER` - ​​called at the beginning of the method in the process of restoring a web page or folder from the trash. You have access to the object being restored.
- `AFTER_RECOVER` - ​​called after restoring a web page or folder from the Recycle Bin. You have access to an already restored object.

## Current published events

Currently WebJET publishes the following events:

- Web pages - saving a web page - the object `DocDetails` is published both before and after saving in the page editor when calling `EditorFacade.save`, condition: `#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`. You can verify whether this is publishing the page or just saving a working version in the attribute `doc.getEditorFields().isRequestPublish()`, which returns the value `false` if it is a working version of the page.
- Web pages - deleting a web page - object `DocDetails` is published both before and after deletion when calling `DeleteServlet.deleteDoc`, condition: `"event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`.
- Web pages - saving and deleting a directory - the object `GroupDetails` is published both before and after saving when calling `GroupsDB.setGroup` and `GroupsDB.deleteGroup`, which should be used for standard operations with the web page directory. Condition: `#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'`.
- Web pages - display of a web page on the frontend - the object `ShowDocBean` is published after obtaining the `DocDetails` object (event `ON_START`) and before routing to the JSP template, the event `ON_END` is published. At `ON_START`, it is possible to set the attribute `forceShowDoc` to the `DocDetails` object that will be used to display the page, the attribute `doc` is empty for now. Only `docId` is set. At event `ON_END`, the `DocDetails` object is displayed in the attribute `doc`. Condition: `#event.clazz eq 'sk.iway.iwcm.doc.ShowDocBean'`.
- Web pages - when publishing a page at a time - the object `DocumentPublishEvent` is published, which contains `DocDetails` of the published web page and the attribute `oldVirtualPath` with information about the original URL address of the page (to detect whether it changed during publishing). Condition `#event.clazz eq 'sk.iway.iwcm.system.spring.events.DocumentPublishEvent'`, event `ON_PUBLISH`.
- Web pages - at the beginning of the process of restoring a web page or folder from the trash, an event `ON_RECOVER` is published, which contains the restored object. After the restoration is complete, an event `AFTER_RECOVER` is published, which contains the restored page or folder object. When restoring a folder, the event is published only for the given folder, individual events for subfolders and web pages located in this folder are not published.
- Configuration - creating and changing a configuration variable - the object `ConfDetails` is published after saving the value via the user interface by calling `ConfDB.setName`, condition: `#event.clazz eq 'sk.iway.iwcm.system.ConfDetails'`.
- File upload - object `File` is published as `WebjetEvent<File> fileWebjetEvent = new WebjetEvent<>(tempfile, WebjetEventType.ON_XHR_FILE_UPLOAD);`, condition: `#event.clazz eq 'java.io.File'`.
- Updating codes in the text - object `UpdateCodesEvent` is published after processing standard codes in method `DocTools.updateCodes`, allows adding custom codes. Condition: `#event.clazz eq 'sk.iway.iwcm.system.spring.events.UpdateCodesEvent'`, event `ON_START` and `ON_END` for the possibility of replacing codes before WebJET processing and after processing.

## Listening to an event

To listen to an event, you need to implement a class with the annotation `@Component` and a method with the annotation `@EventListener`. Using the attribute `condition`, the triggered events are filtered. Theoretically (according to the instructions), a correctly set generic type is enough, but in practice this did not help and the event was also triggered with a generic type other than the set one.

### Synchronous events

Events are by default invoked synchronously, meaning that both the event invocation and the methods that listen to the event are executed within the same thread. This effectively allows for data modification in events before they are saved.

Example of listening to a synchronous event:

```java
package sk.iway...;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class SaveListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'")
    public void handleGroupSave(final WebjetEvent<GroupDetails> event) {

    }
}
```

In the `condition` attribute, it is possible to filter events by `clazz` as well as by `eventType`, for example:

```java
@EventListener(condition = "#event.eventType.name() == 'AFTER_SAVE' && event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
public void handleAfterSaveEditor2(final WebjetEvent<DocDetails> event) {
}
```

### Asynchronous events

If the method implementing the event listener does not need to modify data, or its duration is long, it is advisable to implement it asynchronously. In this case, it is executed in a separate thread, the original event trigger does not wait for its completion.

The setting is provided in the `BaseSpringConfig` WebJET class using the `@EnableAsync` annotation. The method that listens to the event and is to be executed asynchronously needs the `@Async` annotation.

When you get `Thread.currentThread().getName()`, you can see that this is a separate thread, different from the standard `http` thread.

Example:

```java
package sk.iway...;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class SaveListenerAsync {

   @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'")
   @Async
   public void handleGroupSaveAsync(final WebjetEvent<GroupDetails> event) {

      Logger.debug(SaveListenerAsync.class, "================================================= handleAfterSave GROUP ASYNC type=" + event.getEventType() + ", source=" + event.getSource().getClass()+" thread="+Thread.currentThread().getName());
   }
}

```

## Publish an event

Event publishing is provided by the `WebjetEventPublisher` class, but for ease of use, the `WebjetEvent` class directly contains the `publishEvent` method. Example of publishing an event for an `GroupDetails` object:

```java
public boolean setGroup(GroupDetails group)
{
    (new WebjetEvent<GroupDetails>(group, WebjetEventType.ON_START)).publishEvent();
    ...
    (new WebjetEvent<GroupDetails>(newGroup, WebjetEventType.AFTER_SAVE)).publishEvent();
}
```

Typically, the event type `WebjetEventType.ON_START` should be invoked at the beginning of the method and `WebjetEventType.AFTER_SAVE` at the end (after saving the data).

## Updating codes in text

If you need to add custom codes to the page text (e.g. `!CUSTOM_CODE!`), you can use the `ON_START` or `ON_END` event for `UpdateCodesEvent`. These events are published before and after the standard codes are processed in the `DocTools.updateCodes` method.

Example of implementing your own `listener`:

```java
package sk.iway.custom;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UpdateCodesEvent;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;

@Component
public class CustomCodesListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.common.UpdateCodesEvent'")
    public void handleUpdateCodes(final WebjetEvent<UpdateCodesEvent> event) {
        if (event.getEventType() != WebjetEventType.ON_START) {
            return; // only process ON_START event, skip ON_END
        }
        UpdateCodesEvent updateCodesEvent = event.getSource();
        StringBuilder text = updateCodesEvent.getText();

        // Add custom code
        text = Tools.replace(text, "!CUSTOM_CODE!", "My Company VAT ID");
        text = Tools.replace(text, "!COMPANY_NAME!", "My Company Ltd.");

        // Set the processed text back
        updateCodesEvent.setText(text);
    }
}
```

In this example, the listener listens to the `ON_START` event and replaces its own codes in the text. You can access all the parameters from `UpdateCodesEvent`:

- `text` - ​​page text (modifiable)
- `user` - ​​currently logged in user
- `currentDocId` - ​​ID of the current page
- `request` - ​​HTTP request
