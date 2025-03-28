# Spring MVC aplikace

Ukázková aplikace ve veřejné webové stránce s použitím Spring MVC a Thymeleaf šablon.

![](list.png)

## Backend

Vytvořená třída musí rozšiřovat třídu `WebjetComponentAbstract` a zároveň obsahovat anotaci `@WebjetComponent`. To zajistí nastavení Spring `@Component` anotace a provedení aplikace přes `!INCLUDE()!` příkaz ve web stránce.

Jednotlivé metody `view, edit, add, saveForm` jsou volány na základě shody URL parametru, například. metoda `edit` se zavolá přidáním URL parametru `?edit=true`. Pomocí anotace `@DefaultHandler` se určí metoda, která se provede, pokud není nalezena jiná shoda URL parametru. Jméno metody může být libovolné, nemusí být s uvedeného seznamu, stačí existence URL parametru se stejným jménem.

Příklad:

```java
package sk.iway.basecms.contact;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * <p>Príkladová trieda pre komponentu - http://docs.webjetcms.sk/v2022/#/custom-apps/spring-mvc/</p>
 * <p>Trieda musí byt anotovaná @WebjetComponent, pre názov v anotácii sa musí použiť celý názov triedy aj s package</p>
 * <p>Príklad include:</p>
 * <code>!INCLUDE(sk.iway.basecms.contact.ContactApp, country="sk")!</code>
 *
 * Anotacia @WebjetAppStore zabezpeci zobrazenie aplikacie v zozname aplikacii v editore (v AppStore)
 *
 * V pripade, ze nejaka metoda ma byt dostupna len pre prihlaseneho pouzivatela, admina, prip. nejaku pouzivatelsku skupinu mozeme pouzit anotacie:
 * @PreAuthorize("@WebjetSecurityService.isLogged()") - prihalseny pouzivatel
 * @PreAuthorize("@WebjetSecurityService.isAdmin()") - admin
 * @PreAuthorize("@WebjetSecurityService.isInUserGroup('nazov-skupiny')") - patri do skupiny
 * @see sk.iway.iwcm.system.spring.services.WebjetSecurityService
 */
@WebjetComponent("sk.iway.basecms.contact.ContactApp")
@Getter
@Setter
public class ContactApp extends WebjetComponentAbstract {

    //Spring DATA repozitar
    @JsonIgnore
    private ContactRepository contactRepository;

    /**
     * Privatne vlastnosti s get/set-rami slúžia na prenesenie parametrov pageParams z !INCLUDE()! do triedy
     */
    private String country;

    @Autowired
    public ContactApp(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    /**
     * metoda init sa vola po vytvoreni objektu a nastaveni parametrov,
     * je volana pred kazdym view volanim a umozni nastavit pripadne atributy
     * @param request
     * @param response
     */
    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        Logger.debug(ContactApp.class, "Init call, request.getHeader(User-Agent)="+request.getHeader("User-Agent"));
    }

    /**
     * Metóda anotovaná @DefaultHandler sa vykoná, ak v requeste nie je žiaden parameter, ktorý by sa zhodoval s názvom inej metódy v triede
     * Metóda môže mať ľubovolný názov
     * @param model
     * @param request
     * @return String URL adresa súboru ktorý bude v contente renderovaný namiesto !INCLUDE()!
     */
    @DefaultHandler
	public String view(Model model, HttpServletRequest request)
	{
        model.addAttribute("contants", contactRepository.findAllByCountry(country, null));
		return "/apps/contact/mvc/list";
	}

    /**
     * Metóda edit slúži na zobrazenie formuláru pre úpravu existujúceho záznamu
     * @param id
     * @param model
     * @param request
     * @return
     */
    public String edit(@RequestParam("id") long id, Model model, HttpServletRequest request) {
        ContactEntity contact = contactRepository.getById(id);
        model.addAttribute("entity", contact);

        //pridaj do modelu moznosti pre select country
        model.addAttribute("countries", ContactRestController.getCountries());

        return "/apps/contact/mvc/edit";
    }

    /**
     * Metóda edit slúži na zobrazenie formuláru pre úpravu existujúceho záznamu
     * @param model
     * @param request
     * @return
     */
    public String add(Model model) {
        ContactEntity contact = new ContactEntity();
        //tu mozete nastavit nejake default hodnoty
        contact.setCountry("sk");

        model.addAttribute("entity", contact);
        return "/apps/contact/mvc/edit";
    }

    /**
     * Metóda saveForm slúži na validáciu a uloženie úpravy existujúceho záznamu
     * @param entity
     * @param result
     * @param model
     * @param request
     * @return
     */
    public String saveForm(@Valid @ModelAttribute("entity") ContactEntity entity, BindingResult result, Model model, HttpServletRequest request) {
        if (!result.hasErrors()) {
            contactRepository.save(entity);
            return "redirect:" + PathFilter.getOrigPath(request);
        }
        model.addAttribute("error", result);
        model.addAttribute("entity", entity);
        return "/apps/contact/mvc/edit";
    }

}
```

### Validace

Pomocí anotace `@Valid` a `@ModelAttribute` je možné provést validaci atributů entity. Výsledek se uloží do objektu `BindingResult result`:

```java
public String saveForm(@Valid @ModelAttribute("entity") ContactEntity entity, BindingResult result, Model model, HttpServletRequest request) {
    if (!result.hasErrors()) {
        contactRepository.save(entity);
        return "redirect:" + PathFilter.getOrigPath(request);
    }
    model.addAttribute("error", result);
    model.addAttribute("entity", entity);
    return "/apps/contact/mvc/edit";
}
```

### Vložení aplikace do web stránky

Aplikace se do veřejné web stránky vkládá stejně jako jiná standardní aplikace pomocí značky `!INCLUDE()!`. Zadané jméno se musí shodovat s anotací `@WebjetComponent` ve třídě. Jméno musí být unikátní, proto je třeba nastavit název třídy s celou cestou. `@WebjetComponent("sk.iway.basecms.contact.ContactApp")`.

```html
!INCLUDE(sk.iway.basecms.contact.ContactApp, country="sk")!
```

### Používání parametrů aplikace

Každá aplikace může v `!INCLUDE()!` značce obsahovat parametry, v našem příkladu se jedná o parametr `country`. Ten se automaticky přenese do atributu `country` ve třídě `ContactApp`.

```html
!INCLUDE(sk.iway.basecms.contact.ContactApp, country="sk")!
```

```java
/**
 * Privatne vlastnosti s get/set-rami slúžia na prenesenie parametrov pageParams z !INCLUDE()! do triedy
 */
private String country;
```

Podporovány jsou následující datové typy:

```java
String, BigDecimal, Boolean, Integer, Double, Float, boolean, int, double, float
```

Zobrazení aplikace v seznamu aplikací a nastavení jejích parametrů je v kapitole [Zobrazení v seznamu aplikací](../appstore/README.md)

## Frontend

Pro zobrazení lze použít následující typy souborů:
- `JSP` - soubor má koncovku `.jsp`
- `Freemarker` - soubor má koncovku `.ftl`
- `Thymeleaf` - soubor má koncovku `.html`

Pro nové aplikace doporučujeme striktně používat [Thymeleaf](../../developer/frameworks/thymeleaf.md). Všimněte si, že v `ContactApp` metody vracejí cestu bez přípony (např. `return "/apps/contact/mvc/edit"`), WebJET automaticky hledá soubor podle uvedeného seznamu a použije první, který najde. Můžete tak snadno měnit technologii použitou na frontendu beze změny backendu.

Pro `Thymeleaf` jsou automaticky do modelu vloženy objekty `request a session`, které lze použít jako nap. `${request.getAttribute('ninja').page.urlPath}`.

### Zobrazení seznamu firem

Seznam firem v souboru `src/main/webapp/apps/contact/mvc/list.html` využívá [iteraci seznamu](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#using-theach) firem ve standardní HTML tabulce:

![](list.png)

```html
<h3 data-th-text="#{components.contact.page.list}">Zoznam kontaktov</h3>

<p><a class="btn btn-primary" data-th-href="${'?add=true'}" data-th-text="#{components.contact.value.name}">Create</a></p>

<div class="table-responsive"></div>
    <table class="table table-striped table-hover">
        <tr>
            <th data-th-text="#{components.contact.property.name}">Firma</th>
            <th data-th-text="#{components.contact.property.vatid}">IČ DPH</th>
            <th data-th-text="#{components.contact.property.city}">Mesto</th>
            <th></th>
        </tr>
        <!--/* https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#using-theach */-->
        <tr data-th-each="contact : ${contants}">
            <td data-th-text="${contact.name}">InterWay</td>
            <td data-th-text="${contact.vatid}">SK123456789</td>
            <td data-th-text="${contact.city}">Blava</td>
            <td>
                <a class="btn btn-secondary" data-th-href="${'?edit=true&id='+contact.id}" data-th-text="#{components.checkform.confirm_table.button.edit}">Edit</a>
            </td>
        </tr>
    </table>
</div>
```

### Editace firmy

Soubor `src/main/webapp/apps/contact/mvc/edit.html` pro editaci firmy je použit také pro přidání nové firmy. Využívá standardní možnosti pro [formuláře ve Springu](https://spring.io/guides/gs/handling-form-submission/). Ve vrchní části zobrazuje [výpis chybových zpráv](https://www.baeldung.com/spring-thymeleaf-error-messages) v případě validační chyby (např. nezadané jméno firmy nebo nevhovující formát PSČ).

![](edit.png)

```html
<h3 data-th-text="#{components.contact.dialog_title}">Kontakt</h3>

<!--/* https://spring.io/guides/gs/handling-form-submission/ */-->
<form data-th-action="${request.getAttribute('ninja').page.urlPath}" data-th-object="${entity}" method="post">
    <div data-th-if="${error!=null}" class="alert alert-danger">
        <!--/* https://www.baeldung.com/spring-thymeleaf-error-messages */-->
        <p data-th-text="#{chat.form_fill_error}"></p>
        <ul style="margin: 0px;">
            <li data-th-each="err : ${error.allErrors}">
                <span data-th-text="#{components.contact.property.__${err.field}__}">zložený prekladový kľúč zo statického prefixu a dynamického mena poľa</span> - <span data-th-text="${err.defaultMessage}">chybová správa</span>
            </li>
        </ul>
    </div>

    <div class="mb-3">
      <label class="form-label" data-th-text="#{components.contact.property.name}">Firma</label>
      <input type="text" class="form-control" data-th-field="*{name}">
    </div>
    <div class="mb-3">
        <label class="form-label" data-th-text="#{components.contact.property.vatid}">IČ DPH</label>
        <input type="text" class="form-control" data-th-field="*{vatid}">
    </div>
    <div class="mb-3">
        <label class="form-label" data-th-text="#{components.contact.property.street}"></label>
        <input type="text" class="form-control" data-th-field="*{street}">
    </div>
    <div class="mb-3">
        <label class="form-label" data-th-text="#{components.contact.property.city}"></label>
        <input type="text" class="form-control" data-th-field="*{city}">
    </div>
    <div class="mb-3">
        <label class="form-label" data-th-text="#{components.contact.property.zip}"></label>
        <input type="text" class="form-control" data-th-field="*{zip}">
    </div>
    <div class="mb-3">
        <label class="form-label" data-th-text="#{components.contact.property.country}"></label>
        <!--/* https://www.baeldung.com/thymeleaf-select-option */-->
        <select class="form-control" data-th-field="*{country}">
            <option data-th-each="country : ${countries}" data-th-value="${country.value}" data-th-text="${country.label}"></option>
        </select>
    </div>
    <div class="mb-3">
        <label class="form-label" data-th-text="#{components.contact.property.contact}"></label>
        <input type="text" class="form-control" data-th-field="*{contact}">
    </div>
    <div class="mb-3">
        <label class="form-label" data-th-text="#{components.contact.property.phone}"></label>
        <input type="text" class="form-control" data-th-field="*{phone}">
    </div>
    <button type="submit" class="btn btn-primary" data-th-text="#{button.submit}" name="saveForm">Submit</button>
    <input type="hidden" name="id" data-th-field="*{id}"/>
</form>
```

### Zobrazení validačních chyb

Validační chyby jsou podmíněně zobrazeny v bloku `<div data-th-if="${error!=null}" class="alert alert-danger">`. V ukázce se zobrazují v jednoduchém seznamu, je ale možné použít i zobrazení chyby při [jednotlivých polích](https://www.baeldung.com/spring-thymeleaf-error-messages).

![](validation.png)

### Automatické hledání souboru

Pokud třída rozšiřuje třídu `WebjetComponentAbstract`, zápis vložené aplikace může obsahovat parametr `viewFolder`, díky kterému umíme určit podadresář, kde se mají hledat soubory pro zobrazení.

V případě, že metoda vrátí `/components/contact/edit` a je zadaný parametr `viewFolder`, WebJET automaticky do linky vloží `installName` a hodnotu ve `viewFolder`:

```java
/components/{installName}/contact/{viewFolder}/edit.html

//ak neexistue tak skúsi bez installName:
/components/contact/{viewFolder}/edit.html

//Ak nie je vyplneny viewFolder, automaticky vloží len installName:
/components/{installName}/contact/edit.html

//ak neexistue tak skúsi bez installName:*
/components/contact/{viewFolder}/edit.html
```

Příklad:

```java
!INCLUDE(sk.iway.basecms.contact.ContactApp, viewFolder="subfolder", country="sk")!
```
- v tomto případě se použije třída \`\`cs.iway.basecms.contact.ContactApp\`\`\`,
- zavolá se metoda s anotací `@DefaultHandler`, která vrátí `return "/apps/contact/mvc/list";`,
- WebJET vyhledá `/components/{installName}/contact/subfolder/list.html`, nebo `/components/contact/subfolder/list.html`
- výsledný HTML kód vloží do stránky.
