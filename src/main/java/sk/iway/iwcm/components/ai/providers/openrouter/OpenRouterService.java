package sk.iway.iwcm.components.ai.providers.openrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiClient;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.i18n.Prop;

/** WebJET UI and lifecycle adapter for the standalone OpenRouter provider. */
@Service
public class OpenRouterService extends LibrarySupportLogic {

    @Autowired
    public OpenRouterService(AiClient aiClient, WebjetAiConfigurationService configurationService) {
        super(aiClient, configurationService);
    }

    @Override
    public String getProviderId() {
        return "openrouter";
    }

    @Override
    public String getTitleKey() {
        return "components.ai_assistants.provider.openrouter.title";
    }

    @Override
    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        return null;
    }
}
