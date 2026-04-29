# Field Type - JSON

A website or Scripts application uses a 1:N mapping to other objects. In the case of a website, this is the selected directory and the copy of the website in the directories, and in the case of the Scripts application, this is the mapping of the script to the directory and the website.

The JSON field for DT Editor implements a **UI for displaying directory or web page selection** from a JS tree component with the ability to set a JSON object for **a single field or a list of fields**.

In JSON data from the server, this mapping is returned as:

- [private GroupDetails groupDetails](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) for the directory where the website is located
- [private List<GroupDetails> groupCopyDetails](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) for a copy of the page in directories
- [List<InsertScriptGroupBean> groupIds](../../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) for the Scripts application mapping to directories
- [List<InsertScriptDocBean> docIds](../../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) for the Scripts mapping to web pages application
- [List<DirTreeItem> writableFolders](../../../../src/main/java/sk/iway/iwcm/components/users/userdetail/UserDetailsEditorFields.java) for selecting a list of directories in the file system

The attributes listed use the ```@DataTableColumn(inputType = DataTableColumnType.JSON, className = "dt-tree-group"``` annotation, which is the JSON field type. The className attribute determines the behavior of the returned object.

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

Note the use of the ```data-dt-json-addbutton``` attribute to set the text of the button in the directory list. The keys ```editor.json.addPage``` are provided for adding a website and ```editor.json.addGroup``` for adding a directory.

> **Warning:** if you use client paging (attribute ```serverSide: false```) then empty ```List<>``` objects need to be returned as ```null``` instead of an empty field (otherwise the value in the datatable will not be updated after deleting all directories/pages from the list).

## ClassName options

```dt-tree-group``` - vrátený JSON objekt typu ```GroupDetails``` a nahradí aktuálnu hodnotu. Ak má byť možnosť vybrať aj koreňový priečinok zadajte ```dt-tree-group-root```. Možné je zadať koreňový priečinok ako ```@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "23")``` kde `23` je ID priečinka vo webových stránkach, ktorý chcete použiť ako koreňový. Možné je zadať aj cestu k priečinku ```@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")```.
![](field-json-group.png)

```dt-tree-group-null``` - vrátený JSON objekt typu ```GroupDetails``` alebo ```NULL``` - umožňuje nastaviť žiadnu/prázdnu hodnotu poľu (napr. pre voliteľné pole), v GUI sa zobrazí aj ikona koša pre zmazanie hodnoty.

```dt-tree-group-array``` - vrátený JSON objekt typu ```GroupDetails``` a vytvára pole hodnôt s možnosťou pridať objekt, nahradiť niektorý existujúci a zmazať objekt.
![](field-json-group-array.png)

```dt-tree-groupid/dt-tree-groupid-root``` - nastavuje do poľa len ID priečinku, do ```data-text``` nastaví cestu k priečinku (```fullPath```). Používa sa mimo editora v aplikácii štatistika pre výber priečinka.

```dt-tree-page``` - vrátený JSON objekt typu ```DocDetails``` a nahradí aktuálnu hodnotu. Možné je zadať koreňový priečinok ako ```@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "23")``` kde `23` je ID priečinka vo webových stránkach, ktorý chcete použiť ako koreňový. Možné je zadať aj cestu k priečinku ```@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")```.
![](field-json-page.png)

```dt-tree-page-null``` - vrátený JSON objekt typu ```DocDetails``` alebo ```NULL``` - umožňuje nastaviť žiadnu/prázdnu hodnotu poľu (napr. pre voliteľné pole), v GUI sa zobrazí aj ikona koša pre zmazanie hodnoty.

![](field-json-page-null.png)

```dt-tree-page-array``` - vrátený JSON objekt typu ```DocDetails``` a vytvára pole hodnôt s možnosťou pridať objekt, nahradiť niektorý existujúci a zmazať objekt.
![](field-json-page-array.png)

```dt-tree-dir``` - vrátený JSON objekt typu ```DirTreeItem``` pre **výber adresára v súborovom systéme**

```dt-tree-dir-simple``` - vrátený **reťazec** s hodnotou pre **výber adresára v súborovom systéme**, možné zadať koreňový priečinok ako ```@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/images/gallery")```. Do ```data-dt-field-skipFolders``` je možné zadať meno konfiguračnej premennej s čiarkou oddeleným zoznamom priečinkov, ktoré sa nezobrazia v stromovej štruktúre (skryté priečinky). Taktiež je možné skryť rodičov zvoleného koreňového priečinka pomocou ```@DataTableColumnEditorAttr(key = "data-dt-field-hideRootParents", value = "true")```, prednastavené sa rodičia koreňového priečinka zobrazujú, aj keď sa nedajú zvoliť (pre lepší prehľad štruktúry).

![](../../frontend/webpages/customfields/webpages-dir.png)

```dt-tree-dir-array``` - vrátený JSON objekt typu ```DirTreeItem``` pre **výber zoznamu adresárov v súborovom systéme**

```dt-tree-dir-array-root``` - vrátený JSON objekt typu ```DirTreeItem``` pre **výber zoznamu adresárov v súborovom systéme** vrátane možnosti Koreňový priečinok.

```dt-tree-universal-array``` - vrátený univerzálny JSON objekt zdedený z typu ```JsTreeItem``` pre zákazkové zobrazenie ```jsTree``` štruktúry

Pridaním prípony ```-alldomains``` je možné pre výber ```GroupDetails``` alebo ```DocDetails``` vypnúť filtrovanie podľa aktuálne zvolenej domény. Ako koreňové priečinky sa zobrazia všetky povolené domény a vybrať je možné priečinok/stránku zo všetkých dostupných domén. Používa sa napr. v používateľoch, kde sa nastavujú práva naprieč všetkými doménami.

## Použitie špecifických JSON objektov

Ak mapovaný objekt nie je priamo ```GroupDetails``` alebo ```DocDetails``` je potrebné nastaviť mapovanie vráteného objektu na požadovaný JSON formát.

Napríklad je potrebné, aby výstupný JSON objekt vyzeral nasledovne:

```javascript
{
    "id":0,
    "groupId":26,
    "domainId":1,
    "insertScriptGrId":0
}
```

The sample Java implementation is in [InsertScriptBean](../../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) where the following annotations are used:

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

It is important to mark ```inputType=DataTableColumnType.JSON``` and set the correct ```className```.

**TIP**: ```className``` can contain an additional ```suffix``` (for example ```dt-tree-group-array-insert-script```) for further differentiation in your code. For example, if you had multiple JSON objects of the same type ```GroupDetails``` and needed to generate different resulting JSON objects.

On the frontend, in the Datatable constructor, it is possible to define an object ```jsonField``` in which the function ```getItem``` converts the returned node from jstree (GroupDetails or DocDetails) to the target format. The function ```getKey``` is used to verify the existence of an object in the array, returning a unique object identifier.

An example is in the file [insert-script.pug](../../../../src/main/webapp/admin/v9/views/pages/apps/insert-script.pug), which provides conversion of standard ```DocDetails``` and ```GroupDetails``` objects to a format of the type:

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

If you need to implement your own tree view, you can take inspiration from the ```DirTreeRestController``` class and the ```DirTreeItem``` entity. It uses the basic view object ```jsTree - JsTreeItem```, which the VUE component can then work with. It is important to set the attributes in the ```JsTreeItem``` entity correctly.

Example of a REST service:

```java
package sk.iway.iwcm.system.elfinder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

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

and entities ```DirTreeItem```:

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

Example of an annotation with a specified REST service URL:

```java
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="useredit.addGroup", tab = "rightsTab", className = "dt-tree-universal-array", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/elfinder/tree")
        })
    })
    private List<DirTreeItem> writableFolders;
```

We recommend leaving the value ```className``` at the value ```dt-tree-universal-array``` so that the button for adding a record, deleting and replacing is displayed correctly (comparison by ID works correctly).

## Displaying the JSON value of a column in a Datatable

The Java bean used must contain the method ```getFullPath()``` or ```getVirtualPath()```, the value of which will be used when listing the value of the given object in the data table and editor:

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

!>**Warning**: json columns can only be displayed in the browser, value search cannot (yet) be used in server-side search.

If you cannot implement the ```getFullPath()``` method, we recommend using the ```hidden=true``` attribute to disable the display of the json column in the table. You can implement an additional attribute to display the value in the datatable, to which you set the ```hiddenEditor=true``` attribute to disable the attribute in the editor. This way you get a separate attribute for the editor and the datatable.

## Listening for a change in value

If you need to listen for a field value change outside of the VUE component, you can listen for the change event on the nested ```textarea``` element that contains the current JSON object:

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

[field-type-json.js](../../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-json.js) a new data type ```$.fn.dataTable.Editor.fieldTypes.json``` is defined. It is implemented using the VUE component [webjet-dte-jstree](../../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue). It also contains a hidden field of type ```textarea```, into which the current JSON object is copied. However, this field is only used to "inspect" the current data. The get function always returns the current data from the VUE component.

[datatables-config.js](../../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js) implements the ```renderJson(td, type, rowData, row)``` function to display data in a table. It iterates through the records using the ```fullPath``` attribute.

[webjet-dte-jstree.vue](../../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue) is the root component that passes the ```child``` record row component according to the data. For objects of type **array** it also displays a button to add a new record to the array.

The component uses [EventBus](../../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/event-bus.js) in which it listens for the event ```change-jstree```. This event occurs when a directory or web page in the JS Tree is clicked.

The ```processTreeItem(that, data)``` function processes a click on an object (DocDetails or GroupDetails) in the JS tree component. It performs validation and possible conversion of the JSON object.

[webjet-dte-jstree-item.js](../../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree-item.vue) is a row of a list of existing objects. In each row it displays a button for editing and deleting the record. The click is processed by the root component via a call to ```this.$parent.processTreeItem(this, data);```.

[vue-folder-tree.vue](../../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue) encapsulates the JS Tree library into a VUE component.

If ```Doc/GroupDetails``` is an object ```null```, no field would be displayed. Therefore, in ```field-type-json.js``` there is a function ```fixNullData```, which artificially creates a basic object for this case. If it is a web page, it contains the attribute ```docId=-1```, for a directory ```groupId=-1``` and for other objects ```id=-1```. The attribute ```fullPath``` is set to an empty value.
