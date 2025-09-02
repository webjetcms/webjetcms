package sk.iway.iwcm.system.datatable.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class DataTableAi {
    private String from;
    private String to;
    private Long assistantId;
    private String provider;
    private String providerTitle;
    private String description;
    private String instructions;
    private boolean useStreaming;
    private String action;
    private String groupName;
    private boolean userPromptEnabled;
    private String userPromptLabel;
    private String icon;

    public DataTableAi() {}

    @JsonIgnore
    public boolean isEmpty() {
        //Check that all required fields are set
        if (Tools.isEmpty(to) || assistantId == null) return true;

        //Check that required fields do NOT have "EMPTY_VALUE" what means it was not set
        String emptyValue = AiAssistantsService.EMPTY_VALUE;
        if( emptyValue.equals(to)) return true;

        //Its good
        return false;
    }
}
