# Typ pole - JSON

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Typ pole - JSON](#typ-pole---json)
  - [Možnosti className](#možnosti-názvu-třídy)
  - [Použití specifických objektů JSON](#použití-specifických-objektů-json)
  - [Vlastní konfigurace zobrazené stromové struktury](#vlastní-konfigurace-zobrazené-stromové-struktury)
  - [Zobrazení hodnoty sloupce JSON v Datatable](#zobrazit-hodnotu-sloupce-json-v-datové-tabulce)
  - [Naslouchání změně hodnoty](#naslouchání-změně-hodnoty)
  - [Implementační detaily](#podrobnosti-o-provádění)

<!-- /code_chunk_output -->

Webová stránka nebo aplikace Skripty používá mapování 1:N na jiné objekty. V případě webové stránky je to vybraný adresář a kopie webové stránky v adresářích a v případě aplikace Scripts je to mapování skriptu na adresáře a webové stránky.

Pole typu JSON pro DT Editor implementuje **Uživatelské rozhraní pro zobrazení výběru adresáře nebo webové stránky** z JS stromové komponenty s možností nastavit JSON objekt pro **jedno pole nebo seznam (List) polí**.

V datech JSON ze serveru je toto mapování vráceno jako:
- [private GroupDetails groupDetails](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) pro adresář, ve kterém je webová stránka
- [\<GroupDetails> private Seznam groupCopyDetails](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) pro kopii stránky v adresářích
- [\<InsertScriptGroupBean> Seznam groupIds](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) pro mapování aplikačních skriptů do adresářů
- [\<InsertScriptDocBean> Seznam docIds](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) pro mapování aplikace Skripty na webové stránky
- [\<DirTreeItem> Seznam writableFolders](../../../src/main/java/sk/iway/iwcm/components/users/UserDetailsEditorFields.java) vybrat seznam adresářů v souborovém systému.

Výše uvedené atributy používají anotaci `@DataTableColumn(inputType = DataTableColumnType.JSON, className = "dt-tree-group"`, tj. typ pole JSON. Atribut className slouží k určení chování vráceného objektu.

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

všimněte si použití atributu `data-dt-json-addbutton` nastavit text tlačítka ve výpisu adresáře. Klávesy jsou připraveny `editor.json.addPage` přidat webovou stránku a `editor.json.addGroup` přidat adresář.

> **Varování:** pokud používáte stránkování klienta (atribut `serverSide: false`), takže potřebujete prázdný `List<>` objekty vrátí jako `null` místo prázdného pole (jinak by se hodnota v datové tabulce po vymazání všech adresářů/stránek ze seznamu neaktualizovala).

## Možnosti className

`dt-tree-group` - vrácený objekt JSON typu `GroupDetails` a nahradí aktuální hodnotu. Pokud by měla být k dispozici také možnost výběru kořenové složky, zadejte příkaz `dt-tree-group-root`. ![](field-json-group.png)

`dt-tree-group-null` - vrácený objekt JSON typu `GroupDetails` nebo `NULL` - umožňuje nastavit poli žádnou/prázdnou hodnotu (např. pro nepovinné pole), v grafickém uživatelském rozhraní se také zobrazí ikona koše pro odstranění hodnoty.

`dt-tree-group-array` - vrácený objekt JSON typu `GroupDetails` a vytváří pole hodnot s možností přidat objekt, nahradit existující objekt a odstranit objekt. ![](field-json-group-array.png)

`dt-tree-groupid/dt-tree-groupid-root` - nastaví v poli pouze ID složky, na `data-text` nastaví cestu ke složce (`fullPath`). Používá se mimo editor v aplikaci statistiky pro výběr složek.

`dt-tree-page` - vrácený objekt JSON typu `DocDetails` a nahradí aktuální hodnotu. ![](field-json-page.png)

`dt-tree-page-null` - vrácený objekt JSON typu `DocDetails` nebo `NULL` - umožňuje nastavit poli žádnou/prázdnou hodnotu (např. pro nepovinné pole), v grafickém uživatelském rozhraní se také zobrazí ikona koše pro odstranění hodnoty. ![](field-json-page-null.png)

`dt-tree-page-array` - vrácený objekt JSON typu `DocDetails` a vytváří pole hodnot s možností přidat objekt, nahradit existující objekt a odstranit objekt. ![](field-json-page-array.png)

`dt-tree-dir` - vrácený objekt JSON typu `DirTreeItem` Pro **výběr adresáře v souborovém systému**

`dt-tree-dir-simple` - vrátil **Řetěz** s hodnotou pro **výběr adresáře v souborovém systému**, je možné zadat kořenovou složku jako `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/images/gallery")`

![](../../frontend/webpages/customfields/webpages-dir.png)

`dt-tree-dir-array` - vrácený objekt JSON typu `DirTreeItem` Pro **výběr seznamu adresářů v souborovém systému.**

`dt-tree-universal-array` - vrácený univerzální JSON objekt zděděný z typu `JsTreeItem` pro vlastní zobrazení `jsTree` struktury

Přidáním přípony `-alldomains` je možné vybrat `GroupDetails` nebo `DocDetails` zakázat filtrování podle aktuálně vybrané domény. Všechny povolené domény se zobrazí jako kořenové složky a vy můžete vybrat složku/stránku ze všech dostupných domén. Toho se využívá např. u uživatelů, kde jsou práva nastavena napříč všemi doménami.

## Použití specifických objektů JSON

Pokud mapovaný objekt není přímo `GroupDetails` nebo `DocDetails` je nutné nastavit mapování vráceného objektu do požadovaného formátu JSON.

Výstupní objekt JSON musí vypadat například takto:

```javascript
{
    "id":0,
    "groupId":26,
    "domainId":1,
    "insertScriptGrId":0
}
```

Ukázka implementace v jazyce Java je v [InsertScriptBean](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) kde jsou použity následující poznámky:

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

**TIP**: `className` mohou obsahovat další `suffix` (například `dt-tree-group-array-insert-script`) pro další řešení ve vašem kódu. Pokud byste například měli více objektů JSON stejného typu. `GroupDetails` a potřeboval vygenerovat různé výsledné objekty JSON.

Na frontendu je možné definovat objekt v konstruktoru Datatable `jsonField` přičemž funkce `getItem` převede vrácený uzel z jstree (GroupDetails nebo DocDetails) do cílového formátu. Funkce `getKey` se používá při ověřování existence objektu v poli a vrací jedinečný identifikátor objektu.

Příklad je v souboru [insert-script.pug](../../../src/main/webapp/admin/v9/views/pages/apps/insert-script.pug), který umožňuje převod standardních `DocDetails` a `GroupDetails` objekty do formátu typu:

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

Pokud potřebujete implementovat vlastní zobrazení stromové struktury, můžete se inspirovat příkazem `DirTreeRestController` entita`DirTreeItem`. Používá základní objekt pro zobrazení `jsTree - JsTreeItem`, se kterými může komponenta VUE následně pracovat. Důležité je správně nastavit atributy ve složce `JsTreeItem` Entita.

Příklad služby REST:

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

entita`DirTreeItem`:

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

Příklad anotace se zadanou adresou URL služby REST:

```java
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="useredit.addGroup", tab = "rightsTab", className = "dt-tree-universal-array", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/elfinder/tree")
        })
    })
    private List<DirTreeItem> writableFolders;
```

Hodnota `className` doporučujeme ponechat na hodnotě `dt-tree-universal-array` správně zobrazit tlačítko přidat, odstranit a nahradit (porovnání ID funguje správně).

## Zobrazení hodnoty sloupce JSON v Datatable

Použitá fazole Java musí obsahovat metodu `getFullPath()` nebo `getVirtualPath()` jehož hodnota se použije při výpisu hodnoty objektu v datové tabulce a editoru:

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

!>**Oznámení**: zobrazit sloupce json pomocí `renderuje` pouze v prohlížeči, vyhledávání v hodnotě (zatím) nelze použít při vyhledávání na serveru.

Pokud metoda `getFullPath()` nemůžete implementovat, doporučujeme použít atribut `hidden=true` zakázat zobrazování sloupců json v tabulce. Můžete implementovat další atribut pro zobrazení hodnoty v datové tabulce, kterému nastavíte atribut `hiddenEditor=true` pro zakázání atributu v editoru. Tím získáte samostatný atribut pro editor a datovou tabulku.

## Naslouchání změně hodnoty

Pokud potřebujete naslouchat změně hodnoty pole mimo komponentu VUE, je možné naslouchat události změny ve vnořené komponentě. `textarea` který obsahuje aktuální objekt JSON:

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

[field-type-json.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-json.js) je definován nový datový typ `$.fn.dataTable.Editor.fieldTypes.json`. To se provádí pomocí komponenty VUE [webjet-dte-jstree](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue). Obsahuje také skryté pole typu `textarea` do kterého se zkopíruje aktuální objekt JSON. Toto pole však slouží pouze ke "kontrole" aktuálních dat. Ve funkci get jsou vždy vrácena aktuální data z komponenty VUE.

[datatables-config.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js) implementuje funkci `renderJson(td, type, rowData, row)` pro zobrazení dat v tabulce. Ten předává záznamy, z nichž používá atribut `fullPath`.

[webjet-dte-jstree.vue](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue) je kořenová složka, která podle údajů prochází přes `child` součást záznamového řádku. Pro objekty typu **pole** také zobrazí tlačítko pro přidání nového záznamu do pole.

Komponenta používá [EventBus](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/event-bus.js) ve kterém naslouchá události `change-jstree`. Tato událost nastane po kliknutí na adresář nebo webovou stránku ve stromu JS.

Funkce `processTreeItem(that, data)` zpracovává kliknutí na objekt (DocDetails nebo GroupDetails) ve stromové komponentě JS. Provede validaci a případnou konverzi JSON objektu.

[webjet-dte-jstree-item.js](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree-item.vue) je řádek seznamu existujících objektů. Na každém řádku je zobrazeno tlačítko pro úpravu a odstranění záznamu. Kliknutí zpracovává kořenová komponenta prostřednictvím volání `this.$parent.processTreeItem(this, data);`.

[vue-folder-tree.vue](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue) zapouzdřuje knihovnu JS Tree do komponenty VUE.

Pokud `Doc/GroupDetails` objekt `null` se nezobrazí žádné pole. Proto v `field-type-json.js` je funkcí `fixNullData` který pro tento případ uměle vytvoří základní objekt. Pokud se jedná o webovou stránku, obsahuje atribut `docId=-1`, pro adresář `groupId=-1` a pro ostatní objekty `id=-1`. Atribut `fullPath` je nastavena na prázdnou hodnotu.
