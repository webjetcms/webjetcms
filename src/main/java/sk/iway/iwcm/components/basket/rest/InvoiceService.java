package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;


public class InvoiceService {

    private InvoiceService() {
        // private constructor to hide the implicit public one
    }

    public static String validatePayment(BasketInvoicePaymentEntity entity, long requestInvoiceId, BasketInvoicePaymentsRepository bipr, BasketInvoicesRepository bir, BasketInvoiceItemsRepository biir) {
        // If payment isnt confirmed dont check -> doesnt mataer because this payment do not count
        if(Tools.isFalse(entity.getConfirmed())) return null;

        long basketInvoiceId;
        if(entity.getId() != null && entity.getId() > 0) {
            //EDIT
            basketInvoiceId = entity.getInvoiceId();
        } else {
            //CREATE
            basketInvoiceId = requestInvoiceId;
            if(basketInvoiceId < 1) return "html_area.insert_image.error_occured";
        }

        BasketInvoiceEntity invoice = bir.findFirstByIdAndDomainId(basketInvoiceId, CloudToolsForCore.getDomainId()).orElse(null);
        if(invoice==null)
            return "html_area.insert_image.error_occured";

        BigDecimal totalPriceVat = ProductListService.getPriceToPay(invoice.getId(), biir);
        BigDecimal totalPayedPrice = ProductListService.getPayedPrice(invoice.getId(), bipr);

        if(entity.getId() != null && entity.getId() > 0) {
            //Update
            BasketInvoicePaymentEntity originalPayment = bipr.getReferenceById(entity.getId());
            BigDecimal toSubstract = Tools.isTrue(originalPayment.getConfirmed()) ? originalPayment.getPayedPrice() : BigDecimal.ZERO;
            //Full price to pay (with updated price) cant be bigger than price to pay
            BigDecimal priceOvervalue = totalPayedPrice.subtract(toSubstract).add(entity.getPayedPrice());
            if( priceOvervalue.compareTo(totalPriceVat) > 0) return "components.basket.invoice.payments.price_over_err";
            if( priceOvervalue.compareTo(BigDecimal.ZERO) < 0) return "components.invoice_payment.over_refund_err";
        } else {
            //Insert
            BigDecimal priceOvervalue = totalPayedPrice.add(entity.getPayedPrice());
            if( priceOvervalue.compareTo(totalPriceVat) > 0) return "components.basket.invoice.payments.price_over_err";
            if( priceOvervalue.compareTo(BigDecimal.ZERO) < 0) return "components.invoice_payment.over_refund_err";
        }

        return null;
    }
}