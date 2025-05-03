package sk.iway.iwcm.components.basket.payment_methods.rest;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.InvoicePaymentStatus;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodEntity;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodRepository;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState.PaymentStatus;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState.RefundationStatus;
import sk.iway.iwcm.components.basket.rest.InvoiceService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Service for payment methods.
 */
@Service
public class PaymentMethodsService {

    private final List<BasePaymentMethod> basePaymentMethodsList;
    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodsService(List<BasePaymentMethod> basePaymentMethodsList, PaymentMethodRepository paymentMethodRepository) {
        //This is base list, contain every class that extends from BasePaymnetMethod (aka every configured payment method)
        this.basePaymentMethodsList = basePaymentMethodsList;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public List<PaymentMethodEntity> getAllPaymentMethods(Prop prop) {
        List<PaymentMethodEntity> allPaymentMethods = new ArrayList<>();
        for(int i = 0; i < basePaymentMethodsList.size(); i++) {
            PaymentMethodEntity paymentMethod = basePaymentMethodsList.get(i).getPayment(Long.valueOf(i + (long) 1), paymentMethodRepository, ProcessItemAction.GETALL, prop);
            if (paymentMethod != null) {
                allPaymentMethods.add(paymentMethod);
            }
        }
        return allPaymentMethods;
    }

    public PaymentMethodEntity getPaymentMethod(long id, Prop prop) {
        return basePaymentMethodsList.get((int) id - 1).getPayment(id, paymentMethodRepository, ProcessItemAction.GETONE, prop);
    }

    public PaymentMethodEntity savePaymentMethod(PaymentMethodEntity paymentMethod, Prop prop) {
        return basePaymentMethodsList.get( paymentMethod.getId().intValue() - 1 ).savePaymentMethod(paymentMethod, paymentMethodRepository, prop);
    }

    public void deletePaymentMethod(long id) {
        basePaymentMethodsList.get((int) id - 1).clearPaymentMethodConfiguration(paymentMethodRepository);
    }

    public void validateEditorValues(PaymentMethodEntity entity, Errors errors, Prop prop) {
        basePaymentMethodsList.get(entity.getId().intValue() - 1).validateEditorValues(entity, errors, prop);
    }

    public RefundationState refundPayment(BigDecimal refundAmount, BasketInvoicePaymentEntity paymentEntity, HttpServletRequest request) {
        //Check that payment is NOT REFUNDATION (refundation have negative price)
        if(paymentEntity.getPayedPrice().signum() == -1) return new RefundationState(RefundationStatus.ERROR, BasePaymentMethod.CANT_REFUND_REFUNDATION, null);
        //Check refundation amount
        if(refundAmount.signum() <= 0) return new RefundationState(RefundationStatus.ERROR, BasePaymentMethod.REFUNDATION_AMOUNT_TOO_LOW, null);
        //Check that payment is confirmed
        if(Tools.isFalse(paymentEntity.getConfirmed())) return new RefundationState(RefundationStatus.ERROR, BasePaymentMethod.CANT_REFUND_NOT_CONFIRMED, null);

        BasePaymentMethod paymentMethod = null;
        for(BasePaymentMethod bpm : basePaymentMethodsList) {
            if(paymentEntity.getPaymentMethod().equals( bpm.getClass().getName() )) {
                paymentMethod = bpm;
                break;
            }
        }
        if(paymentMethod == null) return new RefundationState(RefundationStatus.ERROR, BasePaymentMethod.REFUNDATION_FAILED, null);

        RefundationState result = paymentMethod.doPaymentRefund(refundAmount, paymentEntity, request);
        Prop prop = Prop.getInstance(request);

        //Create refundation record ONLY if refundation was successful OR refundation failed on bank side
        RefundationStatus status = result.getStatus();
        if(status == RefundationStatus.SUCCESS || status == RefundationStatus.FAIL) {
            BasketInvoicePaymentEntity refundation = new BasketInvoicePaymentEntity();
            refundation.setId(null);
            refundation.setInvoiceId(paymentEntity.getInvoiceId());
            refundation.setCreateDate(new Date());
            refundation.setClosedDate(new Date());
            refundation.setPayedPrice(refundAmount.multiply(new BigDecimal(-1)));
            refundation.setPaymentMethod(paymentEntity.getPaymentMethod());
            refundation.setRealPaymentId(paymentEntity.getRealPaymentId());

            StringBuilder sb = new StringBuilder();
            sb.append(prop.getText("components.invoice.payment_refundation_title"));
            sb.append(" id: ").append(Long.toString(paymentEntity.getId()));
            sb.append(" \n ").append(result.getBonusText());
            refundation.setPaymentDescription( sb.toString() );

            if(status == RefundationStatus.SUCCESS) {
                refundation.setConfirmed(true);
                refundation.setPaymentStatus( InvoicePaymentStatus.REFUND_SUCCESS.getCode() );
            } else {
                refundation.setConfirmed(false);
                refundation.setPaymentStatus( InvoicePaymentStatus.REFUND_FAIL.getCode() );
            }

            BasketInvoicePaymentsRepository bpr = Tools.getSpringBean("basketInvoicePaymentsRepository", BasketInvoicePaymentsRepository.class);
            bpr.save(refundation);

            if(status == RefundationStatus.SUCCESS) {
                //Update invoice status
                InvoiceService.updateInvoiceStatus((long) paymentEntity.getInvoiceId());

                //Update status of refunded payment
                paymentEntity.setPaymentStatus( result.getStatusAfterRefund().getCode() );
                bpr.save(paymentEntity);
            }
        }

        return result;
    }

    // STATIC version for FE use, where I cant Autowire this service
    public static final Map<String, String> getConfiguredPaymentMethodsMap(Prop prop) {
        PaymentMethodRepository pmr = Tools.getSpringBean("paymentMethodRepository", PaymentMethodRepository.class);
        List<PaymentMethodEntity> allMethods = pmr.findAllByDomainId(CloudToolsForCore.getDomainId());

        Map<String, String> configuredMethods = new HashMap<>();
        for(PaymentMethodEntity paymentMethod : allMethods) {
            try {
                BasePaymentMethod basePaymentMethod = getBasePaymentMethod(paymentMethod.getPaymentMethodName());
                if(basePaymentMethod == null) continue;
                basePaymentMethod.setToMapIfConfigured(paymentMethod, configuredMethods, prop);
             } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return configuredMethods;
    }

    public static final List<LabelValue> getConfiguredPaymentMethodsLabels(Prop prop) {
        Map<String, String> configuredMethods = getConfiguredPaymentMethodsMap(prop);
        List<LabelValue> labelValues = new ArrayList<>();
        for(Map.Entry<String, String> entry : configuredMethods.entrySet()) {
            labelValues.add(new LabelValue(entry.getValue(), entry.getKey()));
        }

        //TODO: add sort_priority to DB and sort by admin preferences
        //Sort by label
        labelValues.sort((o1, o2) -> {
            if(o1.getLabel() == null) return -1;
            if(o2.getLabel() == null) return 1;
            return o1.getLabel().compareToIgnoreCase(o2.getLabel());
        });

        return labelValues;
    }

    private static final BasePaymentMethod getBasePaymentMethod(String paymentMethod) throws Exception {
        if(Tools.isEmpty(paymentMethod)) return null;

        Class<?> pmClass = Class.forName(paymentMethod);

        //Check taht class is child of BasePaymentMethod
        if(BasePaymentMethod.class.isAssignableFrom(pmClass)) {
            //Get payment method class
            Constructor<?> constructor = pmClass.getConstructor();
            return (BasePaymentMethod) constructor.newInstance();
        }

        return null;
    }

    public static final boolean isPaymentMethodConfigured(String paymentMethod, Prop prop) {
        Map<String, String> configuredMethods = getConfiguredPaymentMethodsMap(prop);
        return configuredMethods.containsKey(paymentMethod);
    }

    public static final String getPaymentResponse(HttpServletRequest request) {
        try{
            //Get payment method
            BasePaymentMethod payment = getBasePaymentMethod(request.getAttribute("paymentMethod").toString());
            if(payment == null) return null;

            //Check attributes
            Long invoiceId = Tools.getLongValue(request.getAttribute("invoiceId").toString(), -1);
            String returnUrl = Tools.getStringValue(request.getAttribute("returnUrl").toString(), null);
            if(invoiceId < 1 || returnUrl == null) return payment.adminLogError("Invalid request parameters.", request);

            //Prepare return url
            String baseHref = Tools.getBaseHref(request);
            if(returnUrl.startsWith(baseHref) == false)
                returnUrl = baseHref + returnUrl;

            //Create in DB payment - need to add in return url
            Long paymentId = payment.saveBasketInvoicePayment(invoiceId, request);
            if(paymentId < 1) return null;

            //Add payment id to return url
            returnUrl = Tools.addParameterToUrl(returnUrl, "invoicePaymentId", Long.toString(paymentId));
            returnUrl = returnUrl.replace("&amp;", "&");

            //Prepare link
            String paymentLink = payment.getPaymentResponse(invoiceId, returnUrl, request);

            //If obtaining payment link failed - remove payment from DB
            if(Tools.isEmpty(paymentLink))
                payment.removeInvoicePayment(paymentId);

            return Tools.unescapeHtmlEntities( paymentLink );
        } catch(Exception e) {
            return BasePaymentMethod.adminLogError(PaymentMethodsService.class, "getPaymentResponse failed : " + e.getMessage(), request);
        }
    }

    public static final PaymentState handlePayment(HttpServletRequest request) {
        Long invoicePaymentId = Tools.getLongValue(request.getParameter("invoicePaymentId"), -1);
        if(invoicePaymentId < 1) return BasePaymentMethod.adminLogErrorState(PaymentMethodsService.class, -1L, "Invalid request parameter invoicePaymentId.", request);

        BasketInvoicePaymentsRepository bpr = Tools.getSpringBean("basketInvoicePaymentsRepository", BasketInvoicePaymentsRepository.class);
        BasketInvoicePaymentEntity bipe = bpr.findById(invoicePaymentId).orElse(null);

        if(bipe == null) {
            //Well not good - user payed but we cant find payment in DB ...
            return BasePaymentMethod.adminLogErrorState(PaymentMethodsService.class, invoicePaymentId, "BasketInvoicePaymentEntity was not found.", request);
        }

        try {
            //Get payment method
            BasePaymentMethod payment = getBasePaymentMethod( bipe.getPaymentMethod() );
            if(payment == null) return BasePaymentMethod.adminLogErrorState(PaymentMethodsService.class, invoicePaymentId, "Cant obtain paymentMethod : " + bipe.getPaymentMethod(), request);

            //Get state of payment
            PaymentState paymentState = payment.getPaymentState(request);
            if(paymentState == null) return BasePaymentMethod.adminLogErrorState(PaymentMethodsService.class, invoicePaymentId, "Cant obtain paymentState of paymentMethod : " + bipe.getPaymentMethod(), request);

            //Set invoice id
            paymentState.setInvoiceId(invoicePaymentId);

            PaymentStatus status = paymentState.getStatus();
            bipe.setClosedDate(new Date());
            bipe.setRealPaymentId( paymentState.getPaymentId() );
            bipe.setPaymentDescription( paymentState.getPaymentDescription() );

            if(status == PaymentStatus.SUCCESS) {
                bipe.setConfirmed(true);
                bipe.setPaymentStatus( InvoicePaymentStatus.SUCCESS.getCode() );
            } else {
                bipe.setConfirmed(false);
                bipe.setPaymentStatus( InvoicePaymentStatus.FAIL.getCode() );
            }

            //Save payment info
            bpr.save(bipe);

            //If payment was successful
            if(status == PaymentStatus.SUCCESS) {
                //Update invoice status
                InvoiceService.updateInvoiceStatus(bipe.getInvoiceId());
            }

            return paymentState;

        } catch(Exception e) {
            return BasePaymentMethod.adminLogErrorState(PaymentMethodsService.class, invoicePaymentId, "handlePayment failed : " + e.getMessage(), request);
        }
    }

    /**
     * From given invoice entity extract payment method and create invoice item of this payment method.
     *
     * @param invoiceEntity
     * @param request
     * @return return id of new created invoice item (aka payment) or -1 if failed
     */
    public static int createPaymentInvoiceItemAndReturnId(BasketInvoiceEntity invoiceEntity, HttpServletRequest request) {
        if(invoiceEntity == null) return -1;

        if(Tools.isEmpty(invoiceEntity.getPaymentMethod())) return -1;

        PaymentMethodRepository pmr = Tools.getSpringBean("paymentMethodRepository", PaymentMethodRepository.class);
        PaymentMethodEntity paymentMethod = pmr.findByPaymentMethodNameAndDomainId(invoiceEntity.getPaymentMethod(), CloudToolsForCore.getDomainId());
        if(paymentMethod == null) {
            BasePaymentMethod.adminLogError(PaymentMethodsService.class, "PaymentMethodEntity was not found.", request);
            return -1;
        }

        try {
            BasePaymentMethod payment = getBasePaymentMethod(invoiceEntity.getPaymentMethod());
            if(payment == null) return -1;

            BasketInvoiceItemEntity item = new BasketInvoiceItemEntity();
            item.setId(null);
            item.setBrowserId(invoiceEntity.getBrowserId());
            item.setLoggedUserId(invoiceEntity.getLoggedUserId());
            item.setItemId(0); //0 meanns no connection to DOC

            Prop prop = Prop.getInstance(request);
            String title = "";
            PaymentMethod annotation = payment.getClass().getAnnotation(PaymentMethod.class);
            if(annotation != null) title = prop.getText(annotation.nameKey());
            item.setItemTitle( title );

            item.setDateInsert(new Date());
            item.setInvoiceId(null);
            item.setItemNote(prop.getText("components.basket.invoice_email.payment_method")); //note that this is payment method
            item.setDomainId(CloudToolsForCore.getDomainId());

            item = payment.getPaymentMethodCost(item, paymentMethod);

            BasketInvoiceItemsRepository biir = Tools.getSpringBean("basketInvoiceItemsRepository", BasketInvoiceItemsRepository.class);
            item = biir.save(item);

            return item.getId().intValue();
        } catch(Exception e) {
            BasePaymentMethod.adminLogError(PaymentMethodsService.class, "getBasePaymentMethod failed : " + e.getMessage(), request);
            return -1;
        }
    }

    public static String getPaymentMethosLabel(String paymentMethod, HttpServletRequest request) {
        if(Tools.isEmpty(paymentMethod)) return "";

        try {
            BasePaymentMethod payment = getBasePaymentMethod(paymentMethod);
            if(payment == null) return "";

            PaymentMethod annotation = payment.getClass().getAnnotation(PaymentMethod.class);
            if(annotation == null) return "";

            return Prop.getInstance(request).getText(annotation.nameKey());
        } catch(Exception e) {
            return "";
        }
    }

    public static final Map<String, String> getAllowedCoutriesMap(Prop prop) {
        String[] supprotedCountries = Constants.getArray("basketInvoiceSupportedCountries");
        String defaultKeyPrefix = "stat.countries.tld";

        Map<String, String> countriesMap = new HashMap<>();
        for(String countryCode : supprotedCountries) {
            countriesMap.putIfAbsent(countryCode, prop.getText(defaultKeyPrefix + countryCode));
        }

        return countriesMap;
    }

    public boolean isPaymentMethodEditableByAdmin(BasketInvoicePaymentEntity paymentEntity) {
        if(paymentEntity == null) return true;
        String paymentMethodName = paymentEntity.getPaymentMethod();
        if(Tools.isEmpty(paymentMethodName)) return true;

        PaymentMethodEntity paymentMethodEntity = paymentMethodRepository.findByPaymentMethodNameAndDomainId(paymentEntity.getPaymentMethod(), CloudToolsForCore.getDomainId());
        if(paymentMethodEntity == null) return true;

        BasePaymentMethod paymentMethod = null;
        for(BasePaymentMethod bpm : basePaymentMethodsList) {
            if(paymentMethodName.equals(bpm.getClass().getName())) {
                paymentMethod = bpm;
                break;
            }
        }

        return paymentMethod == null ? true : paymentMethod.isEditableByAdmin(paymentMethodEntity);
    }
}