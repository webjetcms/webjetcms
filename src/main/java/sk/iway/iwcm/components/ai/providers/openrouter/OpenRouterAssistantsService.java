package sk.iway.iwcm.components.ai.providers.openrouter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

/**
 * Service for OpenRouter assistants - https://openrouter.ai/
 */
@Service
public class OpenRouterAssistantsService implements AiAssitantsInterface {

    private final WebjetAiConfigurationService configurationService;

    @Autowired
    public OpenRouterAssistantsService(WebjetAiConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public String getProviderId() {
        return "openrouter";
    }

    public boolean isInit() {
       return configurationService.isConfigured(getProviderId());
    }

    public List<String> getFieldsToShow(String action) {
        if("create".equals(action)) return List.of("model", "useStreaming");
        else if("edit".equals(action)) return List.of("model", "useStreaming");
        else return new ArrayList<>();
    }

    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity) {
        // Nothing to do
    }

    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        // Nothing to do
    }
}
