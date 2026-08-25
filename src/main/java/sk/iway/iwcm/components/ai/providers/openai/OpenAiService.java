package sk.iway.iwcm.components.ai.providers.openai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.provider.openai.OpenAiProvider;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

/** WebJET UI and lifecycle adapter for the standalone OpenAI provider. */
@Service
public class OpenAiService extends LibrarySupportLogic implements AiAssitantsInterface {

    public static final String API_KEY = "ai_openAiAuthKey";
    public static final String IMAGE_NAME_MODEL = "ai_openAi_generateFileNameModel";

    @Autowired
    public OpenAiService(AiClient aiClient, WebjetAiConfigurationService configurationService) {
        super(aiClient, configurationService);
    }

    @Override
    public String getProviderId() {
        return OpenAiProvider.PROVIDER_ID;
    }

    @Override
    public String getTitleKey() {
        return "components.ai_assistants.provider.openai.title";
    }

    @Override
    public String getApiKey() {
        return Constants.getString(API_KEY);
    }

    @Override
    public String getImageNameModel() {
        return Constants.getString(IMAGE_NAME_MODEL);
    }

    @Override
    public void prepareBeforeSave(AssistantDefinitionEntity assistantEntity) {
        if (Tools.isEmpty(assistantEntity.getModel())) assistantEntity.setModel("gpt-5-mini");
    }

    @Override
    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        // Image options are resolved per model when the assistant is invoked.
    }

    @Override
    public List<String> getFieldsToShow(String action) {
        if ("create".equals(action) || "edit".equals(action)) {
            return List.of("model", "useStreaming", "useTemporal");
        }
        return new ArrayList<>();
    }

}
