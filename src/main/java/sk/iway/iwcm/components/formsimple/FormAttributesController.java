package sk.iway.iwcm.components.formsimple;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.common.CloudToolsForCore;

@RestController
@RequestMapping("/apps/simpleform")
public class FormAttributesController {

    private final FormAttributesRepository formAttributesRepository;

    public FormAttributesController(FormAttributesRepository formAttributesRepository) {
        this.formAttributesRepository = formAttributesRepository;
    }

    @PostMapping(value = "/save_attributes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void saveFormAttributes(@RequestBody FormSimpleApp formSimple) {
        String formName = formSimple.getFormName();
        if(Tools.isEmpty(formName)) return;

        List<FormAttributesEntity> data = formAttributesRepository.findAllByFormNameAndDomainId(formName, CloudToolsForCore.getDomainId());

        List<FormAttributesEntity> toSave = new ArrayList<>();
        for (Field field : formSimple.getClass().getDeclaredFields()) {
            if (field.getName().startsWith(FormSimpleApp.ATTRIBUTE_PREFIX)) {
                toSave.add( handleAttribute(formSimple, field, data) );
            }
        }

        //
        for(FormAttributesEntity attrToSave : toSave) {
            String value = attrToSave.getValue();
            if(Tools.isEmpty(value) || "false".equalsIgnoreCase(value)) {
                //Not valid value - if exist in DB, remove
                if(attrToSave.getId() != null && attrToSave.getId() > 0L)
                    formAttributesRepository.deleteById(attrToSave.getId());
            } else {
                //Its valid value do save that will do insert or update
                formAttributesRepository.save(attrToSave);
            }
        }
    }

    private FormAttributesEntity handleAttribute(FormSimpleApp formSimple, Field attrFiled, List<FormAttributesEntity> attributes) {
        String fieldName = attrFiled.getName();
        String sanitazedFieldName = fieldName.substring(FormSimpleApp.ATTRIBUTE_PREFIX.length());
        Class<?> fieldType = attrFiled.getType();
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        FormAttributesEntity entity = new FormAttributesEntity(null, formSimple.getFormName(), sanitazedFieldName, CloudToolsForCore.getDomainId());
        for(FormAttributesEntity attr : attributes) {
            if(sanitazedFieldName.equals(attr.getParamName())) {
                entity = attr;
                break;
            }
        }

        try {
            Method getter = formSimple.getClass().getMethod(getterName);
            Object value = getter.invoke(formSimple);

            if(fieldType == String.class || Number.class.isAssignableFrom(fieldType) || fieldType == Boolean.class) {
                entity.setValue( value == null ? "" : String.valueOf(value) );
            } else if(fieldType == DocDetailsDto.class) {
                DocDetailsDto page = (DocDetailsDto) value;
                if(page != null) entity.setValue(String.valueOf(page.getDocId()));
            }
        } catch(Exception e) {
            Logger.error(FormAttributesController.class, "Unable to get and process value of field: " + sanitazedFieldName);
        }

        return entity;
    }
}