# Způsoby doručení

Jedná se o speciální tabulku, která je pro své fungování rozšířena o volitelná pole. Tímto jsme dosáhli modulárnosti editoru, kde každý způsob doručení má kromě společných polí i na míru připravená pole v editoru.

Základním prvkem je třída [BaseDeliveryMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/BaseDeliveryMethod.java), která má volitelná pole A-L.

## Přidání nového způsobu doručení

Pro přidání nového způsobu doručení musíme vytvořit Servis např. jak [InStoreService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/InStoreService.java). Každý způsob doručení vyžaduje vlastní soubor.

```java
@Service
@FieldsConfig(
    nameKey = "apps.eshop.delivery_methods.in_store"
)
public class InStoreService extends BaseDeliveryMethod {
    //...
}
```

**Každý takový Service reprezentující dopravu musí splňovat následující podmínky**:
- Povinná `FieldsConfig` anotace, pomocí této anotace nastavíte vzhled editoru pro daný způsob doručení.
- Povinné dědění třídy `BaseDeliveryMethod`, která definuje povinné metody k implementaci jakož i poskytuje podpůrnou logiku.

Každý takto vytvořený Servis je následně získán v [DeliveryMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/DeliveryMethodsService.java). Proces je automatický, takže pokud byl nový způsob doručení vytvořen korektně, automaticky se zobrazí v tabulce a má veškerou potřebnou logiku.

## Anotace `FieldsConfig`

Anotace poskytuje parametry pro nastavení:
- `nameKey`, překladový klíč pro nastavení názvu způsobu doručení.
- `fieldMap`, pro nastavení jednotlivých volitelných polí, která se zobrazí v editoru pomocí anotace `@FieldMapAttr`. Každé pole je reprezentováno svojí anotací.

### `FieldMapAttr`

Interface [`FieldMapAttr`](../../../../../../src/main/java/sk/iway/iwcm/components/basket/support/FieldMapAttr.java) umožňuje definování volitelného pole pro způsob doručení.

Dostupné parametry k nastavení jsou:
- `fieldAlphabet`, označení, které volitelné pole nastavujeme
- `fieldType`, jaký typ má mít volitelné pole v editoru (`text`, `number`, ...)
- `fieldLabel`, překladový klíč pro název volitelného pole
- `isRequired`, je-li nastavena na hodnotu `true`, bude se pole brát za povinné
- `defaultValue`, slouží k před-nastavení hodnoty do pole
