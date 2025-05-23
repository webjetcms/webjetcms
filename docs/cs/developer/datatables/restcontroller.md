# DatatableRestControllerV2.java

Java třída [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) zapouzdřuje komunikaci s [datatabulkou](README.md) a [editorem](../datatables-editor/README.md).

## Základní implementace

Při implementaci specifické REST služby je třeba rozšířit DatatableRestControllerV2. V **ideálním případě použití Spring DATA repozitářů** může implementační Java třída vypadat následovně:

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

důležitý je konstruktor, který předává Spring DATA repozitář `RedirectsRepository`, anotace `@Datatable`, která zajistí korektní zpracování chybových zpráv a anotace `PreAuthorize` pro kontrolu práv:

```java
@Datatable
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
```

!>**Upozornění:** Spring DATA entita musí mít jako PK `Long id` (ne např. `adminlogId` a podobně), pokud se sloupec v databázi jmenuje jinak, je třeba mu nastavit jméno sloupce:

```java
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_adminlog_notify")
@Column(name = "adminlog_notify_id")
@DataTableColumn(inputType = DataTableColumnType.ID)
private Long id;
```

!>**Upozornění:** v datové entitě **NESMÍTE používat primitivní typy** jak `int`, `long` ale objekty `Integer`, `Long`, jinak nebude fungovat vyhledávání. To používá ExampleMatcher, který NULL objekty nevloží do DB dotazu. Pro primitivní typy ale nemůže použít NULL, nastaví je na hodnotu 0 a následně přidá do `WHERE` podmínky.

Komplexní ukázka is kontrolou práv, mazáním souborů a provedením speciální akce:

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

## Metody pro manipulaci s daty

Třídu můžete konstruovat is `NULL` repozitářům, v takovém případě je třeba implementovat metody pro práci s daty:

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

v implementaci můžete využít již existující API, např.:

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

Pokud potřebujete upravit data před uložením, nebo provést akci po uložením do databáze (např. nastavit datum uložení, nebo `domainId`) použijte metodu `beforeSave` nebo `afterSave`:

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

Metoda `public void afterSave(T entity, T saved)` je volána po uložení entity - objekt `entity` je původní odeslaný objekt, `saved` je uložena verze. Při novém záznamu se `ID` nachází jen v `saved` entite. Pokud používáte metody pro aktualizaci cache nezapomeňte implementovat i metodu public `void afterDelete(T entity, long id)` vyvolanou po smazání záznamu.

!>**Upozornění:** nedoporučujeme přepsat přes anotaci `@Override` REST metody, vždy přepisujte ve vaší třídě `xxxItem` metody.

Při duplikování záznamu je třeba přijatým datům "smazat" hodnotu ID atributu. Typicky se jedná o atribut se jménem `id`, ale nemusí to platit vždy. Jméno atributu se tedy hledá podle nastavené anotace `DataTableColumnType.ID`.

Při použití [vnořených/doplňkových atributů](../datatables-editor/datatable-columns.md#vnořené-atributy) ve formě `editorFields` lze implementovat metody `processFromEntity` pro nastavení `editorFields` atributů nebo `processToEntity` pro nastavení atributů v entitě z `editorFields`. Metody se automaticky volají při čtení všech záznamů, při získání jednoho záznamu, vyhledávání nebo při ukládání dat.

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

Při přepsání metod `getAllItems` nebo `searchItem` je třeba vyvolat metodu `processFromEntity` na každém elementu vráceného seznamu. Efektivní můžete zavolat metodu `processFromEntity(Page<T> page, ProcessItemAction action)` nad `Page` objektem, která pro všechny záznamy z `Page` objektu zavolá metodu `processFromEntity(T entity, ProcessItemAction action)`.

## Filtrování při zobrazení všech záznamů

Někdy je třeba i při zobrazení všech záznamů filtrovat data (např. u vnořené datatabulky podle nějaké skupiny nebo podle práv uživatele).

Existuje metoda `Page<T> DatatableRestControllerV2.getAllItemsIncludeSpecSearch(T empty, Pageable pageable)`, kterou můžete použít ve vaší implementaci. Její volání zajistí provedení metod `addSpecSearch` i při získání všech záznamů. Tam můžete implementovat dodatečné podmínky.

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

Standardně z editoru nemusí přicházet všechny atributy entity, proto se před uložením spojují hodnoty stávající entity a údajů zaslaných z editoru. Standardně se přepíší všechny ne `null` atributy. To ale neumožňuje zadat prázdné datum (pokud již bylo jednou nastaveno). Proto atributy anotovány DataTableColumn typu `Date` jsou přeneseny i když mají `null` hodnotu. Toto spojení se provádí v metodě `public T editItem(T entity, long id)` s využitím `NullAwareBeanUtils.copyProperties(entity, one);`.

## Obnovení dat po uložení

Voláním metody `setForceReload(true);` je možné vynutit obnovení dat datatabulky po uložení.

Je to nutné pokud se uložený objekt přesune do jiného adresáře a podobně. Ukázka je ve [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java).

## Číselníky pro select boxy

Pro select boxy v editoru a automatickou konverzi ID na hodnota (např. `templateId` na jméno šablony) je ve WebJETu přidáno rozšíření `PageImpl` objektu [DatatablePageImpl.java](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatablePageImpl.java), které umožňuje posílat číselníková data.

Tyto se automaticky nastaví do `options` objektu v columns definici editor objektu a automaticky se také použijí pro převod ID-hodnota.

Příklad je v [TranslationKeyController.java](../../../../src/main/java/sk/iway/iwcm/components/translation_keys/rest/TranslationKeyController.java) a [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java):

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

Při volání `page.addOptions` nastavuje třetí `true/false` parametr přidání původního `Java Beanu` do výstupního objektu. Pokud je nastaveno na `true`, v JSON objektu bude původní Bean dostupný v objektu original. Lze toho využít k získání dat např. šablony:

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

Existují 2 režimy vyhledávání:
- `ByExample` - využívá metodu `Example<T> exampleQuery = Example.of(search, matcher);` která nastaví vyhledávací podmínky do `search` entity a následně ji použije pro vyhledávání. V entitě musí být všechny atributy jako objekty, ty co mají `NULL` hodnotu se do vyhledávání nepoužijí. Takové vyhledávání nepodporuje všechny možnosti.
- `Specification` - využívá dynamické vytvoření SQL podmínek, repozitář musí rozšiřovat `JpaSpecificationExecutor<T>`. Toto je **doporučené řešení** a umožňuje vyhledávání podle rozsahu dat a také použití speciálního vyhledávání.

Upozornění: pro **Oracle** databázi je třeba nastavit vyhledávání bez ohledu na velikost písma a diakritiku nastavením SQL. Standardně to zajišťuje `trigger tgg_after_logon_ci_ai`, který WebJET automaticky vytvoří. Pokud vyhledávání nefunguje správně, ověřte, že je správně definován:

```sql
CREATE or REPLACE TRIGGER tgg_after_logon_ci_ai
    AFTER LOGON ON SCHEMA
    DECLARE
    BEGIN
        EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_SORT=BINARY_AI NLS_COMP=LINGUISTIC';
    END;
```

Oracle nepodporuje vyhledávání bez ohledu na velikost písma a diakritiky v `clob` sloupcích. Pro tento případ WebJET ale použije funkci `LOWER` před vyhledáváním, což umožní vyhledávání bez ohledu na velikost písmen. Seznam názvů JPA atributů, pro které se funkce použije, je definován v konf. `jpaToLowerFields`. Podmínkou je použití repozitáře, který rozšiřuje `JpaSpecificationExecutor<T>`.

### Vyhledávání podle rozsahu dat

Pro vyhledávání podle rozsahu dat je třeba zkombinovat vyhledávání by Example a specifickou podmínku. DT v `app.js` posílá hodnotu s prefixem `daterange:`, podle které se následně použije vyhledávání v `DatatableRestControllerV2.searchItem`. Využije se zkombinování hodnot v metodě:

```java
private Specification<T> getSpecFromRangeAndExample(Map<String, String> ranges, Example<T> example) {
```

Podmínkou je, aby Spring DATA repozitář rozšiřoval i `JpaSpecificationExecutor`, příklad je v [RedirectsRepository](../../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

```java
package sk.iway.iwcm.components.redirects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RedirectsRepository extends JpaRepository<RedirectBean, Long>, JpaSpecificationExecutor<RedirectBean> {
}
```

!>**Upozornění:** všechny JPA repozitáře je nutné definovat jako public, jinak nebudou dostupné v klientských projektech.

### Speciální vyhledávání

V případě potřeby provedení speciálního vyhledávání je možné přepsat metodu `addSpecSearch` ve které je možné implementovat specifickou podmínku. Příklad je v [GroupSchedulerRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerRestController.java) kde se vyhledává podle jména uživatele i podle parametru:

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

!>**Upozornění:** JPA Repozitář musí dědit iz `JpaSpecificationExecutor`, příklad:

```java
@Repository
public interface GroupSchedulerDtoRepository extends JpaRepository<GroupSchedulerDto, Long>, JpaSpecificationExecutor<GroupSchedulerDto> {

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNotNull(Pageable pageable, Long groupId);

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNull(Pageable pageable, Long groupId1);
}
```

#### Podpůrné metody

Kromě obecné metody `addSpecSearch` (která interně volá následující speciální metody) lze použít metody ze třídy [SpecSearch](../../../javadoc/sk/iway/iwcm/system/datatable/SpecSearch.html):

**Hledání podle jména/příjmení**

Vyhledávání podle zadaného jména/příjmení v `paramValue`, přičemž nalezené ID uživatelů se hledají jako IN v `jpaProperty`:

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

**Hledání podle čárkou odděleného seznamu**

Pro hledání podle čárkou odděleného seznamu ID (např. skupiny uživatelů) lze použít metodu `addSpecSearchPasswordProtected(String userGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)` pro hledání podle jména skupiny uživatelů, nebo metodu `addSpecSearchPasswordProtected(Integer userGroupId, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)` pro hledání podle zadaného ID:

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

Seznam je v databázi uložen jako `id1,id2,id3`, proto hledání skládá všechny možnosti výskytu: `id OR id,% OR %,id,% OR %,id`.

**Hledání podle jména značky (perex skupiny)**

Pro hledání podle čárkou odděleného seznamu ID značek lze použít metodu `addSpecSearchPerexGroup(String perexGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`.

Toto vyhledávání se automaticky použije, pokud vyhledávání obsahuje parametr `searchPerexGroups`, je voláno přímo v `DatatableRestControllerV2.addSpecSearch`.

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

Máte-li vztah mezi tabulkou a jinou tabulkou propojením přes ID a hledáte podle textu můžete použít `addSpecSearchIdInForeignTable(String paramValue, String foreignTableName, String foreignTableId, String foreignColumnName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`. To provede hledání výrazu `paramValue` v zadané tabulce a sloupci, přičemž nalezené hodnoty ID použije jako `IN` výraz při filtrování. Provede se SQL dotaz typu: `"SELECT DISTINCT "+foreignTableId+" FROM "+foreignTableName+" WHERE "+foreignColumnName+" "+operator+" ?", prepend+valueClean+append`.

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

**Hledání docid podle zadané cesty**

Máte-li zobrazenou cestu k `DocDetails` objektu (typicky u pole typu json) lze k filtrování podle cesty použít metodu `addSpecSearchDocFullPath(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)`:

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

**Hledání při mapování entity**

Pokud má vaše entita mapovanou další entitu (typu `ContactCategoryEntity` v příkladu):

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

ve formě výběrového menu (číselníková hodnota) a přenášíte přes `page.addOptions` ID hodnoty a název z REST služby je třeba při vyhledávání podle ID nastavit hledání ve vnořené entitě:

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

V HTML souboru je třeba definovat mapování textu z entity na hodnotu, to zabezpečíte následujícím kódem:

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

### Zobrazení jména uživatele a vyhledávání

Protože v databázi často ukládáme pouze ID uživatele, typicky v poli `user_id` a zobrazovat a vyhledávat chceme podle jména uživatele můžete použít následující řešení. Je třeba použít generované/`@Transient` (neukládá se do databáze) pole `userFullName`, přičemž původní pole `userId` nemusíte anotovat. Příklad v `DTO/Bean` objektu:

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

Spring repozitář musí rozšiřovat `JpaSpecificationExecutor`, jak je uvedeno výše. Standardní implementace v `DatatableRestControllerV2.addSpecSearch` již obsahuje hledání podle parametru `searchUserFullName`, které je typické pro takové případy. Pokud tedy nepotřebujete jiné speciální hledání, bude vám zobrazení a vyhledání podle jména uživatele fungovat automaticky.

## Uspořádání

Standardně se sloupec uspořádá podle definovaného sloupce. Možnost nastavit uspořádání lze vypnout nastavením atributu.`@DataTableColumn(orderable = false)`. Standardně je tento atribut nastaven na `true`, ale pro vnořené atributy `@DataTableColumnNested editorFields` je vypnutý.

Někdy je třeba nastavit jiný sloupec pro uspořádání, případně pro kompozitní sloupce nastavit uspořádání podle více sloupců. Příklad:

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

Pokud potřebujete speciálně uspořádat výsledky je možné přepsat metodu`public Pageable addSpecSort(Map<String, String> params, Pageable pageable)` ve které upravíte `Pageable` objekt. Příklad:

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

## Validace / povinná pole

Do editoru je integrována standardní Java validace [javax.validation](https://www.baeldung.com/javax-validation). Je tedy třeba v `JPA Beane` nastavit požadované validační anotace:

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

Zachycení chybového hlášení je implementováno ve třídě [DatatableExceptionHandlerV2](../../../../src/main/java/sk/iway/iwcm/system/spring/DatatableExceptionHandlerV2.java) kde se konvertuje objekt `ConstraintViolation` na text. Pro možnost úpravy chybového hlášení přes WebJET je hledaný překladový klíč v metodě `getErrorMessage`. Pokud se najde, použije se, jinak se použije standardní hlášení z `javax.validation`.

V překladovém klíči lze použít atributy anotace. `{min}` nebo zadanou hodnotu jako `${validatedValue}`.

Anotacím můžete přes atribut `message` nastavit text chybového hlášení. Doporučujeme ale zadat překladový klíč, ne přímo text v českém jazyce:

```java
@Pattern(message="components.module.property", regexp = "^.+@.+\\.")
```

Při použití `@Convert` anotace s využitím objektu namísto primitivní hodnoty (např. `GroupDetailsConverter`) je na entitě třeba použít anotaci `@Valid`. Ta následně vyvolá validaci vnořeného objektu. Technický problém je v tom, že chyba se vyvolá v `group.navbarName`, ale takové pole neexistuje v datatabulce. Třída `DatatableExceptionHandlerV2` toto ošetřuje tak, že pro pole obsahující znak `.` v názvu do vystupného JSON objektu generuje jako jméno pole hodnotu jen před znakem `.`, neboli například. `group`. Výjimka je pole s názvem `editorFields`, které se ponechá v původním/plném tvaru.

Pokud vám nestačí jednoduchá validace můžete implementovat metodu `validateEditor`:

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

Pokud potřebujete speciálně kontrolovat práva (např. u web stránek povolení ke složkám) můžete implementovat metodu `public boolean checkItemPerms(T entity, Long id)`. Metoda se standardně jmenuje při operacích editace/vytvoření/smazání/provedení akce/získání záznamu:

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

Programově kontrolované chyby je třeba ošetřit přetížením metody `validateEditor` (viz příklad výše), kde můžete provést validace před uložením záznamu. Z parametru `target.getAction()` (DatatableRequest) můžete identifikovat typ akce.

!>**Upozornění:** metoda `validateEditor` se jmenuje i pro vymazání, můžete jej testovat jako `if ("remove".equals(target.getAction()) ...`.

V případě chyby detekované až při uložení (např. v metodě `editItem`) můžete vyvolat obecné chybové hlášení voláním metody `throwError(String error)` nebo `throwError(List<String> errors)`. Příklad je v [GroupsRestController](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java).

## Zabránění smazání / editace záznamu

Pro některé případy je třeba zamezit editaci nebo smazání záznamu. Je možné přetížit metody `beforeSave` nebo `beforeDelete`, příklad:

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

Pro test exportu dat lze volat metodu `isExporting()`, která vrátí hodnotu `true` pokud je právě prováděn export dat.

Pro import lze modifikovat/validovat údaje implementací metody `preImportDataEdit`. Tato metoda je volána před samotným importem a lze v ní data modifikovat. Příklad je ve třídě [EnumerationDataRestController](../../../../src/main/java/sk/iway/iwcm/components/enumerations/rest/EnumerationDataRestController.java).

## Rozšiřující verze

Pro speciální případy existují rozšiřující třídy.

### DatatableRestControllerAvailableGroups.java

Třída implementuje kontrolu práv pro aplikace, jejichž práva jsou založena na struktuře web stránek (uživateli se mají zobrazit jen záznamy, které vyhovují jemu nastaveným právům na stromovou strukturu), případně se používají v MultiWeb instalaci. Příkladem je nastavení média skupin, kde je třeba filtrovat skupiny médií podle práv uživatele na strukturu web stránek. Pokud uživatel má kupř. právo pouze na složku "/Česky/Novinky" mají se zobrazit jen skupiny které se mají nastavené zobrazení v této složce (a případně všechny bez omezení).

Základní použití je podobné jako standardní `DatatableRestControllerV2`, ale do konstruktoru je třeba zadat i jméno sloupce s ID hodnotou a jméno sloupce se seznamem práv na strukturu web stránek:

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

třída `DatatableRestControllerAvailableGroups` v metodě `public boolean checkItemPerms(T entity, Long id)` kontroluje práva na aktuální i původní entitu, aby nebylo možné modifikovat stávající entity, na které uživatel nemá práva.

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
