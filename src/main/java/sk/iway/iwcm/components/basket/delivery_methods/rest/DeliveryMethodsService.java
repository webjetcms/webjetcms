package sk.iway.iwcm.components.basket.delivery_methods.rest;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BasketTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodEntity;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodsRepository;
import sk.iway.iwcm.components.basket.support.MethodDto;
import sk.iway.iwcm.components.basket.support.FieldsConfig;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Service
public class DeliveryMethodsService {

    private final List<BaseDeliveryMethod> deliveryMethods;
    private final DeliveryMethodsRepository repo;

    @Autowired
    public DeliveryMethodsService(List<BaseDeliveryMethod> deliveryMethods, DeliveryMethodsRepository repo) {
        this.deliveryMethods = deliveryMethods;
        this.repo = repo;
    }

    /* PUBLIC STATIC */

    public static String getDeliveryMethodLabel(String deliveryMethod, String title, HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);
        return getDeliveryMethodLabel(deliveryMethod, title, prop);
    }

    public static String getDeliveryMethodLabel(String deliveryMethod, String title, Prop prop) {
        if(Tools.isEmpty(deliveryMethod)) return "";



        if (Tools.isNotEmpty(title)) {
            return prop.getText(title);
        }

        try{
            Class<?> dmClass = Class.forName(deliveryMethod);

            //Check that class is child of BaseDeliveryMethod
            if(BaseDeliveryMethod.class.isAssignableFrom(dmClass)) {
                //Get payment method class
                Constructor<?> constructor = dmClass.getConstructor();
                BaseDeliveryMethod delivery = (BaseDeliveryMethod) constructor.newInstance();

                FieldsConfig annotation = delivery.getClass().getAnnotation(FieldsConfig.class);
                if(annotation == null) return "";

                return prop.getText(annotation.nameKey());
            }
            return "";
        } catch(Exception e) {
            //Ignore
        }

        return prop.getText(deliveryMethod);
    }

    /* PUBLIC  */

    public List<DeliveryMethodEntity> getAllDeliveryMethods(HttpServletRequest request, Prop prop, boolean setTitle) {
        List<DeliveryMethodEntity> allDeliveryMethods = new ArrayList<>();
        for(BaseDeliveryMethod dm : deliveryMethods) {
            allDeliveryMethods.addAll( dm.getDeliveryMethods(repo, request, prop, setTitle) );
        }
        return allDeliveryMethods;
    }

    public List<MethodDto> getAllDeliveryMethods(HttpServletRequest request, Prop prop, String country) { // set title by default -> its importatnt for JSP file
        List<DeliveryMethodEntity> allDeliveryMethods = getAllDeliveryMethods(request, prop, true);
        List<DeliveryMethodEntity> filtered = allDeliveryMethods.stream().filter(dm -> dm.getSupportedCountriesList().contains(country)).toList();
        return filtered.stream().map(dme -> new MethodDto(dme.getId() + "", dme.getCustomerTitle(), dme.getPriceVat())).toList();
    }

    public DeliveryMethodEntity getDeliveryMethod(long id, String deliveryMethodName, Prop prop) {
        DeliveryMethodEntity entity;
        if(id == -1) {
            entity = new DeliveryMethodEntity();
            entity.setDeliveryMethodName(deliveryMethodName);
        } else {
            entity = repo.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElseThrow(() -> new IllegalStateException("Delivery method not found"));
        }

        BaseDeliveryMethod bdm = getBaseDeliveryMethod(id < 1 ? deliveryMethodName : entity.getDeliveryMethodName());
        if(bdm == null) throw new IllegalStateException("Delivery method not found");

        bdm.prepareDelivery(entity, prop);
        return entity;
    }

    public void validateEditorValues(DeliveryMethodEntity deliveryMethod, Errors errors, Prop prop) {
        BaseDeliveryMethod bdm = getBaseDeliveryMethod(deliveryMethod.getDeliveryMethodName());
        if(bdm == null) throw new IllegalStateException("Delivery method not found");
        bdm.validateEditorValues(deliveryMethod, errors, prop, repo);
    }

    public void setOptions(DatatablePageImpl<DeliveryMethodEntity> page, Prop prop) {
        page.addOptions("deliveryMethodName", getDeliveryOptionsClasses(prop), "label", "value", false);
        setCoutryOptions(page, prop);
        setCurrencyOptions(page);
    }

    public final List<LabelValue> getDeliveryOptions(Prop prop) {
        List<LabelValue> deliveryMethodOptions = new ArrayList<>();

        List<DeliveryMethodEntity> deliveryMethodsRepo = repo.findAllByDomainId(CloudToolsForCore.getDomainId());

        for(DeliveryMethodEntity dm : deliveryMethodsRepo) {
            String label = getDeliveryMethodLabel(dm.getDeliveryMethodName(), dm.getTitle(), prop);
            String value = dm.getTitle();
            if (Tools.isEmpty(value)) value = dm.getDeliveryMethodName();

            LabelValue option = new LabelValue(label, value);
            deliveryMethodOptions.add(option);
        }

        return deliveryMethodOptions;
    }

    public final List<LabelValue> getDeliveryOptionsClasses(Prop prop) {
        List<LabelValue> deliveryMethodOptions = new ArrayList<>();

        for(BaseDeliveryMethod dm : deliveryMethods) {
            LabelValue option = dm.getDeliveryMethodOption(prop);
            if(option != null) deliveryMethodOptions.add(option);
        }

        return deliveryMethodOptions;
    }

    /* PRIVATE */

    private BaseDeliveryMethod getBaseDeliveryMethod(String methodName) {
        if(Tools.isEmpty(methodName)) return null;
        for(BaseDeliveryMethod dm : deliveryMethods)
            if(dm.getClass().getName().equals(methodName)) return dm;

        return null;
    }

    private void setCoutryOptions(DatatablePageImpl<DeliveryMethodEntity> page, Prop prop) {
        for (String countryCode : Constants.getArray("basketInvoiceSupportedCountries")) {
            if(countryCode.startsWith(".") == false) countryCode = "." + countryCode;
            page.addDefaultOption("supportedCountries", BasketTools.getCountryName(countryCode, prop), countryCode);
        }
    }

    private void setCurrencyOptions(DatatablePageImpl<DeliveryMethodEntity> page) {
        for(String currency : BasketTools.getSupportedCurrencies())
            page.addDefaultOption("currency", currency, currency);
    }
}