package sk.iway.iwcm.system.datatable;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotifyBean {

    //title and tzpe are required
    private String title;
    private String type;

    private String text;
    private long timeout = 0;

    private List<NotifyButton> buttons;

    public enum NotifyType {
        SUCCESS,
        INFO,
        WARNING,
        ERROR
    }

    public NotifyBean(String title, String text, NotifyType type) {
        this.title = title;
        setType(type);
        this.text = text;
    }

    /**
     * Prida novu notifikaciu
     * @param title
     * @param text
     * @param type
     * @param timeout - pocet ms po ktorych sa notifikacia schova
     */
    public NotifyBean(String title, String text, NotifyType type, long timeout) {
        this.title = title;
        setType(type);
        this.text = text;
        this.timeout = timeout;
    }

    // Set type using string
    public void setType(String type) {
        this.type = type;
    }

    //Set type using create notifyType enum
    public void setType(NotifyType type) {
        if(type == NotifyType.SUCCESS) {
            this.type = "success";
        } else if(type == NotifyType.INFO) {
            this.type = "info";
        } else if(type == NotifyType.WARNING) {
            this.type = "warning";
        } else if(type == NotifyType.ERROR) {
            this.type = "error";
        }
    }

    /**
     * Prida tlacidlo do notifikacie
     * @param button
     * @return
     */
    public NotifyBean addButton(NotifyButton button) {
        if (buttons == null) buttons = new ArrayList<>();
        buttons.add(button);
        return this;
    }
}
