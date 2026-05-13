# Delivery methods

This is a special table that is extended with optional fields for its functionality. This allows us to achieve modularity in the editor, where each delivery method has, in addition to common fields, custom fields in the editor.

The basic element is the class [BaseDeliveryMethod](../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/BaseDeliveryMethod.java), which has optional AL fields.

## Adding a new delivery method

To add a new delivery method, we need to create a Service, e.g. like [InStoreService](../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/InStoreService.java). Each delivery method requires its own file.

```java
@Service
@FieldsConfig(
    nameKey = "apps.eshop.delivery_methods.in_store"
)
public class InStoreService extends BaseDeliveryMethod {
    //...
}
```

**Each such Service representing transportation must meet the following conditions**:

- Mandatory ```FieldsConfig``` annotation, use this annotation to set the editor appearance for a given delivery method.
- Mandatory inheritance of the ```BaseDeliveryMethod``` class, which defines mandatory methods for implementations as well as provides supporting logic.

Each Service created in this way is then retrieved in [DeliveryMethodsService](../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/DeliveryMethodsService.java). The process is automatic, so if the new delivery method was created correctly, it will automatically appear in the table and have all the necessary logic.

## Annotation ```FieldsConfig```

The annotation provides parameters for setting:

- ```nameKey```, translation key for setting the delivery method name.
- ```fieldMap```, for setting individual optional fields that will be displayed in the editor using the annotation ```@FieldMapAttr```. Each field is represented by its own annotation.

### ```FieldMapAttr```

Interface [`FieldMapAttr`](../../../../../src/main/java/sk/iway/iwcm/components/basket/support/FieldMapAttr.java) allows defining an optional field for the delivery method.

The available parameters to set are:

- ```fieldAlphabet```, designation of which optional field we are setting
- ```fieldType```, what type should the optional field in the editor have (```text```, ```number```, ...)
- ```fieldLabel```, translation key for optional field name
- ```isRequired```, if set to ```true```, the field will be considered mandatory
- ```defaultValue```, used to pre-set a value in the field