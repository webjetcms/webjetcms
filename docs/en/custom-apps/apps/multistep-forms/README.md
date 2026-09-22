# Forms

## Validation when leaving a field

Set the configuration value `multistepform_validateOnBlur` to `true` to validate text-like inputs and plain textareas when they lose focus. The default is `false`. The setting is returned as `validateOnBlur` by `/rest/multistep-form/get-step` and applies when a step is loaded.

Validation uses the field's static required setting, trimming, XSS, and regular-expression rules, including localized custom error messages. Conditional visibility and requirement rules are evaluated only when the step is submitted. Errors appear beside the affected field and clear when it becomes valid. Selects, checkboxes, radio buttons, file uploads/Dropzone, CAPTCHA, rich-text editors, hidden fields, buttons, and disabled or readonly controls do not trigger blur validation.

The frontend calls `POST /rest/multistep-form/validate-field` with query parameters `form-name`, `step-id`, `field-id`, and `language`, the `X-CSRF-Token` header, and only the selected field's value as a JSON object, for example `{"email":"visitor@example.com"}`. `field-id` is the logical field identifier without the form-instance DOM prefix (`email` in this example). No values from other fields or previous steps are needed for this check.

The endpoint returns HTTP 200 with `{"fieldErrors":{}}` when valid or `{"fieldErrors":{"fieldId":"message"}}` when invalid. Invalid requests return HTTP 400 with localized `err_msg`; a disabled feature returns HTTP 404. This check does not save values, advance steps, invoke form processors, validate CAPTCHA/uploads, or update statistics. Full validation still runs when the step is submitted.

## Custom form processing

In some cases, it is necessary to perform more complex operations or form validations. For this purpose, in multi-step forms, it is possible to set a Java class in the Form Processor field. This is a special class that is used to process form steps and allows:

- step validation
- trigger interceptor step
- custom form saving

The basis is the implementation of the interface [`FormProcessor`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java), which defines the necessary methods for processing the form.

## Adding a new form handler

To add a new form handler, we need to create a new class that meets the following conditions:

- implements the interface [`FormProcessorInterface`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java), which defines mandatory methods for implementation.
- is notated using the annotation ```@Component``` or ```@Service```

An example of such an implementation is the class [FormEmailVerificationProcessor](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormEmailVerificationProcessor.java).

```java
@Component
public class FormEmailVerificationProcessor implements FormProcessorInterface {
    //  ....
}
```

Each form handler created in this way is obtained in [FormSettingsService](../../../../../src/main/java/sk/iway/iwcm/components/form_settings/rest/FormSettingsService.java) and then offered in the editor for selecting the form handler.

## Interface `FormProcessorInterface`

The interface [`FormProcessorInterface`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java) defines the mandatory methods that every form processor must implement. It consists of the following methods:

- `validateStep` - ​​method is called when validating a step (before saving). In this method, any validation that is necessary for a given step can be performed.
- `runStepInterceptor` - ​​the method is called after the step is validated, but before saving. In this method, any interceptor that is needed for the given step can be run. For example, it can be sending an email/SMS with a code that needs to be entered in the next step.
- `handleFormSave` - ​​the method is called by the overall saving of the form. This method allows for the actual saving of the form, for example by sending it to the `CRM` system. The method returns a `boolean` value that determines whether the classic `WebJET` saving of the form should also be called.

More detailed information about the functioning of individual methods and their parameters is described directly in the file.

!>**Warning:** The [MultistepFormsService](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/rest/MultistepFormsService.java) class takes care of loading and calling individual methods of the form handler (if defined).
