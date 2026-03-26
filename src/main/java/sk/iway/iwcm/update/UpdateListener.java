package sk.iway.iwcm.update;

import jakarta.validation.Validator;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.AbstractUploadListener;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class UpdateListener extends AbstractUploadListener<UpdateForm> {

    protected UpdateListener(Validator validator) {
        super(validator);
    }

    @Override
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='settings' && event.source.subpage=='update'")
    public void processForm(final WebjetEvent<ThymeleafEvent> event) {

        // sprocesovanie dat formularu do objektu a validacia tohto objektu
        super.processForm(event);

        ModelMap model = event.getSource().getModel();
        // ziskanie objektu a vlozenie do modelu
        UpdateForm form = getForm();
        model.addAttribute("form", form);

        model.addAttribute("actualVersion", InitServlet.getActualVersion());

        Logger.debug(UpdateListener.class, "Is post: " + isPost());
        if (!isPost()) {
            // ak nie je request typu post, tak dalej nic nerobime
            return;
        }

        BindingResult errors = getBindingResult();
        if (errors.hasErrors()) {
            Logger.debug(UpdateListener.class, "We have errors: "+errors.getErrorCount());
            model.addAttribute("updateFileName", "ERROR");
            return;
        }

        try {
            //
            String fileVersionName = UpdateService.prepareUpdateFile(form.getDocument());
            model.addAttribute("updateFileName", fileVersionName);
        }  catch (Exception ex) {
            sk.iway.iwcm.Logger.error(UpdateListener.class, ex);
            //
            model.addAttribute("importedFileNameFail", "ERROR");
        } finally {
            //
            model.addAttribute("importedFileNameSucc", form.getDocument().getOriginalFilename());
        }
    }
}
