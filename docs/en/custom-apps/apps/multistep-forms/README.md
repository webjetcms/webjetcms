# Forms

In some cases, more complex operations or form validations need to be performed. For this purpose, in multi-step forms it is possible to set a Java class in the Form Processor field. This is a special class that is used to process the form steps and allows:
- step validation
- launching the interceptor step
- self saving of the form

The implementation of the interface is essential [`FormProcessor`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java) which defines the necessary methods for processing the form.

## Adding a new form processor

To add a new form handler, we need to create a new class that meets the following conditions:
- implements the interface [`FormProcessorInterface`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java), which defines mandatory methods to implement.
- is annotated using annotation `@Component` or `@Service`

An example of such an implementation is the class [FormEmailVerificationProcessor](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormEmailVerificationProcessor.java).

```java
@Component
public class FormEmailVerificationProcessor implements FormProcessorInterface {
    //  ....
}
```

Each form processor created in this way is obtained in [FormSettingsService](../../../../../src/main/java/sk/iway/iwcm/components/form_settings/rest/FormSettingsService.java) and then offered in the editor to select the form processor.

## Interface `FormProcessorInterface`

Interface [`FormProcessorInterface`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java) defines mandatory methods that must be implemented by each form processor. It consists of the following methods:
- `validateStep` - the method is called during step validation (before saving). Any validation that is required for the step can be performed in this method.
- `runStepInterceptor` - method is called after step validation, but before saving. In this method, any interceptor that is needed for the step can be executed. For example, it can be to send an email/SMS with a code that needs to be entered in the next step.
- `handleFormSave` - method is invoked by the overall saving of the form. In this method it is possible to save the form itself, for example by sending it to `CRM` system. The method returns `boolean` value, which determines whether the classic `WebJET` saving the form.

More information about the operation of each method and its parameters is described directly in the file.

!>**Warning:** The individual methods of the form handler (if defined) are loaded and called by the class [MultistepFormsService](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/rest/MultistepFormsService.java).
