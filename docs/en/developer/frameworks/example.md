# Sample code

Sample code for creating a new CRUD application in the administration.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Sample code](#ukážkový-kód)
	- [Pug file](#pug-súbor)
	- [Spring DATA repository and JPA Entity/Bean](#spring-data-repozitár-a-jpa-entitabean)
	- [Rest service](#rest-služba)
<!-- /code_chunk_output -->

## Pug file

Create a pug file with the HTML code of the page. We recommend that you copy the code from e.g. [audit-notifications.pug](../../../src/main/webapp/admin/v9/views/pages/apps/audit-notifications.pug). Place the file in the directory `settings` if it is a setting, or to a directory `apps` if it is a standard application.

```javascript
extends ../../partials/layout

block content

    +breadcrumb(
        'audit-notifications',
        [
            [ '#', 'Zoznam notifikácií', false ]
        ],
        [ '/admin/v9/apps/audit-search/', 'Audit' ]
    )

    script.
        let auditNotificationsTable;
        window.domReady.add(function () {
            let url = "/admin/rest/audit/notify";
            let columns = [(${layout.getDataTableColumns("sk.iway.iwcm.system.audit.AuditNotifyEntity")})];
            //console.log(columns);

            auditNotificationsTable = WJ.DataTable({
                url: url,
                serverSide: true,
                columns: columns
            });
            //vypni zobrazenie tlacitka import
            auditNotificationsTable.hideButton("import");
        });

    <table class="datatableInit table"></table>
```

After creating a new pug file, you need to add it to the webpack in the file [webpack.common.js](../../../src/main/webapp/admin/v9/webpack.common.js) to the plugins section:

```javascript
plugins: [
    ...
    new HtmlWebpackPlugin(generateHtmlPlugins("/settings/menosuborubezpripony"))
    ...
]
```

## Spring DATA repository and JPA Entity/Bean

Create the JPA bean in the appropriate package, typically en.iway.iwcm.components.MENOMODULE with the same name as the table name and the suffix Entity.

**Remark:** for newly created entities, the class name consists of the NameTable and the suffix Entity. That is, `MenoTabulkyEntity.java`. Suffix `Bean` is used for data-bearing classes that are not directly JPA entities. In the old code even JPA entities use the suffix Bean, but for newly created classes please follow this rule.

See class content [AuditNotifyEntity](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyEntity.java) as an example:

```java
package sk.iway.iwcm.system.audit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;

@Entity
@Table(name = "adminlog_notify")
@Getter
@Setter
public class AuditNotifyEntity {

	public AuditNotifyEntity() {
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_adminlog_notify")
	@Column(name = "adminlog_notify_id")
	@DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@Column(name = "adminlog_type")
	@DataTableColumn(inputType = { DataTableColumnType.OPEN_EDITOR,
			DataTableColumnType.SELECT }, renderFormat = "dt-format-select", editor = {
					@DataTableColumnEditor(type = "select") })
	private Integer adminlogType;

	@Size(max = 255)
	@Column(name = "text")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, renderFormat = "dt-format-text", editor = {
			@DataTableColumnEditor(type = "text") })
	private String text;

	@Size(max = 255)
	@Column(name = "email")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, renderFormat = "dt-format-mail", editor = {
			@DataTableColumnEditor(type = "text") })
	private String email;
}
```

note that the class has no getter/settrees, these are automatically generated via [lombook](https://projectlombok.org) @Getter and @Setter annotations. If the build reports errors, install the plugin from the lombook site into your development environment.

Also note `@DataTableColumn` annotations for the datatable. More is in [documentation for annotations](../datatables-editor/datatable-columns.md).

Annotation `@GeneratedValue` uses the default autoincrement column (`GenerationType.IDENTITY`). But older version of Oracle does not support autoincrement columns but uses sequences to get ID value. The name of the sequence is in the attribute `generator`, we typically name the sequence as `S_meno_tabulky`.

Spring DATA repository is typically very simple, example in [AuditNotifyRepository](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyRepository.java):

```java
package sk.iway.iwcm.system.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditNotifyRepository extends JpaRepository<AuditNotifyEntity, Long> {

}
```

If the data includes filtering by dates, the repository also needs to extend the `JpaSpecificationExecutor<UrlRedirectBean>`, an example is [RedirectsRepository.java](../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

```java
package sk.iway.iwcm.system;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.UrlRedirectBean;

@Repository
public interface RedirectsRepository extends JpaRepository<UrlRedirectBean, Long>, JpaSpecificationExecutor<UrlRedirectBean> {

}
```

I recommend having the repository and the JPA bean in the same package. In order for WebJET to initialize the JPA repository you need to add the package to [sk.iway.webjet.v9.JpaDBConfig.java](../../../src/main/java/sk/iway/webjet/v9/JpaDBConfig.java) to the annotation `@EnableJpaRepositories`, **and at the same time to**, `emf.setPackagesToScan`. The JPA bean itself is initialized when WebJET starts, if you have it in a different package than ak.iway.iwcm.components or sk.iway.iwcm.system and the JPA is not working add a new variable in the WebJET administration in Settings->Configuration called `jpaAddPackages` and the package value of your JPA beans.

## Rest service

Rest service is quite simple, most of it is implemented in super class [DatatableRestControllerV2.java](../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java). An example is [RedirectRestController.java](../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java):

If you don't initialize the REST service after startup, you need to add its package to the list of initialized Spring packages in the class [sk.iway.webjet.v9.SpringConfig.java](../../../src/main/java/sk/iway/webjet/v9/SpringConfig.java)

```java
package sk.iway.iwcm.components.redirects;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.system.RedirectsRepository;
import sk.iway.iwcm.system.UrlRedirectBean;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;


@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/redirect")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
public class RedirectRestController extends DatatableRestControllerV2<UrlRedirectBean, Long> {

    @Autowired
    public RedirectRestController(RedirectsRepository redirectsRepository) {
        super(redirectsRepository);
    }

    @Override
    public void beforeSave(UrlRedirectBean entity) {
        //nastav datum ulozenia
        entity.setInsertDate(new Date());

    @Override
    public UrlRedirectBean insertItem(UrlRedirectBean entity) {
        entity.setInsertDate(new Date());
        //ak nebol zadany kod presmerovania, nastav na predvoleny kod 301
        if (entity.getRedirectCode() == null) entity.setRedirectCode(301);
        return super.insertItem(entity);
    }
}
```

Watch out for the annotation [@PreAuthorize](https://docs.webjetcms.sk/#/back-end/spring/rest-url) which provides rights control on the module. Also, @RequestMapping needs to be in the form /admin/rest/MENOMODULE if it is an application, or /admin/rest/settings/MENOMODULE if it is a part to the Control Panel.

In the sample code, the beforeSave method is overridden (there are also editItem and insertItem methods for different settings when editing and creating a record). The latter provides for setting the last modified date. If you override methods from DatatableRestControllerV2 try to override entity methods, not rest methods.

If you need to deal with more advanced rights control (e.g. on the upload directory) you can take inspiration from [GalleryRestController.java](../../../src/main/java/sk/iway/iwcm/components/gallery/GalleryRestController.java) in the checkAccessAllowed (check when data is displayed) and validateEditor (check when data is edited) methods, where you can implement specific code.

The beforeDelete method is called before deleting an entity, for example, you can delete the corresponding file or perform another action before deleting data from the database (e.g. move data to the archive).

Sometimes it is necessary to transfer dial data (e.g. options to select/select fields). For detailed information, see [documentation Dialers for select boxes](https://github.com/webjetcms/webjetcms/blob/main/docs/datatables/restcontroller.md#%C4%8D%C3%ADseln%C3%ADky-pre-select-boxy), but an example is the class [AuditNotifyRestController](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyRestController.java) where in the getAllItems method an adminlogType object is added to the response with a list of options for the selection field:

```java
package sk.iway.iwcm.system.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/audit/notify")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_adminlog')")
public class AuditNotifyRestController extends DatatableRestControllerV2<AuditNotifyEntity, Long> {

	private AuditNotifyRepository auditNotifyRepository;
	private AuditService auditService;

	@Autowired
	public AuditNotifyRestController(AuditNotifyRepository auditNotifyRepository, AuditService auditService) {
		super(auditNotifyRepository);
		this.auditNotifyRepository = auditNotifyRepository;
		this.auditService = auditService;
	}

	@Override
	public Page<AuditNotifyEntity> getAllItems(Pageable pageable) {
		DatatablePageImpl<AuditNotifyEntity> pages = new DatatablePageImpl<>(auditNotifyRepository.findAll(pageable));
		pages.addOptions("adminlogType", auditService.getTypes(getRequest()), "label", "value", false);

		return pages;
	}

}
```
