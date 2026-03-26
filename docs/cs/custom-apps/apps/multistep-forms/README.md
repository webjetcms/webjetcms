# Formuláře

V některých případech je třeba provést složitější operace nebo validace formulářů. Pro tento účel je ve vícekrokových formulářích možné nastavit Java třídu v poli Zpracovatel formulářů. Jedná se o speciální třídu, která slouží ke zpracování kroků formuláře a umožňuje:
- validaci kroku
- spuštění interceptor-a kroku
- vlastní uložení formuláře

Základem je implementace rozhraní [`FormProcessor`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java), který definuje potřebné metody pro zpracování formuláře.

## Přidání nového zpracovatele formuláře

Pro přidání nového zpracovatele formuláře musíme vytvořit novou třídu, která splňuje následující podmínky:
- implementuje rozhraní [`FormProcessorInterface`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java), které definuje povinné metody k implementaci.
- je notována pomocí anotace `@Component` nebo `@Service`

Příkladem takové implementace je například třída [FormEmailVerificationProcessor](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormEmailVerificationProcessor.java).

```java
@Component
public class FormEmailVerificationProcessor implements FormProcessorInterface {
    //  ....
}
```

Každý takto vytvořený zpracovatel formuláře je získán v [FormSettingsService](../../../../../src/main/java/sk/iway/iwcm/components/form_settings/rest/FormSettingsService.java) a následné nabídnut v editoru pro výběr zpracovatele formuláře.

## Rozhraní `FormProcessorInterface`

Rozhraní [`FormProcessorInterface`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java) definuje povinné metody, které musí implementovat každý zpracovatel formuláře. Skládá se z následujících metod:
- `validateStep` - metoda se vyvolá při validaci kroku (před uložením). V této metodě lze provést jakoukoli validaci, která je potřebná pro daný krok.
- `runStepInterceptor` - metoda se vyvolá po validaci kroku, ale před uložením. V této metodě lze spustit jakýkoli interceptor, který je potřebný pro daný krok. Například se může jednat odeslání emailu/SMS s kódem, který je třeba zadat v dalším kroku.
- `handleFormSave` - metoda se vyvolá celkovém ukládání formuláře. V této metodě je možné vlastní uložení formuláře, například odesláním do `CRM` systému. Metoda vrací `boolean` hodnotu, která určuje, zda se má vyvolat i klasické `WebJET` uložení formuláře.

Bližší informace o fungování jednotlivých metod a jejich parametrů je popsáno přímo v souboru.

!>**Upozornění:** O načtení a vyvolání jednotlivých metod zpracovatele formulářů (je-li definován) se stará třída [MultistepFormsService](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/rest/MultistepFormsService.java).
