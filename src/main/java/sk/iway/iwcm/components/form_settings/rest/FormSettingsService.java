package sk.iway.iwcm.components.form_settings.rest;

import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;

public class FormSettingsService {

    public static void prepareSettingsForSave(FormSettingsEntity formSettings, FormSettingsRepository formSettingsRepository) {
        if(formSettings == null) return;

        // Try get and set id
        Long formSettingsId = formSettingsRepository.findId(formSettings.getFormName(), CloudToolsForCore.getDomainId());
        formSettings.setId(formSettingsId);

        // convert object back to id
        if(formSettings.getUseFormDoc() == null) formSettings.setUseFormDoc(null);
        else formSettings.setUseFormDocId(formSettings.getUseFormDoc().getDocId());

        // convert object back to id
        if(formSettings.getUseFormMailDoc() == null) formSettings.setUseFormMailDocId(null);
        else formSettings.setUseFormMailDocId(formSettings.getUseFormMailDoc().getDocId());

        // convert object back to id
        if(formSettings.getFormmailSendUserInfoDoc() == null) formSettings.setFormmailSendUserInfoDoc(null);
        else formSettings.setFormMailSendUserInfoDocId(formSettings.getFormmailSendUserInfoDoc().getDocId());

        //
        if(formSettings.getDomainId() == null) formSettings.setDomainId(CloudToolsForCore.getDomainId());
    }

    public static void prepareSettingsForEdit(FormSettingsEntity formSettings) {
        if(formSettings == null) return;

        if(formSettings.getUseFormDocId() != null) {
            DocDetails doc = DocDB.getInstance().getDoc( formSettings.getUseFormDocId() );
            if(doc != null) formSettings.setFormmailSendUserInfoDoc( new DocDetailsDto(doc) );
        }

        if(formSettings.getUseFormMailDocId() != null) {
            DocDetails doc = DocDB.getInstance().getDoc( formSettings.getUseFormMailDocId() );
            if(doc != null) formSettings.setUseFormMailDoc( new DocDetailsDto(doc) );
        }

        if(formSettings.getFormMailSendUserInfoDocId() != null) {
            DocDetails doc = DocDB.getInstance().getDoc( formSettings.getFormMailSendUserInfoDocId() );
            if(doc != null) formSettings.setFormmailSendUserInfoDoc( new DocDetailsDto(doc) );
        }
    }
}
