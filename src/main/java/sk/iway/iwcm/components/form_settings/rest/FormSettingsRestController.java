package sk.iway.iwcm.components.form_settings.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/form-settings")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_form')")
@Datatable
public class FormSettingsRestController extends DatatableRestControllerV2<FormSettingsEntity, Long> {

    private final FormSettingsRepository formSettingsRepository;

    @Autowired
    public FormSettingsRestController(FormSettingsRepository formSettingsRepository) {
        super(formSettingsRepository);
        this.formSettingsRepository = formSettingsRepository;
    }

    @PostMapping(value = "/save_attributes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void saveFormAttributes(@RequestBody FormSettingsEntity formSettings) {
        if(Tools.isEmpty(formSettings.getFormName())) throw new IllegalStateException("");

        // Try get and set id
        Long formSettingsId = formSettingsRepository.findId(formSettings.getFormName(), CloudToolsForCore.getDomainId());
        formSettings.setId(formSettingsId);

        // convert object back to id
        if(formSettings.getUseFormMailDoc() == null) formSettings.setUseFormMailDocId(null);
        else formSettings.setUseFormMailDocId(formSettings.getUseFormMailDoc().getDocId());

        // convert object back to id
        if(formSettings.getFormmailSendUserInfoDoc() == null) formSettings.setFormmailSendUserInfoDoc(null);
        else formSettings.setFormmailSendUserInfoDocId(formSettings.getFormmailSendUserInfoDoc().getDocId());

        //
        if(formSettings.getDomainId() == null) formSettings.setDomainId(CloudToolsForCore.getDomainId());

        formSettingsRepository.save(formSettings);
    }
}
