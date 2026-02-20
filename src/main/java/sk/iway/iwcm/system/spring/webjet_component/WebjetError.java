package sk.iway.iwcm.system.spring.webjet_component;

public class WebjetError {

    private String field;
    private String message;

    public WebjetError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public WebjetError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
