# Spring MVC - import Excel file

It is often necessary to import a file in Excel format (format ```xlsx```), this example extends [file upload example](admin-with-upload.md) to import data from an Excel file.

[Sample file](contact.xlsx) for import.

# Backend

The class that listens to the file upload event is similar to [file upload example](admin-with-upload.md), it only differs in the subsequent call to the service for importing from an Excel file ```springImportService.importFile(form.getDocument());```.

The sample also includes an example of attribute transfer when using the ```model.addAttribute("redirect", redirect);``` redirect.

```java
package sk.iway.basecms.contact.excelimport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import sk.iway.basecms.contact.upload.Form;
import sk.iway.iwcm.admin.AbstractUploadListener;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.admin.xls.ExcelImportServiceInterface;
import sk.iway.iwcm.admin.xls.exception.ExcelImportServiceException;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

import jakarta.validation.Validator;
import java.util.List;

/**
 * Ukazkovy Thymeleaf listener pre spracovanie uploadu suboru
 * a nasledny import z Excel formatu
 */
@Component
public class ExcelUploadExampleListener extends AbstractUploadListener<Form> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelUploadExampleListener.class);
    private final ExcelImportServiceInterface springImportService;

    protected ExcelUploadExampleListener(ExcelImportServiceInterface springImportService, Validator validator) {
        super(validator);
        this.springImportService = springImportService;
    }

    @Override
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='contact' && event.source.subpage=='excelimport'")
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

        try {
            // importovanie dat zo suboru do DB tabulky
            springImportService.importFile(form.getDocument());

            // presmerovanie po uspesnom importe
            String redirect = "/apps/contact/admin/excelimport/";
            model.addAttribute("redirect", redirect);
            LOGGER.debug("Redirecting to: {}", redirect);

            RedirectAttributes redirectAttributes = event.getSource().getRedirectAttributes();
            redirectAttributes.addFlashAttribute("success", Prop.getInstance().getText("form.flash.success"));
        } catch (ExcelImportServiceException e) {
            // zobrazenie chybovych hlaskok v pripade problemu s importom
            List<String> errs = e.getErrors();
            for (String err : errs) {
                errors.rejectValue("", "", err);
            }
        }
    }
}
```

The import is done automatically by mapping a column in the Excel file to the attribute name ```ContactEntity```. The first row in the Excel file therefore contains the values:

```id name zip street city phone contact```

Trieda pre spracovanie importu rozširuje [AbstractExcelImportService](../../../javadoc/sk/iway/iwcm/admin/xls/AbstractExcelImportService.html), ktorý zabezpečuje samotný import.

V prípade potreby je možné prepísať niektoré metódy, ukážka v metóde ```beforeRow``` generuje náhodné telefónne číslo a upravuje formát bunky PSČ, ktorá aj keď je v Excel súbore označená ako Text importuje sa niekedy ako číslo.

```java
package sk.iway.basecms.contact.excelimport;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import sk.iway.basecms.contact.ContactEntity;
import sk.iway.iwcm.admin.xls.AbstractExcelImportService;

/**

- Ukazkova service trieda pre import z Excel suboru do Spring DATA repozitara
 */
@Service
@RequestScope
public class ExcelImportService extends AbstractExcelImportService<ContactEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelImportService.class);

    public ExcelImportService(CrudRepository<ContactEntity, Long> repository, ConversionService conversionService) {
        super(repository, conversionService);
    }

    /**
     * Metoda sa vola pred konvertovanim udajov do beanu, umoznuje modifikovat data
     */
    @Override
    public void beforeRow(Row row) {
        LOGGER.debug("beforeRow cell: {}", row.getRowNum());

        int i = 0;
        for (Cell cell : row) {
            String columnName = getFirstRowNames().get(i);

            //vygeneruj nahodne telefonne cislo pre prvy riadok
            if (row.getRowNum() == 1 && "phone".equals(columnName)) {
                cell.setCellValue("+421 90" + RandomStringUtils.random(1, false, true) + " " + RandomStringUtils.random(3, false, true) + " " + RandomStringUtils.random(3, false, true));
            }

            //uprav format PSC, Excel sa nam to snazi dat ako cislo
            if ("zip".equals(columnName) && cell.getCellType().equals(CellType.NUMERIC)) {
                //ponechaj len celociselnu (int) cast cisla, lebo PSC 123456 importuje ako 123456.0
                cell.setCellValue(String.valueOf((int)cell.getNumericCellValue()));
            }

            i++;
        }
    }

    /**
     * Metoda sa vola po konvertovani riadku do beanu, pred jeho ulozenim do databazy
     */
    @Override
    public void afterRow(ContactEntity bean) {
        LOGGER.debug("afterRow: {}", bean.getName());
    }
}
```

# Frontend

The frontend file is almost identical to [file upload example](admin-with-upload.md), the only difference is in the displayed message of successful import ```${success}``` and the URL address for submitting the form ```@{/apps/contact/admin/excelimport/}```.

```html
<script type="text/javascript">
    window.domReady.add(function () {
        WJ.breadcrumb({
            id: "upload",
            tabs: [
                {
                    url: '/apps/contact/admin/',
                    title: '[[#{components.contact.title}]]',
                    active: false
                },
                {
                    url: '/apps/contact/admin/upload/',
                    title: 'Spring MVC Excel import example',
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

                <p data-th-text="${success}" data-th-if="${success ne null}" class="alert alert-success"></p>

                <form method="post" data-th-action="@{/apps/contact/admin/excelimport/}" data-th-object="${form}" enctype="multipart/form-data">
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

To convert a value from Excel to Entity, ```ConversionService``` configured in ```V9SpringConfig.conversionService()``` is used. The goal was to use the same converter as is used between strings from form submissions in standard Spring. It provides conversion of data types string, number, binary value, etc.