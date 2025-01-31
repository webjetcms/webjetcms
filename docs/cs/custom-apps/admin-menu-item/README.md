# Položka menu v sekci správce

Správa je založena na [datové tabulky](../../developer/datatables/README.md) a [Rozhraní REST](../../developer/datatables/restcontroller.md). Základem je pochopení jejich fungování podle dokumentace pro [Programátor systému WebJET CMS](../../developer/README.md).

V této ukázce naprogramujeme správu kontaktů, tabulka se používá. `contact` který existuje ve standardní instalaci WebJET CMS (ale obvykle se nepoužívá).

![](contact.png)

## Backend

Ideální řešení využívá `Spring DATA` úložiště, řadič REST a datovou tabulku vygenerovanou z `@DataTableColumn`, [Anotace](../../developer/datatables-editor/datatable-columns.md).

Ukázka entity JPA s anotacemi `@DataTableColumn`, [pro datovou tabulku a editor](../../developer/datatables-editor/datatable-columns.md). Všimněte si také poznámky `@EntityListeners` pro automatické [logovaní auditu](../../developer/backend/auditing.md) při změně entity.

Povinná pole a další validace [nastavíte anotace](../../developer/datatables/restcontroller.md#validace---povinná-pole), `@NotBlank,@Size,@Email` atd.

!>**Varování:** nepoužívat primitivní typy v entitě `int, long` ale pouze objekt `Integer, Long` jinak nebude filtrování/vyhledávání fungovat správně.

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
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID", renderFormat = "dt-format-selector")
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

Vzorové úložiště JPA, při jeho použití vždy použijte také `Pageable` objekt pro stránkování a rozvržení. Podpora dynamického generování vyhledávacích kritérií (v metodě řadiče REST `addSpecSearch`) úložiště také rozšiřuje `JpaSpecificationExecutor`.

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

!>**Varování:** si všimněte použití `JpaSpecificationExecutor`. Umožňuje dynamické vytváření příkazu SQL pro vyhledávání/filtraci/uspořádání záznamů v datové tabulce. Pokud byste jej nepoužili, vyhledává ve tvaru [Dotazování podle příkladu](https://www.baeldung.com/spring-data-query-by-example) když **Nesmíte** používat primitivní typy a inicializační hodnoty v entitě (např. `String text="test"`). Při vyhledávání by pak hodnota byla `test` automaticky vyhledávány, i když nejsou zadány. To řeší `JpaSpecificationExecutor`, který hledá pouze parametry zadané ve filtru (testováním parametrů požadavku začínajících na znaky `search`).

Ukázka kontroléru REST, vždy věnujte pozornost kontrole práv pomocí anotace `@PreAuthorize` a nezapomeňte na anotaci `@Datatable` pro správné generování chybových odpovědí.

V metodě `getOptions` nastaví možnosti pole pro výběr země.

V metodě `beforeSave` je možné nastavit další (needitovatelné) údaje před uložením entity do databáze.

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

Jak bylo uvedeno výše, pro frontend se používají datové tabulky. Administrace je zkompilována ze souborů pug do html a poté zobrazena.

Abychom vám usnadnili vytvoření nové stránky v administraci, zavedli jsme předpřipravenou šablonu rozvržení stránky. Přidáte pouze prostřední část stránky (šablona zajistí vygenerování záhlaví a nabídky). [Mapování adres URL](../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) se připravuje jako `/apps/{app}/admin/` nebo `/apps/{app}/admin/{subpage}`.

Stačí připravit soubor `/apps/{app}/admin/index.html` nebo `/apps/{app}/admin/{subpage}.html` Kde: `{app}` je název adresáře, ve kterém je aplikace umístěna (bez diakritiky a mezer), a `{subpage}` je název souboru bez přípony (pokud to není název index.html).

Příkladem je aplikace `src/main/webapp/apps/contact/admin/index.html` voláno prostřednictvím adresy URL `/apps/contact/admin/`:

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

Použití funkce [WJ.breadcrumb](../../developer/frameworks/webjetjs.md#navigační-panel) je vygenerován navigační panel.

![](breadcrumb.png)

Funkce [WJ.DataTable](../../developer/datatables/README.md#možnosti-konfigurace) inicializuje datovou tabulku ve stránce na tabulku HTML s kódem `id=dataTable`. Vezměte prosím na vědomí výzvu `window.domReady.add` použít místo `$(document).ready` - volání čekající na inicializaci [překladové klíče](../../developer/libraries/translator.md) a teprve po jejich získání se zavolá zadaná funkce.

![](datatable.png)

Pro zobrazení položky nabídky a zobrazení uživatelských práv je třeba ještě vytvořit konfigurační soubor. `modinfo.properties`, umístěte ji do `/apps/MENO_APLIKACIE/`, pro náš případ `src/main/webapp/apps/contact/modinfo.properties`:

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

Pokud máte web, jako je `master-detail` který není definován v `modinfo.properties` nemusí se správně zobrazit levé menu, protože neví, jakou položku má zvýraznit. Nejjednodušším řešením je pojmenovat takovou stránku `meno-details.html` (jako je adresa URL `/apps/stat/admin/top-details/?docId=35267&dateRange=` v souboru `/apps/stat/admin/top-details.html`). Termín `-details` nebo `-detail` je při hledání položky menu, kterou chcete zvýraznit, odstraněn, takže hlavní (`master`) Strana.

Pokud takové použití není vhodné, můžete na stránce použít funkci JavaScript. `WJ.selectMenuItem(href)` zvýraznit zadanou položku nabídky. V parametru `href` zadejte přímo adresu URL stránky v nabídce, kterou chcete zvýraznit:

```JavaScript
window.domReady.add(function () {
    WJ.selectMenuItem("/admin/v9/apps/gallery/");
});
```

V případě staré komponenty JSP zavolejte funkci pomocí příkazu `setTimeout` které se provedou po zobrazení stránky.

### Připojení souboru JavaScript

Pokud potřebujete k aplikaci připojit JavaScript, modul WebJET automaticky vyhledá soubor. `/apps/{app}/admin/{app}.js` a pokud existuje, vloží jej do záhlaví kódu HTML jako modul a importuje jej jako objekt. `appModule`:

```html
<script type="module">
        import * as appModule from "[[${appIncludePathJs}]]";
        window.appModule = appModule;
</script>
```

Například v aplikaci stat by byl soubor `/apps/stat/admin/stat.js`:

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

který je pak v kódu JavaScript/HTML volán jako `appModule.ChartType.Bar` atd.

## Automatizovaný test

Tabulka s daty je připravena [základní automatizovaný test](../../developer/testing/datatable.md), který stačí nakonfigurovat. Vytvořte nový testovací scénář v `src/test/webapp/tests/apps/contact.js` s alespoň základním testem:

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
