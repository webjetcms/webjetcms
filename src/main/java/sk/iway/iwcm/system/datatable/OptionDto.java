package sk.iway.iwcm.system.datatable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO pre options hodnoty v datatabulke (hodnoty select boxov)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OptionDto {
    private String label;
    private String value;
    private Object original;
}