# Spring MVC - admin s nahráním souboru

Ukázková Spring MVC aplikace v administraci s nahráním souboru na backend.

![](admin-upload.png)

## Backend

V administraci se zobrazení stránky realizuje pomocí [AdminThymeleafController](../../developer/frameworks/README.md). Formulář/data pro MVC aplikaci jsou do modelu vloženy pomocí poslechu [události pro zobrazení stránky v administraci](../../developer/frameworks/thymeleaf.md#vložení-vlastních-objektů-do-modelu).

Zpracování nahraného souboru je komplikovanější, pokud potřebujete využít i soubor doporučujeme rozšířit třídu [AbstractUploadListener](../../../javadoc/sk/iway/iwcm/admin/AbstractUploadListener.html), která zjednodušuje zpracování nahraného souboru.

Třída `AbstractUploadListener` zajistí zpracování `multipart requestu` v konstruktoru voláním `super.processForm(event);` a nastavení atributů zadaného beanu (formuláře). Volání `getForm()` získá objekt formuláře a volání `getBindingResult()` získá případné chybové zprávy - ověření povinných polí atp.

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

Třída `Form` je jen jednoduchý bean, nahrávaný soubor je typu `MultipartFile`. Pro ukázku obsahuje pole `p1` a `p2` is ukázkou validace povinného pole a **ukázky validace délky 10 až 20 znaků**.

Pro validaci jsou podporovány standardní anotace pro validace polí pomocí [javax.validation.Validator](https://www.baeldung.com/javax-validation).

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

Použity jsou následující překladové klíče:

```
form.p1.not_blank=Pole P1 nemôže byť prázdne
form.p2.size=Pole P2 musí byť medzi {min} a {max} znakmi
form.document.not_null=Dokument nemôže byť prázdny
```

## Frontend

HTML kód stránky obsahuje standardní `Spring MVC` formulář.

Ukázka obsahuje zobrazení jména nahraného souboru, který se získá z atributu `importedFileName`, který se nastaví na backendu po úspěšném nahrání souboru.

Chybové zprávy jsou získány z objektu `fields.errors` a pokud existují jsou zobrazeny ve formuláři.

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

Konfigurace pro nahrávání souboru je nastavena v `V9SpringConfig.multipartResolver()` a obsloužená přes `ThymeleafAdminController.appHandlerPost()`, který zpracovává `consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }`.
