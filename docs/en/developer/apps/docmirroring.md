# Mirroring the structure

The user description of the mirroring structure is in [documentation for the editor](../../redactor/apps/docmirroring/README.md). Only the technical description is included in this documentation.

The basic java package is `sk.iway.iwcm.components.structuremirroring` and directory for JSP components `/components/structuremirroring/`.

## Linking method

Linking individual directories and pages uses the database attribute `sync_id`, originally used for synchronization between WebJets. However, such functionality is not currently used (export and import in XML format is used) and the attribute has not been used.

In the attribute `sync_id` the linked directories and pages are set to a value from the key generator by calling `PkeyGenerator.getNextValue("structuremirroring")`. Value `sync_id` of interlinked directories or pages must of course be the same. According to the value in `sync_id` linked directories/pages can be searched in the database.

## Link initialization

The link is set in the configuration variable `structureMirroringConfig` where each line lists the directory IDs (the main directories of that language) to be linked. Setting a configuration variable fires an event that listens `SaveListener.handleConfSave` and subsequently invokes `MirroringService.checkRootGroupsConfig()`. Here the attribute is checked `syncId` of defined directories, if not set it will be set automatically.

Calling `MirroringService.isEnabled(int groupId)` verifies that structure mirroring is enabled for the specified directory ID.

## Linking directories

Directory linking is done by listening for a directory change event `SaveListener.handleGroupSave`, which subsequently calls `GroupMirroringServiceV9.handleGroupSave`.

Note: the exception is for the directory named **New subdirectory** (translation key `editor.dir.new_dir`), which will be created in the tree structure by clicking Add new directory in the context menu. This would create directories in other languages `Nový podadresár` which would be of no practical use (since changing the directory name no longer changes the mirrored copies created).

In an event `WebjetEventType.ON_START` is checked to see if the directory has already set `syncId`. If not, we know that it is a new directory that is not yet mirrored and we set a new value for it `syncId`.

In an event `WebjetEventType.AFTER_SAVE`, that is, after saving the directory according to `syncId` looking for mirrored directories. If none exist, new directories are created. The API is used `groupsDB.setGroup(mirror, false);` with the other `false` an attribute that **does not bring up** event again after saving the directory (which would cause recursion).

When a link exists, it verifies that a parent directory change has occurred. This is a rather complicated detection. First, a list of linked directories is obtained for the current directory. Then it is verified that the actually linked directories have parents from that list. If not, a directory move to the correct one is performed.

Verifying the ordering priority is straightforward - linked directories and a matching ordering priority value are verified. If it is not the same, it is set.

In an event `WebjetEventType.AFTER_DELETE` with the call `GroupsDB.deleteGroup` mirrored directories are also deleted. These are typically moved to the Recycle Bin. They will remain set `syncId`, but since they are outside the mirrored structure, their subsequent changes are no longer mirrored.

## Linking websites

The linking of web pages is implemented in `DocMirroringServiceV9.handleDocSave(DocDetails doc, WebjetEventType type)`, which solves the saving of the web page in the page editor. Object `DocDetails` but does not contain the value `syncId`, so in the first step the value to the object is set (according to `docId`).

Similar to the directory for the event `WebjetEventType.ON_START` for a page that `syncId` has not been set will generate a new value.

In an event `WebjetEventType.AFTER_SAVE` linked pages are obtained. If none exist, they are created by calling `DocDB.saveDoc(mirror, false)`, attribute `false` solves the problem with recursion (does not trigger events again). The only problem that may arise is rights and approvals, the current implementation does not address this. If the editor doesn't have rights to another language the web page will still be created.

If the currently stored page already has linked pages, as for a directory, the correctness of the parent directory and the ordering priority is verified.

Deleting a page is detected in `SaveListener.handleDocSave(final WebjetEvent<DocDetails> event)`. In an event `WebjetEventType.AFTER_DELETE` the list of mirrored pages is retrieved and the `DeleteServlet.deleteDoc`.

## Enforcing the restoration of the tree structure

If a change in the structure requires the tree structure to be restored, an attribute can be set in the events:

```java
RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
```

This is then verified in the REST services after the save is complete and the tree structure restore is invoked. Calling via `RequestBean` has been used on the ground that it is available throughout the `requestu` and is statically available.
