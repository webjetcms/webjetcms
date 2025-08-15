package sk.iway.iwcm.components.ai.providers;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

public interface AiInterface {
    public List<LabelValue> getSupportedModels(Prop prop);

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, String content, Prop prop, AiStatRepository statRepo) throws Exception;
    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, String content, Prop prop, AiStatRepository statRepo, PrintWriter writer) throws Exception;
    public AssistantResponseDTO getAiImageResponse(File fileImage) throws Exception;

    public Pair<String, String> getProviderInfo(Prop prop);
    public String getProviderId();
    public boolean isInit();
}