package sk.iway.iwcm.doc.ninja;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.doc.TemplatesGroupDB;

public class TempGroup {
    private Ninja ninja;
    private TemplatesGroupBean templatesGroupBean;
    private String iwayPropertiesPrefix = "temp-group-";
    @SuppressWarnings("unused")
    private String textPrefix;

    public TempGroup(Ninja ninja) {
        this.ninja = ninja;

        setDefaults();
    }

    private void setDefaults() {
        //this.iwayProperties = Prop.getChangedProperties(PageLng.getUserLng(ninja.getRequest()), iwayPropertiesPrefix);
        getTemplatesGroupBean();
        if (templatesGroupBean != null) {
            this.iwayPropertiesPrefix = "temp-group-" + templatesGroupBean.getTemplatesGroupId() + ".";
            this.textPrefix = templatesGroupBean.getKeyPrefix();
        }
    }

    private String controlEmptyTextKey(String textKey){
        if (textKey.startsWith(getIwayPropertiesPrefix()))
            return "";
        return textKey;
    }

    public TemplatesGroupBean getTemplatesGroupBean() {
        if (templatesGroupBean == null && ninja.getPage().getDoc() != null) {
            templatesGroupBean = TemplatesGroupDB.getInstance().getById(TemplatesDB.getInstance().getTemplate(ninja.getPage().getDoc().getTempId()).getTemplatesGroupId());
        }
        return templatesGroupBean;
    }

    public String getAuthor(){
        return Tools.html2text(controlEmptyTextKey(ninja.getProp().getText(getIwayPropertiesPrefix() + "project.author")));
    }

    public String getDeveloper(){
        return Tools.html2text(controlEmptyTextKey(ninja.getProp().getText(getIwayPropertiesPrefix() + "project.developer")));
    }

    public String getGenerator(){
        return Tools.html2text(controlEmptyTextKey(ninja.getProp().getText(getIwayPropertiesPrefix() + "project.generator")));
    }

    public String getFieldA() {
        return Tools.html2text(controlEmptyTextKey(ninja.getProp().getText(getIwayPropertiesPrefix() + "project.field.a")));
    }

    public String getFieldB() {
        return Tools.html2text(controlEmptyTextKey(ninja.getProp().getText(getIwayPropertiesPrefix() + "project.field.b")));
    }

    public String getFieldC() {
        return Tools.html2text(controlEmptyTextKey(ninja.getProp().getText(getIwayPropertiesPrefix() + "project.field.c")));
    }

    public String getFieldD() {
        return Tools.html2text(controlEmptyTextKey(ninja.getProp().getText(getIwayPropertiesPrefix() + "project.field.d")));
    }

    public String getCopyright(){
        return Tools.html2text(controlEmptyTextKey(ninja.getProp().getText(getIwayPropertiesPrefix() + "project.copyright")));
    }

    public String getTextPrefix() {
        return getTextPrefix();
    }

    public String getSiteName(){
        return controlEmptyTextKey(ninja.getProp().getText(getIwayPropertiesPrefix() + "project.name"));
    }

    private String getIwayPropertiesPrefix() {
        return iwayPropertiesPrefix;
    }

    public void setIwayPropertiesPrefix(String iwayPropertiesPrefix) {
        this.iwayPropertiesPrefix = iwayPropertiesPrefix;
    }

    public void setTextPrefix(String textPrefix) {
        this.textPrefix = textPrefix;
    }
}