package sk.iway.iwcm.admin.upload;

import java.io.Serializable;
import java.util.Date;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;

/**
 * Validated bulk action parameters attached to one file archive upload.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class FileArchiveBulkUploadOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    static final String PARAM_SAVE_LATER = "fileArchiveSaveLater";
    static final String PARAM_DATE_UPLOAD_LATER = "fileArchiveDateUploadLater";
    static final String PARAM_EMAILS = "fileArchiveEmails";
    static final String ERROR_SAVE_LATER_REPLACE = "components.file_archiv.bulk_upload.error.save_later_replace";
    static final String ERROR_INVALID_ADVANCED_OPTIONS = "components.file_archiv.bulk_upload.error.invalid_advanced_options";

    static final String ERROR_INVALID_UPLOAD_DATE = "components.file_archiv.upload.upload_date_wrong";
    static final int MAX_EMAILS_LENGTH = 1100;
    static final int MAX_TEXT_LENGTH = 255;
    static final int MAX_NOTE_LENGTH = 1100;
    private static final long MAX_DATE = 253402300799999L;

    private final Date validFrom;
    private final Date validTo;
    private final boolean saveLater;
    private final Date dateUploadLater;
    private final String emails;
    private final String product;
    private final String category;
    private final String productCode;
    private final Boolean showFile;
    private final Boolean indexFile;
    private final Integer priority;
    private final String referenceToMain;
    private final String note;
    private final Boolean uploadRedundantFile;
    private final String errorKey;

    static FileArchiveBulkUploadOptions none() {
        return new FileArchiveBulkUploadOptions(null, null, false, null, null, null, null, null,
            null, null, null, null, null, null, null);
    }

    /**
     * Parses and validates bulk metadata from an upload request.
     * Missing parameters retain the original upload behavior.
     * @param request upload request
     * @return validated options, including an error key for invalid input
     */
    static FileArchiveBulkUploadOptions fromRequest(HttpServletRequest request) {
        Date validationTime = new Date();
        try {
            Date validFrom = parseDate(request.getParameter("fileArchiveValidFrom"), "components.file_archiv.bulk_upload.error.invalid_valid_from");
            Date validTo = parseDate(request.getParameter("fileArchiveValidTo"), "components.file_archiv.bulk_upload.error.invalid_valid_to");
            String saveLaterValue = request.getParameter(PARAM_SAVE_LATER);
            boolean saveLater = "true".equals(saveLaterValue);
            if (Tools.isNotEmpty(saveLaterValue) && saveLater == false && "false".equals(saveLaterValue) == false) {
                return error("components.file_archiv.bulk_upload.error.invalid_save_later");
            }
            Date dateUploadLater = null;
            String emails = null;
            if (saveLater) {
                dateUploadLater = parseDate(request.getParameter(PARAM_DATE_UPLOAD_LATER), ERROR_INVALID_UPLOAD_DATE);
                if (dateUploadLater == null || dateUploadLater.after(validationTime) == false) {
                    return error(ERROR_INVALID_UPLOAD_DATE);
                }
                emails = request.getParameter(PARAM_EMAILS);
                if (areEmailsValid(emails) == false) return error("components.file_archiv.upload.emails_wrong");
            }

            String product = parseText(request.getParameter("fileArchiveProduct"), MAX_TEXT_LENGTH);
            String category = parseText(request.getParameter("fileArchiveCategory"), MAX_TEXT_LENGTH);
            String productCode = parseText(request.getParameter("fileArchiveProductCode"), MAX_TEXT_LENGTH);
            String referenceToMain = parseText(request.getParameter("fileArchiveReferenceToMain"), MAX_TEXT_LENGTH);
            String note = parseText(request.getParameter("fileArchiveNote"), MAX_NOTE_LENGTH);
            Boolean showFile = parseOptionalBoolean(trimToNull(request.getParameter("fileArchiveShowFile")));
            Boolean indexFile = parseOptionalBoolean(trimToNull(request.getParameter("fileArchiveIndexFile")));
            Boolean uploadRedundantFile = parseOptionalBoolean(trimToNull(request.getParameter("fileArchiveUploadRedundantFile")));
            String priorityValue = trimToNull(request.getParameter("fileArchivePriority"));
            Integer priority = priorityValue == null ? null : Integer.valueOf(priorityValue);

            return new FileArchiveBulkUploadOptions(validFrom, validTo, saveLater, dateUploadLater, emails,
                product, category, productCode, showFile, indexFile, priority, referenceToMain, note,
                uploadRedundantFile, null);
        } catch (NumberFormatException ex) {
            return error(ERROR_INVALID_ADVANCED_OPTIONS);
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
    }

    String getErrorKey() {
        return errorKey;
    }

    boolean isSaveLater() {
        return saveLater;
    }

    String applyTo(FileArchivatorBean entity) {
        if (validFrom != null || validTo != null) {
            Date effectiveValidFrom = validFrom != null ? validFrom : entity.getValidFrom();
            Date effectiveValidTo = validTo != null ? validTo : entity.getValidTo();
            if (effectiveValidFrom != null && effectiveValidTo != null
                && effectiveValidFrom.after(effectiveValidTo)) {
                return "components.file_archiv.bulk_upload.error.invalid_validity_interval";
            }
        }

        if (validFrom != null) {
            entity.setValidFrom(new Date(validFrom.getTime()));
        }
        if (validTo != null) {
            entity.setValidTo(new Date(validTo.getTime()));
        }
        if (saveLater) {
            entity.getEditorFields().setSaveLater(true);
            entity.getEditorFields().setDateUploadLater(new Date(dateUploadLater.getTime()));
            entity.getEditorFields().setEmails(emails);
        }
        if (product != null) entity.setProduct(product);
        if (category != null) entity.setCategory(category);
        if (productCode != null) entity.setProductCode(productCode);
        if (showFile != null) entity.setShowFile(showFile);
        if (indexFile != null) entity.setIndexFile(indexFile);
        if (priority != null) entity.setPriority(priority);
        if (referenceToMain != null) entity.setReferenceToMain(referenceToMain);
        if (note != null) entity.setNote(note);
        if (uploadRedundantFile != null) entity.getEditorFields().setUploadRedundantFile(uploadRedundantFile);
        return null;
    }

    private static FileArchiveBulkUploadOptions error(String errorKey) {
        return new FileArchiveBulkUploadOptions(null, null, false, null, null, null, null, null,
            null, null, null, null, null, null, errorKey);
    }

    private static Date parseDate(String value, String errorKey) {
        if (Tools.isEmpty(value)) return null;
        try {
            long timestamp = Long.parseLong(value);
            if (timestamp < 0 || timestamp > MAX_DATE) throw new IllegalArgumentException(errorKey);
            return new Date(timestamp);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorKey, ex);
        }
    }

    private static boolean areEmailsValid(String emails) {
        if (Tools.isEmpty(emails) || emails.length() > MAX_EMAILS_LENGTH) return false;

        String[] emailTokens = emails.split(",", -1);
        for (String email : emailTokens) {
            if (Tools.isEmail(email.trim()) == false) return false;
        }
        return true;
    }

    private static Boolean parseOptionalBoolean(String value) {
        if (value == null) return null;
        if ("true".equals(value)) return Boolean.TRUE;
        if ("false".equals(value)) return Boolean.FALSE;
        throw new IllegalArgumentException(ERROR_INVALID_ADVANCED_OPTIONS);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String parseText(String value, int maximumLength) {
        String trimmed = trimToNull(value);
        if (trimmed != null && trimmed.length() > maximumLength) {
            throw new IllegalArgumentException(ERROR_INVALID_ADVANCED_OPTIONS);
        }
        return trimmed;
    }
}
