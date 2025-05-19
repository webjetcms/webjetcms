package sk.iway.iwcm.components.basket.payment_methods.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.validation.Errors;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.InvoicePaymentStatus;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentFieldMapAttr;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodEntity;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodRepository;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState.PaymentStatus;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UsersDB;

/**
 * Contain logic for payment methods. Once class extends this class and is annotated with PaymentMethod, it will be considered as payment method.
 */
public abstract class BasePaymentMethod {

    private static final Character LAST_ALPHABET = 'L';

    private static final String STATUS_CONFIGURED_TEXT = "apps.eshop.payments.status_configured";
    private static final String STATUS_NOT_CONFIGURED_TEXT = "apps.eshop.payments.status_not_configured";

    public static final String MERCHANT_ID_TEXT = "apps.eshop.payments.merchant_id";
    public static final String PRIVATE_KEY_TEXT = "apps.eshop.payments.private_key";
    public static final String HASH_PRIVATE_KEY_TEXT = "apps.eshop.payments.hash_private_key";
    public static final String BANK_URL_TEXT = "apps.eshop.payments.bank_url";
    public static final String CONST_SYMBOL_TEXT = "apps.eshop.payments.constant_symbol";
    public static final String NOTIF_EMAIL_TEXT = "apps.eshop.payments.notif_email";
    public static final String ENABLE_TATRA_PAY_TEXT = "apps.eshop.payments.enable_tatra_pay";
    public static final String ENABLE_CARD_PAY_TEXT = "apps.eshop.payments.enable_card_pay";

    public static final String REFUNDATION_TITLE = "components.invoice.payment_refundation_title";
    public static final String REFUNDATION_SUCCESS = "components.invoice.payment_refundation.success";
    public static final String NON_REFUNDABLE_METHOD = "components.invoice.payment_refundation.non_refundable_method";
    public static final String REFUNDATION_FAILED = "components.invoice.payment_refundation.failed";
    public static final String REFUNDATION_AMOUNT_TOO_HIGH = "components.invoice.payment_refundation.amount_too_high";
    public static final String REFUNDATION_AMOUNT_TOO_LOW = "components.invoice.payment_refundation.amount_too_low";
    public static final String NON_REFUNDABLE_PAYMENT = "components.invoice.payment_refundation.non_refundable_payment";
    public static final String CANT_REFUND_NOT_CONFIRMED = "components.invoice.payment_refundation.not_confirmed";
    public static final String CANT_REFUND_REFUNDATION = "components.invoice.payment_refundation.is_refundation";

    protected static final String PAYMENT_METHOD_REPOSITORY = "paymentMethodRepository";

    /**
     * Return encrypted given key.
     * @param key
     * @return
     */
    protected String encryptKey(String key) {
        if(Tools.isAnyEmpty(key) == true) return null;

        try {
            Password password = new Password();
            return password.encrypt(key);
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * Return prepared payment method, with set fields by annotation and status.
     *
     * @param id
     * @param paymentMethodRepositor
     * @param action
     * @param prop
     * @return
     */
    protected PaymentMethodEntity getPayment(Long id, PaymentMethodRepository paymentMethodRepositor, ProcessItemAction action, Prop prop) {
        PaymentMethod annotation = this.getClass().getAnnotation(PaymentMethod.class);
        if(annotation == null) return null;

        PaymentMethodEntity paymentMethod = paymentMethodRepositor.findByPaymentMethodNameAndDomainId(this.getClass().getName(), CloudToolsForCore.getDomainId());
        if(paymentMethod == null) {
            paymentMethod = new PaymentMethodEntity();
            //No need to check status, if it's not in DB, it's not configured
            paymentMethod.setStatus( prop.getText(STATUS_NOT_CONFIGURED_TEXT) );
        } else {
            //Check status
            if( isPaymentMethodConfigured(annotation, paymentMethod) == true ) {
                paymentMethod.setStatus( prop.getText(STATUS_CONFIGURED_TEXT) );
            } else {
                paymentMethod.setStatus( prop.getText(STATUS_NOT_CONFIGURED_TEXT) );
            }
        }

        paymentMethod.setId(id);
        //Always set method name from annotation.paymentMethodName, value in DB serve just for identification
        paymentMethod.setPaymentMethodName( prop.getText(annotation.nameKey()) );

        //Prepare fields only if it's GETONE action (in table view, we cant see this fields anyway)
        if(ProcessItemAction.GETONE.equals(action) == true) {
            preparePayment(annotation, paymentMethod, prop);
        }

        return paymentMethod;
    }

    /**
     * Save payment method. If required fields by annotation are not set, it will not be saved.
     *
     * @param paymentMethod
     * @param paymentMethodRepositor
     * @param prop
     * @return
     */
    protected PaymentMethodEntity savePaymentMethod(PaymentMethodEntity paymentMethod, PaymentMethodRepository paymentMethodRepositor, Prop prop) {
        PaymentMethod annotation = this.getClass().getAnnotation(PaymentMethod.class);
        if(annotation == null) return null;

        beforeSave(paymentMethod, prop);

        //If required fields are not set, we cant save it
        if( isPaymentMethodConfigured(annotation, paymentMethod) == false ) return null;

        //We show to user our 'fake' id what is in nutshell just index+1 of list where are all payment methods
        Long fakeId = paymentMethod.getId();
        Long dbId = paymentMethodRepositor.getPymentMethodId(this.getClass().getName(), CloudToolsForCore.getDomainId());

        //While saving, we must use real DB id
        paymentMethod.setId(dbId);
        //While saving we must use real DB method name identifier
        paymentMethod.setPaymentMethodName( this.getClass().getName() );
        paymentMethod.setDomainId(CloudToolsForCore.getDomainId());
        paymentMethodRepositor.save(paymentMethod);

        //Set back our fake ID, fake method name and set status
        paymentMethod.setId(fakeId);
        paymentMethod.setPaymentMethodName( prop.getText(annotation.nameKey()) );
        paymentMethod.setStatus( prop.getText(STATUS_CONFIGURED_TEXT) );

        return paymentMethod;
    }

    /**
     * Clear payment method configuration by removing configured payment method columns from DB.
     */
    protected void clearPaymentMethodConfiguration(PaymentMethodRepository paymentMethodRepositor) {
        paymentMethodRepositor.deleteByPaymentMethodNameAndDomainId(this.getClass().getName(), CloudToolsForCore.getDomainId());
    }

    @SuppressWarnings("all")
    protected void validateEditorValues(PaymentMethodEntity paymentMethod, Errors errors, Prop prop) {
        PaymentMethod annotation = this.getClass().getAnnotation(PaymentMethod.class);
        if(annotation == null) return;

        for(PaymentFieldMapAttr fieldMapAttr : annotation.fieldMap()) {
            if(fieldMapAttr.isRequired() == true) {
                String fieldValue = getPaymentFieldValue(paymentMethod, fieldMapAttr.fieldAlphabet());
                if(Tools.isAnyEmpty(fieldValue) == true) {
                    errors.rejectValue("errorField.field" + fieldMapAttr.fieldAlphabet(), null, prop.getText("javax.validation.constraints.NotBlank.message"));
                }
            }
        }
    }

    /**
     * Before save abstract method, to allow user do before save control or changes.
     *
     * @param paymentMethod
     * @param prop
     */
    protected void beforeSave(PaymentMethodEntity paymentMethod, Prop prop) {}

    /**
     * Initialize payment method fields by annotation. Set fields label, type. If field is not used, set it as NONE type.
     *
     * @param annotation
     * @param paymentMethod
     * @param prop
     */
    private void preparePayment(PaymentMethod annotation, PaymentMethodEntity paymentMethod, Prop prop) {
        BaseEditorFields pef = new BaseEditorFields();
        List<Field> fields = pef.getFields(paymentMethod, "payments", LAST_ALPHABET);

        for (char alphabet = 'A'; alphabet <= LAST_ALPHABET; alphabet++) {
            int index = alphabet - 'A';
            if(index < 0 || index > fields.size() - 1) continue;

            boolean found = false;
            for(PaymentFieldMapAttr fieldMapAttr : annotation.fieldMap()) {
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

        pef.setFieldsDefinition(fields);
        paymentMethod.setEditorFields(pef);
    }

    /**
     * Check if all required fields by annotation are not null or empty.
     *
     * @param annotation
     * @param paymentMethod
     * @return
     */
    private final boolean isPaymentMethodConfigured(PaymentMethod annotation, PaymentMethodEntity paymentMethod) {
        for(PaymentFieldMapAttr fieldMapAttr : annotation.fieldMap()) {
            if(fieldMapAttr.isRequired() == true) {
                String fieldValue = getPaymentFieldValue(paymentMethod, fieldMapAttr.fieldAlphabet());
                if(Tools.isAnyEmpty(fieldValue) == true) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if payment method is configured. Need to in DB and have all required fields set.
     * @param paymentMethodeed
     * @return
     */
    protected final boolean isPaymentMethodConfigured(PaymentMethodEntity paymentMethod) {
        PaymentMethod annotation = this.getClass().getAnnotation(PaymentMethod.class);
        if(annotation == null) return false;
        return isPaymentMethodConfigured(annotation, paymentMethod);
    }

    /**
     * Set payment method to map paymentMethods - if it's configured.
     * @param paymentMethod
     * @param paymentMethods
     * @param prop
     */
    protected final void setToMapIfConfigured(PaymentMethodEntity paymentMethod, Map<String, String> paymentMethods, Prop prop) {
        PaymentMethod annotation = this.getClass().getAnnotation(PaymentMethod.class);
        if(annotation == null) return;

        if(isPaymentMethodConfigured(annotation, paymentMethod) == true) {
            paymentMethods.put(paymentMethod.getPaymentMethodName(), prop.getText(annotation.nameKey()));
        }
    }

    /**
     * Get payment field value by field alphabet.
     * @param paymentMethod
     * @param fieldAlphabet
     * @return
     */
    private String getPaymentFieldValue(PaymentMethodEntity paymentMethod, char fieldAlphabet) {
        switch(fieldAlphabet) {
            case 'A': return paymentMethod.getFieldA();
            case 'B': return paymentMethod.getFieldB();
            case 'C': return paymentMethod.getFieldC();
            case 'D': return paymentMethod.getFieldD();
            case 'E': return paymentMethod.getFieldE();
            case 'F': return paymentMethod.getFieldF();
            case 'G': return paymentMethod.getFieldG();
            case 'H': return paymentMethod.getFieldH();
            case 'I': return paymentMethod.getFieldI();
            case 'J': return paymentMethod.getFieldJ();
            case 'K': return paymentMethod.getFieldK();
            case 'L': return paymentMethod.getFieldL();
            default: return null;
        }
    }

    protected String adminLogError(String message, HttpServletRequest request) { return adminLogError(this.getClass(), message, request); }
    public static String adminLogError(Class<?> currentClass, String message, HttpServletRequest request) {
        int userId = -1;
		Identity user = UsersDB.getCurrentUser(request);
        if(user != null) userId = user.getUserId();

        message = currentClass.getName() + " : \n" + message;
        Adminlog.add(Adminlog.TYPE_PAYMENT_GATEWAY, userId, message, -1, -1);
        return null;
    }

    protected Long adminLogErrorLong(String message, HttpServletRequest request) { return adminLogErrorLong(this.getClass(), message, request); }
    public static Long adminLogErrorLong(Class<?> currentClass, String message, HttpServletRequest request) {
        adminLogError(currentClass, message, request);
        return -1L;
    }

    protected PaymentState adminLogErrorState(Long invoiceId,String message, HttpServletRequest request) { return adminLogErrorState(this.getClass(), invoiceId, message, request); }
    public static PaymentState adminLogErrorState(Class<?> currentClass, Long invoiceId, String message, HttpServletRequest request) {
        adminLogError(currentClass, message, request);

        PaymentState state = new PaymentState();
        state.setStatus(PaymentStatus.ERROR);
        state.setInvoiceId(invoiceId);
        state.setPaymentDateTime(new Date());
        return state;
    }

    protected final Long saveBasketInvoicePayment(Long invoiceId, HttpServletRequest request) {
        BasketInvoicesRepository bir = Tools.getSpringBean("basketInvoicesRepository", BasketInvoicesRepository.class);
        BasketInvoiceEntity bie = bir.findById(invoiceId).orElse(null);
        if(bie == null) return adminLogErrorLong(this.getClass(), "BasketInvoiceEntity wasnt found by given invoiceId.", request);

        //BasketInvoicePaymentsRepository
        BasketInvoicePaymentEntity bipe = new BasketInvoicePaymentEntity();
        bipe.setId(-1L);
        bipe.setInvoiceId(invoiceId);
        bipe.setCreateDate(new Date());
        bipe.setPaymentMethod(this.getClass().getName());
        bipe.setPayedPrice(bie.getPriceToPayVat());
        bipe.setConfirmed(false);
        //Right know, we dont know if the payment will be successful or not
        bipe.setPaymentStatus(InvoicePaymentStatus.UNKNOWN.getCode());

        //Save payment
        BasketInvoicePaymentsRepository bpr = Tools.getSpringBean("basketInvoicePaymentsRepository", BasketInvoicePaymentsRepository.class);
        bipe = bpr.save(bipe);
        return bipe.getId();
    }

    protected final void removeInvoicePayment(Long paymentId) {
        BasketInvoicePaymentsRepository bpr = Tools.getSpringBean("basketInvoicePaymentsRepository", BasketInvoicePaymentsRepository.class);
        bpr.deleteById(paymentId);
    }

    /**
     * Prepare payment method and return some response to user LIKE payment instructions OR button to redirect to payment gateway.
     *
     * @param invoiceId
     * @param returnUrl
     * @param request
     * @return
     */
    protected abstract String getPaymentResponse(Long invoiceId, String returnUrl, HttpServletRequest request);

    /**
     * Get payemnt state after payment is done. Called right after user payed for order.
     * @param request
     * @return
     */
    protected abstract PaymentState getPaymentState(HttpServletRequest request);

    /**
     * Do refundation of payment. This payment can be partial or full. Returned RefundationState will contain status of refundation with some message if needed.
     * @param refundAmount
     * @param paymentEntity
     * @param request
     * @return
     */
    protected abstract RefundationState doPaymentRefund(BigDecimal refundAmount, BasketInvoicePaymentEntity paymentEntity, HttpServletRequest request);

    /**
     * Get payment method cost. This method will set item price, quantity and VAT.
     * @param entity
     * @param paymentMethod
     * @return
     */
    protected abstract BasketInvoiceItemEntity getPaymentMethodCost(BasketInvoiceItemEntity entity, PaymentMethodEntity paymentMethod);

    //KK
    protected abstract boolean isEditableByAdmin(PaymentMethodEntity paymentMethod);
}