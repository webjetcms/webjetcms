# Menu item in the admin section

The administration is based on [data tables](../../developer/datatables/README.md) a [REST interface](../../developer/datatables/restcontroller.md). As a basis, it is necessary to understand their operation according to the documentation for [WebJET CMS Programmer](../../developer/README.md).

In this demonstration we will program the contact management, the table is used `contact` which exists in the standard WebJET CMS installation (but is typically not used).

![](contact.png)

## Backend

The ideal solution uses `Spring DATA` repository, REST controller and datatable generated from `@DataTableColumn`, [Annotations](../../developer/datatables-editor/datatable-columns.md).

Sample JPA entity with annotations `@DataTableColumn`, [for datatable and editor](../../developer/datatables-editor/datatable-columns.md). Also note the annotation `@EntityListeners` for automatic [audit logging](../../developer/backend/auditing.md) when changing the entity.

Mandatory fields and other validations [you set annotations](../../developer/datatables/restcontroller.md#validation---required-fields), `@NotBlank,@Size,@Email` etc.

!>**Warning:** do not use primitive types in the entity `int, long` but only object `Integer, Long` otherwise filtering/searching will not work correctly.

```java
package sk.iway.basecms.contact;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Ukazkova JPA entita pre pouzitie v datatabulke
 * http://docs.webjetcms.sk/v2022/#/custom-apps/admin-menu-item
 */

//nastavenie JPA entity
@Entity
@Table(name = "contact")
//automaticke generovanie getter/setter metod cez lombok
@Getter
@Setter
//nastavenie automatickeho auditovania zmien, dolezite je nastavit Adminlog.TYPE_XXX
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CLIENT_SPECIFIC)
public class ContactEntity {

    @Id
    @Column(name = "contact_id")
	@GeneratedValue(generator = "WJGen_contact")
	@TableGenerator(name = "WJGen_contact", pkColumnValue = "contact")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title="components.contact.property.name")
    @NotBlank
    private String name;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.vatid")
    private String vatid;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.street")
    private String street;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.city")
    private String city;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.zip")
    @Size(min=5, max=8)
    private String zip;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title="components.contact.property.country")
    private String country;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.contact")
    @Email
    private String contact;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.phone")
    private String phone;

}
```

Sample JPA repository, when using it always use also `Pageable` object for paging and layout. To support dynamic generation of search criteria (in the REST controller method `addSpecSearch`) the repository also extends `JpaSpecificationExecutor`.

```java
package sk.iway.basecms.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Ukazkovy Spring DATA repozitar pre pouzitie v datatabulke
 * http://docs.webjetcms.sk/v2022/#/custom-apps/admin-menu-item
 */
public interface ContactRepository extends JpaRepository<ContactEntity, Long>, JpaSpecificationExecutor<ContactEntity> {

    //citanie firiem podla zadanej krajiny pre MVC ukazku
    public Page<ContactEntity> findAllByCountry(String country, Pageable pageable);

}
```

!>**Warning:** note the use of `JpaSpecificationExecutor`. It allows dynamic creation of SQL statement for searching/filtering/arranging records in the datatable. If you wouldn't use it, it searches in the form of [Query By Example](https://www.baeldung.com/spring-data-query-by-example) when **You must not** use primitive types and initialization values in Entity (e.g. `String text="test"`). When searching, the value would then be `test` automatically searched even when not entered. This solves `JpaSpecificationExecutor`, which searches only for the parameters specified in the filter (by testing request parameters starting with `search`).

Sample REST controller, always pay attention to checking rights via annotation `@PreAuthorize` and don't forget the annotation `@Datatable` for correct generation of error responses.

In the method `getOptions` sets the options for the country selection field.

In the method `beforeSave` it is possible to set additional (non-editable) data before storing the entity in the database.

```java
package sk.iway.basecms.contact;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Ukazkovy Spring REST pre pouzitie v datatabulke
 * http://docs.webjetcms.sk/v2022/#/custom-apps/admin-menu-item
 */

@RestController
//nastavenie URL adresy REST controllera
@RequestMapping("/admin/rest/apps/contact/")
//nastavenie kontroly prav na alikaciu cmp_contact (tento kluc sa definuje v modinfo.properties)
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_contact')")
@Datatable
public class ContactRestController extends DatatableRestControllerV2<ContactEntity, Long> {

    @SuppressWarnings("unused")
    private final ContactRepository contactRepository;

    @Autowired
    public ContactRestController(ContactRepository contactRepository) {
        super(contactRepository);
        //odlozme si repozitar na pripadne specialne pouzitie
        this.contactRepository = contactRepository;
    }

    @Override
    public void getOptions(DatatablePageImpl<ContactEntity> page) {

        //pridaj zoznam pre pole country
        page.addOptions("country", getCountries(), "label", "value", false);
    }

    @Override
    public void beforeSave(ContactEntity entity) {
        //tu mozete vykonat nastavenie pred ulozenim, napr. nastavit datum poslednej zmeny
    }

    /**
     * Vrati zoznam vyberoveho pola pre krajinu
     * @return
     */
    public static List<LabelValue> getCountries() {
        //vytvor zoznam krajin, toto by sa idealne malo citat z nejakeho ciselnika
        List<LabelValue> countries = new ArrayList<>();
        countries.add(new LabelValue("Slovenská republika", "sk"));
        countries.add(new LabelValue("Česká republika", "cz"));
        countries.add(new LabelValue("Rakúsko", "at"));

        return countries;
    }
}
```

## Frontend

As mentioned above datatables are used for the frontend. The administration is compiled from pug files to html and then displayed.

To make it easy to create a new page in the administration, we have introduced a pre-made page layout template. You will only add the middle part of the page (the template will ensure the generation of the header and menu). [URL mapping](../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) is prepared as `/apps/{app}/admin/` or `/apps/{app}/admin/{subpage}`.

Just prepare the file `/apps/{app}/admin/index.html` or `/apps/{app}/admin/{subpage}.html` Where `{app}` is the name of the directory where the application is located (without accents and spaces), and `{subpage}` is the filename without suffix (if it is not the name index.html).

An example is the application `src/main/webapp/apps/contact/admin/index.html` called via URL `/apps/contact/admin/`:

```html
<script>
    var dataTable;

    window.domReady.add(function () {

        WJ.breadcrumb({
            id: "contact",
            tabs: [
                {
                    url: '/apps/contact/admin/',
                    title: '[[#{components.contact.title}]]',
                    active: true
                }
            ]
        })

        //URL adresa REST rozhrania
        let url = "/admin/rest/apps/contact";
        //package a meno triedy s JPA entitou
        let columns = [(${layout.getDataTableColumns("sk.iway.basecms.contact.ContactEntity")})];

        dataTable = WJ.DataTable({
            url: url,
            serverSide: true,
            columns: columns,
            //id musi byt zhodne s tagom table nizsie
            id: "dataTable",
            fetchOnEdit: true,
            fetchOnCreate: true
        });
    });

</script>

<table id="dataTable" class="datatableInit table"></table>
```

Using the function [WJ.breadcrumb](../../developer/frameworks/webjetjs.md#navigation-bar) a navigation bar is generated.

![](breadcrumb.png)

Feature [WJ.DataTable](../../developer/datatables/README.md#configuration-options) initializes the datatable in the page to an HTML table with `id=dataTable`. Please note the call `window.domReady.add` to be used instead of `$(document).ready` - call waiting for initialization [translation keys](../../developer/libraries/translator.md) and only after they are obtained, the specified function is called.

![](datatable.png)

In order to display the menu item and display the user rights, you still need to create a configuration file `modinfo.properties`, place this in the `/apps/MENO_APLIKACIE/`, for our case `src/main/webapp/apps/contact/modinfo.properties`:

```sh
#prekladovy kluc menu polozky
leftMenuNameKey=components.contact.title
#pouzivatelske pravo
itemKey=cmp_contact
#ak je true, je mozne pravo nastavovat
userItem=true
#linka v menu
leftMenuLink=/apps/contact/admin/
#ikona v menu (https://fontawesome.com/v5/search?s=solid%2Cbrands)
icon=address-book
#ak je true bude po pridani aplikacie zakazana pre vsetkych pouzivatelov a musi sa implicitne povolit
defaultDisabled=true
#ak je true, bude v zozname aplikacii na zaciatku (je to zakaznicka aplikacia)
custom=true

#submenu
#leftSubmenu1NameKey=components.contact.subpage.title
#leftSubmenu1Link=/apps/contact/admin/subpage/
```

If you have a site like `master-detail` which is not defined in `modinfo.properties` it may not display the left menu correctly because it doesn't know what item to highlight. The simplest solution is to name such a page `meno-details.html` (such as a URL `/apps/stat/admin/top-details/?docId=35267&dateRange=` in the file `/apps/stat/admin/top-details.html`). The term `-details` or `-detail` is removed when searching for a menu item to highlight, so that the main (`master`) Page.

If such use is not appropriate you can use the JavaScript function in your page `WJ.selectMenuItem(href)` to highlight the specified menu item. In the parameter `href` enter directly the URL of the page in the menu you want to highlight:

```JavaScript
window.domReady.add(function () {
    WJ.selectMenuItem("/admin/v9/apps/gallery/");
});
```

In the case of an old JSP component, call the function using `setTimeout` to be executed after the page is displayed.

### Attaching a JavaScript file

If you need to attach JavaScript to your application, the WebJET module automatically searches for the file `/apps/{app}/admin/{app}.js` and, if it exists, inserts it into the HTML code header as a module, importing it as an object `appModule`:

```html
<script type="module">
        import * as appModule from "[[${appIncludePathJs}]]";
        window.appModule = appModule;
</script>
```

For example, in a stat application there would be a file `/apps/stat/admin/stat.js`:

```javascript
export const ChartType = {
    Bar: "bar"
    ...
}

export class ChartForm {
    constructor(chartType, yAxeName, xAxeName, chartTitle) {
        this.chartType = chartType;
        ...
    }
}

export async function setAmchart(chartForm) {
    ...
}
```

which is then called in JavaScript/HTML code as `appModule.ChartType.Bar` etc.

## Automated test

For the date table is ready [basic automated test](../../developer/testing/datatable.md), which you just need to configure. Create a new test scenario in `src/test/webapp/tests/apps/contact.js` with at least a basic test:

```javascript
Feature('contact');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/apps/contact/admin/");
});

Scenario('contact-zakladne testy', async ({ I, DataTables, DTE }) => {
    await DataTables.baseTest({
         dataTable: 'dataTable',
         //meno prava na kontrolu podla modinfo.properties
         perms: 'cmp_contact',
         createSteps: function(I, options) {
            //toto pole musime vyplnit rucne, kedze ma specialnu validaciu
            DTE.fillField("zip", "85106");
         }
    });
});
```
