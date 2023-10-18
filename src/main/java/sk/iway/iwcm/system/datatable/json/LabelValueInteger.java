package sk.iway.iwcm.system.datatable.json;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

/**
 * Jednoducha trieda pre generovanie:
 *   options: [
 *     {label: "ano", value: 36}
 *   ]
 */
@Getter
@Setter
@AllArgsConstructor
public class LabelValueInteger {
    private String label;
    private Integer value;
}