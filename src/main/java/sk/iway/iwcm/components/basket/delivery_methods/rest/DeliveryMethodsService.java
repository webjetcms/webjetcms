package sk.iway.iwcm.components.basket.delivery_methods.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodEntity;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodsRepository;
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

    public List<DeliveryMethodEntity> getAllDeliveryMethods(Prop prop) {
        List<DeliveryMethodEntity> allDeliveryMethods = new ArrayList<>();
        for(BaseDeliveryMethod dm : deliveryMethods) {
            allDeliveryMethods.addAll( dm.getDeliveryMethods(repo, prop) );
        }
        return allDeliveryMethods;
    }

    public DeliveryMethodEntity getDeliveryMethod(long id, String deliveryMethodName, Prop prop) {
        DeliveryMethodEntity entity;
        if(id == -1) {
            entity = new DeliveryMethodEntity();
            entity.setDeliveryMethodName(this.getClass().getName());
        } else {
            entity = repo.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElseThrow(() -> new IllegalStateException("Delivery method not found"));
        }

        BaseDeliveryMethod bdm = getDeliveryMethods(id < 1 ? deliveryMethodName : entity.getDeliveryMethodName());
        if(bdm == null) throw new IllegalStateException("Delivery method not found");

        bdm.prepareDelivery(entity, prop);
        return entity;
    }

    public void validateEditorValues(DeliveryMethodEntity deliveryMethod, Errors errors, Prop prop) {
        BaseDeliveryMethod bdm = getDeliveryMethods(deliveryMethod.getDeliveryMethodName());
        if(bdm == null) throw new IllegalStateException("Delivery method not found");
        bdm.validateEditorValues(deliveryMethod, errors, prop, repo);
    }

    private BaseDeliveryMethod getDeliveryMethods(String methodName) {
        if(Tools.isEmpty(methodName)) return null;
        for(BaseDeliveryMethod dm : deliveryMethods)
            if(dm.getClass().getName().equals(methodName)) return dm;

        return null;
    }

    public void setOptions(DatatablePageImpl<DeliveryMethodEntity> page, Prop prop) {
        page.addOptions("deliveryMethodName", getDeliveryOptions(prop), "label", "value", false);
        setCoutryOptions(page, prop);
        setCurrencyOptions(page);
    }

    public final List<LabelValue> getDeliveryOptions(Prop prop) {
        List<LabelValue> deliveryMethodOptions = new ArrayList<>();

        for(BaseDeliveryMethod dm : deliveryMethods) {
            LabelValue option = dm.getDeliveryMethodOption(prop);
            if(option != null) deliveryMethodOptions.add(option);
        }

        return deliveryMethodOptions;
    }

    private void setCoutryOptions(DatatablePageImpl<DeliveryMethodEntity> page, Prop prop) {
        String[] supprotedCountries = Constants.getArray("basketInvoiceSupportedCountries");


        for (String countryCode : supprotedCountries) {
            page.addDefaultOption("supportedCountries", prop.getText( BaseDeliveryMethod.COUNTRY_KEY_PREFIX + countryCode ), countryCode);
        }
    }

    private void setCurrencyOptions(DatatablePageImpl<DeliveryMethodEntity> page) {
        for(String currency : Constants.getString("supportedCurrencies").split(",") ) {
            page.addDefaultOption("currency", currency, currency);
        }
    }
}
