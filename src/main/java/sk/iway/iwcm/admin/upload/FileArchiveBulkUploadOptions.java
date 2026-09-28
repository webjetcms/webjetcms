package sk.iway.iwcm.admin.upload;

import java.beans.PropertyEditorSupport;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;
import sk.iway.iwcm.components.file_archiv.FileArchivatorEditorFields;

/**
 * Retains only supplied bulk-upload fields across upload chunks and binds them directly to the archive entity.
 * The same allowlist controls request binding and the fields displayed in the bulk-upload dialog.
 */
public final class FileArchiveBulkUploadOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    static final String PARAM_SAVE_LATER = "editorFields.saveLater";
    static final String PARAM_DATE_UPLOAD_LATER = "editorFields.dateUploadLater";
    static final String PARAM_EMAILS = "editorFields.emails";
    static final String ERROR_SAVE_LATER_REPLACE = "components.file_archiv.bulk_upload.error.save_later_replace";
    static final String ERROR_INVALID_ADVANCED_OPTIONS = "components.file_archiv.bulk_upload.error.invalid_advanced_options";
    static final String ERROR_INVALID_UPLOAD_DATE = "components.file_archiv.upload.upload_date_wrong";

    private static final List<String> ALLOWED_FIELDS = List.of(
        "validFrom", "validTo", PARAM_SAVE_LATER, PARAM_DATE_UPLOAD_LATER, PARAM_EMAILS,
        "product", "category", "productCode", "showFile", "indexFile", "priority", "referenceToMain", "note",
        "editorFields.uploadRedundantFile", "fieldA", "fieldB", "fieldC", "fieldD", "fieldE"
    );

    private final Map<String, String> values;
    private final String errorKey;

    private FileArchiveBulkUploadOptions(Map<String, String> values) {
        this.values = Map.copyOf(values);
        errorKey = bindTo(new FileArchivatorBean());
    }

    public static List<String> getAllowedFields() {
        return ALLOWED_FIELDS;
    }

    static FileArchiveBulkUploadOptions none() {
        return new FileArchiveBulkUploadOptions(Map.of());
    }

    /**
     * Collects supplied entity properties and validates their types before accepting upload chunks.
     * Legacy prefixed parameters remain supported for upload dialogs opened before an application update.
     *
     * @param request upload or conflict-resolution request
     * @return optional field values and any validation error
     */
    static FileArchiveBulkUploadOptions fromRequest(HttpServletRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : ALLOWED_FIELDS) {
            String value = request.getParameter(field);
            if (value == null) {
                String name = field.substring(field.lastIndexOf('.') + 1);
                value = request.getParameter("fileArchive" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
            }
            if (value != null && value.isBlank() == false) values.put(field, value.trim());
        }
        if ("true".equals(values.get(PARAM_SAVE_LATER)) == false) {
            values.remove(PARAM_DATE_UPLOAD_LATER);
            values.remove(PARAM_EMAILS);
        }
        return new FileArchiveBulkUploadOptions(values);
    }

    String getErrorKey() {
        return errorKey;
    }

    boolean isSaveLater() {
        return "true".equals(values.get(PARAM_SAVE_LATER));
    }

    /**
     * Binds supplied properties without copying entity defaults over existing metadata.
     * Validates the resulting validity interval and delayed-upload settings.
     *
     * @param entity new or existing archive entity receiving the supplied values
     * @return a localized error key, or {@code null} when the metadata is valid
     */
    String bindTo(FileArchivatorBean entity) {
        if (values.isEmpty()) return null;
        if (entity.getEditorFields() == null) entity.setEditorFields(new FileArchivatorEditorFields());
        DataBinder binder = new DataBinder(entity);
        registerEditor(binder, Date.class, value -> {
            long timestamp = Long.parseLong(value);
            if (timestamp < 0 || timestamp > 253402300799999L) throw new IllegalArgumentException("Invalid upload timestamp");
            return new Date(timestamp);
        });
        registerEditor(binder, Boolean.class, value -> {
            if ("true".equals(value)) return Boolean.TRUE;
            if ("false".equals(value)) return Boolean.FALSE;
            throw new IllegalArgumentException("Invalid upload boolean");
        });
        registerEditor(binder, Integer.class, Integer::valueOf);
        registerEditor(binder, Long.class, Long::valueOf);
        binder.bind(new MutablePropertyValues(values));
        if (binder.getBindingResult().hasErrors()) {
            return switch (binder.getBindingResult().getFieldError().getField()) {
                case "validFrom" -> "components.file_archiv.bulk_upload.error.invalid_valid_from";
                case "validTo" -> "components.file_archiv.bulk_upload.error.invalid_valid_to";
                case PARAM_SAVE_LATER -> "components.file_archiv.bulk_upload.error.invalid_save_later";
                case PARAM_DATE_UPLOAD_LATER -> ERROR_INVALID_UPLOAD_DATE;
                default -> ERROR_INVALID_ADVANCED_OPTIONS;
            };
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            int maxLength = "note".equals(entry.getKey()) || PARAM_EMAILS.equals(entry.getKey()) ? 1100 : 255;
            if (entry.getValue().length() > maxLength) {
                return PARAM_EMAILS.equals(entry.getKey()) ? "components.file_archiv.upload.emails_wrong" : ERROR_INVALID_ADVANCED_OPTIONS;
            }
        }
        if ((values.containsKey("validFrom") || values.containsKey("validTo"))
            && entity.getValidFrom() != null && entity.getValidTo() != null
            && entity.getValidFrom().after(entity.getValidTo())) {
            return "components.file_archiv.bulk_upload.error.invalid_validity_interval";
        }
        if (isSaveLater()) {
            Date uploadDate = entity.getEditorFields().getDateUploadLater();
            if (uploadDate == null || uploadDate.after(new Date()) == false) return ERROR_INVALID_UPLOAD_DATE;
            String emails = entity.getEditorFields().getEmails();
            if (Tools.isEmpty(emails)) return "components.file_archiv.upload.emails_wrong";
            for (String email : emails.split(",", -1)) {
                if (Tools.isEmail(email.trim()) == false) return "components.file_archiv.upload.emails_wrong";
            }
        }
        return null;
    }

    /**
     * Registers strict conversion by property type, without Spring's lenient fallback editors.
     *
     * @param binder entity binder
     * @param type property type
     * @param parser conversion of a supplied string to the property type
     */
    private static void registerEditor(DataBinder binder, Class<?> type, Function<String, ?> parser) {
        binder.registerCustomEditor(type, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(parser.apply(text));
            }
        });
    }
}
