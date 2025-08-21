package sk.iway.iwcm.components.ai.providers.local;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

@Service
public class LocalAssistantsService implements AiAssitantsInterface {

    private static final String PROVIDER_ID = "local";

    public boolean isInit() {
       return true;
    }

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public List<AssistantDefinitionEntity> getAiAssistants(Prop prop) throws Exception {
        //All assistant are local in DB, so we have nothing more to return
        return new ArrayList<>();
    }

    public List<String> getFieldsToShow(String action) {
        //Local assistant do not need specific fields, base are enought
        return new ArrayList<>();
    }

    public void deleteAssistant(AssistantDefinitionEntity assistantEnity, Prop prop) throws Exception {
        // TODO Auto-generated method stub

    }

    public String insertAssistant(AssistantDefinitionEntity entity, Prop prop) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity) {
        // TODO Auto-generated method stub

    }

    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        // TODO Auto-generated method stub

    }

    public void updateAssistant(AssistantDefinitionEntity assistantEnity, Prop prop) throws Exception {
        // TODO Auto-generated method stub
    }

}
