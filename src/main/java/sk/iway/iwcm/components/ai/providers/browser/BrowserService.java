package sk.iway.iwcm.components.ai.providers.browser;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

/**
 * Service for Chrome Built-in AI:
 * https://developer.chrome.com/docs/ai/built-in
 */
@Service
public class BrowserService implements AiInterface {

    private static final String PROVIDER_ID = "browser";
    private static final String TITLE_KEY = "components.ai_assistants.provider.browser.title";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return Constants.getBoolean("ai_browserAiEnabled");
    }

    public Pair<String, String> getProviderInfo(Prop prop) {
        return new Pair<>(PROVIDER_ID, prop.getText(TITLE_KEY));
    }

    public AssistantResponseDTO getAiAssistantResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception {
        return null;
    }

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) {
        return null;
    }

    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception {
        return null;
    }

    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request) {
        ArrayList<LabelValue> models = new ArrayList<>();

        models.add(new LabelValue("Gemini Nano", "v3Nano"));

        return models;
    }

    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, PrintWriter writer, HttpServletRequest request) throws Exception {
        return null;
    }

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        return null;
    }
}