package sk.iway.iwcm.components.ai.providers;

import java.util.List;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

public interface AiAssitantsInterface {
    //Table related methods
    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity);
    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop);
    public List<String> getFieldsToShow(String action);
    public String getProviderId();
    public boolean isInit();
}