package sk.iway.iwcm.doc.ninja;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.GalleryDBTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.tags.support.ResponseUtils;

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
        String seoTitle = getSeoTitleHtml();
        //also remove HTML if present in title
        seoTitle = Tools.html2text(seoTitle);
        return seoTitle;
    }

    public String getCanonical(){
        String canonical = null;
        if(doc!=null){
            if (doc.getFieldQ()!=null && doc.getFieldQ().contains("/")) {
                canonical = ResponseUtils.filter(doc.getFieldQ());
                canonical = Tools.replace(canonical, "&amp;", "&");
            }

            DocDB docDB = DocDB.getInstance();
            String docLink = docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), true, ninja.getRequest());
            if(Tools.isEmpty(canonical)){
                canonical = docLink;
            } else {
                //if canonical is not empty, check if it contains http:// or https://, if not, add domain to it
                if (canonical != null && canonical.toLowerCase().startsWith("http") == false) {
                    //reuse domain from docLink, but only domain, without path
                    int i = docLink.indexOf("/", 10);
                    String domain = null;
                    if (i > 0 && i < docLink.length() - 1) domain = docLink.substring(0, i);
                    else domain = getUrlDomain();

                    canonical = domain + canonical;
                }
            }
        }

        if (Tools.isEmpty(canonical) || "-".equals(canonical)) canonical = getUrl();

        Map<String, String[]> params = getUrlParameters();
        String[] pageParams = params.get("page");
        if(pageParams != null && pageParams.length > 0 && Tools.isNotEmpty(canonical)){
            canonical = Tools.addParameterToUrl(canonical, "page", pageParams[0]);
        }

        return canonical;
    }

    public String getSeoTitleHtml() {
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

    /**
     * Returns the width of the SEO image in pixels.
     *
     * @return image width, or 0 if the image is missing or cannot be read
     */
    public int getSeoImageWidth() {
        String seoImage = getSeoImage();
        if (Tools.isEmpty(seoImage) || !FileTools.isFile(seoImage)) return 0;

        int[] dimensions = GalleryDBTools.getImageSize(new IwcmFile(seoImage));
        return dimensions != null && dimensions.length > 0 ? dimensions[0] : 0;
    }

    /**
     * Returns the height of the SEO image in pixels.
     *
     * @return image height, or 0 if the image is missing or cannot be read
     */
    public int getSeoImageHeight() {
        String seoImage = getSeoImage();
        if (Tools.isEmpty(seoImage) || !FileTools.isFile(seoImage)) return 0;

        int[] dimensions = GalleryDBTools.getImageSize(new IwcmFile(seoImage));
        return dimensions != null && dimensions.length > 1 ? dimensions[1] : 0;
    }

    public String getStringValue(String value, String defaultValue)
    {
        String ret = defaultValue;
        if(Tools.isNotEmpty(value)) ret = value;
        return(ret);
    }

    public String getRobots(){
        return PathFilter.getXRobotsTagValue(doc);
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
