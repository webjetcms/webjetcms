package sk.iway.iwcm.headless.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single field-level validation error.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldError {

    private String field;
    private String message;

    public FieldError() {
    }

    public FieldError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
