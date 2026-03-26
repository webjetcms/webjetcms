# DatatableRestControllerV2.java

Java class [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) encapsulates communication with [datatable](README.md) a [To the editor](../datatables-editor/README.md).

## Basic implementation

When implementing a specific REST service, it is necessary to extend DatatableRestControllerV2. V **ideally using Spring DATA repositories** a Java implementation class can look like this:

```java
package sk.iway.iwcm.components.redirects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;


@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/redirect")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
public class RedirectRestController extends DatatableRestControllerV2<RedirectBean, Long> {

    @Autowired
    public RedirectRestController(RedirectsRepository redirectsRepository) {
        super(redirectsRepository, RedirectBean.class);
    }
}
```

important is the constructor that passes the Spring DATA repository `RedirectsRepository` and the entity class to be used when creating the new record. This will also set the values when the new record is created. Annotation `@Datatable` ensure correct error message handling and annotation `PreAuthorize` used to control rights:

```java
@Datatable
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
```

!>**Warning:** Spring DATA entity must have as PK `Long id` (not e.g. `adminlogId` and so on), if the column in the database is called differently, it is necessary to set the column name:

```java
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_adminlog_notify")
@Column(name = "adminlog_notify_id")
@DataTableColumn(inputType = DataTableColumnType.ID)
private Long id;
```

!>**Warning:** in the data entity **You MUST NOT use primitive types** Like `int`, `long` but the objects `Integer`, `Long`, otherwise the search will not work. This is used by ExampleMatcher, which does not insert NULL objects into the DB query. But for primitive types it cannot use NULL, it sets them to 0 and then adds them to the `WHERE` conditions.

Comprehensive demonstration including rights checking, deleting files and performing a special action:

```java
package sk.iway.iwcm.components.gallery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.admin.upload.UploadService;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UsersDB;

import javax.servlet.http.HttpServletRequest;

/**
 * GalleryRestController
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/components/gallery")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuGallery')")
public class GalleryRestController extends DatatableRestControllerV2<GalleryEntity, Long> {

    private final HttpServletRequest request;

    @Autowired
    public GalleryRestController(GalleryRepository galleryRepository, HttpServletRequest request) {
        super(galleryRepository);
        this.request = request;
    }

    /**
     * Metoda na kontrolu prav pouzivatela
     *
     * @param request
     * @return
     */
    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        Identity currentUser = UsersDB.getCurrentUser(request);
        return currentUser.isEnabledItem("menuGallery");
    }


    /**
     * Prikladova metoda na validaciu dat odoslanych z datatables editora.
     * Metoda je volana pre kazdy odoslaby objekt.
     * Chyby pridava do error objeku pomocou {@link Errors}.rejectValue
     *
     * @param request
     * @param user
     * @param errors
     * @param id
     * @param entity
     */
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, GalleryEntity> target, Identity user, Errors errors, Long id, GalleryEntity entity) {
        if (!user.isFolderWritable(entity.getImagePath())) {
            //objekt errorField je nase T, v tomto pripade GalleryEntity, setuje sa presna property kde sa ma error vypisat
            errors.rejectValue("errorField.imagePath", null, Prop.getInstance(request).getText("user.rights.no_folder_rights"));
            //pre globalnu chybu (netykajucu sa konkretneho fieldu) je mozne pouzit kod:
            //((BindingResult)errors).addError(new ObjectError("global", Prop.getInstance(request).getText("datatable.error.fieldErrorMessage")));
        }
    }

    /**
     * Vykonanie akcie otocenia obrazka
     */
    @Override
    public boolean processAction(GalleryEntity entity, String action) {
        String imageUrl = entity.getImagePath() + "/" + entity.getImageName();
        int status = -1;

        if ("rotate".equals(action)) {
            status = ImageTools.rotateImage(imageUrl, 90);
        }

        return status == 0;
    }

    /**
     * Pri zmazani entity z DB je potrebne zmazat aj subory z disku
     */
    public boolean beforeDelete(GalleryEntity entity) {
        UploadService uploadService = new UploadService(entity.getImagePath() + "/" + entity.getImageName(), request);
        return uploadService.processDelete();
    }
}
```

## Methods for data manipulation

You can also construct the class with `NULL` repository, in which case you need to implement methods to work with the data:

```java
/**
 * Vlozi NOVU entitu do databazy
 * @param entity
 * @return
 */
public T insertItem(T entity)

/**
 * Ulozi existujucu entitu do databazy
 * @param entity
 * @param id
 * @return
 */
public T editItem(T entity, long id)

/**
 * metoda pre ziskanie entity s rovnakou hodnotou v stlci propertyName ako hodnota v obj
 * @param propertyName
 * @param obj
 * @return
 * @throws IllegalAccessException
 * @throws NoSuchMethodException
 * @throws InvocationTargetException
 */
public List<T> findItemBy(String propertyName, T original)

/**
 * Zmaze danu entitu z databazy
 * @param entity
 * @param id
 * @return
 */
public boolean deleteItem(T entity, long id)

/**
 * Ziska z databazy entitu so zadanym id
 * @param id
 * @return
 */
public T getOneItem(long id)

/**
 * Ziska z databazy vsetky zaznamy
 * @param pageable
 * @return
 */
public Page<T> getAllItems(Pageable pageable)

/**
 * Vrati vsetky zaznamy, pricom vykona volanie metody addSpecSearch,
 * cize je mozne pouzit URL parametre na filtrovanie vsetkych zaznamov.
 * @param empty - prazdny objekt (je potrebny kvoli vytvoreniu instance)
 * @param pageable
 * @return
 */
public Page<T> getAllItemsIncludeSpecSearch(T empty, Pageable pageable) {

/**
 * Do objektu searchProperties naplni hladane vyrazy, vrati pripadne upraveny ExampleMatcher
 * @param params
 * @param searchProperties - vratena mapa request parametrov pre vyhladavanie
 * @param searchWrapped
 * @param matcher - ak sa jedna o exampleMatcher, moze byt null
 * @param isExampleSearch
 * @return
 */
public ExampleMatcher getSearchProperties(Map<String, String> params, Map<String, String> searchProperties, BeanWrapperImpl searchWrapped, ExampleMatcher matcher, boolean isExampleSearch) {

/**
 * Vyhlada objekty podla zadaneho search objektu a pripadnych parametrov z requestu
 * @param params
 * @param pageable
 * @param search
 * @return
 */
public Page<T> searchItem(@RequestParam Map<String, String> params, Pageable pageable, T search)
```

you can use existing APIs in the implementation, e.g.:

```java
@Override
public Page<DomainRedirectBean> getAllItems(Pageable pageable) {
    List<DomainRedirectBean> listedBeans = DomainRedirectDB.getAllRedirects();
    return new DatatablePageImpl<>(listedBeans);
}

@Override
public DomainRedirectBean insertItem(DomainRedirectBean entity) {
    DomainRedirectDB.insert(entity);
    return entity;
}

@Override
public DomainRedirectBean editItem(DomainRedirectBean entity, long id) {
    entity.setRedirectId((int)id);
    return DomainRedirectDB.update(entity);
}

@Override
public boolean deleteItem(DomainRedirectBean entity, long id) {
    DomainRedirectDB.delete((int)id);
    return true;
}

@Override
public Page<T> searchItem(Map<String, String> params, Pageable pageable, T search) {

}
```

If you need to edit the data before saving, or perform an action after saving to the database (e.g. set the save date, or `domainId`) use the method `beforeSave` or `afterSave`:

```java

    /**
	 * Metoda volana pred insert/save danej entity,
	 * da sa pouzit na nastavenie udajov, napr. datum ulozenia, domainId a podobne
	 * @param entity
	 */
    @Override
    public void beforeSave(InsertScriptBean entity) {
        //nastav datum ulozenia
        entity.setSaveDate(new Date());

        //nastav korektne domainId
        int domainId = CloudToolsForCore.getDomainId();
        entity.setDomainId(domainId);
        //nastav aj na inner objekty
        if (entity.getGroupIds()!=null) {
            for (InsertScriptGroupBean isg : entity.getGroupIds()) {
                isg.setDomainId(domainId);
            }
        }
    }

    /**
	 * Metoda volana pred duplikovanim danej entity,
	 * da sa pouzit na resetovanie udajov, napr. priradena default stranka adresara a podobne
	 * @param entity
	 */
	public void beforeDuplicate(T entity) {

	}

    /**
	 * Metoda volana po duplikovanim danej entity,
	 * da sa pouzit na dokopirovanie udajov, napr. media web stranky
	 * @param entity - novo ulozena (zduplikovana) entita
	 * @param originalId - ID povodneho zaznamu ktory sa duplikoval
	 */
	public void afterDuplicate(T entity, Long originalId) {

	}

    /**
	 * Metoda volana po ulozeni entity.
	 * POZOR: pre novo vytvaranu entitu bude jej ID ulozene len v saved entite, povodna entity bude mat ID=0
	 * @param entity - povodna odoslana entita
	 * @param saved - uz ulozena verzia entity
	 */
    @Override
    public void afterSave(UserDetailsEntity entity, UserDetails saved) {
        Integer userId = saved.getId().intValue();
        ...
    }

    /**
	 * Metoda volana po zmazanim enity z DB, moze vykonat dodatocne akcie
	 * napr. zmazanie suborov z disku, ulozenie do archivu,
	 * alebo obnovu cache objektov
	 * @param entity
	 * @return
	 */
    @Override
    public void afterDelete(UserGroupsEntity entity, long id) {
        UserGroupsDB.getInstance(true);
    }

    /**
	 * Metoda sa vola pri importe po kazdom chunku
	 * @param chunk - aktualny chunk
	 * @param totalChunks - celkovy pocet chunkov
	 */
	public void afterImportChunk(int chunk, int totalChunks) {

	}
```

Method `public void afterSave(T entity, T saved)` is called after saving the entity - object `entity` is the original sent object, `saved` is the saved version. When a new record is `ID` found only in `saved` entity. If you use methods to update the cache, don't forget to implement the public method `void afterDelete(T entity, long id)` called after deleting an entry.

!>**Warning:** we do not recommend overwriting via annotation `@Override` REST methods, always override in your class `xxxItem` Methods.

When duplicating a record, it is necessary to "delete" the attribute ID value from the received data. Typically, this is an attribute with the name `id`, but this may not always be the case. So the attribute name is searched according to the set annotation `DataTableColumnType.ID`.

When using [nested/additional attributes](../datatables-editor/datatable-columns.md#nested-attributes) in the form of `editorFields` it is possible to implement methods `processFromEntity` for setting `editorFields` attributes or `processToEntity` for setting attributes in entity z `editorFields`. Methods are automatically called when reading all records, retrieving a single record, searching, or saving data.

```java
	/**
	 * Vykona upravy v entite pred vratenim cez REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from entity to editorFields)
	 * @param entity
	 * @param action - typ zmeny - create,edit,getall...
	 */
	public T processFromEntity(T entity, ProcessItemAction action) {
		return entity;
	}

	/**
	 * Vykona upravy v entite pri zapise cez REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from editorFields to entity)
	 * @param entity
	 * @param action - typ zmeny - create,edit,getall,
	 */
	public T processToEntity(T entity, ProcessItemAction action) {
		return entity;
	}
```

When overriding methods `getAllItems` or `searchItem` it is necessary to invoke the method `processFromEntity` on each element of the returned list. Effectively, you can call the method `processFromEntity(Page<T> page, ProcessItemAction action)` Above `Page` object, which for all records from `Page` of the object calls the method `processFromEntity(T entity, ProcessItemAction action)`.

## Filtering when displaying all records

Sometimes it is necessary to filter the data even when displaying all records (e.g. in a nested datatable by some group or by user rights).

There is a method `Page<T> DatatableRestControllerV2.getAllItemsIncludeSpecSearch(T empty, Pageable pageable)` that you can use in your implementation. Calling it will ensure that the methods are executed `addSpecSearch` even when all the records have been retrieved. You can implement additional conditions there.

```java
@Override
public Page<UserDetailsEntity> getAllItems(Pageable pageable) {

    DatatablePageImpl<UserDetailsEntity> page = new DatatablePageImpl<>(getAllItemsIncludeSpecSearch(new UserDetailsEntity(), pageable));

    page.addOptions("editorFields.emails", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL), "userGroupName", "userGroupId", false);
    page.addOptions("editorFields.permisions", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);
    page.addOptions("editorFields.enabledItems", moduleItems, "itemKey", "itemKey", false);
    page.addOptions("editorFields.permGroups", (new PermissionGroupDB()).getAll(), "title", "id", false);

    return page;
}

@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<UserDetailsEntity> root, CriteriaBuilder builder) {

    Identity user = UsersDB.getCurrentUser(getRequest());

    //Check user permissions and return for this user editable entities
    if(!user.isEnabledItem("users.edit_admins")  && user.isEnabledItem("users.edit_public_users")) {
        predicates.add(builder.isFalse(root.get("admin")));
    } else if(user.isEnabledItem("users.edit_admins")  && !user.isEnabledItem("users.edit_public_users")) {
        predicates.add(builder.isTrue(root.get("admin")));
    } else if(!user.isEnabledItem("users.edit_admins")  && !user.isEnabledItem("users.edit_public_users")) {
        //If user doesnt have admin and public perm in same time
        predicates.add(builder.isFalse(root.get("admin")));
        predicates.add(builder.isTrue(root.get("admin")));
    }

    SpecSearch<UserDetailsEntity> specSearch = new SpecSearch<>();
    String permissions = params.get("searchEditorFields.permisions");
    if (permissions!=null) {
        specSearch.addSpecSearchPasswordProtected(permissions, "userGroupsIds", predicates, root, builder);
    }
    String emails = params.get("searchEditorFields.emails");
    if (emails!=null) {
        specSearch.addSpecSearchPasswordProtected(emails, "userGroupsIds", predicates, root, builder);
    }
    int userGroupId = Tools.getIntValue(params.get("userGroupId"), -1);
    if (userGroupId>0) {
        specSearch.addSpecSearchPasswordProtected(userGroupId, "userGroupsIds", predicates, root, builder);
    }

    super.addSpecSearch(params, predicates, root, builder);
}
```

## Non-existing attributes in the editor

By default, not all attributes of an entity need to come from the editor, so the values of the existing entity and the data sent from the editor are merged before saving. By default, all non `null` Attributes. This does not allow you to enter a blank date (once it has been set). Therefore, attributes of the annotated DataTableColumn type `Date` are transferred even if they have `null` Value. This connection is made in the method `public T editItem(T entity, long id)` using `NullAwareBeanUtils.copyProperties(entity, one);`.

## Recovering data after saving

By calling the method `setForceReload(true);` it is possible to force the datatable data to be restored after saving.

This is necessary if the saved object is moved to another directory and so on. The sample is in [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java).

## Dials for select boxes

For select boxes in the editor and automatic conversion of ID to value (e.g. `templateId` on the template name) is added in WebJET extension `PageImpl` object [DatatablePageImpl.java](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatablePageImpl.java), which allows you to send dial data.

These are automatically set to `options` of the object in the columns definition of the object editor and are automatically used for the ID-value conversion.

An example is in [TranslationKeyController.java](../../../../src/main/java/sk/iway/iwcm/components/translation_keys/rest/TranslationKeyController.java) a [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java):

```java
    //najlepsie riesenie je prepisat metodu getOptions a doplnit page.addOptions metody
    @Override
    getOptions(DatatablePageImpl<T> page) {
        LayoutService ls = new LayoutService(getRequest());
        page.addOptions("lng", ls.getLanguages(false, true), "label", "value", false);
    }

    //Ukazka pouzitia so standardnym Spring DATA repozitarom a prepisanim metody getAllItems
    @Override
    public Page<TranslationKeyEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<TranslationKeyEntity> page = new DatatablePageImpl<>(translationKeyService.getTranslationKeys(getRequest(), pageable));
        LayoutService ls = new LayoutService(getRequest());
        page.addOptions("lng", ls.getLanguages(false, true), "label", "value", false);
        return page;
    }

    //Ukazka pouzitia bez Spring DATA repozitara a prepisanim metody getAll
    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {
        int groupId = Tools.getIntValue(request.getParameter("groupId"), Constants.getInt("rootGroupId"));

        Prop prop = Prop.getInstance(request);
        WebpagesService ws = new WebpagesService(groupId, prop);

        DatatablePageImpl<DocDetails> page = new DatatablePageImpl<>(ws.getPages());
        page.addOptions("tempId", ws.getTemplates(), "tempName", "tempId", true);
        page.addOptions("menuDocId,rightMenuDocId", ws.getMenuList(), "title", "docId", false);
        page.addOptions("headerDocId,footerDocId", ws.getDocList(), "title", "docId", false);

        return page;
    }
```

When calling `page.addOptions` sets the third `true/false` parameter adding the original `Java Beanu` to the output object. If set to `true`, in the JSON object, the original Bean will be available in the original object. This can be used to retrieve data e.g. templates:

```javascript
setCssStyle() {
    //console.log("this.json.options=", this.datatable.DATA.json);
    for (let template of this.datatable.DATA.json.options.tempId) {
        //console.log("Testing temp: ", template.label, "id=", template.original.tempId, " json.tempId=", this.json.tempId);
        if (template.original.tempId == this.json.tempId) {
            ...
        }
    }
}
```

## Search

There are 2 search modes:
- `ByExample` - uses the method `Example<T> exampleQuery = Example.of(search, matcher);` which sets the search conditions to `search` entity and then use it for the search. All attributes in the entity must be objects, those that have `NULL` value will not be used in the search. Such a search does not support all options.
- `Specification` - uses dynamic creation of SQL conditions, the repository must extend `JpaSpecificationExecutor<T>`. This is **recommended solution** and allows you to search by date ranges and also use special searches.

Warning. **Oracle** database, you need to set up a search regardless of font size and diacritics by setting SQL. By default, this is provided by `trigger tgg_after_logon_ci_ai` that WebJET automatically creates. If the search does not work correctly, verify that it is correctly defined:

```sql
CREATE or REPLACE TRIGGER tgg_after_logon_ci_ai
    AFTER LOGON ON SCHEMA
    DECLARE
    BEGIN
        EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_SORT=BINARY_AI NLS_COMP=LINGUISTIC';
    END;
```

Oracle does not support searching regardless of font size and diacritics in `clob` columns. But for this case WebJET will use the function `LOWER` before the search, which will allow searching regardless of case. The list of JPA attribute names for which the function will be used is defined in the conf. variable `jpaToLowerFields`. A prerequisite is the use of a repository that extends `JpaSpecificationExecutor<T>`.

### Search by date range

To search by date range, you need to combine a search by Example and a specific condition. DT in `app.js` sends a value with a prefix `daterange:`, according to which the search in `DatatableRestControllerV2.searchItem`. The combining of values in the method will be used:

```java
private Specification<T> getSpecFromRangeAndExample(Map<String, String> ranges, Example<T> example) {
```

It is a condition that the Spring DATA repository also extends the `JpaSpecificationExecutor`, an example is in [RedirectsRepository](../../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

```java
package sk.iway.iwcm.components.redirects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RedirectsRepository extends JpaRepository<RedirectBean, Long>, JpaSpecificationExecutor<RedirectBean> {
}
```

!>**Warning:** all JPA repositories must be defined as public, otherwise they will not be available in client projects.

### Special search

If a special search needs to be performed, it is possible to override the method `addSpecSearch` in which a specific condition can be implemented. An example is in [GroupSchedulerRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerRestController.java) where you search by both user name and parameter:

```java
@Override
@SuppressWarnings("unchecked")
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<AuditLogEntity> root, CriteriaBuilder builder) {
    String searchUserFullName = params.get("searchUserFullName");
    if (Tools.isNotEmpty(searchUserFullName)) {
        //ziskaj zoznam IDecok userov, ktory maju dane meno
        List<Integer> userIds = (new SimpleQuery()).forListInteger("SELECT DISTINCT user_id FROM users WHERE first_name LIKE ? OR last_name LIKE ?", "%"+searchUserFullName+"%", "%"+searchUserFullName+"%");
        if (userIds.size()>0) predicates.add(root.get("userId").in(userIds));
    }
    //pridaj vyhladavanie podla modu
    String mode = getRequest().getParameter("mode");
    if ("history".equals(mode)) predicates.add(builder.isNull(root.get("datePrepared")));
    else predicates.add(builder.isNotNull(root.get("datePrepared")));
}
```

!>**Warning:** The JPA Repository must also inherit from `JpaSpecificationExecutor`, Example:

```java
@Repository
public interface GroupSchedulerDtoRepository extends JpaRepository<GroupSchedulerDto, Long>, JpaSpecificationExecutor<GroupSchedulerDto> {

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNotNull(Pageable pageable, Long groupId);

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNull(Pageable pageable, Long groupId1);
}
```

#### Supporting methods

In addition to the general method `addSpecSearch` (which internally calls the following special methods) it is possible to use methods from the class [SpecSearch](../../../javadoc/sk/iway/iwcm/system/datatable/SpecSearch.html):

**Search by name/surname**

Search by a given name in `paramValue`, where the found user IDs are searched for as IN in `jpaProperty`:

`addSpecSearchUserFullName(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`

example of use:

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder) {

    SpecSearch<DocDetails> specSearch = new SpecSearch<>();

    //vyhladanie na zaklade Meno autora, hladane v DB tabulke nasledne v stlpci authorId
    String searchAuthorName = params.get("searchAuthorName");
    if (searchAuthorName != null) {
        specSearch.addSpecSearchUserFullName(searchAuthorName, "authorId", predicates, root, builder);
    }

    super.addSpecSearch(params, predicates, root, builder);

}
```

**Search by comma-separated list**

To search by a comma-separated list of IDs (e.g. user groups), you can use the method `addSpecSearchPasswordProtected(String userGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)` to search by user group name, or the method `addSpecSearchPasswordProtected(Integer userGroupId, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)` to search by the specified ID:

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<UserDetailsEntity> root, CriteriaBuilder builder) {

    SpecSearch<UserDetailsEntity> specSearch = new SpecSearch<>();
    String permissions = params.get("searchEditorFields.permisions");
    if (permissions!=null) {
        specSearch.addSpecSearchPasswordProtected(permissions, "userGroupsIds", predicates, root, builder);
    }
    String emails = params.get("searchEditorFields.emails");
    if (emails!=null) {
        specSearch.addSpecSearchPasswordProtected(emails, "userGroupsIds", predicates, root, builder);
    }
    int userGroupId = Tools.getIntValue(params.get("userGroupId"), -1);
    if (userGroupId>0) {
        specSearch.addSpecSearchPasswordProtected(userGroupId, "userGroupsIds", predicates, root, builder);
    }

    super.addSpecSearch(params, predicates, root, builder);
}
```

The list is stored in the database as `id1,id2,id3`, therefore the search consists of all possibilities of occurrence: `id OR id,% OR %,id,% OR %,id`.

**Search by brand name (perex groups)**

To search by comma-separated list of tag IDs, you can use the method `addSpecSearchPerexGroup(String perexGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`.

This search is automatically used if the search contains the parameter `searchPerexGroups`, is called directly in `DatatableRestControllerV2.addSpecSearch`.

```java
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder) {
    //vyhladavanie podla perexSkupiny
    String searchPerexGroups = params.get("searchPerexGroups");
    if (Tools.isNotEmpty(searchPerexGroups)) {
        SpecSearch<T> specSearch = new SpecSearch<>();
        //ziskaj zoznam IDecok userov, ktory maju dane meno
        specSearch.addSpecSearchPerexGroup(searchPerexGroups, "perexGroups", predicates, root, builder);
    }
}
```

**Search by value in a foreign table**

If you have a relationship between a table and another table by linking via ID and you are searching by text you can use `addSpecSearchIdInForeignTable(String paramValue, String foreignTableName, String foreignTableId, String foreignColumnName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`. It performs a search for the expression `paramValue` in the specified table and column, using the found ID values as `IN` expression when filtering. An SQL query of type: `"SELECT DISTINCT "+foreignTableId+" FROM "+foreignTableName+" WHERE "+foreignColumnName+" "+operator+" ?", prepend+valueClean+append`.

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<Media> root, CriteriaBuilder builder) {
    super.addSpecSearch(params, predicates, root, builder);
    String docTitle = params.get("searchEditorFields.docDetails");
    if (Tools.isNotEmpty(docTitle)) {
        SpecSearch<Media> specSearch = new SpecSearch<>();
        specSearch.addSpecSearchIdInForeignTable(docTitle, "documents", "doc_id", "title", "mediaFkId", predicates, root, builder);
    }
}
```

**Search for docid by specified path**

If you are shown the path to `DocDetails` object (typically for an array of type json) it is possible to use the `addSpecSearchDocFullPath(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`:

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<Media> root, CriteriaBuilder builder) {
    super.addSpecSearch(params, predicates, root, builder);
    int groupId = Tools.getIntValue(params.get("searchEditorFields.groups"), -1);
    if (groupId > 0) {
        predicates.add(builder.equal(root.join("groups").get("id"), groupId));
    }
    String docTitle = params.get("searchEditorFields.docDetails");
    if (Tools.isNotEmpty(docTitle)) {
        SpecSearch<Media> specSearch = new SpecSearch<>();
        specSearch.addSpecSearchDocFullPath(docTitle, "mediaFkId", predicates, root, builder);
    }
}
```

**Entity mapping search**

If your entity has another entity mapped (of type `ContactCategoryEntity` in the example):

```java
@Getter
@Setter
@Entity
@Table(name = "CONTACT_PLACE")
public class ContactPlaceEntity {
    @Id
    @GeneratedValue(generator = "WJGen_ContactPlace")
    @TableGenerator(name = "WJGen_ContactPlace", pkColumnValue = "ContactPlace")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @JoinColumn(name = "CATEGORY_ID")
    @ManyToOne
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.contact_places.label.category")
    private ContactCategoryEntity category;

    @Column(name = "NAME")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "components.contact_places.label.name")
    private String name;
    ...
```

in the form of a selection menu (dial value) and transfer via `page.addOptions` The value ID and name from the REST service need to be set to search in the nested entity when searching by ID:

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<ContactPlaceEntity> root, CriteriaBuilder builder) {
    super.addSpecSearch(params, predicates, root, builder);
    SpecSearch<ContactPlaceEntity> specSearch = new SpecSearch<>();
    if (params.containsKey("searchCategory")) {
        JpaTools.removePredicateWithName("category", predicates);
        Predicate predicate = builder.equal(root.get("category").get("id"), params.get("searchCategory"));
        predicates.add(predicate);
    }

    if (params.containsKey("searchCity")) {
        JpaTools.removePredicateWithName("city", predicates);
        Predicate predicate = builder.equal(root.get("city").get("id"), params.get("searchCity"));
        predicates.add(predicate);
    }
}
```

In the HTML file it is necessary to define the mapping of text from entity to value, this is done by the following code:

```JavaScript
let columns = [(${layout.getDataTableColumns("sk.iway.projekt.model.ContactPlaceEntity")})];

$.each(columns, function(i, v){
    if (v.data === 'category') {
        v.className = "";
        v.render = function (data, type, row) {
            return row.category.name;
        }
    }

    if (v.data === 'city') {
        v.render = function (data, type, row) {
            return row.city.name;
        }
    }
});
```

### View user name and search

Since we often store only the user ID in the database, typically in the `user_id` and display and search by user name, you can use the following solution. It is necessary to use generated/`@Transient` (not stored in the database) field `userFullName`, with the original field `userId` you don't need to annotate. Example in `DTO/Bean` facility:

```java
    @Transient
	@DataTableColumn(inputType = DataTableColumnType.TEXT, tab="main", renderFormat = "dt-format-text", title="components.audit_log.user_full_name", orderable = false, editor = {
			@DataTableColumnEditor(type = "text", attr = {
					@DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
	private String userFullName;

	public String getUserFullName() {
		if (userFullName == null && userId != null && userId.intValue()>0) {
			UserDetails user = UsersDB.getUserCached(userId.intValue());
			if (user!=null)	userFullName = user.getFullName();
			else userFullName = "";
		}
		return userFullName;
	}

	public void setUserFullName(String userFullName) {
		this.userFullName = userFullName;
	}
```

Spring repository must extend `JpaSpecificationExecutor` as above. Standard implementation in `DatatableRestControllerV2.addSpecSearch` already includes search by parameter `searchUserFullName` which is typical of such cases. So if you don't need any other special search, the display and search by user name will work automatically.

## Arrangement

By default, the column is arranged according to the defined column. The option to set the ordering can be disabled by setting the attribute `@DataTableColumn(orderable = false)`. By default, this attribute is set to `true`, but for nested attributes `@DataTableColumnNested editorFields` is off.

Sometimes it is necessary to set a different column for the layout, or for composite columns to set the layout by multiple columns. Example:

```java
public class BasketInvoiceEditorFields extends BaseEditorFields {
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.basket.invoice_email.surname",
		hiddenEditor = true,
        sortAfter = "id",
        orderable = true,
        orderProperty = "contactLastName,deliverySurName"
    )
    private String lastName;

    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.basket.invoice.name",
		hiddenEditor = true,
        sortAfter = "id",
        orderable = true,
        orderProperty = "contactFirstName,deliveryName"
    )
    private String firstName;
}
```

If you need to specially arrange the results it is possible to override the method `public Pageable addSpecSort(Map<String, String> params, Pageable pageable)` in which you edit `Pageable` object. Example:

```java
public class BasketInvoiceRestController extends DatatableRestControllerV2<BasketInvoiceEntity, Long> {
    ...
    @Override
    public Pageable addSpecSort(Map<String, String> params, Pageable pageable) {

        Sort modifiedSort = pageable.getSort();

        String[] sortList = Tools.getTokens(params.get("sort"), "\n", true);
        for (String sort : sortList ) {
            String[] data = Tools.getTokens(sort, ",", true);
            if (data.length!=2) continue;
            String field = data[0];
            Direction direction;
            if ("asc".equals(data[1])) {
                direction = Direction.ASC;
            } else if ("desc".equals(data[1])) {
                direction = Direction.DESC;
            } else {
                continue;
            }
            if ("editorFields.firstName".equals(field)) {
                modifiedSort = modifiedSort.and(Sort.by(direction, "contactFirstName", "deliveryName"));
            } else if ("editorFields.lastName".equals(field)) {
                modifiedSort = modifiedSort.and(Sort.by(direction, "contactLastName", "deliverySurName"));
            }
        }

        // create new Pageable object with modified sort
        Pageable modifiedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), modifiedSort);
        return super.addSpecSort(params, modifiedPageable);
    }
}
```

## Validation / mandatory fields

Standard Java validation is integrated into the editor [javax.validation](https://www.baeldung.com/javax-validation). It is therefore necessary in `JPA Beane` set the required validation annotations:

```java
@Entity
@Table(name="url_redirect")
public class UrlRedirectBean extends ActiveRecordRepository implements Serializable
{
    @Column(name="old_url")
	@NotBlank
	@Size(max=255)
    String oldUrl;
}
```

Most common annotations:

```java
@NotBlank //nepovoli ani whitespace
@NotEmpty //povoli whitespace
@Size(min=5, max=255) //povolene 5-255 znakov
@Email //validuje email adresu
@Pattern(regexp = "^.+@.+\\.") //validuje podla zadaneho regularneho vyrazu
@DecimalMax(value="200") //akceptuje maximalnu hodnotu 200 vratane
@DecimalMin(value="100") //akceptuje miminalnu hodnoty 100 vratane
@Future //zadany datum musi byt v buducnosti, aplikovatelne na Date objekty
@Past //zadamy datum musi byt v minulosti, aplikovatelne na Date objekty
```

The error message trapping is implemented in the class [DatatableExceptionHandlerV2](../../../../src/main/java/sk/iway/iwcm/system/spring/DatatableExceptionHandlerV2.java) where the object is converted `ConstraintViolation` on the text. To be able to edit the error message via WebJET, the translation key is searched for in the method `getErrorMessage`. If found, it is used, otherwise the standard reporting from `javax.validation`.

The translation key can use annotation attributes, e.g. `{min}` or the specified value as `${validatedValue}`.

You can annotate via the attribute `message` set the text of the error message. However, we recommend that you enter the translation key, not the text in Slovak language directly:

```java
@Pattern(message="components.module.property", regexp = "^.+@.+\\.")
```

When using `@Convert` annotations using an object instead of a primitive value (e.g. `GroupDetailsConverter`) annotation is to be used on the entity `@Valid`. This then invokes the validation of the nested object. The technical problem is that the error is raised in `group.navbarName`, but no such field exists in the datatable. Class `DatatableExceptionHandlerV2` treats this so that for an array containing a character `.` in the name to the output JSON object generates as the field name the value only before the character `.`, e.g. `group`. The exception is the field named `editorFields` which shall be left in its original/full form.

If simple validation is not enough you can implement the method `validateEditor`:

```java
    /**
     * Prikladova metoda na validaciu dat odoslanych z datatables editora.
     * Metoda je volana pre kazdy odoslaby objekt.
     * Chyby pridava do error objeku pomocou {@link Errors}.rejectValue
     *
     * @param request
     * @param user
     * @param errors
     * @param id
     * @param entity
     */
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, GalleryEntity> target, Identity user, Errors errors, Long id, GalleryEntity entity) {
        if (!user.isFolderWritable(entity.getImagePath())) {
            //objekt errorField je nase T, v tomto pripade GalleryEntity, setuje sa presna property kde sa ma error vypisat
            errors.rejectValue("errorField.imagePath", null, Prop.getInstance(request).getText("user.rights.no_folder_rights"));
            //pre globalnu chybu (netykajucu sa konkretneho fieldu) je mozne pouzit kod:
            //((BindingResult)errors).addError(new ObjectError("global", Prop.getInstance(request).getText("datatable.error.fieldErrorMessage")));
        }
    }
```

If you need to specifically control permissions (e.g., for web pages, folder permissions) you can implement the method `public boolean checkItemPerms(T entity, Long id)`. The method is called by default for edit/create/delete/action/record retrieval operations:

```java
    @Override
    public boolean checkItemPerms(MediaGroupBean entity, Long id) {
        if (InitServlet.isTypeCloud() && entity.getId() != null && entity.getId().longValue()>0) {
            if (GroupsDB.isGroupsEditable(getUser(), entity.getAvailableGroups())==false) return false;
            MediaGroupBean old = getOneItem(entity.getId());
            if (old != null && GroupsDB.isGroupsEditable(getUser(), old.getAvailableGroups())==false) return false;
        }
        return true;
    }
```

## Invocation of an error

Program-controlled errors need to be treated by overloading the method `validateEditor` (see example above), where you can perform validations before saving the record. From the parameter `target.getAction()` (DatatableRequest) can identify the type of action.

!>**Warning:** Method `validateEditor` is also called for deletion, you can test it as `if ("remove".equals(target.getAction()) ...`.

In the case of an error detected only at the time of saving (e.g. in the method `editItem`) you can raise a general error message by calling the method `throwError(String error)` or `throwError(List<String> errors)`. An example is in [GroupsRestController](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java).

## Preventing deletion / editing of a record

For some cases it is necessary to prevent editing or deleting the record. It is possible to overload methods `beforeSave` or `beforeDelete`, Example:

```java
@Override
public void beforeSave(ConfPreparedEntity entity) {
	throwError("datatables.error.recordIsNotEditable");
}

@Override
public boolean beforeDelete(ConfPreparedEntity entity) {
    //zmazat je mozne len planovany zaznam v buducnosti
    if (entity.getId()>0 && entity.getDatePrepared()!=null && entity.getDatePrepared().getTime()>Tools.getNow()) return true;

    throwError("admin.cong_editor.youCanOnlyDeleteFutureRecords");
    return false;
}
```

## Export and import of data

For the data export test, the method can be called `isExporting()` which returns the value `true` if data export is currently being performed.

For importing it is possible to modify/validate the data by implementing the method `preImportDataEdit`. This method is called before the import and it is possible to modify the data. An example is in the class [EnumerationDataRestController](../../../../src/main/java/sk/iway/iwcm/components/enumerations/rest/EnumerationDataRestController.java).

## Redirection after saving

If you need to redirect the user to another page after the record has been successfully saved, it is possible to set the URL by calling `setRedirect(String redirect)`:

```java
    @Override
    public void afterSave(FormsEntity entity, FormsEntity saved) {
        if(entity.getFormSettings().getId() == null || entity.getFormSettings().getId() == -1L) {
            if ("multistep".equals(entity.getFormType())) {
                setRedirect("/apps/form/admin/form-content/?formName=" + Tools.URLEncode(saved.getFormName()));
            }
        }
    }
```

## Extension versions

There are extension classes for special cases.

### DatatableRestControllerAvailableGroups.java

The class implements rights checking for applications whose rights are based on the structure of web pages (only records that match the rights set for the tree structure should be displayed to the user), or are used in a MultiWeb installation. An example is the media groups setting, where media groups need to be filtered according to the user's rights on the web page structure. For example, if a user has rights only to the folder "/Slovak/News" only groups that are set to display in this folder (and possibly all without restrictions) should be displayed.

Basic use is similar to standard `DatatableRestControllerV2`, but the constructor also needs to specify the name of the column with the ID value and the name of the column with the list of rights to the web page structure:

```java
@RestController
@Datatable
@RequestMapping("/admin/rest/media-group")
@PreAuthorize("@WebjetSecurityService.hasPermission('editor_edit_media_group')")
public class MediaGroupRestController extends DatatableRestControllerAvailableGroups<MediaGroupBean, Long> {

    @Autowired
    public MediaGroupRestController(MediaGroupRepository mediaGroupRepository) {
        super(mediaGroupRepository, "id", "availableGroups");
    }

}
```

Class `DatatableRestControllerAvailableGroups` in the method `public boolean checkItemPerms(T entity, Long id)` Controls the rights to both the current and original entity so that existing entities that the user does not have rights to cannot be modified.

```java
public abstract class DatatableRestControllerAvailableGroups<T, ID extends Serializable> extends DatatableRestControllerV2<T, ID> {
    ...
    @Override
    public boolean checkItemPerms(T entity, Long id) {

        BeanWrapperImpl bw = new BeanWrapperImpl(entity);
        Number entityId = (Number) bw.getPropertyValue(idColumnName);
        String availableGroups = (String) bw.getPropertyValue(availableGroupsColumnName);

        if ((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) && entityId != null && entityId.longValue()>0) {
            //if it's empty AND it's NOT multiweb then it is available for all domains
            if (InitServlet.isTypeCloud()==false && Tools.isEmpty(availableGroups)) return true;

            if (GroupsDB.isGroupsEditable(getUser(), availableGroups)==false) return false;
            T old = getOneItem(entityId.longValue());
            if (old != null) {
                //check also if original entity is editable, you can't just remove perms and edit entity which not belongs to you
                BeanWrapperImpl bwOld = new BeanWrapperImpl(old);
                availableGroups = (String) bwOld.getPropertyValue(availableGroupsColumnName);
                if (GroupsDB.isGroupsEditable(getUser(), availableGroups)==false) return false;
            }
        }
        return true;
    }
}
```
