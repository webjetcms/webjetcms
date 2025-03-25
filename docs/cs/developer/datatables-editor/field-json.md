# Field Type - JSON

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Field Type - JSON](#field-type---json)
  - [Možnosti className](#možnosti-classname)
  - [Použití specifických JSON objektů](#použití-specifických-json-objektů)
  - [Vlastní konfigurace zobrazené stromové struktury](#vlastní-konfigurace-zobrazené-stromové-struktury)
  - [Zobrazení hodnoty JSON sloupce v Datatable](#zobrazení-hodnoty-json-sloupce-v-datatable)
  - [Poslech na změnu hodnoty](#poslech-na-změnu-hodnoty)
  - [Implementační detaily](#implementační-detaily)

<!-- /code_chunk_output -->

Web stránky nebo aplikace Skripty používá mapování 1:N na další objekty. V případě web stránky je to vybrána adresář a kopie web stránky v adresářích a v případě aplikace Skripty je to mapování skriptu na adresáře a web stránky.

Pole typu JSON pro DT Editor implementuje **UI pro zobrazení výběru adresáře nebo web stránky** z JS tree komponenty s možností nastavení JSON objektu pro **jedno pole nebo seznam (List) polí**.

V JSON datech ze serveru je toto mapování vráceno jako:
- [private GroupDetails groupDetails](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) pro adresář ve kterém je web stránka
- [private List\<GroupDetails> groupCopyDetails](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) pro kopii stránky v adresářích
- [List\<InsertScriptGroupBean> groupIds](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) pro aplikaci Skripty mapování na adresáře
- [List\<InsertScriptDocBean> docIds](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) pro aplikaci Skripty mapování na web stránky
- [List\<DirTreeItem> writableFolders](../../../src/main/java/sk/iway/iwcm/components/users/UserDetailsEditorFields.java) pro výběr seznamu adresářů v souborovém systému

Uvedené atributy používají anotaci `@DataTableColumn(inputType = DataTableColumnType.JSON, className = "dt-tree-group"`, neboli typ pole JSON. Atribut className určuje chování vráceného objektu.

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

všimněte si použití atributu `data-dt-json-addbutton` pro nastavení textu tlačítka v seznamu adresářů. Připraveny jsou klíče `editor.json.addPage` pro přidání web stránky a `editor.json.addGroup` pro přidání adresáře.

> **Upozornění:** pokud používáte klientské stránkování (atribut `serverSide: false`) tak je třeba prázdné `List<>` objekty vrátit jako `null` místo prázdného pole (jinak se v datatabulce neaktualizuje hodnota po smazání všech adresářů/stránek ze seznamu).

## Možnosti className

`dt-tree-group` - vrácený JSON objekt typu `GroupDetails` a nahradí aktuální hodnotu. Pokud má být možnost vybrat i kořenovou složku zadejte `dt-tree-group-root`. Lze zadat kořenovou složku jako `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "23")` kde `23` je ID složky ve webových stránkách, který chcete použít jako kořenový. Je možné zadat i cestu ke složce `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")`. ![](field-json-group.png)

`dt-tree-group-null` - vrácený JSON objekt typu `GroupDetails` nebo `NULL` - umožňuje nastavit žádnou/prázdnou hodnotu poli (např. pro volitelné pole), v GUI se zobrazí i ikona koše pro smazání hodnoty.

`dt-tree-group-array` - vrácený JSON objekt typu `GroupDetails` a vytváří pole hodnot s možností přidat objekt, nahradit některý existující a smazat objekt. ![](field-json-group-array.png)

`dt-tree-groupid/dt-tree-groupid-root` - nastavuje do pole pouze ID složky, do `data-text` nastaví cestu ke složce (`fullPath`). Používá se mimo editor v aplikaci statistika pro výběr složky.

`dt-tree-page` - vrácený JSON objekt typu `DocDetails` a nahradí aktuální hodnotu. Lze zadat kořenovou složku jako `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "23")` kde `23` je ID složky ve webových stránkách, který chcete použít jako kořenový. Je možné zadat i cestu ke složce `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")`. ![](field-json-page.png)

`dt-tree-page-null` - vrácený JSON objekt typu `DocDetails` nebo `NULL` - umožňuje nastavit žádnou/prázdnou hodnotu poli (např. pro volitelné pole), v GUI se zobrazí i ikona koše pro smazání hodnoty. ![](field-json-page-null.png)

`dt-tree-page-array` - vrácený JSON objekt typu `DocDetails` a vytváří pole hodnot s možností přidat objekt, nahradit některý existující a smazat objekt. ![](field-json-page-array.png)

`dt-tree-dir` - vrácený JSON objekt typu `DirTreeItem` pro **výběr adresáře v souborovém systému**

`dt-tree-dir-simple` - vrácen **řetězec** s hodnotou pro **výběr adresáře v souborovém systému**, lze zadat kořenovou složku jako `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/images/gallery")`

![](../../frontend/webpages/customfields/webpages-dir.png)

`dt-tree-dir-array` - vrácený JSON objekt typu `DirTreeItem` pro **výběr seznamu adresářů v souborovém systému**

`dt-tree-universal-array` - vrácený univerzální JSON objekt zděděný z typu `JsTreeItem` pro zakázkové zobrazení `jsTree` struktury

Přidáním přípony `-alldomains` je možné pro výběr `GroupDetails` nebo `DocDetails` vypnout filtrování podle aktuálně zvolené domény. Jako kořenové složky se zobrazí všechny povolené domény a vybrat lze složku/stránku ze všech dostupných domén. Používá se například. v uživatelích, kde se nastavují práva napříč všemi doménami.

## Použití specifických JSON objektů

Pokud mapovaný objekt není přímo `GroupDetails` nebo `DocDetails` je třeba nastavit mapování vráceného objektu na požadovaný JSON formát.

Například je třeba, aby výstupní JSON objekt vypadal následovně:

```javascript
{
    "id":0,
    "groupId":26,
    "domainId":1,
    "insertScriptGrId":0
}
```

Ukázková Java implementace je v [InsertScriptBean](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) kde jsou použity následující anotace:

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

důležité je označení `inputType=DataTableColumnType.JSON` a nastavení správného `className`.

**TIP**: `className` může obsahovat doplňkový `suffix` (například `dt-tree-group-array-insert-script`) pro další rozlišení ve vašem kódu. Například pokud byste měli více JSON objektů stejného typu `GroupDetails` a potřebovali generovat rozdílné výsledné JSON objekty.

Na frontendu je v konstruktoru Datatable možné definovat objekt `jsonField` ve kterém funkce `getItem` konvertuje vrácený uzel z jstree (GroupDetails nebo DocDetails) na cílový formát. Funkce `getKey` se používá při ověřování existence objektu v poli, vrací jednoznačný identifikátor objektu.

Příklad je v souboru [insert-script.pug](../../../src/main/webapp/admin/v9/views/pages/apps/insert-script.pug), který zajišťuje konverzi standardních `DocDetails` a `GroupDetails` objektů na formát typu:

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

## Vlastní konfigurace zobrazené stromové struktury

Pokud potřebujete implementovat vlastní zobrazení stromové struktury můžete se inspirovat ve třídě `DirTreeRestController` a entite `DirTreeItem`. Používá základní objekt pro zobrazení `jsTree - JsTreeItem`, se kterým umí následně pracovat VUE komponenta. Důležité je korektní nastavení atributů v `JsTreeItem` entite.

Příklad REST služby:

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

a entity `DirTreeItem`:

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

Příklad anotace se zadanou URL adresou REST služby:

```java
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="useredit.addGroup", tab = "rightsTab", className = "dt-tree-universal-array", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/elfinder/tree")
        })
    })
    private List<DirTreeItem> writableFolders;
```

Hodnotu `className` doporučujeme ponechat na hodnotě `dt-tree-universal-array` aby se korektně zobrazilo tlačítko pro přidání záznamu, smazání a náhradu (korektně fungovalo srovnání podle ID).

## Zobrazení hodnoty JSON sloupce v Datatable

Použitý Java bean musí obsahovat metodu `getFullPath()` nebo `getVirtualPath()`, jejíž hodnota se použije při výpisu hodnoty daného objektu v datatabulce a editoru:

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

!>**Upozornění**: zobrazení json sloupců se `renderuje` až v prohlížeči, vyhledávání v hodnotě (zatím) nelze použít při serverovém vyhledávání.

Pokud metodu `getFullPath()` nemůžete implementovat doporučujeme použít atribut `hidden=true` pro vypnutí zobrazení json sloupce v tabulce. Můžete implementovat doplňkový atribut pro zobrazení hodnoty v datatabulce, kterému nastavíte atribut `hiddenEditor=true` pro vypnutí atributu v editoru. Tak získáte samostatný atribut pro editor a datatabulku.

## Poslech na změnu hodnoty

Pokud potřebujete poslouchat na změnu hodnoty pole mimo VUE komponenty lze poslouchat událost změny na vnořeném `textarea` elemente, které obsahuje aktuální JSON objekt:

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

## Implementační detaily

[field-type-json.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-json.js) je definován nový datový typ `$.fn.dataTable.Editor.fieldTypes.json`. Ten je implementován pomocí VUE komponenty [webjet-dte-jstree](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue). Obsahuje také skryté pole typu `textarea`, do kterého se kopíruje aktuální JSON objekt. Toto pole ale slouží pouze k "inspekci" aktuálních dat. Ve funkci get se vždy vrátí aktuální data z VUE komponenty.

[datatables-config.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js) implementuje funkci `renderJson(td, type, rowData, row)` pro zobrazení dat v tabulce. Ta prochází záznamy ze kterých použije atribut `fullPath`.

[webjet-dte-jstree.vue](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue) je kořenová komponenta, která podle dat prochází `child` komponentu řádku záznamu. Pro objekty typu **array** zobrazí také tlačítko pro přidání nového záznamu do pole.

Komponenta používá [EventBus](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/event-bus.js) ve kterém poslouchá na událost `change-jstree`. Tato událost nastane po kliknutí na adresář nebo web stránku v JS Tree.

Funkce `processTreeItem(that, data)` zpracuje kliknutí na objekt (DocDetails nebo GroupDetails) v JS tree komponentě. Provede validaci a případnou konverzi JSON objektu.

[webjet-dte-jstree-item.js](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree-item.vue) je řádek seznamu existujících objektů. V každém řádku zobrazuje tlačítko pro editaci a smazání záznamu. Klepnutí je zpracováno kořenovou komponentou přes volání `this.$parent.processTreeItem(this, data);`.

[vue-folder-tree.vue](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue) zapouzdřuje knihovnu JS Tree do VUE komponenty.

Pokud je `Doc/GroupDetails` objekt `null` nezobrazilo by se žádné pole. Proto v `field-type-json.js` je funkce `fixNullData`, která pro tento případ uměle vytvoří základní objekt. Pokud se jedná o web stránku obsahuje atribut `docId=-1`, pro adresář `groupId=-1` a pro ostatní objekty `id=-1`. Atribut `fullPath` je nastaven na prázdnou hodnotu.
