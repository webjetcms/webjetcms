package sk.iway.iwcm.admin.layout;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.datatable.DataTableColumnsFactory;
import sk.iway.iwcm.system.stripes.CSRF;

/**
 * LayoutBean - drzi atributy pre model
 */
@Getter
@Setter
public class LayoutBean {

    private Identity user;
    private HeaderBean header;
    //brand: cms, net, lms
    private String brand;
    //plna verzia WebJETu: Web JET admin verzia 8.7.979 18.03.2020 13:06:59 Enterprise
    private String version;
    private List<MenuBean> menu;
    private String lng;
    private String lngWebjet;
    private String csrfToken;
    private String nopermsCss;
    private String nopermsJavascript;
    //datum poslednej zmeny properties suborov pre efektivnejsie (ne)volanie REST sluzby pre ich aktualizaciu na FE
    private long propertiesLastModified;

    public String getConstant(String name)
    {
        return Constants.getString(name);
    }

    public String getConstant(String name, String defaultValue)
    {
        return Constants.getString(name, defaultValue);
    }

    public int getConstantInt(String name)
    {
        return Constants.getInt(name);
    }

    public int getConstantInt(String name, int defaultValue)
    {
        return Constants.getInt(name, defaultValue);
    }

    public boolean getConstantBoolean(String name)
    {
        return Constants.getBoolean(name);
    }

    public String getDataTableColumns(String className) throws JsonProcessingException {
        String json = new DataTableColumnsFactory(className).getColumnsJson();
        return json;
    }

    public String getUserDto() throws JsonProcessingException {
        return JsonTools.objectToJSON(new UserDto(getUser()));
    }

    /**
     * Overi prava pouzivatela na zadanu polozku
     * @param permission - kod prava (napr. cmp_crypto)
     * @return
     */
    public boolean hasPermission(String permission) {
        return user.isEnabledItem(permission);
    }

    /**
     * Vrati true, ak existuje viac ako 30 perex skupin pre ich zobrazenie ako multiselect
     * @return
     */
    public static boolean isPerexGroupsRenderAsSelect() {
        //ak existuje viac ako 30 perex skupin renderuj to ako multiselect
        int count = DocDB.getInstance().getPerexGroups().size();
        if (count > 30) return true;
        return false;
    }

    /**
     * Returns CSRF token parameter name
     * @return
     */
    public String getCsrfParameterName() {
        return CSRF.getParameterName();
    }
}