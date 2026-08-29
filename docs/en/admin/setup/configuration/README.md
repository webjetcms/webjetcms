# Configuration

The configuration section is used to view and manage configuration variables. On the left side there is a tree that divides the variables into the following views:

- **Changed** - variables whose value is stored in the database. This view is selected after opening the page.
- **Custom** - variables stored only in the database without definition in `Constants` /`ConstantsV9` or variables whose name starts with the current value `Constants.getInstallName()` (for example `aceintegration_test`).
- **All** - all registered variables, including their default values ​​and custom variables stored only in the database.
- **Modules** - hierarchical groups, for example `apps.gallery` or `security.oauth2`. Selecting a parent node will also display variables from all its subnodes. One variable can be included in multiple branches.

The tree can be searched. Module names are technical names and are not translated. In the selected module, it is possible to edit an existing variable, but not to create a new one or to start an import, because the database variable itself does not have information about being included in the module.

![](page.png)

The installation section contains a list of [most commonly used configuration variables](../../../install/config/README.md).

## Adding configuration variables

When adding, the most important parameter is **Configuration Name**, which behaves like a text field with auto-complete. When entering a configuration name, it will offer names of already existing variables, including those that are not yet edited (not in the table).

![](editor_1.png)

3 situations can occur:

- we will use autocomplete and select an existing configuration
  - if this configuration IS already in the table, **only an edit will be performed** (the existing record in the table will be edited)
  - if this configuration is NOT in the table, **a new entry is added** to the table **but** a new configuration variable is not added (we just changed its default value)
- we will not use autocomplete, **a new record will be added** to the table **and at the same time** we will define a completely new configuration variable

If we select the offered option, the current/default value of the entered configuration variable will be displayed in the editor.

![](editor_2.png)

The change usually takes effect immediately after the add/edit action. However, some configuration variables require a restart of the application server.

## Editing configuration items

3 situations can occur:

- we do not change the **Configuration name**, so the **edit** of the variable we were editing will be made
- we change the **Configuration name**, so the original configuration variable **will not be modified**
  - if the changed name is the name of an **existing** configuration variable, then the **edit** is performed
  - if the changed name is for the name of a **non-existent** configuration variable, then a new variable will be **added**

## Temporary value setting

If you need to change the value of a configuration variable only to verify its behavior, enable the **Set temporarily** option in the editor. The value is set only in memory on the current node, and is not stored in the database or propagated to other nodes in the cluster. After the application server is restarted, the value stored in the database is used again.

When set temporarily, the value cannot be encrypted or scheduled to change, so the **Encrypt** and **Change from** fields in the editor are hidden.

If the current value on the node differs from the saved value, the **Value** column displays both values ​​in the format "current value / saved value". The second value is displayed in a dimmed color and is marked as currently inactive when the cursor is placed. This can be the value stored in the database or the default value for a variable without a database entry. When the editor is reopened, the saved, not temporary, value is loaded into the **Value** field.

## Deleting configuration items

Deleting means resetting the database value. There can be 2 situations:

- if **a preset value exists**, it will be used and the variable will remain displayed in the **All** view and in the relevant modules; it will disappear from the **Changed** view,
- if **there is no preset value**, the custom database variable will cease to exist after reset.

A variable that does not have a database value cannot be deleted.
