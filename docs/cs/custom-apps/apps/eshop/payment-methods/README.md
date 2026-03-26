# Způsoby platby

Jedná se o speciální tabulku, která pro své fungování primárně využívá volitelná pole. Tímto jsme dosáhli modulárnosti editoru, kde každý způsob platby má na míru připravená pole v editoru.

Základním prvkem je třída [BasePaymentMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/jpa/PaymentMethodEntity.java), která má volitelná pole A-L (v případě potřeby lze snadno přidat).

## Přidání nového způsobu platby

Pro přidání nového způsobu platby musíme vytvořit Servis např. jak [GoPayService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/GoPayService.java). Každý způsob platby vyžaduje vlastní soubor.

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

**Každý takový Service reprezentující platbu musí splňovat následující podmínky**:
- Povinná `FieldsConfig` anotace, pomocí této anotace nastavíte vzhled editoru pro daný způsob platby.
- Povinné dědění třídy `BasePaymentMethod`, která definuje povinné metody k implementaci jakož i poskytuje podpůrnou logiku.

Každý takto vytvořený Servis je následně získán v [PaymentMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/PaymentMethodsService.java). Proces je automatický, takže pokud byl nový způsob platby vytvořen korektně, automaticky se zobrazí v tabulce a má veškerou potřebnou logiku.

## Anotace `FieldsConfig`

Anotace poskytuje parametry pro nastavení:
- `nameKey`, překladový klíč pro nastavení názvu způsobu platby.
- `fieldMap`, pro nastavení jednotlivých volitelných polí, která se zobrazí v editoru pomocí anotace `@FieldMapAttr`. Každé pole je reprezentováno svojí anotací.

### `FieldMapAttr`

Interface [`FieldMapAttr`](../../../../../../src/main/java/sk/iway/iwcm/components/basket/support/FieldMapAttr.java) umožňuje definování volitelného pole pro způsob platby.

Dostupné parametry k nastavení jsou:
- `fieldAlphabet`, označení, které volitelné pole nastavujeme
- `fieldType`, jaký typ má mít volitelné pole v editoru (`text`, `number`, ...)
- `fieldLabel`, překladový klíč pro název volitelného pole
- `isRequired`, je-li nastavena na hodnotu `true`, bude se pole brát za povinné
- `defaultValue`, slouží k před-nastavení hodnoty do pole

## Třída `BasePaymentMethod`

Třída [BasePaymentMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/BasePaymentMethod.java) obsahuje veškerou potřebnou logiku pro přidávaný nový způsob platby, aby zabránilo duplicitě. Třída je primárně určena dědicí platby, takže veřejně dostupné jsou pouze metody pro logování chyb.

Automatický:
- nastaví název platební metody a podle anotace `PaymentMethod`
- nastaví volitelná pole podle anotace `PaymentMethod`
- při ukládání ověří, že všechna povinná pole jsou zadána
- ...

Jednotlivé metody a jejich logika je blíže popsána přímo v souboru.

### Metoda `beforeSave`

Třída `BasePaymentMethod` poskytuje také `beforeSave` metodu, která slouží k přetížení. Metoda se jmenuje před samotným uložením, takže dává možnost upravit data editoru předtím, než se provede samotné uložení. Kvůli bezpečnosti se při uložení ještě jednou zkontroluje, zda jsou zadána všechna povinná pole.

## Třída `PaymentMethodsService`

Třída [PaymentMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/PaymentMethodsService.java) poskytuje všechny veřejné metody pro práci s platebními způsoby, které budete potřebovat.

Poskytuje metody pro:
- získání všech nebo jednotlivých platebních metod
- uložení/vymazání metod
- validaci hodnot metody
- validaci refundace
- ...

Jednotlivé metody a jejich logika je blíže popsána přímo v souboru.
