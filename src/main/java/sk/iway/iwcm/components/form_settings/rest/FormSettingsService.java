package sk.iway.iwcm.components.form_settings.rest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.multistep_form.support.FormProcessorInterface;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.form.FormFileRestriction;

/**
 * Prepares form settings for administration and bridges persisted settings with legacy attribute maps.
 * Also resolves available custom processors and upload restrictions for forms in the current domain.
 */
@Service
public class FormSettingsService {

    private final FormSettingsRepository formSettingsRepository;
    private final List<FormProcessorInterface> formProcessorInterfaces;

    @Autowired
    public FormSettingsService(FormSettingsRepository formSettingsRepository, List<FormProcessorInterface> formProcessorInterfaces) {
        this.formSettingsRepository = formSettingsRepository;
        this.formProcessorInterfaces = formProcessorInterfaces;
    }

    // Skip Transient fields or technical fields like ID, domainId, etc.
    private static final List<String> EXCLUDED_FIELDS = List.of(
        "id",
        "formName",
        "formAddClasses",
        "formCss",
        "useFormMailDoc",
        "encrypKeyInfo",
        "useFormDoc",
        "forwardAsContent",
        "formmailSendUserInfoDoc",
        "domainId"
    );

    /* FUNCTIONS for REST */

    public void prepareSettingsForSave(FormSettingsEntity formSettings, String formType) {
        prepareSettingsForSave(formSettings, formType, formSettingsRepository);
    }

    /**
     * Updates settings in place with the existing record ID, processor configuration, and selected page IDs.
     * Assigns the current domain when none is set; does not persist the entity.
     *
     * @param formSettings settings to prepare, or {@code null} to do nothing
     * @param formType form type used to map the multistep processor field
     * @param formSettingsRepository repository used to find the existing settings ID in the current domain
     */
    public static void prepareSettingsForSave(FormSettingsEntity formSettings, String formType, FormSettingsRepository formSettingsRepository) {
        if(formSettings == null) return;

        // Try get and set id
        Long formSettingsId = formSettingsRepository.findId(formSettings.getFormName(), CloudToolsForCore.getDomainId());
        formSettings.setId(formSettingsId);

        if (FormsService.FORM_TYPE.MULTISTEP.value().equals(formType)) {
            // Copy value from formProcessr (transient autocomple) into real filed
            formSettings.setAfterSendInterceptor( formSettings.getFormProcessor() );
        } else {
            formSettings.setFormProcessor(null);
        }

        // convert object back to id
        if(formSettings.getUseFormDoc() == null) formSettings.setUseFormDoc(null);
        else formSettings.setUseFormDocId(formSettings.getUseFormDoc().getDocId());

        // convert object back to id
        if(formSettings.getUseFormMailDoc() == null) formSettings.setUseFormMailDocId(null);
        else formSettings.setUseFormMailDocId(formSettings.getUseFormMailDoc().getDocId());

        // convert object back to id
        if(formSettings.getFormmailSendUserInfoDoc() == null) formSettings.setFormmailSendUserInfoDoc(null);
        else formSettings.setFormMailSendUserInfoDocId(formSettings.getFormmailSendUserInfoDoc().getDocId());

        //
        if(formSettings.getDomainId() == null) formSettings.setDomainId(CloudToolsForCore.getDomainId());
    }

    /**
     * Populates transient page selections and the multistep processor field for the settings editor.
     *
     * @param formSettings settings to update in place, or {@code null} to do nothing
     * @param formType form type determining whether the processor autocomplete is populated
     */
    public static void prepareSettingsForEdit(FormSettingsEntity formSettings, String formType) {
        if(formSettings == null) return;

        if (FormsService.FORM_TYPE.MULTISTEP.value().equals(formType)) {
            // Copy value from real field into transient autocomplete
            formSettings.setFormProcessor( formSettings.getAfterSendInterceptor() );
        } else {
            formSettings.setFormProcessor(null);
        }

        if(formSettings.getUseFormDocId() != null) {
            DocDetails doc = DocDB.getInstance().getDoc( formSettings.getUseFormDocId() );
            if(doc != null) formSettings.setUseFormDoc( new DocDetailsDto(doc) );
        }

        if(formSettings.getUseFormMailDocId() != null) {
            DocDetails doc = DocDB.getInstance().getDoc( formSettings.getUseFormMailDocId() );
            if(doc != null) formSettings.setUseFormMailDoc( new DocDetailsDto(doc) );
        }

        if(formSettings.getFormMailSendUserInfoDocId() != null) {
            DocDetails doc = DocDB.getInstance().getDoc( formSettings.getFormMailSendUserInfoDocId() );
            if(doc != null) formSettings.setFormmailSendUserInfoDoc( new DocDetailsDto(doc) );
        }
    }

    /**
     * Finds registered processor class names matching a case-insensitive substring.
     *
     * @param term search text; {@code null}, blank, or {@code %} includes all registered processors
     * @return matching fully qualified class names
     */
    public List<String> getFormProcessorOptions(String term) {
        List<String> ac = new ArrayList<>();

        for(FormProcessorInterface processorInterface : formProcessorInterfaces) {
            String className = processorInterface.getClass().getName();
            if (Tools.isEmpty(term) || "%".equals(term) || className.toLowerCase().contains( term.toLowerCase() )) {
                ac.add(className);
            }
        }

        return ac;
    }

    /* FUNCTIONS AS replacement for old ATTRIBUTE's logic */

    /**
     * Loads settings for the current domain as legacy form attributes, excluding technical and editor-only fields.
     *
     * @param formName logical form name
     * @return attributes with nullable string values, or an empty map when the repository or settings are unavailable
     */
    public static Map<String, String> load(String formName) {
        FormSettingsRepository formSettingsRepository = Tools.getSpringBean("formSettingsRepository", FormSettingsRepository.class);
        if(formSettingsRepository == null) return new HashMap<>();

        FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        if(formSettings == null) return new HashMap<>();

        return toAttributeMap(formSettings);
    }

    /**
     * Creates or updates settings selected by form name and current domain using entity field names as keys.
     * Converts string, integer, long, and boolean fields; logs individual mapping failures and continues.
     * Does nothing when the repository bean is unavailable.
     *
     * @param formName logical form name used to locate or initialize the settings
     * @param parameters field values to apply, leaving unspecified fields unchanged
     */
    public static void save(String formName, Map<String, String> parameters) {
        FormSettingsRepository formSettingsRepository = Tools.getSpringBean("formSettingsRepository", FormSettingsRepository.class);
        if(formSettingsRepository == null) return;

        FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        if(formSettings == null) {
            formSettings = new FormSettingsEntity();
            formSettings.setFormName(formName);
            formSettings.setDomainId(CloudToolsForCore.getDomainId());
        }

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            try {
                Field field = FormSettingsEntity.class.getDeclaredField(key);
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (fieldType == String.class) {
                    field.set(formSettings, value);
                } else if (fieldType == Integer.class) {
                    field.set(formSettings, getIntegerValue(value));
                } else if (fieldType == Long.class) {
                    field.set(formSettings, getLongValue(value));
                } else if (fieldType == Boolean.class) {
                    field.set(formSettings, getBooleanValue(value));
                }
            } catch (Exception e) {
                Logger.error(FormSettingsService.class, "Failed to set field: " + key + " with value: " + value, e);
            }
        }

        formSettingsRepository.save(formSettings);
    }

    private static Integer getIntegerValue(String value) {
        int ret = Tools.getIntValue(value, -1);
        return ret == -1 ? null : ret;
	}

    private static Long getLongValue(String value) {
        long ret = Tools.getLongValue(value, -1L);
        return ret == -1L ? null : ret;
    }

    private static Boolean getBooleanValue(String value) {
        return Tools.getBooleanValue(value, false);
    }

    /**
     * Converts declared entity fields to legacy attributes, omitting the configured excluded fields.
     *
     * @param entity settings entity to read, or {@code null} for an empty map
     * @return field names mapped to string values, retaining {@code null} values
     * @throws RuntimeException when a field cannot be read after making it accessible
     */
    private static Map<String, String> toAttributeMap(Object entity) {
        Map<String, String> attributes = new HashMap<>();

        if (entity == null) return attributes;

        Class<?> clazz = entity.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            // Skipp fields that should not be included
            if (EXCLUDED_FIELDS.contains(field.getName())) continue;

            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                attributes.put(field.getName(), value != null ? value.toString() : null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException( "Failed to read field: " + field.getName(), e);
            }
        }

        return attributes;
    }

    /**
     * Extracts the first value of each request parameter whose name starts with {@code attribute_}.
     *
     * @param parameters request parameters with at least one value per matching name
     * @return selected values keyed by names with {@code attribute_} removed
     */
    public static Map<String, String> filterAttributes(Map<String, String[]> parameters)
	{
		Map<String, String> filtered = new HashMap<String, String>();
		for (Map.Entry<String, String[]> entry : parameters.entrySet())
		{
			String paramName = entry.getKey();
			if (paramName.startsWith("attribute_"))
				filtered.put(paramName.replace("attribute_", ""), entry.getValue()[0]);
		}
		return filtered;
	}

    /**
     * Loads upload restrictions for a form in the current domain through the repository bean.
     *
     * @param formName logical form name
     * @return configured restrictions, defaults when settings are missing, or an empty restriction object
     *         when the repository bean is unavailable
     */
    public static FormFileRestriction getFileRestriction(String formName) {
        FormSettingsRepository formSettingsRepository = Tools.getSpringBean("formSettingsRepository", FormSettingsRepository.class);
        if(formSettingsRepository == null) {
            return new FormFileRestriction();
        }

        return getFileRestriction(formName, formSettingsRepository);
    }

    public static FormFileRestriction getFileRestriction(String formName, FormSettingsRepository formSettingsRepository) {
        FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        return getFileRestriction(formName, formSettings);
    }

    /**
     * Builds upload restrictions from settings, treating unset numeric limits as zero.
     * Without settings, uses a 5000 KB per-file limit and 3000 by 2000 pixel image limits,
     * with no extension filter or combined-size limit.
     *
     * @param formName logical form name attached to the restrictions
     * @param formSettings settings to read, or {@code null} to use the default limits
     * @return a new restriction object for the form
     */
    public static FormFileRestriction getFileRestriction(String formName, FormSettingsEntity formSettings) {
        if(formSettings == null) {
            FormFileRestriction restriction = new FormFileRestriction();
            restriction.setFormName(formName);
            restriction.setAllowedExtensions("");
            restriction.setMaxSizeInKilobytes(5000);
            restriction.setPictureWidth(3000);
            restriction.setPictureHeight(2000);
            return restriction;
        }

        FormFileRestriction restriction = new FormFileRestriction();
        restriction.setFormName(formName);
        restriction.setAllowedExtensions(formSettings.getAllowedExtensions());
        restriction.setMaxSizeInKilobytes(formSettings.getMaxSizeInKilobytes() != null ? formSettings.getMaxSizeInKilobytes() : 0);
        restriction.setMaxCombinedSizeInKilobytes(formSettings.getMaxCombinedSizeInKilobytes() != null ? formSettings.getMaxCombinedSizeInKilobytes() : 0);
        restriction.setPictureWidth(formSettings.getPictureWidth() != null ? formSettings.getPictureWidth() : 0);
        restriction.setPictureHeight(formSettings.getPictureHeight() != null ? formSettings.getPictureHeight() : 0);
        return restriction;
    }
}
