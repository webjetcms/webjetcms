package sk.iway.iwcm.components.basket.payment_methods.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodEntity;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodRepository;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState;
import sk.iway.iwcm.components.basket.support.FieldMapAttr;
import sk.iway.iwcm.components.basket.support.FieldsConfig;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsSearchDto;
import sk.iway.iwcm.components.customfields.rest.CustomFieldsService;
import sk.iway.iwcm.editor.FieldType;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

/** Verifies payment-method editor preparation for persisted provider configurations. */
class BasePaymentMethodTest {

    /** Verifies GETONE can generate fields after the provider class name is replaced by its label. */
    @Test
    void opensConfiguredPaymentAfterLocalizedNameIsApplied() {
        PaymentMethodEntity stored = new PaymentMethodEntity();
        stored.setPaymentMethodName(TestPayment.class.getName());
        stored.setFieldA("merchant");
        PaymentMethodRepository repository = mock(PaymentMethodRepository.class);
        when(repository.findByPaymentMethodNameAndDomainId(eq(TestPayment.class.getName()), anyInt())).thenReturn(stored);

        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation ->
            "payment.test".equals(invocation.getArgument(0)) ? "GoPay" : invocation.getArgument(0));

        try (MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Prop> props = mockStatic(Prop.class);
                MockedStatic<CustomFieldsService> fields = mockStatic(CustomFieldsService.class, CALLS_REAL_METHODS)) {
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(1);
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("sk");
            props.when(Prop::getInstance).thenReturn(prop);
            props.when(() -> Prop.getInstance("sk")).thenReturn(prop);
            fields.when(() -> CustomFieldsService.getCustomFieldsMap(any(CustomFieldsSearchDto.class))).thenReturn(Map.of());

            PaymentMethodEntity result = new TestPayment().open(7L, repository, prop);

            assertEquals("GoPay", result.getPaymentMethodName());
            assertNotNull(result.getEditorFields());
            assertEquals("text", result.getEditorFields().getFieldsDefinition().get(0).getType());
        }
    }

    @FieldsConfig(nameKey = "payment.test", fieldMap = {
        @FieldMapAttr(fieldAlphabet = 'A', fieldType = FieldType.TEXT, fieldLabel = "payment.field_a")
    })
    private static class TestPayment extends BasePaymentMethod {
        PaymentMethodEntity open(Long id, PaymentMethodRepository repository, Prop prop) {
            return getPayment(id, repository, ProcessItemAction.GETONE, prop);
        }
        @Override
        protected String getPaymentResponse(Long invoiceId, String returnUrl, HttpServletRequest request) { return null; }
        @Override
        protected PaymentState getPaymentState(HttpServletRequest request) { return null; }
        @Override
        protected RefundationState doPaymentRefund(BigDecimal amount, BasketInvoicePaymentEntity payment, HttpServletRequest request) { return null; }
        @Override
        protected BasketInvoiceItemEntity getPaymentMethodCost(BasketInvoiceItemEntity item, PaymentMethodEntity payment) { return item; }
        @Override
        protected boolean isEditableByAdmin(PaymentMethodEntity payment) { return true; }
    }
}
