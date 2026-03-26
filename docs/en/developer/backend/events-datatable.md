# Datatable events

WebJET publishes events for datatable operations (performed via `DatatableRestControllerV2`). These events use the Spring event publishing mechanism, see the documentation for more information [basic events](events.md).

Datatable events are implemented in the class [DatatableEvent](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableEvent.java) which is generic with the entity type. It contains the following attributes:
- `source` - the source object of the entity (e.g. `UserDetailsEntity`, `WebpagesEntity`)
- `eventType` - [type of event](#event-types)
- `clazz` - class name with package
- `originalEntity` - the original entity before saving (for `AFTER_SAVE`)
- `entityId` - ID of the deleted entity (for `AFTER_DELETE`)
- `originalId` - ID of the original record when duplicating (for `AFTER_DUPLICATE`)

## Types of events

Datatable event types are implemented in `enum` Classroom [DatatableEventType](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableEventType.java), the following types are currently available:
- `BEFORE_SAVE` - called after calling the method `beforeSave()` before saving the entity (creation and modification)
- `AFTER_SAVE` - called after calling the method `afterSave()` after saving the entity, contains both the original and the saved entity
- `BEFORE_DELETE` - called after calling the method `beforeDelete()` before deleting the entity (only if `beforeDelete` returned `true`)
- `AFTER_DELETE` - called after calling the method `afterDelete()` after deleting an entity, contains the ID of the deleted entity
- `BEFORE_DUPLICATE` - called after calling the method `beforeDuplicate()` before duplicating the record
- `AFTER_DUPLICATE` - called after calling the method `afterDuplicate()` after duplicating a record, contains the ID of the original record

## Publishing events

Events for datatables are automatically published in the class [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) for standard REST operations (`/add`, `/edit`, `/delete`) and also for bulk operations (import, bulk update using `editItemByColumn`).

They are published in the following places:
- In the method `add()` - right after the call `beforeSave()` a `afterSave()`
- In the method `edit()` - right after the call `beforeSave()` a `afterSave()`
- In the method `delete()` - right after the call `beforeDelete()` a `afterDelete()`
- In the method `editItemByColumn()` - at each entry after the call `beforeSave()` a `afterSave()`
- In the method `handleEditor()` - when duplicating after a call `beforeDuplicate()` a `afterDuplicate()`

**Important:** Events are published **For** by calling methods `beforeSave`, `afterSave`, etc. This means that even if you override these methods in your subclass `DatatableRestControllerV2`, the event is still being published. You cannot "turn off" the event by overriding the method.

## Examples of use

To listen to an event, you need to implement a class with annotation `@Component` and method with annotation `@EventListener`. Using the attribute `condition` the triggered events are filtered by entity class and event type.

By default, events are invoked synchronously, that is, they are executed in the same thread. For events of type `BEFORE_SAVE` or `BEFORE_DELETE` you can modify the entity data before saving.

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

Filter only a specific type of event:

```java
@EventListener(condition = "#event.eventType.name() == 'AFTER_DELETE' && #event.clazz eq 'sk.iway.iwcm.users.UserDetailsEntity'")
public void handleUserDelete(final DatatableEvent<UserDetailsEntity> event) {
    UserDetailsEntity deletedUser = event.getSource();
    Long deletedId = event.getEntityId();

    Logger.debug(UserDatatableListener.class, "Používateľ bol zmazaný, ID: " + deletedId);

    // Vykonajte potrebné akcie po zmazaní
}
```

Notifications on deletion

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

Duplication of a record

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

At `AFTER_SAVE` events it is important to know that:
- `event.getSource()` - returns **Retrieved from** the entity with the set ID
- `event.getOriginalEntity()` - returns **Original** entity sent (for new records it has ID=0 or null)

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

## Differences compared to WebjetEvent

`DatatableEvent` originated as a generic version of `WebjetEvent` for datatable operations, **when to use which:**

- `WebjetEvent` - for specific business operations, primarily working with websites
- `DatatableEvent` - automatically available for all CRUD operations in datatables

## Notes

- Events are also published in bulk operations (import, bulk update), so many events can be fired at once
- When importing tens/hundreds of records, asynchronous events can overload the system
- `BEFORE_DELETE` is published only if the method `beforeDelete()` Returns `true`
- Events use the universal `WebjetEventPublisher` that works for all types of Spring `ApplicationEvent`
