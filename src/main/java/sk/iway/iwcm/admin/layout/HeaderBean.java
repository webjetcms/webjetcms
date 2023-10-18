package sk.iway.iwcm.admin.layout;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * HeaderBean - data pre hlavicku
 */
@Getter
@Setter
public class HeaderBean {

    private List<String> domains;
    private String currentDomain;

}