package sk.iway.iwcm.admin.layout;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * MenuBean - reprezentuje polozku v menu (sidebar.pug)
 */
@Getter
@Setter
@Accessors(chain = true)
public class MenuBean {

    private String href = "#";
    private String text;
    private String icon;
    private String group;
    private boolean active = false;
    private List<MenuBean> childrens;
    private boolean custom = false;

}