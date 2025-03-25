# Bootstrap

Jak `grid` systém se používá [Bootstrap verze 5](https://getbootstrap.com/docs/5.0/).

## Responziva

Zobrazení administrace je responzivní, používají se následující šířky zlomu (definované v \_variables.scss):
- `$bp-laptop:1200px` - při menší než této šířce je hlavička a levé menu schované a dostupné po kliknutí na hamburger menu.
- `$bp-tablet:992px` - při menší než této šířce je editor datatabulky zobrazen na plnou plochu okna (horizontálně i vertikálně).
- `$bp-dinosaur:576px` - při menší než této šířce je změněno rozložení v editoru - názvy polí se přesunou z levé části nad pole.

Více je popsáno v sekci [Hlavní ovládací prvky](../../redactor/admin/README.md) v manuálně pro redaktora.

## Použité doplňky

### Bootstrap select

Výběrová pole jsou nahrazena doplňkem [Bootstrap Select](https://github.com/snapappointments/bootstrap-select/).
