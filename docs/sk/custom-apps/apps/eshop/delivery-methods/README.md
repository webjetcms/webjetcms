# Spôsoby doručenia

Ide o špeciálnu tabuľku, ktorá je pre svoje fungovanie rozšírená o voliteľné polia. Týmto sme dosiahli modulárnosť editora, kde každý spôsob doručenia má okrem spoločných polí aj na mieru pripravené polia v editore.

Základným prvkom je trieda [BaseDeliveryMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/BaseDeliveryMethod.java), ktorá má voliteľné polia A-L.

## Pridanie nového spôsobu doručenia

Pre pridanie nového spôsobu doručenia musíme vytvoriť Servis napr. ako [InStoreService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/InStoreService.java). Každý spôsob doručenia vyžaduje vlastný súbor.

```java
@Service
@FieldsConfig(
    nameKey = "apps.eshop.delivery_methods.in_store"
)
public class InStoreService extends BaseDeliveryMethod {
    //...
}
```

**Každý takýto Service reprezentujúci dopravu musí spĺňať nasledujúce podmienky**:

- Povinná ```FieldsConfig``` anotácia, pomocou tejto anotácie nastavíte vzhľad editora pre daný spôsob doručenia.
- Povinné dedenie triedy ```BaseDeliveryMethod```, ktorá definuje povinné metódy k implementácií ako aj poskytuje podpornú logiku.

Každý takto vytvorený Servis je následne získaný v [DeliveryMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/delivery_methods/rest/DeliveryMethodsService.java). Proces je automatický, takže ak bol nový spôsob doručenia vytvorený korektne, automaticky sa zobrazí v tabuľke a má všetku potrebnú logiku.

## Anotácia ```FieldsConfig```

Anotácia poskytuje parametre pre nastavenie:

- ```nameKey```, prekladový kľúč pre nastavenie názvu spôsobu doručenia.
- ```fieldMap```, pre nastavenie jednotlivých voliteľných polí, ktoré sa zobrazia v editore pomocou anotácie ```@FieldMapAttr```. Každé pole je reprezentované svojou anotáciou.

### ```FieldMapAttr```

Interface [`FieldMapAttr`](../../../../../../src/main/java/sk/iway/iwcm/components/basket/support/FieldMapAttr.java) umožňuje zadefinovanie voliteľného poľa pre spôsob doručenia.

Dostupné parametre k nastaveniu sú:

- ```fieldAlphabet```, označenie, ktoré voliteľné pole nastavujeme
- ```fieldType```, aký typ má mať voliteľné pole v editore (```text```, ```number```, ...)
- ```fieldLabel```, prekladový kľúč pre názov voliteľného poľa
- ```isRequired```, ak je nastavená na hodnotu ```true```, bude sa pole brať za povinné
- ```defaultValue```, slúži na pred-nastavenie hodnoty do poľa