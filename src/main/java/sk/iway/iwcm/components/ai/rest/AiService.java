package sk.iway.iwcm.components.ai.rest;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

@Service
public class AiService {

    private final List<AiInterface> aiInterfaces;
    private static final String CACHE_OPTION_KEYS = "AiService.ProviderOptions.";
    private static final int CACHE_MODELS_TIME = 24 * 60;
    private static final String ACTION_PREFIX = "components.ai_assistants.supported_actions.";

    @Autowired
    public AiService(List<AiInterface> aiInterfaces) {
        this.aiInterfaces = aiInterfaces;
    }

    /* PUBLIC METHODS */

    public List<LabelValue> getSupportedProviders(Prop prop) {
        List<LabelValue> supportedValues = new ArrayList<>();
        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == false) continue;
            Pair<String, String> providerInfo = aiInterface.getProviderInfo(prop);
            supportedValues.add( new LabelValue(providerInfo.getSecond(), providerInfo.getFirst()) );
        }
        return supportedValues;
    }

    public List<LabelValue> getSupportedActions(Prop prop) {
        List<LabelValue> supportedValues = new ArrayList<>();

        supportedValues.add(new LabelValue("", ""));
        for(String action : List.of("generate_text", "generate_image", "edit_image", "live_chat")) {
            supportedValues.add(new LabelValue(prop.getText(ACTION_PREFIX + action), action));
        }

        return supportedValues;
    }

    public List<LabelValue> getModelOptions(String provider, Prop prop) {
        List<LabelValue> supportedValues = new ArrayList<>();

        //First check, if they are cached
        try {
            Cache c = Cache.getInstance();
            @SuppressWarnings("unchecked")
            List<LabelValue> cachedModels = (List<LabelValue>)c.getObject(CACHE_OPTION_KEYS + provider);
            if(cachedModels != null) return cachedModels;
        } catch (Exception e) {
            return supportedValues;
        }

        //No cached, get them from PROVIDER
        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(provider)) {
                supportedValues = aiInterface.getSupportedModels(prop);
                break;
            }
        }

        if(supportedValues.size() < 1) return supportedValues;

        //Set to cache
        try {
            Cache c = Cache.getInstance();
            c.setObject(CACHE_OPTION_KEYS + provider, supportedValues, CACHE_MODELS_TIME);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return supportedValues;
    }

    public List<String> getModelOptions(String term, String provider, Prop prop) {
        List<String> values = new ArrayList<>();
        if(Tools.isEmpty(provider) == true) return values;

        values = labelsToStrings( getModelOptions(provider, prop) );

        if("%".equals(term)) return values;

        List<String> ac = new ArrayList<>();
        for(String value : values) {
            if(value.toLowerCase().contains( term.toLowerCase() ))
                ac.add(value);
        }
        return ac;
    }

    public AssistantResponseDTO getAiResponse(String assistantName, String inputData, Prop prop, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo) throws Exception {

        InputDataDTO inputDataDTO = new InputDataDTO(inputData);

        AssistantDefinitionEntity assistant = getAssistant(assistantName, assistantRepo);

        if(Tools.isFalse( assistant.getKeepHtml() )) {
            inputDataDTO.removeHtml();
        }

        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                return aiInterface.getAiResponse(assistant, inputDataDTO, prop, statRepo);
            }
        }

        throw new IllegalStateException("Something went wrong");
    }

    public AssistantResponseDTO getAiImageResponse(String assistantName, String inputData, Prop prop, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo) throws Exception {

        InputDataDTO inputDataDTO = new InputDataDTO(inputData);

        AssistantDefinitionEntity assistant = getAssistant(assistantName, assistantRepo);

        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                return aiInterface.getAiImageResponse(assistant, inputDataDTO, prop, statRepo);
            }
        }

        throw new IllegalStateException("Something went wrong");
    }

    public AssistantResponseDTO getAiStreamResponse(String assistantName, String inputData, Prop prop, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo, PrintWriter writer) throws Exception {

        InputDataDTO inputDataDTO = new InputDataDTO(inputData);

        AssistantDefinitionEntity assistant = getAssistant(assistantName, assistantRepo);

        if(Tools.isFalse( assistant.getKeepHtml() )) {
            inputDataDTO.removeHtml();
        }

        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                return aiInterface.getAiStreamResponse(assistant, inputDataDTO, prop, statRepo, writer);
            }
        }

        throw new IllegalStateException("Something went wrong");
    }

    private AssistantDefinitionEntity getAssistant(String assistantName, AssistantDefinitionRepository assistantRepo) {
        //
        if(Tools.isEmpty(assistantName)) throw new IllegalStateException("No assistant found.");

        //
        String prefix = AiAssistantsService.getAssitantPrefix();
        Optional<AssistantDefinitionEntity> assistant = assistantRepo.findFirstByNameAndDomainId(prefix + assistantName, CloudToolsForCore.getDomainId());

        if(assistant.isPresent() == false) throw new IllegalStateException("No assistant found.");

        return assistant.get();
    }

    /* PRIVATE SUPPORT METHODS */

    private List<String> labelsToStrings(List<LabelValue> labels) {
        List<String> values = new ArrayList<>();
        for(LabelValue label : labels) {
            values.add(label.getValue());
        }
        return values;
    }
}
