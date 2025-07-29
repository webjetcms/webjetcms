package sk.iway.iwcm.system.datatable.json;

import org.json.JSONObject;

import sk.iway.iwcm.Tools;

public class IdFullPath {
    Integer id;
    String fullPath;

    public IdFullPath() {}

    public IdFullPath(String value) {
        if (Tools.isEmpty(value) == true) {
            this.id = null;
            this.fullPath = null;
        }

        try {
            this.id = Integer.parseInt(value);
            this.fullPath = null;
        } catch (NumberFormatException e) {
            this.id = null;
            this.fullPath = null;
        }
    }

    public IdFullPath(Integer id, String fullPath) {
        this.id = id;
        this.fullPath = fullPath;
    }

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getFullPath() {
        return fullPath;
    }
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    private int getValidId() {
        if(id == null || id < 1) return -1;
        else return id;
    }

    private String getValidFullPath() {
        return fullPath == null ? "" : fullPath;
    }

    @Override
    public String toString() {
        JSONObject thisObject = new JSONObject();
        thisObject.put("id", getValidId());
        thisObject.put("fullPath", getValidFullPath());
        return thisObject.toString();
    }
}