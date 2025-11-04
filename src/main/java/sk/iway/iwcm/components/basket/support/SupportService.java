package sk.iway.iwcm.components.basket.support;

import java.math.BigDecimal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.validation.Errors;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BasketTools;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
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
    public static final void validateCustomFields(SupportMethod annotation, SupportMethodEntity methodEntity, Errors errors, Prop prop) {
        for(FieldMapAttr fieldMapAttr : annotation.fieldMap()) {
            if(fieldMapAttr.isRequired() == true) {
                String fieldValue = getFieldValue(methodEntity, fieldMapAttr.fieldAlphabet());
                if(Tools.isAnyEmpty(fieldValue) == true) {
                    errors.rejectValue("errorField.field" + fieldMapAttr.fieldAlphabet(), null, prop.getText("javax.validation.constraints.NotBlank.message"));
                }
            }
        }
    }

    public static final String getJspTitle(BigDecimal priceVat, HttpServletRequest request, Prop prop, SupportMethod annotation) {
        StringBuilder name = new StringBuilder();
        name.append( prop.getText(annotation.nameKey()) ).append(": ");
        BigDecimal convertedPrice = BasketTools.convertToBasketDisplayCurrency(priceVat, request);
        name.append( CurrencyTag.formatNumber(convertedPrice) ).append(" ");
        name.append( CurrencyTag.getLabelFromCurrencyCode( Constants.getString("basketDisplayCurrency") ) );
        return name.toString();
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