# Spring MVC - import Excel súboru

Často je potrebné importovať súbor vo formáte Excel (formát ```xlsx```), tento príklad rozširuje [ukážku nahratia súboru](admin-with-upload.md) o import údajov z Excel súboru.

[Ukážkový súbor](contact.xlsx) pre import.

# Backend

Trieda, ktorá počúva udalosť nahratia súboru je podobná [ukážke nahratia súboru](admin-with-upload.md), líši sa len následným volaním služby pre import z Excel súboru ```springImportService.importFile(form.getDocument());```.

Ukážka obsahuje aj príklad prenosu atribútov pri použití presmerovania ```model.addAttribute("redirect", redirect);```.

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

import javax.validation.Validator;
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

Import prebieha automaticky mapovaním stĺpca v Excel súbore na meno atribútu ```ContactEntity```. Prvý riadok v Excel súbore teda obsahuje hodnoty:

```id name zip street city phone contact```

Trieda pre spracovanie importu rozširuje [AbstractExcelImportService](../../../javadoc/sk/iway/iwcm/admin/xls/AbstractExcelImportService.html), ktorý zabezpečuje samotný import.

V prípade potreby je možné prepísať niektoré metódy, ukážka v metóde ```beforeRow``` generuje náhodné telefónne číslo a upravuje formát bunky PSČ, ktorá aj keď je v Excel súbore označená ako Text importuje sa niekedy ako číslo.

```java
package sk.iway.basecms.contact.excelimport;

import org.apache.commons.lang.RandomStringUtils;
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
 * Ukazkova service trieda pre import z Excel suboru do Spring DATA repozitara
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

Frontend súbor je temer zhodný s [ukážkou nahratia súboru](admin-with-upload.md), rozdiel je len v zobrazenom hlásení úspešného importu ```${success}``` a URL adrese pre odoslanie formuláru ```@{/apps/contact/admin/excelimport/}```.

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

## Poznámky k implementácii

Pre konverziu hodnoty z Excelu do Entity sa používa ```ConversionService``` nakonfigurované v ```V9SpringConfig.conversionService()```. Cieľom bolo použiť rovnaký konverter ako sa používa medzi reťazcami z odoslania formuláru v štandardnom Springu. Zabezpečuje konverziu dátových typov reťazec, číslo, binárna hodnota atď.