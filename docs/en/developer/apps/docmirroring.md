# Mirroring the structure

A user-friendly description of structure mirroring is in the [editor documentation](../../redactor/apps/docmirroring/README.md). This documentation contains only a technical description.

The basic java package is ```sk.iway.iwcm.components.structuremirroring``` and the directory for JSP components is ```/components/structuremirroring/```.

## Connection method

The linking of individual directories and pages uses the database attribute ```sync_id```, originally used for synchronization between WebJETs. However, such functionality is not currently used (export and import in XML format are used) and the given attribute was not used.

The value from the key generator is set to the attribute ```sync_id``` for linked directories and pages by calling ```PkeyGenerator.getNextValue("structuremirroring")```. The value ```sync_id``` of the directories or pages linked to each other must of course be the same. The value in ```sync_id``` allows you to search for linked directories/pages in the database.

## Initializing the connection

The link is set in the configuration variable ```structureMirroringConfig``` where each line contains a list of directory IDs (main directories of a given language mutation) that are to be linked. Setting the configuration variable triggers an event that ```SaveListener.handleConfSave``` listens to and then triggers ```MirroringService.checkRootGroupsConfig()```. Here the attribute ```syncId``` of the defined directories is checked, if it is not set it is set automatically.

The call ```MirroringService.isEnabled(int groupId)``` verifies whether structure mirroring is enabled for the specified directory ID.

## Linking directories

Directory linking is implemented by listening to the directory change event ```SaveListener.handleGroupSave```, which in turn calls ```GroupMirroringServiceV9.handleGroupSave```.

Note: the exception is for the directory named **New subdirectory** (translation key ```editor.dir.new_dir```), which is created in the tree structure by clicking on add new directory in the context menu. This would create directories ```Nový podadresár``` in other language mutations, which would have no practical meaning (since changing the directory name does not change the created mirror copies).

The ```WebjetEventType.ON_START``` event checks whether the directory already has ```syncId``` set. If not, we know that it is a new directory that is not yet mirrored and we set it to a new value of ```syncId```.

For event ```WebjetEventType.AFTER_SAVE```, i.e. after saving the directory according to ```syncId```, we look for mirrored directories. If none exist, new directories are created. The ```groupsDB.setGroup(mirror, false);``` API is used with a second ```false``` attribute, which **does not** raise the event again after saving the directory (which would cause recursion).

If a link exists, it is checked whether the parent directory has changed. This is a relatively complicated detection. First, a list of linked directories for the current directory is obtained. Then, it is checked whether the actually linked directories have parents from the specified list. If not, the directory is moved to the correct one.

Verifying the sort priority is simple - the linked directories are checked and the sort priority value matches. If it is not the same, it is set.

In the event ```WebjetEventType.AFTER_DELETE```, calling ```GroupsDB.deleteGroup``` also deletes mirrored directories. These are typically moved to the trash. They will still have ```syncId``` set, but since they are outside the mirrored structure, subsequent changes to them are no longer mirrored.

## Linking websites

The web page link is implemented in ```DocMirroringServiceV9.handleDocSave(DocDetails doc, WebjetEventType type)```, which solves the problem of saving the web page in the page editor. The object ```DocDetails``` does not contain the value ```syncId```, so in the first step the value is set to the object (according to ```docId```).

Similar to the directory, a new value is generated for a page that does not have ```syncId``` set for the ```WebjetEventType.ON_START``` event.

The linked pages are retrieved at the ```WebjetEventType.AFTER_SAVE``` event. If none exist, they are created by calling ```DocDB.saveDoc(mirror, false)```, the ```false``` attribute solves the recursion problem (does not call the events again). The only problem that may arise is rights and approval, the current implementation does not solve this. If the editor does not have rights to another language mutation, the website will be created anyway.

If the currently saved page already has linked pages, similarly to a directory, the correctness of the parent directory and arrangement priorities is verified.

A page deletion is detected at ```SaveListener.handleDocSave(final WebjetEvent<DocDetails> event)```. On event ```WebjetEventType.AFTER_DELETE```, a list of mirrored pages is obtained and ```DeleteServlet.deleteDoc``` is called.

## Forcing tree structure to be restored

If a change in the structure requires a tree structure to be restored, the following attribute can be set in the events:

```java
RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
```

This is then validated in the REST services after the save is complete and a tree structure refresh is invoked. The call via ```RequestBean``` was used because it is available throughout ```requestu``` and is statically available.
