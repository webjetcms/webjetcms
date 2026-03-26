# Spring MVC - import Excel souboru

Často je třeba importovat soubor ve formátu Excel (formát `xlsx`), tento příklad rozšiřuje [ukázku nahrání souboru](admin-with-upload.md) o import údajů z Excel souboru.

[Ukázkový soubor](contact.xlsx) pro import.

# Backend

Třída, která poslouchá událost nahrání souboru je podobná [ukázce nahrání souboru](admin-with-upload.md), liší se jen následným voláním služby pro import z Excel souboru `springImportService.importFile(form.getDocument());`.

Ukázka obsahuje i příklad přenosu atributů při použití přesměrování `model.addAttribute("redirect", redirect);`.

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

Import probíhá automaticky mapováním sloupce v Excel souboru na jméno atributu `ContactEntity`. První řádek v Excel souboru tedy obsahuje hodnoty:

`id name zip street city phone contact`

Třída pro zpracování importu rozšiřuje [AbstractExcelImportService](../../../javadoc/sk/iway/iwcm/admin/xls/AbstractExcelImportService.html), který zajišťuje samotný import.

V případě potřeby je možné přepsat některé metody, ukázka v metodě `beforeRow` generuje náhodné telefonní číslo a upravuje formát buňky PSČ, která i když je v Excel souboru označena jako Text importuje se někdy jako číslo.

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

Frontend soubor je téměř shodný s [ukázkou nahrání souboru](admin-with-upload.md), rozdíl je pouze v zobrazeném hlášení úspěšného importu `${success}` a URL adrese pro odeslání formuláře `@{/apps/contact/admin/excelimport/}`.

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

## Poznámky k implementaci

Pro konverzi hodnoty z Excelu do Entity se používá `ConversionService` nakonfigurováno v `V9SpringConfig.conversionService()`. Cílem bylo použít stejný konverter jako se používá mezi řetězci z odeslání formuláře ve standardním Springu. Zajišťuje konverzi datových typů řetězec, číslo, binární hodnota atp.
