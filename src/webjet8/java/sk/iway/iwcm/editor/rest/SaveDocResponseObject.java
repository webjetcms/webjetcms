package sk.iway.iwcm.editor.rest;

import java.util.List;

public class SaveDocResponseObject {
    private boolean ajaxSaveFormRefreshLeft;
    private int ajaxSaveFormDocId;
    private String ajaxSaveFormVirtualPath;
    private boolean ajaxSaveFormAvailable;
    private int ajaxSaveFormLastDocId;
    private boolean ajaxSaveFormPermDenied;
    private List<String> ajaxSaveFormWarnings;

    public boolean isAjaxSaveFormRefreshLeft() {
        return ajaxSaveFormRefreshLeft;
    }

    public void setAjaxSaveFormRefreshLeft(boolean ajaxSaveFormRefreshLeft) {
        this.ajaxSaveFormRefreshLeft = ajaxSaveFormRefreshLeft;
    }

    public int getAjaxSaveFormDocId() {
        return ajaxSaveFormDocId;
    }

    public void setAjaxSaveFormDocId(int ajaxSaveFormDocId) {
        this.ajaxSaveFormDocId = ajaxSaveFormDocId;
    }

    public String getAjaxSaveFormVirtualPath() {
        return ajaxSaveFormVirtualPath;
    }

    public void setAjaxSaveFormVirtualPath(String ajaxSaveFormVirtualPath) {
        this.ajaxSaveFormVirtualPath = ajaxSaveFormVirtualPath;
    }

    public boolean isAjaxSaveFormAvailable() {
        return ajaxSaveFormAvailable;
    }

    public void setAjaxSaveFormAvailable(boolean ajaxSaveFormAvailable) {
        this.ajaxSaveFormAvailable = ajaxSaveFormAvailable;
    }

    public int getAjaxSaveFormLastDocId() {
        return ajaxSaveFormLastDocId;
    }

    public void setAjaxSaveFormLastDocId(int ajaxSaveFormLastDocId) {
        this.ajaxSaveFormLastDocId = ajaxSaveFormLastDocId;
    }

    public boolean isAjaxSaveFormPermDenied() {
        return ajaxSaveFormPermDenied;
    }

    public void setAjaxSaveFormPermDenied(boolean ajaxSaveFormPermDenied) {
        this.ajaxSaveFormPermDenied = ajaxSaveFormPermDenied;
    }

    public List<String> getAjaxSaveFormWarnings() {
        return ajaxSaveFormWarnings;
    }

    public void setAjaxSaveFormWarnings(List<String> ajaxSaveFormWarnings) {
        this.ajaxSaveFormWarnings = ajaxSaveFormWarnings;
    }
}