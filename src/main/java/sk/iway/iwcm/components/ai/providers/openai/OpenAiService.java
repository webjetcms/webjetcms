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
import sk.iway.iwcm.system.datatable.json.LabelValue;

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
    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        if ("edit_image".equals(assistant.getAction()) || "generate_image".equals(assistant.getAction())) {
            return """
                <div class='bonus-content row mt-3'>
                    <div class='col-sm-4'>
                        <label for='bonusContent-imageCount'>%s</label>
                        <input id='bonusContent-imageCount' type='number' class='form-control' value=1>
                    </div>
                    %s
                    %s
                </div>
                """.formatted(
                    prop.getText("components.ai_assistants.imageCount"),
                    getImageSizeSelect(assistant.getModel(), prop),
                    getImageQualitySelect(assistant.getModel(), prop)
                );
        }
        return "";
    }

    @Override
    public void prepareBeforeSave(AssistantDefinitionEntity assistantEntity) {
        if (Tools.isEmpty(assistantEntity.getModel())) assistantEntity.setModel("gpt-5-mini");
    }

    @Override
    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        page.addOptions("imagesQuality", getQualityOptions(), "label", "value", false);
        page.addOptions("imagesSize", getSizeOptions(), "label", "value", false);
    }

    @Override
    public List<String> getFieldsToShow(String action) {
        if ("create".equals(action) || "edit".equals(action)) {
            return List.of("model", "useStreaming", "useTemporal");
        }
        return new ArrayList<>();
    }

    private List<LabelValue> getQualityOptions() {
        return List.of(
            new LabelValue("low", "low"),
            new LabelValue("medium", "medium"),
            new LabelValue("high", "high")
        );
    }

    private List<LabelValue> getSizeOptions() {
        return List.of(
            new LabelValue("auto", "auto"),
            new LabelValue("1024x1024", "1024x1024"),
            new LabelValue("1024x1536", "1024x1536"),
            new LabelValue("1536x1024", "1536x1024")
        );
    }

    private String getImageSizeSelect(String model, Prop prop) {
        if (Tools.isEmpty(model)) return "";
        String square = prop.getText("components.gallery.tui-image-editor.Square");
        String options;

        if ("gpt-image-1".equals(model)) {
            options = """
                <option value="auto">auto</option>
                <option value="1024x1024">%s (1024x1024)</option>
                <option value="1024x1536">%s (1024x1536)</option>
                <option value="1536x1024">%s (1536x1024)</option>
                """.formatted(
                    square,
                    prop.getText("components.datatables-data-export.Na_vysku"),
                    prop.getText("components.datatables-data-export.Na_sirku")
                );
        } else if ("dall-e-3".equals(model)) {
            options = """
                <option value="1024x1024">%s (1024x1024)</option>
                <option value="1024x1792">%s (1024x1792)</option>
                <option value="1792x1024">%s (1792x1024)</option>
                """.formatted(
                    square,
                    prop.getText("components.datatables-data-export.Na_vysku"),
                    prop.getText("components.datatables-data-export.Na_sirku")
                );
        } else if ("dall-e-2".equals(model)) {
            options = """
                <option value="256x256">%s (256x256)</option>
                <option value="512x512">%s (512x512)</option>
                <option value="1024x1024">%s (1024x1024)</option>
                """.formatted(square, square, square);
        } else {
            return "";
        }

        return """
            <div class='col-sm-4'>
                <label for='bonusContent-imageSize'>%s</label>
                <select id='bonusContent-imageSize' class='form-control'>%s</select>
            </div>
            """.formatted(prop.getText("components.ai_assistants.imageSize"), options);
    }

    private String getImageQualitySelect(String model, Prop prop) {
        if (Tools.isEmpty(model)) return "";
        String options;

        if ("gpt-image-1".equals(model)) {
            options = """
                <option value="low">low</option>
                <option value="medium" selected="selected">medium</option>
                <option value="high">high</option>
                """;
        } else if ("dall-e-3".equals(model)) {
            options = """
                <option value="standard">standard</option>
                <option value="hd">hd</option>
                """;
        } else {
            return "";
        }

        return """
            <div class='col-sm-4'>
                <label for='bonusContent-imageQuality'>%s</label>
                <select id='bonusContent-imageQuality' class='form-control'>%s</select>
            </div>
            """.formatted(prop.getText("components.ai_assistants.imageQuality"), options);
    }
}
