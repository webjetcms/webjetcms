package sk.iway.iwcm.common;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Identity;

/**
 * Event data holder for DocTools.updateCodes() method.
 * Contains all parameters needed for custom code processing.
 * The text field is mutable so listeners can modify the output.
 */
public class UpdateCodesEvent {

    private StringBuilder text;
    private Identity user;
    private int currentDocId;
    private HttpServletRequest request;

    public UpdateCodesEvent(StringBuilder text, Identity user, int currentDocId, HttpServletRequest request) {
        this.text = text;
        this.user = user;
        this.currentDocId = currentDocId;
        this.request = request;
    }

    /**
     * Get the text being processed. This can be modified by listeners.
     * @return mutable StringBuilder with the text
     */
    public StringBuilder getText() {
        return text;
    }

    public void setText(StringBuilder text) {
        this.text = text;
    }

    public Identity getUser() {
        return user;
    }

    public void setUser(Identity user) {
        this.user = user;
    }

    public int getCurrentDocId() {
        return currentDocId;
    }

    public void setCurrentDocId(int currentDocId) {
        this.currentDocId = currentDocId;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
}
