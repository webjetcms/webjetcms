package sk.iway.iwcm.grideditor.generator;

import java.util.ArrayList;
import java.util.List;

/**Sluzi ako zoznam parametrov pre Phantom / ScreenShotGenerator
 *
 * povinny je iba jeden parameter a to screenshotUrl
 *
 */
public class ScreenShotPropBean {
    private String screenshotUrl;
    private String saveImageToPath;
    private int zoom;
    private int screenShotDelayMilisecond;
    private int imageWidth;
    private int imageHeight;
    private List<String> errors;
    private List<String> phantomErrors;
    private int resultNumber;
    private String cookieHtmlData;
    private boolean debug;
    private boolean autoHeigth;

    public ScreenShotPropBean(String screenshotUrl)
    {
        this.screenshotUrl = screenshotUrl;
        zoom = 1;
        screenShotDelayMilisecond = 3000;
        imageWidth = 1920;
        imageHeight = 1080;
        resultNumber = - 10;
        errors = new ArrayList<>();
        phantomErrors = new ArrayList<>();
        debug = false;
    }

    public String getCookieHtmlData() {
        return cookieHtmlData;
    }

    public void setCookieHtmlData(String cookieHtmlData) {
        this.cookieHtmlData = cookieHtmlData;
    }

    public int getScreenShotDelayMilisecond() {
        return screenShotDelayMilisecond;
    }

    public void setScreenShotDelayMilisecond(int screenShotDelayMilisecond) {
        this.screenShotDelayMilisecond = screenShotDelayMilisecond;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getResultNumber() {
        return resultNumber;
    }

    public void setResultNumber(int resultNumber) {
        this.resultNumber = resultNumber;
    }

    public String getScreenshotUrl() {
        return screenshotUrl;
    }

    public void setScreenshotUrl(String screenshotUrl) {
        this.screenshotUrl = screenshotUrl;
    }

    public String getSaveImageToPath() {
        return saveImageToPath;
    }

    public void setSaveImageToPath(String saveImageToPath) {
        this.saveImageToPath = saveImageToPath;
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    /** Cas v milisekundach po ktorom sa vykona screenshot stranky od okamihu prveho nacitania stranky. Je to cas potrebny napr. na
     *  vykonanie javascriptov
     *
     * @return
     */
    public int getTimeDelayMilisecond() { //NOSONAR
        return screenShotDelayMilisecond;
    }

    /** Cas v milisekundach po ktorom sa vykona screenshot stranky od okamihu prveho nacitania stranky. Je to cas potrebny napr. na
     *  vykonanie javascriptov
     *
     * @param screenShotDelayMilisecond
     */
    public void setTimeDelayMilisecond(int screenShotDelayMilisecond) { //NOSONAR
        this.screenShotDelayMilisecond = screenShotDelayMilisecond;
    }

    /** Sirka generovaneho obrazku
     *
     * @return
     */
    public int getImageWidth() {
        return imageWidth;
    }

    /**Sirka generovaneho obrazku
     *
     * @param imageWidth
     */
    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    /** Vyska generovaneho obrazku
     *
     * @return
     */
    public int getImageHeight() {
        return imageHeight;
    }

    /** Vyska generovaneho obrazku
     *
     * @param imageHeight
     */
    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getPhantomErrors() {
        return phantomErrors;
    }

    public void setPhantomErrors(List<String> phantomErrors) {
        this.phantomErrors = phantomErrors;
    }

    public boolean isGenerated() {
        return resultNumber == 0;
    }

    public boolean isAutoHeigth() {
        return autoHeigth;
    }

    public void setAutoHeigth(boolean autoHeigth) {
        this.autoHeigth = autoHeigth;
    }
}
