
export default class AutoCompleter {

    constructor(target)
    {
        this.target = target;
        this.$target = $(target);
        this.name = this.$target.attr("name");
        this.url = '';
        this.params = null;
        this.onOptionSelect = null;
        this.hasQueryString = false;
        this.maxLengthOfAutocomplete = 30;
        this.minLengthBeforeSearch = 1;
        this.collision = "flipfit";
        this.select = false;
        this.instance = null;
        this.renderItemFn = null;
    }

    setName(name) {
        //console.log("setName", name);
        this.name = name;
        return this;
    }

    setUrl(url){
        //console.log("setUrl", url);
        this.url = url;
        this.hasQueryString = url.indexOf('?') !== -1;
        return this;
    };

    setParams(paramString){
        this.params = paramString.replace(/\s+/, '').split(',');
        return this;
    };

    setOnOptionSelect(functionName){
        //console.log("setOnOptionSelect", functionName);
        this.onOptionSelect = functionName;
        return this;
    };

    setMaxRows(max) {
        this.maxLengthOfAutocomplete = max;
        return this;
    };

    setMinLength(minLength) {
        this.minLengthBeforeSearch = minLength;
        return this;
    };

    setRenderItemFn(fnName) {
        this.renderItemFn = fnName;
        return this;
    }

    getInstance() {
        return this.instance;
    };

    // @todo    Túto metódu by sa zišlo v budúcnosti premenovať.
    // @todo    Vzniká totiž konflikt: Incompatible override, should have signature '(ctx: CanvasRenderingContext2D): void'
    transform() {
        this.addAutoCompleteTo(this);
    };

    //bindne autocompleter podla data parametrov
    autobind() {
        //povinne
        this._setParameterFromData("url", value => { this.setUrl(value) });
        //volitelne
        this._setParameterFromData("click", value => { this.setOnOptionSelect(value) });
        this._setParameterFromData("name", value => { this.setName(value) });
        this._setParameterFromData("min-length", value => { this.setMinLength(value) });
        this._setParameterFromData("max-rows", value => { this.setMaxRows(value) });
        this._setParameterFromData("params", value => { this.setParams(value) });

        this._setParameterFromData("select", value => { this.select = value });
        if (true===this.select) {
            this.collision = "none";
        }

        this._setParameterFromData("collision", value => { this.collision = value });

        //ak nie je nastavene name pouzi defaultnu hodnotu term
        if (!this.name) {
            this.setName("term")
        }

        this._setParameterFromData("render-item-fn", value => { this.setRenderItemFn(value) });

        this.transform();

        return this;
    };

    //overi data param a nastavi cez callback
    _setParameterFromData(name, callback) {
        const dataParam = this.$target.data("ac-"+name);
        //console.log("mam datataram ("+name+"): ", dataParam);
        if (!!dataParam) {
            callback(dataParam);
        }
    }

    addAutoCompleteTo(autoCompleter)
    {
        try
        {
            //console.log("autoCompleter.name=", autoCompleter.name);
            autoCompleter.$target.autocomplete({
                source : function(actual, options){
                    let url = autoCompleter.url;
                    var term = actual.term;
                    if ("*"===term) term = "%";
                    else if (" "===term) term = "%";
                    url += autoCompleter.hasQueryString ? '&' : '?';
                    url += autoCompleter.name + "=" + encodeURIComponent(term);

                    for (const param in autoCompleter.params) {
                        if (!autoCompleter.params.hasOwnProperty(param)) {
                            continue;
                        }
                        const paramEl = $(autoCompleter.params[param]);
                        var name = paramEl.attr("name");
                        if (typeof name == "undefined") name = autoCompleter.params[param].replaceAll("#", "");

                        /**
                         * Before manual set of tree, it return object. We need to parse value out of it.
                         */
                        let val = paramEl.val();
                        try {
                            if (val && val.includes("{")) {
                                const parsedValue = JSON.parse(paramEl.val());
                                if (parsedValue && typeof parsedValue === "object" && "id" in parsedValue) {
                                    const valNew = parsedValue["id"];
                                    if (valNew != null) {
                                        val = encodeURIComponent(valNew.toString());
                                    }
                                }
                            }
                        } catch (error) {
                            console.error("Error parsing JSON value:", error);
                        }

                        url += "&" + name + "=" + val;

                    }
                    $.ajax({
                        url: url,
                        cache: false,
                        success: function(response){
                            const allOptions = eval(response);
                            if (allOptions) {
                                while (allOptions.length > autoCompleter.maxLengthOfAutocomplete) {
                                    allOptions.pop();
                                }
                                options(allOptions);
                            }
                        },
                        async: false
                    });
                },
                appenTo: "#"+autoCompleter.target,
                select: () => {
                    if (autoCompleter.onOptionSelect != null) {
                        //console.log("select func, callback=", autoCompleter.onOptionSelect, "target=", autoCompleter.$target);

                        if ("fireEnter" == autoCompleter.onOptionSelect) {
                            //console.log("Firing keyup event");
                            setTimeout(() => {
                                var e = $.Event( "keyup", { keyCode: 13 } );
                                autoCompleter.$target.trigger(e);
                            }, 100);
                        }
                        else {
                            const callback = eval(autoCompleter.onOptionSelect);
                            setTimeout(() => {
                                //console.log("Evaluating:", autoCompleter.onOptionSelect);
                                callback();
                            }, 100);
                        }
                    }
                },
                delay: 800,
                minLength: autoCompleter.minLengthBeforeSearch,
                position: { my: "left top+2", at: "left bottom", collision: autoCompleter.collision }
            })

            //If renderItemFn is set, use this method via window to render items (LI elements)
            if(autoCompleter.renderItemFn != null) {
                autoCompleter.$target.autocomplete("instance")._renderItem = function( ul, item) {
                    return window[autoCompleter.renderItemFn](ul, item);
                };
            }

            //If LI element contain class 'disabled', it will not be selectable !!
            autoCompleter.$target.autocomplete("instance").widget().menu( "option", "items", "> :not(.disabled)" );

            autoCompleter.instance = autoCompleter.$target.autocomplete("instance");
            
            if (autoCompleter.select===true) {
                //console.log("setting focus", autoCompleter.instance);
                autoCompleter.$target.on("focus", function () {
                    //console.log("focus, target=", autoCompleter.$target);
                    setTimeout(() => {
                        autoCompleter.instance.search("*");
                    }, 50);
                });

                autoCompleter.instance.menu.element.addClass("dt-autocomplete-select");
            }

        } catch (e) { console.log("error in addAutoCompleteTo:"); console.log(e); }
    }

}