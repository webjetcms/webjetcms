package sk.iway.iwcm.doc.ninja;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

public class Webjet {
    private Ninja ninja;
    private String installName;

    public Webjet(Ninja ninja) {
        this.ninja = ninja;
    }

    public String getInstallName(){
        if (installName == null) {
            installName = ninja.getConfig("installName", Constants.getInstallName());
        }
        return installName;
    }

    public String getInsertJqueryHtml() {
        return Tools.insertJQuery(ninja.getRequest());
    }

    public String getInsertJqueryFake() {
        Tools.insertJQuery(ninja.getRequest());
        return "";
    }

    public String getPageFunctionsPath() {
        return "/components/_common/javascript/page_functions.js.jsp";
    }

    public String getGeneratedTime() {
        return Tools.formatDateTimeSeconds(Tools.getNow());
    }

}
