package sk.iway.iwcm.components.forms;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Objekt pre zoznam stlpcov formulara
 */
@Getter
@Setter
public class FormColumns {
    private int count = 0;
    private List<LabelValue> columns;
}
