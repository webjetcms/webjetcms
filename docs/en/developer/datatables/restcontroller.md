# DatatableRestControllerV2.java

The Java class [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) encapsulates communication with the [datatable](README.md) and the [editor](../datatables-editor/README.md).

## Basic implementation

When implementing a specific REST service, it is necessary to extend DatatableRestControllerV2. In the **ideal case of using Spring DATA repositories**, the implementation Java class might look like this:

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

The important thing is the constructor, which passes the Spring DATA repository ```RedirectsRepository``` and the entity class that will be used when creating a new record. This also sets the values ​​when creating a new record. The annotation ```@Datatable``` ensures correct processing of error messages and the annotation ```PreAuthorize``` is used to check permissions:

```java
@Datatable
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
```

!>**Warning:** Spring DATA entity must have PK ```Long id``` (not e.g. ```adminlogId``` and similar), if the column in the database is called differently, it is necessary to set the column name:

```java
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_adminlog_notify")
@Column(name = "adminlog_notify_id")
@DataTableColumn(inputType = DataTableColumnType.ID)
private Long id;
```

!>**Warning:** in the data entity **YOU MUST NOT use primitive types** like ```int```, ```long``` but objects ```Integer```, ```Long```, otherwise the search will not work. This is used by ExampleMatcher, which does not insert NULL objects into the DB query. However, it cannot use NULL for primitive types, it sets them to the value 0 and then adds the conditions to ```WHERE```.

A comprehensive demonstration including checking permissions, deleting files, and performing a special action:

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

import jakarta.servlet.http.HttpServletRequest;

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

You can also construct a class with a ```NULL``` repository, in which case you need to implement methods for working with the data:

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

You can use existing APIs in your implementation, e.g.:

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

If you need to edit the data before saving, or perform an action after saving to the database (e.g. set the save date, or ```domainId```) use the ```beforeSave``` or ```afterSave``` method:

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

The ```public void afterSave(T entity, T saved)``` method is called after the entity is saved - the ```entity``` object is the original sent object, ```saved``` is the saved version. For a new record, ```ID``` is only found in the ```saved``` entity. If you use methods for updating the cache, do not forget to implement the public ```void afterDelete(T entity, long id)``` method called after the record is deleted.

!>**Warning:** we do not recommend overriding REST methods via the ```@Override``` annotation, always override ```xxxItem``` methods in your class.

When duplicating a record, it is necessary to "delete" the attribute ID value from the received data. Typically, this is an attribute with the name ```id```, but this may not always be the case. The attribute name is therefore searched for according to the set annotation ```DataTableColumnType.ID```.

When using [nested/additional attributes](../datatables-editor/datatable-columns.md#nested-attributes) in the form ```editorFields```, it is possible to implement methods ```processFromEntity``` to set ```editorFields``` attributes or ```processToEntity``` to set attributes in an entity from ```editorFields```. The methods are automatically called when reading all records, retrieving a single record, searching, or saving data.

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

When overriding the ```getAllItems``` or ```searchItem``` methods, you need to call the ```processFromEntity``` method on each element of the returned list. You can effectively call the ```processFromEntity(Page<T> page, ProcessItemAction action)``` method on the ```Page``` object, which will call the ```processFromEntity(T entity, ProcessItemAction action)``` method for all entries in the ```Page``` object.

## Filtering when viewing all records

Sometimes it is necessary to filter data even when displaying all records (e.g. in a nested data table by a group or by user rights).

There is a method ```Page<T> DatatableRestControllerV2.getAllItemsIncludeSpecSearch(T empty, Pageable pageable)``` that you can use in your implementation. Calling it will ensure that the methods ```addSpecSearch``` are executed even when all records are retrieved. You can implement additional conditions there.

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

## Non-existent attributes in the editor

By default, not all entity attributes may come from the editor, so the values ​​of the existing entity and the data sent from the editor are combined before saving. By default, all non-```null``` attributes are overwritten. However, this does not allow you to enter an empty date (if it has already been set). Therefore, attributes annotated with DataTableColumn type ```Date``` are transferred even if they have a ```null``` value. This combination is performed in the ```public T editItem(T entity, long id)``` method using ```NullAwareBeanUtils.copyProperties(entity, one);```.

## Restoring data after saving

By calling the ```setForceReload(true);``` method, it is possible to force the data table to be refreshed after saving.

This is necessary if the saved object is moved to another directory, etc. The example is in [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java).

## Dials for select boxes

For select boxes in the editor and automatic conversion of ID to value (e.g. ```templateId``` to template name), an extension ```PageImpl``` of the [DatatablePageImpl.java](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatablePageImpl.java) object has been added to WebJET, which allows sending codebook data.

These are automatically set to the ```options``` object in the columns definition of the object editor and are also automatically used for ID-value conversion.

An example is in [TranslationKeyController.java](../../../../src/main/java/sk/iway/iwcm/components/translation_keys/rest/TranslationKeyController.java) and [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java):

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

When calling ```page.addOptions```, the third ```true/false``` parameter sets the addition of the original ```Java Beanu``` to the output object. If set to ```true```, the original Bean will be available in the original object in the JSON object. This can be used to retrieve data, e.g. from a template:

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

- `ByExample` - ​​uses the `Example<T> exampleQuery = Example.of(search, matcher);` method which sets the search conditions in the `search` entity and then uses it for the search. All attributes in the entity must be as objects, those with a `NULL` value will not be used in the search. Such a search does not support all options.
- `Specification` - ​​uses dynamic SQL condition creation, the repository must extend `JpaSpecificationExecutor<T>`. This is the **recommended solution** and allows searching by date range and also using special searches.

Note: For **Oracle** database, case-insensitive and case-insensitive search needs to be set up via SQL settings. This is provided by default by `trigger tgg_after_logon_ci_ai`, which WebJET automatically creates. If search does not work properly, verify that it is defined correctly:

```sql
CREATE or REPLACE TRIGGER tgg_after_logon_ci_ai
    AFTER LOGON ON SCHEMA
    DECLARE
    BEGIN
        EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_SORT=BINARY_AI NLS_COMP=LINGUISTIC';
    END;
```

Oracle does not support case-insensitive searching in `clob` columns. For this case, WebJET will use the `LOWER` function before the search, which will allow case-insensitive searching. The list of JPA attribute names for which the function will be used is defined in the conf. variable `jpaToLowerFields`. The condition is to use a repository that extends `JpaSpecificationExecutor<T>`.

### Search by date range

To search by date range, it is necessary to combine the search by Example and a specific condition. DT in ```app.js``` sends a value with the prefix ```daterange:```, according to which the search in ```DatatableRestControllerV2.searchItem``` is subsequently used. Combining values ​​in the method is used:

```java
private Specification<T> getSpecFromRangeAndExample(Map<String, String> ranges, Example<T> example) {
```

The condition is that the Spring DATA repository also extends ```JpaSpecificationExecutor```, an example is in [RedirectsRepository](../../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

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

If a special search is needed, it is possible to override the `addSpecSearch` method in which a specific condition can be implemented. An example is in [GroupSchedulerRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerRestController.java) where the search is done by both the user name and the parameter:

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

!>**Warning:** JPA Repository must also inherit from ```JpaSpecificationExecutor```, example:

```java
@Repository
public interface GroupSchedulerDtoRepository extends JpaRepository<GroupSchedulerDto, Long>, JpaSpecificationExecutor<GroupSchedulerDto> {

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNotNull(Pageable pageable, Long groupId);

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNull(Pageable pageable, Long groupId1);
}
```

#### Supportive methods

In addition to the general method ```addSpecSearch``` (which internally calls the following special methods), it is possible to use methods from the [SpecSearch] class (../../../javadoc/sk/iway/iwcm/system/datatable/SpecSearch.html):

**Search by first/last name**

Search by entered first/last name in ```paramValue```, while the found user IDs are searched as IN in ```jpaProperty```:

```addSpecSearchUserFullName(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)```

príklad použitia:

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

**Search by comma separated list**

To search by a comma-separated list of IDs (e.g. user groups), you can use the ```addSpecSearchPasswordProtected(String userGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)``` method to search by user group name, or the ```addSpecSearchPasswordProtected(Integer userGroupId, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)``` method to search by a specified ID:

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

The list is stored in the database as ```id1,id2,id3```, so the search consists of all possible occurrences of: ```id OR id,% OR %,id,% OR %,id```.

**Search by brand name (perex group)**

To search by a comma-separated list of tag IDs, the ```addSpecSearchPerexGroup(String perexGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)``` method can be used.

This search is automatically used if the search contains the parameter ```searchPerexGroups```, it is called directly in ```DatatableRestControllerV2.addSpecSearch```.

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

If you have a relationship between a table and another table via an ID link and you are searching by text, you can use ```addSpecSearchIdInForeignTable(String paramValue, String foreignTableName, String foreignTableId, String foreignColumnName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)```. This will search for the expression ```paramValue``` in the specified table and column, using the found ID values ​​as the ```IN``` expression when filtering. An SQL query of the type: ```"SELECT DISTINCT "+foreignTableId+" FROM "+foreignTableName+" WHERE "+foreignColumnName+" "+operator+" ?", prepend+valueClean+append``` will be executed.

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

**Searching for docid by specified path**

If you have a path to the ```DocDetails``` object displayed (typically for a json field), you can use the ```addSpecSearchDocFullPath(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)``` method to filter by path:

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

in the form of a selection menu (numeric value) and you transfer the ID value and name from the REST service via `page.addOptions`, it is necessary to set the search in the nested entity when searching by ID:

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

In the HTML file, you need to define the mapping of text from entity to value, you can do this with the following code:

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

### Viewing username and searching

Since we often store only the user ID in the database, typically in the ```user_id``` field, and we want to display and search by user name, you can use the following solution. You need to use the generated/```@Transient``` (not stored in the database) field ```userFullName```, while you do not need to annotate the original field ```userId```. Example in the ```DTO/Bean``` object:

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

The Spring repository must extend ```JpaSpecificationExecutor``` as mentioned above. The standard implementation in ```DatatableRestControllerV2.addSpecSearch``` already includes a search by parameter ```searchUserFullName```, which is typical for such cases. So, if you do not need any other special search, displaying and searching by username will work automatically for you.

## Arrangement

By default, the column is sorted by the defined column. The ability to set the sorting can be disabled by setting the `@DataTableColumn(orderable = false)` attribute. By default, this attribute is set to `true`, but for nested attributes `@DataTableColumnNested editorFields` it is disabled.

Sometimes it is necessary to set a different column for sorting, or for composite columns to set sorting by multiple columns. Example:

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

If you need to specifically sort the results, you can override the `public Pageable addSpecSort(Map<String, String> params, Pageable pageable)` method in which you modify the `Pageable` object. Example:

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

## Validation / required fields

The editor integrates standard Java validation [jakarta.validation](https://www.baeldung.com/javax-validation). It is therefore necessary to set the required validation annotations in ```JPA Beane```:

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

Error message capture is implemented in the class [DatatableExceptionHandlerV2](../../../../src/main/java/sk/iway/iwcm/system/spring/DatatableExceptionHandlerV2.java) where the object ```ConstraintViolation``` is converted to text. To be able to edit the error message via WebJET, a translation key is searched for in the method ```getErrorMessage```. If found, it is used, otherwise the standard message from ```jakarta.validation``` is used.

In the translation key, it is possible to use annotation attributes, e.g. ```{min}``` or a specified value such as ```${validatedValue}```.

You can set the error message text for annotations via the ```message``` attribute. However, we recommend entering a translation key, not the text in Slovak directly:

```java
@Pattern(message="components.module.property", regexp = "^.+@.+\\.")
```

When using the ```@Convert``` annotation using an object instead of a primitive value (e.g. ```GroupDetailsConverter```), the ```@Valid``` annotation must be used on the entity. This will then trigger validation of the nested object. The technical problem is that the error is raised in ```group.navbarName```, but such a field does not exist in the data table. The ```DatatableExceptionHandlerV2``` class handles this by generating the value before the ```.``` character in the name of the output JSON object as the field name, for a field containing the ```.``` character in the name, i.e. ```group```. The exception is the field named ```editorFields```, which is left in its original/full form.

If simple validation is not enough for you, you can implement the ```validateEditor``` method:

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

If you need to specifically check permissions (e.g. folder permissions for websites), you can implement the `public boolean checkItemPerms(T entity, Long id)` method. The method is called by default for edit/create/delete/perform action/get record operations:

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

## Error triggering

Programmatically checked errors need to be handled by overloading the ```validateEditor``` method (see example above), where you can perform validations before saving the record. You can identify the type of action from the ```target.getAction()``` (DatatableRequest) parameter.

!>**Warning:** method `validateEditor` is also called for deletion, you can test it as ```if ("remove".equals(target.getAction()) ...```.

In case of an error detected only during saving (e.g. in the ```editItem``` method), you can raise a general error message by calling the ```throwError(String error)``` or ```throwError(List<String> errors)``` method. An example is in [GroupsRestController](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java).

## Preventing deletion/editing of a record

For some cases it is necessary to prevent editing or deleting a record. It is possible to overload the ```beforeSave``` or ```beforeDelete``` methods, example:

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

## Export and import data

To test data export, you can call the `isExporting()` method, which returns the value `true` if data export is currently being performed.

For import, it is possible to modify/validate data by implementing the `preImportDataEdit` method. This method is called before the import itself and it is possible to modify data in it. An example is in the class [EnumerationDataRestController](../../../../src/main/java/sk/iway/iwcm/components/enumerations/rest/EnumerationDataRestController.java).

## Redirect after saving

If you need to redirect the user to another page after successfully saving the record, you can set the URL address by calling `setRedirect(String redirect)`:

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

## Expansion versions

There are extension classes for special cases.

### DatatableRestControllerAvailableGroups.java

The class implements rights control for applications whose rights are based on the structure of web pages (the user should only see records that match his/her set rights to the tree structure), or are used in a MultiWeb installation. An example is the setting of media groups, where it is necessary to filter media groups according to the user's rights to the structure of web pages. For example, if the user has rights only to the folder "/Slovensky/Novinky", only the groups that are set to be displayed in this folder should be displayed (and possibly all without restrictions).

The basic usage is similar to the standard `DatatableRestControllerV2`, but you also need to enter the name of the column with the ID value and the name of the column with the list of rights to the website structure into the constructor:

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

The `DatatableRestControllerAvailableGroups` class in the `public boolean checkItemPerms(T entity, Long id)` method checks the rights to both the current and original entities to prevent modification of existing entities to which the user does not have rights.

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