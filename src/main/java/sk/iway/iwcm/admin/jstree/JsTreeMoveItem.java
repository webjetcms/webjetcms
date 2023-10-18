package sk.iway.iwcm.admin.jstree;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsTreeMoveItem {
    /**
     * ID pre web stranky
     */
    private String id;

    /**
     * URL pre galeriu
     */
    private String url;

    /**
     * Povodny rodic
     */
    @JsonProperty("old_parent")
    private String oldParent;

    /**
     * Novy rodic
     */
    private String parent;

    /**
     * Povodna pozicia
     */
    @JsonProperty("old_position")
    private int oldPosition;

    /**
     * Nova pozicia
     */
    private int position;

    /**
     * Upraveny item JS, ak potrebujeme povodne vrateny, tento objekt obsahuje original
     */
    private JsTreeItem node;

    public int getIdInt() {
        return Tools.getIntValue(id, 0);
    }
}
