package sk.iway.iwcm.components.form_settings.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/form-settings")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_form')")
@Datatable
public class FormSettingsRestController extends DatatableRestControllerV2<FormSettingsEntity, Long> {

    private final FormSettingsRepository formSettingsRepository;
    private final FormSettingsService formSettingsService;

    @Autowired
    public FormSettingsRestController(FormSettingsRepository formSettingsRepository, FormSettingsService formSettingsService) {
        super(formSettingsRepository);
        this.formSettingsRepository = formSettingsRepository;
        this.formSettingsService = formSettingsService;
    }

    @PostMapping(value = "/save_attributes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void saveFormAttributes(@RequestBody FormSettingsEntity formSettings) {
        if(Tools.isEmpty(formSettings.getFormName())) throw new IllegalStateException("Form name (formName) is required and must not be empty.");

        formSettings.setFormName( DocTools.removeChars(formSettings.getFormName(), true) );
        formSettingsService.prepareSettingsForSave(formSettings, FormsService.FORM_TYPE.UNKNOWN.value()); // setting unknown so it wont run MULTISTEP logic
        formSettingsRepository.save(formSettings);
    }

    @GetMapping("/autocomplete-formProcessor")
    public List<String> getAutocompleteClass(@RequestParam String term) {
        return formSettingsService.getFormProcessorOptions(term);
    }
}