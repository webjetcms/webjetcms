package sk.iway.iwcm.components.basket.delivery_methods.rest;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.validation.Errors;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodEntity;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodsRepository;
import sk.iway.iwcm.components.basket.support.FieldsConfig;
import sk.iway.iwcm.components.basket.support.SupportService;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Base class for delivery method implementations. You can extend this class to create your own delivery method.
 * See existing implementations in ByMailService or InStoreService for examples.
 */
public abstract class BaseDeliveryMethod {

    private static final Character LAST_ALPHABET = 'L';

    protected List<DeliveryMethodEntity> getDeliveryMethods(DeliveryMethodsRepository repo, HttpServletRequest request, Prop prop, boolean setTitle) {
        FieldsConfig annotation = this.getClass().getAnnotation(FieldsConfig.class);
        if(annotation == null) return null;

        List<DeliveryMethodEntity> deliveryMethods = repo.findAllByDeliveryMethodNameAndDomainId(this.getClass().getName(), CloudToolsForCore.getDomainId());
        if(setTitle == true) {
            deliveryMethods.stream().forEach(dme -> dme.setCustomerTitle( SupportService.getCustomerTitle(dme, request, prop, annotation) ));
        }

        return deliveryMethods;
    }

    protected LabelValue getDeliveryMethodOption(Prop prop) {
        FieldsConfig annotation = this.getClass().getAnnotation(FieldsConfig.class);
        if(annotation == null) return null;

        return new LabelValue(prop.getText(annotation.nameKey()), this.getClass().getName());
    }

    @SuppressWarnings("all")
    protected void validateEditorValues(DeliveryMethodEntity deliveryMethod, Errors errors, Prop prop, DeliveryMethodsRepository repo) {
        FieldsConfig annotation = this.getClass().getAnnotation(FieldsConfig.class);
        if(annotation == null) return;

        SupportService.validateCustomFields(annotation, deliveryMethod, errors, prop);

        //At least one country must be selected
        String[] countries = deliveryMethod.getSupportedCountries();
        if(countries == null || countries.length < 1) {
            errors.rejectValue("errorField.supportedCountries", null, prop.getText("apps.eshop.delivery_methods.supproted_countries_empty_err"));
            return;
        }
    }

    protected void prepareDelivery(DeliveryMethodEntity deliveryMethod, Prop prop) {
        FieldsConfig annotation = this.getClass().getAnnotation(FieldsConfig.class);
        if(annotation == null) return;

        BaseEditorFields def = new BaseEditorFields();
        List<Field> fields = def.getFields(deliveryMethod, "delivery", LAST_ALPHABET);

        SupportService.prepareFields(fields, annotation.fieldMap(), LAST_ALPHABET, prop);

        def.setFieldsDefinition(fields);
        deliveryMethod.setEditorFields(def);
    }
}
