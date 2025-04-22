package sk.iway.iwcm.system.datatable;

import java.util.List;

public class EditorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<NotifyBean> notifyBeans;

    public final List<NotifyBean> getNotifyBeans() {
        return notifyBeans;
    }

    public EditorException(String message, List<NotifyBean> notifyBeans) {
        super(message);
        this.notifyBeans = notifyBeans;
    }

    public EditorException(String message, List<NotifyBean> notifyBeans, Throwable cause) {
        super(message, cause);
        this.notifyBeans = notifyBeans;
    }
}
