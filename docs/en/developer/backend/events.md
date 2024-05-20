# Events

WebJET uses Spring to publish and listen to events. A basic description can be found at [baeldung](https://www.baeldung.com/spring-events). Both synchronous and asynchronous events are supported.

Events are typically implemented generically using the class `WebjetEvent`which is a universal carrier of events. It contains the following attributes:
- `source` - the source object of the event (e.g. `GroupDetails`, `DocDetails`)
- `eventType` - [type of event](#typy-udalostí)
- `clazz` - the name of the class also with the package for [event filtering](#počúvanie-udalosti) while listening

## Types of events

For standard operations, event types are implemented in `enum` Classroom `WebjetEventType`, the following types are currently available:
- `ON_START` - called at the beginning of the method, at this point you can modify the object data via the synchronous event
- `AFTER_SAVE` - invoked after saving an object, you have access to the already saved object
- `ON_DELETE` - invoked before deleting the object
- `AFTER_DELETE` - invoked after deleting the object
- `ON_XHR_FILE_UPLOAD` - invoked after uploading a file via URL `/XhrFileUpload`
- `ON_END` - called at the end of the method, it is used when no saving is performed (i.e. it is not called `AFTER_SAVE`), but just some action

## Current published events

Currently WebJET publishes the following events:
- Web page - saving a web page - an object is published `DocDetails` before and after saving in the page editor when calling `EditorFacade.save`, condition: `#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`. You can check whether you are publishing a page or just saving a working version in the attribute `doc.getEditorFields().isRequestPublish()`which returns the value `false` if it is a working version of the page.
- Web page - delete web page - object is published `DocDetails` before and after deletion when calling `DeleteServlet.deleteDoc`, condition: `"event.clazz eq 'sk.iway.iwcm.doc.DocDetails'`.
- Web pages - saving and deleting the directory - the object is published `GroupDetails` before and after saving when calling `GroupsDB.setGroup` a `GroupsDB.deleteGroup`which should be used for standard web page directory operations. Prerequisite: `#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'`.
- Web pages - displaying the web page on the frontend - the object is published `ShowDocBean` after obtaining `DocDetails` object (event `ON_START`) and an event is published before routing to the JSP template `ON_END`. At `ON_START` attribute can be set `forceShowDoc` at `DocDetails` the object to be used to display the page, the attribute `doc` is empty for now. It is only set `docId`. In an event `ON_END` is in the attribute `doc` Retrieved from `DocDetails` object. Condition: `#event.clazz eq 'sk.iway.iwcm.doc.ShowDocBean'`.
- Web page - when the page is published in time - the object is published `DocumentPublishEvent`which contains `DocDetails` published web page and attribute `oldVirtualPath` with information about the original URL of the page (to detect if it has changed during publishing). Prerequisite `#event.clazz eq 'sk.iway.iwcm.system.spring.events.DocumentPublishEvent'`, event `ON_PUBLISH`.
- Configuration - create and change a configuration variable - an object is published `ConfDetails` after saving the value via the user interface by calling `ConfDB.setName`, condition: `#event.clazz eq 'sk.iway.iwcm.system.ConfDetails'`.
- Uploading a file - an object is published `File` Like `WebjetEvent<File> fileWebjetEvent = new WebjetEvent<>(tempfile, WebjetEventType.ON_XHR_FILE_UPLOAD);`, condition: `#event.clazz eq 'java.io.File'`.

## Listening event

To listen to an event, you need to implement a class with annotation `@Component` and method with annotation `@EventListener`. Using the attribute `condition` the triggered events are filtered. Theoretically (according to the instructions) a correctly set generic type is enough, but practically this did not help and the event was fired with a different generic type than the set one.

### Synchronous events

By default, events are invoked as synchronous, that is, within the same thread, both the invocation of the event and the methods that listen for the event are executed. Effectively, this makes it possible to modify data in events before saving.

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

In the attribute `condition` it is possible to filter events by `clazz` also according to `eventType`, for example:

```java
@EventListener(condition = "#event.eventType.name() == 'AFTER_SAVE' && event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
public void handleAfterSaveEditor2(final WebjetEvent<DocDetails> event) {
}
```

### Asynchronous events

If the method implementing event listening does not need to modify the data or its duration is long, it is advisable to implement it asynchronously. In this case it is executed in a separate thread, the original event trigger does not wait for it to complete.

The setting is provided in the classroom `BaseSpringConfig` WebJET using annotation `@EnableAsync`. A method that listens for an event and is to be executed asynchronously needs an annotation `@Async`.

When obtaining `Thread.currentThread().getName()` it can be seen that this is a separate thread, different from the standard `http` fibers.

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

## Publishing an event

Event publishing is provided by the class `WebjetEventPublisher`, for ease of use but directly class `WebjetEvent` contains a method `publishEvent`. Example of publishing an object event `GroupDetails`:

```java
public boolean setGroup(GroupDetails group)
{
    (new WebjetEvent<GroupDetails>(group, WebjetEventType.ON_START)).publishEvent();
    ...
    (new WebjetEvent<GroupDetails>(newGroup, WebjetEventType.AFTER_SAVE)).publishEvent();
}
```

Typically, raising an event of type `WebjetEventType.ON_START` should be at the beginning of the method and `WebjetEventType.AFTER_SAVE` at the end (after saving the data).
