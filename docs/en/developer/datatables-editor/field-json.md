# Field Type - JSON

The Scripts web page or application uses a 1:N mapping to other objects. In the case of a Web page, this is the selected directory and a copy of the Web page in the directories, and in the case of a Scripts application, it is a mapping of the script to directories and Web pages.

JSON type field for DT Editor implements **UI for displaying directory or web page selection** from JS tree component with the possibility to set JSON object for **one field or a list (List) of fields**.

In the JSON data from the server, this mapping is returned as:
- [private GroupDetails groupDetails](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) for the directory in which the web page is
- [private List\<GroupDetails> groupCopyDetails](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) for a copy of a page in directories
- [List\<InsertScriptGroupBean> groupIds](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) for application Scripts mapping to directories
- [List\<InsertScriptDocBean> docIds](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) for Scripts application mapping to web pages
- [List\<DirTreeItem> writableFolders](../../../src/main/java/sk/iway/iwcm/components/users/UserDetailsEditorFields.java) to select a list of directories in the file system

The above attributes use annotation `@DataTableColumn(inputType = DataTableColumnType.JSON, className = "dt-tree-group"`, i.e. the JSON field type. The className attribute is used to specify the behavior of the returned object.

```java
//jednoduche pole pre vyber adresara, ak je mozne vybrat aj root pouzite dt-tree-group-root
@DataTableColumn(inputType = DataTableColumnType.JSON, title="editor.superior_directory",
    tab = "basic", sortAfter = "externalLink", className = "dt-tree-group"
)
private GroupDetails groupDetails;

//pole pre vyber zoznamu adresarov
@DataTableColumn(inputType = DataTableColumnType.JSON, title="editor.webpage_copy_in",
    tab = "basic", sortAfter = "editorFields.groupDetails", className = "dt-tree-group-array")
private List<GroupDetails> groupCopyDetails;

//nastavenie textu tlacidla pre pridanie adresara atributom data-dt-json-addbutton
@DataTableColumn(inputType=DataTableColumnType.JSON, tab="scriptPerms", title="grouptree.title", className="dt-tree-group-array",
    editor = { @DataTableColumnEditor( attr = {
        @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addGroup")
    } )}
)
List<InsertScriptGroupBean> groupIds;

//vyber adresara suboroveho systemu (priamo String hodnota napr. /files/adresar)
@DataTableColumn(inputType = DataTableColumnType.JSON, className = "dt-tree-dir-simple", title="editor.baseFolder"
)
private String baseFolder = "";

//vyber adresara suboroveho systemu, vratene ako objekt DirTreeItem
@DataTableColumn(inputType = DataTableColumnType.JSON, className = "dt-tree-dir", title="editor.baseFolder"
)
private DirTreeItem baseFolder = "";

//vyber zoznamu adresarov suboroveho systemu
@DataTableColumn(inputType = DataTableColumnType.JSON, title="user.writableFolders.label", tab = "rightsTab", className = "dt-tree-dir-array", editor = {
    @DataTableColumnEditor(attr = {
        @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "useredit.addGroup")
    })
})
private List<DirTreeItem> writableFolders;

//vyber z roznych domen - vypis nie je filtrovany podla aktualne zvolenej domeny
@DataTableColumn(inputType = DataTableColumnType.JSON, title="user.editableGroups.label", tab = "rightsTab", hidden = true, className = "dt-tree-group-array-alldomains", editor = {
    @DataTableColumnEditor(attr = {
        @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.group.rights"),
        @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addGroup") })
})
private List<GroupDetails> editableGroups;
```

note the use of the attribute `data-dt-json-addbutton` to set the button text in the directory listing. Keys are ready `editor.json.addPage` to add a web page and `editor.json.addGroup` to add a directory.

> **Warning:** if you use client paging (attribute `serverSide: false`) so you need a blank `List<>` objects return as `null` instead of an empty field (otherwise the value in the datatable is not updated after deleting all directories/pages from the list).

## Options className

`dt-tree-group` - returned JSON object of type `GroupDetails` and replaces the current value. If the option to select the root folder should also be available enter `dt-tree-group-root`. It is possible to specify the root folder as `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "23")` Where `23` is the ID of the folder in the website that you want to use as the root. It is also possible to specify the path to the folder `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")`. ![](field-json-group.png)

`dt-tree-group-null` - returned JSON object of type `GroupDetails` or `NULL` - allows to set no/empty value to a field (e.g. for an optional field), also a trash can icon is displayed in the GUI to delete the value.

`dt-tree-group-array` - returned JSON object of type `GroupDetails` and creates an array of values with the ability to add an object, replace an existing object, and delete an object. ![](field-json-group-array.png)

`dt-tree-groupid/dt-tree-groupid-root` - sets only the folder ID in the field, to `data-text` sets the path to the folder (`fullPath`). It is used outside the editor in the statistics application for folder selection.

`dt-tree-page` - returned JSON object of type `DocDetails` and replaces the current value. It is possible to specify the root folder as `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "23")` Where `23` is the ID of the folder in the website that you want to use as the root. It is also possible to specify the path to the folder `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")`. ![](field-json-page.png)

`dt-tree-page-null` - returned JSON object of type `DocDetails` or `NULL` - allows to set no/empty value to a field (e.g. for an optional field), also a trash can icon is displayed in the GUI to delete the value. ![](field-json-page-null.png)

`dt-tree-page-array` - returned JSON object of type `DocDetails` and creates an array of values with the ability to add an object, replace an existing object, and delete an object. ![](field-json-page-array.png)

`dt-tree-dir` - returned JSON object of type `DirTreeItem` For **directory selection in the file system**

`dt-tree-dir-simple` - returned **Chain** with value for **directory selection in the file system**, it is possible to specify the root folder as `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/images/gallery")`. Do `data-dt-field-skipFolders` it is possible to specify the name of a configuration variable with a comma-separated list of folders that do not appear in the tree structure.

![](../../frontend/webpages/customfields/webpages-dir.png)

`dt-tree-dir-array` - returned JSON object of type `DirTreeItem` For **selecting a list of directories in the file system**

`dt-tree-dir-array-root` - returned JSON object of type `DirTreeItem` For **selecting a list of directories in the file system** including the Root Folder option.

`dt-tree-universal-array` - returned universal JSON object inherited from type `JsTreeItem` for custom display `jsTree` structures

By adding the suffix `-alldomains` it is possible to select `GroupDetails` or `DocDetails` disable filtering by the currently selected domain. All allowed domains are displayed as root folders and you can select a folder/page from all available domains. This is used e.g. in users where rights are set across all domains.

## Using specific JSON objects

If the mapped object is not directly `GroupDetails` or `DocDetails` it is necessary to set the mapping of the returned object to the required JSON format.

For example, the output JSON object needs to look like this:

```javascript
{
    "id":0,
    "groupId":26,
    "domainId":1,
    "insertScriptGrId":0
}
```

A sample Java implementation is in [InsertScriptBean](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) where the following annotations are used:

```java
@JsonManagedReference(value="insertScriptBeanGr")
@OneToMany(mappedBy="insertScriptBeanGr",fetch=FetchType.LAZY,cascade={CascadeType.ALL},orphanRemoval=true)
@DataTableColumn(inputType=DataTableColumnType.JSON, tab="scriptPerms", title="grouptree.title", className="dt-tree-group-array")
List<InsertScriptGroupBean> groupIds;

@JsonManagedReference(value="insertScriptBeanDoc")
@OneToMany(mappedBy="insertScriptBeanDoc",fetch=FetchType.LAZY,cascade={CascadeType.ALL},orphanRemoval=true)
@DataTableColumn(inputType=DataTableColumnType.JSON, tab="scriptPerms", title="components.insert_script.choose_pages", className="dt-tree-page-array")
List<InsertScriptDocBean> docIds;
```

the important thing is the marking `inputType=DataTableColumnType.JSON` and setting the correct `className`.

**TIP**: `className` may contain additional `suffix` (for example `dt-tree-group-array-insert-script`) for further resolution in your code. For example, if you had multiple JSON objects of the same type `GroupDetails` and needed to generate different resulting JSON objects.

On the frontend it is possible to define an object in the Datatable constructor `jsonField` wherein the function `getItem` converts the returned node from the jstree (GroupDetails or DocDetails) to the target format. Function `getKey` is used when verifying the existence of an object in an array, it returns the unique identifier of the object.

An example is in the file [insert-script.pug](../../../src/main/webapp/admin/v9/views/pages/apps/insert-script.pug), which provides conversion of standard `DocDetails` a `GroupDetails` objects to type format:

```javascript
insertScriptTable = WJ.DataTable({
    id: 'insertScriptTable',
    url: '/admin/rest/components/insert-script',
    columns: columns,
    tabs: tabs,
    serverSide: false,
    editorId: "id",
    jsonField: {
        getItem: function(props, data) {
            let obj;
            if (props.click.indexOf("dt-tree-page")==0) {
                //web stranka - DocDetails objekt
                let doc = data.node.original.docDetails;
                //pre ziskanie fullPath potrebujeme aj parent GroupDetails objekt
                let group = data.parentNode.original.groupDetails;
                obj = {
                    id: 0,
                    docId: doc.docId,
                    fullPath: group.fullPath + "/" + doc.title,
                    insertScriptDocId: 0
                }
            }
            else {
                //zvoleny bol adresar - GroupDetails objekt
                let group = data.node.original.groupDetails;
                obj = {
                    id: 0,
                    groupId: group.groupId,
                    domainId: 1,
                    insertScriptGrId: 0,
                    fullPath: group.fullPath
                }
            }

            return obj;
        },
        getKey: function(props, data) {
            let key;
            if (props.click.indexOf("dt-tree-page")==0) key = data.docId;
            else key = data.groupId;
            return key;
        }
    }
});
```

## Custom configuration of the displayed tree structure

If you need to implement a custom tree structure view, you can take inspiration from the `DirTreeRestController` an entity `DirTreeItem`. Uses a base object to display `jsTree - JsTreeItem`, which the VUE component can subsequently work with. It is important to correctly set the attributes in the `JsTreeItem` entity.

Example of a REST service:

```java
package sk.iway.iwcm.system.elfinder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeMoveItem;
import sk.iway.iwcm.admin.jstree.JsTreeRestController;
import sk.iway.iwcm.io.IwcmFile;

@RestController
@RequestMapping(value = "/admin/rest/elfinder/tree")
@PreAuthorize(value = "@WebjetSecurityService.isAdmin()")
public class DirTreeRestController extends JsTreeRestController<DirTreeItem> {

    @Override
    protected void tree(Map<String, Object> result, JsTreeMoveItem item) {
        String parentPath = item.getId();
        if ("-1".equals(parentPath)) parentPath = "/";

        boolean isRoot = "/".equals(parentPath);

        IwcmFile directory = new IwcmFile(Tools.getRealPath(parentPath));
        List<IwcmFile> files = Arrays.asList(FileTools.sortFilesByName(directory.listFiles(file -> {
            if (file.isFile()) return false;
            if (isRoot==false && file.isJarPackaging()) return false;

            return true;
        })));

        List<DirTreeItem> items = files.stream().map(f -> new DirTreeItem(f)).collect(Collectors.toList());

        result.put("result", true);
        result.put("items", items);
    }

    @Override
    protected void move(Map<String, Object> result, JsTreeMoveItem item) {
        result.put("result", false);
        result.put("error", getProp().getText("components.jstree.access_denied__group"));
        return;
    }

    @Override
    protected void save(Map<String, Object> result, DirTreeItem item) {
        result.put("result", false);
        result.put("error", getProp().getText("components.jstree.access_denied__group"));
        return;
    }

    @Override
    protected void delete(Map<String, Object> result, DirTreeItem item) {
        result.put("result", false);
        result.put("error", getProp().getText("components.jstree.access_denied__group"));
        return;
    }

    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        //TODO: kontrola prav na zobrazenie adresara
        return true;
    }
}
```

and entity `DirTreeItem`:

```java
package sk.iway.iwcm.system.elfinder;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.admin.jstree.JsTreeItemType;
import sk.iway.iwcm.io.IwcmFile;

@Getter
@Setter
public class DirTreeItem extends JsTreeItem {

    public DirTreeItem() {
        super();
    }

    public DirTreeItem(IwcmFile f) {
        super();
        setId(f.getVirtualPath());
        setText(f.getName());
        setVirtualPath(f.getVirtualPath());

        setIcon("ti ti-folder");
        setState(new JsTreeItemState());

        setType(JsTreeItemType.DIR);
        setChildren(hasChildren(f));
    }

    public DirTreeItem(String virtualPath) {
        this(new IwcmFile(Tools.getRealPath(virtualPath)));
    }

    private boolean hasChildren(IwcmFile f) {
        IwcmFile[] subfiles = f.listFiles();
        if (subfiles.length>0) return true;
        return false;
    }
}
```

An example of an annotation with the specified REST service URL:

```java
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="useredit.addGroup", tab = "rightsTab", className = "dt-tree-universal-array", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/elfinder/tree")
        })
    })
    private List<DirTreeItem> writableFolders;
```

Value `className` we recommend to leave at the value `dt-tree-universal-array` to correctly display the add, delete and replace button (ID comparison works correctly).

## Display JSON column value in Datatable

The Java bean used must contain a method `getFullPath()` or `getVirtualPath()` whose value is used when listing the value of the object in the datatable and the editor:

```java
/**
 * Vratenie cesty pre vue komponentu
 * @return
 */
@JsonProperty(access = Access.READ_ONLY)
public String getFullPath() {
    DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
    if (doc != null) {
        GroupDetails grp = GroupsDB.getInstance().getGroup(doc.getGroupId());
        if (grp != null) return grp.getFullPath()+"/"+doc.getTitle();
    }
    return "";
}
```

!>**Notice**: display json columns with `renderuje` only in the browser, the search in the value (yet) cannot be used in server searches.

If the method `getFullPath()` you cannot implement we recommend using the attribute `hidden=true` to disable the display of json columns in the table. You can implement an additional attribute to display the value in the datatable, which you set the attribute `hiddenEditor=true` to disable the attribute in the editor. This gives you a separate attribute for the editor and the datatable.

## Listening for a change in value

If you need to listen for a field value change outside of the VUE component it is possible to listen for a change event on a nested `textarea` element that contains the current JSON object:

```javascript
$("#DTE_Field_editorFields-parentGroupDetails").on("change", function(e) {
    //console.log("Input changed, e=", e);
    let json = JSON.parse($(e.target).val());
    //console.log("json=", json);
    //tu pouzivame groupId, lebo je to objekt parent adresara
    let groupId = json[0].groupId;
    showHideDomainName(groupId);
});
```

## Implementation details

[field-type-json.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-json.js) a new data type is defined `$.fn.dataTable.Editor.fieldTypes.json`. This is implemented using the VUE component [webjet-dte-jstree](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue). It also contains a hidden field of type `textarea` to which the current JSON object is copied. But this field is only used to "inspect" the current data. In the get function, the current data from the VUE component is always returned.

[datatables-config.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js) implements the function `renderJson(td, type, rowData, row)` to display the data in the table. The latter passes the records from which it uses the attribute `fullPath`.

[webjet-dte-jstree.vue](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue) is the root component, which, according to the data, passes through `child` component of the record line. For objects of type **array** also displays a button to add a new record to the field.

The component uses [EventBus](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/event-bus.js) in which he listens to the event `change-jstree`. This event occurs when you click on a directory or web page in the JS Tree.

Feature `processTreeItem(that, data)` handles clicking on an object (DocDetails or GroupDetails) in the JS tree component. Performs validation and possible JSON conversion of the object.

[webjet-dte-jstree-item.js](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree-item.vue) is a line of the list of existing objects. Each row displays a button for editing and deleting the record. The click is handled by the root component via a call `this.$parent.processTreeItem(this, data);`.

[vue-folder-tree.vue](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue) encapsulates the JS Tree library into a VUE component.

If `Doc/GroupDetails` object `null` no field would appear. Therefore, in `field-type-json.js` is a function of `fixNullData` which artificially creates a base object for this case. If it is a web page it contains the attribute `docId=-1`, for the directory `groupId=-1` and for other objects `id=-1`. Attribute `fullPath` is set to an empty value.
