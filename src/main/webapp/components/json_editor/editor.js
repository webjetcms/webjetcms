/**
 * suchyJSONEditor
 */

var EditorItemFieldRenderer = (function () {
    /**
     * EditorItemFieldRenderer render form field
     * @id number index in list
     * @name string field/property name
     * @value string field/property value
     * @editorItemFields Object configuration of form fields, example: { propertyName : { title: "Title", type: "Type" } }
     */
    function EditorItemFieldRenderer(id, name, value, editorItemFields) {
        this.id = id;
        this.name = name;
        this.value = value == undefined ? "" : value;
        this.editorItemFields = editorItemFields;
    };

    EditorItemFieldRenderer.prototype.renderLabel = function() {
        if(this.itemFieldExists()) {
            return '<label>' + this.editorItemFields[this.name].title + '</label>';
        }
        return 'emptyLabelError';
    };

    EditorItemFieldRenderer.prototype.getWrapperId = function() {
        return this.name + '_' + this.id;
    }

    EditorItemFieldRenderer.prototype.escapeHtml = function(unsafe) {
        return (unsafe + "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    EditorItemFieldRenderer.prototype.renderInput = function() {
        if(this.itemFieldExists()) {
            switch(this.editorItemFields[this.name].type) {
                case 'textAreaWysiwyg'  : return '<textarea id="'+this.name+this.id+'" cols="40" rows="4" class="wysiwyg editorItemValue" name="' + this.name + '">' + this.value + '</textarea>';
                case 'textArea'         : return '<textarea id="'+this.name+this.id+'" cols="40" rows="4" class="editorItemValue" name="' + this.name + '">' + this.value + '</textarea>';

                case 'image'            : return '<div class="imageDiv"><img style="max-width:100px; max-height:100px;" src="' + (this.escapeHtml(this.value) ? "/thumb"+this.escapeHtml(this.value)+"?w=100&h=100&ip=5" : '/components/json_editor/admin_imgbg.png') + '"/></div>' +
                                                 '<input class="editorItemValue" type="hidden" name="' + this.name + '" value="' + this.escapeHtml(this.value) + '" id="' + this.getWrapperId() + 'image">';

                case 'conditionalText'  : return '<input class="editorItemCheckbox" type="checkbox" id="' + this.name + 'Checkbox' + this.id + '">' +
                                                 '<br/><br/>' +
                                                 '<input class="editorItemValue" type="text" name="' + this.name + '" value="' + this.escapeHtml(this.value) + '">';

                case 'colorpicker'      : return '<input class="editorItemValue colorpicker-rgba" type="text" name="' + this.name+ '" size="10" value="' + this.value + '">';
                case 'textStyle'        : return '<input class="editorItemValue" id="'+this.name+this.id+'" name="'+this.name+'" type="text" style="display:none" value="' + this.value + '"/> <a data-id="'+this.id+'" data-parentfield="'+this.editorItemFields[this.name].parentField+'" class="rozsireneLink" onclick="openCustomStyle('+this.name+this.id+')"></a>';

                case 'select'			: var select = '<select class="editorItemValue col-sm-12" id="'+this.name+this.id+'" name="' + this.name + '"';
                                          if(this.editorItemFields[this.name].onchange) select += ' onchange="'+this.editorItemFields[this.name].onchange+'(this)"';
                                          select += '>';
                						  if(this.editorItemFields[this.name].options) {
                                              for (var i = 0; i < this.editorItemFields[this.name].options.length; i++) {
                                                  var val = this.editorItemFields[this.name].options[i].value;
                                                  var title = this.editorItemFields[this.name].options[i].title;
                                                  select += '<option value="' + val + '" ' + (this.value == val ? 'selected="selected"' : ' ') + '>' + title + '</option>';
                                              }
                                          }

                						  select += '</select>';
                						  return select;
                case 'hidden'			: return '<input class="editorItemValue" id="'+this.name+this.id+'" type="hidden" name="' + this.name + '" value="' + this.escapeHtml(this.value) + '">' ;

                case 'checkbox'         : return '<input class="editorItemValue checkbox" id="'+this.name+this.id+'" type="checkbox" name="' + this.name + '" '+this.value+'>';

                default                 : return '<input class="editorItemValue" id="'+this.name+this.id+'" type="text" name="' + this.name + '" value="' + this.escapeHtml(this.value) + '">' ;
            }
        }
        return 'emptyInputError';


    };


    EditorItemFieldRenderer.prototype.render = function() {
        var cleaner = '';
        var comment = '';
        try {
            if (this.editorItemFields[this.name].addCleaner) {
                cleaner = '<div class="cleaner"></div>';
            }
            if (this.editorItemFields[this.name].comment) {
                comment = '<div class="comment col-sm-12">' + this.editorItemFields[this.name].comment + '</div>';
            }
            if ("checkbox" == this.editorItemFields[this.name].type) {
                if ("true" == this.value || true == this.value) this.value = "checked";
                else this.value = "";
            }

            if ("select" == this.editorItemFields[this.name].type && this.editorItemFields[this.name].onchange)
            {
                var elementId = this.name+this.id;
                setTimeout(function() {
                    //console.log("TIMEOUT "+elementId);
                    $("#"+elementId).trigger("change")
                }, 100);
            }

            //console.log("rendering EditorItemFieldRenderer");

            return '<div class="' + this.getClasses() + ' clearfix" id="' + this.getWrapperId() + '">' +
                (this.editorItemFields[this.name].type != 'hidden' ? '<div class="col-sm-4">' + this.renderLabel() + '</div>' : '') +
                '<div class="col-sm-8">' +
                this.renderInput() +
                '</div>' +
                comment +
                '</div>' +
                cleaner;
        }
        catch (e) {
            console.log("EditorError");
            console.log(e);
        }
        return '';
    };

    EditorItemFieldRenderer.prototype.getClasses = function() {
        return 'propertyWrapper ' + this.name + 'Wrapper ' + this.editorItemFields[this.name].classes;
    };

    EditorItemFieldRenderer.prototype.itemFieldExists = function() {
        return this.editorItemFields && this.editorItemFields[this.name];
    };

    return EditorItemFieldRenderer;
}());

var EditorRenderer = (function () {
    /**
     * EditorRenderer render sortable list of properties to edit
     * @selector string css selector of wrapper for sortable list, example: #listWrapper
     * @editorItemsList EditorItemsList list of items loaded from PageParams JSON
     * @editorItemFields Object configuration of form fields, example: { propertyName : { title: "Title", type: "Type" } }
     * @editorItemsToUse Array list of properties which can be edited, example: ["title", "description", "image"]
     * @ItemRenderer string EditorItemFieldRenderer you can use another class, but it has to have render() method
     */
    function EditorRenderer(selector, editorItemsList, editorItemFields, editorItemsToUse, itemRenderer) {
        this.selector = selector;
        this.editorItemsList = editorItemsList;
        this.editorItemFields = editorItemFields;
        this.editorItemsToUse = editorItemsToUse;
        this.itemRenderer = itemRenderer;
    }

    EditorRenderer.prototype.setProperty = function(propertyName, value) {
        if(value != undefined) {
            this[propertyName] = value;
        }
    };

    EditorRenderer.prototype.setAfterRenderCallback = function(myCallback) {
        this.afterRenderCallback = myCallback;
    };

    EditorRenderer.prototype.render = function(editorItemsList) {
        var self = this;
        this.setProperty('editorItemsList', editorItemsList);

        //console.log("rendering...");

        // remove old data from DOM
        $(this.selector).html("");

        var data = this.editorItemsList.getData();
        // render each item
    	for(var i in data) {
            //console.log("rendering... i=" + i );

            var itemsToAppend = '<form class="item clearfix" name="item_' + i + '" id="item_' + i + '">';

            // render each field of item
            for(var j in this.editorItemsToUse) {
                var propertyName = this.editorItemsToUse[j];
                var propertyValue = data[i][propertyName];

                var item =  new window[this.itemRenderer](i, propertyName, propertyValue, this.editorItemFields);
                itemsToAppend += item.render();

                //console.log("rendering... item=" + propertyName );

            }

            itemsToAppend += '<span class="glyphicon removeItem glyphicon-remove" aria-hidden="true"></span>';

            itemsToAppend += '<span class="moveItem"></span>';
            itemsToAppend += '</form>';

            // append item to DOM
    		$(this.selector).append(
    			itemsToAppend
    		);
    	}
        // bind sortable plugin
        $(this.selector).sortable();
        $(this.selector).disableSelection();

        // execute callback defined by setAfterRenderCallback
        if(this.afterRenderCallback != undefined) {

            this.afterRenderCallback.apply(null, [this]);
        }
    };

    return EditorRenderer;
}());

var EditorItemsList = (function () {
    function EditorItemsList(itemsList, editorItemsToUse) {
        this.data = [];
        this.editorItemsToUse = [];

        this.setData(itemsList);
        this.setEditorItemsToUse(editorItemsToUse);
    }

    EditorItemsList.decodeJSONData = function(inputData) {
        //%2B is +, allows it in placeholder field as eg. +421
    	var parsed = JSON.parse(decodeURI(atob(inputData)).replace(/\%2B/gi, "+"));
        //console.log(parsed);
        return parsed;
    }

    EditorItemsList.encodeJSONData = function(inputData) {
        //console.log("inputData=", inputData, "json=", JSON.stringify(inputData), "encodeURI=", encodeURI(JSON.stringify(inputData)));
        var encoded = encodeURI(JSON.stringify(inputData));
        //allow + in placeholder field, like +421 as placeholder, encodeURI doesnt process +
        encoded = encoded.replace(/\+/gi, "%2B");
        //console.log("encoded=", encoded);
    	return btoa(encoded)
    }

    EditorItemsList.prototype.setArrayToProperty = function (propertyName, list) {
        if(!this.isEmpty(list)) {
            this[propertyName] = list;
        } else {
            this[propertyName] = [];
        }
    };

    EditorItemsList.prototype.setData = function (itemsList) {
        this.setArrayToProperty('data', itemsList);
    };

    EditorItemsList.prototype.setEditorItemsToUse = function (itemsList) {
        this.setArrayToProperty('editorItemsToUse', itemsList);
    };

    EditorItemsList.prototype.setDataFromDom = function(jqObject) {
        var self = this;
        var newItems = [];
        $(jqObject).each(function() {
            var newItem = {};
            $(this).find('.editorItemValue').each(function()
            {
                var val = $(this).val();
                if ($(this).hasClass("checkbox")) val = this.checked;
                newItem[$(this).attr('name')] = val;
            });
            newItems.push(newItem);
        });
        this.setData(newItems);
    };

    EditorItemsList.prototype.addNewItem = function (editorItem) {
        var newItem = {};
        for(index in this.editorItemsToUse) {
            newItem[this.editorItemsToUse[index]] = "";
        }
        this.addItem(newItem);
    };

    EditorItemsList.prototype.addItem = function (editorItem) {
        this.data.push(editorItem);
    };

    EditorItemsList.prototype.removeItem = function(index) {
        delete this.data[index];
    };

    EditorItemsList.prototype.getItem = function(index) {
        return this.data[index];
    };

    EditorItemsList.prototype.getData = function() {
        return this.data;
    };

    EditorItemsList.prototype.isEmpty = function(list) {
        return list == undefined || list == null;
    };

    return EditorItemsList;
}());
