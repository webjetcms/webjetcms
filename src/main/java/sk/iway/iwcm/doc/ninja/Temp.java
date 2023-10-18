package sk.iway.iwcm.doc.ninja;

import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.TemplatesGroupBean;

import java.util.ArrayList;
import java.util.List;

public class Temp {
    private Ninja ninja;
    private TempGroup group;
    private String basePath;
    private String basePathNoSuffix;
    private String templateFolderName;

    public Temp(Ninja ninja) {
        this.ninja = ninja;
        this.group = new TempGroup(ninja);
    }

    public String getBasePath(){
        if (basePath == null) {
            basePath = getBasePathNoSuffix();

            //aby sme vedeli na koniec pridat /dist
            String basePathSuffix = ninja.getConfig("basePathSuffix", null);
            if (Tools.isNotEmpty(basePathSuffix)) {
                basePath = basePath+basePathSuffix;
            }
        }

        return basePath;
    }

    public String getBasePathNoSuffix() {
        if (basePathNoSuffix == null) {
            if (ninja.getDoc() == null) {
                basePathNoSuffix = getBasePathFromUri(ninja.getRequest().getRequestURI());
            } else {

                //39796 - Constants.getInstallName() uz nepotrebujeme, pridava ho WJ automaticky
                basePathNoSuffix = "/templates/";
                String configInstallName = ninja.getConfig("installName", "");
                if (Tools.isNotEmpty(configInstallName))
                {
                    basePathNoSuffix += configInstallName + "/";
                }

                String templateFolderName = getTemplateFolderName();
                if (Tools.isNotEmpty(templateFolderName) && !("/".equals(templateFolderName))) {
                    basePathNoSuffix += templateFolderName + "/";
                }
            }
        }

        return basePathNoSuffix;
    }

    public String getTemplateFolderName() {
        if (templateFolderName == null) {
            templateFolderName = ninja.getConfig("templateFolderName", "");

            if (Tools.isEmpty(templateFolderName) && ninja.getPage().getDoc() != null) {
                TemplatesGroupBean templatesGroupBean = group.getTemplatesGroupBean();
                templateFolderName = templatesGroupBean.getDirectory();
            }
        }

        return templateFolderName;
    }

    private String getBasePathFromUri(String uri) {
        return uri.substring(0, uri.lastIndexOf("/") + 1);
    }

    public String getBasePathAssets(){
        return getBasePath()+"assets/";
    }

    public String getBasePathCss(){
        return getBasePath()+"assets/css/";
    }

    public String getBasePathJs(){
        return getBasePath()+"assets/js/";
    }

    public String getBasePathPlugins(){
        return getBasePath()+"assets/plugins/";
    }

    public String getBasePathImg(){
        return getBasePath()+"assets/images/";
    }

    public  String getInsertTouchIconsHtml() {
        int defaultDimension = 192;
        StringBuilder sb = new StringBuilder();

        List <Integer> dimensions = new ArrayList<>();
        dimensions.add(0);
        dimensions.add(72);
        dimensions.add(76);
        dimensions.add(114);
        dimensions.add(120);
        dimensions.add(144);
        dimensions.add(152);
        dimensions.add(180);

        for (Integer dimension : dimensions) {
            String sizes = "";
            if (dimension > 0) {
                sizes = dimension + "x" + dimension;
            }
            else {
                dimension = defaultDimension;
            }
            //ak uz tam nieco je odsad nech to je krajsie v HTML kode
            if (sb.length()>0) sb.append("        ");

            sb.append("<link rel=\"apple-touch-icon-precomposed\"");
            if (Tools.isNotEmpty(sizes)) {
                sb.append(" sizes=\"").append(sizes).append("\"");
            }
            sb.append(" href=\"/thumb").append(getBasePath()).append("assets/images/touch-icon.png?w=").append(dimension).append("&h=").append(dimension).append("&ip=5\" />\n");
        }

        sb.append("        <link rel=\"icon\" sizes=\"192x192\" href=\"/thumb").append(getBasePath()).append("assets/images/touch-icon.png?w=192&h=192&ip=5\" />\n");

        return sb.toString();
    }

    public String getCharset(){
        return Tools.getStringValue(ninja.getRequest().getParameter("SetCharacterEncodingFilter.encoding"), SetCharacterEncodingFilter.getEncoding());
    }

    public String getLngIso(){
        return PageLng.getUserLngIso(ninja.getRequest());
    }

    public TempGroup getGroup() {
        return group;
    }

    public String getBaseCssLink() {
        String link = (String)ninja.getRequest().getAttribute("base_css_link_nocombine");
        if ("/css/page.css".equals(link)) link = "";
        return link;
    }

    public String getCssLink() {
        return (String)ninja.getRequest().getAttribute("css_link_nocombine");
    }
}
