package sk.iway.iwcm.system.datatable.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.openai.OpenAiSupportService;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class DataTableAi {
    private String from;
    private String to;
    private String assistant;
    private String provider;

    public DataTableAi() {}

    public DataTableAi(String from, String to, String assistant) {
        this.from = from;
        this.to = to;
        this.assistant = assistant;
    }

    @JsonIgnore
    public boolean isEmpty() {
        //Check that all required fields are set
        if (Tools.isEmpty(from) || Tools.isEmpty(to) || Tools.isEmpty(assistant)) return true;

        //Check that required fields do NOT have "EMPTY_VALUE" what means it was not set
        String emptyValue = OpenAiSupportService.EMPTY_VALUE;
        if( emptyValue.equals(from) || emptyValue.equals(to) || emptyValue.equals(assistant) ) return true;

        //Its good
        return false;
    }
}
