# Methods of delivery

This is a special table that is extended with optional fields for its functioning. This is how we achieved the modularity of the editor, where each delivery method has custom fields in the editor in addition to the common fields.

The basic element is the class [BaseDeliveryMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/BaseDeliveryMethod.java) which has optional fields A-L.

## Adding a new delivery method

To add a new delivery method we need to create a Service e.g. as [InStoreService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/InStoreService.java). Each delivery method requires its own file.

```java
@Service
@FieldsConfig(
    nameKey = "apps.eshop.delivery_methods.in_store"
)
public class InStoreService extends BaseDeliveryMethod {
    //...
}
```

**Each such Service representing a transport shall meet the following conditions**:
- Mandatory `FieldsConfig` annotation, use this annotation to set the appearance of the editor for a given delivery method.
- Compulsory class inheritance `BaseDeliveryMethod`, which defines mandatory methods to implement as well as provides supporting logic.

Each Service created in this way is subsequently obtained in [DeliveryMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/DeliveryMethodsService.java). The process is automatic, so if the new delivery method has been created correctly, it will automatically appear in the table and has all the necessary logic.

## Annotation `FieldsConfig`

The annotation provides parameters for the setup:
- `nameKey`, the translation key for setting the name of the delivery method.
- `fieldMap` to set the individual optional fields that will be displayed in the editor using the annotation `@FieldMapAttr`. Each field is represented by its annotation.

### `FieldMapAttr`

Interface [`FieldMapAttr`](../../../../../../src/main/java/sk/iway/iwcm/components/basket/support/FieldMapAttr.java) allows you to define an optional field for the delivery method.

Available parameters to set are:
- `fieldAlphabet`, indicating which optional field we are setting
- `fieldType`, what type the optional field should be in the editor (`text`, `number`, ...)
- `fieldLabel`, translation key for the name of the optional field
- `isRequired`, if set to `true`, the field will be taken as mandatory
- `defaultValue`, used to pre-set the value in the field
