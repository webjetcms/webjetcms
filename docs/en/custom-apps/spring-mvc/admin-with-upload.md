# Spring MVC - admin with file upload

Sample Spring MVC application in the administration with file upload to the backend.

![](admin-upload.png)

## Backend

In the administration, the page display is implemented using [AdminThymeleafController](../../developer/frameworks/README.md). The form/data for the MVC application is inserted into the model by listening to [page display events in the administration](../../developer/frameworks/thymeleaf.md#inserting-custom-objects-into-the-model).

Processing an uploaded file is more complicated, if you also need to use a file, we recommend extending the [AbstractUploadListener](../../../javadoc/sk/iway/iwcm/admin/AbstractUploadListener.html) class, which simplifies processing an uploaded file.

The ```AbstractUploadListener``` class will handle ```multipart requestu``` in the constructor by calling ```super.processForm(event);``` and setting the attributes of the specified bean (form). Calling ```getForm()``` will get the form object and calling ```getBindingResult()``` will get any error messages - validation of required fields, etc.

```java
package sk.iway.basecms.contact.upload;

import jakarta.validation.Validator;

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

The ```Form``` class is just a simple bean, the uploaded file is of type ```MultipartFile```. For the example, it contains the fields ```p1``` and ```p2``` with a demonstration of mandatory field validation and **demonstrations of 10 to 20 character length validation**.

For validation, standard annotations for field validations are supported using [jakarta.validation.Validator](https://www.baeldung.com/javax-validation).

```java
package sk.iway.basecms.contact.upload;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

The following translation keys are used:

```
form.p1.not_blank=Pole P1 nemôže byť prázdne
form.p2.size=Pole P2 musí byť medzi {min} a {max} znakmi
form.document.not_null=Dokument nemôže byť prázdny
```

## Frontend

The HTML code of the page contains the standard `Spring MVC` form.

The sample includes a display of the name of the uploaded file, which is obtained from the ```importedFileName``` attribute, which is set on the backend after the file is successfully uploaded.

Error messages are retrieved from the ```fields.errors``` object and, if they exist, are displayed in the form.

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

## Implementation notes

The configuration for file upload is set up in ```V9SpringConfig.multipartResolver()``` and served via ```ThymeleafAdminController.appHandlerPost()```, which is processed by ```consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }```.