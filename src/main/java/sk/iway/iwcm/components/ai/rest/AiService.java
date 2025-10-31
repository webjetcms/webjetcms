package sk.iway.iwcm.components.ai.rest;

import static sk.iway.iwcm.components.ai.jpa.SupportedActions.doesSupportAction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.json.DataTableAi;
import sk.iway.iwcm.system.datatable.json.DataTableColumn;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Main service for AI assistants - handles calls to specific providers
 */
@Service
public class AiService {

    private final List<AiInterface> aiInterfaces;
    private final AiTaskRegistry aiTaskRegistry;

    private static final String CACHE_OPTION_KEYS = "AiService.ProviderOptions.";
    private static final int CACHE_MODELS_TIME = 24 * 60;
    private static final String GROUPS_PREFIX = "components.ai_assistants.groups.";

    @Autowired
    public AiService(List<AiInterface> aiInterfaces, AiTaskRegistry aiTaskRegistry) {
        this.aiInterfaces = aiInterfaces;
        this.aiTaskRegistry = aiTaskRegistry;
    }

    /* PUBLIC METHODS */
    public List<LabelValue> getProviders(Prop prop) {
        List<LabelValue> supportedValues = new ArrayList<>();
        for(AiInterface aiInterface : aiInterfaces) {
            String configuredAppend = "";
            if (aiInterface.isInit() == false) configuredAppend = " (" + prop.getText("components.ai_assistants.provider.not_configured") + ")";
            supportedValues.add( new LabelValue(prop.getText(aiInterface.getTitleKey()) + configuredAppend, aiInterface.getProviderId()) );
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

    public AssistantResponseDTO getAiResponse(InputDataDTO inputData, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo, HttpServletRequest request) throws IllegalStateException, ProviderCallException {

        Prop prop = Prop.getInstance(request);
        inputData.prepareData(request);

        AssistantDefinitionEntity assistant = getAssistant(inputData.getAssistantId(), assistantRepo, prop);

        if(doesSupportAction(assistant, SupportedActions.GENERATE_TEXT, SupportedActions.LIVE_CHAT) == false) {
            throw new IllegalStateException( getNotSupportedAction(prop) );
        }

        if(Tools.isFalse( assistant.getKeepHtml() )) {
            inputData.removeHtml();
        }

        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                // Prepare task
                Callable<AssistantResponseDTO> task = () -> aiInterface.getAiResponse(assistant, inputData, prop, statRepo, request);
                // Run task
                return aiTaskRegistry.runAssistantTask(task, inputData, request);
            }
        }

        throw new IllegalStateException( getSomethingWrongErr(prop) );
    }

    public AssistantResponseDTO getAiImageResponse(InputDataDTO inputData, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo, HttpServletRequest request) throws IllegalStateException, ProviderCallException {

        Prop prop = Prop.getInstance(request);
        inputData.prepareData(request);

        AssistantDefinitionEntity assistant = getAssistant(inputData.getAssistantId(), assistantRepo, prop);

        if(doesSupportAction(assistant, SupportedActions.GENERATE_IMAGE) == false && doesSupportAction(assistant, SupportedActions.EDIT_IMAGE) == false) {
            throw new IllegalStateException( getNotSupportedAction(prop) );
        }

        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                // Prepare task
                Callable<AssistantResponseDTO> task = () -> aiInterface.getAiImageResponse(assistant, inputData, prop, statRepo, request);
                // Run task
                return aiTaskRegistry.runAssistantTask(task, inputData, request);
            }
        }

        throw new IllegalStateException( getSomethingWrongErr(prop) );
    }

    public AssistantResponseDTO getAiStreamResponse(InputDataDTO inputData, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo, BufferedWriter writer, HttpServletRequest request) throws IllegalStateException, ProviderCallException {

        Prop prop = Prop.getInstance(request);
        inputData.prepareData(request);

        AssistantDefinitionEntity assistant = getAssistant(inputData.getAssistantId(), assistantRepo, prop);

        if(doesSupportAction(assistant, SupportedActions.GENERATE_TEXT, SupportedActions.LIVE_CHAT) == false || Tools.isFalse(assistant.getUseStreaming())) {
            throw new IllegalStateException( getNotSupportedAction(prop) );
        }

        if(Tools.isFalse( assistant.getKeepHtml() )) {
            inputData.removeHtml();
        }

        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                // Prepare task
                Callable<AssistantResponseDTO> task = () -> aiInterface.getAiStreamResponse(assistant, inputData, prop, statRepo, writer, request);
                // Run task
                return aiTaskRegistry.runAssistantTask(task, inputData, request);
            }
        }

        throw new IllegalStateException( getSomethingWrongErr(prop) );
    }

    public String getBonusHtml(Long assistantId, AssistantDefinitionRepository assistantRepo, HttpServletRequest request) {

        AssistantDefinitionEntity assistant = getAssistant(assistantId, assistantRepo, Prop.getInstance(request));

        String bonusHtml = null;

        Prop prop = Prop.getInstance(request);
        for(AiInterface aiInterface : aiInterfaces) {
            if(aiInterface.isInit() == true && aiInterface.getProviderId().equals(assistant.getProvider())) {
                bonusHtml = aiInterface.getBonusHtml(assistant, prop);
                break;
            }
        }

        if (Tools.isNotEmpty(bonusHtml)) {
            return bonusHtml;
        }

        //for chat fields enable option to add content after current OR replace it
        if (Tools.isTrue(assistant.getUserPromptEnabled()) && assistant.getAction().equals(SupportedActions.LIVE_CHAT.getAction())) {
            //for chat fields enable option to add content after current OR replace it
            String editHTML = "";
            String checked = " checked";
            if (Tools.isNotEmpty(assistant.getFieldFrom())) {
                editHTML = """
                    <input type="radio" class="btn-check" name="bonusContent-replaceMode" id="bonusContent-replaceMode-edit" value="edit" autocomplete="off" checked>
                    <label class="btn btn-outline-primary btn-sm" for="bonusContent-replaceMode-edit" title="%s" data-bs-toggle="tooltip">%s</label>
                """.formatted(
                    prop.getText("components.ai_assistants.editor.replaceMode.edit.tooltip"),
                    prop.getText("components.ai_assistants.editor.replaceMode.edit")
                );
                checked = "";
            }

            return """
                <div class='bonus-content row mt-3'>
                    <div class='col-12'>
                        <div class="btn-group" role="group" aria-label="%s">
                            <input type="radio" class="btn-check" name="bonusContent-replaceMode" id="bonusContent-replaceMode-append" value="append" autocomplete="off" %s>
                            <label class="btn btn-outline-primary btn-sm" for="bonusContent-replaceMode-append" title="%s" data-bs-toggle="tooltip">%s</label>

                            %s

                            <input type="radio" class="btn-check" name="bonusContent-replaceMode" id="bonusContent-replaceMode-replace" value="replace" autocomplete="off">
                            <label class="btn btn-outline-primary btn-sm" for="bonusContent-replaceMode-replace" title="%s" data-bs-toggle="tooltip">%s</label>
                        </div>
                    </div>
                </div>
            """.formatted(
                prop.getText("components.ai_assistants.editor.replaceMode.label"),
                checked,
                prop.getText("components.ai_assistants.editor.replaceMode.append.tooltip"),
                prop.getText("components.ai_assistants.editor.replaceMode.append"),
                editHTML,
                prop.getText("components.ai_assistants.editor.replaceMode.replace.tooltip"),
                prop.getText("components.ai_assistants.editor.replaceMode.replace")
            );
        }

        return null;
    }

    private AssistantDefinitionEntity getAssistant(Long assistantId, AssistantDefinitionRepository assistantRepo, Prop prop) {
        if(assistantId == null || assistantId < 1L) throw new IllegalStateException( getNotFoundAssistantErr(prop) );
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

    public String checkExistance(String fileLocation, String currentName, String generatedName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode response = mapper.createArrayNode();

        if (Tools.isEmpty(fileLocation) == true) throw new IOException("XX");

        Path location = Paths.get(Tools.getRealPath(fileLocation));

        if (Files.notExists(location)) {
            //In this scenario ... location does not exist YET, so no re-write can happen
            putAnswer(response, currentName, false);
            putAnswer(response, generatedName, false);
            return response.toString();
        }

        if (Files.isDirectory(location) == false) throw new IOException("Location is not a dicrectory");

        putAnswer(response, currentName, containFileName(location, currentName));
        putAnswer(response, generatedName, containFileName(location, generatedName));

        return response.toString();
    }

    private void putAnswer(ArrayNode response, String name, boolean doExist) {
        ObjectNode newObject = response.addObject();
        newObject.put("name", name);
        newObject.put("doExist", doExist);
    }

    private boolean containFileName(Path location, String targetName) throws IOException {
        //compare only names without extension
        final String targetNameNoExt = FileTools.getFileNameWithoutExtension(targetName);
        try (var stream = Files.list(location)) {
            return stream
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .map(name -> {
                    int dotIndex = name.lastIndexOf('.');
                    return (dotIndex == -1) ? name : name.substring(0, dotIndex);
                })
                .anyMatch(baseName -> baseName.equalsIgnoreCase(targetNameNoExt));
        }
    }

    public static List<DataTableAi> getAiAssistantsForField(String toField, String javaClassName, DataTableColumn column, Prop prop) {
        List<DataTableAi> aiList = new ArrayList<>();
        try {
            List<AssistantDefinitionEntity> assistants = AiAssistantsService.getAssistantAndFieldFrom(toField, column, javaClassName);

            //sort assistants by group and name
            assistants.sort(
                Comparator.comparing(AssistantDefinitionEntity::getGroupName, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(AssistantDefinitionEntity::getName)
            );

            if(assistants != null && assistants.size() > 0) {

                for (AssistantDefinitionEntity ade : assistants) {
                    DataTableAi ai = new DataTableAi();
                    ai.setAssistantId(ade.getId());
                    ai.setFrom(ade.getFieldFrom());
                    ai.setTo(toField);
                    ai.setToDefinition(ade.getFieldTo());
                    if (Tools.isEmpty(ade.getDescription())) ai.setDescription(ade.getName());
                    else ai.setDescription(prop.getText(ade.getDescription()));
                    ai.setProvider(ade.getProvider());

                    String providerTitleKey = "components.ai_assistants.provider."+ade.getProvider()+".title";
                    String providerTitle = prop.getText(providerTitleKey);
                    if (providerTitleKey.equals(providerTitle)) providerTitle = ade.getProvider();
                    ai.setProviderTitle(providerTitle);

                    ai.setUseStreaming(Tools.isTrue(ade.getUseStreaming()));
                    ai.setAction(ade.getAction());

                    String groupName = ade.getGroupName();
                    ai.setGroupName(prop.getText(AssistantDefinitionRestController.GROUPS_PREFIX + groupName));
                    ai.setUserPromptEnabled(Tools.isTrue(ade.getUserPromptEnabled()));
                    ai.setUserPromptLabel(prop.getText(ade.getUserPromptLabel()));
                    ai.setIcon(ade.getIcon());
                    if (Tools.isEmpty(ai.getIcon())) ai.setIcon("clipboard-text");

                    if ("browser".equals(ai.getProvider())) {
                        //we need instructions to execute local AI in browser
                        ai.setInstructions(ade.getInstructions());
                    }
                    if (ai.isEmpty()==false) {
                        aiList.add(ai);
                    }
                }

                //for custom fields detect if we have any assistant specially for this field, if yes, remove other general assistants
                if (Tools.isNotEmpty(toField) && toField.startsWith("field") && toField.length()=="fieldX".length()) {
                    List<DataTableAi> specificAis = new ArrayList<>();
                    for (DataTableAi ai : aiList) {
                        String[] toFields = Tools.getTokens(ai.getToDefinition(), "\n,;", true);
                        for (String tf : toFields) {
                            if ("*".equals(tf)) continue;
                            if (AiAssistantsService.isMatching(tf, toField)) {
                                specificAis.add(ai);
                                break;
                            }
                        }
                    }
                    if (specificAis.size()>0) {
                        aiList = specificAis;
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(DataTableAi.class, "Error setting properties", e);
        }
        return aiList;
    }
}