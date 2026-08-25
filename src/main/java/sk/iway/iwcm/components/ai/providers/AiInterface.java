package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedWriter;
import java.util.List;

import com.webjetcms.ai.AiProviderConfig;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Interface for AI providers - main methods to implement in specific provider services
 */
public interface AiInterface {

    List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request);

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws ProviderCallException;
    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, BufferedWriter writer, HttpServletRequest request) throws ProviderCallException;
    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws ProviderCallException;

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop);

    public String getProviderId();
    public String getTitleKey();
    public boolean isInit();

    default String getApiKey() {
        return "";
    }

    default String getImageNameModel() {
        return "";
    }

    /** Adds provider-specific settings using a referer validated by WebJET CMS. */
    default void configure(AiProviderConfig.Builder builder, String trustedReferer) {
        // Most providers only need an API key and the shared timeout settings.
    }
}
