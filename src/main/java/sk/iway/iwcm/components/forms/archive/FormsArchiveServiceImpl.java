package sk.iway.iwcm.components.forms.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;

@Service
public class FormsArchiveServiceImpl extends FormsService<FormsArchiveRepository, FormsArchiveEntity> {

    //private final FormsRepository formsRepository;

    @Autowired
    public FormsArchiveServiceImpl(FormsArchiveRepository formsHistoryRepository, FormSettingsRepository formSettingsRepository, FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository) {
        super(formsHistoryRepository, formSettingsRepository, formStepsRepository, formItemsRepository);
        //this.formsRepository = formsRepository;
    }
}