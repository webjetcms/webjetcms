package sk.iway.iwcm.components.ai.providers.gemini;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

@Service
public class GeminiAssistantsService implements AiAssitantsInterface {

    private static final String PROVIDER_ID = "gemini";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(GeminiSupportService.getApiKey());
    }

    @Override
    public List<String> getFieldsToShow(String action) {
        if("create".equals(action)) return List.of("model", "useStreaming");
        else if("edit".equals(action)) return List.of("model", "useStreaming");
        else return new ArrayList<>();
    }

    @Override
    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity) {
       if(Tools.isEmpty(assistantEnity.getModel())) assistantEnity.setModel("gemini-pro-latest");
    }

    @Override
    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
      //Nothing to set
    }
}
