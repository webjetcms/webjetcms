package sk.iway.iwcm.components.ai.providers;

/** WebJET configuration keys used by the CMS adapter, never by the AI library. */
public final class WebjetAiConfigKeys {

    public static final String OPENAI_API_KEY = "ai_openAiAuthKey";
    public static final String GEMINI_API_KEY = "ai_geminiAuthKey";
    public static final String OPENROUTER_API_KEY = "ai_openRouterAuthKey";
    public static final String BROWSER_ENABLED = "ai_browserAiEnabled";
    public static final String FILE_NAME_PROMPT = "ai_generateFileNamePrompt";
    public static final String OPENAI_FILE_NAME_MODEL = "ai_openAi_generateFileNameModel";
    public static final String GEMINI_FILE_NAME_MODEL = "ai_gemini_generateFileNameModel";
    public static final String OPENROUTER_FILE_NAME_MODEL = "ai_openRouter_generateFileNameModel";
    public static final String AUDIT_MAX_LENGTH = "ai_auditMaxLength";
    public static final String PROVIDER_CONNECT_TIMEOUT_SECONDS = "ai_providerConnectTimeoutSeconds";
    public static final String PROVIDER_RESPONSE_TIMEOUT_SECONDS = "ai_providerResponseTimeoutSeconds";

    private WebjetAiConfigKeys() { }
}
