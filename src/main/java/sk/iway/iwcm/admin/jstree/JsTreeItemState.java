package sk.iway.iwcm.admin.jstree;

import lombok.Getter;
import lombok.Setter;

/**
 * Mozne stavy polozky
 */
@Getter
@Setter
public class JsTreeItemState {
    private boolean opened; // is the node open
    private boolean disabled; // is the node disabled
    private boolean selected; // is the node selected
    private boolean loaded = true;
    private boolean failed;
    private boolean loading;
}