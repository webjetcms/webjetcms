package sk.iway.iwcm.components.ai.providers.browser;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.i18n.Prop;

/**
 * Service for Chrome Built-in AI:
 * https://developer.chrome.com/docs/ai/built-in
 */
@Service
public class BrowserAssistantsService implements AiAssitantsInterface {

    @Override
    public List<AssistantDefinitionEntity> getAiAssistants(Prop prop) throws Exception {
        return new ArrayList<>();
    }

    @Override
    public String insertAssistant(AssistantDefinitionEntity entity, Prop prop) throws Exception {
        return null;
    }

    @Override
    public void updateAssistant(AssistantDefinitionEntity assistantEnity, Prop prop) throws Exception {

    }

    @Override
    public void deleteAssistant(AssistantDefinitionEntity assistantEnity, Prop prop) throws Exception {

    }

    @Override
    public String getProviderId() {
        return "browser";
    }

    @Override
    public boolean isInit() {
        return true;
    }

}
