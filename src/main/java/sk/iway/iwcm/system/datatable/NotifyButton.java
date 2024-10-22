package sk.iway.iwcm.system.datatable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reprezentuje tlacidlo zobrazene v notifikacii
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotifyButton {

    private String title;
    private String cssClass;
    private String icon;
    private String click;

}
