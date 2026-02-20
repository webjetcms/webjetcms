package sk.iway.iwcm.system.spring.webjet_component;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class WebjetErrors {
    private final List<WebjetError> errors;

    public WebjetErrors() {
        errors = new ArrayList<>();
    }

    public void addError(WebjetError error) {
        errors.add(error);
    }

    public WebjetErrors addError(String error) {
        WebjetError objectError = new WebjetError(error);
        errors.add(objectError);

        return this;
    }

    public List<WebjetError> getErrors() {
        return errors;
    }
}
