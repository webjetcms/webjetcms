# Oddelenie údajov domén

Často je potrebné vo viac doménovej inštalácii oddeliť údaje aplikácie pre doménu (každá doména má navonok samostatné údaje v aplikácii). WebJET takéto oddelenie podporuje pridaním databázového stĺpca ```domain_id```, ktorý reprezentuje ID domény (technicky je zhodný s prvým ID adresára v danej doméne) do databázovej tabuľky (napr. ```questions_answers```). Následne sa pri výbere dát z tabuľky tieto filtrujú na základe aktuálne zvolenej domény podľa stĺpca ```domain_id``` v databáze.

Nastavenie režimu viacerých domén je opísané v [nastavení šablón](../../frontend/setup/README.md#správa-viacerých-domén), dôležité je nastavenie konfiguračnej premennej ```enableStaticFilesExternalDir=true```, ktorý zapne oddelenie dát domén (predvolene sa v multidomain režim používajú spoločné dáta, konf. premenná je predvolene nastavená na ```false```).

Hodnotu pre stĺpec ```domain_id``` získate v Java kóde volaním ```CloudToolsForCore.getDomainId()```.

![](../../redactor/webpages/domain-select.png)

## Entita

Java entita musí obsahovať stĺpec ```domainId```:

```java
    @Column(name = "domain_id")
	@DataTableColumn(
        inputType = DataTableColumnType.HIDDEN
    )
	private Integer domainId;
```

a samozrejme v databáze musí existovať stĺpec ```domain_id```:

```sql
ALTER TABLE table_name ADD domain_id int DEFAULT 1 NOT NULL;
```

## Repozitár

WebJET má pripravený základný repozitár, ktorý automaticky podporuje všetky potrebné operácie pre zabezpečenie oddelenia údajov domén, vo vašej implementácii stačí rozšíriť váš repozitár z triedy ```DomainIdRepository<T, ID>```:

```java
package sk.iway.iwcm.components.qa;

import org.springframework.stereotype.Repository;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;
import java.util.List;

@Repository
public interface QuestionsAnswersRepository extends DomainIdRepository<QuestionsAnswersEntity, Long> {

}
```

priamo ```DatatableRestControllerV2``` následne zabezpečí doplnenie filtrovania údajov podľa aktuálne zvolenej domény.

## REST rozhranie

Pri vašej implementácii pri preťažení metódy ```getOneItem``` (alebo iných ```Item``` metód) vždy skontrolujte správnosť vašej implementácie. Prípadne ak potrebujete len doplniť údaje využite volanie ```super.get...```, ktoré zabezpečí kontrolu domény a až následne spracujte ďalší kód:

```java
@Override
public QuestionsAnswersEntity getOneItem(long id) {

    if(id != -1) {
        //ziskaj udaje volanim super.getOneItem aby sa korektne vykonalo porovnanie domain_id stlpca
        QuestionsAnswersEntity entity = super.getOneItem(id);
        ...
        return entity;
    }

    QuestionsAnswersEntity entity = new QuestionsAnswersEntity();
    ...

    return entity;
}
```

## Implementačné detaily

V triede ```DatatableRestControllerV2``` sa v konštruktore deteguje podpora ```DomainIdRepository``` repozitára, čo spôsobí nastavenie premennej ```checkDomainId``` na hodnotu ```true```.

```java
    if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
		if (repo instanceof DomainIdRepository) checkDomainId = true;
	}
```

Tento repozitár definuje metódy pre získanie údajov podľa ID domény:

```java
package sk.iway.iwcm.system.datatable.spring;

@NoRepositoryBean
public interface DomainIdRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    Optional<T> findFirstByIdAndDomainId(Long id, int domainId);

    List<T> findAllByDomainId(int domainId);
    Page<T> findAllByDomainId(int domainId, Pageable pageable);

}
```

Premenná ```checkDomainId``` sa následne v ```DatatableRestControllerV2``` používa na testovanie, či sa jedná o repozitár s podporou viacerých domén a následne sa volajú príslušné metódy (napr. ```findAllByDomainId```):

```java
public Page<T> getAllItems(Pageable pageable) {
    Page<T> page;

    if (checkDomainId) {
        //ak nemame size parameter tak sa jedna o serverSide: false, takze pageable nemame pouzit
        if (getRequest().getParameter("size")==null) page = new DatatablePageImpl<>(getDomainRepo().findAllByDomainId(CloudToolsForCore.getDomainId()));
        else page = getDomainRepo().findAllByDomainId(CloudToolsForCore.getDomainId(), pageable);
    } else {
        //ak nemame size parameter tak sa jedna o serverSide: false, takze pageable nemame pouzit
        if (getRequest().getParameter("size")==null) page = new DatatablePageImpl<>(repo.findAll());
        else page = repo.findAll(pageable);
    }


    processFromEntity(page, ProcessItemAction.GETALL);
    return page;
}
```

Podobne sa ID domény používa pri získaní záznamu, jeho editácii alebo mazaní (mazanie sa technicky vykoná získaním entity volaním ```findFirstByIdAndDomainId``` a následným volaním ```delete```). Pri vyhľadávaní sa využíva ```JpaSpecificationExecutor``` pomocou ktorého sa k vyhľadávaniu pridáva podmienka:

```java
@SuppressWarnings("unchecked")
public List<T> findItemBy(String propertyName, T original) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
    ...
    //pridaj domainId podmienku ak entita obsahuje domainId stlpec (aby sa neaktualizovali entity v inej domene)
    if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
        if (bw.getPropertyType("domainId")!=null) {
            exp = exp.and(builder.get("domainId").equal(CloudToolsForCore.getDomainId()));
        }
    }
    ...
}
```