package sk.iway.iwcm.components.basket.delivery_methods.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.validation.Errors;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethod;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodEntity;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodsRepository;
import sk.iway.iwcm.components.basket.support.FieldMapAttr;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.json.LabelValue;

public abstract class BaseDeliveryMethod {

    private static final Character LAST_ALPHABET = 'H';

    public static final String COUNTRY_KEY_PREFIX = "stat.countries.tld";

    protected List<DeliveryMethodEntity> getDeliveryMethods(DeliveryMethodsRepository repo, Prop prop) {
        DeliveryMethod annotation = this.getClass().getAnnotation(DeliveryMethod.class);
        if(annotation == null) return null;

        List<DeliveryMethodEntity> deliveryMethods = repo.findAllByDeliveryMethodNameAndDomainId(this.getClass().getName(), CloudToolsForCore.getDomainId());

        return deliveryMethods;
    }

    protected LabelValue getDeliveryMethodOption(Prop prop) {
        DeliveryMethod annotation = this.getClass().getAnnotation(DeliveryMethod.class);
        if(annotation == null) return null;

        return new LabelValue(prop.getText(annotation.nameKey()), this.getClass().getName());
    }

    @SuppressWarnings("all")
    protected void validateEditorValues(DeliveryMethodEntity deliveryMethod, Errors errors, Prop prop, DeliveryMethodsRepository repo) {
        DeliveryMethod annotation = this.getClass().getAnnotation(DeliveryMethod.class);
        if(annotation == null) return;

        for(FieldMapAttr fieldMapAttr : annotation.fieldMap()) {
            if(fieldMapAttr.isRequired() == true) {
                String fieldValue = getDeliveryFieldValue(deliveryMethod, fieldMapAttr.fieldAlphabet());
                if(Tools.isAnyEmpty(fieldValue) == true) {
                    errors.rejectValue("errorField.field" + fieldMapAttr.fieldAlphabet(), null, prop.getText("javax.validation.constraints.NotBlank.message"));
                }
            }
        }

        //Now check, if this delivery method is allredy used for this country
        List<String> handledCountries = new ArrayList<>();
        for(String countriesStr : repo.getHandledCountriesByDeliveryMethod(this.getClass().getName(), CloudToolsForCore.getDomainId(), deliveryMethod.getId() == null ? -1L : deliveryMethod.getId()))
            handledCountries.addAll( Arrays.asList( Tools.getTokens(countriesStr, ",+") ));

        List<String> redundantCountries = new ArrayList<>();
        for(String country : deliveryMethod.getSupportedCountries())
            if(handledCountries.contains(country)) redundantCountries.add(country);

        if(redundantCountries.size() > 0) {
            StringBuilder errCoiuntries = new StringBuilder();
            for(String country : redundantCountries) errCoiuntries.append( prop.getText(COUNTRY_KEY_PREFIX + country) ).append(", ");
            errors.rejectValue("errorField.supportedCountries", null, prop.getText("apps.eshop.delivery_methods.supproted_countries_err", errCoiuntries.toString()));
        }

    }

    private String getDeliveryFieldValue(DeliveryMethodEntity deliveryMethod, char fieldAlphabet) {
        switch(fieldAlphabet) {
            case 'A': return deliveryMethod.getFieldA();
            case 'B': return deliveryMethod.getFieldB();
            case 'C': return deliveryMethod.getFieldC();
            case 'D': return deliveryMethod.getFieldD();
            case 'E': return deliveryMethod.getFieldE();
            case 'F': return deliveryMethod.getFieldF();
            case 'G': return deliveryMethod.getFieldG();
            case 'H': return deliveryMethod.getFieldH();
            default: return null;
        }
    }

    protected void prepareDelivery(DeliveryMethodEntity deliveryMethod, Prop prop) {
        DeliveryMethod annotation = this.getClass().getAnnotation(DeliveryMethod.class);
        if(annotation == null) return;

        BaseEditorFields def = new BaseEditorFields();
        List<Field> fields = def.getFields(deliveryMethod, "delivery", LAST_ALPHABET);

        for (char alphabet = 'A'; alphabet <= LAST_ALPHABET; alphabet++) {
            int index = alphabet - 'A';
            if(index < 0 || index > fields.size() - 1) continue;

            boolean found = false;
            for(FieldMapAttr fieldMapAttr : annotation.fieldMap()) {
                if(alphabet == fieldMapAttr.fieldAlphabet()) {
                    fields.get(index).setLabel( prop.getText(fieldMapAttr.fieldLabel()) );
                    fields.get(index).setType( fieldMapAttr.fieldType().name().toLowerCase() );

                    if( Tools.isEmpty(fields.get(index).getValue()) ) {
                        fields.get(index).setValue( fieldMapAttr.defaultValue() );
                    }

                    found = true;
                    break;
                }
            }

            //'Not found' = 'not used', set them as NONE type
            if(found == false) {
                fields.get(index).setLabel("");
                fields.get(index).setType("none");

            }
        }

        def.setFieldsDefinition(fields);
        deliveryMethod.setEditorFields(def);
    }
}
