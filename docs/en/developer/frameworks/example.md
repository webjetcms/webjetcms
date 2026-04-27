# Sample code

Sample code for creating a new CRUD application in the administration.

## Pug file

Create a pug file with the HTML code of the page. We recommend copying the code, for example, from [audit-notifications.pug](../../../../src/main/webapp/admin/v9/views/pages/apps/audit-notifications.pug). Place the file in the `settings` directory if it is a settings application, or in the `apps` directory if it is a standard application.

```javascript
extends ../../partials/layout

block content

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

After creating a new pug file, you need to add it to `webpack` in the [webpack.common.js](../../../../src/main/webapp/admin/v9/webpack.common.js) file in the plugins section:

```javascript
plugins: [
    ...
    new HtmlWebpackPlugin(generateHtmlPlugins("/settings/menosuborubezpripony"))
    ...
]
```

## Spring DATA repository and JPA Entity/Bean

Create the JPA bean in the appropriate package, typically `sk.iway.iwcm.components.MENOMODULU` with the same name as the table name and the Entity extension.

**Note:** for newly created entities, the class name consists of TableName and the Entity suffix. So ```MenoTabulkyEntity.java```. The ```Bean``` suffix is ​​used for classes carrying data that are not directly JPA entities. In the old code, JPA entities also use the Bean suffix, but for newly created classes, please follow this rule.

See the content of the [AuditNotifyEntity](../../../../src/main/java/sk/iway/iwcm/system/audit/jpa/AuditNotifyEntity.java) class as an example:

```java
package sk.iway.iwcm.system.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

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

Note that the class has no `getter/setter`, these are automatically generated via [lombook](https://projectlombok.org) annotations `@Getter,@Setter`. If you get errors while compiling, install the plugin from the lombook website into your development environment.

Also note the ```@DataTableColumn``` annotations for the datatable. More details can be found in the [annotation documentation](../datatables-editor/datatable-columns.md).

The annotation ```@GeneratedValue``` uses an autoincrement column (```GenerationType.IDENTITY```) by default. Older versions of Oracle do not support autoincrement columns but use sequences to get the ID value. The sequence name is in the attribute ```generator```, typically we name the sequence as ```S_meno_tabulky```.

The Spring DATA repository is typically very simple, an example in [AuditNotifyRepository](../../../../src/main/java/sk/iway/iwcm/system/audit/jpa/AuditNotifyRepository.java):

```java
package sk.iway.iwcm.system.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditNotifyRepository extends JpaRepository<AuditNotifyEntity, Long> {

}
```

If the data also contains filtering by dates, it is necessary for the repository to also extend ```JpaSpecificationExecutor<UrlRedirectBean>```, an example is [RedirectsRepository.java](../../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

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

I recommend having the repository and JPA bean in the same package. In order for WebJET to initialize the JPA repository, it is then necessary to add the package to [sk.iway.webjet.v9.V9JpaDBConfig.java](../../../../src/main/java/sk/iway/webjet/v9/V9JpaDBConfig.java) to the annotation ```@EnableJpaRepositories```, **and also to**, ```emf.setPackagesToScan```. The JPA bean itself is initialized when WebJET starts, if you have it in a package other than ak.iway.iwcm.components or sk.iway.iwcm.system and JPA is not working for you, add a new variable in the WebJET administration in Settings->Configuration with the name `jpaAddPackages` and the value of the package of your JPA entity.

## Rest service

The Rest service is quite simple, most of it is implemented in the super class [DatatableRestControllerV2.java](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java). An example is [RedirectRestController.java](../../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java):

If your REST service does not initialize after startup, you need to add its package to the list of initialized Spring packages in the class [sk.iway.webjet.v9.V9SpringConfig.java](../../../../src/main/java/sk/iway/webjet/v9/V9SpringConfig.java)

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

Be careful of the annotation [@PreAuthorize](https://docs.webjetcms.sk/#/back-end/spring/rest-url) which ensures module rights control. It is also necessary that @RequestMapping be in the form `/admin/rest/MODULENAME` if it is an application, or `/admin/rest/settings/MODULENAME` if it is a part of the Control Panel.

In the sample code, the beforeSave method is overridden (there are also editItem and insertItem methods for different settings when editing and creating a record). This ensures that the last change date is set. If you are overriding methods from DatatableRestControllerV2, try to override entity methods, not rest methods.

If you need to solve more advanced rights control (e.g. for the upload directory), you can take inspiration from [GalleryRestController.java](../../../../src/main/java/sk/iway/iwcm/components/gallery/GalleryRestController.java) in the checkAccessAllowed (check when displaying data) and validateEditor (check when editing data) methods, where you can implement specific code.

The beforeDelete method is called before deleting an entity, you can, for example, delete the relevant file or perform another action before deleting data from the database (e.g. move data to the archive).

Sometimes it is necessary to transfer code table data (e.g. options to select fields). For detailed information, see the [Code tables for select boxes](https://github.com/webjetcms/webjetcms/blob/main/docs/datatables/restcontroller.md#%C4%8D%C3%ADseln%C3%ADky-pre-select-boxy) documentation, but an example is the [AuditNotifyRestController](../../../../src/main/java/sk/iway/iwcm/system/audit/rest/AuditNotifyRestController.java) class where in the getAllItems method an object `adminlogType` with a list of options for the select field is added to the response:

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