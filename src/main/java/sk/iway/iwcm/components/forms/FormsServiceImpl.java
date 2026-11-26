package sk.iway.iwcm.components.forms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.users.UserDetails;

@Service
public class FormsServiceImpl extends FormsService<FormsRepository, FormsEntity> {

    //private final FormsRepository formsRepository;

    @Autowired
    public FormsServiceImpl(FormsRepository formsRepository, FormSettingsRepository formSettingsRepository, FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository) {
        super(formsRepository, formSettingsRepository, formStepsRepository, formItemsRepository);
        //this.formsRepository = formsRepository;
    }

    /** Requires access before loading a form's editor rows or options. */
    public void requireFormAccess(String formName, UserDetails user) {
        if (!isFormAccessible(formName, user)) throw new AccessDeniedException("Form access denied.");
    }
}
