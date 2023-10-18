package sk.iway.iwcm.admin.xls.exception;

import java.util.ArrayList;
import java.util.List;

public class ExcelImportServiceException extends RuntimeException {
    List<String> errors;

    public ExcelImportServiceException() {
    }

    public ExcelImportServiceException(String message) {
        addError(message);
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }

        this.errors.add(error);
    }
}

