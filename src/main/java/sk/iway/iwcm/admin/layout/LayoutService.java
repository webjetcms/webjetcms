package sk.iway.iwcm.admin.layout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.*;
import sk.iway.iwcm.admin.AdminPropRestController;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UsersDB;

/**
 * LayoutService - sluzby pre zakladny layout WJ
 * Primarne do modelu pre Thymeleaf nastavi potrebne data
 */
public class LayoutService
{
    private HttpServletRequest request;
    private Identity user;
    LayoutBean layout;

    public LayoutService(HttpServletRequest request) {
        this.request = request;
        user = UsersDB.getCurrentUser(request);
    }

    public LayoutBean getLayoutBean() {
        if (layout == null && user != null) {
            layout = new LayoutBean();
            HeaderBean header = new HeaderBean();
            layout.setHeader(header);

            setGlobalData();
            setHeaderData();
            setMenuData();

        }
        return layout;
    }

    public LayoutService setGlobalData() {
        String brand = InitServlet.getBrandSuffix();
        //if (request.getParameter("brand")!=null) brand = request.getParameter("brand");
        layout.setBrand(brand);
        layout.setVersion(InitServlet.getActualVersionLong());
        setUser();
        String lng = Prop.getLngForJavascript(request);
        layout.setLng(lng);
        String lngWebjet = lng;
        if ("cs".equals(lngWebjet)) lngWebjet = "cz";
        layout.setLngWebjet(lngWebjet);
        layout.setCsrfToken(CSRF.getCsrfToken(request.getSession(), true));
        layout.setPropertiesLastModified(AdminPropRestController.getLastModified(layout.getLngWebjet()));
        setNopermsCss();
        return this;
    }

    public LayoutService setHeaderData() {
        setDomainNameSelect();
        return this;
    }

    public LayoutService setMenuData() {
        MenuService menuService = new MenuService(request);
        layout.setMenu(menuService.getMenu());
        return this;
    }


    /************************* GLOBAL *************************/

    private void setUser()
    {
        layout.setUser(user);
    }

    private void setNopermsCss() {
        StringBuilder css = new StringBuilder();
        StringBuilder javascript = new StringBuilder("var nopermsJavascript = new Array();\n");
        if (user != null && user.getDisabledItemsTable() != null && user.isAdmin()) {
            try {
                Iterator<String> e = user.getDisabledItemsTable().keySet().iterator();
                String name;
                while (e.hasNext()) {
                    name = e.next();
                    name = Tools.replace(name, ".", "_");
                    css.append(".noperms-").append(name).append(" { display: none !important; }\n");
                    javascript.append("nopermsJavascript[\"").append(name).append("\"]=true;\n");
                }
                if ("B".equals(Constants.getString("wjVersion"))) {
                    css.append(".noperms-ver-bas { display: none !important; }\n");
                    javascript.append("nopermsJavascript[\"ver-bas\"]=true;\n");
                } else if ("P".equals(Constants.getString("wjVersion"))) {
                    css.append(".noperms-ver-pro { display: none !important; }\n");
                    javascript.append("nopermsJavascript[\"ver-pro\"]=true;\n");
                }
            } catch (Exception ex) {
                Logger.error(LayoutService.class, ex);
            }
        }
        javascript.append("window.nopermsJavascript=nopermsJavascript;\n");
        layout.setNopermsCss(css.toString());
        layout.setNopermsJavascript(javascript.toString());
    }

    /************************* HEADER *************************/

    /**
     * Pripravi data pre select box vyberu aktualnej domeny
     */
    private void setDomainNameSelect()
    {
        List<String> userDomains;
        String currentDomain = CloudToolsForCore.getDomainName();
        //tu pouzijeme multiDomainEnabled a nie enableStaticFilesExternalDir aby sa vyberove menu zobrazovalo aj v multidomain prostredi pre filtrovanie vo web strankach
        if (InitServlet.isTypeCloud()==false && Constants.getBoolean("multiDomainEnabled"))
        {
            userDomains = GroupsDB.getInstance().getUserRootDomainNames(user);
        }
        else
        {
            userDomains = new ArrayList<>();
            userDomains.add(currentDomain);
        }

        if (Constants.getBoolean("multiDomainEnabled") && userDomains.isEmpty()==false) {
            //over, ze currentDomain je v zozname, ak nie, setni ako current prvu v zozname
            boolean found = false;
            for (String domainName : userDomains) {
                if (domainName.equals(currentDomain)) {
                    found = true;
                    break;
                }
            }
            if (found==false) {
                String firstDomain = userDomains.get(0);
                if (Tools.isNotEmpty(firstDomain)) {
                    //musime setnut domenu do session
                    request.getSession().setAttribute("preview.editorDomainName", firstDomain);
                    RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
                    if (rb != null) {
                        rb.setDomain(firstDomain);
                    }
                }
            }
        }

        layout.getHeader().setDomains(userDomains);
        layout.getHeader().setCurrentDomain(currentDomain);
    }

    /**
     * Vrati zoznam dostupnych jazykov, prvy v poradi bude jazyk podla konf. premennej defaultLanguage
     * @param addFirstEmpty - ak je true, prida na zaciatok prazdnu hodnotu
     * @param translateValue - ak je true prelozi aj hodnotu (namiesto sk vypise Slovensky)
     * @return
     */
    public List<LabelValueDetails> getLanguages(boolean addFirstEmpty, boolean translateValue) {
        return getLanguages(addFirstEmpty, translateValue, null);
    }

    /**
     * Vrati zoznam dostupnych jazykov, prvy v poradi bude jazyk podla konf. premennej defaultLanguage
     * @param addFirstEmpty - ak je true, prida na zaciatok prazdnu hodnotu
     * @param translateValue - ak je true prelozi aj hodnotu (namiesto sk vypise Slovensky)
     * @param appendTextKey - textovy kluc, ktoreho text sa prida za hodnotu (value)
     * @return
     */
    public List<LabelValueDetails> getLanguages(boolean addFirstEmpty, boolean translateValue, String appendTextKey) {
        List<LabelValueDetails> languages = new ArrayList<>();

        String[] lngArr = Constants.getArray("languages");
        String defaultLang = Constants.getString("defaultLanguage");
        Prop prop = Prop.getInstance(request);

        String appendText = null;
        if (Tools.isNotEmpty(appendTextKey)) appendText = prop.getText(appendTextKey);

        for (String lng : lngArr) {
            LabelValueDetails lvd = new LabelValueDetails(lng, lng);
            if (translateValue) lvd.setLabel(prop.getText("language."+lng));
            if (appendText != null) lvd.setLabel(lvd.getLabel()+" "+appendText);
            if (lng.equals(defaultLang)) languages.add(0, lvd);
            else languages.add(lvd);
        }

        if (addFirstEmpty) languages.add(0, new LabelValueDetails("", ""));

        return languages;
    }

    /**
     * Vrati Locale objekt pre prihlaseneho pouzivatela (alebo predvoleny jazyk)
     * @param request
     * @return
     */
    public static Locale getUserLocale(HttpServletRequest request) {
        String lng = Prop.getLng(request, false);
        return getUserLocale(lng);
    }

    /**
     * Vrati Locale objekt pre zadany jazyk, riesi aj problem cz/cs
     * @param lng
     * @return
     */
    public static Locale getUserLocale(String lng) {
        String[] isoLocale = Tools.getTokens(PageLng.getUserLngIso(lng), "-");
        Locale loc;
        if (isoLocale.length==2) {
            loc = new Locale(isoLocale[0], isoLocale[1]);
        } else {
            loc = Locale.getDefault();
        }
        return loc;
    }
}
