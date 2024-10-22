# Spring MVC - administrace s nahráváním souborů

Ukázka aplikace Spring MVC v administraci s nahráváním souborů do backendu.

![](admin-upload.png)

## Backend

V administraci je zobrazení stránky realizováno pomocí [AdminThymeleafController](../../developer/frameworks/README.md). Formulář/data pro aplikaci MVC se do modelu vkládají pomocí poslouchání [události pro zobrazení stránky v administraci](../../developer/frameworks/thymeleaf.md#vkládání-vlastních-objektů-do-modelu).

Zpracování nahraného souboru je složitější, pokud potřebujete soubor použít, doporučujeme rozšířit třídu [AbstractUploadListener](../../../javadoc/sk/iway/iwcm/admin/AbstractUploadListener.html) což zjednodušuje zpracování nahraného souboru.

Třída `AbstractUploadListener` zajistit zpracování `multipart requestu` v konstruktoru voláním `super.processForm(event);` a nastavení atributů zadané fazole (formuláře). Volání `getForm()` získá objekt formuláře a volání `getBindingResult()` získat chybová hlášení - ověření povinných polí atd.

```java
package sk.iway.basecms.contact.upload;

import javax.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import sk.iway.iwcm.admin.AbstractUploadListener;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class UploadExampleListener extends AbstractUploadListener<Form> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadExampleListener.class);

    protected UploadExampleListener(Validator validator) {
        super(validator);
    }

    @Override
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='contact' && event.source.subpage=='upload'")
    public void processForm(final WebjetEvent<ThymeleafEvent> event) {
        // sprocesovanie dat formularu do objektu a validacia tohto objektu
        super.processForm(event);
        ModelMap model = event.getSource().getModel();
        // ziskanie objektu a vlozenie do modelu
        Form form = getForm();
        model.addAttribute("form", form);

        LOGGER.debug("Is post: {}", isPost());
        if (!isPost()) {
            // ak nie je request typu post, tak dalej nic nerobime
            return;
        }

        // ziskanie validacnych errorov
        BindingResult errors = getBindingResult();
        if (errors.hasErrors()) {
            LOGGER.debug("We have errors: {}", errors.getErrorCount());
            return;
        }

        //
        //...spravte s formularom co potrebujete
        //
        model.addAttribute("importedFileName", form.getDocument().getOriginalFilename());
    }
}
```

Třída `Form` je jen jednoduchá fazole, zaznamenaný soubor je typu `MultipartFile`. Obsahuje například pole `p1` a `p2` s ukázkou validace povinného pole a **validační vzorky o délce 10 až 20 znaků**.

Pro ověřování jsou podporovány standardní anotace pro ověřování polí pomocí [javax.validation.Validator](https://www.baeldung.com/javax-validation).

```java
package sk.iway.basecms.contact.upload;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Component
@Getter
@Setter
public class Form {
    @NotNull(message = "form.document.not_null")
    private MultipartFile document;

    @NotBlank(message = "form.p1.not_blank")
    private String p1;

    @Size(min = 10, max = 20, message = "form.p2.size")
    private String p2;
}
```

Používají se následující překladové klíče:

```
form.p1.not_blank=Pole P1 nemôže byť prázdne
form.p2.size=Pole P2 musí byť medzi {min} a {max} znakmi
form.document.not_null=Dokument nemôže byť prázdny
```

## Frontend

Kód HTML stránky obsahuje standardní `Spring MVC` Forma.

Ukázka obsahuje zobrazení názvu nahraného souboru, který je získán z atributu `importedFileName`, která se nastaví na backendu po úspěšném nahrání souboru.

Chybová hlášení se načítají z objektu `fields.errors` a zda jsou ve formuláři zobrazeny.

```html
<script type="text/javascript">
window.domReady.add(function () {
    WJ.breadcrumb({
        id: "upload",
        tabs: [
            {
                url: '/apps/contact/admin/upload/',
                title: 'Spring MVC upload example',
                active: true
            }
        ]
    });
});
</script>
<section class="my-3">
    <div class="container">
        <div class="row">
            <div class="col-md-8 mx-auto">

                <p data-th-if="${importedFileName ne null}" class="alert alert-success">
                    File successfully uploaded, fileName: <span data-th-text="${importedFileName}"></span>
                </p>

                <form method="post" data-th-action="@{/apps/contact/admin/upload/}" data-th-object="${form}" enctype="multipart/form-data">
                    <div data-th-if="${#fields.hasErrors('*')}" class="alert alert-danger">
                        <ul style="margin-bottom: 0;">
                            <li data-th-each="error : ${#fields.errors('*')}" data-th-text="${error}">error</li>
                        </ul>
                    </div>

                    <div class="form-group mb-3">
                        <label for="p1" class="form-label">P1</label>
                        <input type="text" data-th-field="*{p1}" id="p1" class="form-control">
                    </div>
                    <div class="form-group mb-3">
                        <label for="p2" class="form-label">P2</label>
                        <input type="text" data-th-field="*{p2}" id="p2" class="form-control">
                    </div>
                    <div class="form-group mb-3">
                        <label for="document" class="form-label">Dokument</label>
                        <input type="file" data-th-field="*{document}" id="document" class="form-control">
                    </div>
                    <div class="form-group mb-3 text-end">
                        <button type="submit" class="btn btn-primary">[[#{button.submit}]]</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</section>
```

## Poznámky k implementaci

Konfigurace pro nahrávání souboru je nastavena v položce `V9SpringConfig.multipartResolver()` a podává se prostřednictvím `ThymeleafAdminController.appHandlerPost()`, který zpracovává `consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }`.
