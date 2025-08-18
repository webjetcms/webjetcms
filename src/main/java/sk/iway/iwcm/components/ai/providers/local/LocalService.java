package sk.iway.iwcm.components.ai.providers.local;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

@Service
public class LocalService implements AiInterface {

    private static final String PROVIDER_ID = "local";
    private static final String TITLE_KEY = "components.ai_provider.local";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return true;
    }

    public Pair<String, String> getProviderInfo(Prop prop) {
        return new Pair<>(PROVIDER_ID, prop.getText(TITLE_KEY));
    }

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo) {
        return null;
    }

    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo) throws Exception {
        return null;
    }

    public List<LabelValue> getSupportedModels(Prop prop) {
        return new ArrayList<>();
    }

    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, PrintWriter writer) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }
}