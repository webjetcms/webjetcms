# Ukázkový kód

Ukázkový kód vytvoření nové CRUD aplikace v administraci.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Ukázkový kód](#ukázkový-kód)
  - [Pug soubor](#pug-soubor)
  - [Spring DATA repozitář a JPA Entita/Bean](#spring-data-repozitář-a-jpa-entitabean)
  - [Rest služba](#rest-služba)

<!-- /code_chunk_output -->

## Pug soubor

Vytvořte si pug soubor s HTML kódem stránky. Doporučujeme vám zkopírovat kód např. z [audit-notifications.pug](../../../src/main/webapp/admin/v9/views/pages/apps/audit-notifications.pug). Soubor umístěte do adresáře `settings` pokud se jedná o nastavení, nebo do adresáře `apps` pokud se jedná o standardní aplikaci.

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

Po vytvoření nového pug souboru je třeba jej přidat do webpacku v souboru [webpack.common.js](../../../src/main/webapp/admin/v9/webpack.common.js) do sekce plugins:

```javascript
plugins: [
    ...
    new HtmlWebpackPlugin(generateHtmlPlugins("/settings/menosuborubezpripony"))
    ...
]
```

## Spring DATA repozitář a JPA Entita/Bean

JPA bean vytvořte v příslušném package, typicky sk.iway.iwcm.components.MENOMODULU se stejným názvem jako jméno tabulky a suffixem Entity.

**Poznámka:** pro nově vytvářené entity platí, že jméno třídy se skládá z JménoTabulky a suffixu Entity. Čili `MenoTabulkyEntity.java`. Suffix `Bean` se používá na třídy nesoucí data, která nejsou přímo JPA entity. Ve starém kódu ale i JPA entity používají suffix Bean, pro nově vytvářené třídy ale prosím dodržte toto pravidlo.

Prohlédněte si obsah třídy [AuditNotifyEntity](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyEntity.java) jako příklad:

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

všimněte si, že třída nemá žádné gettere/settre, ty se automaticky vygenerují přes [lombook](https://projectlombok.org) anotace @Getter a @Setter. Pokud vám kompilace hlásí chyby, nainstalujte si z lombook stránky plugin do vašeho vývojového prostředí.

Také si všimněte `@DataTableColumn` anotace pro datatabulku. Více je v [dokumentací k anotacím](../datatables-editor/datatable-columns.md).

Anotace `@GeneratedValue` používá ve výchozím nastavení autoincrement sloupec (`GenerationType.IDENTITY`). Starší verze Oracle ale nepodporuje autoincrement sloupce, ale používá sekvence pro získání ID hodnoty. Jméno sekvence je v atributu `generator`, typicky pojmenováváme sekvenci jako `S_meno_tabulky`.

Spring DATA repozitář je typicky velmi jednoduchý, příklad v [AuditNotifyRepository](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyRepository.java):

```java
package sk.iway.iwcm.system.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditNotifyRepository extends JpaRepository<AuditNotifyEntity, Long> {

}
```

Pokud data obsahují filtrování i podle dat je třeba aby repozitář rozšiřoval i `JpaSpecificationExecutor<UrlRedirectBean>`, příkladem je [RedirectsRepository.java](../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

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

Repozitář a JPA bean doporučuji mít ve stejném package. Aby WebJET inicializoval JPA repozitář je následně třeba ještě přidat package do [cs.iway.webjet.v9.JpaDBConfig.java](../../../src/main/java/sk/iway/webjet/v9/JpaDBConfig.java) do anotace `@EnableJpaRepositories`, **a zároveň do**, `emf.setPackagesToScan`. Samotná JPA beana se inicializuje při startu WebJETu, pokud ji máte v jiném package než ak.iway.iwcm.components nebo sk.iway.iwcm.system a JPA vám nefunguje přidejte v administraci WebJETu v Nastavení->Konfigurace novou proměnnou s názvem `jpaAddPackages` a hodnotou package vaší JPA beany.

## Rest služba

Rest služba je poměrně jednoduchá, většina je implementována v super třídě [DatatableRestControllerV2.java](../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java). Příkladem je [RedirectRestController.java](../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java):

Pokud se vám REST služba po startu neinicializuje je třeba přidat její package do seznamu inicializovaných Spring packages ve třídě [cs.iway.webjet.v9.SpringConfig.java](../../../src/main/java/sk/iway/webjet/v9/SpringConfig.java)

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

Dejte si pozor na anotaci [@PreAuthorize](https://docs.webjetcms.sk/#/back-end/spring/rest-url) která zajišťuje kontrolu práv na modul. Rovněž je třeba aby @RequestMapping byl ve tvaru /admin/rest/JMENOMODULU pokud se jedná o aplikaci, nebo /admin/rest/settings/JMENOMODULU pokud se jedná o část do Ovládacího panelu.

V ukázkovém kódu je přepsána metoda beforeSave (existují také metody editItem a insertItem pro rozdílné nastavení při editaci a vytvoření záznamu). Ta zajišťuje nastavení data poslední změny. Pokud přepisujete metody z DatatableRestControllerV2 snažte se přepsat entitní metody, ne restové.

Pokud potřebujete řešit pokročilejší kontrolu práv (např. na adresář pro upload) můžete se inspirovat v [GalleryRestController.java](../../../src/main/java/sk/iway/iwcm/components/gallery/GalleryRestController.java) v metodách checkAccessAllowed (kontrola při zobrazení dat) a validateEditor (kontrola při editaci dat), kde můžete implementovat specifický kód.

Metoda beforeDelete se jmenuje před smazáním entity, můžete v ní například. smazat příslušný soubor nebo provést jinou akci před smazáním dat z databáze (např. přesunout data do archivu).

Někdy je třeba přenášet číselníková data (např. možnosti do výběrových/select polí). Podrobné informace naleznete v [dokumentaci Číselníky pro select boxy](https://github.com/webjetcms/webjetcms/blob/main/docs/datatables/restcontroller.md#%C4%8D%C3%ADseln%C3%ADky-pre-select-boxy), příkladem je ale třída [AuditNotifyRestController](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyRestController.java) kde v metodě getAllItems je do response přidán objekt adminlogType se seznamem možností pro výběrové pole:

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
