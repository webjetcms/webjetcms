# Payment methods

This is a special table that primarily uses optional fields for its operation. In this way, we have achieved modularity of the editor, where each payment method has customized fields in the editor.

The basic element is the class [BasePaymentMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/jpa/PaymentMethodEntity.java) which has optional fields A-L (easily added if necessary).

## Adding a new payment method

To add a new payment method we need to create a Service e.g. as [GoPayService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/GoPayService.java). Each payment method requires its own file.

```java
@Service
@PaymentMethod(
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
- Mandatory `PaymentMethod` annotation, use this annotation to set the appearance of the editor for a given payment method.
- Compulsory class inheritance `BasePaymentMethod`, which defines mandatory methods to implement as well as provides supporting logic.

Each Service created in this way is subsequently obtained in [PaymentMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/PaymentMethodsService.java). The process is automatic, so if the new payment method has been created correctly, it will automatically appear in the spreadsheet and has all the necessary logic.

## Annotation `PaymentMethod`

The annotation provides parameters for the setup:
- `nameKey`, the translation key for setting the payment method name.
- `fieldMap` to set the individual optional fields that will be displayed in the editor using the annotation `@PaymentFieldMapAttr`. Each field is represented by its annotation.

### `PaymentFieldMapAttr`

Interface [`PaymentFieldMapAttr`](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/jpa/PaymentFieldMapAttr.java) allows you to define an optional field for the payment method.

Available parameters to set are:
- `fieldAlphabet`, indicating which optional field we are setting
- `fieldType`, what type the optional field should be in the editor (`text`, `number`, ...)
- `fieldLabel`, translation key for the name of the optional field
- `isRequired`, if set to `true`, the field will be taken as mandatory
- `defaultValue`, used to pre-set the value in the field

## Class `BasePaymentMethod`

Class [BasePaymentMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/BasePaymentMethod.java) contains all the necessary logic for adding a new payment method to avoid duplication. The class is primarily for inheriting payments, so only the error logging methods are publicly available.

Automatic:
- sets the name of the payment method and according to the annotation `PaymentMethod`
- sets the optional fields according to the annotation `PaymentMethod`
- when saving, verify that all required fields are entered
- ...

The individual methods and their logic are described in more detail directly in the file.

### Method `beforeSave`

Class `BasePaymentMethod` also provides `beforeSave` a method that serves to overload. The method is called before the actual save, so it gives the ability to modify the editor data before the actual save is performed. For security reasons, the save will double check that all required fields are entered.

## Class `PaymentMethodsService`

Class [PaymentMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/PaymentMethodsService.java) provides all the public methods to work with the payment methods you will need.

It provides methods for:
- obtaining all or individual payment methods
- saving/deleting methods
- validation of method values
- validation of the refund
- ...

The individual methods and their logic are described in more detail directly in the file.
