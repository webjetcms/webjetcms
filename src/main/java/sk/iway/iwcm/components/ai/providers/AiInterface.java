package sk.iway.iwcm.components.ai.providers;

import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

public interface AiInterface {
    public List<LabelValue> getSupportedModels(Prop prop);

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception;
    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, PrintWriter writer, HttpServletRequest request) throws Exception;
    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception;

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop);

    public Pair<String, String> getProviderInfo(Prop prop);
    public String getProviderId();
    public boolean isInit();
}