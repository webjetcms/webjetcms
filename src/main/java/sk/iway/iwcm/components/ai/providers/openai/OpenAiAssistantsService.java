package sk.iway.iwcm.components.ai.providers.openai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Service
public class OpenAiAssistantsService extends OpenAiSupportService implements AiAssitantsInterface {

    private static final String PROVIDER_ID = "openai";
    private static final String AUTH_KEY = "open_ai_auth_key";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(Constants.getString(AUTH_KEY));
    }

    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity) {
        if(assistantEnity.getModel() == null) assistantEnity.setModel("gpt-3.5-turbo");
    }

    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        //open AI specific options
        page.addOptions("imagesQuality", getQualityOptions(), "label", "value", false);
        page.addOptions("imagesSize", getSizeOptions(), "label", "value", false);
    }

    public List<String> getFieldsToShow(String action) {
        if("create".equals(action)) return List.of("model", "useStreaming", "useTemporal");
        else if("edit".equals(action)) return List.of("model", "assistantKey", "useStreaming", "useTemporal");
        else return new ArrayList<>();
    }

    private List<LabelValue> getQualityOptions() {
        List<LabelValue> qualityOptions = new ArrayList<>();
        qualityOptions.add(new LabelValue("low", "low"));
        qualityOptions.add(new LabelValue("medium", "medium"));
        qualityOptions.add(new LabelValue("high", "high"));
        return qualityOptions;
    }

    private List<LabelValue> getSizeOptions() {
        List<LabelValue> sizeOptions = new ArrayList<>();
        sizeOptions.add(new LabelValue("auto", "auto"));
        sizeOptions.add(new LabelValue("1024x1024", "1024x1024"));
        sizeOptions.add(new LabelValue("1024x1536", "1024x1536"));
        sizeOptions.add(new LabelValue("1536x1024", "1536x1024"));
        return sizeOptions;
    }
}