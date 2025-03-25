# Oddělení údajů domén

Často je třeba ve více doménové instalaci oddělit data aplikace pro doménu (každá doména má navenek samostatná data v aplikaci). WebJET takové oddělení podporuje přidáním databázového sloupce `domain_id`, který reprezentuje ID domény (technicky je shodný s prvním ID adresáře v dané doméně) do databázové tabulky (např. `questions_answers`). Následně se při výběru dat z tabulky tyto filtrují na základě aktuálně zvolené domény podle sloupce `domain_id` v databázi.

Nastavení režimu více domén je popsáno v [nastavení šablon](../../frontend/setup/README.md#správa-více-domén), důležité je nastavení konfigurační proměnné `enableStaticFilesExternalDir=true`, který zapne oddělení dat domén (výchozí se v multidomain režim používají společná data, konf. proměnná je ve výchozím nastavení nastavena na `false`).

Hodnotu pro sloupec `domain_id` získáte v Java kódu voláním `CloudToolsForCore.getDomainId()`.

![](../../redactor/webpages/domain-select.png)

## Entita

Java entita musí obsahovat sloupec `domainId`:

```java
    @Column(name = "domain_id")
	@DataTableColumn(
        inputType = DataTableColumnType.HIDDEN
    )
	private Integer domainId;
```

a samozřejmě v databázi musí existovat sloupec `domain_id`:

```sql
ALTER TABLE table_name ADD domain_id int DEFAULT 1 NOT NULL;
```

## Repozitář

WebJET má připravený základní repozitář, který automaticky podporuje všechny potřebné operace pro zabezpečení oddělení údajů domén, ve vaší implementaci stačí rozšířit váš repozitář ze třídy `DomainIdRepository<T, ID>`:

```java
package sk.iway.iwcm.components.qa;

import org.springframework.stereotype.Repository;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;
import java.util.List;

@Repository
public interface QuestionsAnswersRepository extends DomainIdRepository<QuestionsAnswersEntity, Long> {

}
```

přímo `DatatableRestControllerV2` následně zajistí doplnění filtrování údajů podle aktuálně zvolené domény.

## REST rozhraní

Při vaší implementaci při přetížení metody `getOneItem` (nebo jiných `Item` metod) vždy zkontrolujte správnost vaší implementace. Případně pokud potřebujete jen doplnit údaje využijte volání `super.get...`, které zajistí kontrolu domény a až následně zpracujte další kód:

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

## Implementační detaily

Ve třídě `DatatableRestControllerV2` se v konstruktoru detekuje podpora `DomainIdRepository` repozitáře, což způsobí nastavení proměnné `checkDomainId` na hodnotu `true`.

```java
    if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
		if (repo instanceof DomainIdRepository) checkDomainId = true;
	}
```

Tento repozitář definuje metody pro získání údajů podle ID domény:

```java
package sk.iway.iwcm.system.datatable.spring;

@NoRepositoryBean
public interface DomainIdRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    Optional<T> findFirstByIdAndDomainId(Long id, int domainId);

    List<T> findAllByDomainId(int domainId);
    Page<T> findAllByDomainId(int domainId, Pageable pageable);

}
```

Proměnná `checkDomainId` se následně v `DatatableRestControllerV2` používá k testování, zda se jedná o repozitář s podporou více domén a následně se jmenují příslušné metody (např. `findAllByDomainId`):

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

Podobně se ID domény používá při získání záznamu, jeho editaci nebo mazání (mazání se technicky provede získáním entity voláním `findFirstByIdAndDomainId` a následným voláním `delete`). Při vyhledávání se využívá `JpaSpecificationExecutor` pomocí kterého se k vyhledávání přidává podmínka:

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
