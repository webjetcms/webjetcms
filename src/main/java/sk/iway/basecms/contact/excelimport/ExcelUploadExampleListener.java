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
