package sk.iway.iwcm.components.ai.providers;

import java.util.List;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.i18n.Prop;

public interface AiAssitantsInterface {
    public List<AssistantDefinitionEntity> getAiAssistants(Prop prop) throws Exception;
    public String insertAssistant(AssistantDefinitionEntity entity, Prop prop) throws Exception;
    public void updateAssistant(AssistantDefinitionEntity assistantEnity, Prop prop) throws Exception;
    public void deleteAssistant(AssistantDefinitionEntity assistantEnity, Prop prop) throws Exception;
    public String getProviderId();
    public boolean isInit();

    default String getAssistantPrefix() {
        return AiAssistantsService.getAssitantPrefix();
    }
}