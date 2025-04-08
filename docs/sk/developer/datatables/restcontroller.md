# DatatableRestControllerV2.java

Java trieda [DatatableRestControllerV2](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) zapúzdruje komunikáciu s [datatabuľkou](README.md) a [editorom](../datatables-editor/README.md).

## Základná implementácia

Pri implementácii špecifickej REST služby je potrebné rozšíriť DatatableRestControllerV2. V **ideálnom prípade použitia Spring DATA repozitárov** môže implementačná Java trieda vyzerať nasledovne:

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

dôležitý je konštruktor, ktorý odovzdáva Spring DATA repozitár ```RedirectsRepository```, anotácia ```@Datatable```, ktorá zabezpečí korektné spracovanie chybových správ a anotácia ```PreAuthorize``` pre kontrolu práv:

```java
@Datatable
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
```

!>**Upozornenie:** Spring DATA entita musí mať ako PK ```Long id``` (nie napr. ```adminlogId``` a podobne), ak sa stĺpec v databáze volá inak, je potrebné mu nastaviť meno stĺpca:

```java
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_adminlog_notify")
@Column(name = "adminlog_notify_id")
@DataTableColumn(inputType = DataTableColumnType.ID)
private Long id;
```

!>**Upozornenie:** v dátovej entite **NESMIETE používať primitívne typy** ako ```int```, ```long``` ale objekty ```Integer```, ```Long```, inak nebude fungovať vyhľadávanie. To používa ExampleMatcher, ktorý NULL objekty nevloží do DB dotazu. Pre primitívne typy ale nemôže použiť NULL, nastaví ich na hodnotu 0 a následne pridá do ```WHERE``` podmienky.

Komplexná ukážka aj s kontrolou práv, mazaním súborov a vykonaním špeciálnej akcie:

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

## Metódy pre manipuláciu s dátami

Triedu môžete konštruovať aj s ```NULL``` repozitárom, v takom prípade je potrebné implementovať metódy na prácu s dátami:

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

v implementácii môžete využiť už existujúce API, napr.:

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

Ak potrebujete upraviť dáta pred uložením, alebo vykonať akciu po uložením do databázy (napr. nastaviť dátum uloženia, alebo ```domainId```) použite metódu ```beforeSave``` alebo ```afterSave```:

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

Metóda ```public void afterSave(T entity, T saved)``` je volaná po uložení entity - objekt ```entity``` je pôvodný odoslaný objekt, ```saved``` je uložená verzia. Pri novom zázname sa ```ID``` nachádza len v ```saved``` entite. Ak používate metódy pre aktualizáciu cache nezabudnite implementovať aj metódu public ```void afterDelete(T entity, long id)``` vyvolanú po zmazaní záznamu.

!>**Upozornenie:** neodporúčame prepísať cez anotáciu ```@Override``` REST metódy, vždy prepisujte vo vašej triede ```xxxItem``` metódy.

Pri duplikovaní záznamu je potrebné prijatým dátam "zmazať" hodnotu ID atribútu. Typicky sa jedná o atribút s menom ```id```, ale nemusí to platiť vždy. Meno atribútu sa teda hľadá podľa nastavenej anotácie ```DataTableColumnType.ID```.

Pri použití [vnorených/doplnkových atribútov](../datatables-editor/datatable-columns.md#vnorené-atribúty) vo forme ```editorFields``` je možné implementovať metódy ```processFromEntity``` pre nastavenie ```editorFields``` atribútov alebo ```processToEntity``` pre nastavenie atribútov v entite z ```editorFields```. Metódy sa automaticky volajú pri čítaní všetkých záznamov, pri získaní jedného záznamu, vyhľadávaní alebo pri ukladaní údajov.

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

Pri prepísaní metód ```getAllItems``` alebo ```searchItem``` je potrebné vyvolať metódu ```processFromEntity``` na každom elemente vráteného zoznamu. Efektívne môžete zavolať metódu ```processFromEntity(Page<T> page, ProcessItemAction action)``` nad ```Page``` objektom, ktorá pre všetky záznamy z ```Page``` objektu zavolá metódu ```processFromEntity(T entity, ProcessItemAction action)```.

## Filtrovanie pri zobrazení všetkých záznamov

Niekedy je potrebné aj pri zobrazení všetkých záznamov filtrovať dáta (napr. pri vnorenej datatabuľke podľa nejakej skupiny, alebo podľa práv používateľa).

Existuje metóda ```Page<T> DatatableRestControllerV2.getAllItemsIncludeSpecSearch(T empty, Pageable pageable)```, ktorú môžete použiť vo vašej implementácii. Jej volanie zabezpečí vykonanie metód ```addSpecSearch``` aj pri získaní všetkých záznamov. Tam môžete implementovať dodatočné podmienky.

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

## Neexistujúce atribúty v editore

Štandardne z editora nemusia prichádzať všetky atribúty entity, preto sa pred uložením spájajú hodnoty existujúcej entity a údajov zaslaných z editora. Štandardne sa prepíšu všetky nie ```null``` atribúty. To ale neumožňuje zadať prázdny dátum (ak už bol raz nastavený). Preto atribúty anotované DataTableColumn typu ```Date``` sú prenesené aj keď majú ```null``` hodnotu. Toto spojenie sa vykonáva v metóde ```public T editItem(T entity, long id)``` s využitím ```NullAwareBeanUtils.copyProperties(entity, one);```.

## Obnovenie údajov po uložení

Volaním metódy ```setForceReload(true);``` je možné vynútiť obnovenie údajov datatabuľky po uložení.

Je to potrebné ak sa uložený objekt presunie do iného adresára a podobne. Ukážka je vo [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java).

## Číselníky pre select boxy

Pre select boxy v editore a automatickú konverziu ID na hodnota (napr. ```templateId``` na meno šablóny) je vo WebJETe pridané rozšírenie ```PageImpl``` objektu [DatatablePageImpl.java](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatablePageImpl.java), ktoré umožňuje posielať číselníkové dáta.

Tieto sa automaticky nastavia do ```options``` objektu v columns definícii editor objektu a automaticky sa aj použijú pre prevod ID-hodnota.

Príklad je v [TranslationKeyController.java](../../../../src/main/java/sk/iway/iwcm/components/translation_keys/rest/TranslationKeyController.java) a [WebpagesRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java):

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

Pri volaní ```page.addOptions``` nastavuje tretí ```true/false``` parameter pridanie pôvodného ```Java Beanu``` do výstupného objektu. Ak je nastavené na ```true```, v JSON objekte bude pôvodný Bean dostupný v objekte original. Je možné to využiť na získanie dát napr. šablóny:

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

## Vyhľadávanie

Existujú 2 režimy vyhľadávania:

- `ByExample` - využíva metódu `Example<T> exampleQuery = Example.of(search, matcher);` ktorá nastaví vyhľadávacie podmienky do `search` entity a následne ju použije pre vyhľadávanie. V entite musia byť všetky atribúty ako objekty, tie čo majú `NULL` hodnotu sa do vyhľadávania nepoužijú. Takéto vyhľadávanie nepodporuje všetky možnosti.
- `Specification` - využíva dynamické vytvorenie SQL podmienok, repozitár musí rozširovať `JpaSpecificationExecutor<T>`. Toto je **odporúčané riešenie** a umožňuje vyhľadávanie podľa rozsahu dátumov a aj použitie špeciálneho vyhľadávania.

Upozornenie: pre **Oracle** databázu je potrebné nastaviť vyhľadávanie bez ohľadu na veľkosť písma a diakritiku nastavením SQL. Štandardne to zabezpečuje `trigger tgg_after_logon_ci_ai`, ktorý WebJET automaticky vytvorí. Ak vyhľadávanie nefunguje správne, overte, že je správne definovaný:

```sql
CREATE or REPLACE TRIGGER tgg_after_logon_ci_ai
    AFTER LOGON ON SCHEMA
    DECLARE
    BEGIN
        EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_SORT=BINARY_AI NLS_COMP=LINGUISTIC';
    END;
```

Oracle nepodporuje vyhľadávanie bez ohľadu na veľkosť písma a diakritiky v `clob` stĺpcoch. Pre tento prípad WebJET ale použije funkciu `LOWER` pred vyhľadávaním, čo umožní vyhľadávanie bez ohľadu na veľkosť písmen. Zoznam názvov JPA atribútov, pre ktoré sa funkcia použije je definovaný v konf. premennej `jpaToLowerFields`. Podmienkou je použitie repozitára, ktorý rozširuje `JpaSpecificationExecutor<T>`.

### Vyhľadávanie podľa rozsahu dátumov

Pre vyhľadávanie podľa rozsahu dátumov je potrebné skombinovať vyhľadávanie by Example a špecifickú podmienku. DT v ```app.js``` posiela hodnotu s prefixom ```daterange:```, podľa ktorej sa následne použije vyhľadávanie v ```DatatableRestControllerV2.searchItem```. Využije sa skombinovanie hodnôt v metóde:

```java
private Specification<T> getSpecFromRangeAndExample(Map<String, String> ranges, Example<T> example) {
```

Podmienkou je, aby Spring DATA repozitár rozširoval aj ```JpaSpecificationExecutor```, príklad je v [RedirectsRepository](../../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

```java
package sk.iway.iwcm.components.redirects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RedirectsRepository extends JpaRepository<RedirectBean, Long>, JpaSpecificationExecutor<RedirectBean> {
}
```

!>**Upozornenie:** všetky JPA repozitáre je nutné definovať ako public, inak nebudú dostupné v klientskych projektoch.

### Špeciálne vyhľadávanie

V prípade potreby vykonania špeciálneho vyhľadávania je možné prepísať metódu `addSpecSearch` v ktorej je možné implementovať špecifickú podmienku. Príklad je v [GroupSchedulerRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerRestController.java) kde sa vyhľadáva podľa mena používateľa aj podľa parametra:

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

!>**Upozornenie:** JPA Repozitár musí dediť aj z ```JpaSpecificationExecutor```, príklad:

```java
@Repository
public interface GroupSchedulerDtoRepository extends JpaRepository<GroupSchedulerDto, Long>, JpaSpecificationExecutor<GroupSchedulerDto> {

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNotNull(Pageable pageable, Long groupId);

    Page<GroupSchedulerDto> findAllByGroupIdAndWhenToPublishIsNull(Pageable pageable, Long groupId1);
}
```

#### Podporné metódy

Okrem všeobecnej metódy ```addSpecSearch``` (ktorá interne volá nasledovné špeciálne metódy) je možné použiť metódy z triedy [SpecSearch](../../../javadoc/sk/iway/iwcm/system/datatable/SpecSearch.html):

**Hľadanie podľa mena/priezviska**

Vyhľadávanie podľa zadaného mena/priezviska v ```paramValue```, pričom nájdené ID používateľov sa hľadajú ako IN v ```jpaProperty```:

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

**Hľadanie podľa čiarkou oddeleného zoznamu**

Pre hľadanie podľa čiarkou oddeleného zoznamu ID (napr. skupiny používateľov) je možné použiť metódu ```addSpecSearchPasswordProtected(String userGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)``` pre hľadanie podľa mena skupiny používateľov, alebo metódu ```addSpecSearchPasswordProtected(Integer userGroupId, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)``` pre hľadanie podľa zadaného ID:

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

Zoznam je v databáze uložený ako ```id1,id2,id3```, preto hľadanie skladá všetky možnosti výskytu: ```id OR id,% OR %,id,% OR %,id```.

**Hľadanie podľa mena značky (perex skupiny)**

Pre hľadanie podľa čiarkou oddeleného zoznamu ID značiek je možné použiť metódu ```addSpecSearchPerexGroup(String perexGroupName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)```.

Toto vyhľadávanie sa automaticky použije ak vyhľadávanie obsahuje parameter ```searchPerexGroups```, je volané priamo v ```DatatableRestControllerV2.addSpecSearch```.

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

**Hľadanie podľa hodnoty v cudzej tabuľke**

Ak máte vzťah medzi tabuľkou a inou tabuľkou prepojením cez ID a hľadáte podľa textu môžete použiť ```addSpecSearchIdInForeignTable(String paramValue, String foreignTableName, String foreignTableId, String foreignColumnName, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)```. To vykoná hľadanie výrazu ```paramValue``` v zadanej tabuľke a stĺpci, pričom nájdené hodnoty ID použije ako ```IN``` výraz pri filtrovaní. Vykoná sa SQL dotaz typu: ```"SELECT DISTINCT "+foreignTableId+" FROM "+foreignTableName+" WHERE "+foreignColumnName+" "+operator+" ?", prepend+valueClean+append```.

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

**Hľadanie docid podľa zadanej cesty**

Ak máte zobrazenú cestu k ```DocDetails``` objektu (typicky pri poli typu json) je možné na filtrovanie podľa cesty použiť metódu ```addSpecSearchDocFullPath(String paramValue, String jpaProperty, List<Predicate> predicates, Root<T> root, CriteriaBuilder builder)```:

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

**Hľadanie pri mapovaní entity**

Ak má vaša entita mapovanú ďalšiu entitu (typu `ContactCategoryEntity` v príklade):

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

vo forme výberového menu (číselníková hodnota) a prenášate cez `page.addOptions` ID hodnoty a názov z REST služby je potrebné pri vyhľadávaní podľa ID nastaviť hľadanie vo vnorenej entite:

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

V HTML súbore je potrebné definovať mapovanie textu z entity na hodnotu, to zabezpečíte nasledovným kódom:

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

### Zobrazenie mena používateľa a vyhľadávanie

Keďže v databáze často ukladáme len ID používateľa, typicky v poli ```user_id``` a zobrazovať a vyhľadávať chceme podľa mena používateľa môžete použiť nasledovné riešenie. Je potrebné použiť generované/```@Transient``` (neukladá sa do databázy) pole ```userFullName```, pričom pôvodné pole ```userId``` nemusíte anotovať. Príklad v ```DTO/Bean``` objekte:

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

Spring repozitár musí rozširovať ```JpaSpecificationExecutor```, ako je uvedené vyššie. Štandardná implementácia v ```DatatableRestControllerV2.addSpecSearch``` už obsahuje hľadanie podľa parametra ```searchUserFullName```, ktoré je typické pre takéto prípady. Ak teda nepotrebujete iné špeciálne hľadanie, bude vám zobrazenie a vyhľadanie podľa mena používateľa fungovať automaticky.

## Usporiadanie

Štandardne sa stĺpec usporiada podľa definovaného stĺpca. Možnosť nastaviť usporiadanie je možné vypnúť nastavením atribútu `@DataTableColumn(orderable = false)`. Štandardne je tento atribút nastavený na `true`, ale pre vnorené atribúty `@DataTableColumnNested editorFields` je vypnutý.

Niekedy je potrebné nastaviť iný stĺpec pre usporiadanie, prípadne pre kompozitné stĺpce nastaviť usporiadanie podľa viacerých stĺpcov. Príklad:

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

Ak potrebujete špeciálne usporiadať výsledky je možné prepísať metódu `public Pageable addSpecSort(Map<String, String> params, Pageable pageable)` v ktorej upravíte `Pageable` objekt. Príklad:

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

## Validácia / povinné polia

Do editora je integrovaná štandardná Java validácia [javax.validation](https://www.baeldung.com/javax-validation). Je teda potrebné v ```JPA Beane``` nastaviť požadované validačné anotácie:

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

Najčastejšie anotácie:

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

Zachytenie chybového hlásenia je implementované v triede [DatatableExceptionHandlerV2](../../../../src/main/java/sk/iway/iwcm/system/spring/DatatableExceptionHandlerV2.java) kde sa konvertuje objekt ```ConstraintViolation``` na text. Pre možnosť úpravy chybového hlásenia cez WebJET je hľadaný prekladový kľúč v metóde ```getErrorMessage```. Ak sa nájde, použije sa, inak sa použije štandardné hlásenie z ```javax.validation```.

V prekladovom kľúči je možné použiť atribúty anotácie, napr. ```{min}``` alebo zadanú hodnotu ako ```${validatedValue}```.

Anotáciám môžete cez atribút ```message``` nastaviť text chybového hlásenia. Odporúčame ale zadať prekladový kľúč, nie priamo text v slovenskom jazyku:

```java
@Pattern(message="components.module.property", regexp = "^.+@.+\\.")
```

Pri použití ```@Convert``` anotácie s využitím objektu namiesto primitívnej hodnoty (napr. ```GroupDetailsConverter```) je na entite potrebné použiť anotáciu ```@Valid```. Tá následne vyvolá validáciu vnoreného objektu. Technický problém je v tom, že chyba sa vyvolá v ```group.navbarName```, ale takéto pole neexistuje v datatabuľke. Trieda ```DatatableExceptionHandlerV2``` toto ošetruje tak, že pre pole obsahujúce znak ```.``` v názve do vystupného JSON objektu generuje ako meno pola hodnotu len pred znakom ```.```, čiže napr. ```group```. Výnimka je pole s názvom ```editorFields```, ktoré sa ponechá v pôvodnom/plnom tvare.

Ak vám nestačí jednoduchá validácia môžete implementovať metódu ```validateEditor```:

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

Ak potrebujete špeciálne kontrolovať práva (napr. pri web stránkach povolenie na priečinky) môžete implementovať metódu `public boolean checkItemPerms(T entity, Long id)`. Metóda sa štandardne volá pri operáciach editácia/vytvorenie/zmazanie/vykonanie akcie/získanie záznamu:

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

## Vyvolanie chyby

Programovo kontrolované chyby je potrebné ošetriť preťažením metódy ```validateEditor``` (viď príklad vyššie), kde môžete vykonať validácie pred uložením záznamu. Z parametra ```target.getAction()``` (DatatableRequest)  môžete identifikovať typ akcie.

!>**Upozornenie:** metóda `validateEditor` sa volá aj pre vymazanie, môžete ho testovať ako ```if ("remove".equals(target.getAction()) ...```.

V prípade chyby detegovanej až pri uložení (napr. v metóde ```editItem```) môžete vyvolať všeobecné chybové hlásenie volaním metódy ```throwError(String error)``` alebo ```throwError(List<String> errors)```. Príklad je v [GroupsRestController](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java).

## Zabránenie zmazania / editácie záznamu

Pre niektoré prípady je potrebné zamedziť editácii alebo zmazaniu záznamu. Je možné preťažiť metódy ```beforeSave``` alebo ```beforeDelete```, príklad:

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

## Export a import dát

Pre test exportu dát je možné volať metódu `isExporting()`, ktorá vráti hodnotu `true` ak je práve vykonávaný export dát.

Pre import je možné modifikovať/validovať údaje implementáciou metódy `preImportDataEdit`. Táto metóda je volaná pred samotným importom a je možné v nej dáta modifikovať. Príklad je v triede [EnumerationDataRestController](../../../../src/main/java/sk/iway/iwcm/components/enumerations/rest/EnumerationDataRestController.java).

## Rozširujúce verzie

Pre špeciálne prípady existujú rozširujúce triedy.

### DatatableRestControllerAvailableGroups.java

Trieda implementuje kontrolu práv pre aplikácie, ktorých práva sú založené na štruktúre web stránok (používateľovi sa majú zobraziť len záznamy, ktoré vyhovujú jemu nastaveným právam na stromovú štruktúru), prípadne sa používajú v MultiWeb inštalácii. Príkladom je nastavenie média skupín, kde je potrebné filtrovať skupiny médií podľa práv používateľa na štruktúru web stránok. Ak používateľ má napr. právo len na priečinok "/Slovensky/Novinky" majú sa zobraziť len skupiny ktoré sa majú nastavené zobrazenie v tomto priečinku (a prípadne všetky bez obmedzení).

Základné použitie je podobné ako štandardný `DatatableRestControllerV2`, ale do konštruktora je potrebné zadať aj meno stĺpca s ID hodnotou a meno stĺpca so zoznamom práv na štruktúru web stránok:

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

trieda `DatatableRestControllerAvailableGroups` v metóde `public boolean checkItemPerms(T entity, Long id)` kontroluje práva na aktuálnu aj pôvodnú entitu, aby nebolo možné modifikovať existujúce entity, na ktoré používateľ nemá práva.

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