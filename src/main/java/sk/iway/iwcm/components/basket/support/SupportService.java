package sk.iway.iwcm.components.basket.support;

import java.math.BigDecimal;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.validation.Errors;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BasketTools;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodEntity;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.components.basket.rest.BasketPricingService;
import sk.iway.iwcm.components.basket.rest.EshopService;
import sk.iway.tags.CurrencyTag;

public class SupportService {

    private SupportService() {}

    public static final void prepareFields(List<Field> fields, FieldMapAttr[] fieldMapAttrs,  Character lastAlphabet, Prop prop) {
        for (char alphabet = 'A'; alphabet <= lastAlphabet; alphabet++) {
            int index = alphabet - 'A';
            if(index < 0 || index > fields.size() - 1) continue;

            boolean found = false;
            for(FieldMapAttr fieldMapAttr : fieldMapAttrs) {
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
    }

    @SuppressWarnings("java:S4449")
    public static final void validateCustomFields(FieldsConfig annotation, SupportMethodEntity methodEntity, Errors errors, Prop prop) {
        for(FieldMapAttr fieldMapAttr : annotation.fieldMap()) {
            if(fieldMapAttr.isRequired() == true) {
                String fieldValue = getFieldValue(methodEntity, fieldMapAttr.fieldAlphabet());
                if(Tools.isAnyEmpty(fieldValue) == true) {
                    errors.rejectValue("errorField.field" + fieldMapAttr.fieldAlphabet(), null, prop.getText("jakarta.validation.constraints.NotBlank.message"));
                }
            }
        }
    }

    public static final String getCustomerTitle(BigDecimal priceVat, HttpServletRequest request, Prop prop, FieldsConfig annotation) {
        return prop.getText(annotation.nameKey()) + ": " + formatPrice(priceVat, request);
    }

    public static final String getCustomerTitle(DeliveryMethodEntity dme, HttpServletRequest request, Prop prop, FieldsConfig annotation) {
        String title = Tools.isNotEmpty(dme.getTitle()) ? dme.getTitle() : annotation.nameKey();
        return prop.getText(title, false) + ": " + formatPrice(dme.getPriceVat(), request);
    }

    /** Returns the converted fee amount used by labels and fee previews. */
    public static BigDecimal getLocalPriceVat(BigDecimal priceVat, HttpServletRequest request) {
        BigDecimal converted = BasketTools.convertToBasketDisplayCurrency(priceVat, request);
        return BasketPricingService.isEnabled()
            ? BasketPricingService.roundLineGross(BasketPricingService.roundGross(converted), 1) : converted;
    }

    private static String formatPrice(BigDecimal priceVat, HttpServletRequest request) {
        String currency = BasketPricingService.isEnabled() ? EshopService.getDisplayCurrency(request) : Constants.getString("basketDisplayCurrency");
        return CurrencyTag.formatNumber(getLocalPriceVat(priceVat, request)) + " " + CurrencyTag.getLabelFromCurrencyCode(currency);
    }

    public static final String getFieldValue(SupportMethodEntity method, char fieldAlphabet) {
        switch(fieldAlphabet) {
            case 'A': return method.getFieldA();
            case 'B': return method.getFieldB();
            case 'C': return method.getFieldC();
            case 'D': return method.getFieldD();
            case 'E': return method.getFieldE();
            case 'F': return method.getFieldF();
            case 'G': return method.getFieldG();
            case 'H': return method.getFieldH();
            case 'I': return method.getFieldI();
            case 'J': return method.getFieldJ();
            case 'K': return method.getFieldK();
            case 'L': return method.getFieldL();
            default: return null;
        }
    }

}
