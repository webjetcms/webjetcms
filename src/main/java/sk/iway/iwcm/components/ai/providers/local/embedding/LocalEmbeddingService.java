package sk.iway.iwcm.components.ai.providers.local.embedding;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiClient;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

/** CMS configuration adapter for the local embedding-only provider. */
@Service
public class LocalEmbeddingService extends LibrarySupportLogic implements AiAssitantsInterface {

    public static final String DEFAULT_MODEL = LocalEmbeddingProvider.MODEL_ID;

    private final LocalEmbeddingProvider localEmbeddingProvider;

    @Autowired
    public LocalEmbeddingService(
        AiClient aiClient,
        WebjetAiConfigurationService configurationService,
        LocalEmbeddingProvider localEmbeddingProvider
    ) {
        super(aiClient, configurationService);
        this.localEmbeddingProvider = localEmbeddingProvider;
    }

    @Override
    public String getProviderId() {
        return LocalEmbeddingProvider.PROVIDER_ID;
    }

    @Override
    public String getTitleKey() {
        return "components.ai_assistants.provider.local-embedding.title";
    }

    @Override
    public boolean isInit() {
        return localEmbeddingProvider.isConfigured();
    }

    @Override
    public List<String> getFieldsToShow(String action) {
        if ("create".equals(action) || "edit".equals(action)) return List.of("model");
        return new ArrayList<>();
    }

    @Override
    public void prepareBeforeSave(AssistantDefinitionEntity assistantEntity) {
        if (Tools.isEmpty(assistantEntity.getModel())) assistantEntity.setModel(DEFAULT_MODEL);
        assistantEntity.setUseStreaming(false);
        assistantEntity.setUseTemporal(false);
    }

    @Override
    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        // The validated bundle defines the only supported model and has no UI-specific options.
    }
}
