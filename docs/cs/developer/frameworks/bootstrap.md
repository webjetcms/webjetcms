# Bootstrap

Jako `grid` se používá systém [Bootstrap verze 5](https://getbootstrap.com/docs/5.0/).

## Reakce

Zobrazení administrace je responzivní a používá následující šířky zlomů (definované v souboru \_variables.scss):
- `$bp-laptop:1200px` - při šířce menší než tato jsou záhlaví a levé menu skryté a dostupné kliknutím na hamburger menu.
- `$bp-tablet:992px` - menší než tato šířka, zobrazí se editor datových souborů v celé ploše okna (horizontálně i vertikálně).
- `$bp-dinosaur:576px` - při menší šířce než je tato šířka se změní rozložení v editoru - názvy polí se přesunou zleva nad pole.

Více je popsáno v části [Hlavní ovládací prvky](../../redactor/admin/README.md) v příručce pro editor.

## Použité příslušenství

### Bootstrap select

Výběrová pole jsou nahrazena doplňkem [Bootstrap Select](https://github.com/snapappointments/bootstrap-select/).
