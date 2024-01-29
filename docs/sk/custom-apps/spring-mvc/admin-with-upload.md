# Spring MVC - admin s nahratím súboru

Ukážková Spring MVC aplikácia v administrácii s nahratím súboru na backend.

![](admin-upload.png)

## Backend

V administrácii sa zobrazenie stránky realizuje pomocou [AdminThymeleafController](../../developer/frameworks/README.md). Formulár/dáta pre MVC aplikáciu sú do modelu vložené pomocou počúvania [udalosti pre zobrazenie stránky v administrácii](../../developer/frameworks/thymeleaf.md#vloženie-vlastných-objektov-do-modelu).

Spracovanie nahratého súboru je komplikovanejšie, ak potrebujete využiť aj súbor odporúčame rozšíriť triedu [AbstractUploadListener](../../developer/javadoc/sk/iway/iwcm/admin/AbstractUploadListener.html), ktorá zjednodušuje spracovanie nahratého súboru.

Trieda ```AbstractUploadListener``` zabezpečí spracovanie ```multipart requestu``` v konštruktore volaním ```super.processForm(event);``` a nastavenie atribútov zadaného beanu (formuláru). Volanie ```getForm()``` získa objekt formuláru a volanie ```getBindingResult()``` získa prípadné chybové správy - overenie povinných polí atď.

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

Trieda ```Form``` je len jednoduchý bean, nahrávaný súbor je typu ```MultipartFile```. Pre ukážku obsahuje polia ```p1``` a ```p2``` aj s ukážkou validácie povinného poľa a **ukážky validácie dĺžky 10 až 20 znakov**.

Pre validáciu sú podporované štandardné anotácie pre validácie polí pomocou [javax.validation.Validator](https://www.baeldung.com/javax-validation).

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

Použité sú nasledovné prekladové kľúče:

```
form.p1.not_blank=Pole P1 nemôže byť prázdne
form.p2.size=Pole P2 musí byť medzi {min} a {max} znakmi
form.document.not_null=Dokument nemôže byť prázdny
```

## Frontend

HTML kód stránky obsahuje štandardný Spring MVC formulár.

Ukážka obsahuje zobrazenie mena nahratého súboru, ktorý sa získa z atribútu ```importedFileName```, ktorý sa nastaví na backende po úspešnom nahratí súboru.

Chybové správy sú získané z objektu ```fields.errors``` a ak existujú sú zobrazené vo formulári.

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

## Poznámky k implementácii

Konfigurácia pre nahrávanie súboru je nastavená v ```V9SpringConfig.multipartResolver()``` a obslúžená cez ```ThymeleafAdminController.appHandlerPost()```, ktorý spracováva ```consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }```.