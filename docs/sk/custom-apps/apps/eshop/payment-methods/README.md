# Spôsoby platby

Ide o špeciálnu tabuľku, ktorá pre svoje fungovanie primárne využíva voliteľné polia. Týmto sme dosiahli modulárnosť editora, kde každý spôsob platby má na mieru pripravené polia v editore.

Základným prvkom je trieda [BasePaymentMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/jpa/PaymentMethodEntity.java), ktorá má voliteľné polia A-L (v prípade potreby sa dajú jednoducho pridať).

## Pridanie nového spôsobu platby

Pre pridanie nového spôsobu platby musíme vytvoriť Servis napr. ako [GoPayService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/GoPayService.java). Každý spôsob platby vyžaduje vlastný súbor.

```java
@Service
@PaymentMethod(
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

**Každý takýto Service reprezentujúci platbu musí spĺňať nasledujúce podmienky**:

- Povinná ```PaymentMethod``` anotácia, pomocou tejto anotácie nastavíte vzhľad editora pre daný spôsob platby.
- Povinné dedenie triedy ```BasePaymentMethod```, ktorá definuje povinné metódy k implementácií ako aj poskytuje podpornú logiku.

Každý takto vytvorený Servis je následne získaný v [PaymentMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/PaymentMethodsService.java). Proces je automatický, takže ak bol nový spôsob platby vytvorený korektne, automatický sa zobrazí v tabuľke a má všetku potrebnú logiku.

## Anotácia ```PaymentMethod```

Anotácia poskytuje parametre pre nastavenie:

- ```nameKey```, prekladový kľúč pre nastavenie názvu spôsobu platby.
- ```fieldMap```, pre nastavenie jednotlivých voliteľných polí, ktoré sa zobrazia v editore pomocou anotácie ```@PaymentFieldMapAttr```. Každé pole je reprezentované svojou anotáciou.

### ```PaymentFieldMapAttr```

Interface [`PaymentFieldMapAttr`](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/jpa/PaymentFieldMapAttr.java) umožňuje zadefinovanie voliteľného poľa pre spôsob platby.

Dostupné parametre k nastaveniu sú:

- ```fieldAlphabet```, označenie, ktoré voliteľné pole nastavujeme
- ```fieldType```, aký typ má mať voliteľné pole v editore (```text```, ```number```, ...)
- ```fieldLabel```, prekladový kľúč pre názov voliteľného poľa
- ```isRequired```, ak je nastavená na hodnotu ```true```, bude sa pole brať za povinné
- ```defaultValue```, slúži na pred-nastavenie hodnoty do poľa

## Trieda ```BasePaymentMethod```

Trieda [BasePaymentMethod](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/BasePaymentMethod.java) obsahuje všetku potrebnú logiku pre pridávaný nový spôsob platby, aby zabránilo duplicite. Trieda je primárne určená dediace platby, takže verejne dostupné sú iba metódy pre logovanie chýb.

Automatický:

- nastaví názov platobnej metódy a podľa anotácie ```PaymentMethod```
- nastaví voliteľné polia podľa anotácie ```PaymentMethod```
- pri ukladaní overí, že všetky povinné polia sú zadané
- ...

Jednotlivé metódy a ich logika je bližšie opísaná priamo v súbore.

### Metóda ```beforeSave```

Trieda ```BasePaymentMethod``` poskytuje taktiež ```beforeSave``` metódu, ktorá slúži na preťaženie. Metóda sa volá pred samotným uložením, takže dáva možnosť upraviť dáta editora predtým, než sa vykoná samotné uloženie. Kvôli bezpečnosti sa pri uložení ešte raz skontroluje, či sú zadané všetky povinné polia.

## Trieda ```PaymentMethodsService```

Trieda [PaymentMethodsService](../../../../../../src/main/java/sk/iway/iwcm/components/basket/payment_methods/rest/PaymentMethodsService.java) poskytuje všetky verejné metódy pre prácu s platobnými spôsobmi, ktoré budete potrebovať.

Poskytuje metódy pre:

- získanie všetkých alebo jednotlivých platobných metód
- uloženie/vymazanie metód
- validáciu hodnôt metódy
- validáciu refundácie
- ...

Jednotlivé metódy a ich logika je bližšie opísaná priamo v súbore.