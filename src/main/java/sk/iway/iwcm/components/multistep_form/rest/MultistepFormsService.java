package sk.iway.iwcm.components.multistep_form.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

public class MultistepFormsService {

    private static final String ITEM_KEY_LABEL_PREFIX = "components.formsimple.label.";
    private static final String ITEM_KEY_HIDE_FIELDS_PREFIX = "components.formsimple.hide.";

    public static String getFormName(Map<String, String> params) {
        String formName = Tools.getStringValue(params.get("formName"), "");
        return DocTools.removeChars(formName, true);
    }

    public static String getFormName(HttpServletRequest request) {
        String formName = Tools.getStringValue(request.getParameter("formName"), "");
        return DocTools.removeChars(formName, true);
    }

    public static List<LabelValue> getFormStepsOptions(String formName, FormStepsRepository formStepsRepository) {
        List<LabelValue> options = new ArrayList<>();

        if(Tools.isEmpty(formName)) return options;

        int counter = 1;
        for(FormStepEntity step : formStepsRepository.findAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId())) {
            options.add(new LabelValue(step.getStepName() + " (" + counter++ + ")", step.getId() + ""));
        }

        return options;
    }

    public static List<LabelValue> getFieldTypes(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<LabelValue> options = new ArrayList<>();
        Map<String, String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_LABEL_PREFIX);

        for(Entry<String, String> entry : formsimpleFields.entrySet())
            options.add(new LabelValue(entry.getValue(), entry.getKey().substring(ITEM_KEY_LABEL_PREFIX.length())));

        return options;
    }

    public static List<LabelValue> getFiledTypeVisibility(HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);

        List<LabelValue> options = new ArrayList<>();
        Map<String, String> formsimpleFields = prop.getTextStartingWith(ITEM_KEY_HIDE_FIELDS_PREFIX);

        for(Entry<String, String> entry : formsimpleFields.entrySet()) {
            options.add(new LabelValue(entry.getValue(), entry.getKey().substring(ITEM_KEY_HIDE_FIELDS_PREFIX.length())));
        }


        options.add(new LabelValue("id,formName", "allwaysHidden"));
        return options;
    }
}