package sk.iway.iwcm.components.ai.tools;

import java.util.List;

import io.swagger.v3.core.util.Json;
import lombok.Getter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

/** DataTable projection of a registered tool. Schema details are loaded when the editor opens. */
@Getter
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "datatable.tab.basic", selected = true),
    @DataTableTab(id = "input", title = "components.ai_assistants.tools.arguments"),
    @DataTableTab(id = "output", title = "components.ai_assistants.tools.response")
})
public class AiToolEntity {

    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "components.ai_assistants.tools.name", tab = "basic", disabled = true)
    private final String name;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.ai_assistants.tools.description", tab = "basic", className = "show-html", disabled = true)
    private final String description;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.ai_assistants.tools.http_method", tab = "basic", disabled = true)
    private final String httpMethod;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.tools.path", tab = "basic", className = "dt-style-text-wrap show-html", disabled = true)
    private final String path;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.tools.call", tab = "basic", hidden = true, disabled = true)
    private String call;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.ai_assistants.tools.conditions", tab = "basic", hidden = true, disabled = true)
    private String conditions;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.ai_assistants.tools.required", tab = "input", hidden = true, disabled = true)
    private String required;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.ai_assistants.tools.input_schema", tab = "input",
        hidden = true, ai = false, className = "wrap",
        editor = {
            @DataTableColumnEditor(type = "textarea", attr = {
                @DataTableColumnEditorAttr(key = "class", value = "textarea-code"),
                @DataTableColumnEditorAttr(key = "readonly", value = "readonly"),
                @DataTableColumnEditorAttr(key = "entityDecode", value = "false")
            })
        })
    private String inputSchema;

    private String example;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.ai_assistants.tools.response", tab = "output",
        hidden = true, ai = false, className = "wrap",
        editor = {
            @DataTableColumnEditor(type = "textarea", attr = {
                @DataTableColumnEditorAttr(key = "class", value = "textarea-code"),
                @DataTableColumnEditorAttr(key = "readonly", value = "readonly"),
                @DataTableColumnEditorAttr(key = "entityDecode", value = "false")
            })
        })
    private String response;

    private String responseExample;

    public AiToolEntity(AiToolDefinition tool) {
        name = tool.name();
        description = tool.description();
        httpMethod = tool.httpMethod();
        path = tool.path();
    }

    public static AiToolEntity detail(AiToolDefinition tool, Prop prop) {
        AiToolEntity entity = new AiToolEntity(tool);
        entity.call = "POST /admin/rest/ai/tools/" + tool.name() + "/call";
        if (!tool.conditions().isEmpty()) entity.conditions = Json.pretty(tool.conditions());
        if (tool.inputSchema().get("required") instanceof List<?> names) {
            entity.required = String.join(", ", names.stream().map(Object::toString).toList());
        }
        entity.example = AiToolExample.forInput(tool.inputSchema());
        entity.inputSchema = Json.pretty(tool.inputSchema());
        if (tool.outputSchema() == null) {
            entity.response = prop.getText("components.ai_assistants.tools.response_" + tool.responseType());
        } else {
            entity.response = Json.pretty(tool.outputSchema());
            entity.responseExample = AiToolExample.forResponse(tool.outputSchema());
        }
        return entity;
    }
}
