package sk.iway.iwcm.doc.ninja;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;

public class Page {
    private Ninja ninja;
    private DocDetails doc;

    public Page(Ninja ninja) {
        this.ninja = ninja;

        // request nemoze byt null
        if (ninja.getRequest() == null) return;

        this.doc = (DocDetails) ninja.getRequest().getAttribute("docDetails");
    }

    public DocDetails getDoc(){
        return doc;
    }

    public String getSeoTitle() {
        String seoTitle = "";
        if(doc != null) {
            seoTitle = doc.getFieldR();
            if(Tools.isEmpty(seoTitle)){
                seoTitle = doc.getTitle();
            }
        }
        return seoTitle;
    }

    public String getSeoDescription(){
        String seoDesc = "";
        if(doc!=null){
            seoDesc = doc.getFieldS();
            if(Tools.isEmpty(seoDesc)){
                seoDesc = doc.getPerexPre();
            }
        }
        return Tools.html2text(seoDesc).replace("\"","");
    }

    public String getSeoImage(){
        String seoImage = "";
        if(doc!=null){
            seoImage = doc.getFieldT();
            //skontroluj, ze obsahuje / a ., inak to nie je obrazok, ale nejaky text
            if (seoImage.contains("/")==false || seoImage.contains(".")==false) seoImage = "";
            if(Tools.isEmpty(seoImage)){
                seoImage = getStringValue(doc.getPerexImage(), ninja.getTemp().getBasePathImg() + ninja.getConfig("defaultSeoImage"));
            }
        }
        return seoImage;
    }

    public String getStringValue(String value, String defaultValue)
    {
        String ret = defaultValue;
        if(Tools.isNotEmpty(value)) ret = value;
        return(ret);
    }

    public String getRobots(){
        String robots = "";
        if(doc!=null){
            robots = doc.isSearchable() ? "index, follow" :  "noindex, follow";
        }
        return robots;
    }

    public String getUrl(){
        return getUrlDomain() +""+ getUrlPath();
    }

    public String getUrlDomain(){
        return Tools.getBaseHref(ninja.getRequest());
    }

    public String getUrlPath(){
        return PathFilter.getOrigPath(ninja.getRequest());
    }

    /**
     * Returns URL address with Query String (if exists)
     * @return
     */
    public String getUrlPathQString(){
        String path = getUrlPath();
        String qString = (String)ninja.getRequest().getAttribute("path_filter_query_string");
        if (qString != null) path += "?" + qString;
        return path;
    }

    public Map<String, String[]> getUrlParameters() {
        Optional<HttpServletRequest> requestOptional = Optional.ofNullable(ninja.getRequest());
        return requestOptional.isPresent() ? requestOptional.get().getParameterMap() : Collections.emptyMap();
    }

    public String getTitle() {
        return ninja.replaceNbspSingleChar(doc.getTitle());
    }

    public String getPerex() {
        return ninja.replaceNbspSingleChar(doc.getPerex());
    }

    public String getPerexPlace() {
        return ninja.replaceNbspSingleChar(doc.getPerexPlace());
    }
}
