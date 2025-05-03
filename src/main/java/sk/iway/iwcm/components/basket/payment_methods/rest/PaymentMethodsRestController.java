package sk.iway.iwcm.components.basket.payment_methods.rest;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodEntity;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/eshop/payment-methods")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
@Datatable
public class PaymentMethodsRestController extends DatatableRestControllerV2<PaymentMethodEntity, Long> {

    private final PaymentMethodsService paymentMethodsService;

    @Autowired
    public PaymentMethodsRestController(PaymentMethodsService paymentMethodsService) {
        super(null);
        this.paymentMethodsService = paymentMethodsService;
    }

    @Override
    public Page<PaymentMethodEntity> getAllItems(Pageable pageable) {
        return new DatatablePageImpl<>( paymentMethodsService.getAllPaymentMethods(getProp()) );
    }

    @Override
    public PaymentMethodEntity getOneItem(long id) {
        return paymentMethodsService.getPaymentMethod(id, getProp());
    }

    @Override
    public PaymentMethodEntity editItem(PaymentMethodEntity entity, long id) {
        return paymentMethodsService.savePaymentMethod(entity, getProp());
    }

    @Override
    public boolean deleteItem(PaymentMethodEntity entity, long id) {
        paymentMethodsService.deletePaymentMethod(id);
        setForceReload(true);
        return true;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, PaymentMethodEntity> target, Identity user, Errors errors, Long id, PaymentMethodEntity entity) {
        if ("remove".equals(target.getAction())) return;

        //Optional fields CAN'T validate required fields, we need to do it manually
        paymentMethodsService.validateEditorValues(entity, errors, getProp());
    }
}
