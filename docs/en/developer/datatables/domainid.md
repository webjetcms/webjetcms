# Domain data separation

It is often necessary in a multi-domain installation to separate application data for a domain (each domain has its own data in the application). WebJET supports such separation by adding a database column ```domain_id```, which represents the domain ID (technically it is the same as the first directory ID in the given domain) to the database table (e.g. ```questions_answers```). Subsequently, when selecting data from the table, it is filtered based on the currently selected domain according to the column ```domain_id``` in the database.

Setting up multi-domain mode is described in [template settings](../../frontend/setup/README.md#multi-domain-management), it is important to set the configuration variable ```enableStaticFilesExternalDir=true```, which turns on domain data separation (by default, common data is used in multidomain mode, the conf. variable is set to ```false``` by default).

You can get the value for column ```domain_id``` in Java code by calling ```CloudToolsForCore.getDomainId()```.

![](../../redactor/webpages/domain-select.png)

## Entity

The Java entity must contain the column ```domainId```:

```java
    @Column(name = "domain_id")
	@DataTableColumn(
        inputType = DataTableColumnType.HIDDEN
    )
	private Integer domainId;
```

and of course the column ```domain_id``` must exist in the database:

```sql
ALTER TABLE table_name ADD domain_id int DEFAULT 1 NOT NULL;
```

## Repository

WebJET has a ready-made basic repository that automatically supports all necessary operations to ensure the separation of domain data; in your implementation, you just need to extend your repository from the ```DomainIdRepository<T, ID>``` class:

```java
package sk.iway.iwcm.components.qa;

import org.springframework.stereotype.Repository;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;
import java.util.List;

@Repository
public interface QuestionsAnswersRepository extends DomainIdRepository<QuestionsAnswersEntity, Long> {

}
```

```DatatableRestControllerV2``` will then provide additional data filtering according to the currently selected domain.

## REST interface

When overloading the ```getOneItem``` method (or other ```Item``` methods), always check the correctness of your implementation. Alternatively, if you just need to add data, use the ```super.get...``` call, which will check the domain and only then process the next code:

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

## Implementation details

In class ```DatatableRestControllerV2```, the ```DomainIdRepository``` repository support is detected in the constructor, which causes the variable ```checkDomainId``` to be set to the value ```true```.

```java
    if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
		if (repo instanceof DomainIdRepository) checkDomainId = true;
	}
```

This repository defines methods for retrieving data by domain ID:

```java
package sk.iway.iwcm.system.datatable.spring;

@NoRepositoryBean
public interface DomainIdRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    Optional<T> findFirstByIdAndDomainId(Long id, int domainId);

    List<T> findAllByDomainId(int domainId);
    Page<T> findAllByDomainId(int domainId, Pageable pageable);

}
```

The variable ```checkDomainId``` is then used in ```DatatableRestControllerV2``` to test whether it is a repository with multi-domain support and then the appropriate methods are called (e.g. ```findAllByDomainId```):

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

Similarly, the domain ID is used when retrieving, editing, or deleting a record (deletion is technically done by retrieving the entity by calling ```findFirstByIdAndDomainId``` and then calling ```delete```). When searching, ```JpaSpecificationExecutor``` is used to add a condition to the search:

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