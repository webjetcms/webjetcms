# Datatable events

WebJET publishes events on datatable operations (performed via `DatatableRestControllerV2`). These events use the Spring event publishing mechanism, more information can be found in the [core events](events.md) documentation.

Datatable events are implemented in the [DatatableEvent](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableEvent.java) class, which is generic with an entity type. It contains the following attributes:

- `source` - ​​source entity object (e.g. `UserDetailsEntity`, `WebpagesEntity`)
- `eventType` - ​​[event type](#event-types)
- `clazz` - ​​class name with package
- `originalEntity` - ​​original entity before saving (for `AFTER_SAVE`)
- `entityId` - ​​Deleted entity ID (for `AFTER_DELETE`)
- `originalId` - ​​ID of the original record when duplicating (for `AFTER_DUPLICATE`)

## Event types

Datatable event types are implemented in the `enum` class [DatatableEventType](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableEventType.java), the following types are currently available:

- `BEFORE_SAVE` - ​​called after calling the `beforeSave()` method before saving the entity (both creation and modification)
- `AFTER_SAVE` - ​​called after calling the `afterSave()` method after saving the entity, contains both the original and saved entity
- `BEFORE_DELETE` - ​​called after calling method `beforeDelete()` before deleting the entity (only if `beforeDelete` returned `true`)
- `AFTER_DELETE` - ​​invoked after calling the `afterDelete()` method after deleting an entity, contains the ID of the deleted entity
- `BEFORE_DUPLICATE` - ​​called after calling method `beforeDuplicate()` before duplicating the record
- `AFTER_DUPLICATE` - ​​called after calling the `afterDuplicate()` method after duplicating a record, contains the ID of the original record

## Publishing events

Events for datatables are automatically published in the [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) class for standard REST operations (`/add`, `/edit`, `/delete`) and also for bulk operations (import, bulk update using `editItemByColumn`).

They are published in the following places:

- In the method `add()` - ​​right after calling `beforeSave()` and `afterSave()`
- In the method `edit()` - ​​right after calling `beforeSave()` and `afterSave()`
- In the method `delete()` - ​​right after calling `beforeDelete()` and `afterDelete()`
- In the method `editItemByColumn()` - ​​for each record after calling `beforeSave()` and `afterSave()`
- In method `handleEditor()` - ​​when duplicating behind calls to `beforeDuplicate()` and `afterDuplicate()`

**Important:** Events are published **after** calling the methods `beforeSave`, `afterSave`, etc. This means that even if you override these methods in your `DatatableRestControllerV2` subclass, the event is still published. You cannot "turn off" the event by overriding the method.

## Usage examples

To listen to an event, you need to implement a class with the `@Component` annotation and a method with the `@EventListener` annotation. Using the `condition` attribute, the triggered events are filtered by entity class and event type.

Events are fired synchronously by default, meaning they are executed in the same thread. For events of type `BEFORE_SAVE` or `BEFORE_DELETE`, you can modify the entity data before saving.

```java
package sk.iway.custom;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.system.datatable.events.DatatableEvent;
import sk.iway.iwcm.system.datatable.events.DatatableEventType;

@Component
public class UserDatatableListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.components.users.userdetail.UserDetailsEntity'")
    public void handleUserSave(final DatatableEvent<UserDetailsEntity> event) {
        if (event.getEventType() == DatatableEventType.BEFORE_SAVE) {
            UserDetailsEntity user = event.getSource();
            Logger.debug(UserDatatableListener.class, "Saving user: " + user.getLogin());

            // modify user data before saving
            if (Tools.isEmpty(user.getFieldA())) {
                user.setFieldA(user.getEditorFields().getLogin() + "@example.com");
            }
        }

        if (event.getEventType() == DatatableEventType.AFTER_SAVE) {
            UserDetailsEntity saved = event.getSource();
            UserDetailsEntity original = event.getOriginalEntity();
            Logger.debug(UserDatatableListener.class, "User saved with ID: " + saved.getId() + " original ID: " + original.getId());
        }
    }
}
```

Filtering only a specific event type:

```java
@EventListener(condition = "#event.eventType.name() == 'AFTER_DELETE' && #event.clazz eq 'sk.iway.iwcm.users.UserDetailsEntity'")
public void handleUserDelete(final DatatableEvent<UserDetailsEntity> event) {
    UserDetailsEntity deletedUser = event.getSource();
    Long deletedId = event.getEntityId();

    Logger.debug(UserDatatableListener.class, "Používateľ bol zmazaný, ID: " + deletedId);

    // Vykonajte potrebné akcie po zmazaní
}
```

Notifications upon deletion

```java
@Component
public class DeleteNotificationListener {

    @Autowired
    private EmailService emailService;

    @EventListener(condition = "#event.eventType.name() == 'AFTER_DELETE' && #event.clazz eq 'sk.iway.custom.ImportantEntity'")
    @Async
    public void notifyOnDelete(final DatatableEvent<ImportantEntity> event) {
        Long deletedId = event.getEntityId();
        ImportantEntity entity = event.getSource();

        // Pošlite email notifikáciu
        emailService.sendNotification("Dôležitý záznam bol zmazaný: " + deletedId);
    }
}
```

Duplicate a record

```java
@Component
public class DuplicateListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.gallery.GalleryEntity'")
    public void handleGalleryDuplicate(final DatatableEvent<GalleryEntity> event) {
        if (event.getEventType() == DatatableEventType.AFTER_DUPLICATE) {
            GalleryEntity newGallery = event.getSource();
            Long originalId = event.getOriginalId();

            // Skopírujte súvisiace dáta z pôvodnej galérie
            Logger.debug(DuplicateListener.class,
                "Galéria bola duplikovaná z ID " + originalId + " na ID " + newGallery.getId());
        }
    }
}
```

In the event of a `AFTER_SAVE` it is important to know that:

- `event.getSource()` - ​​returns the **saved** entity with the set ID
- `event.getOriginalEntity()` - ​​returns the **original** sent entity (for new records it has ID=0 or null)

Example:

```java
@EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.users.UserDetailsEntity'")
public void handleUserAfterSave(final DatatableEvent<UserDetailsEntity> event) {
    if (event.getEventType() == DatatableEventType.AFTER_SAVE) {
        UserDetailsEntity saved = event.getSource();        // ID je nastavené
        UserDetailsEntity original = event.getOriginalEntity();  // ID môže byť null/0

        if (original.getUserId() == null || original.getUserId() < 1) {
            Logger.debug(getClass(), "Bol vytvorený nový používateľ s ID: " + saved.getUserId());
        } else {
            Logger.debug(getClass(), "Bol upravený používateľ s ID: " + saved.getUserId());
        }
    }
}
```

## Differences from WebjetEvent

`DatatableEvent` was created as a generic version of `WebjetEvent` for datatable operations, **when to use which:**

- `WebjetEvent` - ​​for specific business operations, primarily working with websites
- `DatatableEvent` - ​​automatically available for all CRUD operations in data tables

## Notes

- Events are also published during bulk operations (import, bulk update), so many events can be triggered at once
- When importing tens/hundreds of records, asynchronous events can burden the system
- `BEFORE_DELETE` is published only if the method `beforeDelete()` returns `true`
- Events use a universal `WebjetEventPublisher` that works for all Spring types `ApplicationEvent`

## DatatableColumnsEvent event

The [DatatableColumnsEvent](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableColumnsEvent.java) event is published when generating a JSON response for a datatable, specifically in the `getColumnsJson()` method of the [DataTableColumnsFactory](../../../../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java) class. This event allows you to dynamically modify the definition of datatable columns before sending the response to the client.

Unlike `DatatableEvent`, which is called during CRUD operations on data, `DatatableColumnsEvent` is called when loading the **column configuration** of a datatable. This allows you to dynamically change column properties (name, visibility, formatting, etc.) without having to change the annotations on the JPA entity.

### Event attributes

- `columns` (`List<DataTableColumn>`) - list of datatable columns that can be modified
- `dto` (`Class<?>`) - entity class (DTO) for which JSON is generated
- `clazz` (`String`) - full entity class name (e.g. `sk.iway.iwcm.components.gallery.GalleryEntity`), used for filtering in `condition`

### Publish an event

The event is automatically published in the `getColumnsJson()` method of the `DataTableColumnsFactory` class:

```java
if (dto != null) {
    DatatableColumnsEvent event = new DatatableColumnsEvent(columnsSorted, dto);
    event.publishEvent();
}
```

The event is raised **synchronously** before the columns are serialized to JSON, so any changes made to `listener` will be reflected in the response.

### Usage examples

#### Change column name

```java
@Component
public class GalleryColumnsListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.components.gallery.GalleryEntity'")
    public void onDatatablesJson(DatatableColumnsEvent event) {
        event.getColumns().forEach((c) -> {
            if ("descriptionLongSk".equals(c.getName())) {
                c.setTitle("Nový názov stĺpca");
            }
        });
    }
}
```

#### Hiding a column by user rights

```java
@Component
public class ConditionalColumnsListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.custom.MyEntity'")
    public void onDatatablesJson(DatatableColumnsEvent event) {
        Identity user = UsersDB.getCurrentUser(SessionHelper.getRequest());
        if (user == null || user.isAdmin() == false) {
            // non-admin users should not see internal notes column
            event.getColumns().removeIf(c -> "internalNotes".equals(c.getName()));
        }
    }
}
```

#### Removing a column from a table

```java
@Component
public class RemoveColumnListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.custom.MyEntity'")
    public void onDatatablesJson(DatatableColumnsEvent event) {
        // remove column that should not be displayed
        event.getColumns().removeIf(c -> "temporaryField".equals(c.getName()));
    }
}
```

### Notes

- The event is published **every** time the datatable column configuration is loaded (typically when opening a page with a datatable)
- Changes in `listener` are permanent only while the request is being processed - on the next load, the columns will be regenerated from the annotations
- To filter events by entity, use the attribute `condition` with the match `#event.clazz eq 'full.class.Name'`
- `DatatableColumnsEvent` extends `ApplicationEvent` (not `DatatableEvent`), so it is worked with separately