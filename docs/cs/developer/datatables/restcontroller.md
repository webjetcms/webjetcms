# DatatableRestControllerV2.java

Třída Java [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) zapouzdřuje komunikaci s [datová tabulka](README.md) a [Redakci](../datatables-editor/README.md).

## Základní implementace

Při implementaci konkrétní služby REST je nutné rozšířit DatatableRestControllerV2. V **v ideálním případě pomocí repozitářů Spring DATA** implementační třída v jazyce Java může vypadat takto:

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
        super(redirectsRepository);
    }
}
```

důležitý je konstruktor, který předává úložiště Spring DATA. `RedirectsRepository`, anotace `@Datatable` zajistit správné zpracování chybových hlášení a anotací. `PreAuthorize` pro kontrolu práv:

```java
@Datatable
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
```

!>**Varování:** Entita Spring DATA musí mít jako PK `Long id` (ne např. `adminlogId` a podobně), pokud se sloupec v databázi jmenuje jinak, je nutné nastavit název sloupce:

```java
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_adminlog_notify")
@Column(name = "adminlog_notify_id")
@DataTableColumn(inputType = DataTableColumnType.ID)
private Long id;
```

!>**Varování:** v datové entitě **NESMÍTE používat primitivní typy** Stejně jako `int`, `long` ale objekty `Integer`, `Long`, jinak vyhledávání nebude fungovat. Toho využívá ExampleMatcher, který do dotazu do DB nevkládá objekty NULL. U primitivních typů však NULL použít nemůže, nastaví jim hodnotu 0 a pak je přidá do databáze. `WHERE` podmínky.

Komplexní ukázka včetně kontroly práv, odstranění souborů a provedení speciální akce:

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

## Metody manipulace s daty

Třídu můžete také zkonstruovat pomocí `NULL` úložiště, v takovém případě je třeba implementovat metody pro práci s daty:

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

můžete v implementaci použít existující rozhraní API, např.:

```java
@Override
public Page<DomainRedirectBean> getAllItems(Pageable pageable) {
    List<DomainRedirectBean> listedBeans = DomainRedirectDB.getAllRedirects();
    return new PageImpl<>(listedBeans);
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

Pokud potřebujete data před uložením upravit nebo provést nějakou akci po uložení do databáze (např. nastavit datum uložení nebo `domainId`) použijte metodu `beforeSave` nebo `afterSave`:

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

Metoda `public void afterSave(T entity, T saved)` je volán po uložení entity - objektu `entity` je původní odeslaný objekt, `saved` je uložená verze. Když je nový záznam `ID` se nachází pouze v `saved` entitu. Pokud používáte metody pro aktualizaci mezipaměti, nezapomeňte implementovat veřejnou metodu `void afterDelete(T entity, long id)` volán po odstranění položky.

!>**Varování:** nedoporučujeme přepisovat pomocí anotace. `@Override` Metody REST vždy překryjte ve své třídě. `xxxItem` Metody.

Při duplikaci záznamu je nutné z přijatých dat "vymazat" hodnotu ID atributu. Obvykle se jedná o atribut s názvem `id`, ale nemusí tomu tak být vždy. Název atributu se tedy hledá podle anotace množiny `DataTableColumnType.ID`.

Při použití [vnořené/dodatečné atributy](../datatables-editor/datatable-columns.md#vnořené-atributy) ve formě `editorFields` lze implementovat metody `processFromEntity` pro nastavení `editorFields` atributy nebo `processToEntity` pro nastavení atributů v entitě z `editorFields`. Metody jsou automaticky volány při čtení všech záznamů, načítání jednoho záznamu, vyhledávání nebo ukládání dat.

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

Při přepisování metod `getAllItems` nebo `searchItem` je nutné vyvolat metodu `processFromEntity` na každém prvku vráceného seznamu. V podstatě můžete zavolat metodu `processFromEntity(Page<T> page, ProcessItemAction action)` Nad stránkami `Page` objekt, který pro všechny záznamy z `Page` objektu volá metodu `processFromEntity(T entity, ProcessItemAction action)`.

## Filtrování při zobrazení všech záznamů

Někdy je nutné filtrovat data i při zobrazení všech záznamů (např. ve vnořené datové tabulce podle nějaké skupiny nebo uživatelských práv).

Existuje metoda `Page<T> DatatableRestControllerV2.getAllItemsIncludeSpecSearch(T empty, Pageable pageable)` které můžete použít ve své implementaci. Její volání zajistí, že se metody provedou. `addSpecSearch` i po načtení všech záznamů. Můžete zde implementovat další podmínky.

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

## Neexistující atributy v editoru

Ve výchozím nastavení nemusí všechny atributy entity pocházet z editoru, takže hodnoty existující entity a data odeslaná z editoru se před uložením sloučí. Ve výchozím nastavení jsou všechny ne `null` Atributy. To však neumožňuje zadat prázdné datum (po jeho nastavení). Proto jsou atributy typu DataTableColumn s poznámkou `Date` jsou převedeny, i když mají `null` Hodnota. Toto spojení je provedeno v metodě `public T editItem(T entity, long id)` pomocí `NullAwareBeanUtils.copyProperties(entity, one);`.

## Obnovení dat po uložení

Voláním metody `setForceReload(true);` je možné vynutit obnovení datové tabulky po uložení.

To je nutné, pokud je uložený objekt přesunut do jiného adresáře apod. Ukázka je v [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java).

## Číselníky pro výběrová pole

Pro výběrová pole v editoru a automatický převod ID na hodnotu (např. `templateId` na název šablony) je přidán do rozšíření WebJETu. `PageImpl` objekt [DatatablePageImpl.java](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatablePageImpl.java), který umožňuje odesílat data vytáčení.

Ty jsou automaticky nastaveny na `options` objektu v definici sloupců v editoru objektů a jsou automaticky použity pro převod ID na hodnotu.

Příkladem je [TranslationKeyController.java](../../../../src/main/java/sk/iway/iwcm/components/translation_keys/rest/TranslationKeyController.java) a [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java):

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

Při volání `page.addOptions` nastaví třetí `true/false` parametr přidávající původní `Java Beanu` do výstupního objektu. Pokud je nastavena hodnota `true`, v objektu JSON, bude původní Bean k dispozici v původním objektu. Toho lze využít k načtení dat, např. šablon:

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

## Vyhledávání

K dispozici jsou 2 režimy vyhledávání:
- `ByExample` - používá metodu `Example<T> exampleQuery = Example.of(search, matcher);` který nastaví podmínky vyhledávání na `search` entitu a následně ji použít pro vyhledávání. Všechny atributy v entitě musí být objekty, ty, které mají `NULL` nebude při vyhledávání použita. Takové vyhledávání nepodporuje všechny možnosti.
- `Specification` - používá dynamické vytváření podmínek SQL, musí úložiště rozšířit `JpaSpecificationExecutor<T>`. To je **doporučené řešení** a umožňuje vyhledávat podle datových rozsahů a také používat speciální vyhledávání.

Varování. **Oracle** databázi, je třeba nastavit vyhledávání bez ohledu na velikost písma a diakritiku pomocí nastavení SQL. Ve výchozím nastavení to zajišťuje funkce `trigger tgg_after_logon_ci_ai` které WebJET automaticky vytvoří. Pokud vyhledávání nefunguje správně, zkontrolujte, zda je správně definováno:

```sql
CREATE or REPLACE TRIGGER tgg_after_logon_ci_ai
    AFTER LOGON ON SCHEMA
    DECLARE
    BEGIN
        EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_SORT=BINARY_AI NLS_COMP=LINGUISTIC';
    END;
```

Společnost Oracle nepodporuje vyhledávání bez ohledu na velikost písma a diakritiku v. `clob` sloupce. Pro tento případ však WebJET použije funkci `LOWER` před vyhledáváním, což umožní vyhledávání bez ohledu na případ. Seznam názvů atributů JPA, pro které bude funkce použita, je definován v proměnné conf. `jpaToLowerFields`. Předpokladem je použití úložiště, které rozšiřuje `JpaSpecificationExecutor<T>`.

### Vyhledávání podle rozsahu dat

Chcete-li vyhledávat podle rozsahu dat, musíte kombinovat vyhledávání podle příkladu a konkrétní podmínky. DT v `app.js` odešle hodnotu s předponou `daterange:`, podle něhož se vyhledávání v `DatatableRestControllerV2.searchItem`. V metodě se použije kombinace hodnot:

```java
private Specification<T> getSpecFromRangeAndExample(Map<String, String> ranges, Example<T> example) {
```

Podmínkou je, aby úložiště Spring DATA také rozšiřovalo repozitář `JpaSpecificationExecutor`, příkladem je [PřesměrováníÚložiště](../../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

```java
package sk.iway.iwcm.components.redirects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RedirectsRepository extends JpaRepository<RedirectBean, Long>, JpaSpecificationExecutor<RedirectBean> {
}
```

!>**Varování:** všechna úložiště JPA musí být definována jako veřejná, jinak nebudou v klientských projektech dostupná.

### Speciální vyhledávání

Pokud je třeba provést speciální vyhledávání, je možné metodu přepsat `addSpecSearch` ve kterém lze implementovat konkrétní podmínku. Příkladem může být [GroupSchedulerRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerRestController.java) kde můžete vyhledávat podle uživatelského jména i parametru:

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

!>**Varování:** Úložiště JPA musí také dědit z funkce `JpaSpecificationExecutor`, Příklad:

```java
@Repository
public interface GroupSchedulerDtoRepository extends JpaRepository<GroupSchedulerDto, Long>, JpaSpecificationExecutor<GroupSchedulerDto> {

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNotNull(Pageable pageable, Long groupId);

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNull(Pageable pageable, Long groupId1);
}
```

#### Podpůrné metody

Kromě obecné metody `addSpecSearch` (která interně volá následující speciální metody) je možné použít metody ze třídy [SpecSearch](../../../javadoc/sk/iway/iwcm/system/datatable/SpecSearch.html):

**Vyhledávání podle jména/příjmení**

Vyhledávání podle jména v `paramValue`, kde jsou nalezená ID uživatelů vyhledávána jako IN v položce `jpaProperty`:

`addSpecSearchUserFullName(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`

příklad použití:

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

**Vyhledávání podle seznamu odděleného čárkou**

Chcete-li vyhledávat podle seznamu ID oddělených čárkou (např. skupin uživatelů), můžete použít metodu `addSpecSearchPasswordProtected(String userGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)` pro vyhledávání podle názvu skupiny uživatelů nebo metodou `addSpecSearchPasswordProtected(Integer userGroupId, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)` pro vyhledávání podle zadaného ID:

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

Seznam je uložen v databázi jako `id1,id2,id3`, proto se hledání skládá ze všech možností výskytu: `id OR id,% OR %,id,% OR %,id`.

**Vyhledávání podle názvu značky (skupiny perex)**

Chcete-li vyhledávat podle seznamu ID značek oddělených čárkou, můžete použít metodu `addSpecSearchPerexGroup(String perexGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`.

Toto hledání se automaticky použije, pokud hledání obsahuje parametr `searchPerexGroups`, se volá přímo v `DatatableRestControllerV2.addSpecSearch`.

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

**Hledání podle hodnoty v cizí tabulce**

Pokud máte vztah mezi tabulkou a jinou tabulkou pomocí propojení přes ID a hledáte podle textu, můžete použít příkaz `addSpecSearchIdInForeignTable(String paramValue, String foreignTableName, String foreignTableId, String foreignColumnName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`. Provede vyhledání výrazu `paramValue` v zadané tabulce a sloupci s použitím nalezených hodnot ID jako `IN` výraz při filtrování. Dotaz SQL typu: `"SELECT DISTINCT "+foreignTableId+" FROM "+foreignTableName+" WHERE "+foreignColumnName+" "+operator+" ?", prepend+valueClean+append`.

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

**Vyhledávání docid podle zadané cesty**

Pokud se vám ukáže cesta k `DocDetails` (typicky pro pole typu json) je možné použít příkaz `addSpecSearchDocFullPath(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`:

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

**Vyhledávání mapování entit**

Pokud má vaše entita namapovanou jinou entitu (typu `ContactCategoryEntity` v příkladu):

```java
@Getter
@Setter
@Entity
@Table(name = "CONTACT_PLACE")
public class ContactPlaceEntity {
    @Id
    @GeneratedValue(generator = "WJGen_ContactPlace")
    @TableGenerator(name = "WJGen_ContactPlace", pkColumnValue = "ContactPlace")
    @DataTableColumn(inputType = DataTableColumnType.ID, title = "ID", renderFormat = "dt-format-selector")
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

ve formě výběrového menu (číselníková hodnota) a přenos prostřednictvím `page.addOptions` Při vyhledávání podle ID je třeba nastavit hodnotu ID a název ze služby REST pro vyhledávání ve vnořené entitě:

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

V souboru HTML je nutné definovat mapování textu z entity na hodnotu, což se provádí pomocí následujícího kódu:

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

### Zobrazení uživatelského jména a vyhledávání

Vzhledem k tomu, že v databázi často ukládáme pouze ID uživatele, typicky v adresáři `user_id` a zobrazení a vyhledávání podle uživatelského jména, můžete použít následující řešení. Je nutné použít generované/`@Transient` (není uloženo v databázi) pole `userFullName`, s původním polem `userId` nemusíte psát poznámky. Příklad v `DTO/Bean` zařízení:

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

Úložiště Spring musí rozšířit `JpaSpecificationExecutor` jako výše. Standardní implementace v `DatatableRestControllerV2.addSpecSearch` již zahrnuje vyhledávání podle parametru `searchUserFullName` což je pro tyto případy typické. Pokud tedy nepotřebujete žádné další speciální vyhledávání, bude zobrazení a vyhledávání podle uživatelského jména fungovat automaticky.

## Ověřování / povinná pole

Do editoru je integrována standardní validace jazyka Java [javax.validation](https://www.baeldung.com/javax-validation). Proto je nutné v `JPA Beane` nastavit požadované validační anotace:

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

Nejčastější anotace:

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

Zachytávání chybových zpráv je implementováno ve třídě [DatatableExceptionHandlerV2](../../../../src/main/java/sk/iway/iwcm/system/spring/DatatableExceptionHandlerV2.java) kde je objekt převeden `ConstraintViolation` na textu. Aby bylo možné upravit chybové hlášení prostřednictvím WebJET, hledá se překladový klíč v metodě `getErrorMessage`. Pokud je nalezen, použije se, jinak se použije standardní hlášení z `javax.validation`.

Klíč překladu může používat atributy anotace, např. `{min}` nebo zadanou hodnotu jako `${validatedValue}`.

Anotaci můžete provést pomocí atributu `message` nastavit text chybové zprávy. Doporučujeme však zadat překladový klíč, nikoli přímo text ve slovenštině:

```java
@Pattern(message="components.module.property", regexp = "^.+@.+\\.")
```

Při použití `@Convert` anotace s použitím objektu namísto primitivní hodnoty (např. `GroupDetailsConverter`) anotace má být použita pro entitu `@Valid`. To pak vyvolá ověření vnořeného objektu. Technický problém spočívá v tom, že chyba je vyvolána v systému `group.navbarName`, ale v datové tabulce žádné takové pole neexistuje. Třída `DatatableExceptionHandlerV2` s tím zachází tak, že pro pole obsahující znak `.` v názvu do výstupního JSON objektu vygeneruje jako název pole hodnotu pouze před znakem `.`, např. `group`. Výjimkou je pole s názvem `editorFields` která se ponechává v původní/úplné podobě.

Pokud vám jednoduchá validace nestačí, můžete implementovat metodu `validateEditor`:

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

Pokud potřebujete specificky kontrolovat oprávnění (např. pro webové stránky, oprávnění složek), můžete implementovat metodu `public boolean checkItemPerms(T entity, Long id)`. Metoda je standardně volána pro operace úprav/vytvoření/smazání/akce/vyhledání záznamu:

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

## Vyvolání chyby

Chyby řízené programem je třeba ošetřit přetížením metody `validateEditor` (viz příklad výše), kde můžete před uložením záznamu provést ověření. Z parametru `target.getAction()` (DatatableRequest) lze identifikovat typ akce.

!>**Varování:** Metoda `validateEditor` je také volán ke smazání, můžete jej otestovat jako `if ("remove".equals(target.getAction()) ...`.

V případě chyby zjištěné až při ukládání (např. v metodě `editItem`) můžete vyvolat obecnou chybovou zprávu zavoláním metody `throwError(String error)` nebo `throwError(List<String> errors)`. Příkladem je [GroupsRestController](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java).

## Zabránění odstranění / úpravě záznamu

V některých případech je nutné zabránit úpravám nebo vymazání záznamu. Je možné přetížit metody `beforeSave` nebo `beforeDelete`, Příklad:

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

## Export a import dat

Pro test exportu dat je možné zavolat metodu `isExporting()` který vrací hodnotu `true` pokud se právě provádí export dat.

Při importu je možné data upravit/ověřit implementací metody `preImportDataEdit`. Tato metoda se volá před importem a je možné data upravit. Příkladem je třída [EnumerationDataRestController](../../../../src/main/java/sk/iway/iwcm/components/enumerations/rest/EnumerationDataRestController.java).

## Verze rozšíření

Pro speciální případy existují rozšiřující třídy.

### DatatableRestControllerAvailableGroups.java

Třída implementuje kontrolu práv pro aplikace, jejichž práva jsou založena na struktuře webových stránek (uživateli by se měly zobrazit pouze záznamy, které odpovídají právům nastaveným pro stromovou strukturu), nebo jsou použity v instalaci MultiWeb. Příkladem je nastavení skupin médií, kde je třeba filtrovat skupiny médií podle práv uživatele na struktuře webových stránek. Například pokud má uživatel práva pouze ke složce "/Slovak/News", měly by se zobrazit pouze skupiny, které jsou nastaveny pro zobrazení v této složce (a případně všechny bez omezení).

Základní použití je podobné standardnímu `DatatableRestControllerV2`, ale v konstruktoru je třeba zadat také název sloupce s hodnotou ID a název sloupce se seznamem práv ke struktuře webové stránky:

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

Třída `DatatableRestControllerAvailableGroups` v metodě `public boolean checkItemPerms(T entity, Long id)` Kontroluje práva k aktuální i původní entitě, aby nebylo možné upravovat existující entity, ke kterým uživatel nemá práva.

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
