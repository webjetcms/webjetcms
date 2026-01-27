package sk.iway.iwcm.components.form_settings.rest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.form.FormFileRestriction;

public class FormSettingsService {

    // Skip Transient fields or technical fields like ID, domainId, etc.
    private static final List<String> EXCLUDED_FIELDS = List.of(
        "id",
        "formName",
        "formAddClasses",
        "formCss",
        "useFormMailDoc",
        "encrypKeyInfo",
        "useFormDoc",
        "formmailSendUserInfoDoc",
        "domainId"
    );

    public static void prepareSettingsForSave(FormSettingsEntity formSettings, FormSettingsRepository formSettingsRepository) {
        if(formSettings == null) return;

        // Try get and set id
        Long formSettingsId = formSettingsRepository.findId(formSettings.getFormName(), CloudToolsForCore.getDomainId());
        formSettings.setId(formSettingsId);

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

    public static void prepareSettingsForEdit(FormSettingsEntity formSettings) {
        if(formSettings == null) return;

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

    public Map<String, String> load(String formName) {
        FormSettingsRepository formSettingsRepository = Tools.getSpringBean("formSettingsRepository", FormSettingsRepository.class);
        if(formSettingsRepository == null) return new HashMap<>();

        FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        if(formSettings == null) return new HashMap<>();

        return toAttributeMap(formSettings);
    }

    public void save(String formName, Map<String, String> parameters) {
        FormSettingsRepository formSettingsRepository = Tools.getSpringBean("formSettingsRepository", FormSettingsRepository.class);
        if(formSettingsRepository == null) {
            return;
        }

        Long formSettingsId = formSettingsRepository.findId(formName, CloudToolsForCore.getDomainId());
        FormSettingsEntity formSettings = new FormSettingsEntity();
        formSettings.setId(formSettingsId);

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
                    field.set(formSettings, value != null ? Integer.valueOf(value) : null);
                } else if (fieldType == Long.class) {
                    field.set(formSettings, value != null ? Long.valueOf(value) : null);
                } else if (fieldType == Boolean.class) {
                    field.set(formSettings, value != null ? Boolean.valueOf(value) : null);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to set field: " + key + " with value: " + value, e);
            }
        }

        formSettingsRepository.save(formSettings);
    }

    private static Map<String, String> toAttributeMap(Object entity) {
        Map<String, String> attributes = new HashMap<>();

        if (entity == null) {
            return attributes;
        }

        Class<?> clazz = entity.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            // Skipp fields that should not be included
            if (EXCLUDED_FIELDS.contains(field.getName())) continue;

            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                attributes.put(field.getName(), value != null ? value.toString() : null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(
                        "Failed to read field: " + field.getName(), e
                );
            }
        }

        return attributes;
    }

    public Map<String, String> filterAttributes(Map<String, String[]> parameters)
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

    public static FormFileRestriction getFileRestriction(String formName) {
        FormSettingsRepository formSettingsRepository = Tools.getSpringBean("formSettingsRepository", FormSettingsRepository.class);
        if(formSettingsRepository == null) {
            return new FormFileRestriction();
        }

        return getFileRestriction(formName, formSettingsRepository);
    }

    public static FormFileRestriction getFileRestriction(String formName, FormSettingsRepository formSettingsRepository) {
        FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        if(formSettings == null) return new FormFileRestriction();

        FormFileRestriction restriction = new FormFileRestriction();
        restriction.setFormName(formName);
        restriction.setAllowedExtensions(formSettings.getAllowedExtensions());
        restriction.setMaxSizeInKilobytes(formSettings.getMaxSizeInKilobytes() != null ? formSettings.getMaxSizeInKilobytes() : 0);
        restriction.setPictureWidth(formSettings.getPictureWidth() != null ? formSettings.getPictureWidth() : 0);
        restriction.setPictureHeight(formSettings.getPictureHeight() != null ? formSettings.getPictureHeight() : 0);
        return restriction;
    }
}
