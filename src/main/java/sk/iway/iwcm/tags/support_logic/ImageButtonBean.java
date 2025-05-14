package sk.iway.iwcm.tags.support_logic;

import java.io.Serializable;

public class ImageButtonBean implements Serializable {

    private String x;
    private String y;

    public ImageButtonBean() {
        // do nothing
    }

    public ImageButtonBean(String x, String y) {
        this.x = x;
        this.y = y;
    }

    public String getX() {
        return (this.x);
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return (this.y);
    }

    public void setY(String y) {
        this.y = y;
    }

    public boolean isSelected() {
        return ((x != null) || (y != null));
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ImageButtonBean[");

        sb.append(this.x);
        sb.append(", ");
        sb.append(this.y);
        sb.append("]");

        return (sb.toString());
    }
}