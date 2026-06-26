package sk.iway.iwcm.headless.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Request body for the headless forms submit endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormSubmitRequest {

    private String formId;
    private String formName;
    private String componentKey;
    private Map<String, String> fields;
    private String pagePath;
    private String locale;

    public FormSubmitRequest() {
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getComponentKey() {
        return componentKey;
    }

    public void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
