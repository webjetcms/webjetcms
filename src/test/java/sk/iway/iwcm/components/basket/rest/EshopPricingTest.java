package sk.iway.iwcm.components.basket.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodEntity;
import sk.iway.iwcm.components.basket.delivery_methods.rest.DeliveryMethodsService;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;

/**
 * Tests storefront repricing and checkout using only existing item and invoice columns.
 */
class EshopPricingTest {

    private static final String PAYMENT = "test.payment";
    private final Map<String, String> originalSettings = new HashMap<>();
    private final List<BasketInvoiceItemEntity> originalItems = new ArrayList<>();
    private String originalInstallName;
    private MockHttpServletRequest request;
    private BasketInvoicesRepository invoices;
    private BasketInvoiceItemsRepository items;
    private DeliveryMethodsService deliveries;
    private DeliveryMethodEntity delivery;
    private EshopService service;
    private Prop prop;
    private BigDecimal paymentNet = BigDecimal.ZERO;
    private MockedStatic<CloudToolsForCore> cloudMock;
    private MockedStatic<PaymentMethodsService> paymentsMock;
    private MockedStatic<Prop> propMock;

    @BeforeEach
    void setUp() {
        originalInstallName = Constants.getInstallName();
        config("basketRoundPrices", "true");
        config("currencyFormat", "0.00");
        config("basketProductCurrency", "eur");
        config("basketDisplayCurrency", "eur");
        config("supportedCurrencies", "eur,czk");
        config("constantsAliasSearch", "false");
        config("multiDomainUseAliasAsInstallName", "false");
        Constants.setInstallName("pricing-test");
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.getSession().setAttribute("BasketDB.browserIdSession", "100");
        request.setParameter("deliveryMethod", "1");
        request.setParameter("paymentMethod", PAYMENT);
        request.setParameter("contactCountry", "sk");
        request.setAttribute("xssTestDisabled", "true");

        invoices = mock(BasketInvoicesRepository.class);
        items = mock(BasketInvoiceItemsRepository.class);
        deliveries = mock(DeliveryMethodsService.class);
        service = new EshopService(invoices, items, mock(BasketInvoicePaymentsRepository.class), deliveries);
        cloudMock = mockStatic(CloudToolsForCore.class);
        cloudMock.when(CloudToolsForCore::getDomainId).thenReturn(1);
        when(items.findAllByBrowserIdAndItemsBasketInvoiceNullAndDomainId(100L, 1)).thenAnswer(ignored -> originalItems);
        when(items.save(any())).thenAnswer(call -> {
            BasketInvoiceItemEntity saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(998L);
                originalItems.add(saved);
            }
            return saved;
        });

        prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(call -> call.getArgument(0));
        propMock = mockStatic(Prop.class);
        propMock.when(() -> Prop.getInstance(request)).thenReturn(prop);
        delivery = new DeliveryMethodEntity();
        delivery.setId(1L);
        delivery.setDeliveryMethodName("test.delivery");
        delivery.setTitle("Delivery");
        delivery.setPrice(BigDecimal.ZERO);
        delivery.setVat(0);
        delivery.setSupportedCountriesStr("sk,cz");
        when(deliveries.getDeliveryMethod(1L, null, prop)).thenReturn(delivery);

        paymentsMock = mockStatic(PaymentMethodsService.class);
        paymentsMock.when(() -> PaymentMethodsService.isPaymentMethodConfigured(PAYMENT, request, prop)).thenReturn(true);
        paymentsMock.when(() -> PaymentMethodsService.createPaymentInvoiceItemAndReturnId(any(BasketInvoiceEntity.class), any(), any()))
            .thenAnswer(ignored -> {
                BasketInvoiceItemEntity fee = fee();
                fee.setId(999L);
                originalItems.add(fee);
                when(items.findById(999L)).thenReturn(java.util.Optional.of(fee));
                return 999;
            });
    }

    @AfterEach
    void tearDown() {
        paymentsMock.close();
        propMock.close();
        cloudMock.close();
        originalSettings.forEach(Constants::setString);
        Constants.setInstallName(originalInstallName);
    }

    /** Checkout refreshes prices, includes fees and saves amounts that can be rebuilt from existing fields. */
    @Test
    void savesCurrentPricesAndFees() {
        config("currencyFormat", "0.0000");
        product(100, "1.594", 23, 3);
        product(200, "0.0001", 0, 10000);
        delivery.setPrice(new BigDecimal("1.594"));
        assertTrue(service.addDeliveryMethod(request, 1, prop));
        verify(items, never()).save(any());
        paymentNet = new BigDecimal("2.675");
        request.setParameter("priceToPayVat", "0.01");
        request.setParameter("currency", "usd");
        List<BasketInvoiceItemEntity> stored = new ArrayList<>();
        when(items.saveAll(any())).thenAnswer(call -> {
            ((Iterable<BasketInvoiceItemEntity>) call.getArgument(0)).forEach(stored::add);
            return stored;
        });
        when(invoices.save(any())).thenAnswer(call -> {
            BasketInvoiceEntity invoice = call.getArgument(0);
            invoice.setId(501L);
            return invoice;
        });
        PlatformTransactionManager manager = transactionManager();
        try (MockedStatic<Tools> tools = transactionTools(manager);
             MockedStatic<Adminlog> audit = mockStatic(Adminlog.class);
             MockedStatic<PageLng> language = mockStatic(PageLng.class)) {
            BasketInvoiceEntity invoice = service.saveOrder(request);
            assertNotNull(invoice);
            assertEquals(new BigDecimal("11.15"), invoice.getTotalPriceVat());
            assertEquals(new BigDecimal("10.05"), invoice.getTotalPrice());
            assertEquals("eur", invoice.getCurrency());
            assertEquals(4, stored.size());
            assertTrue(stored.stream().allMatch(row -> row.getInvoiceId() == 501));
            for (BasketInvoiceItemEntity row : stored) {
                row.setItemPrice(BigDecimal.valueOf(row.getItemPrice().doubleValue()));
                row.setRoundedUnitPriceVat(null);
                row.setLineVatAmount(null);
            }
            BasketPricingService.allocateVat(stored);
            assertEquals(new BigDecimal("11.15"), EshopService.getTotalLocalPriceVat(stored, request));
            assertEquals(new BigDecimal("10.05"), EshopService.getTotalLocalPrice(stored, request));
            verify(manager).commit(any());
            assertNull(request.getSession().getAttribute("BasketDB.browserIdSession"));
        }
    }

    /** Persistence and commit failures leave the customer's basket session intact. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = { false, true })
    void retainsBasketOnFailedOrder(boolean commitFails) {
        product(100, "1.594", 0, 3);
        PlatformTransactionManager manager = transactionManager();
        if (commitFails) {
            when(invoices.save(any())).thenAnswer(call -> {
                BasketInvoiceEntity invoice = call.getArgument(0);
                invoice.setId(501L);
                return invoice;
            });
            doThrow(new IllegalStateException("Commit failure")).when(manager).commit(any());
        } else when(invoices.save(any())).thenThrow(new IllegalStateException("Save failure"));
        try (MockedStatic<Tools> tools = transactionTools(manager);
             MockedStatic<Adminlog> audit = mockStatic(Adminlog.class)) {
            assertNull(service.saveOrder(request));
            if (commitFails) verify(manager).commit(any());
            else verify(manager).rollback(any());
            assertEquals("100", request.getSession().getAttribute("BasketDB.browserIdSession"));
        }
    }

    private BasketInvoiceItemEntity product(long id, String net, int vat, int quantity) {
        DocDetails doc = mock(DocDetails.class);
        when(doc.getPrice(request)).thenReturn(new BigDecimal(net));
        when(doc.getVat()).thenReturn(BigDecimal.valueOf(vat));
        when(doc.getCurrency()).thenReturn("eur");
        when(doc.getTitle()).thenReturn("Product " + id);
        BasketInvoiceItemEntity item = new BasketInvoiceItemEntity();
        item.setId(id);
        item.setBrowserId(100L);
        item.setDomainId(1);
        item.setItemId((int) id + 1000);
        item.setItemPrice(new BigDecimal("99"));
        item.setItemVat(vat);
        item.setItemQty(quantity);
        item.setDoc(doc);
        originalItems.add(item);
        return item;
    }

    private BasketInvoiceItemEntity fee() {
        BasketInvoiceItemEntity item = new BasketInvoiceItemEntity();
        item.setItemId(0);
        item.setItemTitle("Payment");
        item.setItemNote("Payment fee");
        item.setItemPrice(paymentNet);
        item.setItemVat(0);
        item.setItemQty(1);
        return item;
    }

    private PlatformTransactionManager transactionManager() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return manager;
    }

    private MockedStatic<Tools> transactionTools(PlatformTransactionManager manager) {
        return mockStatic(Tools.class, call -> {
            if ("getSpringBean".equals(call.getMethod().getName()) && "webjet2022TransactionManager".equals(call.getArgument(0))) return manager;
            return call.callRealMethod();
        });
    }

    private void config(String key, String value) {
        originalSettings.putIfAbsent(key, Constants.getString(key));
        Constants.setString(key, value);
    }
}
