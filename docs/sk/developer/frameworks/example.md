# Ukážkový kód

Ukážkový kód vytvorenia novej CRUD aplikácie v administrácii.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Ukážkový kód](#ukážkový-kód)
  - [Pug súbor](#pug-súbor)
  - [Spring DATA repozitár a JPA Entita/Bean](#spring-data-repozitár-a-jpa-entitabean)
  - [Rest služba](#rest-služba)

<!-- /code_chunk_output -->


## Pug súbor

Vytvorte si pug súbor s HTML kódom stránky. Odporúčame vám skopírovať kód napr. z [audit-notifications.pug](../../../src/main/webapp/admin/v9/views/pages/apps/audit-notifications.pug). Súbor umiestnite do adresára `settings` ak sa jedná o nastavenia, alebo do adresára `apps` ak sa jedná o štandardnú aplikáciu.

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

Po vytvorení nového pug súboru je potrebné ho pridať do webpacku v súbore [webpack.common.js](../../../src/main/webapp/admin/v9/webpack.common.js) do sekcie plugins:

```javascript
plugins: [
    ...
    new HtmlWebpackPlugin(generateHtmlPlugins("/settings/menosuborubezpripony"))
    ...
]
```

## Spring DATA repozitár a JPA Entita/Bean

JPA bean vytvorte v príslušnom package, typicky sk.iway.iwcm.components.MENOMODULU s rovnakým názvom ako meno tabuľky a suffixom Entity.

**Poznámka:** pre novo vytvárané entity platí, že meno triedy sa skladá z MenoTabulky a suffixu Entity. Čiže ```MenoTabulkyEntity.java```. Suffix ```Bean``` sa používa na triedy nesúce dáta, ktoré nie sú priamo JPA entitmi. V starom kóde ale aj JPA entity používajú suffix Bean, pre novo vytvárané triedy ale prosím dodržte toto pravidlo.

Pozrite si obsah triedy [AuditNotifyEntity](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyEntity.java) ako príklad:

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

všimnite si, že trieda nemá žiadne gettere/settre, tie sa automaticky vygenerujú cez [lombook](https://projectlombok.org) anotácie @Getter a @Setter. Ak vám kompilácia hlási chyby, nainštalujte si z lombook stránky plugin do vášho vývojového prostredia.

Tiež si všimnite ```@DataTableColumn``` anotácie pre datatabuľku. Viac je v [dokumentácií k anotáciám](../datatables-editor/datatable-columns.md).

Anotácia ```@GeneratedValue``` používa predvolene autoincrement stĺpec (```GenerationType.IDENTITY```). Staršia verzia Oracle ale nepodporuje autoincrement stĺpce ale používa sekvencie pre získanie ID hodnoty. Meno sekvencie je v atribúte ```generator```, typicky pomenováme sekvenciu ako ```S_meno_tabulky```.

Spring DATA repozitár je typicky veľmi jednoduchý, príklad v [AuditNotifyRepository](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyRepository.java):

```java
package sk.iway.iwcm.system.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditNotifyRepository extends JpaRepository<AuditNotifyEntity, Long> {

}
```

Ak dáta obsahujú filtrovanie aj podľa dátumov je potrebné aby repozitár rozširoval aj ```JpaSpecificationExecutor<UrlRedirectBean>```, príkladom je [RedirectsRepository.java](../../../src/main/java/sk/iway/iwcm/system/RedirectsRepository.java):

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

Repozitár a JPA bean odporúčam mať v rovnakom package. Aby WebJET inicializoval JPA repozitár je následne potrebné ešte pridať package do [sk.iway.webjet.v9.JpaDBConfig.java](../../../src/main/java/sk/iway/webjet/v9/JpaDBConfig.java) do anotácie ```@EnableJpaRepositories```, **a zároveň do**, ```emf.setPackagesToScan```. Samotná JPA beana sa inicializuje pri štarte WebJETu, ak ju máte v inom package ako ak.iway.iwcm.components alebo sk.iway.iwcm.system a JPA vám nefunguje pridajte v administrácii WebJETu v Nastavenia->Konfigurácia novú premennú s názvom `jpaAddPackages` a hodnotou package vašej JPA beany.

## Rest služba

Rest služba je pomerne jednoduchá, väčšina je implementovaná v super triede [DatatableRestControllerV2.java](../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java). Príkladom je [RedirectRestController.java](../../../src/main/java/sk/iway/iwcm/components/redirects/RedirectRestController.java):

Ak sa vám REST služba po štarte neinicializuje je potrebné pridať jej package do zoznamu inicializovaných Spring packages v triede [sk.iway.webjet.v9.SpringConfig.java](../../../src/main/java/sk/iway/webjet/v9/SpringConfig.java)

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

Dajte si pozor na anotáciu [@PreAuthorize](https://docs.webjetcms.sk/#/back-end/spring/rest-url) ktorá zabezpečuje kontrolu práv na modul. Rovnako je potrebné aby @RequestMapping bol v tvare /admin/rest/MENOMODULU ak sa jedná o aplikáciu, alebo /admin/rest/settings/MENOMODULU ak sa jedná o časť do Ovládacieho panelu.

V ukážkovom kóde je prepísaná metóda beforeSave (existujú aj metódy editItem a insertItem pre rozdielne nastavenie pri editácii a vytvorení záznamu). Tá zabezpečuje nastavenie dátumu poslednej zmeny. Ak prepisujete metódy z DatatableRestControllerV2 snažte sa prepísať entitné metódy, nie restové.

Ak potrebujete riešiť pokročilejšiu kontrolu práv (napr. na adresár pre upload) môžete sa inšpirovať v [GalleryRestController.java](../../../src/main/java/sk/iway/iwcm/components/gallery/GalleryRestController.java) v metódach checkAccessAllowed (kontrola pri zobrazenie dát) a validateEditor (kontrola pri editácii dát), kde môžete implementovať špecifický kód.

Metóda beforeDelete sa volá pred zmazaním entity, môžete v nej napr. zmazať príslušný súbor alebo vykonať inú akciu pred zmazaním dát z databázy (napr. presunúť dáta do archívu).

Niekedy je potrebné prenášať číselníkové dáta (napr. možnosti do výberových/select polí). Podrobné informácie nájdete v [dokumentácii Číselníky pre select boxy](https://github.com/webjetcms/webjetcms/blob/main/docs/datatables/restcontroller.md#%C4%8D%C3%ADseln%C3%ADky-pre-select-boxy), príkladom je ale trieda [AuditNotifyRestController](../../../src/main/java/sk/iway/iwcm/system/audit/AuditNotifyRestController.java) kde v metóde getAllItems je do response pridaný objekt adminlogType so zoznamom možností pre výberové pole:

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