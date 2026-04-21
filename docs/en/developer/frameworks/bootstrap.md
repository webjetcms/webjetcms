# Bootstrap

[Bootstrap version 5](https://getbootstrap.com/docs/5.0/) is used as the ```grid``` system.

## Responsive

The administration display is responsive, the following break widths are used (defined in _variables.scss):

- ```$bp-laptop:1200px``` - ​​if the width is less than this, the header and left menu are hidden and available after clicking on the hamburger menu.
- ```$bp-tablet:992px``` - ​​if the width is less than this, the data table editor is displayed in the full window area (horizontally and vertically).
- ```$bp-dinosaur:576px``` - ​​if the width is less than this, the layout in the editor is changed - the field names are moved from the left part above the field.

More is described in the [Main Controls](../../redactor/admin/README.md) section of the editor manual.

## Accessories used

### Bootstrap select

The selection fields are replaced by the [Bootstrap Select](https://github.com/snapappointments/bootstrap-select/) plugin.