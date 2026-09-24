package sk.iway.iwcm.components.basket.payment_methods.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import cz.gopay.api.v3.model.common.Currency;
import cz.gopay.api.v3.model.payment.BasePayment;
import sk.iway.iwcm.common.BasketTools;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodEntity;

/** Covers the monetary contract sent to GoPay, including partial payments. */
class GoPayServiceTest {

    /** Payment attempts use stored totals and currency, then charge only the remaining balance. */
    @Test
    void chargesStoredInvoiceAndOutstandingBalance() {
        try (MockedStatic<BasketTools> currencies = mockStatic(BasketTools.class)) {
            currencies.when(BasketTools::getSystemCurrency).thenReturn("eur");
            BasePayment full = payment("4.77", "0.00");
            assertEquals(Currency.CZK, full.getCurrency());
            assertEquals(477L, full.getAmount());
            assertEquals(477L, full.getItems().get(0).getAmount());
            assertEquals(3L, full.getItems().get(0).getCount());

            BasePayment partial = payment("4.77", "1.00");
            assertEquals(377L, partial.getAmount());
            assertNull(partial.getItems());
            assertNull(payment("4.77", "4.77"));

            BasePayment differingBreakdown = payment("5.00", "0.00");
            assertEquals(500L, differingBreakdown.getAmount());
            assertNull(differingBreakdown.getItems());
        }
    }

    /** Gateway amounts must not silently discard fractional cents or overflow. */
    @Test
    void convertsMinorUnitsExactly() {
        assertEquals(159L, GoPayService.exactMinorUnits(new BigDecimal("1.5900")));
        assertThrows(ArithmeticException.class, () -> GoPayService.exactMinorUnits(new BigDecimal("1.594")));
        assertThrows(ArithmeticException.class, () -> GoPayService.exactMinorUnits(new BigDecimal("92233720368547758.08")));
    }

    private BasePayment payment(String total, String paid) {
        BasketInvoiceEntity invoice = new BasketInvoiceEntity();
        invoice.setId(10L);
        invoice.setCurrency("czk");
        invoice.setUserLng("en");
        invoice.setPriceToPayVat(new BigDecimal(total));
        BasketInvoiceItemEntity item = new BasketInvoiceItemEntity();
        item.setInvoiceId(10);
        item.setItemTitle("Product");
        item.setItemPrice(new BigDecimal("1.59"));
        item.setRoundedUnitPriceVat(new BigDecimal("1.59"));
        item.setItemVat(0);
        item.setItemQty(3);
        BasketInvoiceItemsRepository items = mock(BasketInvoiceItemsRepository.class);
        when(items.findAllByInvoiceIdAndDomainId(eq(10L), anyInt())).thenReturn(List.of(item));
        BasketInvoicePaymentsRepository payments = mock(BasketInvoicePaymentsRepository.class);
        BasketInvoicePaymentEntity confirmed = new BasketInvoicePaymentEntity();
        confirmed.setPayedPrice(new BigDecimal(paid));
        when(payments.findAllByInvoiceIdAndConfirmedTrue(10L)).thenReturn(List.of(confirmed));
        PaymentMethodEntity method = new PaymentMethodEntity();
        method.setFieldD("1234");
        method.setFieldG("Test order");
        return new GoPayService().getPayment(invoice, "https://example.test/payment", method, items, payments);
    }
}
