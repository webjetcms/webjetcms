# Oddělení dat domény

Při instalaci s více doménami je často nutné oddělit data aplikace pro doménu (každá doména má v aplikaci externě oddělená data). WebJET takové oddělení podporuje přidáním databázového sloupce `domain_id`, které představuje ID domény (technicky stejné jako ID prvního adresáře v doméně) v databázové tabulce (např. `questions_answers`). Při výběru dat z tabulky se pak data filtrují podle aktuálně vybrané domény podle sloupce `domain_id` v databázi.

Nastavení multidoménového režimu je popsáno v části [nastavení šablony](../../frontend/setup/README.md#správa-více-domén), nastavení konfigurační proměnné je důležité `enableStaticFilesExternalDir=true` který umožňuje oddělit data domén (ve výchozím nastavení se v režimu multidomény používají společná data, proměnná conf. je nastavena na hodnotu `false`).

Hodnota sloupce `domain_id` lze v kódu Javy získat voláním `CloudToolsForCore.getDomainId()`.

![](../../redactor/webpages/domain-select.png)

## Entita

Entita Java musí obsahovat sloupec `domainId`:

```java
    @Column(name = "domain_id")
	@DataTableColumn(
        inputType = DataTableColumnType.HIDDEN
    )
	private Integer domainId;
```

a samozřejmě musí být v databázi sloupec `domain_id`:

```sql
ALTER TABLE table_name ADD domain_id int DEFAULT 1 NOT NULL;
```

## Úložiště

WebJET připravil základní úložiště, které automaticky podporuje všechny potřebné operace pro zajištění oddělení doménových dat, ve vaší implementaci stačí rozšířit úložiště z třídy `DomainIdRepository<T, ID>`:

```java
package sk.iway.iwcm.components.qa;

import org.springframework.stereotype.Repository;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;
import java.util.List;

@Repository
public interface QuestionsAnswersRepository extends DomainIdRepository<QuestionsAnswersEntity, Long> {

}
```

přímo `DatatableRestControllerV2` pak zajistí, že filtrování dat bude dokončeno podle aktuálně vybrané domény.

## Rozhraní REST

Když je metoda ve vaší implementaci přetížená. `getOneItem` (nebo jiné `Item` metody) vždy zkontrolujte správnost své implementace. Pokud potřebujete pouze přidat data, použijte volání `super.get...`, který zkontroluje doménu a poté zpracuje další kód:

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

Ve třídě `DatatableRestControllerV2` podpora je detekována v konstruktoru `DomainIdRepository` úložiště, což způsobí nastavení proměnné `checkDomainId` na hodnotu `true`.

```java
    if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
		if (repo instanceof DomainIdRepository) checkDomainId = true;
	}
```

Toto úložiště definuje metody pro vyhledávání dat podle ID domény:

```java
package sk.iway.iwcm.system.datatable.spring;

@NoRepositoryBean
public interface DomainIdRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    Optional<T> findFirstByIdAndDomainId(Long id, int domainId);

    List<T> findAllByDomainId(int domainId);
    Page<T> findAllByDomainId(int domainId, Pageable pageable);

}
```

Proměnná `checkDomainId` je následně v `DatatableRestControllerV2` se otestuje, zda se jedná o multidoménové úložiště, a poté se zavolají příslušné metody (např. `findAllByDomainId`):

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

Podobně se ID domény používá při načítání, úpravě nebo mazání záznamu (mazání se technicky provádí načtením entity voláním `findFirstByIdAndDomainId` a pak zavolání `delete`). Vyhledávání používá `JpaSpecificationExecutor` který přidá do vyhledávání podmínku:

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
