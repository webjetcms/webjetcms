package sk.iway.iwcm.components.basket.payment_methods.rest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import cz.gopay.api.v3.GPClientException;
import cz.gopay.api.v3.IGPConnector;
import cz.gopay.api.v3.impl.apacheclient.HttpClientGPConnector;
import cz.gopay.api.v3.model.common.Currency;
import cz.gopay.api.v3.model.payment.BasePayment;
import cz.gopay.api.v3.model.payment.BasePaymentBuilder;
import cz.gopay.api.v3.model.payment.Payment;
import cz.gopay.api.v3.model.payment.Payment.SessionState;
import cz.gopay.api.v3.model.payment.PaymentFactory;
import cz.gopay.api.v3.model.payment.PaymentResult;
import cz.gopay.api.v3.model.payment.PaymentResult.Result;
import cz.gopay.api.v3.model.payment.support.AdditionalParam;
import cz.gopay.api.v3.model.payment.support.ItemType;
import cz.gopay.api.v3.model.payment.support.OrderItem;
import cz.gopay.api.v3.model.payment.support.Payer;
import cz.gopay.api.v3.model.payment.support.PayerBuilder;
import cz.gopay.api.v3.model.payment.support.PayerContact;
import cz.gopay.api.v3.model.payment.support.PaymentInstrument;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.InvoicePaymentStatus;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodEntity;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodRepository;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState.PaymentStatus;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState.RefundationStatus;
import sk.iway.iwcm.components.basket.supprot.FieldMapAttr;
import sk.iway.iwcm.editor.FieldType;

@Service
@PaymentMethod(
    nameKey = "apps.eshop.payments.go_pay",
    fieldMap = {
        @FieldMapAttr(fieldAlphabet = 'A', fieldType = FieldType.TEXT, fieldLabel = "apps.eshop.payments.client_id", isRequired = true),
        @FieldMapAttr(fieldAlphabet = 'B', fieldType = FieldType.TEXT, fieldLabel = "apps.eshop.payments.secret", isRequired = true),
        @FieldMapAttr(fieldAlphabet = 'C', fieldType = FieldType.TEXT, fieldLabel = "apps.eshop.payments.url", isRequired = true),
        @FieldMapAttr(fieldAlphabet = 'D', fieldType = FieldType.NUMBER, fieldLabel = "apps.eshop.payments.go_id", isRequired = true),
        @FieldMapAttr(fieldAlphabet = 'E', fieldType = FieldType.NUMBER, fieldLabel = "components.basket.invoice_payments.price", isRequired = true, defaultValue = "0"),
        @FieldMapAttr(fieldAlphabet = 'F', fieldType = FieldType.NUMBER, fieldLabel = "components.basket.invoice_payments.vat", isRequired = true, defaultValue = "0"),
        @FieldMapAttr(fieldAlphabet = 'G', fieldType = FieldType.TEXT, fieldLabel = "components.basket.invoice_payments.gopay.orderDescription", isRequired = false, defaultValue = ""),
        @FieldMapAttr(fieldAlphabet = 'H', fieldType = FieldType.QUILL, fieldLabel = "components.payment_methods.mmoney_transfer_note", isRequired = false),
        @FieldMapAttr(fieldAlphabet = 'I', fieldType = FieldType.BOOLEAN_TEXT, fieldLabel = "components.payment_methods.allow_admin_edit", isRequired = false, defaultValue = "false"),
})
public class GoPayService extends BasePaymentMethod {
    //Scope only for craeting payments
    private static final String SCOPE_CREATE = "payment-create";
    //Scope that allows all actions
    private static final String SCOPE_ALL = "payment-all";

    private static final String PAYMENT_DATETIME = "paymentDateTime";

    private final IGPConnector getConnector (PaymentMethodEntity paymentMethod, String scope) {
        IGPConnector connector = HttpClientGPConnector.build( paymentMethod.getFieldC() );

        try{
            connector.getAppToken(paymentMethod.getFieldA(), paymentMethod.getFieldB(), scope);
            return connector;

        } catch(GPClientException ex) {
            Logger.error(ex);
            return null;
        }
    }

    private final PayerContact getPayerContact(BasketInvoiceEntity bie) {
        PayerContact contact = new PayerContact();
        contact.setFirstName( bie.getDeliveryName() );
        contact.setLastName( bie.getDeliverySurName() );
        contact.setEmail( bie.getContactEmail() );
        contact.setPhoneNumber( bie.getContactPhone() );
        return contact;
    }

    private final Payer getPayer(BasketInvoiceEntity bie) {
        return
            new PayerBuilder()
                .withContactData( getPayerContact(bie) ) //Set user contact data
                .withAllowedPaymentInstruments(
                    Arrays.asList(
                        PaymentInstrument.PAYMENT_CARD,
                        PaymentInstrument.APPLE_PAY,
                        PaymentInstrument.BANK_ACCOUNT,
                        PaymentInstrument.PRSMS,
                        PaymentInstrument.MPAYMENT,
                        PaymentInstrument.COUPON,
                        PaymentInstrument.PAYSAFECARD,
                        PaymentInstrument.GOPAY,
                        PaymentInstrument.PAYPAL,
                        PaymentInstrument.BITCOIN,
                        PaymentInstrument.MASTERPASS,
                        PaymentInstrument.GPAY,
                        PaymentInstrument.ACCOUNT,
                        PaymentInstrument.CLICK_TO_PAY,
                        PaymentInstrument.TWISTO,
                        PaymentInstrument.SKIPPAY
                    )
                )
                .build();
    }

    private final BasePayment getPayment(BasketInvoiceEntity bie, String returnUrl, PaymentMethodEntity paymentMethod, BasketInvoiceItemsRepository biir) {
        Date currentDate = new Date();

        //Prepare payment builder
        BasePaymentBuilder payment =
            PaymentFactory.createBasePaymentBuilder()
                .addAdditionalParameter(PAYMENT_DATETIME, currentDate.getTime() + "") // Creating payment datetime
                .withCallback(returnUrl, "")
                .payer( getPayer(bie) )
                .inLang( bie.getUserLng().toUpperCase() )
                .toEshop( Long.valueOf(paymentMethod.getFieldD()) );

        //Add itemns to payment
        BigDecimal totalPriceToPay = new BigDecimal(0);
        for(BasketInvoiceItemEntity item : biir.findAllByInvoiceIdAndDomainId(bie.getId(), CloudToolsForCore.getDomainId())) {
            totalPriceToPay = totalPriceToPay.add( item.getItemPriceVatQty() );
            payment.addItem(
                basketItemToOrderItem(item)
            );
        }

        //Retrun builded payment
        return
            payment
                .order(
                    bie.getId().toString(),
                    bigDecimalPriceToLong( totalPriceToPay ),
                    Currency.getByCode( bie.getCurrency().toUpperCase() ),
                    paymentMethod.getFieldG()
                ).build();
    }

    @Override
    public String getPaymentResponse(Long invoiceId, String returnUrl, HttpServletRequest request) {
        BasketInvoicesRepository bir = Tools.getSpringBean("basketInvoicesRepository", BasketInvoicesRepository.class);
        BasketInvoiceItemsRepository biir = Tools.getSpringBean("basketInvoiceItemsRepository", BasketInvoiceItemsRepository.class);

        BasketInvoiceEntity bie = bir.findById(invoiceId).orElse(null);
        if(bie == null) return adminLogError("BasketInvoiceEntity wasnt found by given invoiceId.", request);

        PaymentMethodRepository pmr = Tools.getSpringBean(PAYMENT_METHOD_REPOSITORY, PaymentMethodRepository.class);
        PaymentMethodEntity paymentMethod = pmr.findByPaymentMethodNameAndDomainId(this.getClass().getName(), CloudToolsForCore.getDomainId());
        if(paymentMethod == null) return adminLogError("PaymentMethodEntity " + this.getClass().getName() + " wasnt found in DB.", request);

        //Prepare connector
        IGPConnector connector = getConnector(paymentMethod, SCOPE_CREATE);
        if(connector == null) return adminLogError("Cant init connector for GoPay.", request);

        //Prepare payment
        BasePayment payment = getPayment(bie, returnUrl, paymentMethod, biir);
        if(payment == null) return adminLogError("Cant init payment for GoPay.", request);

        try {
            //Performa payment
            Payment result = connector.createPayment(payment);

            //This is return url with payment
            StringBuilder sb = new StringBuilder();
            sb.append("<span>Vašu objednáku môžete zaplatiť tu :</span> <a href=\"").append(result.getGwUrl()).append("\"> <button class=\"btn btn-primary\">Zaplatiť</button> </a> ");
            sb.append(paymentMethod.getFieldH());
            return sb.toString();
        } catch (GPClientException e) {
            adminLogError("GoPay payment failed : " + e.extractMessage(), request);
            return "<div class=\"alert alert-danger\"> <span> Platobnú metódu sa nepodarilo inicializovať ! </span> </div>";
        }
    }

    @Override
    protected PaymentState getPaymentState(HttpServletRequest request) {
        Long paymentId = Tools.getLongValue(request.getParameter("id"), -1);
        if(paymentId < 1) {
            adminLogError("Invalid request parameter id (real payment id).", request);
            return null;
        }

        PaymentMethodRepository pmr = Tools.getSpringBean(PAYMENT_METHOD_REPOSITORY, PaymentMethodRepository.class);
        PaymentMethodEntity paymentMethod = pmr.findByPaymentMethodNameAndDomainId(this.getClass().getName(), CloudToolsForCore.getDomainId());
        if(paymentMethod == null) {
            adminLogError("PaymentMethodEntity was not found.", request);
            return null;
        }

        //Prepare connector
        IGPConnector connector = getConnector(paymentMethod, SCOPE_ALL);
        if(connector == null) {
            adminLogError("Preparing connector failed.", request);
            return null;
        }

        try {
            //Get paiment and check status
            Payment payment = connector.paymentStatus(paymentId);
            SessionState sessionState = payment.getState();

            PaymentState paymentState = new PaymentState();
            paymentState.setPaymentId( paymentId.toString() );

            if(sessionState == SessionState.PAID) {
                //Payment is paid - all good
                paymentState.setStatus(PaymentStatus.SUCCESS);
                paymentState.setPaymentDescription("GoPay payment state : " + sessionState.name());
            } else {
                //Payment is not paid - something went wrong with user or bank
                paymentState.setStatus(PaymentStatus.FAIL);
                paymentState.setPaymentDescription("GoPay payment state : " + sessionState.name() + " , substate : " + payment.getSubState());
            }

            //Find param with datetime of creating payment
            Date paymentDateTime = new Date();
            for(AdditionalParam param : payment.getAdditionalParams()) {
                if(param.getName().equalsIgnoreCase(PAYMENT_DATETIME))
                    paymentDateTime = new Date( Long.parseLong(param.getValue()) );
            }
            paymentState.setPaymentDateTime( paymentDateTime );

            return paymentState;
        } catch (GPClientException e) {
            adminLogError("GoPay state checking failed : " + e.extractMessage(), request);
            return null;
        }
    }

    @Override
    protected RefundationState doPaymentRefund(BigDecimal refundAmount, BasketInvoicePaymentEntity paymentEntity, HttpServletRequest request) {
        Long paymentId = Tools.getLongValue(paymentEntity.getRealPaymentId(), -1L);
        if(paymentId < 1) return new RefundationState(RefundationStatus.ERROR, REFUNDATION_FAILED);

        PaymentMethodRepository pmr = Tools.getSpringBean(PAYMENT_METHOD_REPOSITORY, PaymentMethodRepository.class);
        PaymentMethodEntity paymentMethod = pmr.findByPaymentMethodNameAndDomainId(this.getClass().getName(), CloudToolsForCore.getDomainId());
        if(paymentMethod == null) {
            adminLogError("PaymentMethodEntity was not found.", request);
            return new RefundationState(RefundationStatus.ERROR, REFUNDATION_FAILED);
        }

        //Prepare connector
        IGPConnector connector = getConnector(paymentMethod, SCOPE_ALL);
        if(connector == null) {
            adminLogError("Preparing connector failed.", request);
            return new RefundationState(RefundationStatus.ERROR, REFUNDATION_FAILED);
        }

        try {
            //Get paiment and check status
            Payment payment = connector.paymentStatus(paymentId);
            SessionState sessionState = payment.getState();

            if(sessionState == SessionState.PAID || sessionState == SessionState.PARTIALLY_REFUNDED) {

                Long availableAmount = payment.getAmount();
                Long refundAmountLong = bigDecimalPriceToLong(refundAmount);

                if(availableAmount < refundAmountLong) {
                    adminLogError("Refund amount is higher than available amount.", request);
                    return new RefundationState(RefundationStatus.ERROR, REFUNDATION_AMOUNT_TOO_HIGH);
                }

                //Perform refund
                PaymentResult result = connector.refundPayment(paymentId, refundAmountLong);

                if(result.getResult() == Result.FAILED) {
                    return new RefundationState(RefundationStatus.FAIL, REFUNDATION_FAILED, result.getDescription());
                }

                RefundationState refundState = new RefundationState(RefundationStatus.SUCCESS, REFUNDATION_SUCCESS, result.getDescription());
                //Try get status of refunded payment
                try {
                    Payment refundedPayment = connector.paymentStatus(paymentId);
                    SessionState refundSessionState = refundedPayment.getState();
                    if(refundSessionState == SessionState.REFUNDED) {
                        refundState.setStatusAfterRefund(InvoicePaymentStatus.REFUNDED);
                    } else if(refundSessionState == SessionState.PARTIALLY_REFUNDED) {
                        refundState.setStatusAfterRefund(InvoicePaymentStatus.PARTIALLY_REFUNDED);
                    } else if(refundSessionState == SessionState.PAID) {
                        refundState.setStatusAfterRefund(InvoicePaymentStatus.SUCCESS);
                    } else {
                        refundState.setStatusAfterRefund(InvoicePaymentStatus.UNKNOWN);
                    }
                } catch (GPClientException e) {
                    adminLogError("GoPay refundation, failed get payment status after refund : " + e.extractMessage(), request);
                    refundState.setStatusAfterRefund(InvoicePaymentStatus.UNKNOWN);
                }

                return refundState;
            } else {
                return new RefundationState(RefundationStatus.ERROR, NON_REFUNDABLE_PAYMENT);
            }

        } catch (GPClientException e) {
            adminLogError("GoPay refundation failed : " + e.extractMessage(), request);
            return new RefundationState(RefundationStatus.FAIL, REFUNDATION_FAILED, e.extractMessage());
        }
    }

    // SUPPORT THINGS
    private final OrderItem basketItemToOrderItem(BasketInvoiceItemEntity item) {
        OrderItem orderItem = new OrderItem();
        orderItem.setName(item.getTitle());
        orderItem.setAmount( item.getItemPrice().longValue() );
        orderItem.setVatRate(item.getItemVat());
        orderItem.setCount( item.getItemQty().longValue() );
        orderItem.setItemType( ItemType.ITEM );
        return orderItem;
    }

    private final Long bigDecimalPriceToLong(BigDecimal price) {
        return price.multiply(new BigDecimal(100)).longValue();
    }

    @Override
    protected BasketInvoiceItemEntity getPaymentMethodCost(BasketInvoiceItemEntity entity, PaymentMethodEntity paymentMethod) {
        if(paymentMethod == null) return null;
        entity.setItemPrice( new BigDecimal(paymentMethod.getFieldE()) );
        entity.setItemQty(1);
        entity.setItemVat( Integer.valueOf(paymentMethod.getFieldF()) );
        return entity;
    }

    @Override
    protected boolean isEditableByAdmin(PaymentMethodEntity paymentMethod) {
        if(paymentMethod == null) return false;
        return Tools.getBooleanValue(paymentMethod.getFieldI(), false);
    }
}