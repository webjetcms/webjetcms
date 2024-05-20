# Dialers

The Dials application allows you to create/edit/delete and duplicate named types of dials, which can then be used to store the data of these dials. Dial types and data can also be exported and imported using a file.

![](dataTable_enumType.png)

## Types of dials

When creating a new dial type, you must specify a name that is unique. The other fields are optional. Note the "Strings" / "Numbers" / "Boolean" / "Dates" tabs, which contain several numbered fields. These fields are used to define what format the data of a given dial will have. If you give a name to a field, the dial data will generate a field with a name that corresponds to the specified text and type of the given card.

![](editor_enumType.png)

Example: if you fill in 2 fields in the "Strings" tab, 2 columns/fields of the string type with the names that have been entered will be displayed in the codebook data.

![](editor_stringTab.png)

This means that you can define the dial data format for each dial separately. As the tab names suggest, you can combine text, numeric, Boolean or date fields. Their number is limited for each type by the number of fields in each tab.

### Basic tab

In the "Basic" tab, the properties are set:
- Type name - a unique name for the dial type, must not be empty.
- Link to dial - select from all dials to link the dial.
- Enable link to dialpad - enable link to another dialpad (same as in the case of the dialpad type).
- Enable parent link to this dial's data - determines whether the data of this dial will be allowed to select the parent link.
You cannot have "dial link" and "parent link" enabled for data at the same time.

**Link to the dial** has some limitations and therefore some options are either not selectable or return an error message.
1.  Linking the dial to itself is disabled. If you select to link to another dial for dial X, dial X will also be in the list, but this option will not be selectable.

![](editor_select_1.png)

2.  Circular linking of dials. If dial X chooses to link to dial Y, dial Y cannot link back to dial X. Although the X option will be visible to Y and he will be able to select it, but an error message will be displayed when he tries to save it.

![](editor_select_2.png)

1.  Link to an already deleted dial. It may happen that dial X was linked to dial Z, which was subsequently deleted. In this case you will see the deleted dial Z in the options with the prefix "(!deleted)\_". This prefix will make it clear that the dial has already been deleted and such an option will not be selectable. However, a dial X that was linked before the deletion of dial Z will retain the link. It will be possible to change this link, but once changed, it will not be able to link back to the deleted dial again.

![](editor_select_3.png)

**Enable link to dialpad** if enabled, individual dial data will be able to link to dials. Also in this case there are some limitations.
1.  Linking to the dials from which the data is based is disabled. If the data you are creating under dial X has dial linking enabled, the option to dial X will not be selectable.

2.  Link to an already deleted dial. This case has the same conditions as **Link to the dial** for the dial type.

**Enable the parent link to the data of this dialer** if enabled, individual dial data will be able to choose a parent from among the other data following the same dial. Also in this case there are some limitations.
1.  In this case, one condition must be met. To use the parent link, the dial type must have an option defined for `Reťazec 1`. The reason is that the value in the generated column `Reťazec 1` will be used as an identifier to select a link to the parent.

2.  Linking a dial entry to itself is disabled. If dial X has a parent link enabled and you create a parent link under this dial `záznamA, záznamB a záznamC`, so you such a record can not be a parent. For `záznamA` the possibility of parental connection to each other will be made impossible (will see the possibility of `zaznamA` but you will not be able to select it).

**It is important to note**that if the 'dial link permission' or 'parent link permission' is revoked, all links that have been created to the data of this dial will be removed, even if this permission is granted again.
As an example, consider a situation where we create a dial type with X and this dial type allows "link to dial". Under this dial type, we will create a record that has dial linking enabled and will link to dial Z. If we then remove the "link to dialer" permission for dialer type X, the record of this dialer that has linked to dialer Z will lose this link permanently. If we enable "link to dialer" again, the option would appear for the record but the previous option to dialer Z would be reset.

## List of dial data

Dial Data allows you to edit the data of created dial types. It is necessary to select the dial to be managed from the menu in the header of the page. After selecting a specific dial, its corresponding data will be displayed. If the dial type has some columns unnamed, these columns and their data will not be displayed.

![](dataTable_enumData.png)

Example:

When creating the X codebook, we filled in the fields `reťazec1, reťazec2`. If we create a new record (new data) for the X dialer, we will generate 2 fields of the string type in the editor. The names of these fields will be the same as what we entered when we created the X dialer. Also, the table will contain only these 2 defined columns. Of course, if dial X has allowed it, we will be able to select a parent or a link to the dial when creating the data (these selections are described in more detail earlier in the chapter).

![](editor_enumData.png)

## Data deletion

By default, when you delete a record of a codebook or data type, it is not physically deleted from the database, but marked as deleted. This is to protect against data retrieval errors in old data. For example, if the Car Color dial is used and we no longer want a color to be selected for new records, but at the same time the color needs to be displayed in the old records. Technically it is possible to restore a deleted record directly in the database by setting the attribute `hidden`, but the user interface does not currently allow it.
