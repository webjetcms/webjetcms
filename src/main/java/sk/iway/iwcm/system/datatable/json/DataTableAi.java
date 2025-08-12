package sk.iway.iwcm.system.datatable.json;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.kokos.OpenAiAssistantsService;
import sk.iway.iwcm.kokos.OpenAiSupportService;
import sk.iway.iwcm.utils.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class DataTableAi {
    private String from;
    private String to;
    private String assistant;

    public DataTableAi() {}

    public DataTableAi(String from, String to, String assistant) {
        this.from = from;
        this.to = to;
        this.assistant = assistant;
    }

    public void setProperties(Class controller, Field field) {
        try {
            String toField = field.getName();

            Pair<String, String> kk = OpenAiAssistantsService.getAssistantAndFieldFrom(toField, controller.getName());
            if(kk != null) {
                this.assistant = kk.getFirst();
                this.from = kk.getSecond();
                this.to = toField;
            }
        } catch (Exception e) {
            Logger.error(DataTableAi.class, "Error setting properties", e);
        }
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
