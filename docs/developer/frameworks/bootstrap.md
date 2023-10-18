# Bootstrap

Ako ```grid``` systém sa používa [Bootstrap verzie 5](https://getbootstrap.com/docs/5.0/).

## Responzíva

Zobrazenie administrácie je responzívne, používajú sa nasledovné šírky zlomu (definované v _variables.scss):

- ```$bp-laptop:1200px``` - pri menšej ako tejto šírke je hlavička a ľavé menu schované a dostupné po kliknutí na hamburger menu.
- ```$bp-tablet:992px``` - pri menšej ako tejto šírke je editor datatabuľky zobrazený na plnú plochu okna (horizontálne aj vertikálne).
- ```$bp-dinosaur:576px``` - pri menšej ako tejto šírke je zmenené rozloženie v editore - názvy polí sa presunú z ľavej časti nad pole.

Viac je opísané v sekcii [Hlavné ovládacie prvky](../../redactor/admin/README.md) v manuálne pre redaktora.

## Použité doplnky

### Bootstrap select

Výberové polia sú nahradené doplnkom [Bootstrap Select](https://github.com/snapappointments/bootstrap-select/).