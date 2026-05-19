# Události datatabulek

WebJET publikuje události při operacích s datatabulkami (prováděných přes `DatatableRestControllerV2`). Tyto události využívají Spring mechanismus publikování událostí, více informací naleznete v dokumentaci k [základním událostem](events.md).

Události datatabulek jsou implementovány ve třídě [DatatableEvent](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableEvent.java), která je generická s typem entity. Obsahuje následující atributy:

- `source` - ​​zdrojový objekt entity (např. `UserDetailsEntity`, `WebpagesEntity`)
- `eventType` - ​​[typ události](#typy-událostí)
- `clazz` - ​​jméno třídy is package
- `originalEntity` - ​​původní entita před uložením (pro `AFTER_SAVE`)
- `entityId` - ​​ID smazané entity (pro `AFTER_DELETE`)
- `originalId` - ​​ID původního záznamu při duplikování (pro `AFTER_DUPLICATE`)

## Typy událostí

Typy událostí datatabulek jsou implementovány v `enum` třídě [DatatableEventType](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableEventType.java), aktuálně jsou dostupné následující typy:

- `BEFORE_SAVE` - ​​vyvoláno po zavolání metody `beforeSave()` před uložením entity (vytvoření i úprava)
- `AFTER_SAVE` - ​​vyvolané po zavolání metody `afterSave()` po uložení entity, obsahuje původní i uloženou entitu
- `BEFORE_DELETE` - ​​vyvolané po zavolání metody `beforeDelete()` před smazáním entity (pouze pokud `beforeDelete` vrátil `true`)
- `AFTER_DELETE` - ​​vyvolané po zavolání metody `afterDelete()` po smazání entity, obsahuje ID smazané entity
- `BEFORE_DUPLICATE` - ​​vyvoláno po zavolání metody `beforeDuplicate()` před duplikováním záznamu
- `AFTER_DUPLICATE` - ​​vyvolané po zavolání metody `afterDuplicate()` po duplikování záznamu, obsahuje ID původního záznamu

## Publikování událostí

Události pro datatabulky jsou automaticky publikovány ve třídě [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) při standardních REST operacích (`/add`, `/edit`, `/edit`, bulk update pomocí `editItemByColumn`).

Publikují se na následujících místech:

- V metodě `add()` - ​​hned za voláním `beforeSave()` a `afterSave()`
- V metodě `edit()` - ​​hned za voláním `beforeSave()` a `afterSave()`
- V metodě `delete()` - ​​hned za voláním `beforeDelete()` a `afterDelete()`
- V metodě `editItemByColumn()` - ​​při každém záznamu za voláním `beforeSave()` a `afterSave()`
- V metodě `handleEditor()` - ​​při duplikování za voláním `beforeDuplicate()` a `afterDuplicate()`

**Důležité:** Události jsou publikovány **za** voláním metod `beforeSave`, `afterSave`, atd. To znamená, že i když přepíšete tyto metody ve vaší podtřídě `DatatableRestControllerV2`, událost se stále publikuje. Nemůžete přitom událost "vypnout" přepsáním metody.

## Příklady použití

Pro poslech události potřebujete implementovat třídu s anotací `@Component` a metodou s anotací `@EventListener`. S využitím atributu `condition` se filtrují vyvolané události podle třídy entity a typu události.

Události jsou standardně vyvolány synchronně, čili se provedou ve stejném vlákně. Při událostech typu `BEFORE_SAVE` nebo `BEFORE_DELETE` můžete před uložením modifikovat data entity.

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

Filtrování jen konkrétního typu události:

```java
@EventListener(condition = "#event.eventType.name() == 'AFTER_DELETE' && #event.clazz eq 'sk.iway.iwcm.users.UserDetailsEntity'")
public void handleUserDelete(final DatatableEvent<UserDetailsEntity> event) {
    UserDetailsEntity deletedUser = event.getSource();
    Long deletedId = event.getEntityId();

    Logger.debug(UserDatatableListener.class, "Používateľ bol zmazaný, ID: " + deletedId);

    // Vykonajte potrebné akcie po zmazaní
}
```

Notifikace při smazání

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

Duplikace záznamu

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

Při `AFTER_SAVE` události je důležité vědět, že:

- `event.getSource()` - ​​vrací **uloženou** entitu s nastaveným ID
- `event.getOriginalEntity()` - ​​vrací **původní** odeslanou entitu (pro nové záznamy má ID=0 nebo null)

Příklad:

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

## Rozdíly oproti WebjetEvent

`DatatableEvent` vznikl jako generická verze `WebjetEvent` pro operace s datatabulkami, **kdy použít který:**

- `WebjetEvent` - ​​pro specifické byznys operace, primární práce s web stránkami
- `DatatableEvent` - ​​automaticky dostupné u všech CRUD operací v datatabulkách

## Poznámky

- Události se publikují i ​​při hromadných operacích (import, bulk update), čili může se vyvolat množství událostí najednou
- Při importu desítek/stovek záznamů mohou asynchronní události zatížit systém
- `BEFORE_DELETE` se publikuje pouze pokud metoda `beforeDelete()` vrátí `true`
- Události využívají univerzální `WebjetEventPublisher`, který funguje pro všechny typy Spring `ApplicationEvent`

## Událost DatatableColumnsEvent

Událost [DatatableColumnsEvent](../../../../src/main/java/sk/iway/iwcm/system/datatable/events/DatatableColumnsEvent.java) se publikuje při generování JSON odpovědi pro datatabulku, konkrétně v metodě `getColumnsJson()` třídy [DataTableColumnsFactory](../../../../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java). Tato událost umožňuje dynamicky modifikovat definici sloupců datatabulky před odesláním odpovědi klientovi.

Na rozdíl od `DatatableEvent`, který se vyvolává při CRUD operacích s daty, `DatatableColumnsEvent` se vyvolává při načítání **konfigurace sloupců** datatabulky. Díky tomu můžete dynamicky měnit vlastnosti sloupců (název, viditelnost, formátování apod.) bez nutnosti měnit anotace na JPA entitě.

### Atributy události

- `columns` (`List<DataTableColumn>`) - seznam sloupců datatabulky, které lze modifikovat
- `dto` (`Class<?>`) - třída entity (DTO), pro kterou se generuje JSON
- `clazz` (`String`) - plný název třídy entity (např. `sk.iway.iwcm.components.gallery.GalleryEntity`), používá se pro filtrování v `condition`

### Publikování události

Událost se automaticky publikuje v metodě `getColumnsJson()` třídy `DataTableColumnsFactory`:

```java
if (dto != null) {
    DatatableColumnsEvent event = new DatatableColumnsEvent(columnsSorted, dto);
    event.publishEvent();
}
```

Událost se vyvolá **synchronně** před serializací sloupců do JSON, takže všechny změny provedené v `listener` se projeví v odpovědi.

### Příklady použití

#### Změna názvu sloupce

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

#### Skrytí sloupce podle práv uživatele

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

#### Odstranění sloupce z tabulky

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

### Poznámky

- Událost se publikuje při **každém** načtení konfigurace sloupců datatabulky (typicky při otevření stránky s datatabulkou)
- Změny v `listener` jsou trvalé jen během zpracování požadavku - při dalším načtení se sloupce vygenerují nově z anotací
- Pro filtrování událostí podle entity použijte atribut `condition` s porovnáním `#event.clazz eq 'full.class.Name'`
- `DatatableColumnsEvent` rozšiřuje `ApplicationEvent` (ne `DatatableEvent`), proto se s ním pracuje samostatně