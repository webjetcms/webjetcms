package sk.iway.iwcm.components.ai.rest;

import static sk.iway.iwcm.components.ai.jpa.SupportedActions.doesSupportAction;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

@Service
public class AiService {

    private final List<AiInterface> aiInterfaces;
    private static final String CACHE_OPTION_KEYS = "AiService.ProviderOptions.";
    private static final int CACHE_MODELS_TIME = 24 * 60;
    private static final String GROUPS_PREFIX = "components.ai_assistants.groups.";

    @Autowired
    public AiService(List<AiInterface> aiInterfaces) {
        this.aiInterfaces = aiInterfaces;
    }

    /* PUBLIC METHODS */
    public List<LabelValue> getProviders(Prop prop) {
        List<LabelValue> supportedValues = new ArrayList<>();
        for(AiInterface aiInterface : aiInterfaces) {
            Pair<String, String> providerInfo = aiInterface.getProviderInfo(prop);
            String configuredAppend = "";
            if (aiInterface.isInit() == false) configuredAppend = " (" + prop.getText("components.ai_assistants.provider.not_configured") + ")";
            supportedValues.add( new LabelValue(providerInfo.getSecond() + configuredAppend, providerInfo.getFirst()) );
        }
        return supportedValues;
    }

    public List<LabelValue> getModelOptions(String provider, Prop prop, HttpServletRequest request) {
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
                supportedValues = aiInterface.getSupportedModels(prop, request);
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

    public List<String> getModelOptions(String term, String provider, Prop prop, HttpServletRequest request) {
        List<String> values = new ArrayList<>();
        if(Tools.isEmpty(provider) == true) return values;

        values = labelsToStrings( getModelOptions(provider, prop, request) );

        if("%".equals(term)) return values;

        List<String> ac = new ArrayList<>();
        for(String value : values) {
            if(value.toLowerCase().contains( term.toLowerCase() ))
                ac.add(value);
        }
        return ac;
    }

    public AssistantResponseDTO getAiResponse(InputDataDTO inputData, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo, HttpServletRequest request) throws Exception {

        Prop prop = Prop.getInstance(request);
        AssistantDefinitionEntity assistant = getAssistant(inputData.getAssistantId(), assistantRepo, prop);

        if(doesSupportAction(assistant, SupportedActions.GENERATE_TEXT) == false) {
            throw new IllegalStateException( getNotSupportedAction(prop) );
        }

        if(Tools.isFalse( assistant.getKeepHtml() )) {
            inputData.removeHtml();
        }

        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                return aiInterface.getAiResponse(assistant, inputData, prop, statRepo, request);
            }
        }

        throw new IllegalStateException( getSomethingWrongErr(prop) );
    }

    public AssistantResponseDTO getAiImageResponse(InputDataDTO inputData, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo, HttpServletRequest request) throws Exception {

        Prop prop = Prop.getInstance(request);
        inputData.prepareData();

        AssistantDefinitionEntity assistant = getAssistant(inputData.getAssistantId(), assistantRepo, prop);

        if(doesSupportAction(assistant, SupportedActions.GENERATE_IMAGE) == false && doesSupportAction(assistant, SupportedActions.EDIT_IMAGE) == false) {
            throw new IllegalStateException( getNotSupportedAction(prop) );
        }

        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                return aiInterface.getAiImageResponse(assistant, inputData, prop, statRepo, request);
            }
        }

        throw new IllegalStateException( getSomethingWrongErr(prop) );
    }

    public AssistantResponseDTO getAiStreamResponse(InputDataDTO inputData, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo, PrintWriter writer, HttpServletRequest request) throws Exception {

        Prop prop = Prop.getInstance(request);
        AssistantDefinitionEntity assistant = getAssistant(inputData.getAssistantId(), assistantRepo, prop);

        if(doesSupportAction(assistant, SupportedActions.GENERATE_TEXT) == false || Tools.isFalse(assistant.getUseStreaming())) {
            throw new IllegalStateException( getNotSupportedAction(prop) );
        }

        if(Tools.isFalse( assistant.getKeepHtml() )) {
            inputData.removeHtml();
        }

        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                return aiInterface.getAiStreamResponse(assistant, inputData, prop, statRepo, writer, request);
            }
        }

        throw new IllegalStateException( getSomethingWrongErr(prop) );
    }

    public String getBonusHtml(Long assistantId, AssistantDefinitionRepository assistantRepo, HttpServletRequest request) {

        AssistantDefinitionEntity assistant = getAssistant(assistantId, assistantRepo, Prop.getInstance(request));

        Prop prop = Prop.getInstance(request);
        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                return aiInterface.getBonusHtml(assistant, prop);
            }
        }

        throw new IllegalStateException( getSomethingWrongErr(Prop.getInstance(request)) );
    }

    private AssistantDefinitionEntity getAssistant(Long assistantId, AssistantDefinitionRepository assistantRepo, Prop prop) {
        //
        if(assistantId == null || assistantId < 1L) throw new IllegalStateException( getNotFoundAssistantErr(prop) );

        //
        Optional<AssistantDefinitionEntity> assistant = assistantRepo.findByIdAndDomainId(assistantId, CloudToolsForCore.getDomainId());

        if(assistant.isPresent() == false) throw new IllegalStateException( getNotFoundAssistantErr(prop) );

        return assistant.get();
    }

    public List<OptionDto> getGroupsOptions(Prop prop) {
        //group is defined as text key with prefix
        Map<String,String> groups = prop.getTextStartingWith(GROUPS_PREFIX);

        //sort groups by key
        List<OptionDto> groupsList = new ArrayList<>();
        for (Map.Entry<String, String> entry : groups.entrySet()) {
            //skip empty/- values - so the user can aka delete default entries
            if (Tools.isEmpty(entry.getValue()) || entry.getValue().length()<2) continue;
            groupsList.add(new OptionDto(entry.getValue(), entry.getKey().substring(GROUPS_PREFIX.length()), null));
        }
        groupsList.sort(Comparator.comparing(OptionDto::getValue));
        return groupsList;
    }

    /* PRIVATE SUPPORT METHODS */

    private List<String> labelsToStrings(List<LabelValue> labels) {
        List<String> values = new ArrayList<>();
        for(LabelValue label : labels) {
            values.add(label.getValue());
        }
        return values;
    }

    private String getNotSupportedAction(Prop prop) {
        return prop.getText("components.ai_assistants.not_supproted_action_err");
    }

    private String getSomethingWrongErr(Prop prop) {
        return prop.getText("html_area.insert_image.error_occured");
    }

    private String getNotFoundAssistantErr(Prop prop) {
        return prop.getText("components.ai_assistants.not_found");
    }
}
