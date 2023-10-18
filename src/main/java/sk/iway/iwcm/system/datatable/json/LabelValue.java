package sk.iway.iwcm.system.datatable.json;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

/**
 * Jednoducha trieda pre generovanie:
 *   options: [
 *     {label: "ano", value: true}
 *   ]
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LabelValue {
    private String label;
    private String value;
}