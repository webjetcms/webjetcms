package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.tags.CurrencyTag;

@RestController
@RequestMapping("/admin/rest/eshop/basket-payments")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
@Datatable
public class BasketInvoicePaymentRestController extends DatatableRestControllerV2<BasketInvoicePaymentEntity, Long> {

    private final BasketInvoicePaymentsRepository basketInvoicePaymentRepository;
    private final BasketInvoicesRepository basketInvoicesRepository;
    private static final BigDecimal MIN_PAYED_PRICE = BigDecimal.valueOf(0.01);

    @Autowired
    public BasketInvoicePaymentRestController(BasketInvoicePaymentsRepository basketInvoicePaymentRepository, BasketInvoicesRepository basketInvoicesRepository) {
        super(basketInvoicePaymentRepository);
        this.basketInvoicePaymentRepository = basketInvoicePaymentRepository;
        this.basketInvoicesRepository = basketInvoicesRepository;
    }

    @Override
    public Page<BasketInvoicePaymentEntity> getAllItems(Pageable pageable) {
        long invoiceId = getInvoiceId();
        if(invoiceId < 1)  return new PageImpl<>(new ArrayList<>());
        BasketInvoiceEntity invoice = basketInvoicesRepository.findFirstByIdAndDomainId(invoiceId, CloudToolsForCore.getDomainId()).orElse(null);
        if(invoice == null) return new PageImpl<>(new ArrayList<>());
        return basketInvoicePaymentRepository.findAllByInvoiceId(invoiceId, pageable);
    }

    @Override
    public BasketInvoicePaymentEntity getOne(@PathVariable("id") long id) {
        if(id < 1) {
            BasketInvoicePaymentEntity entity = new BasketInvoicePaymentEntity();
            entity.setCreateDate(new Date());
            return entity;
        } else {
            return super.getOne(id);
        }
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, BasketInvoicePaymentEntity> target, Identity currentUser, Errors errors, Long id, BasketInvoicePaymentEntity entity) {
        if(entity.getPayedPrice() == null || entity.getPayedPrice().compareTo(MIN_PAYED_PRICE) < 0)
            errors.rejectValue("errorField.payedPrice", "", getProp().getText("components.basket.invoice.payments.price_err"));
    }

    @Override
    public void beforeSave(BasketInvoicePaymentEntity entity) {
        long basketInvoiceId;
        if(entity.getId() != null && entity.getId() > 0) {
            //EDIT
            basketInvoiceId = entity.getInvoiceId();
        } else {
            //CREATE
            basketInvoiceId = getInvoiceId();
            if(basketInvoiceId < 1) throwError("Something went wrong.");
        }

        BasketInvoiceEntity invoice = basketInvoicesRepository.findFirstByIdAndDomainId(basketInvoiceId, CloudToolsForCore.getDomainId()).orElse(null);
        if(invoice==null) {
            throwError("Something went wrong.");
            return;
        }

        entity.setClosedDate(new Date());

        //Check for null date value
        if(entity.getCreateDate() == null) entity.setCreateDate(new Date());

        //Need null time part
        Calendar cal = Calendar.getInstance();
        cal.setTime(entity.getCreateDate());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        entity.setCreateDate(cal.getTime());

        BigDecimal totalPriceVat = invoice.getTotalPriceVat(); //Full price to pay
        BigDecimal totalPayedPrice = invoice.getTotalPayedPrice(); //Already payed

        if(entity.getId() != null && entity.getId() > 0) {
            //Update
            BasketInvoicePaymentEntity originalPayment = super.getOne(entity.getId());
            //Full price to pay (with updated price) cant be bigger than price to pay

            BigDecimal priceOvervalue = totalPayedPrice.subtract(originalPayment.getPayedPrice()).add(entity.getPayedPrice());
            if( priceOvervalue.compareTo(totalPriceVat) > 0) throwError(getProp().getText("components.basket.invoice.payments.price_over_err"));
        } else {
            //Insert

            BigDecimal priceOvervalue = totalPayedPrice.add(entity.getPayedPrice());
            if( priceOvervalue.compareTo(totalPriceVat) > 0) throwError(getProp().getText("components.basket.invoice.payments.price_over_err"));

            entity.setConfirmed(true);
            entity.setInvoiceId(basketInvoiceId);
        }
    }

    @Override
    public void afterSave(BasketInvoicePaymentEntity entity, BasketInvoicePaymentEntity saved) {
        Optional<BasketInvoiceEntity> basketInvoice = basketInvoicesRepository.findById((long) entity.getInvoiceId());

        if(basketInvoice.isPresent()) {
            BigDecimal totalPriceVat = basketInvoice.get().getTotalPriceVat();
            BigDecimal totalPayedPrice = basketInvoice.get().getTotalPayedPrice();

            if(CurrencyTag.formatNumber(totalPriceVat).equals(CurrencyTag.formatNumber(totalPayedPrice)))
                basketInvoice.get().setStatusId(BasketInvoiceEntity.INVOICE_STATUS_PAID);
            else if(totalPayedPrice.compareTo(BigDecimal.valueOf(0)) > 0)
                basketInvoice.get().setStatusId(BasketInvoiceEntity.INVOICE_STATUS_PARTIALLY_PAID);
            else
                basketInvoice.get().setStatusId(BasketInvoiceEntity.INVOICE_STATUS_NEW);

            basketInvoicesRepository.save(basketInvoice.get());
        }
    }

    @Override
    public void afterDelete(BasketInvoicePaymentEntity entity, long id) {
        Optional<BasketInvoiceEntity> basketInvoice = basketInvoicesRepository.findById((long) entity.getInvoiceId());

        if(basketInvoice.isPresent()) {
            BigDecimal totalPriceVat = basketInvoice.get().getTotalPriceVat();
            BigDecimal totalPayedPrice = basketInvoice.get().getTotalPayedPrice();

            if(CurrencyTag.formatNumber(totalPriceVat).equals(CurrencyTag.formatNumber(totalPayedPrice)))
                basketInvoice.get().setStatusId(BasketInvoiceEntity.INVOICE_STATUS_PAID);
            else if(totalPayedPrice.compareTo( BigDecimal.valueOf(0) ) > 0)
                basketInvoice.get().setStatusId(BasketInvoiceEntity.INVOICE_STATUS_PARTIALLY_PAID);
            else
                basketInvoice.get().setStatusId(BasketInvoiceEntity.INVOICE_STATUS_NEW);

            basketInvoicesRepository.save(basketInvoice.get());
        }
    }

    private final long getInvoiceId() {
        return Tools.getLongValue(getRequest().getParameter("invoiceId"), -1);
    }
}
