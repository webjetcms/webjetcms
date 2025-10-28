package sk.iway.iwcm.components.basket.delivery_methods.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.validation.Errors;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BasketTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodEntity;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodsRepository;
import sk.iway.iwcm.components.basket.support.SupportMethod;
import sk.iway.iwcm.components.basket.support.SupportService;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.json.LabelValue;

public abstract class BaseDeliveryMethod {

    private static final Character LAST_ALPHABET = 'L';

    protected List<DeliveryMethodEntity> getDeliveryMethods(DeliveryMethodsRepository repo, HttpServletRequest request, Prop prop, boolean setTitle) {
        SupportMethod annotation = this.getClass().getAnnotation(SupportMethod.class);
        if(annotation == null) return null;

        List<DeliveryMethodEntity> deliveryMethods = repo.findAllByDeliveryMethodNameAndDomainId(this.getClass().getName(), CloudToolsForCore.getDomainId());
        if(setTitle == true)
            deliveryMethods.stream().forEach(dme -> dme.setTitle( SupportService.getJspTitle(dme.getPriceVat(), request, prop, annotation) ));

        return deliveryMethods;
    }

    protected LabelValue getDeliveryMethodOption(Prop prop) {
        SupportMethod annotation = this.getClass().getAnnotation(SupportMethod.class);
        if(annotation == null) return null;

        return new LabelValue(prop.getText(annotation.nameKey()), this.getClass().getName());
    }

    @SuppressWarnings("all")
    protected void validateEditorValues(DeliveryMethodEntity deliveryMethod, Errors errors, Prop prop, DeliveryMethodsRepository repo) {
        SupportMethod annotation = this.getClass().getAnnotation(SupportMethod.class);
        if(annotation == null) return;

        SupportService.validateCustomFields(annotation, deliveryMethod, errors, prop);

        //At least one country must be selected
        String[] countries = deliveryMethod.getSupportedCountries();
        if(countries == null || countries.length < 1) {
            errors.rejectValue("errorField.supportedCountries", null, prop.getText("apps.eshop.delivery_methods.supproted_countries_empty_err"));
            return;
        }

        //Now check, if this delivery method is allready used for this country
        List<String> handledCountries = new ArrayList<>();
        for(String countriesStr : repo.getHandledCountriesByDeliveryMethod(this.getClass().getName(), CloudToolsForCore.getDomainId(), deliveryMethod.getId() == null ? -1L : deliveryMethod.getId()))
            handledCountries.addAll( Arrays.asList( Tools.getTokens(countriesStr, ",+") ));

        List<String> redundantCountries = new ArrayList<>();
        for(String country : deliveryMethod.getSupportedCountries())
            if(handledCountries.contains(country)) redundantCountries.add(country);

        if(redundantCountries.size() > 0) {
            StringBuilder errCountries = new StringBuilder();
            for(String country : redundantCountries) errCountries.append( BasketTools.getCountryName(country, prop) ).append(", ");
            errCountries.deleteCharAt(errCountries.length() - 1);
            errors.rejectValue("errorField.supportedCountries", null, prop.getText("apps.eshop.delivery_methods.supproted_countries_err", errCountries.toString()));
        }
    }

    protected void prepareDelivery(DeliveryMethodEntity deliveryMethod, Prop prop) {
        SupportMethod annotation = this.getClass().getAnnotation(SupportMethod.class);
        if(annotation == null) return;

        BaseEditorFields def = new BaseEditorFields();
        List<Field> fields = def.getFields(deliveryMethod, "delivery", LAST_ALPHABET);

        SupportService.prepareFields(fields, annotation.fieldMap(), LAST_ALPHABET, prop);

        def.setFieldsDefinition(fields);
        deliveryMethod.setEditorFields(def);
    }
}
