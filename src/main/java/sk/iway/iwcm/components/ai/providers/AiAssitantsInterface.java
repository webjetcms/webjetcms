package sk.iway.iwcm.components.ai.providers;

import java.util.List;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

/**
 * Interface for AI assistants providers for datatable handling - shows provider specific options in the DT Editor
 */
public interface AiAssitantsInterface {
    /** Providers can apply their model defaults before an assistant is saved. */
    default void prepareBeforeSave(AssistantDefinitionEntity assistant) { }

    /** Providers can add their own option lists to the assistant editor. */
    default void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) { }

    public List<String> getFieldsToShow(String action);
    public String getProviderId();
    public boolean isInit();
}