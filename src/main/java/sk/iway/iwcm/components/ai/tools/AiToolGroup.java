package sk.iway.iwcm.components.ai.tools;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/** Catalog row for the visible endpoints registered by one REST controller. */
@Getter
public class AiToolGroup {
    public record Endpoint(String name, String httpMethod, String path) {}

    private final String id;

    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "components.ai_assistants.tools.controller", hiddenEditor = true)
    private final String controllerFile;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.ai_assistants.tools.description", hiddenEditor = true)
    private final String controllerDescription;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.tools.path_prefix", className = "dt-style-text-wrap", hiddenEditor = true)
    private final String pathPrefix;

    @DataTableColumn(inputType = DataTableColumnType.TEXT_NUMBER, title = "components.ai_assistants.tools.endpoint_count", hiddenEditor = true)
    private final int endpointCount;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.ai_assistants.tools.endpoint", tab = "basic",
        hidden = true, ai = false, className = "focus-priority")
    private final String endpoint = "";

    private final List<Endpoint> endpoints;

    AiToolGroup(Class<?> controllerType, List<AiToolRegistry.Entry> entries, Prop language) {
        this.id = controllerType.getName();
        this.controllerFile = controllerType.getNestHost().getSimpleName() + ".java";
        this.controllerDescription = AiToolRegistry.controllerDescription(controllerType, language);
        this.endpoints = entries.stream()
            .map(entry -> new Endpoint(entry.definition().name(), entry.method().name(), entry.path())).toList();
        this.pathPrefix = commonPathPrefix(endpoints);
        this.endpointCount = endpoints.size();
    }

    private static String commonPathPrefix(List<Endpoint> endpoints) {
        // A final slash lets an endpoint's full path match a parent segment of another endpoint.
        String[] paths = endpoints.stream().map(Endpoint::path)
            .map(path -> path.endsWith("/") ? path : path + "/").toArray(String[]::new);
        String prefix = StringUtils.getCommonPrefix(paths);
        int lastSlash = prefix.lastIndexOf('/');
        return lastSlash > 0 ? prefix.substring(0, lastSlash) : "/";
    }
}
