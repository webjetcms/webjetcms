# jsTree

The [jsTree](https://www.jstree.com/) library is a jQuery ```plugin``` that we use to display tree structures in WebJET.

## Basic initialization in collaboration with Spring REST

The WebJET implementation of jsTree is configured using a JSON object in ```app.js```.


Basic example:

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

## Configuration

The address of the called REST service is configured using the HTML attributes ```data-rest-url``` and ```data-rest-param-name``` (the name of the parameter sent to the REST service).

In the ```window.treeInitialJson``` object, it is possible to set the initialization JSON data for the initial display of the tree structure. The first display will therefore be faster, since it is not necessary to call the REST service. Data transfer on the backend is described in the section [thymeleaf](../frameworks/thymeleaf.md#inserting-custom-objects-into-the-model).

### Creating a new REST controller

The new controller should extend the [JsTreeRestController](../../../src/main/java/sk/iway/iwcm/admin/jstree/JsTreeRestController.java) class, implement abstract methods tree (menu items), ```move``` (BE implementation of moving an item) and ```checkAccessAllowed``` (checking user rights using a request).

### Custom object types

```String customType``` was added to the ```JsTreeItem``` object, to define a custom type in customer implementations. The usage is the same as in the case of ```Type```, where we can compare ```Enumy```, only in this case we compare ```Stringy```, using the ```equals``` method.

Use type
```java
if (original.getType() == JsTreeItemType.GROUP) {
    // vlastny kod pre dany type
}
```

Using ```customType```
```java
if ("custom-jstree-item".equals(original.getCustomType())) {
    // vlastny kod pre dany custom type
}
```

### Display adjustment

Before displaying the tree structure, the JS object ```window.jstreeCustomizeData``` is tested. If it exists, it is called. It can modify the received data before displaying it.

An example is the display of ID and sorting order in a list of web pages implemented in ```src/js/pages/web-pages-list/jstreesettings.js```:

```javascript
    jstreeCustomizeData(data) {
        //console.log("Customize data: ", data);

        if (data==null || typeof data == "undefined" || data.length < 1 ) return;

        const idShow = window.jstreeSettings.isIdShow();
        const priorityShow = window.jstreeSettings.isPriotityShow();

        data.forEach(function(item) {
            //console.log(item);
            if (typeof item.groupDetails != "undefined") {
                if (idShow) item.text = `<span class="id">#${item.groupDetails.groupId}</span> ${item.text}` ;
                if (priorityShow) item.text = `${item.text} <span class="sortPriority">(${item.groupDetails.sortPriority})</span>` ;
            } else if (typeof item.docDetails != "undefined") {
                if (idShow) item.text = `<span class="id">#${item.docDetails.docId}</span> ${item.text}` ;
                if (priorityShow) item.text = `${item.text} <span class="sortPriority">(${item.docDetails.sortPriority})</span>` ;
            }
        });
    }
```

![](../../redactor/webpages/jstree-settings.png)

### Changing the REST service URL

The REST service URL is obtained from the data attribute ```data-rest-url```, if you need to change it dynamically you can create a JavaScript function ```window.getJstreeUrl``` that will be used instead of the value in the data attribute:

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

## Icons and CSS classes

The icon is set using the [FontAwesome](https://fontawesome.com/icons?d=gallery) CSS class by calling [JsTreeItem.setIcon](../../../src/main/java/sk/iway/iwcm/admin/jstree/JsTreeItemState.java). The value is, for example, ```ti ti-folder-filled``` or ```ti ti-map-pin```.

The API provides methods for setting HTML attributes on both LI and A elements using the [li_attr and a_attr](https://www.jstree.com/docs/json/) attributes. To easily add a CSS class, the API provides the ```JsTreeItem.addLiClass``` and ```JsTreeItem.addAClass``` methods. This allows you to set CSS states ```is-not-public``` or ```fa-is-internal```.

JsTree does not allow displaying multiple icons in a row, which we need to display the status of directories and web pages, we add them as HTML code to the text using the ```JsTreeItem.addTextIcon``` method.

Examples are in the classes [GroupsJsTreeItem](../../../src/main/java/sk/iway/iwcm/doc/GroupsJsTreeItem.java) and [DocumentsJsTreeItem](../../../src/main/java/sk/iway/iwcm/doc/DocumentsJsTreeItem.java).

### How to use icons

The use of icons has the following rules:

-<i class="ti ti-folder-filled" role="presentation"></i> /<i class="ti ti-map-pin" role="presentation"></i> full page and folder icon = displayed in the menu
-<i class="ti ti-folder" role="presentation"></i> /<i class="ti ti-map-pin-off" role="presentation"></i> empty page and folder icon = not displayed in the menu
-<i class="ti ti-lock" role="presentation"></i> lock = only available to logged in visitors
- <span style="color: #E00028">red color</span> = unavailable to the public (internal directory) or page with disabled display (in DT it is possible to use the CSS class is-not-public per line)
- **bold** = main page of the directory (in DT you can use the CSS class ```is-default-page```)
-<i class="ti ti-external-link"></i> out arrow = page is redirected
-<i class="ti ti-eye-off"></i> crossed out eye = page cannot be searched