package sk.iway.iwcm.components.customfields.rest;

import java.util.List;
import java.util.stream.Collectors;

import sk.iway.iwcm.system.datatable.DatatableFieldError;
import sk.iway.iwcm.system.datatable.EditorException;

/**
 * Carries custom-field validation errors through REST and import save boundaries.
 */
public class CustomFieldsValidationException extends EditorException {

    private static final long serialVersionUID = 1L;
    private final List<DatatableFieldError> fieldErrors;

    public CustomFieldsValidationException(List<DatatableFieldError> fieldErrors) {
        super(fieldErrors.stream().map(error -> error.getName() + ": " + error.getStatus()).collect(Collectors.joining("; ")), null);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<DatatableFieldError> getFieldErrors() {
        return fieldErrors;
    }
}
