import Scrollbar from 'smooth-scrollbar';
import Ninja from '../js/global-functions.js';
import WJ from './webjet.js';

window.domReady.add(initClosure, 1, true);

window.webjetTranslationService.onAfterLoad(() => {
    $(document).ready(() => {
        window.domReady.fire();
    });
}, true).load();

// =======================
// In iframe dialog view
// =======================
if (window != window.top) {
    //nastavujeme takto, aby sa to nastavilo ihned pred renderingom
    if (window.location.href.indexOf("showOnlyEditor=true")!=-1) {
        document.getElementsByTagName("html")[0].classList.add("in-iframe");
    } else {
        document.getElementsByTagName("html")[0].classList.add("in-iframe-show-table");
    }
}

function initClosure() {

    // =======================
    // In iframe dialog view
    // =======================
    //console.log("window.parent.WJ=", window.parent.WJ);
    if (window != window.top && typeof window.parent.WJ != "undefined" && typeof window.parent.WJ.dispatchEvent != "undefined") {
        window.parent.WJ.dispatchEvent('WJ.iframeLoaded', {
            window: window
        });
    }

    // =======================
    // SCROLLBAR INIT
    // =======================

    const scrollbarMenu = Scrollbar.init(document.querySelector('.menu-wrapper'), {
        damping: 0.2
    });

    Scrollbar.initAll();
    Scrollbar.detachStyle();

    // =======================
    // MENU EVENTS INIT
    // =======================

    $(".md-main-menu__item__link").on("click", function (e) {

        if ($(this).siblings(".md-main-menu__item__sub-menu").length) {
            e.preventDefault();

            $(this).parent(".md-main-menu__item").toggleClass('md-main-menu__item--open');
            // if ($(this).parent(".md-main-menu__item").hasClass("md-main-menu__item--open")) {
            //     $(this).parent(".md-main-menu__item").removeClass("md-main-menu__item--open");
            // } else {
            //     $(this).parent(".md-main-menu__item").addClass("md-main-menu__item--open");
            // }
        }
    });

    $(".md-large-menu__item__link").on("click", function (e) {

        if ($(this).parent(".md-large-menu__item").hasClass("md-large-menu__item--active")) {
            e.preventDefault();
        } else {

            $(".md-large-menu__item").not($(this).parent(".md-large-menu__item")).removeClass("md-large-menu__item--active");
            $(this).parent(".md-large-menu__item").addClass("md-large-menu__item--active");

            var menuId = $(this).attr("data-menu-id");

            $(".md-main-menu").removeClass("md-main-menu--open");
            $('.md-main-menu[data-menu-id="' + menuId + '"]').addClass("md-main-menu--open");
            $(".md-main-menu__item").removeClass("md-main-menu__item--open");
        }

        scrollbarMenu.scrollTop = 0;
    });

    $(function () {
        var current = location.pathname;
        //../logon-user-details/ nemame v menu, chceme zobrazit menu polozku .../logon-user/
        current = current.replace("index-details", "");
        current = current.replace("index-detail", "");
        current = current.replace("-details", "");
        current = current.replace("-detail", "");
        if (current.endsWith("/") === false && current.indexOf(".") === -1) {
            current += "/";
        }
        var search = location.search;
        if (search!=null && search.indexOf("?menu=")==0) current = current + search;
        if ("/admin/v9/webpages/linkcheck/"==current) current = "/admin/v9/webpages/web-pages-list/";
        $('.md-main-menu__item__link, .md-main-menu__item__sub-menu__item__link').each(function () {
            var $this = $(this);

            //console.log("Comparing: this=", $this.attr('href'), "current=", current, " eq=", ($this.attr('href')==current));
            if ($this.attr('href').indexOf("/v9") === -1 && $this.attr('href').indexOf("/apps") != 0) {
                //to co nie je v9 daj CSS triedu v8version
                $this.parents(".md-main-menu__item__sub-menu__item").addClass("md-main-menu__item__sub-menu__item--v8version");
            }

            if ($this.attr('href') === current) {
                $this.parents(".md-main-menu__item__sub-menu__item").addClass("md-main-menu__item__sub-menu__item--active");

                $this.parents(".md-main-menu__item").addClass("md-main-menu__item--active");
                $this.parents(".md-main-menu__item").addClass("md-main-menu__item--open");

                $this.parents(".md-main-menu").addClass("md-main-menu--open");
                var menuId = $this.parents(".md-main-menu").data("menu-id");
                $('.md-large-menu__item__link[data-menu-id="' + menuId + '"]').parents(".md-large-menu__item").addClass("md-large-menu__item--open md-large-menu__item--active");
            }
        });

        $('.md-main-menu__item').each(function () {
            var $this = $(this);
            var hasSomeV9 = false;
            $this.find("a").each(function () {
                if ($(this).attr('href').indexOf("/v9") !== -1 || $(this).attr('href').indexOf("/apps") === 0) {
                    hasSomeV9 = true;
                }
            });

            if (hasSomeV9 === false) {
                $this.addClass("md-main-menu__item--v8version");
            }
        });

        //scrolll selected left menu item into view
        let $menuItem = $("div.md-main-menu__item--open");
        if ($menuItem.length>0) {
            let top = $menuItem.position().top;
            if (top > 150) {
                setTimeout(()=> {
                    scrollbarMenu.scrollTop = top-52;
                }, 50);
            }
        }
    });

    //sidebar toogler responsive
    $("div.js-sidebar-toggler").on("click", function(e) {
        $("div.ly-sidebar").toggleClass("active");
        $("div.ly-page-wrapper").toggleClass("active");
        $(this).children("i").toggleClass("fa-times");
    });

    // =======================
    // NINJA INIT
    // =======================

    Ninja.init({
        debug: false
    });


    // =======================
    // TOOLTIP INIT
    // =======================
    WJ.initTooltip($('[data-toggle*="tooltip"]'));

    // =======================
    // ALERT POSITION CHANGE
    // =======================

    $('.alert-dismissible').appendTo("body");

    // =======================
    // MODAL INIT
    // =======================

    $('.modal').attr('data-keyboard', false).attr('data-backgrop', 'static').appendTo("body");

    $.each($('[data-toggle*="modal"]'), function (key, item) {
        $(item).on("click", function () {
            $($(item).data("target")).modal({
                backdrop: 'static',
                keyboard: false
            });
        });
    });

    // =======================
    // MODAL DRAGGABLE INIT
    // =======================

    $('.modal-dialog').draggable({
        handle: ".modal-title",
        cursor: 'move'
    });

    // =======================
    // SELECTPICKER INIT
    // =======================

    $.fn.selectpicker.Constructor.BootstrapVersion = '5';

    $('select').selectpicker({
        container: "body",
        style: "dropdown bootstrap-select btn-outline-secondary",
        liveSearch: true,
        showSubtext: true,
        noneSelectedText: '\xa0' //nbsp
    });

    // =======================
    // JSTREE INIT
    // =======================

    var somStromcek = $('#SomStromcek');
    var jsTreeMoveUrl = somStromcek.data("rest-move-url");
    var jsTreeParamName = somStromcek.data("rest-param-name");

    function getJstreeUrl() {
        if (typeof window.getJstreeUrl=="function") return window.getJstreeUrl();
        return somStromcek.data("rest-url");
    }

    $.jstree.defaults.core.dblclick_toggle = false;
    window.jstree = somStromcek.jstree({
        'core': {
            'check_callback': function (operation, node, parent, position, more) {
                if (operation === 'copy_node') {
                    return false;
                }

                if (operation === 'move_node') {
                    if (parent.id === '#') {
                        return false; // prevent moving a child above or below the root
                    }
                    //if (more.ref) console.log(more.ref.id+" "+more.pos+" "+position, "more=", more);
                    if (more && more.ref && more.ref.id)
                    {
                        if (more.ref.id.indexOf("docId-")!=-1)
                        {
                            //web stranka
                            //console.log(node);
                            if (node && node.id && node.id.indexOf("docId-")!=-1 && ( more.pos=='b' || more.pos=='a'))
                            {
                                //console.log("TRUE 1");
                                return true;
                            }
                            //console.log("FALSE");
                            return false;
                        }
                        else
                        {
                            //adresar
                            if (node && node.id && node.id.indexOf("docId-")==-1 && ( more.pos=='b' || more.pos=='a'))
                            {
                                //console.log("TRUE 2");
                                return true;
                            }
                        }
                    }
                }

                return true; // allow everything else
            },
            'data': function (obj, callback) {
                var jsTreeParamValue;
                if (jsTreeParamName === 'url') {
                    jsTreeParamValue = '/images';
                    if (obj.id !== '#' && typeof obj.original !== 'undefined') {
                        jsTreeParamValue = obj.original.virtualPath;
                    }
                } else {
                    jsTreeParamValue = 0;
                    if (obj.id !== '#') {
                        jsTreeParamValue = obj.id;
                    }
                }

                var data = {};
                data[jsTreeParamName] = jsTreeParamValue;

                //console.log("SOM DATA, url=", getJstreeUrl(), "jsTreeParamValue=", jsTreeParamValue);

                if (typeof window.treeInitialJson !== 'undefined' && window.treeInitialJson != null && window.treeInitialJson.length>0) {

                    //uloz original objekt na debug, cez json.parse vznikne kopia
                    if (typeof window.treeInitialJsonOriginal=="undefined") window.treeInitialJsonOriginal = JSON.parse(JSON.stringify(window.treeInitialJson));

                    let items = [];
                    let itemsRemaining = [];
                    window.treeInitialJson.forEach((item) => {
                        //najdi selected objekt
                        if (item.state.selected===true) {
                            //console.log("Selected item=", item);
                            window.selectedNode = item;
                        }
                        //console.log(item.id, "item.state.loaded=", item.state.loaded);
                        if ((jsTreeParamValue == 0 && (item.parent == null || item.parent=='' || item.parent=="#")) ||
                            (jsTreeParamValue == item.parent)) {
                            items.push(item);
                        } else {
                            itemsRemaining.push(item);
                        }
                    });
                    //na dalsie pouzitie zachovaj remaining
                    window.treeInitialJson = itemsRemaining;

                    if (typeof window.selectedNode != undefined && window.selectedNode != null) {
                        //podla selectedNode nastav tlacidla (prava)
                        if (window.selectedNode.icon.indexOf("folder-times")!=-1) {
                            $('.tree-col .dt-header-row .buttons-create').addClass("disabled");
                            $('.tree-col .dt-header-row .buttons-edit').addClass("disabled");
                            $('.tree-col .dt-header-row .buttons-remove').addClass("disabled");
                            $('#datatableInit_wrapper .dt-header-row .buttons-create').addClass("disabled");
                            setTimeout(()=> {
                                $('#datatableInit_wrapper .dt-header-row .buttons-create').addClass("disabled");
                            }, 500);
                        }
                    }

                    //console.log("items:", items, "itemsRemaining:", itemsRemaining);

                    //ak sme nieco nasli v JSON datach pouzi, inak sprav ajax request
                    if (items.length>0) {
                        if (typeof window.jstreeCustomizeData == "function") {
                            window.jstreeCustomizeData(items);
                        }

                        callback.call(this, items);
                        return;
                    }
                }

                //console.log("Nenasiel som, robim request, jsTreeParamValue=", jsTreeParamValue, "data=", data);
                //nenaslo sa v initial datach, spravime rest request
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

                        if (typeof window.jstreeCustomizeData == "function") {
                            window.jstreeCustomizeData(data.items);
                        }

                        callback.call(this, data.items);
                    }
                });
            }
        },
        "plugins": [
            "dnd"
        ],
        "types": {
            "#": {
                "max_children": 1,
                "max_depth": 4,
                "valid_children": ["root"]
            },
            "root": {
                "icon": "/static/3.3.8/assets/images/tree_icon.png",
                "valid_children": ["default"]
            },
            "default": {
                "valid_children": ["default", "file"]
            },
            "file": {
                "icon": "glyphicon glyphicon-file",
                "valid_children": []
            }
        },
        dnd: {
            is_draggable: function (node) {
                var allowDragDrop = $("#treeAllowDragDrop").is(":checked");
                //console.log("is draggable, node", node, "allowDragDrop=", allowDragDrop);
                return allowDragDrop;
            }
        }
    });

    window.jstree.on("move_node.jstree", function (e, data) {
        //console.log("Drop node " + data.node.id + " to " + data.parent);
        //console.log("Data", data);
        delete (data.node.children);
        //console.log("Data", data);
        var json = {
            node: data.node,
            old_parent: data.old_parent,
            old_position: data.old_position,
            parent: data.parent,
            position: data.position
        };

        $("div.dataTables_processing").show();
        $.ajax({
            url: WJ.urlAddPath(getJstreeUrl(), "/move"),
            method: 'post',
            dataType: 'json',
            contentType: 'application/json',
            data: JSON.stringify(json),
            success: function (response) {
                $("div.dataTables_processing").hide();
                if (!response.result) {
                    WJ.notifyError(response.error);
                    return;
                }

                // LPA: bez refreshu parenta jstree hadzalo internu chybu
                window.jstree.jstree(true).refresh(data.parent);
            },
            error: () => {
                $("div.dataTables_processing").hide();
            }
        })
    });

    //we want to always open the node on click
    window.jstree.on("click.jstree", ".jstree-anchor", function (e) {
        setTimeout(() => {
            //console.log("expanded=", $(e.target).attr("aria-expanded"));
            if ($(e.target).attr("aria-expanded")=="false") window.jstree.jstree("open_node", e.target);
        }, 100);
    });

    window.jstree.on('select_node.jstree', function (e, data) {
        //console.log("Tree item clicked, data=", data, "jstree=", jstree);
        window.jstree.jstree("open_node", $("#" + data.node.id.replace(/\//gi, "\\/")));

        window.jsTreeDocumentOpener.next();
        window.jsTreeFolderOpener.next();
        window.jsTreeFolderOpener.setInputValue(data?.selected[0]);
    });

    window.jstree.on('loaded.jstree', function () {
        window.jsTreeDocumentOpener.next();
        window.jsTreeFolderOpener.next();
    });

    window.jstree.on('after_open.jstree', function () {
        window.jsTreeDocumentOpener.next();
        window.jsTreeFolderOpener.next();
    });

    $('.tree-col .dt-header-row .buttons-refresh').on("click", function () {
        window.jstree.jstree("refresh");
    });

    /*
    $(document).on("dnd_start.vakata", function(e, data) {
        console.log("Start dnd");
    })
    .on("dnd_move.vakata", function(e, data) {
        console.log("Move dnd");
    })
    .on("dnd_stop.vakata", function(e, data) {
        console.log("Stop dnd");
    });
     */

    // =======================
    // GLOBAL LNG
    // =======================

    var userLng = window.userLng;

    // =======================
    // MOMENT INIT
    // =======================

    moment.updateLocale(userLng, {invalidDate: ""});
    moment.locale(userLng);

    // =======================
    // NUMERAL INIT
    // =======================

    numeral.locale(userLng);

    // =======================
    // Drzanie session
    // =======================
    WJ.keepSession();

    // =======================
    // Autocomplete autobind
    // =======================
    setTimeout(() => {
        //bindneme cez timaout, nech sa zrelaxuje prehliadac/cpu
        $("input.autocomplete").each((index, value) => {
            //console.log("Binding autocomplete, value=", value);
            new AutoCompleter($(value)).autobind();
        })
    }, 1000);

    // =======================
    // Kontrola otvoreneho dialogu pri opusteni stranky
    // =======================
    window.addEventListener('beforeunload', (event) => {
        try {
            //console.log("Beforeunload event, modal.length=", $("div.modal.DTED.show").length);
            if ($("div.modal.DTED.show").length>0 && window.currentUser.login.indexOf("tester")!=0) {
                var confirmationMessage = WJ.translate("admin.confirmExitMessage.js");

                //console.log("confirmationMessage=", confirmationMessage);

                //prehliadace aj tak ignoruju co vratim a zobrazia svoje hlasenie
                event.returnValue = confirmationMessage;
            }
        }
        catch (ex) { console.log(ex); }
    });

    //useredit profile dialog clickink on name in header
    window.openProfileDialog = function(userId, onlyself) {
        var width = Math.min(1170, $(window).innerWidth());
        var height = $(window).innerHeight()-32-71-20;

        var url = '/admin/v9/users/user-list/?showOnlyEditor=true&id='+userId;
        if (onlyself) url = '/admin/v9/users/self/?showOnlyEditor=true&id='+userId;

        WJ.openIframeModal({
            url: url,
            width: width,
            height: height,
            buttonTitleKey: "button.save",
            closeButtonPosition: "close-button-over",
            okclick: function() {
                $('#modalIframeIframeElement').contents().find('div.modal.DTED.show div.DTE_Footer button.btn-primary').trigger("click");
                return false;
            },
            onload: function(detail) {
                let iframeWindow = detail.window;
                iframeWindow.addEventListener("WJ.DTE.close", function(event) {
                    WJ.notifyInfo(WJ.translate("user.profile.save.title.js"), WJ.translate("user.profile.save.notifyText.js"), 10000, [
                        {
                            "title": WJ.translate("menu.logout"),
                            "cssClass": "btn btn-primary",
                            "icon": "far fa-sign-out",
                            "click": "window.location.href=$('.js-logout-toggler').attr('href')"
                        }
                    ]);
                    WJ.closeIframeModal();
                });
                iframeWindow.addEventListener("WJ.DTE.open", function(event) {
                    iframeWindow.$("#modalIframeLoader").css("display", "none");
                });
            }
        });
    }
}

window.WJ.DataTable.mergeColumns = function (columns, obj) {
    if (typeof obj.name === 'undefined') {
        console.error("Objekt nema nastaveny name");
        return;
    }

    var found = false;
    $.each(columns, function (i, v) {
        if (v.name !== obj.name) {
            return true;
        }

        found = true;
        columns[i] = $.extend(true, {}, v, obj);
    });

    if (!found) {
        console.error("Objekt s name: " + obj.name + ", nenajdeny");
    }
};