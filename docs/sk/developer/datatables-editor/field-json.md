# Field Type - JSON

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Field Type - JSON](#field-type---json)
  - [Možnosti className](#možnosti-classname)
  - [Použitie špecifických JSON objektov](#použitie-špecifických-json-objektov)
  - [Vlastná konfigurácia zobrazenej stromovej štruktúry](#vlastná-konfigurácia-zobrazenej-stromovej-štruktúry)
  - [Zobrazenie hodnoty JSON stĺpca v Datatable](#zobrazenie-hodnoty-json-stĺpca-v-datatable)
  - [Počúvanie na zmenu hodnoty](#počúvanie-na-zmenu-hodnoty)
  - [Implementačné detaily](#implementačné-detaily)

<!-- /code_chunk_output -->


Web stránky alebo aplikácia Skripty používa mapovanie 1:N na ďalšie objekty. V prípade web stránky je to vybraná adresár a kópia web stránky v adresároch a v prípade aplikácie Skripty je to mapovanie skriptu na adresáre a web stránky.

Pole typu JSON pre DT Editor implementuje **UI pre zobrazenie výberu adresára alebo web stránky** z JS tree komponenty s možnosťou nastavenia JSON objektu pre **jedno pole alebo zoznam (List) polí**.

V JSON dátach zo servera je toto mapovanie vrátené ako:

- [private GroupDetails groupDetails](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) pre adresár v ktorom je web stránka
- [private List&lt;GroupDetails&gt; groupCopyDetails](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) pre kópiu stránky v adresároch
- [List&lt;InsertScriptGroupBean&gt; groupIds](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) pre aplikáciu Skripty mapovanie na adresáre
- [List&lt;InsertScriptDocBean&gt; docIds](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) pre aplikáciu Skripty mapovanie na web stránky
- [List&lt;DirTreeItem&gt; writableFolders](../../../src/main/java/sk/iway/iwcm/components/users/UserDetailsEditorFields.java) pre výber zoznamu adresárov v súborovom systéme

Uvedené atribúty používajú anotáciu ```@DataTableColumn(inputType = DataTableColumnType.JSON, className = "dt-tree-group"```, čiže typ poľa JSON. Pomocou atribútu className sa určuje správanie vráteného objektu.

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

všimnite si použitie atribútu ```data-dt-json-addbutton``` pre nastavenie textu tlačidla v zozname adresárov. Pripravené sú kľúče ```editor.json.addPage``` pre pridanie web stránky a ```editor.json.addGroup``` pre pridanie adresára.

> **Upozornenie:** ak používate klientské stránkovanie (atribút ```serverSide: false```) tak je potrebné prázdne ```List<>``` objekty vrátiť ako ```null``` namiesto prázdneho poľa (inak sa v datatabuľke neaktualizuje hodnota po zmazaní všetkých adresárov/stránok zo zoznamu).

## Možnosti className

```dt-tree-group``` - vrátený JSON objekt typu ```GroupDetails``` a nahradí aktuálnu hodnotu. Ak má byť možnosť vybrať aj koreňový priečinok zadajte ```dt-tree-group-root```.
![](field-json-group.png)

```dt-tree-group-null``` - vrátený JSON objekt typu ```GroupDetails``` alebo ```NULL``` - umožňuje nastaviť žiadnu/prázdnu hodnotu poľu (napr. pre voliteľné pole), v GUI sa zobrazí aj ikona koša pre zmazanie hodnoty.

```dt-tree-group-array``` - vrátený JSON objekt typu ```GroupDetails``` a vytvára pole hodnôt s možnosťou pridať objekt, nahradiť niektorý existujúci a zmazať objekt.
![](field-json-group-array.png)

```dt-tree-groupid/dt-tree-groupid-root``` - nastavuje do poľa len ID priečinku, do ```data-text``` nastaví cestu k priečinku (```fullPath```). Používa sa mimo editora v aplikácii štatistika pre výber priečinka.

```dt-tree-page``` - vrátený JSON objekt typu ```DocDetails``` a nahradí aktuálnu hodnotu.
![](field-json-page.png)

```dt-tree-page-null``` - vrátený JSON objekt typu ```DocDetails``` alebo ```NULL``` - umožňuje nastaviť žiadnu/prázdnu hodnotu poľu (napr. pre voliteľné pole), v GUI sa zobrazí aj ikona koša pre zmazanie hodnoty.
![](field-json-page-null.png)

```dt-tree-page-array``` - vrátený JSON objekt typu ```DocDetails``` a vytvára pole hodnôt s možnosťou pridať objekt, nahradiť niektorý existujúci a zmazať objekt.
![](field-json-page-array.png)

```dt-tree-dir``` - vrátený JSON objekt typu ```DirTreeItem``` pre **výber adresára v súborovom systéme**

```dt-tree-dir-simple``` - vrátený **reťazec** s hodnotou pre **výber adresára v súborovom systéme**, možné zadať koreňový priečinok ako ```@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/images/gallery")```

![](../../frontend/webpages/customfields/webpages-dir.png)

```dt-tree-dir-array``` - vrátený JSON objekt typu ```DirTreeItem``` pre **výber zoznamu adresárov v súborovom systéme**

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

Ukážková Java implementácia je v [InsertScriptBean](../../../src/main/java/sk/iway/iwcm/components/insertScript/InsertScriptBean.java) kde sú použité nasledovné anotácie:

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

dôležité je označenie ```inputType=DataTableColumnType.JSON``` a nastavenie správneho ```className```.

**TIP**: ```className``` môže obsahovať doplnkový ```suffix``` (napríklad ```dt-tree-group-array-insert-script```) na ďalšie rozlíšenie vo vašom kóde. Napríklad ak by ste mali viaceré JSON objekty rovnakého typu ```GroupDetails``` a potrebovali generovať rozdielne výsledné JSON objekty.


Na frontende je v konštruktore Datatable možné definovať objekt ```jsonField``` v ktorom funkcia ```getItem``` konvertuje vrátený uzol z jstree (GroupDetails alebo DocDetails) na cieľový formát. Funkcia ```getKey``` sa používa pri overovaní existencie objektu v poli, vracia jednoznačný identifikátor objektu.

Príklad je v súbore [insert-script.pug](../../../src/main/webapp/admin/v9/views/pages/apps/insert-script.pug), ktorý zabezpečuje konverziu štandardných ```DocDetails``` a ```GroupDetails``` objektov na formát typu:

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

## Vlastná konfigurácia zobrazenej stromovej štruktúry

Ak potrebujete implementovať vlastné zobrazenie stromovej štruktúry môžete sa inšpirovať v triede ```DirTreeRestController``` a entite ```DirTreeItem```. Používa základný objekt pre zobrazenie ```jsTree - JsTreeItem```, s ktorým vie následne pracovať VUE komponenta. Dôležité je korektné nastavenie atribútov v ```JsTreeItem``` entite.

Príklad REST služby:

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

a entity ```DirTreeItem```:

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

Príklad anotácie so zadanou URL adresou REST služby:

```java
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="useredit.addGroup", tab = "rightsTab", className = "dt-tree-universal-array", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/elfinder/tree")
        })
    })
    private List<DirTreeItem> writableFolders;
```

Hodnotu ```className``` odporúčame ponechať na hodnote ```dt-tree-universal-array``` aby sa korektne zobrazilo tlačidlo na pridanie záznamu, zmazanie a náhradu (korektne fungovalo porovnanie podľa ID).

## Zobrazenie hodnoty JSON stĺpca v Datatable

Použitý Java bean musí obsahovať metódu ```getFullPath()``` alebo ```getVirtualPath()```, ktorej hodnota sa použije pri výpise hodnoty daného objektu v datatabuľke a editore:

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

!>**Upozornenie**: zobrazenie json stĺpcov sa ```renderuje``` až v prehliadači, vyhľadávanie v hodnote (zatiaľ) nie je možné použiť pri serverovom vyhľadávaní.

Ak metódu ```getFullPath()``` nemôžete implementovať odporúčame použiť atribút ```hidden=true``` pre vypnutie zobrazenia json stĺpca v tabuľke. Môžete implementovať doplnkový atribút pre zobrazenie hodnoty v datatabuľke, ktorému nastavíte atribút ```hiddenEditor=true``` pre vypnutie atribútu v editore. Takto získate samostatný atribút pre editor a datatabuľku.

## Počúvanie na zmenu hodnoty

Ak potrebujete počúvať na zmenu hodnoty poľa mimo VUE komponenty je možné počúvať udalosť zmeny na vnorenom ```textarea``` elemente, ktoré obsahuje aktuálny JSON objekt:

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

## Implementačné detaily

[field-type-json.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-json.js) je definovaný nový dátový typ ```$.fn.dataTable.Editor.fieldTypes.json```. Ten je implementovaný pomocou VUE komponenty [webjet-dte-jstree](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue). Obsahuje aj skryté pole typu ```textarea```, do ktorého sa kopíruje aktuálny JSON objekt. Toto pole ale slúži len na "inšpekciu" aktuálnych dát. Vo funkcii get sa vždy vrátia aktuálne dáta z VUE komponenty.

[datatables-config.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js) implementuje funkciu ```renderJson(td, type, rowData, row)``` pre zobrazenie dát v tabuľke. Tá prechádza záznamy z ktorých použije atribút ```fullPath```.

[webjet-dte-jstree.vue](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree.vue) je koreňová komponenta, ktorá podľa dát prechádza ```child``` komponentu riadku záznamu. Pre objekty typu **array** zobrazí aj tlačidlo pre pridanie nového záznamu do poľa.

Komponenta používa [EventBus](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/event-bus.js) v ktorom počúva na udalosť ```change-jstree```. Táto udalosť nastane po kliknutí na adresár alebo web stránku v JS Tree.

Funkcia ```processTreeItem(that, data)``` spracuje kliknutie na objekt (DocDetails alebo GroupDetails) v JS tree komponente. Vykoná validáciu a prípadnú konverziu JSON objektu.

[webjet-dte-jstree-item.js](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/webjet-dte-jstree-item.vue) je riadok zoznamu existujúcich objektov. V každom riadku zobrazuje tlačidlo pre editáciu a zmazanie záznamu. Kliknutie je spracované koreňovou komponentou cez volanie ```this.$parent.processTreeItem(this, data);```.

[vue-folder-tree.vue](../../../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue) zapúzdruje knižnicu JS Tree do VUE komponenty.

Ak je ```Doc/GroupDetails``` objekt ```null``` nezobrazilo by sa žiadne pole. Preto v ```field-type-json.js``` je funkcia ```fixNullData```, ktorá pre tento prípad umelo vytvorí základný objekt. Ak sa jedná o web stránku obsahuje atribút ```docId=-1```, pre adresár ```groupId=-1``` a pre ostatné objekty ```id=-1```. Atribút ```fullPath``` je nastavený na prázdnu hodnotu.
