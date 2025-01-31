# Všeobecné nastavenia

Nižšie nájdete zoznam všeobecných nastavení pomocou ktorých je možné zmeniť správanie primárne web stránok. Jednotlivé konfiguračné premenné sa nastavujú v sekcii [Konfigurácia](../../admin/setup/configuration/README.md).

Hodnoty odporúčame nastaviť na začiatku vytvárania web sídla a následne už nemeniť, keďže môžu ovplyvniť správanie už vytvorených web stránok.

## URL adresy

Správanie URL adries web stránok je možné nastaviť nasledovnými konfiguračnými premennými:

- `virtualPathLastSlash` predvolene `true` - nastavuje možnosť posledného `/` pre URL adresy **hlavnej stránky**. Pri nastavení na `true` bude URL adresa stránky Produkty vytvorená ako `/products/`, pri nastavení na `false` ako `/products`.
- `editorPageExtension` predvolene `.html` - nastavuje príponu pre **ďalšie stránky v priečinku**. Pri nastavení na `.html` bude URL adresa stránky `iPhone` vytvorená ako `/products/iphone.html`, pri nastavení na `/` bude vytvorená ako `/products/iphone/`.

Po zmene hodnôt je potrebné vykonať reštart aplikačného servera.