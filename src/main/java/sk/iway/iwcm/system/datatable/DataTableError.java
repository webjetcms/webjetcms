package sk.iway.iwcm.system.datatable;

import org.springframework.validation.FieldError;

public class DataTableError {
    private String name;
    private String status;

    public DataTableError(FieldError error) {
        this.name = error.getField();
        this.status = error.getDefaultMessage();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
