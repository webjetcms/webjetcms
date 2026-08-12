package sk.iway.iwcm.components.ai.providers.gemini;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiClient;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.i18n.Prop;

/** WebJET UI and lifecycle adapter for the standalone Gemini provider. */
@Service
public class GeminiService extends LibrarySupportLogic {

    @Autowired
    public GeminiService(AiClient aiClient, WebjetAiConfigurationService configurationService) {
        super(aiClient, configurationService);
    }

    @Override
    public String getProviderId() {
        return "gemini";
    }

    @Override
    public String getTitleKey() {
        return "components.ai_assistants.provider.gemini.title";
    }

    @Override
    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        return null;
    }
}
