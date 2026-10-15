# Forms

## Validation when leaving the field

Text field validation on loss of focus is enabled by default. The configuration variable `multistepform_validateOnBlur` has a default value of `true` ; setting it to `false` disables the validation. The setting applies to all multi-step forms.

The check uses the **Required Field** setting and trims leading and trailing spaces according to the **Trim Spaces** setting before validation. **Allowed Value** refers to the [regular expressions](../../../redactor/apps/form/regexps.md) selected in the editor for a specific item. The entered value must satisfy all selected rules. If no rule is selected, the value format is not validated by this check.

An error is displayed next to the field. The check does not save data or call the handler. Full validation, including conditions, occurs when the step is submitted.

## Data processed upon return

The **Back** button temporarily saves the values ​​of the current step to the HTTP session, separate from the confirmed responses. Incomplete values, hidden fields, canceled selections, and completed uploads are also preserved. This saving does not require successful validation and works even if field exit checking is disabled.

When returning to a step, the processed values ​​take precedence over older confirmed responses. Moving forward will check and confirm the values ​​of visible fields. If the temporary save fails, the current step remains open. Processed data is deleted when the form is completed, the attempt is finally terminated, or the session expires; it is not restored when the page is refreshed.

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
