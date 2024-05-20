export function typeAttrs() {

    const TYPE_STRING = 0;
    const TYPE_INT = 1;
    const TYPE_BOOL = 2;
    const TYPE_DOUBLE = 3;

    /**
     * Generate all input elements into editor tab if it's not allready generated
     * @param {*} conf
     * @param {*} container
     * @returns
     */
    function generateInputElements(conf, container) {
        //iterate over attributes and append them to the panel-body div
        //console.log("container=", container, "conf=", conf);

        //check if elements are already generated
        if (container.find(".row-atr-def").length>0) return;

        $.each(conf.jsonData, function( key, value ) {
            //console.log("Setting attr: key=", key, " value=", value);
            var htmlCode = $(`
            <div class="DTE_Field form-group row row-atr-def" data-atr-group="${value.group}">
                <label data-dte-e="label" class="col-sm-4 col-form-label" for="DTE_Field_editorFields-attrs">${getTextByLanguage(value.name)}</label>
                <div class="col-sm-7">
                    ${getHtmlForField(value)}
                </div>
                <div class="col-sm-1">
                    <button type="button" tabindex="-1" class="btn btn-link btn-tooltip" data-toggle="tooltip" data-html="true" title="${value.description}"><i class="ti ti-info-circle"></i></button>
                </div>
            </div>`);
            container.append(htmlCode);
        });

        container.find("button.btn-tooltip").tooltip({
            placement: 'top',
            trigger: 'hover',
            html: true
        });
        container.find("input,select").addClass("form-control");
        container.find("select").addClass("form-select");
        container.find("input[type=radio]").removeClass("form-control");

        //nastav autocomplete
        //console.log("Setting autocomplete");
        $(container.find('input.form-control[data-ac-url]')).each(function () {
            //console.log("autocomplete", this);
            var autocompleter = new AutoCompleter('#' + $(this).attr("id")).autobind();
            $(this).closest("div.DTE_Field").addClass("dt-autocomplete");

            $(this).parents(".modal-body").on("scroll", function() {
                if (autocompleter.getInstance().menu.element.is(":visible")) {
                    //console.log("scroll, autocompleter=", autocompleter.getInstance());
                    let ins = autocompleter.getInstance();
                    var ul = ins.menu.element;
                    ul.position( $.extend( {
                        of: ins.element
                    }, ins.options.position ) );
                }
            });
        });
    }

    /**
     * Return html code for input element according to AtrDef
     * @param {*} def
     * @returns
     */
    function getHtmlForField(def) {
        let entity = getEntity(def);

        let delimeter = ",";
        let splitValues = null;
        if(def.defaultValue != null)
        {
            if (def.defaultValue.indexOf("|") != -1) delimeter = "|";	//ak najdem '|', parsujem podla '|'
            splitValues = def.defaultValue.split(delimeter);
        }

        let defaultValue = getTextByLanguage(def.defaultValue);
        if (defaultValue == null) defaultValue = "";

        if (splitValues != null && splitValues.length > 1) {
            //select box
            let html = `<select class="form-control" id="atr_${def.id}">`;
            for (let value of splitValues) {
                html += `<option value="${getTextByLanguage(value)}">${getTextByLanguage(value)}</option>`;
            }
            html += `</select>`;
            return html;
        } else if ("autoSelect"===defaultValue) {
            return `<input type="text" class="form-control" data-ac-url="/admin/rest/webpages/attributes/doc/autoselect/${def.id}/" data-ac-max-rows="100" data-ac-select="true" id="atr_${def.id}" value="" />`;
        } else if (defaultValue.indexOf("multiline")==0) {
            let cols=40;
      		let rows=4;
      		let st = defaultValue.split("-");
      		if (st.length == 3)
      		{
      			//format: multiline-cols-rows - multiline-40-4
      			cols = st[1];
      			rows = st[2];
      		}

            return `<textarea rows="${rows}" cols="${cols}" class="form-control" id="atr_${def.id}">${entity.valueString}</textarea>`;
        } else if (def.type==TYPE_BOOL) {
            return `<input type="radio" class="form-check-input" name="atr_${def.id}" id="atr_${def.id}_false" value="${def.falseValue}"/> <label class="form-check-label" for="atr_${def.id}_false">${def.falseValue}</label>
                    <input type="radio" class="form-check-input" name="atr_${def.id}" id="atr_${def.id}_true" value="${def.trueValue}"/> <label class="form-check-label" for="atr_${def.id}_true">${def.trueValue}</label>`
        } else if (def.type==TYPE_INT) {
            return `<input type="number" class="form-control" id="atr_${def.id}" value="" />`;
        } else if (def.type==TYPE_DOUBLE) {
            return `<input type="number" step="any" class="form-control" id="atr_${def.id}" value="" />`;
        }

        return `<input type="text" class="form-control" id="atr_${def.id}" value="" />`;
    }

    /**
     * Set HTML input values from JSON object conf.jsonData
     * @param {*} conf
     */
    function setInputValues(conf) {
        let firstNonEmptyGroup = null;

        //iterate over attributes and append them to the panel-body div
        //console.log("container=", container, "id=", conf._id);
        $.each(conf.jsonData, function( key, def ) {
            let entity = getEntity(def);
            let input = $("#atr_"+def.id);

            if (def.type==TYPE_BOOL) {
                if (entity.valueBoolean===true) {
                    $("#atr_"+def.id+"_true").prop("checked", true);
                } else {
                    $("#atr_"+def.id+"_false").prop("checked", true);
                }
            } else if (def.type==TYPE_INT) {
                if (entity.valueNumber=="") input.val("");
                else input.val(""+parseInt(entity.valueNumber));
            } else if (def.type==TYPE_DOUBLE) {
                input.val(""+entity.valueNumber);
            } else {
                let text = getTextByLanguage(entity.valueString);
                input.val(text);
                if (firstNonEmptyGroup==null && text != null && text.length>0) firstNonEmptyGroup = def.group;
            }
        });

        if (firstNonEmptyGroup != null) {
            //switch group to the first non-empty group
            let select = $("#DTE_Field_editorFields-attrGroup");
            select.val(firstNonEmptyGroup);
            select.selectpicker("refresh");
            select.trigger("change");
        }
    }

    /**
     * Update back JSON object for save from HTML input elements
     * @param {*} conf
     */
    function updateJsonFromInputs(conf) {

        $.each(conf.jsonData, function( key, def ) {
            let entity = getEntity(def);
            let input = $("#atr_"+def.id);

            if (def.type==TYPE_BOOL) {
                if ($("#atr_"+def.id+"_true").is(":checked")) {
                    entity.valueBoolean=true;
                } else {
                    entity.valueBoolean=false;
                }
            } else if (def.type==TYPE_INT) {
                entity.valueNumber = parseInt(input.val());
            } else if (def.type==TYPE_DOUBLE) {
                entity.valueNumber = input.val();
            } else {
                entity.valueString = input.val();
            }
        });

    }

    /**
     * Get entity from AtrDef, it it's null set and return empty/new entity
     * @param {*} def
     * @returns
     */
    function getEntity(def) {
        let entity = def?.docAtrEntityFirst;
        if (typeof entity == "undefined" || entity == null) {
            entity = {
                valueString: "",
                valueNumber: "",
                valueBool: false
            };
            def.docAtrEntityFirst = entity;
        }
        return entity;
    }

    /**
     * There is support for multilanguage titles in AtrDef, this function returns title in current language.
     * Format is: SK:title|CZ:Czech title|EN:English title
     * @param {*} text
     * @returns
     */
    function getTextByLanguage(text) {
        if (typeof text == "undefined" || text == null) return text;
        if (text.indexOf("|") == -1 || text.indexOf(":") == -1) return text;

        let lng = window.userLng.toLowerCase();
        if ("cs"==lng) lng = "cz";

        let titles = text.split("|");
        if (titles.length==1) return text;
        for (let title of titles) {
            let lngTitle = title.split(":");
            if (lngTitle.length==2 && lngTitle[0].toLowerCase()==lng) return lngTitle[1];
        }
        return text;
    }

    /**
     * Show/hide HTML input elements by Atr group
     * @param {*} group
     */
    function showHideAttrByGroup(group) {
        $("div.row-atr-def").each(function() {
            let $this = $(this);
            let myGroup = $this.attr("data-atr-group");
            if (myGroup != group) $this.hide();
            else $this.show();
        });
    }

    /**
     * Event on opened editor window, mainly to check for multiedit mode for which we hide Attributes tab
     * Also the tab is hidden when there are no attributes defined
     * @param {*} EDITOR
     * @param {*} conf
     * @returns
     */
    function editorOnOpened(EDITOR, conf) {
        var tab = $("#pills-dt-datatableInit-attributes-tab").parents("li.nav-item");
        //console.log("editorOnOpen, tab=", tab, "input=", conf._input, "display=", conf._input.parents("div.DTE_Field").find("div.multi-value").css("display"), "json=", conf.jsonData);

        //there are no attributes defined
        if (conf.jsonData==null || conf.jsonData.length==0) {
            tab.hide();
            return;
        }

        //disable tab on multi-edit (div.multi-value is not hidden)
        if (conf._input.parents("div.DTE_Field").find("div.multi-value").css("display")=="none") {
            tab.show();
        } else {
            tab.hide();
        }
    }

    return {
        create: function ( conf ) {
            //console.log("Creating JSON field, conf=", conf);
            var id = $.fn.dataTable.Editor.safeId( conf.id );
            //tato jquery konstrukcia vytvori len pole objektov, nie su to este normalne elementy
            var htmlCode = $('<textarea id="'+id+'"></textarea>');
            conf._id = id;
            //htmlCode je pole elementov, input pole je prvy objekt v zapise (textarea)
            conf._input = htmlCode;
            if (typeof conf.attr != "undefined" && conf.attr != null) {
                $.each(conf.attr, function( key, value ) {
                    //console.log("Setting attr: key=", key, " value=", value);
                    $(conf._input).attr(key, value);
                });
            }
            conf.jsonData = [];

            let EDITOR = this;
            window.addEventListener("WJ.DTE.opened", function(e) {
                editorOnOpened(EDITOR, conf);
            });

            return htmlCode;
        },

        get: function ( conf ) {
            updateJsonFromInputs(conf);
            //console.log("Returning json ("+conf.className+"): ", conf.jsonData);
            return conf.jsonData;
        },

        set: function ( conf, val ) {
            //console.log("set, val=", val, " EDITOR=", this, "typeof=", typeof val);
            if (typeof val != "object" || ""===val) return;

            var jsonString = JSON.stringify(val, undefined, 4);
            conf._input.val(jsonString);

            conf.jsonData = JSON.parse(conf._input.val());
            if ("null" == conf._input.val()) conf.jsonData = [];

            //console.log("jsonData=", conf.jsonData);

            var container = conf._input.parents(".panel-body");
            //console.log("container", container, "row-atr-def: ", container.find(".row-atr-def"));
            if (container.find(".row-atr-def").length==0) {
                setTimeout(function() {
                    if (container.find(".row-atr-def").length>0) return;

                    //hide textarea
                    conf._input.parents(".DTE_Field").hide();

                    //update container with current HTML
                    container = conf._input.parents(".panel-body");

                    generateInputElements(conf, container);
                    //set input values
                    setInputValues(conf);

                    $("#DTE_Field_editorFields-attrGroup").on("change", function() { showHideAttrByGroup($("#DTE_Field_editorFields-attrGroup").val()); });
                    showHideAttrByGroup($("#DTE_Field_editorFields-attrGroup").val());
                }, 100);
            } else {
                //set input values
                setInputValues(conf);
            }
        },

        enable: function ( conf ) {
            //console.log("enable, conf=", conf);
            conf._input.prop( 'disabled', false );
        },

        disable: function ( conf ) {
            //console.log("disable, conf=", conf);
            conf._input.prop( 'disabled', true );
        },

        canReturnSubmit: function ( conf, node ) {
            return false;
        }
    }
}