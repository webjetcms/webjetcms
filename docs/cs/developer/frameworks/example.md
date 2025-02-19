# Ukázka kódu

Ukázka kódu pro vytvoření nové aplikace CRUD v administraci.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Ukázka kódu](#vzorový-kód)
  - [Soubor Pug](#Soubor-mopslíků)
  - [Úložiště Spring DATA a JPA Entity/Bean](#jarní-datové-úložiště-a-jpa-entitabean)
  - [Rest služba](#odpočinková-služba)

<!-- /code_chunk_output -->

## Soubor Pug

Vytvořte soubor pug s kódem HTML stránky. Doporučujeme zkopírovat kód např. z webu. [audit-notifications.pug](../../../src/main/webapp/admin/v9/views/pages/apps/audit-notifications.pug). Umístěte soubor do adresáře `settings` pokud se jedná o nastavení, nebo do adresáře `apps` pokud se jedná o standardní aplikaci.

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

Po vytvoření nového souboru pug je třeba jej přidat do balíčku webpack v souboru [webpack.common.js](../../../src/main/webapp/admin/v9/webpack.common.js) do sekce zásuvných modulů:

```javascript
plugins: [
    ...
    new HtmlWebpackPlugin(generateHtmlPlugins("/settings/menosuborubezpripony"))
    ...
]
```

## Úložiště Spring DATA a JPA Entity/Bean

Vytvořte bean JPA v příslušném balíčku, typicky en.iway.iwcm.components.MENOMODULE se stejným názvem jako název tabulky a příponou Entity.

**Poznámka:** u nově vytvořených entit se název třídy skládá z NameTable a přípony Entity. To znamená, `MenoTabulkyEntity.java`. Přípona `Bean` se používá pro třídy nesoucí data, které nejsou přímo entitami JPA. Ve starém kódu se i pro entity JPA používá přípona Bean, ale u nově vytvářených tříd dodržujte toto pravidlo.

Viz obsah třídy [AuditNotifyEntity](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyEntity.java) jako příklad:

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

Všimněte si, že třída nemá getter/settrees, ty jsou automaticky generovány prostřednictvím [lombook](https://projectlombok.org) Anotace @Getter a @Setter. Pokud sestavení hlásí chyby, nainstalujte zásuvný modul z webu lombook do vývojového prostředí.

Všimněte si také `@DataTableColumn` anotace pro datovou tabulku. Další informace naleznete v [dokumentace k anotacím](../datatables-editor/datatable-columns.md).

Anotace `@GeneratedValue` používá výchozí sloupec s automatickým zvyšováním (`GenerationType.IDENTITY`). Starší verze systému Oracle však nepodporuje sloupce s automatickým navýšením, ale k získání hodnoty ID používá sekvence. Název sekvence je v atributu `generator`, obvykle tuto posloupnost pojmenováváme jako `S_meno_tabulky`.

Úložiště Spring DATA je typicky velmi jednoduché, příkladem je [AuditNotifyRepository](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyRepository.java):

```java
package sk.iway.iwcm.system.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditNotifyRepository extends JpaRepository<AuditNotifyEntity, Long> {

}
```

Pokud data zahrnují filtrování podle data, musí úložiště také rozšířit funkci `JpaSpecificationExecutor<UrlRedirectBean>`, příkladem je [RedirectsRepository.java](../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

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

Doporučuji mít úložiště a fazoli JPA ve stejném balíčku. Aby WebJET inicializoval úložiště JPA, je třeba přidat balíček do složky [sk.iway.webjet.v9.JpaDBConfig.java](../../../src/main/java/sk/iway/webjet/v9/JpaDBConfig.java) k anotaci `@EnableJpaRepositories`, **a zároveň**, `emf.setPackagesToScan`. Samotný JPA bean se inicializuje při spuštění WebJETu, pokud jej máte v jiném balíčku než ak.iway.iwcm.components nebo sk.iway.iwcm.system a JPA nefunguje, přidejte v administraci WebJETu v Nastavení->Konfigurace novou proměnnou s názvem `jpaAddPackages` a hodnotu balíčku vašich fazolí JPA.

## Rest služba

Rest služba je poměrně jednoduchá, většina z ní je implementována v nadtřídě [DatatableRestControllerV2.java](../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java). Příkladem je [RedirectRestController.java](../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java):

Pokud službu REST po spuštění neinicializujete, musíte její balíček přidat do seznamu inicializovaných balíčků Spring ve třídě [sk.iway.webjet.v9.SpringConfig.java](../../../src/main/java/sk/iway/webjet/v9/SpringConfig.java)

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

Pozor na anotaci [@PreAuthorize](https://docs.webjetcms.sk/#/back-end/spring/rest-url) který zajišťuje kontrolu práv modulu. Také @RequestMapping musí být ve tvaru /admin/rest/MENOMODULE, pokud se jedná o aplikaci, nebo /admin/rest/settings/MENOMODULE, pokud se jedná o součást ovládacího panelu.

V ukázkovém kódu je nadefinována metoda beforeSave (existují také metody editItem a insertItem pro různá nastavení při editaci a vytváření záznamu). Ta zajišťuje nastavení data poslední změny. Pokud přepisujete metody z DatatableRestControllerV2, snažte se přepisovat metody entit, nikoli metody restů.

Pokud potřebujete řešit pokročilejší kontrolu práv (např. v adresáři pro nahrávání), můžete se inspirovat následujícími příklady [GalleryRestController.java](../../../src/main/java/sk/iway/iwcm/components/gallery/GalleryRestController.java) v metodách checkAccessAllowed (kontrola při zobrazení dat) a validateEditor (kontrola při úpravě dat), kde můžete implementovat konkrétní kód.

Metoda beforeDelete se volá před odstraněním entity, například můžete odstranit příslušný soubor nebo provést jinou akci před odstraněním dat z databáze (např. přesunout data do archivu).

Někdy je nutné přenést číselníková data (např. možnosti výběru/výběru polí). Podrobné informace naleznete v části [dokumentace Číselníky pro výběrová pole](https://github.com/webjetcms/webjetcms/blob/main/docs/datatables/restcontroller.md#%C4%8D%C3%ADseln%C3%ADky-pre-select-boxy), ale příkladem je třída [AuditNotifyRestController](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyRestController.java) kde je v metodě getAllItems do odpovědi přidán objekt adminlogType se seznamem možností pro pole výběru:

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
