package sk.iway.iwcm.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.form.validators.PhoneValidator;
import sk.iway.iwcm.i18n.Prop;

/**
 * Verifies minimum-length extraction and validation messages for classic forms.
 */
class FormMailActionValidationTest {

    @ParameterizedTest
    @CsvSource({
        "minLen8, 8",
        "required minLen6 maxLen10, 6",
        "minLen12 required, 12",
        "required minLen128, 128",
        "'  required  minLen6  maxLen10  ', 6",
        "'required\tminLen6\nmaxLen10', 6",
        "'required\rminLen6\fmaxLen10', 6",
        "minLen6 minLen10, 6",
        "custom-minLen8 minLen12 minLen4-extra, 12",
        "minLen0, 0"
    })
    void extractsMinimumLengthFromCompleteCssClass(String classNames, String expected) {
        assertEquals(expected, FormMailAction.getMinLen(classNames));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        " ", "required maxLen10", "minLen", "minLen-8", "minLen8extra",
        "minLen8-extra", "custom-minLen8", "minLen8.5", "minLen 8", "MinLen8"
    })
    void ignoresMissingOrInvalidMinimumLengthClasses(String classNames) {
        assertEquals("", FormMailAction.getMinLen(classNames));
    }

    /**
     * Uses the CSS minimum in both error outputs instead of the submitted value or other classes.
     */
    @Test
    void reportsMinimumLengthInContextAndSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("language", "en");
        request.setParameter("name", "abc");
        Prop prop = mock(Prop.class);
        when(prop.getText("validation.minlength.valueTooShort")).thenReturn("{0} must be at least {2} characters long");
        FormDB formDB = mock(FormDB.class);
        when(formDB.getAllRegularExpression()).thenReturn(List.<String[]>of(new String[] {"1", "minLen6", ".{6,}"}));
        ActionBeanContext context = mock(ActionBeanContext.class);
        ValidationErrors validationErrors = new ValidationErrors();
        when(context.getValidationErrors()).thenReturn(validationErrors);

        try (MockedStatic<Prop> props = mockStatic(Prop.class);
             MockedStatic<FormDB> forms = mockStatic(FormDB.class);
             MockedStatic<PhoneValidator> phones = mockStatic(PhoneValidator.class);
             MockedStatic<RequestBean> requestBeans = mockStatic(RequestBean.class)) {
            props.when(() -> Prop.getInstance("en")).thenReturn(prop);
            forms.when(FormDB::getInstance).thenReturn(formDB);
            phones.when(PhoneValidator::getInstance).thenReturn(mock(PhoneValidator.class));

            boolean valid = ReflectionTestUtils.invokeMethod(FormMailAction.class, "checkRequiredFields", request,
                List.of(new LabelValueDetails("name", "required minLen6 maxLen10")), Map.<String, String>of(), context);

            String expected = "name must be at least 6 characters long";
            assertFalse(valid);
            assertEquals(expected, ((SimpleError) validationErrors.get("name").get(0)).getMessage());
            assertEquals("<ol class='formMailValidationErrors'><li>" + expected + "</li>\n</ol>",
                request.getSession().getAttribute("formMailValidationErrors"));
        }
    }
}
