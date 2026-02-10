# Spracovateľ formulárov

Ide o špeciálnu triedu, ktorá slúži na spracovanie krokov formuláru a umožňuje:

- validáciu kroku
- spustenie interceptor-a kroku
- vlastné uloženie formuláru

Základom je implementácia rozhrania [`FormProcessor`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java), ktorý definuje potrebné metódy pre spracovanie formuláru.

## Pridanie nového spracovateľa formuláru

Pre pridanie nového spracovateľa formuláru musíme vytvoriť novú triedu, ktorá spĺňa nasledujúce podmienky:

- implementuje rozhranie [`FormProcessorInterface`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java), ktoré definuje povinné metódy k implementácií.
- je notovaná pomocou anotácie ```@Component``` alebo ```@Service```

Príkladom takejto implementácie je napríklad trieda [FormEmailVerificationProcessor](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormEmailVerificationProcessor.java).

```java
@Component
public class FormEmailVerificationProcessor implements FormProcessorInterface {
    //  ....
}
```

Každý takto vytvorený spracovateľ formuláru je získaný v [FormSettingsService](../../../../../src/main/java/sk/iway/iwcm/components/form_settings/rest/FormSettingsService.java) a následné ponúknutý v editore pre výber spracovateľa formuláru.

## Rozhranie `FormProcessorInterface`

Rozhranie [`FormProcessorInterface`](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/support/FormProcessorInterface.java) definuje povinné metódy, ktoré musí implementovať každý spracovateľ formuláru. Skladá sa z nasledujúcich metód:

- `validateStep` - metóda sa vyvolá pri validácií kroku (pred uložením). V tejto metóde je možné vykonať akúkoľvek validáciu, ktorá je potrebná pre daný krok.
- `runStepInterceptor` - metóda sa vyvolá po validácií kroku, ale pred uložením. V tejto metóde je možné spustiť akýkoľvek interceptor, ktorý je potrebný pre daný krok. Napríklad sa môže jednať odoslanie emailu/SMS s kódom, ktorý je potrebné zadať v ďalšom kroku.
- `handleFormSave` - metóda sa vyvolá celkovom ukladaní formuláru. V tejto metóde je možné vlastné uloženie formuláru, napríklad odoslaním do `CRM` systému. Metóda vracia `boolean` hodnotu, ktorá určuje, či sa má vyvolať aj klasické `WebJET` uloženie formuláru.

Bližšie informácie o fungovaní jednotlivých metód a ich parametrov je popísané priamo v súbore.

!>**Upozornenie:** O načítanie a vyvolanie jednotlivých metód spracovateľa formulárov (ak je zadefinovaný) sa stará trieda [MultistepFormsService](../../../../../src/main/java/sk/iway/iwcm/components/multistep_form/rest/MultistepFormsService.java).