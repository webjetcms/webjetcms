package sk.iway.iwcm.components.basket.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.components.basket.jpa.BasketFeeStatsProjection;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceStatsProjection;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.BasketProductStatsProjection;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;

/**
 * Tests legacy fee resolution and fee and product aggregation in {@link BasketStatsService}.
 */
class BasketStatsServiceTest {

    private BasketInvoiceItemsRepository invoiceItemsRepository;
    private DocDetailsRepository docDetailsRepository;
    private BasketStatsService service;

    @BeforeEach
    void setUp() {
        invoiceItemsRepository = mock(BasketInvoiceItemsRepository.class);
        docDetailsRepository = mock(DocDetailsRepository.class);
        service = new BasketStatsService(
            mock(BasketInvoicesRepository.class),
            invoiceItemsRepository,
            docDetailsRepository
        );
    }

    /**
     * Verifies that configured legacy fee groups resolve to their document identifiers.
     */
    @Test
    void resolvesLegacyFeeDocumentsFromConfiguredGroupPaths() {
        GroupsDB groupsDB = mock(GroupsDB.class);
        GroupDetails deliveryGroup = group(10);
        GroupDetails paymentGroup = group(20);
        when(groupsDB.getGroupByPath("/custom/delivery")).thenReturn(deliveryGroup);
        when(groupsDB.getGroupByPath("/custom/payment")).thenReturn(paymentGroup);
        when(docDetailsRepository.findAllByGroupId(10)).thenReturn(List.of(doc(101), doc(102)));
        when(docDetailsRepository.findAllByGroupId(20)).thenReturn(List.of(doc(201)));

        Set<Integer> deliveryIds = service.getLegacyFeeDocumentIds(groupsDB, "/custom/delivery");
        Set<Integer> paymentIds = service.getLegacyFeeDocumentIds(groupsDB, "/custom/payment");

        assertEquals(Set.of(101, 102), deliveryIds);
        assertEquals(Set.of(201), paymentIds);
        verify(docDetailsRepository).findAllByGroupId(10);
        verify(docDetailsRepository).findAllByGroupId(20);
    }

    /**
     * Verifies that missing legacy groups produce empty results and safe query parameters.
     */
    @Test
    void handlesMissingLegacyGroupsAndEmptyQueryLists() {
        GroupsDB groupsDB = mock(GroupsDB.class);

        Set<Integer> itemIds = service.getLegacyFeeDocumentIds(groupsDB, "/missing");

        assertTrue(itemIds.isEmpty());
        assertEquals(List.of(-1), BasketStatsService.toQueryItemIds(itemIds));
        assertEquals(List.of(101, 201), BasketStatsService.toQueryItemIds(Set.of(201, 101)));
        verify(docDetailsRepository, never()).findAllByGroupId(any());
    }

    /**
     * Verifies modern and legacy fee classification and exclusion of fee documents from product totals.
     */
    @Test
    void classifiesLegacyAndModernFeesAndExcludesLegacyItemsFromProducts() {
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> switch (invocation.getArgument(0, String.class)) {
            case "components.basket.invoice_email.delivery_method" -> "Delivery method";
            case "components.basket.invoice_email.payment_method" -> "Payment method";
            case "apps.eshop.stats.categories_root" -> "Categories";
            case "apps.eshop.stats.category_direct" -> "Direct";
            default -> "Unknown";
        });
        when(docDetailsRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(docDetailsRepository.getDistGroupIdsByDataLike(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Collections.emptyList());

        BasketInvoiceStatsProjection invoice = invoice(new BigDecimal("100.00"));
        List<BasketFeeStatsProjection> fees = List.of(
            fee(101, "Old delivery note", "10.00", 1, 20),
            fee(201, "Old payment note", "5.00", 2, 0),
            fee(0, "Delivery method", "2.00", 1, 0),
            fee(0, "Payment method", "3.00", 1, 0),
            fee(999, "Delivery method", "50.00", 1, 0)
        );
        List<BasketProductStatsProjection> products = List.of(
            product(101, "Legacy delivery", 1),
            product(201, "Legacy payment", 1),
            product(999, "Real product", 2)
        );

        GroupsDB groupsDB = mock(GroupsDB.class);
        try (MockedStatic<GroupsDB> groupsDbMock = mockStatic(GroupsDB.class)) {
            groupsDbMock.when(GroupsDB::getInstance).thenReturn(groupsDB);

            BasketStatsDTO stats = service.createStats(
                List.of(invoice),
                products,
                fees,
                "eur",
                mock(HttpServletRequest.class),
                prop,
                Set.of(101),
                Set.of(201)
            );

            assertEquals(new BigDecimal("14.00"), stats.getDeliveryFees());
            assertEquals(new BigDecimal("13.00"), stats.getPaymentFees());
            assertEquals(new BigDecimal("73.00"), stats.getNetRevenue());
            assertEquals(2, stats.getSoldItemCount());
            assertEquals(1, stats.getTopProducts().size());
            assertEquals("Real product", stats.getTopProducts().get(0).getName());
            assertEquals(2, stats.getTopProducts().get(0).getCount());
        }
    }

    private GroupDetails group(int groupId) {
        GroupDetails group = new GroupDetails();
        group.setGroupId(groupId);
        return group;
    }

    private DocDetails doc(int docId) {
        DocDetails doc = new DocDetails();
        doc.setDocId(docId);
        return doc;
    }

    private BasketInvoiceStatsProjection invoice(BigDecimal price) {
        BasketInvoiceStatsProjection invoice = mock(BasketInvoiceStatsProjection.class);
        when(invoice.getPriceToPayVat()).thenReturn(price);
        when(invoice.getPriceToPayNoVat()).thenReturn(price);
        when(invoice.getCurrency()).thenReturn("eur");
        return invoice;
    }

    private BasketFeeStatsProjection fee(int itemId, String itemNote, String price, int quantity, int vat) {
        BasketFeeStatsProjection fee = mock(BasketFeeStatsProjection.class);
        when(fee.getItemId()).thenReturn(itemId);
        when(fee.getItemNote()).thenReturn(itemNote);
        when(fee.getItemPrice()).thenReturn(new BigDecimal(price));
        when(fee.getItemQty()).thenReturn(quantity);
        when(fee.getItemVat()).thenReturn(vat);
        when(fee.getCurrency()).thenReturn("eur");
        return fee;
    }

    private BasketProductStatsProjection product(int itemId, String title, long quantity) {
        BasketProductStatsProjection product = mock(BasketProductStatsProjection.class);
        when(product.getItemId()).thenReturn(itemId);
        when(product.getItemTitle()).thenReturn(title);
        when(product.getQuantity()).thenReturn(quantity);
        return product;
    }
}
