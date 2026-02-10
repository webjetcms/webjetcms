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
        "formmailSendUserInfoDoc",
        "domainId"
    );

    /* FUNCTIONS for REST */

    public void prepareSettingsForSave(FormSettingsEntity formSettings, String formType) {
        prepareSettingsForSave(formSettings, formType, formSettingsRepository);
    }

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

    public static Map<String, String> load(String formName) {
        FormSettingsRepository formSettingsRepository = Tools.getSpringBean("formSettingsRepository", FormSettingsRepository.class);
        if(formSettingsRepository == null) return new HashMap<>();

        FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());
        if(formSettings == null) return new HashMap<>();

        return toAttributeMap(formSettings);
    }

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
