package sk.iway.iwcm.components.ai.providers.openrouter;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.ai.providers.openai.OpenAiService;

/**
 * Service for OpenRouter API - https://openrouter.ai/
 */
public class OpenRouterService extends OpenAiService {

    @Override
    public String getProviderId() {
        return "openrouter";
    }
    @Override
    public String getServiceName() {
        return "OpenRouter";
    }
    //TODO: @Override
    public static String getApiKey() {
        return Constants.getString("ai_openRouterAuthKey");
    }

}
