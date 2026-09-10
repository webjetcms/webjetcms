package sk.iway.iwcm.components.basket.payment_methods.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.InvoicePaymentStatus;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodRepository;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState.RefundationStatus;
import sk.iway.iwcm.components.basket.rest.ProductListService;
import sk.iway.iwcm.i18n.Prop;

/** Covers refund limits and exact persistence of the amount sent to the provider. */
class PaymentRefundPricingTest {

    /** Rejects invalid refunds before dispatch and persists the exact accepted remaining amount. */
    @Test
    void refundsOnlyTheRemainingAmountFromTheStoredPayment() {
        BasketInvoiceEntity invoice = new BasketInvoiceEntity();
        invoice.setId(10L);
        BasketInvoicePaymentEntity original = payment(11L, "3.18", "123");
        BasketInvoicePaymentEntity submitted = payment(11L, "999.99", "123");
        List<BasketInvoicePaymentEntity> confirmed = List.of(original,
            payment(12L, "-1.59", "123"), payment(13L, "20.00", "456"));
        BasketInvoicesRepository invoices = mock(BasketInvoicesRepository.class);
        BasketInvoicePaymentsRepository payments = mock(BasketInvoicePaymentsRepository.class);
        when(invoices.findFirstByIdAndDomainId(10L, 1)).thenReturn(Optional.of(invoice));
        when(payments.findById(11L)).thenReturn(Optional.of(original));
        when(payments.findAllByInvoiceIdAndConfirmedTrue(10L)).thenReturn(confirmed);
        GoPayService provider = mock(GoPayService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        RefundationState success = new RefundationState(RefundationStatus.SUCCESS, BasePaymentMethod.REFUNDATION_SUCCESS);
        success.setStatusAfterRefund(InvoicePaymentStatus.PARTIALLY_REFUNDED);
        when(provider.doPaymentRefund(any(), any(), eq(request))).thenReturn(success);
        PaymentMethodsService service = new PaymentMethodsService(List.of(provider), mock(PaymentMethodRepository.class));
        Prop prop = mock(Prop.class);

        try (MockedStatic<Tools> tools = mockStatic(Tools.class);
             MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class);
             MockedStatic<Prop> props = mockStatic(Prop.class);
             MockedStatic<ProductListService> pricing = mockStatic(ProductListService.class)) {
            tools.when(() -> Tools.getSpringBean("basketInvoicesRepository", BasketInvoicesRepository.class)).thenReturn(invoices);
            tools.when(() -> Tools.getSpringBean("basketInvoicePaymentsRepository", BasketInvoicePaymentsRepository.class)).thenReturn(payments);
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(1);
            props.when(() -> Prop.getInstance(request)).thenReturn(prop);

            RefundationState fractional = service.refundPayment(new BigDecimal("1.595"), submitted, request);
            assertEquals(RefundationStatus.ERROR, fractional.getStatus());
            assertEquals(PaymentMethodsService.INVALID_REFUND_AMOUNT, fractional.getMsgKey());
            RefundationState excessive = service.refundPayment(new BigDecimal("1.60"), submitted, request);
            assertEquals(RefundationStatus.ERROR, excessive.getStatus());
            assertEquals(BasePaymentMethod.REFUNDATION_AMOUNT_TOO_HIGH, excessive.getMsgKey());
            verifyNoInteractions(provider);
            verify(payments, never()).save(any());
            pricing.verifyNoInteractions();

            RefundationState result = service.refundPayment(new BigDecimal("1.5900"), submitted, request);
            verify(provider).doPaymentRefund(new BigDecimal("1.59"), original, request);
            assertEquals(RefundationStatus.SUCCESS, result.getStatus());
            ArgumentCaptor<BasketInvoicePaymentEntity> saved = ArgumentCaptor.forClass(BasketInvoicePaymentEntity.class);
            verify(payments, times(2)).save(saved.capture());
            assertEquals(new BigDecimal("-1.59"), saved.getAllValues().get(0).getPayedPrice());
            assertEquals("123", saved.getAllValues().get(0).getRealPaymentId());
            pricing.verify(() -> ProductListService.updatePaymentStatus(10L, true));
        }
    }

    private BasketInvoicePaymentEntity payment(long id, String amount, String providerId) {
        BasketInvoicePaymentEntity payment = new BasketInvoicePaymentEntity();
        payment.setId(id);
        payment.setInvoiceId(10L);
        payment.setPayedPrice(new BigDecimal(amount));
        payment.setConfirmed(true);
        payment.setPaymentMethod(GoPayService.class.getName());
        payment.setRealPaymentId(providerId);
        return payment;
    }
}
