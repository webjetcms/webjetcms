# Cloning a structure

Using Structure Cloning, we can clone the entire contents of a directory in the site to another directory without having to recreate the entire directory structure. This option is available in the **Websites** section as **Structure Cloning**. After selecting this option, a window for cloning actions will appear. Typically, it is used to create a new language mutation of a website from an existing version. The language is taken from the source and target folder settings.

![](clone_structure_set_translator.png)

To perform a cloning action, you need to enter the source folder ID (which folder you are cloning) and the destination folder ID (where the source folder is being cloned to). You can enter the folder IDs directly if you remember them, or you can use the **Select** option, which opens a new window with a folder tree structure, where you select a specific folder by clicking on its name.

Cloning uses [Structure Mirroring](../docmirroring/README.md) and [Automatic Translation](../../../admin/setup/translation.md). A temporary link between the source and target folders is created for the duration of the cloning operation, valid only for this operation. It is saved in the configuration variable `structureMirroringConfig` only if the option **Keep structure mirroring active after cloning** is selected. Concurrent edits to other pages use the original mirroring settings.

All subfolders, including web pages, are cloned to the target folder. If a translator is set, the content is translated according to the languages ​​of the source and target folders. Cloned pages take on the display settings from the source and folders take on the display method in the menu; the `structureMirroringDisabledOnCreate` option does not hide them during cloning.

## Options

### Source directory ID

Set the ID of the folder to clone from.

### Destination directory ID

Set the ID of the folder to clone to. In this folder, pages and subfolders will be created based on the source folder.

### Keep mirroring active

If you select the option **Keep structure mirroring active after cloning**, the set [mirroring](../docmirroring/README.md) between the source and target folders will be preserved. Subsequently, when a new folder is created, or a web page is transferred between the mirrored folders.

You can also undo the setting later by editing the config variable `structureMirroringConfig` and deleting the line with the set folder IDs.

### Keep URL address

By selecting the **Keep URL** option, you are ensuring that the URLs of pages and folders will not be translated into the target folder's language variant. This means that the new language variant will have **the same URLs but a different prefix that they start with**.

Example:
Let's have folders SK (with Slovak language set) and EN (with Slovak language set).
The SK folder contains a subfolder **assets**, which has a main page with the same name. The address of such a page is **/sk/assets/**.
If we use structure cloning **without leaving the URL**, from the SK folder to the EN folder, the copy of this page will have the URL **/en/property/**.
If we use the structure cloning **while keeping the URL**, from the SK folder to the EN folder, the copy of this page will have the URL **/en/majetok/**. As we can see, the url was not translated, only the prefix was changed from /sk to /en, which represents the parent folder.

![](clone_structure_result.png)

## Translator

Since cloning uses [Automatic translation](../../../admin/setup/translation.md), information is displayed about which translator is configured and how many free characters are left for translation. If no translator is configured (e.g. if the license key for translator `DeepL` is not specified) or there are no free characters left for translation, we will be notified when cloning. In this case, the cloned structure **will** not be automatically translated.

![](clone_structure_no_set_translator.png)

## Canceling mirroring

The window offers the option to [cancel mirroring](../docmirroring/README.md) the selected folder. Simply select the folder at the bottom of the window and press the <button class="btn btn-sm btn-outline-secondary" type="button">Cancel mirroring</button> button. The value `sync_id` that provided mirroring for the selected folder and all subfolders as their pages will then be deleted.

To unmirror, you need the **Clone Structure** right and the right to edit the selected folder and all its subfolders. When domain separation is enabled, the entire selected structure must belong to the currently selected domain. If page access control by user group is enabled, access to each page in this structure is also verified. If any check fails, the operation is rejected and the links are preserved.

![](clone_structure_undo_sync.png)

!>**Warning:** During the unmirroring action, cloning cannot be started, as these actions would conflict with each other.
