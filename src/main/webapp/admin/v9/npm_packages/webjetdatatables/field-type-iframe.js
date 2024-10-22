import WJ from "../../src/js/webjet";

export function typeIframe() {

    const getUrlWithParams = function(EDITOR, url) {
        //nahrad parametre z json objektu
        if (url.indexOf("{")!==-1) {
            url = url.replace(/{(.*?)}/gi, function(a, b){
                //Chceme real aktuálnu hodnotu
                try {
                    if (EDITOR.fields().includes(b)) {
                        return EDITOR.field(b).val();
                    }
                } catch (e) {}
                return EDITOR.currentJson[b];
            });
        }
        return url;
    }

    /**
     * @param {Object} evt
     * @param {Object} conf
     * @returns {boolean}
     */
    const isCurrentTab = (evt, conf) => evt.detail.id.indexOf('-' + conf.tab) !== -1;

    return {
        create: function ( conf ) {
            //console.log("Creating IFRAME field, conf=", conf, "this=", this);
            const id = $.fn.dataTable.Editor.safeId( conf.id );
            conf._id = id;
            conf.val = "";

            //tato jquery konstrukcia vytvori len pole objektov, nie su to este normalne elementy
            const htmlCode = $(`<div class="iframeFieldType"><iframe id="${id}" src="about:blank" width="100%" height="523"></iframe></div>`);

            return htmlCode;
        },

        get: function ( conf ) {
            return conf.val;
        },

        set: function (conf, val) {
            const EDITOR = this;
            conf.val = val;

            // Pri novom datatable draw() resetne loaded a zruší eventy
            window.removeEventListener('WJ.DTE.tabclick', function (evt) {
                onTabClickInit(evt, conf);
            });

            function onTabClickInit(evt, conf) {
                if (!isCurrentTab(evt, conf)) {
                    return;
                }
                const url = getUrlWithParams(EDITOR, val);

                //console.log("Setting iframe url=", url, "element=", conf._id);
                $("#"+conf._id).attr("src", url);

                let container = $("#"+conf.id);
                container.closest('div.tab-pane').addClass("tab-pane-nopadding");
            }

            window.addEventListener('WJ.DTE.tabclick', function (evt) {
                onTabClickInit(evt, conf);
            });
        },

        enable: function ( conf ) {
            conf._input.prop( 'disabled', false );
        },

        disable: function ( conf ) {
            conf._input.prop( 'disabled', true );
        },

        canReturnSubmit: function ( conf, node ) {
            return false;
        }
    }
}