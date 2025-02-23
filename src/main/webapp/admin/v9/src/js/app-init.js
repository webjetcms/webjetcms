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
    if (window.location.href.indexOf("showEditorFooterPrimary=true")!=-1) {
        document.getElementsByTagName("html")[0].classList.add("in-iframe-show-footer-primary");
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

    const scrollbarMain = Scrollbar.init(document.querySelector('.ly-content-wrapper .ly-content'));
    window.scrollbarMain = scrollbarMain;
    Scrollbar.detachStyle();

    // =======================
    // MENU EVENTS INIT
    // =======================

    $(".md-large-menu__item__link").on("click", function (e) {

        e.preventDefault();

        if ($(this).parent(".md-large-menu__item").hasClass("md-large-menu__item--active")==false) {

            $(".md-large-menu__item").not($(this).parent(".md-large-menu__item")).removeClass("md-large-menu__item--active");
            $(this).parent(".md-large-menu__item").addClass("md-large-menu__item--active");

            var menuId = $(this).attr("data-menu-id");

            $(".md-main-menu").removeClass("md-main-menu--open");
            $('.md-main-menu[data-menu-id="' + menuId + '"]').addClass("md-main-menu--open");
            $(".md-main-menu__item").removeClass("md-main-menu__item--open");
        }

        scrollbarMenu.scrollTop = 0;
    });

    function hideFirstBreadcrumbItem(a, mainTitle) {
        //if there is element in .md-breadcrumb .nav .nav-item .nav-link with same name hide it
        let breadcrumb = $(".ly-content .md-breadcrumb .nav .nav-item");
        let hidenCount = 0;
        breadcrumb.each(function() {
            let $this = $(this);
            //console.log("Comparing: this=", $this.text(), "a=", a.text());
            if ($this.text()==a.text() || $this.text()==mainTitle) {
                $this.hide();
                hidenCount++;
            }
        });
        if (breadcrumb.length===hidenCount) {
            //console.log("Hiding whole breadcrumb, length=", breadcrumb.length, "hidenCount=", hidenCount);
            //if only one element in breadcrumb hide whole breadcrumb
            breadcrumb.first().parents(".md-breadcrumb").hide();
        }
    }

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
        var currentWithHash = current+window.location.hash;
        currentWithHash = currentWithHash.replace("?#", "#");
        if ("/admin/v9/webpages/linkcheck/"==current) current = "/admin/v9/webpages/web-pages-list/";
        $('.md-main-menu__item__link, .md-main-menu__item__sub-menu__item__link').each(function () {
            let $this = $(this);

            //console.log("Comparing: this=", $this.attr('href'), "current=", current, " eq=", ($this.attr('href')==current));
            if ($this.attr('href') === current || $this.attr('href') === currentWithHash) {
                $this.parents(".md-main-menu__item__sub-menu__item").addClass("md-main-menu__item__sub-menu__item--active");

                let mainMenuItem = $this.parents(".md-main-menu__item");
                mainMenuItem.addClass("md-main-menu__item--active");
                //mainMenuItem.addClass("md-main-menu__item--open");

                let mainMenu = $this.parents(".md-main-menu");
                mainMenu.addClass("md-main-menu--open");
                let menuId = mainMenu.data("menu-id");
                $('.md-large-menu__item__link[data-menu-id="' + menuId + '"]').parents(".md-large-menu__item").addClass("md-large-menu__item--open md-large-menu__item--active");

                let mainTitle = mainMenuItem.find(".md-main-menu__item__link").text();

                //move submenu items to tabs
                let tabs = mainMenuItem.find(".md-main-menu__item__sub-menu div");
                //console.log("tabs=", tabs);
                if (tabs.length>0) {
                    //iterate over all tabs, find A elements and create UL - LI structure
                    let ul = $("<ul class='nav' role='tablist'></ul>");
                    tabs.each(function() {
                        let $this = $(this);
                        let aOriginal = $this.find("a");
                        let active = $this.hasClass("md-main-menu__item__sub-menu__item--active") ? " active" : "";
                        let li = $("<li class='nav-item' role='presentation'></li>");
                        let a = $("<a class='nav-link"+active+"' role='tab'>"+aOriginal.text()+"</a>");
                        a.attr("href", aOriginal.attr("href"));
                        li.append(a);
                        ul.append(li);

                        if (" active"===active) {
                            hideFirstBreadcrumbItem(a, mainTitle);
                        }
                    });
                    //wrap UL with md-tabs div
                    ul = $("<div class='md-tabs md-tabs-dropdown'></div>").append(ul);

                    //append UL to main menu
                    $(".ly-submenu").html(ul);
                    $("body").addClass("ly-submenu-active");
                }

                //set main title
                WJ.setTitle(mainTitle);
                let $headerTitle = $(".header-title");
                hideFirstBreadcrumbItem($headerTitle, mainTitle);

                //handle tabs click - we need also to execute link so it cant be BS tabs
                $(".ly-submenu .md-tabs li").on("click", "a", function(e) {
                    let $this = $(this);
                    let isActive = $this.hasClass("active");
                    $this.parents(".md-tabs").find("li a.active").removeClass("active");
                    if (isActive) {
                        $this.addClass("active");
                        //this is click on active tab burger menu on small device, prevent click, just open/close menu
                        if ($this.closest('ul').css("position")=="relative") {
                            $this.closest('ul').toggleClass("open");
                            e.preventDefault();
                        }
                    } else {
                        $this.addClass("active");
                        $this.closest('ul').removeClass("open");
                    }
                });
            }
        });

        //scrolll selected left menu item into view
        let $menuItem = $("div.md-main-menu__item--active");
        if ($menuItem.length>0) {
            let top = $menuItem.position().top;
            if (top > 150) {
                setTimeout(()=> {
                    scrollbarMenu.scrollTop = top-52;
                }, 50);
            }
        }

        $(".ly-sidebar .menu-wrapper a").on("click", function () {
            var $this = $(this);
            if ($this.parent().hasClass("md-main-menu__item--has-children")) return;
            var href = $(this).attr("href");
			if (href.indexOf("javascript:") === 0) return;
            WJ.selectMenuItem(href);
        });
    });

    //sidebar toogler responsive
    $("div.js-sidebar-toggler").on("click", function(e) {
        $("div.ly-sidebar").toggleClass("active");
        $("div.ly-page-wrapper").toggleClass("active");
        $(this).children("i").toggleClass("ti-x");
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
    WJ.initTooltip($('[data-bs-toggle*="tooltip"]'));

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

    $('select').each(function() {
        let $this = $(this);
        if ($this.hasClass("no-picker")) return;
        let options = {
            container: "body",
            style: "dropdown bootstrap-select btn-outline-secondary",
            liveSearch: true,
            showSubtext: true,
            noneSelectedText: '\xa0', //nbsp
            iconBase: 'ti',
        }
        let liveSearch = $this.data("live-search");
        //console.log("liveSearch=", liveSearch);
        if (typeof liveSearch !== "undefined") options.liveSearch = liveSearch;
        $this.selectpicker(options);
    });

    // =======================
    // JSTREE INIT
    // =======================

    var somStromcek = $('#SomStromcek');
    var jsTreeMoveUrl = somStromcek.data("rest-move-url");
    var jsTreeParamName = somStromcek.data("rest-param-name");
    var treeInitialJsonFired = false;

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
                        //console.log(item.id, "item.state.loaded=", item.state.loaded, "parent=", item.parent);
                        if (((jsTreeParamValue == 0 || jsTreeParamValue == "/images") && (item.parent == null || item.parent=='' || item.parent=="#")) ||
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
                        if (window.selectedNode.icon.indexOf("ti-folder-x")!=-1) {
                            $('.tree-col .dt-header-row .buttons-create').addClass("disabled");
                            $('.tree-col .dt-header-row .buttons-edit').addClass("disabled");
                            $('.tree-col .dt-header-row .buttons-remove').addClass("disabled");
                            $('#datatableInit_wrapper .dt-header-row .buttons-create').addClass("disabled");
                            setTimeout(()=> {
                                $('#datatableInit_wrapper .dt-header-row .buttons-create').addClass("disabled");
                            }, 500);
                        }

                        if (treeInitialJsonFired==false) {
                            treeInitialJsonFired = true;
                            setTimeout(()=> {
                                WJ.dispatchEvent("WJ.treeInitialJson.selectedNode", window.selectedNode);
                            }, 100);
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

                        if(getJstreeUrl().indexOf("treeSearchValue") > 0) {
                            data.items.forEach((item) => {
                                //for search we need to clear children, it's send as false bud jstree needs empty array
                                item.children = [];
                                item.state['opened']  = true;
                            });
                        }

                        callback.call(this, data.items);
                    }
                });
            }
        },
        "search": {
            "show_only_matches": true,
            "search_callback": function(word, node) {
                word = WJ.internationalToEnglish(word).toLowerCase();
                if (WJ.internationalToEnglish(node.text || "").toLowerCase().indexOf(word) >= 0) {
                    return true;
                }
                return false;
            }
        },
        "plugins": [
            "dnd",
            "search"
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

    //
    var select = `
        <select id="tree-folder-search-type" class="filter-input-prepend">
            <option value="contains" selected data-content="<i class=\'ti ti-arrows-horizontal\'></i><small>${WJ.translate('datatables.select.contains.js')}</small>">${WJ.translate('datatables.select.contains.js')}</option>
            <option value="startwith" data-content="<i class=\'ti ti-arrow-left-bar\'></i><small>${WJ.translate('datatables.select.startwith.js')}</small>">${WJ.translate('datatables.select.startwith.js')}</option>
            <option value="endwith" data-content="<i class=\'ti ti-arrow-right-bar\'></i><small>${WJ.translate('datatables.select.endwith.js')}</small>">${WJ.translate('datatables.select.endwith.js')}</option>
            <option value="equals" data-content="<i class=\'ti ti-equal\'></i><small>${WJ.translate('datatables.select.equals.js')}</small>">${WJ.translate('datatables.select.equals.js')}</option>
        </select>
    `;

    // Create the new element
    var searchModul = $(
        '<table class="table datatableInit dataTable no-footer" data-server-side="true" style="margin-left: 0px;" id="jstreeSearchTable">' +
            '<thead>' +
                '<tr>' +
                    `<th class="dt-format-selector dt-select-td cell-not-editable" tabindex="0" aria-controls="datatableInit" rowspan="1" colspan="1" data-column-index="0" style="padding: 8px 24px 4px 0px !important;">${WJ.translate('editor.directory_name')}</th>` +
                '</tr>' +
                '<tr>' +
                    '<th class="dt-format-text" data-column-index="2" rowspan="1" colspan="1" style="padding: 0px 0px 4px 0px !important;">' +
                        '<div class="input-group">' +
                            select +
                            '<input id="tree-folder-search-input" class="form-control form-control-sm filter-input">' +
                            '<button id="tree-folder-search-button" class="btn btn-sm btn-outline-secondary btn-search"><i class="ti ti-search"></i></button>' +
                            '<button id="tree-folder-search-clear-button" class="btn btn-sm btn-outline-secondary btn-clear" style="padding-top: 4px;"><i class="ti ti-circle-x"></i></button>' +
                        '</div>' +
                    '</th>' +
                '</tr>' +
            '</thead>' +
        '</table>');

    // Insert the new element before the somStromcek element
    searchModul.insertBefore(somStromcek);

    const DT_SELECTPICKER_OPTS_NOSEARCH = {
        container: "body",
        style: "btn btn-sm btn-outline-secondary",
        width: "100%",
        noneSelectedText: '\xa0' //nbsp
    };

    $('select.filter-input-prepend').selectpicker(DT_SELECTPICKER_OPTS_NOSEARCH);

    // Check if jsTree is loaded - hide loader
    window.jstree.on('ready.jstree', function (e, data) {
        WJ.hideLoader();
    });

    $('button#tree-folder-search-button').on('click', function () {
        var searchInput = $('input#tree-folder-search-input');
        if(searchInput !== null && searchInput !== undefined && searchInput.length > 0) {
            var searchString = searchInput.val();

            //Show loader
            WJ.showLoader(null, '.hide-while-loading');

            if(searchString === null || searchString === "" || searchString.length < 1) {
                //EMPTY SEARCH
                cancelSearch();
            } else {
                // Update url with search string - fire refresh
                let url = WJ.urlUpdateParam( getJstreeUrl() , "treeSearchValue", searchString);
                url = WJ.urlUpdateParam( url , "treeSearchType", $('#tree-folder-search-type').val());

                somStromcek.data('rest-url', url);
                somStromcek.jstree(true).refresh();
            }
        }
    });

    // Enter key press on search input
    $('#tree-folder-search-input').on('keypress', function (e) {
        if (e.which === 13) { // Enter key pressed
            e.preventDefault(); // Prevent the default form submission
            $('button#tree-folder-search-button').click(); // Trigger the search button click
        }
    });

    window.jstree.on('refresh.jstree', function (e, data) {
        let searchValue = $("#tree-folder-search-input").val();
        if(searchValue !== undefined && searchValue !== null  && searchValue.length > 0) {
            // Search value after tree id refreshed
            somStromcek.jstree(true).search(searchValue);
        } else {
            //NEEDS to be there - clear search after change of tabs
            somStromcek.jstree(true).search("");
        }
        // Hide loader
        WJ.hideLoader();
    });

    $('button#tree-folder-search-clear-button').on('click', function () {
        var searchInput = $('input#tree-folder-search-input');
        if(searchInput !== null && searchInput !== undefined && searchInput.length > 0) {
            cancelSearch();
        }
    });

    function cancelSearch() {
        var searchInput = $('input#tree-folder-search-input');
        if(searchInput !== null && searchInput !== undefined && searchInput.length > 0) {
            // Clear input
            searchInput.val("");

            // Cancel search
            somStromcek.jstree(true).search("");

            // Clean url - fire refresh
            let url = getJstreeUrl().replace(/&treeSearchValue=[^&]*/g, '');
            url = url.replace(/&treeSearchType=[^&]*/g, '');
            somStromcek.data('rest-url', url);
            somStromcek.jstree(true).refresh();
        }
    }

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

        $("div.dt-processing").show();
        $.ajax({
            url: WJ.urlAddPath(getJstreeUrl(), "/move"),
            method: 'post',
            dataType: 'json',
            contentType: 'application/json',
            data: JSON.stringify(json),
            success: function (response) {
                $("div.dt-processing").hide();
                if (!response.result) {
                    WJ.notifyError(response.error);
                    setTimeout(function() {
                        $('#SomStromcek').jstree(true).refresh();
                    }, 500);
                    return;
                }

                // LPA: bez refreshu parenta jstree hadzalo internu chybu
                window.jstree.jstree(true).refresh(data.parent);
            },
            error: () => {
                $("div.dt-processing").hide();
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
        WJ.showLoader(null, ".hide-while-loading");
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
                            "icon": "ti ti-logout",
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