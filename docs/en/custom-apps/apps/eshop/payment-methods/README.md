# Payment methods

This is a special table that primarily uses optional fields to function. This allows us to achieve modularity in the editor, where each payment method has custom fields in the editor.

The basic element is the [BasePaymentMethod](../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/jpa/PaymentMethodEntity.java) class, which has optional AL fields (they can be easily added if necessary).

## Adding a new payment method

To add a new payment method, we need to create a Service, e.g. like [GoPayService](../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/GoPayService.java). Each payment method requires its own file.

```java
@Service
@FieldsConfig(
    nameKey = "apps.eshop.payments.go_pay",
    fieldMap = {
        @PaymentFieldMapAttr(fieldAlphabet = 'A', fieldType = FieldType.TEXT, fieldLabel = "apps.eshop.payments.client_id", isRequired = true),
        @PaymentFieldMapAttr(fieldAlphabet = 'B', fieldType = FieldType.TEXT, fieldLabel = "apps.eshop.payments.secret", isRequired = true),
        @PaymentFieldMapAttr(fieldAlphabet = 'C', fieldType = FieldType.TEXT, fieldLabel = "apps.eshop.payments.url", isRequired = true),
        @PaymentFieldMapAttr(fieldAlphabet = 'D', fieldType = FieldType.NUMBER, fieldLabel = "apps.eshop.payments.go_id", isRequired = true),
        @PaymentFieldMapAttr(fieldAlphabet = 'E', fieldType = FieldType.NUMBER, fieldLabel = "components.basket.invoice_payments.price", isRequired = true, defaultValue = "0"),
        @PaymentFieldMapAttr(fieldAlphabet = 'F', fieldType = FieldType.NUMBER, fieldLabel = "components.basket.invoice_payments.vat", isRequired = true, defaultValue = "0"),
        @PaymentFieldMapAttr(fieldAlphabet = 'G', fieldType = FieldType.TEXT, fieldLabel = "components.basket.invoice_payments.gopay.orderDescription", isRequired = false, defaultValue = ""),
        @PaymentFieldMapAttr(fieldAlphabet = 'H', fieldType = FieldType.QUILL, fieldLabel = "components.payment_methods.mmoney_transfer_note", isRequired = false),
        @PaymentFieldMapAttr(fieldAlphabet = 'I', fieldType = FieldType.BOOLEAN_TEXT, fieldLabel = "components.payment_methods.allow_admin_edit", isRequired = false, defaultValue = "false"),
})
public class GoPayService extends BasePaymentMethod {
    //...
}
```

**Each such Service representing a payment must meet the following conditions**:

- Mandatory ```FieldsConfig``` annotation, use this annotation to set the editor appearance for a given payment method.
- Mandatory inheritance of the ```BasePaymentMethod``` class, which defines mandatory methods for implementations as well as provides supporting logic.

Each Service created in this way is then retrieved in [PaymentMethodsService](../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/PaymentMethodsService.java). The process is automatic, so if the new payment method was created correctly, it will automatically appear in the table and have all the necessary logic.

## Annotation ```FieldsConfig```

The annotation provides parameters for setting:

- ```nameKey```, translation key for setting the payment method name.
- ```fieldMap```, for setting individual optional fields that will be displayed in the editor using the annotation ```@FieldMapAttr```. Each field is represented by its own annotation.

### ```FieldMapAttr```

Interface [`FieldMapAttr`](../../../../../src/main/java/sk/iway/iwcm/components/basket/support/FieldMapAttr.java) allows defining an optional field for the payment method.

The available parameters to set are:

- ```fieldAlphabet```, designation of which optional field we are setting
- ```fieldType```, what type should the optional field in the editor have (```text```, ```number```, ...)
- ```fieldLabel```, translation key for optional field name
- ```isRequired```, if set to ```true```, the field will be considered mandatory
- ```defaultValue```, used to pre-set a value in the field

## Class ```BasePaymentMethod```

The [BasePaymentMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/BasePaymentMethod.java) class contains all the necessary logic for adding a new payment method to avoid duplication. The class is primarily intended for legacy payments, so only the error logging methods are publicly available.

Automatic:

- sets the name of the payment method and according to the annotation ```PaymentMethod```
- sets optional fields according to the annotation ```PaymentMethod```
- when saving, it verifies that all required fields are entered
- ...

Individual methods and their logic are described in more detail directly in the file.

### Method ```beforeSave```

The ```BasePaymentMethod``` class also provides the ```beforeSave``` method, which is used for overloading. The method is called before saving, so it gives the possibility to modify the editor data before saving. For security reasons, when saving, it is checked again whether all required fields are entered.

## Class ```PaymentMethodsService```

The [PaymentMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/PaymentMethodsService.java) class provides all the public methods for working with payment methods that you will need.

It provides methods for:

- obtaining all or individual payment methods
- save/delete methods
- validation of method values
- refund validation
- ...

Individual methods and their logic are described in more detail directly in the file.