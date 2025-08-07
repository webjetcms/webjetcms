package sk.iway.iwcm.system.datatable.json;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class DataTableAi {
    private String from;
    private String to;
    private String assistant;

    public DataTableAi() {}

    public DataTableAi(Field field) {
        setPropertiesFromField(field);
    }

    public DataTableAi(String from, String to, String assistant) {
        this.from = from;
        this.to = to;
        this.assistant = assistant;
    }

    public void setPropertiesFromField(Field field) {
        String pes = "";

        sk.iway.iwcm.system.datatable.annotations.DataTableColumn annotation = field.getAnnotation(sk.iway.iwcm.system.datatable.annotations.DataTableColumn.class);
        sk.iway.iwcm.system.datatable.annotations.DataTableAi[] ai = annotation.ai();

        if (annotation == null || ai == null || ai.length == 0) {
            return;
        }

        String from = ai[0].from();
        if(Tools.isNotEmpty(from))  {
            this.from = from;
        }

        String to = ai[0].to();
        if(Tools.isNotEmpty(to)) {
            this.to = to;
        } else {
            this.to = this.from;
        }

        String assistant = ai[0].assistant();
        if(Tools.isNotEmpty(assistant)) {
            this.assistant = assistant;
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return Tools.isEmpty(from) && Tools.isEmpty(to) && Tools.isEmpty(assistant);
    }
}
