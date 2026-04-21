# User list

## Backend

The backend is implemented in the package ```sk.iway.iwcm.components.users```, the REST controller is in the class ```UserDetailsController```.

### Classes for generating a list of rights

The Rights tab contains a tree structure in jstree showing individual rights. The list of all rights is generated in ```MenuService.getAllPermissions()```. The use of the MenuService class is not happy, but since it shares several methods for editing rights from WebJET version 8 to version 2021, it seemed easier to use it for the list of rights as well.

The first level is represented by sections similar to those in the menu, the second by individual modules, and the third by rights within modules (values ​​```leftSubmenuXItemKey``` in ```modinfo.properties```).

The specific generation of CSS classes ```permgroup``` and ```permgroup-ID ``` according to rights groups is used to display colored circles for individual rights contained in the rights group.

If the 3rd level in the tree structure is also to be displayed (the module contains sub-rights), the item on the 2nd level is modified - the suffix ```-leaf``` is added to its ID to make the item unique and at the same time it is added with the same name to the 3rd level of the menu.

The data for the rights tree structure is inserted into the model in ```UserDetailsListener``` as ```model.addAttribute("jstreePerms", JsonTools.objectToJSON(MenuService.getAllPermissions()));```.

The rights assigned to the user are transferred in ```String[] UserDetailsEditorFields.enabledItems```. Here is the list of allowed rights for the user. Technically, however, for historical reasons, WebJET only stores the disallowed (prohibited) rights in the database. In the methods ```fromUserDetailsEntity``` and ```toUserDetailsEntity```, the list of rights is inverted.

## Frontend

The user list display is in the file [user-list.pug](../../../../src/main/webapp/admin/v9/views/pages/users/user-list.pug). It contains specific JS functions for displaying colored circles paired with groups of rights. The circles are used to display the group of rights that contains the given individual right.

![](../../datatables-editor/field-type-jstree.png)

Events and permission group circles are initialized when the window is first opened via the ```usersDatatable.EDITOR.on('opened', function (e, type, action)``` function. The ```permGroupsColorBinded``` variable ensures initialization only on first opening.

The variable ```niceColors``` contains a list of circle colors (so that they are displayed in the same colors according to the order of the permission groups), the colors from the Finder in MacOS were used. If there are more permission groups than the size of the array, additional colors are generated randomly by calling the function ```randomColor```.

First, the list of rights groups is traversed by calling ```$(".DTE_Field_Name_editorFields\\.permGroups input").each(function(index)```. The group ID is obtained from the input field and its name from the assigned ```label``` element. The first letter of the group name is displayed in circles for better overview. At the same time, a CSS style definition is generated based on the rights group ID, which is then inserted into the ```head``` element.

After obtaining the list of rights groups, each LI element of the jsTree is filled with circles according to CSS styles by calling ```$("#DTE_Field_editorFields-enabledItems li.permgroup").each(function(index)```. CSS styles are added for each element of the tree structure on the backend in ```MenuService.getAllPermissions()```, where each LI element contains CSS classes ```permgroup permgroup-ID```. By passing according to CSS classes ```permgroup```, HTML code with colored circles of rights groups is generated inside the LI element.

Clicking on the rights group selection box is processed in ```$(".DTE_Field_Name_editorFields\\.permGroups").on("click", "input", function() {``` and causes the CSS class ```permgroup-ID-checked``` to be added to the ```body``` element. This in turn causes the circle representing the rights group to be filled in.