//import Quill from 'quill';
//import Toolbar from 'quill/modules/toolbar';
//import Snow from 'quill/themes/snow';

//https://www.npmjs.com/package/quill-html-edit-button
//import htmlEditButton from "quill-html-edit-button";

import { htmlEditButton } from "./quill.htmlEditButton";
import { SoftLineBreakBlot, shiftEnterHandler, lineBreakMatcher } from "./quill.br";

export function typeQuill() {

    var toolbarOptions = [
        [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
        [
            'bold', 'italic', 'underline', 'strike',
            { 'align': [] },
            { 'script': 'sub'}, { 'script': 'super' }, // superscript/subscript
            { 'color': [] }, { 'background': [] },          // dropdown with defaults from theme
            'clean'                                         // remove formatting button
        ],

        [
            { 'list': 'ordered'}, { 'list': 'bullet' },
            { 'indent': '-1'}, { 'indent': '+1' }          // outdent/indent
        ],

        [
            'link',
        ]

    ];

    /**
     * Update HTML to quill supported HTML tags, e.g.:
     * ol-li.bullet -> ul.li
     * @param {String} html
     */
    window.quillFromHtmlFormat = function(htmlCode) {
        //console.log("QUILL SET, htmlCode=", htmlCode);
        if (htmlCode == null || htmlCode=="") htmlCode = "<p><br/></p>";
        if (htmlCode.indexOf("<p")==-1 && htmlCode.indexOf("<h")==-1 && htmlCode.indexOf("<div")==-1) htmlCode = "<p>"+htmlCode+"</p>";

        //aktualna verzia pracuje s P elementami namiesto DIV elementov, musime upravit povodny zapis
        htmlCode = htmlCode.replace(/<div>/gi, "<p>");
        htmlCode = htmlCode.replace(/<\/div>/gi, "</p>");

        //nahrad inline styl za tagy em a strong
        htmlCode = htmlCode.replace(/<span[^<>]+style="font-style:italic">([^<>]+)<\/span>/gi, '<em>$&</em>');
        htmlCode = htmlCode.replace(/<span[^<>]+style="font-weight:bold">([^<>]+)<\/span>/gi, '<strong>$&</strong>');

        //FIX: ak do style elementu pridame aj color, tak quill zachova cely span element (inak ho zmaze)
        //pridame to ako prve, ak by tam bolo dalsie color:nieco
        htmlCode = htmlCode.replace(/ style="/gi, ' style="color:inherit;');

        let $html = $("<section>"+htmlCode+"</section>");
        //append data-list to LI elements depending on OL or UL parent
        $html.find("li").each(function () {
            let $li = $(this);
            if ($li.parent("ol").length > 0) {
                $li.attr("data-list", "ordered");
            } else if ($li.parent("ul").length > 0) {
                $li.attr("data-list", "bullet");
            }
        });

        //replace UL with OL if it has data-list="ordered"
        $html.find("ul").each(function () {
            let $ul = $(this);

            //save all attributes from ul to ol
            let ulAttrs = $ul[0].attributes;
            let $ol = $("<ol>");
            for (let i = 0; i < ulAttrs.length; i++) {
                let attr = ulAttrs[i];
                $ol.attr(attr.name, attr.value);
            }

            $ul.replaceWith(function () {
                return $ol.append($(this).contents());
            });
        });

        htmlCode = $html.html();
        return htmlCode;
    }

    /**
     * Update HTML from quill to standard HTML tags, e.g.:
     * ul.li -> ol-li.bullet
     * @param {String} html
     */
    window.quillToHtmlFormat = function(htmlCode) {
        //remove <span class="ql-ui" contenteditable="false"> from the HTML code
        htmlCode = htmlCode.replace(/<span class="ql-ui" contenteditable="false"><\/span>/gi, '');

        //replace <ol><li data-list="bullet"> with <ul>
        let $html = $("<section>"+htmlCode+"</section>");
        //console.log("htmlCode before ul fix=", $html.html());
        $html.find("li").each(function () {
            let $li = $(this);
            if ($li.data("list") === "bullet") {
                let $ol = $li.parent("ol");
                if ($ol.length > 0) {
                    //replace ol with ul

                    //save all attributes from ol to ul
                    let olAttrs = $ol[0].attributes;
                    let $ul = $("<ul>");
                    for (let i = 0; i < olAttrs.length; i++) {
                        let attr = olAttrs[i];
                        $ul.attr(attr.name, attr.value);
                    }

                    $ol.replaceWith(function () {
                        return $ul.append($(this).contents());
                    });
                }
            }
        });

        //remove data-list attribute from li elements
        $html.find("li").each(function () {
            let $li = $(this);
            if ($li.data("list")) {
                $li.removeAttr("data-list");
            }
        });

        htmlCode = $html.html();
        return htmlCode;
    }

    return {
        create: function (conf) {
            //console.log("Creating quill editor");
            var safeId = $.fn.dataTable.Editor.safeId(conf.id);
            var input = $(
                '<div id="' + safeId + '" class="quill-wrapper">' +
                '<div class="editor"></div>' +
                '</div>'
            );

            Quill.debug('error');
            Quill.register({
                //'modules/toolbar': Toolbar,
                //'themes/snow': Snow,
                "modules/htmlEditButton": htmlEditButton
            });
            Quill.register(SoftLineBreakBlot);

            // Define the matcher function
            function removeStylesAndClasses(node, delta) {
                //console.log("removeStylesAndClasses", node, "delta=", delta);

                try {
                    // Remove style and class attributes from the delta ops
                    delta.ops.forEach(op => {
                        if (op.attributes) {
                            Object.keys(op.attributes).forEach(attr => {
                                if (attr !== 'bold' && attr !== 'italic' && attr !== 'list' && attr !== 'link' && attr !== 'header' && attr !== 'div') {
                                    //console.log("Delete attr", attr);
                                    delete op.attributes[attr];
                                }
                            });
                        }
                    });
                } catch (e) {
                    console.log(e);
                }

                return delta;
            }

            conf._quill = new Quill(input.find('.editor')[0], $.extend(true, {
                theme: 'snow',
                modules: {
                    toolbar: toolbarOptions,
                    htmlEditButton: {
                        msg: WJ.translate("datatables.quill.htmlButton.tooltip.js"), //Custom message to display in the editor, default: Edit HTML here, when you click "OK" the quill editor's contents will be replaced
                        okText: '<i class="ti ti-check"></i> '+WJ.translate("button.submit"), // Text to display in the OK button, default: Ok,
                        cancelText: '<i class="ti ti-x"></i> '+WJ.translate("button.cancel"), // Text to display in the cancel button, default: Cancel
                        buttonHTML: "<i class='ti ti-code'></i> ", // Text to display in the toolbar button, default: <>
                        buttonTitle: WJ.translate("datatables.quill.htmlButton.tooltip.js"), // Text to display as the tooltip for the toolbar button, default: Show HTML source
                        debug: false
                    },
                    clipboard: {
                        matchers: [
                            ['BR', lineBreakMatcher]
                        ]
                    },
                    keyboard: {
                        bindings: {
                            "shift enter": {
                                key: 'Enter',
                                shiftKey: true,
                                handler: function (range) {
                                    shiftEnterHandler(this.quill, range);
                                }
                            }
                        }
                    }
                }
            }, conf.opts));

            // Add the matcher to the clipboard module
            conf._quill.clipboard.addMatcher(Node.ELEMENT_NODE, removeStylesAndClasses);

            return input;
        },

        get: function (conf) {
            //console.log("QUILL GET=", conf._quill.root.innerHTML);
            var htmlCode = conf._quill.root.innerHTML;
            //prazdny text povazuj za prazdny, aby nam fungovalo required field
            if ("<p><br></p>"==htmlCode || ""==conf._quill.getText()) htmlCode = "";

            return window.quillToHtmlFormat(htmlCode);
        },

        set: function (conf, val) {
            var htmlCode = quillFromHtmlFormat(val);

            //console.log("QUILL SET, FIXED htmlCode=", htmlCode);
            conf._quill.root.innerHTML = htmlCode;
        },

        enable: function (conf) {
        }, // not supported by Quill

        disable: function (conf) {
        }, // not supported by Quill

        // Get the Quill instance
        quill: function (conf) {
            return conf._quill;
        }
    }

}