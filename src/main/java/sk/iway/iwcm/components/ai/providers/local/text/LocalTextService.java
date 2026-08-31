package sk.iway.iwcm.components.ai.providers.local.text;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiPromptTemplate;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.components.ai.security.PromptInjectionDefense;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

/**
 * Adapts the local EuroLLM text-generation provider to CMS assistant configuration and prompt validation.
 */
@Service
public class LocalTextService extends LibrarySupportLogic implements AiAssitantsInterface {

    public static final String DEFAULT_MODEL = LocalTextProvider.MODEL_ID;

    private final LocalTextProvider localTextProvider;

    @Autowired
    public LocalTextService(
        AiClient aiClient,
        WebjetAiConfigurationService configurationService,
        LocalTextProvider localTextProvider
    ) {
        super(aiClient, configurationService);
        this.localTextProvider = localTextProvider;
    }

    @Override
    public String getProviderId() {
        return LocalTextProvider.PROVIDER_ID;
    }

    @Override
    public String getTitleKey() {
        return "components.ai_assistants.provider.local-generation.title";
    }

    @Override
    public boolean isInit() {
        return localTextProvider.isConfigured();
    }

    @Override
    public List<String> getFieldsToShow(String action) {
        if ("create".equals(action) || "edit".equals(action)) return List.of("model");
        return List.of();
    }

    /**
     * Applies the fixed local text model and feature flags required for local generation.
     *
     * @param assistantEntity assistant configuration being prepared for persistence
     */
    @Override
    public void prepareBeforeSave(AssistantDefinitionEntity assistantEntity) {
        assistantEntity.setModel(DEFAULT_MODEL);
        assistantEntity.setUseStreaming(false);
        assistantEntity.setUseTemporal(true);
    }

    /**
     * Rejects prompt expansions that contain sources detected as prompt injection.
     *
     * @param expansion expanded prompt and its detected suspicious sources
     * @param assistantId assistant identifier used when auditing detections
     * @throws IOException if any expanded source is detected as prompt injection
     */
    @Override
    protected void validatePromptExpansion(
        AiPromptTemplate.ExpansionResult expansion,
        Long assistantId
    ) throws IOException {
        if (expansion.suspiciousSources().isEmpty() == false) {
            PromptInjectionDefense.auditDetections(expansion.suspiciousSources(), assistantId);
            throw new IOException("Local text generation rejects input detected as prompt injection");
        }
    }

    @Override
    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
    }
}
