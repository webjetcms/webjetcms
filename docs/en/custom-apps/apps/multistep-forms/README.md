# Forms

## Validation when leaving a field

Setting `multistepform_validateOnBlur=true` enables checks of text fields when they lose focus. It is disabled by default. The checks apply the static required setting, trim spaces, and validate allowed values; errors appear beside the field. This check does not save data or call the custom form processor. Full validation, including conditional rules, runs when the step is submitted.

## Unfinished data when going back

The **Back** button temporarily saves the current step's values in the HTTP session, separately from confirmed answers. Incomplete values, hidden fields, cleared selections, and completed uploads are preserved. This does not require successful validation and also works when validation on leaving a field is disabled.

When the step is reopened, unfinished values take precedence over older confirmed answers. Moving forward validates and confirms the values of visible fields. If temporary saving fails, the current step remains open. Unfinished data is removed when the form is completed, the attempt is permanently ended, or the session expires; it is not restored after refreshing the page.

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
