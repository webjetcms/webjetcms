# Udalosti datatabuliek

WebJET publikuje udalosti pri operáciách s datatabuľkami (vykonávaných cez `DatatableRestControllerV2`). Tieto udalosti využívajú Spring mechanizmus publikovania udalostí, viac informácií nájdete v dokumentácii k [základným udalostiam](events.md).

Udalosti datatabuliek sú implementované v triede [DatatableEvent<T>](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableEvent.java), ktorá je generická s typom entity. Obsahuje nasledovné atribúty:

- `source` - zdrojový objekt entity (napr. `UserDetailsEntity`, `WebpagesEntity`)
- `eventType` - [typ udalosti](#typy-udalostí)
- `clazz` - meno triedy aj s package
- `originalEntity` - pôvodná entita pred uložením (pre `AFTER_SAVE`)
- `entityId` - ID zmazanej entity (pre `AFTER_DELETE`)
- `originalId` - ID pôvodného záznamu pri duplikovaní (pre `AFTER_DUPLICATE`)

## Typy udalostí

Typy udalostí datatabuliek sú implementované v `enum` triede [DatatableEventType](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableEventType.java), aktuálne sú dostupné nasledovné typy:

- `BEFORE_SAVE` - vyvolané po zavolaní metódy `beforeSave()` pred uložením entity (vytvorenie aj úprava)
- `AFTER_SAVE` - vyvolané po zavolaní metódy `afterSave()` po uložení entity, obsahuje pôvodnú aj uloženú entitu
- `BEFORE_DELETE` - vyvolané po zavolaní metódy `beforeDelete()` pred zmazaním entity (len ak `beforeDelete` vrátil `true`)
- `AFTER_DELETE` - vyvolané po zavolaní metódy `afterDelete()` po zmazaní entity, obsahuje ID zmazanej entity
- `BEFORE_DUPLICATE` - vyvolané po zavolaní metódy `beforeDuplicate()` pred duplikovaním záznamu
- `AFTER_DUPLICATE` - vyvolané po zavolaní metódy `afterDuplicate()` po duplikovaní záznamu, obsahuje ID pôvodného záznamu

## Publikovanie udalostí

Udalosti pre datatabuľky sú automaticky publikované v triede [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) pri štandardných REST operáciách (`/add`, `/edit`, `/delete`) a taktiež pri hromadných operáciách (import, bulk update pomocou `editItemByColumn`).

Publikujú sa na nasledovných miestach:

- V metóde `add()` - hneď za volaním `beforeSave()` a `afterSave()`
- V metóde `edit()` - hneď za volaním `beforeSave()` a `afterSave()`
- V metóde `delete()` - hneď za volaním `beforeDelete()` a `afterDelete()`
- V metóde `editItemByColumn()` - pri každom zázname za volaním `beforeSave()` a `afterSave()`
- V metóde `handleEditor()` - pri duplikovaní za volaním `beforeDuplicate()` a `afterDuplicate()`

**Dôležité:** Udalosti sú publikované **za** volaním metód `beforeSave`, `afterSave`, atď. To znamená, že aj keď prepíšete tieto metódy vo vašej podtriede `DatatableRestControllerV2`, udalosť sa stále publikuje. Nemôžete pritom udalosť "vypnúť" prepísaním metódy.

## Príklady použitia

Pre počúvanie udalosti potrebujete implementovať triedu s anotáciou `@Component` a metódou s anotáciou `@EventListener`. S využitím atribútu `condition` sa filtrujú vyvolané udalosti podľa triedy entity a typu udalosti.

Udalosti sú štandardne vyvolané synchrónne, čiže sa vykonajú v rovnakom vlákne. Pri udalostiach typu `BEFORE_SAVE` alebo `BEFORE_DELETE` môžete modifikovať údaje entity pred uložením.

```java
package sk.iway.custom;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
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

Filtrovanie len konkrétneho typu udalosti:

```java
@EventListener(condition = "#event.eventType.name() == 'AFTER_DELETE' && #event.clazz eq 'sk.iway.iwcm.users.UserDetailsEntity'")
public void handleUserDelete(final DatatableEvent<UserDetailsEntity> event) {
    UserDetailsEntity deletedUser = event.getSource();
    Long deletedId = event.getEntityId();

    Logger.debug(UserDatatableListener.class, "Používateľ bol zmazaný, ID: " + deletedId);

    // Vykonajte potrebné akcie po zmazaní
}
```

Notifikácie pri zmazaní

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

Duplikácia záznamu

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

Pri `AFTER_SAVE` udalosti je dôležité vedieť, že:

- `event.getSource()` - vracia **uloženú** entitu s nastaveným ID
- `event.getOriginalEntity()` - vracia **pôvodnú** odoslanú entitu (pre nové záznamy má ID=0 alebo null)

Príklad:

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

## Rozdiely oproti WebjetEvent

`DatatableEvent` vznikol ako generická verzia `WebjetEvent` pre operácie s datatabuľkami, **kedy použiť ktorý:**

- `WebjetEvent` - pre špecifické biznis operácie, primárne práca s web stránkami
- `DatatableEvent` - automaticky dostupné pri všetkých CRUD operáciách v datatabuľkách

## Poznámky

- Udalosti sa publikujú aj pri hromadných operáciách (import, bulk update), čiže môže sa vyvolať množstvo udalostí naraz
- Pri importe desiatok/stoviek záznamov môžu asynchrónne udalosti zaťažiť systém
- `BEFORE_DELETE` sa publikuje len ak metóda `beforeDelete()` vráti `true`
- Udalosti využívajú univerzálny `WebjetEventPublisher`, ktorý funguje pre všetky typy Spring `ApplicationEvent`
