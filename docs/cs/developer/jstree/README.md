# jsTree

Knihovna [jsTree](https://www.jstree.com/) je jquery `plugin`, kterým zobrazujeme stromové struktury ve WebJETu.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [jsTree](#jstree)
  - [Základní inicializace v spolupráce se Spring REST](#základní-inicializace-v-spolupráce-se-spring-rest)
  - [Konfigurace](#konfigurace)
    - [Vytvoření nového REST controlleru](#vytvoření-nového-rest-controlleru)
    - [Vlastní typy objektů](#vlastní-typy-objektů)
    - [Úprava zobrazení](#úprava-zobrazení)
    - [Změna URL adresy REST služby](#změna-url-adresy-rest-služby)
  - [Ikony a CSS třídy](#ikony-a-css-třídy)
    - [Způsob použití ikon](#způsob-použití-ikon)

<!-- /code_chunk_output -->

## Základní inicializace v spolupráce se Spring REST

WebJET implementace jsTree se konfiguruje pomocí JSON objektu v `app.js`.

Základní příklad:

```javascript
script.
    let jstree = $('#SomStromcek').jstree({
            'core' : {
                'check_callback' : function (operation, node, parent, position, more) {
                    if (operation === 'copy_node') {
                        return false;
                    }

                    if(operation === 'move_node') {
                        if(parent.id === '#') {
                            return false; // prevent moving a child above or below the root
                        }
                    }

                    return true; // allow everything else
                },
                'data' : function(obj, callback) {
                    let jsTreeParamValue;
                    if (jsTreeParamName === 'url') {
                        jsTreeParamValue = '/images';
                        if (obj.id !== '#' && typeof obj.original !== 'undefined') {
                            jsTreeParamValue = obj.original.virtualPath;
                        }
                    }
                    else {
                        jsTreeParamValue = 0;
                        if (obj.id !== '#') {
                            jsTreeParamValue = obj.id;
                        }
                    }

                    let data = {};
                    data[jsTreeParamName] = jsTreeParamValue;

                    if (typeof window.treeInitialJson != undefined && window.treeInitialJson != null) {
                        let items = window.treeInitialJson;
                        window.treeInitialJson = null;
                        callback.call(this, items);
                    }
                    else {
                        $.ajax({
                            url: WJ.urlAddPath(getJstreeUrl(), "/tree"),
                            method: 'post',
                            contentType: 'application/json',
                            data: JSON.stringify(data),
                            success: function (data) {
                                if (!data.result) {
                                    WJ.notifyError(data.error);
                                    return;
                                }
                                //WJ.log("calling callback, items=", data.items);
                                callback.call(this, data.items);
                            }
                        });
                    }
                }
            },
            "plugins" : [
                "dnd"
            ],
            "types" : {
                "#" : {
                    "max_children" : 1,
                    "max_depth" : 4,
                    "valid_children" : ["root"]
                },
                "root" : {
                    "icon" : "/static/3.3.8/assets/images/tree_icon.png",
                    "valid_children" : ["default"]
                },
                "default" : {
                    "valid_children" : ["default","file"]
                },
                "file" : {
                    "icon" : "glyphicon glyphicon-file",
                    "valid_children" : []
                }
            }
        }).on("move_node.jstree", function(e, data) {
            //console.log("Drop node " + data.node.id + " to " + data.parent);
            //console.log("Data", data);
            delete(data.node.children);
            //console.log("Data", data);
            let json = {
                node: data.node,
                old_parent: data.old_parent,
                old_position: data.old_position,
                parent: data.parent,
                position: data.position
            };

            $.ajax({
                url : jsTreeUrl + "/move",
                method: 'post',
                dataType: 'json',
                contentType: 'application/json',
                data: JSON.stringify(json),
                success: function(response) {
                    //console.log("response", response);
                    if (!response.result) {
                        toastr.error(response.error);

                        return;
                    }

                    // LPA: bez refreshu parenta jstree hadzalo internu chybu
                    jstree.jstree(true).refresh(data.parent);
                }
            })
        });
```

```html
<div id="SomStromcek"
    data-rest-url="/admin/rest/components/gallery/tree"
    data-rest-param-name="url">
</div>
```

## Konfigurace

Adresa volané REST služby se konfiguruje pomocí HTML atributů `data-rest-url` a `data-rest-param-name` (název parametru poslaného do REST služby).

V objektu `window.treeInitialJson` lze nastavit inicializační JSON data pro prvotní zobrazení stromové struktury. První zobrazení bude tedy rychlejší, jelikož není třeba volat REST službu. Předání dat na backendu je popsáno v sekci [thymeleaf](../frameworks/thymeleaf.md#vložení-vlastních-objektů-do-modelu).

### Vytvoření nového REST controlleru

Nový controller by měl rozšiřovat třídu [JsTreeRestController](../../../src/main/java/sk/iway/iwcm/admin/jstree/JsTreeRestController.java), implementovat abstraktní metody tree (položky menu), `move` (BE implementace přesunu položky) a `checkAccessAllowed` (kontrola práv uživatele pomocí requestu).

### Vlastní typy objektů

Do objektu `JsTreeItem` byl přidán `String customType`, pro definování vlastního typu v zákaznických implementacích. Použití je stejné jako v případě `Type`, kde si umíme porovnat `Enumy`, jen v tomto případě porovnáváme `Stringy`, metodou `equals`.

Použití typu

```java
if (original.getType() == JsTreeItemType.GROUP) {
    // vlastny kod pre dany type
}
```

Použití `customType`

```java
if ("custom-jstree-item".equals(original.getCustomType())) {
    // vlastny kod pre dany custom type
}
```

### Úprava zobrazení

Před zobrazením stromové struktury se testuje JS objekt `window.jstreeCustomizeData`. Pokud existuje, je zavolán. Může upravit přijatá data před jejich zobrazením.

Příkladem je zobrazení ID a pořadí uspořádání v seznamu web stránek implementované v `src/js/pages/web-pages-list/jstreesettings.js`:

```javascript
    jstreeCustomizeData(data) {
        //console.log("Customize data: ", data);

        if (data==null || typeof data == "undefined" || data.length < 1 ) return;

        const idShow = window.jstreeSettings.isIdShow();
        const priorityShow = window.jstreeSettings.isPriotityShow();

        data.forEach(function(item) {
            //console.log(item);
            if (typeof item.groupDetails != "undefined") {
                if (idShow) item.text = `<span class="id">#${item.groupDetails.groupId}</span> ${item.text}`;
                if (priorityShow) item.text = `${item.text} <span class="sortPriority">(${item.groupDetails.sortPriority})</span>`;
            } else if (typeof item.docDetails != "undefined") {
                if (idShow) item.text = `<span class="id">#${item.docDetails.docId}</span> ${item.text}`;
                if (priorityShow) item.text = `${item.text} <span class="sortPriority">(${item.docDetails.sortPriority})</span>`;
            }
        });
    }
```

![](../../redactor/webpages/jstree-settings.png)

### Změna URL adresy REST služby

URL adresa REST služby se získává z data atributu `data-rest-url`, pokud jej potřebujete dynamicky měnit můžete vytvořit JavaScript funkci `window.getJstreeUrl`, která se použije místo hodnoty v data atributu:

```JavaScript
var somStromcek = null;

const queryString = window.location.search;
const urlParams = new URLSearchParams(queryString);
let dir = urlParams.get('dir')

window.getJstreeUrl = function() {
    if (somStromcek==null) somStromcek = $('#SomStromcek');
    let url = somStromcek.data("rest-url");
    if (dir != null) url = WJ.urlAddParam(url, "dir", dir);
    return url;
}
```

## Ikony a CSS třídy

Ikonu se nastavuje pomocí [FontAwesome](https://fontawesome.com/icons?d=gallery) CSS třídy voláním [JsTreeItem.setIcon](../../../src/main/java/sk/iway/iwcm/admin/jstree/JsTreeItemState.java). Hodnota je například `ti ti-folder-filled` nebo `ti ti-map-pin`.

API poskytuje metody pro nastavení HTML atributů na LI i A elementu pomocí atributů [li\_attr a a\_attr](https://www.jstree.com/docs/json/). Pro snadné přidání CSS třídy API poskytuje metody `JsTreeItem.addLiClass` a `JsTreeItem.addAClass`. Můžete tak nastavit CSS stavy `is-not-public` nebo `fa-is-internal`.

JsTree neumožňuje zobrazit více ikon v řádku, což potřebujeme pro zobrazení stavu adresářů a web stránek, ty přidáváme jako HTML kód k textu pomocí metody `JsTreeItem.addTextIcon`.

Příklady jsou ve třídách [GroupsJsTreeItem](../../../src/main/java/sk/iway/iwcm/doc/GroupsJsTreeItem.java) a [DocumentsJsTreeItem](../../../src/main/java/sk/iway/iwcm/doc/DocumentsJsTreeItem.java).

### Způsob použití ikon

Použití ikon má následující pravidla:
- <i class="ti ti-folder-filled" role="presentation" ></i> / <i class="ti ti-map-pin" role="presentation" ></i> plná ikonka stránky i složky = zobrazen v menu
- <i class="ti ti-folder" role="presentation" ></i> / <i class="ti ti-map-pin-off" role="presentation" ></i> prázdná ikonka stránky i složky = nezobrazený v menu
- <i class="ti ti-lock" role="presentation" ></i> zámek = dostupné pouze pro přihlášeného návštěvníka
- <span style="color: #FF4B58">červená barva</span> = nedostupné pro veřejnost (interní adresář) nebo stránka s vypnutým zobrazením (v DT lze použít CSS třídu is-not-public na řádek)
- **tučné písmo** = hlavní stránka adresáře (v DT lze použít CSS třídu `is-default-page`)
- <i class="ti ti-external-link" ></i> šipka ven = stránka je přesměrována
- <i class="ti ti-eye-off" ></i> přeškrtnuté oko = stránku nelze vyhledat
