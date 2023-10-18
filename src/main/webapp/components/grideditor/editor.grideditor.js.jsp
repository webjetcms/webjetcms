<%@ page import="sk.iway.iwcm.Tools"%><%@ page import="sk.iway.iwcm.doc.DocDetails"%><%@ page import="sk.iway.iwcm.doc.GroupDetails"%><%@ page import="sk.iway.iwcm.doc.GroupsDB"%><%@ page import="sk.iway.iwcm.doc.DocDB"%><%@ page pageEncoding="utf-8" contentType="text/javascript" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");%>
    <%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>

    (function( $ ){

        $.fn.gridEditor = function( options ) {

            var self = this;
            var grideditor = self.data('grideditor');


            /** Methods **/

            if (arguments[0] == 'getHtml') {
                if (grideditor) {
                    grideditor.deinit();
                    var html = self.html();
                    grideditor.init();
                    return html;
                } else {
                    return self.html();
                }
            }

            /** Initialize plugin */

            self.each(function(baseIndex, baseElem) {
                baseElem = $(baseElem);

                var settings = $.extend({
                    // 'section_classes'   : [{ label: 'Example class', cssClass: 'example-class'}],
                    // 'container_classes' : [{ label: 'Example class', cssClass: 'example-class'}],
                    // 'row_classes'       : [{ label: 'Example class', cssClass: 'example-class'}],
                    // 'col_classes'       : [{ label: 'Example class', cssClass: 'example-class'}],
                    'section_tools'     : [ {
                        title: '<iwcm:text key="grideditor.section.add_anchor" />',
                        iconClass: 'fa-anchor',
                        on: { click: function() {
                            var section = $(this).parents('section');
                            var id=prompt('<iwcm:text key="grideditor.section.add_anchor.select_id" />:',"");
                            section.attr("id",id);
                        } }
                    },{
                        title: '<iwcm:text key="grideditor.section.duplicate" />',
                        iconClass: 'fa-files-o',
                        on: { click: function() {
                            var el = $(this).parents('section')
                                .removeClass('is-toolbar-focus');
                            var el2 = el.clone()
                                .removeClass('is-toolbar-focus')
                                .insertAfter(el);

                            // reset tools-drawer
                            resetEditedElement(el);
                            resetEditedElement(el2);

                            animateElementIn(el2);
                        } }
                    } ],
                    'container_tools'   :   [ {
                        title: '<iwcm:text key="grideditor.container.set_width" />',
                        iconClass: 'fa-arrows-h',
                        on: { click: function() {
                            var container = $(this).parents('.container');
                            if(container.hasClass('container-fluid')){
                                container.removeClass('container-fluid');
                            }else{
                                container.addClass('container-fluid');
                            }
                        } }
                    },{
                        title: '<iwcm:text key="grideditor.container.duplicate" />',
                        iconClass: 'fa-files-o',
                        on: { click: function() {
                            var el = $(this).parents('.container')
                                .removeClass('is-toolbar-focus');
                            var el2 = el.clone()
                                .removeClass('is-toolbar-focus')
                                .insertAfter(el);

                            // reset tools-drawer
                            resetEditedElement(el);
                            resetEditedElement(el2);

                            animateElementIn(el2);
                        } }
                    }],
                    'row_tools'         : [ {
                        title: '<iwcm:text key="grideditor.row.duplicate" />',
                        iconClass: 'fa-files-o',
                        on: { click: function() {
                            var el = $(this).parents('.row')
                                .removeClass('is-toolbar-focus');
                            var el2 = el.clone()
                                .removeClass('is-toolbar-focus')
                                .insertAfter(el);

                            // reset tools-drawer
                            resetEditedElement(el);
                            resetEditedElement(el2);

                            animateElementIn(el2);
                        } }
                    } ],
                    'col_tools'         : [ {
                        title: '<iwcm:text key="grideditor.column.duplicate" />',
                        iconClass: 'fa-files-o',
                        on: { click: function() {
                            var el = $(this).parents('.column')
                                .removeClass('is-toolbar-focus');
                            var el2 = el.clone()
                                .removeClass('is-toolbar-focus')
                                .insertAfter(el);
                            // reset tools-drawer
                            resetEditedElement(el);
                            resetEditedElement(el2);

                            animateElementIn(el2);
                        } }
                    } ],
                    'custom_filter'     : '',
                    'content_types'     : ['ckeditor'],
                    'valid_col_sizes'   : [ 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
                    'source_textarea'   : '',
                    'bootstrap_version' : 3
                }, options);


                // Elems
                var canvas,
                    // addRowGroup,
                    htmlTextArea,
                    sideBar
                ;
                var colClasses = getBootstrapColClasses(settings.bootstrap_version);
                var curColClassIndex = 0; // Index of the column class we are manipulating currently
                var MAX_COL_SIZE = settings.valid_col_sizes[settings.valid_col_sizes.length-1];
                var MIN_COL_SIZE = settings.valid_col_sizes[0];

                // Copy html to sourceElement if a source textarea is given
                if (settings.source_textarea) {
                    var sourceEl = $(settings.source_textarea);

                    sourceEl.addClass('ge-html-output');
                    htmlTextArea = sourceEl;

                    if (sourceEl.val()) {
                        baseElem.html(sourceEl.val());
                    }
                }

                // Wrap content if it is non-bootstrap
                if (baseElem.children().length && !baseElem.find('section').length) {
                    var children = baseElem.children();
                    var newRow = $('<section><div class="container"><div class="row"><div class="col-md-12"/></div></div></section>').appendTo(baseElem);
                    newRow.find('.col-md-12').append(children);
                }

                setup();
                init();

                function setup() {
                    /* Setup canvas */
                    canvas = baseElem.addClass('ge-canvas');
                }

                function reset() {
                    deinit();
                    init();
                }
                function init() {
                    runFilter(true);
                    canvas.addClass('ge-editing');
                    addAllColClasses();
                    wrapContent();
                    canvas.find('.container-fluid').addClass('container');
                    //createCanvasControls();
                    createSectionControls();
                    createContainerControls();
                    createRowControls();
                    createColControls();
                    //createSectionEditablePadding();
                    // makeSortable();
                    setCurColClassIndex();
                    switchLayout(curColClassIndex);
                    bindCustomFunction();

                   <%--createSideBar(null, "");--%>
                }

                function deinit() {
                    canvas.removeClass('ge-editing');
                    canvas.find('.ge-tools-drawer').remove();
                    canvas.find('.ge-tools-drawerPlusColumn').remove();
                    canvas.find('.ge-tools-drawerPlusSection').remove();
                    canvas.find('.ge-emptyElement').removeClass('ge-emptyElement');
                    canvas.find('.container-fluid').removeClass('container');
                    canvas.find('.ge-emptyElement').removeClass('ge-emptyElement');
                    canvas.find('.ge-animateIn').removeClass('animateIn');
                    <%--removeSectionEditablePadding();--%>
                    if(typeof selectGroup != 'undefined' ) selectGroup.remove();
                    <%--sideBar.remove();--%>
                    // removeSortable();
                    runFilter();
                }
                function resetEditedElement(element){
                    deinitElement(element);
                    if( isEmpty(element) ) element.addClass('ge-emptyElement');
                    init();
                }
                function resetElementTools(element){
                    deinitElement(element);
                    initElement(element);
                }
                function resetColSizeTools(element){
                    var minus = element.find('.ge-tools-drawer span.makeNarrower');
                    minus.removeClass('disabledTool');
                    var plus = element.find('.ge-tools-drawer span.makeWider');
                    plus.removeClass('disabledTool')

                    if(isColNarrowest(element)) minus.addClass('disabledTool');
                    if(isColWidest(element)) plus.addClass('disabledTool');
                }

                // TODO: zatial ide len na COLUMN - stale plati????
                function initElement(element) {
                    runFilter(true);
                    if(element.is("section")){
                        createSectionControls();
                    }else if(element.hasClass("container")){
                        createContainerControls();
                    }else if(element.hasClass("row")) {
                        createRowControls();
                    }else if(element.hasClass("column")) {
                        createColControls(element);
                    }
                    // makeSortable();
                    setCurColClassIndex();
                    switchLayout(curColClassIndex);
                }
                function deinitElement(element){
                    element.find('.ge-tools-drawer').remove();
                    element.find('.ge-tools-drawerPlusColumn').remove();
                    element.find('.ge-tools-drawerSection').remove();
                    // removeSortable();
                    runFilter();
                }
                function bindCustomFunction(){
                    canvas.on({
                        mouseenter: function () {
                            $(this).parent().addClass('is-toolbar-focus');
                        },
                        mouseleave: function () {
                            $(this).parent().removeClass('is-toolbar-focus');
                        }
                    }, ".ge-tools-drawer");
                }
                function createCanvasControls() {
                    var drawerPlusSection = $('<div class="ge-tools-drawerPlusSection ge-tools-dPS-canvas"/>').prependTo(canvas);

                    createTool(drawerPlusSection, '<iwcm:text key="grideditor.section.add" />', "", "fa-plus", function () {
                        selectGroup = getSelectGroup();
                        showHideSelectGroup($(this));
                    });

                    var drawerPlusSectionName = $('<span class="ge-tools-drawerPlusSectionName"></span>')
                }
                function createSectionControls(element) {
                    var sections;
                    if(element != undefined) {
                        sections = element;
                    }else {
                        sections = canvas.find('section');
                    }
                    sections.each(function () {
                        var section = $(this);
                        if( isEmpty(section) ) section.addClass('ge-emptyElement');
                        if (section.find('> .ge-tools-drawer').length) {
                            return;
                        }
                        var drawerOuter = $('<div class="ge-tools-drawer"><i class="fa fa-cog"></div>').prependTo(section);
                        var drawer = $('<div class="ge-tools-drawer-inner" />').appendTo(drawerOuter);
                        <%--var drawerPlusSection = $('<div class="ge-tools-drawerPlusSection ge-tools-drawerPlusSection-bottom wj-ge-tools-colored wj-ge-tools-green" />').prependTo(section);--%>
                        <%--var drawerPlusSection_top = $('<div class="ge-tools-drawerPlusSection ge-tools-drawerPlusSection-top wj-ge-tools-colored wj-ge-tools-green" />').prependTo(section);--%>
                        // createTool(drawer, '<iwcm:text key="grideditor.section.move" />', 'ge-move', 'fa-arrows');

                        <%--createTool(drawerPlusSection, '<iwcm:text key="grideditor.section.add" />', "", "fa-plus", function () {--%>
                            <%--selectGroup = getSelectGroup();--%>
                            <%--&lt;%&ndash;showHideSelectGroup($(this));&ndash;%&gt;--%>
                            <%--showSelectGroup($(this));--%>
                        <%--});--%>
                        <%--createTool(drawerPlusSection_top, '<iwcm:text key="grideditor.section.add" />', "", "fa-plus", function () {--%>
                            <%--selectGroup = getSelectGroup();--%>
                            <%--&lt;%&ndash;showHideSelectGroup($(this));&ndash;%&gt;--%>
                            <%--showSelectGroup($(this));--%>
                        <%--});--%>

                        createToolsName(drawer,'<iwcm:text key="grideditor.section" />');

                        createTool(drawer, '<iwcm:text key="grideditor.section.up" />', isFirst(section)? "disabledTool" : "", "fa-arrow-up", function () {
                            var thisSection = $(this).parents('section')
                                .removeClass('is-toolbar-focus');
                            var previousSection = thisSection.prev('section');
                            thisSection.insertBefore(previousSection);

                            $('html, body').animate({
                                scrollTop: thisSection.offset().top
                            }, 2000);

                            // reset tools-drawer
                            resetEditedElement(thisSection);
                            resetEditedElement(thisSection.next('section')); // .next pretoze ich najskor prehodim
                        });

                        createTool(drawer, '<iwcm:text key="grideditor.section.down" />', isLast(section)? "disabledTool" : "", "fa-arrow-down", function () {
                            var thisSection = $(this).parents('section')
                                .removeClass('is-toolbar-focus');
                            var nextSection = thisSection.next('section');
                            thisSection.insertAfter(nextSection);

                            // reset tools-drawer
                            resetEditedElement(section);
                            resetEditedElement(section.prev('section')); // .prev pretoze ich najskor prehodim
                        });
                        settings.section_tools.forEach(function (t) {
                            createTool(drawer, t.title || '', t.className || '', t.iconClass || 'fa-wrench', t.on);
                        });
                        createTool(drawer, '<iwcm:text key="grideditor.section.remove" />', '', 'fa-trash', function () {
                            if (window.confirm('<iwcm:text key="grideditor.section.remove" />?')) {
                                var prevSection = section.prev();
                                var nextSection = section.next();
                                section.slideUp(function () {
                                    section.remove();

                                    // reset tools-drawer
                                    resetEditedElement(prevSection);
                                    resetEditedElement(nextSection);
                                });
                            }
                        });
                        createTool(drawer, '<iwcm:text key="grideditor.section.add_container" />', '', 'fa-plus-circle', function () {
                            var section = $(this).parents('section')
                                .removeClass('ge-emptyElement');
                            var container = createContainer()
                                .appendTo(section);
                            var content = $("<p>&nbsp;</p>")
                                .appendTo(container);
                            init();
                            // reset tools-drawer
                            resetEditedElement(container);
                            resetEditedElement(container.prev());
                        });
                    });
                }
                function createContainerControls(element){
                    var containers;
                    if(element != undefined){
                        containers = element;
                    }else {
                        containers = canvas.find('.container');
                    }
                    containers.each(function() {
                        var container = $(this);
                        if( isEmpty(container) ) container.addClass('ge-emptyElement');
                        if (container.find('> .ge-tools-drawer').length) { return; }

                        var drawerOuter = $('<div class="ge-tools-drawer"><i class="fa fa-cog"></div>').prependTo(container);
                        var drawer = $('<div class="ge-tools-drawer-inner" />').appendTo(drawerOuter);
                        createToolsName(drawer,'<iwcm:text key="grideditor.container" />');

                         <%--createTool(drawer, '<iwcm:text key="grideditor.container.move" />', 'ge-move', 'fa-arrows');--%>
                         <%--createTool(drawer, 'Settings', '', 'glyphicon-cog', function() {--%>
                             <%--details.toggle();--%>
                         <%--});--%>
                        createTool(drawer, '<iwcm:text key="grideditor.container.up" />', isFirst(container)? "disabledTool" : "", "fa-arrow-up", function () {
                            container.removeClass('is-toolbar-focus');
                            var previousContainer = container.prev('.container');
                            container.insertBefore(previousContainer);

                            // reset tools-drawer
                            resetEditedElement(container);
                            resetEditedElement(previousContainer);
                        });

                        createTool(drawer, '<iwcm:text key="grideditor.container.down" />', isLast(container)? "disabledTool" : "", "fa-arrow-down", function () {
                            container.removeClass('is-toolbar-focus');
                            var nextContainer = container.next('.container');
                            container.insertAfter(nextContainer);

                            // reset tools-drawer
                            resetEditedElement(container);
                            resetEditedElement(nextContainer);
                        });

                        settings.container_tools.forEach(function(t) {
                            createTool(drawer, t.title || '', t.className || '', t.iconClass || 'fa-wrench', t.on);
                        });
                        createTool(drawer, '<iwcm:text key="grideditor.container.remove" />', '', 'fa-trash', function() {
                            if (window.confirm('<iwcm:text key="grideditor.container.remove" />?')) {
                                var prevContainer = container.prev();
                                var nextContainer = container.next();
                                container.slideUp(function() {
                                    var section = container.parents('section');
                                    container.remove();
                                    if( isEmpty(section) ) section.addClass('ge-emptyElement');

                                    // reset tools-drawer
                                    resetEditedElement(prevContainer);
                                    resetEditedElement(nextContainer);
                                });
                            }
                        });
                        createTool(drawer, '<iwcm:text key="grideditor.container.add_row" />', 'ge-add-column', 'fa-plus-circle', function() {
                            var new_row = createRow()
                                .append(createColumn(6)).append(createColumn(6));
                            var content = container.find('.ge-content');
                            container.removeClass('ge-emptyElement');

                            var el1;
                            if(content.length >0 ){
                                el1 = content;
                            }else{
                                el1 = container;
                            }
                            if(el1.html()=="<p>&nbsp;</p>"){
                                el1.html("");
                            }
                            el1.append(new_row);


                            var prevRow = new_row.prev();

                            resetEditedElement(prevRow);
                            init();
                        });
                        // var details = createDetails(container, settings.container_classes).appendTo(drawer);
                    });
                }
                function createRowControls(element) {
                    var rows;
                    if(element != undefined){
                        rows = element;
                    }else{
                        rows = canvas.find('.row');
                    }
                    rows.each(function() {
                        var row = $(this);
                        if( isEmpty(row) ) row.addClass('ge-emptyElement');
                        if (row.find('> .ge-tools-drawer').length) { return; }

                        var drawerOuter = $('<div class="ge-tools-drawer"><i class="fa fa-cog"></div>').prependTo(row);
                        var drawer = $('<div class="ge-tools-drawer-inner" />').appendTo(drawerOuter);
                        var drawerPlusColumn = $('<div class="ge-tools-drawerPlusColumn ge-tools-drawerPlusColumn-row wj-ge-tools-colored wj-ge-tools-green"/>').prependTo(row);

                        createToolsName(drawer,'<iwcm:text key="grideditor.row" />');

                        createTool(drawerPlusColumn, '<iwcm:text key="grideditor.column.add_column" />', 'ge-add-column', 'fa-plus', function() {
                            var newColumn = createColumn(3);
                            newColumn.appendTo(row);
                            row.removeClass('ge-emptyElement');
                            init();
                        });

                        // createTool(drawer, '<iwcm:text key="grideditor.row.move" />', 'ge-move', 'fa-arrows');

                        createTool(drawer, '<iwcm:text key="grideditor.row.up" />', isFirst(row)? "disabledTool" : "", "fa-arrow-up", function () {
                            row.removeClass('is-toolbar-focus');
                            var prevRow = row.prev();
                            row.insertBefore(prevRow);

                            // reset tools-drawer
                            resetEditedElement(row);
                            resetEditedElement(prevRow);
                        });

                        createTool(drawer, '<iwcm:text key="grideditor.row.down" />', isLast(row)? "disabledTool" : "", "fa-arrow-down", function () {
                            row.removeClass('is-toolbar-focus');
                            var nextRow = row.next();
                            row.insertAfter(nextRow);

                            // reset tools-drawer
                            resetEditedElement(row);
                            resetEditedElement(nextRow);
                        });

                        settings.row_tools.forEach(function(t) {
                            createTool(drawer, t.title || '', t.className || '', t.iconClass || 'fa-wrench', t.on);
                        });

                        createTool(drawer, '<iwcm:text key="grideditor.row.remove" />', '', 'fa-trash', function() {
                            if (window.confirm('<iwcm:text key="grideditor.row.remove" />?')) {
                                row.slideUp(300,function() {
                                    var prevRow = row.prev();
                                    var nextRow = row.next();
                                    var columnParent = row.parents('.column');
                                    var container = row.parents('.container');
                                    row.remove();
                                    if( isEmpty(columnParent) ) columnParent.addClass('ge-emptyElement');
                                    if( isEmpty(container) ) container.addClass('ge-emptyElement');

                                    resetEditedElement(prevRow);
                                    resetEditedElement(nextRow);
                                });
                            }
                        });
                    });
                }

                function createColControls(element) {
                    var columns;
                    if(element != undefined){
                        columns = element;
                    }else{
                        columns = canvas.find('.column');
                    }
                    columns.each(function() {
                        var col = $(this);
                        if( isEmpty(col) ) col.addClass('ge-emptyElement');
                        if (col.find('> .ge-tools-drawer').length) { return; }

                        var drawerPlusColumnLeft = $('<div class="ge-tools-drawerPlusColumn ge-drawerPlusColumn-left wj-ge-tools-colored wj-ge-tools-green"/>').prependTo(col);
                        var drawerPlusColumnRight = $('<div class="ge-tools-drawerPlusColumn ge-drawerPlusColumn-right wj-ge-tools-colored wj-ge-tools-green"/>').prependTo(col);
                        var drawerOuter = $('<div class="ge-tools-drawer"><i class="fa fa-cog"></div>').prependTo(col);
                        var drawer = $('<div class="ge-tools-drawer-inner" />').appendTo(drawerOuter);

                        createToolsName(drawer,'<iwcm:text key="grideditor.column" />');

                        // createTool(drawer, '<iwcm:text key="grideditor.column.move" />', 'ge-move', 'fa-arrows');


                        createTool(drawer, '<iwcm:text key="grideditor.column.narrower" />', isColNarrowest(col)? "makeNarrower disabledTool":"makeNarrower", 'fa-minus', function (e) {
                            var colSizes = settings.valid_col_sizes;
                            setCurColClassIndex();
                            var curColClass = colClasses[curColClassIndex];
                            var curColSizeIndex = colSizes.indexOf(getColSize(col, curColClass));
                            var newSize = colSizes[clamp(curColSizeIndex - 1, 0, colSizes.length - 1)];
                            if (e.shiftKey) {
                                newSize = colSizes[0];
                            }

                            setColSize(col, curColClass, Math.max(newSize, MIN_COL_SIZE));

                            // reset drawer-tools
                            setTimeout(function(){
                                <%--resetElementTools(col);--%>

                                //aby neblikali tools pri zmensovani stlpca
                                resetColSizeTools(col);
                            },200);
                        });
                        createTool(drawer, '<iwcm:text key="grideditor.column.wider" />', isColWidest(col)?"makeWider disabledTool":"makeWider", 'fa-plus', function (e) {
                            var colSizes = settings.valid_col_sizes;
                            setCurColClassIndex();
                            var curColClass = colClasses[curColClassIndex];
                            var curColSizeIndex = colSizes.indexOf(getColSize(col, curColClass));
                            var newColSizeIndex = clamp(curColSizeIndex + 1, 0, colSizes.length - 1);
                            var newSize = colSizes[newColSizeIndex];
                            if (e.shiftKey) {
                                newSize = colSizes[colSizes.length - 1];
                            }
                            setColSize(col, curColClass, Math.min(newSize, MAX_COL_SIZE));

                            // reset drawer-tools
                            setTimeout(function(){
                                <%--resetElementTools(col);--%>

                                //aby neblikali tools pri zvacsovani stlpca
                                resetColSizeTools(col);
                            },200);
                        });

                        createTool(drawer, '<iwcm:text key="grideditor.column.left" />', isFirst(col)?"disabledTool":"", "fa-chevron-left", function () {
                            var thisColumn = $(this).parents('.column').first().removeClass('is-toolbar-focus');
                            var leftColumn = thisColumn.prev('.column').first();

                            if (leftColumn.length > 0) {
                                leftColumn.insertAfter(thisColumn);
                            } else {
                                alert('<iwcm:text key="grideditor.column.min" />');
                            }

                            // reset drawer-tools
                            resetEditedElement(col);
                            resetEditedElement(col.next('.column')); // .next() pretoze ho najskor presuniem
                        });
                        createTool(drawer,'<iwcm:text key="grideditor.column.right" />',isLast(col)?"disabledTool":"","fa-chevron-right",function(){
                            var thisColumn = $(this).parents('.column').first().removeClass('is-toolbar-focus');
                            var rightColumn = thisColumn.next('.column').first();

                            if( rightColumn.length >0 ){
                                thisColumn.insertAfter(rightColumn);
                            }else{
                                alert('<iwcm:text key="grideditor.column.max" />');
                            }

                            // reset drawer-tools
                            resetEditedElement(col);
                            resetEditedElement(col.prev('.column')); // .prev() pretoze ho najskor presuniem
                        });

                        settings.col_tools.forEach(function(t) {
                            createTool(drawer, t.title || '', t.className || '', t.iconClass || 'fa-wrench', t.on);
                        });

                        createTool(drawer, '<iwcm:text key="grideditor.column.remove" />', '', 'fa-trash', function() {
                            if (window.confirm('<iwcm:text key="grideditor.column.remove" />?')) {
                                var prevCol = col.prev();
                                var nextCol = col.next();
                                var row = col.closest('.row');

                                col.animate({
                                    opacity: 'hide',
                                    width: 'hide',
                                    height: 'hide'
                                }, 400, function() {
                                    col.remove();
                                    if( isEmpty(row) ) row.addClass('ge-emptyElement');

                                    resetEditedElement(prevCol);
                                    resetEditedElement(nextCol);
                                });
                            }
                        });
                        if( col.parentsUntil(canvas,'.row').length <= 1 ) {
                            createTool(drawer, '<iwcm:text key="grideditor.column.add_row" />', 'ge-add-row', 'fa-plus-circle', function () {
                                var row = createRow();
                                row.append(createColumn(6)).append(createColumn(6));
                                var $this = $(this);
                                var content = $this.closest('.column').find('> .ge-content');
                                content.append(row);
                                content.parent().removeClass('ge-emptyElement');
                                init();
                                resetEditedElement(content.find('.row'));

                                animateElementIn(row);
                            });
                        }
                        createTool(drawerPlusColumnLeft, '<iwcm:text key="grideditor.column.add_column" />', 'ge-add-column', 'fa-plus', function() {
                            var newColumn = createColumn(3);
                            newColumn.insertBefore(col);

                            resetEditedElement(newColumn);
                            resetEditedElement(col);

                            animateElementIn(newColumn);
                        });
                        createTool(drawerPlusColumnRight, '<iwcm:text key="grideditor.column.add_column" />', 'ge-add-column', 'fa-plus', function() {
                            var newColumn = createColumn(3);
                            newColumn.insertAfter(col);

                            resetEditedElement(newColumn);
                            resetEditedElement(col);

                            animateElementIn(newColumn);
                        });
                    });
                }

                function createToolsName(drawer,name){
                    var toolName = $('<span class="ge-tools-drawerName">'+name+'</span>');
                    toolName.appendTo(drawer);
                }

                function createTool(drawer, title, className, iconClass, eventHandlers) {
                    var tool = $('<span data-title="' + title + '" class="' + className + '"><i class="fa '+iconClass+'" aria-hidden="true"></i><span class="wj-ge-title" style="display:none;">'+title+'</span></span>')
                        .appendTo(drawer)
                    ;
                    if (typeof eventHandlers == 'function') {
                        tool.on('click', eventHandlers);
                    }
                    if (typeof eventHandlers == 'object') {
                        $.each(eventHandlers, function(name, func) {
                            tool.on(name, func);
                        });
                    }
                }

                function addAllColClasses() {
                    canvas.find('.column, div[class*="col-"]').each(function() {
                        var col = $(this);

                        var size = MAX_COL_SIZE;
                        var sizes = getColSizes(col);
                        <%--if (sizes.length) {--%>
                            <%--size = sizes[sizes.length-1].size;--%>
                        <%--}--%>

                        var elemClass = col.attr('class');
                        var colClassesReversed = colClasses.reverse();

                        colClassesReversed.forEach(function(colClass) {
                            if (elemClass.indexOf(colClass) == -1) {
                                col.addClass(colClass + size);
                            }
                            else{
                                for(var i = 0; i< sizes.length; i++){
                                   if(sizes[i].colClass == colClass ){
                                        size = sizes[i].size;
                                      }
                                }
                            }
                        });
                        colClasses.reverse();

                        col.addClass('column');
                    });
                }

                /**
                 * Return the column size for colClass, or a size from a different
                 * class if it was not found.
                 * Returns null if no size whatsoever was found.
                 */
                function getColSize(col, colClass) {
                    var sizes = getColSizes(col);
                    for (var i = 0; i < sizes.length; i++) {
                        if (sizes[i].colClass == colClass) {
                            return sizes[i].size;
                        }
                    }
                    if (sizes.length) {
                        return sizes[0].size;
                    }
                    return null;
                }

                function getColSizes(col) {
                    var result = [];
                    colClasses.forEach(function(colClass) {
                        var re = new RegExp(colClass + '(\\d+)', 'i');
                        if (re.test(col.attr('class'))) {
                            result.push({
                                colClass: colClass,
                                size: parseInt(re.exec(col.attr('class'))[1])
                            });
                        }
                    });
                    return result;
                }

                function setColSize(col, colClass, size) {
                    var re = new RegExp('(' + colClass + '(\\d+))', 'i');
                    var reResult = re.exec(col.attr('class'));

                    if (reResult && parseInt(reResult[2]) !== size) {
                        col.switchClass(reResult[1], colClass + size, 50);
                    } else {
                        col.addClass(colClass + size);
                    }
                }
                function setCurColClassIndex(){
                    if( canvas.hasClass('ge-layout-desktop') ){
                        curColClassIndex = 0;
                        return;
                    }
                    if( canvas.hasClass('ge-layout-tablet') ){
                        curColClassIndex = 1;
                        return;
                    }
                    if( canvas.hasClass('ge-layout-phone') ){
                        curColClassIndex = 2;
                        return;
                    }
                }
                // TODO: sortable je v povodnom stave! ...pri znovuspusteni skontrolovat celu funkcionalitu!
                // function makeSortable() {
                //     canvas.find('#pageContent').sortable({
                //         items: 'section',
                //         // connectWith: '.ge-canvas section',
                //         handle: '> .ge-tools-drawer .ge-move',
                //         start: sortStart,
                //         helper: 'clone',
                //     });
                //     canvas.find('section').sortable({
                //         items: '> .container',
                //         connectWith: '.ge-canvas .section',
                //         handle: '> .ge-tools-drawer .ge-move',
                //         start: sortStart,
                //         helper: 'clone',
                //     });
                //     canvas.find('.container').sortable({
                //         items: '.row',
                //         connectWith: '.ge-canvas .container',
                //         handle: '> .ge-tools-drawer .ge-move',
                //         start: sortStart,
                //         helper: 'clone',
                //     });
                //     canvas.find('.row').sortable({
                //         items: '> .column',
                //         connectWith: '.ge-canvas .row',
                //         handle: '> .ge-tools-drawer .ge-move',
                //         start: sortStart,
                //         helper: 'clone',
                //     });
                //     canvas.add(canvas.find('.column')).sortable({
                //         items: '> .row, > .ge-content',
                //         connectsWith: '.ge-canvas .container, .ge-canvas .column',
                //         handle: '> .ge-tools-drawer .ge-move',
                //         start: sortStart,
                //         helper: 'clone',
                //     });
                //
                //     function sortStart(e, ui) {
                //         ui.placeholder.css({ height: ui.item.outerHeight()});
                //     }
                // }

                // function removeSortable() {
                //     canvas.add(canvas.find('.column.ui-sortable')).add(canvas.find('.row.ui-sortable')).add(canvas.find('.column.ui-sortable')).add(canvas.find('section.ui-sortable'));
                //     canvas.each(function(){
                //         $(this).sortable('destroy');
                //     });
                // }

                function createSection() {
                    return $('<section/>');
                }
                function createContainer() {
                    return $('<div class="container"/>');
                }
                function createRow() {
                    return $('<div class="row" />');
                }

                function createColumn(size) {
                    var content = createColumnContent();
                    var column = $('<div class="'+colClasses.map(function(c) { return c + size; }).join(' ')+'"></div>');
                    column.append(content);
                    return column;
                }
                function createColumnContent(){
                    return $('<p>&nbsp;Lorem ipsum</p>')
                }
                function isFirst(element){
                    var elemType='section';
                    if( element.hasClass('container') ){ elemType=".container"; }
                    if( element.hasClass('row') ){ elemType=".row"; }
                    if( element.hasClass('column') ){ elemType=".column"; }

                    return element.parent().find(elemType).first().is(element);
                }
                function isLast(element){
                    var elemType='section';
                    if( element.hasClass('container') ){ elemType=".container"; }
                    if( element.hasClass('row') ){ elemType=".row"; }
                    if( element.hasClass('column') ){ elemType=".column"; }

                    return element.parent().find(elemType).last().is(element);
                }
                function isColNarrowest(column){
                    var curColClass = colClasses[curColClassIndex];
                    var col_size = getColSize(column,curColClass);

                    return (col_size==MIN_COL_SIZE);
                }
                function isColWidest(column){
                    var curColClass = colClasses[curColClassIndex];
                    var col_size = getColSize(column,curColClass);

                    return (col_size==MAX_COL_SIZE);
                }
                function isEmpty(element){
                    var el = element.clone();
                    el.find('.ge-tools-drawer').remove();
                    el.find('.ge-tools-drawerPlusColumn').remove();
                    el.find('.ge-tools-drawerPlusSection').remove();
                    var content = el.find('> .ge-content');
                    if(content.length>0){
                        return (content.html()=="" || content.html()=="<br>" || content.html()=="<br/>");
                    }
                    return (el.html()=="");
                }

                function resetColumnTools(){
                    if(canvas.hasClass('ge-editing')){
                        canvas.find('.column').each(function() {
                            resetElementTools($(this));
                        });
                    }
                }

                // funkcia na pridavanie horneho paddingu na section - sluzil na to, aby tam bol vzdy priestor pre tools. Po JBI grafickych upravach zbytocne.
                <%--function createSectionEditablePadding(){--%>
                    <%--canvas.find('section').each(function(){--%>
                        <%--if( $(this).css('padding-top').replace('px','')<=60 ){--%>
                            <%--console.log('createSectionEditablePadding');--%>
                            <%--$(this).addClass('sectionEditablePaddingAdded');--%>
                        <%--}--%>
                    <%--});--%>
                <%--}--%>
                <%--function removeSectionEditablePadding(){--%>
                    <%--canvas.find('section').each(function(){--%>
                        <%--if($(this).hasClass('sectionEditablePaddingAdded')) {--%>
                            <%--$(this).removeClass('sectionEditablePaddingAdded');--%>
                        <%--}--%>
                    <%--});--%>
                <%--}--%>

                <%--/**--%>
                 <%--* json podla ktoreho sa vytvoria objekty v selectGroup--%>
                 <%--**/--%>
                <%--var jsonObjectSelectGroup = getJsonObjectSelectGroup();--%>
                <%--function getJsonObjectSelectGroup(){--%>
                    <%--&lt;%&ndash;var jsonObjectSelectGroup = JSON.parse(JSON.stringify(<%@include file="grideditor_add_section_source_JSONARRAY_ajax.json"%>));&ndash;%&gt;--%>
                    <%--jsonObjectSelectGroup = JSON.parse('[]');--%>
                    <%--$.getJSON( "/service/grideditor/categories", function(data) {--%>
                        <%--jsonObjectSelectGroup = data;--%>
                    <%--}).fail(function( jqxhr, textStatus, error ) {--%>
                        <%--console.log( "Request Failed: " + textStatus + ", " + error );--%>
                    <%--});--%>
                <%--}--%>
                <%--/**--%>
                 <%--* vytvorenie/vratenie elementu selectGroup, ktory sa zobrazi po kliku na PLUS medzi sekciami.--%>
                 <%--**/--%>
                <%--var selectGroup;--%>
                <%--function getSelectGroup(){--%>
                    <%--if( typeof(selectGroup) == "undefined"){--%>

                        <%--selectGroup = $('<div class="wj-ge-selectGroup"></div>');--%>
                        <%--// TODO: sem pridat tools cez funkciu createTool--%>
                        <%--var selectGroup_close = $('<div class="wj-ge-selectGroup-close wj-ge-tools-colored wj-ge-tools-red"></div>').appendTo(selectGroup);--%>

                        <%--var container = $('<div class="container-fluid" style="padding: 50px 15px 15px;"></div>').appendTo(selectGroup);--%>
                        <%--var content = $('<div class="wj-ge-content" style="text-align:center;"></div>').appendTo(container);--%>

                        <%--createTool(selectGroup_close, '<iwcm:text key="grideditor.section.close" />', "", "fa-minus", function () {--%>
                            <%--hideSelectGroup($(this));--%>
                        <%--});--%>

                        <%--// obalovac pre kategorie--%>
                        <%--var categories = $('<div class="wj-ge-categories"></div>')--%>
                            <%--.appendTo(content);--%>
                        <%--// obalovac pre vsetky bloky kategorii (vsetyk tab-contenty)--%>
                        <%--var category_groups = $('<div class="wj-ge-category-groups"></div>')--%>
                            <%--.appendTo(content);--%>

                        <%--for (var i=0; i < jsonObjectSelectGroup.length; i++) {--%>
                            <%--var category = $('<div class="wj-ge-category">'+jsonObjectSelectGroup[i].textKey+'</div>')--%>
                                <%--.attr('data-id',jsonObjectSelectGroup[i].id)--%>
                                <%--.appendTo(categories);--%>
                            <%--var category_group = $('<div class="wj-ge-category-group"></div>')--%>
                                <%--.attr('data-group-id',jsonObjectSelectGroup[i].id)--%>
                                <%--.appendTo(category_groups);--%>
                            <%--if(i>0){--%>
                                <%--category_group.css('display','none');--%>
                            <%--}else{--%>
                                <%--category.addClass('active');--%>
                            <%--}--%>
                            <%--for(var j=0; j< jsonObjectSelectGroup[i].groups.length;j++){--%>
                                <%--category_group.append( getGroupObjectForSelectGroup(jsonObjectSelectGroup[i].groups[j],jsonObjectSelectGroup[i].id) );--%>
                            <%--}--%>
                        <%--}--%>
                        <%--bindFunctionSelectGroupCategory(categories,category_groups);--%>
                        <%--bindFunctionSelectGroupGroup(category_groups);--%>
                    <%--}--%>
                    <%--return selectGroup;--%>
                <%--}--%>
                <%--function getGroupObjectForSelectGroup(thisJsonGroup, categoryId){--%>
                    <%--var group = $('<a href="javascript:void(0);" class="wj-ge-group"></a>')--%>
                        <%--.attr('data-group-id',thisJsonGroup.id)--%>
                        <%--.attr('data-category-id',categoryId)--%>
                        <%--.append($('<img alt="group">')--%>
                            <%--.attr('src',thisJsonGroup.imagePath)--%>
                        <%--)--%>
                        <%--.append('<p>'+thisJsonGroup.textKey+'</p>');--%>

                    <%--if( thisJsonGroup.action != null){--%>
                        <%--group.attr('data-action',thisJsonGroup.action);--%>

                        <%--if(thisJsonGroup.premium==true){--%>
                            <%--group.addClass('wj-ge-premium');--%>
                        <%--}--%>
                        <%--switch(thisJsonGroup.action){--%>
                            <%--case "block":--%>
                                <%--group.attr('data-block-id',thisJsonGroup.blockId);--%>
                                <%--break;--%>
                            <%--case "script":--%>
                                <%--group.attr('data-script-id',thisJsonGroup.blockId);--%>
                                <%--break;--%>
                            <%--default:--%>
                                <%--console.log('unknown action of group. Action called:'+thisJsonGroup.action);--%>
                        <%--}--%>
                    <%--}--%>
                    <%--return group;--%>
                <%--}--%>
                <%--function bindFunctionSelectGroupCategory(categories,category_groups){--%>
                    <%--categories.find('.wj-ge-category').on('click',function(){--%>
                        <%--categories.find('.wj-ge-category').removeClass('active');--%>
                        <%--$(this).addClass('active');--%>
                        <%--category_groups.find('.wj-ge-category-group').hide();--%>
                        <%--category_groups.find('.wj-ge-category-group[data-group-id="'+$(this).data('id')+'"]').show();--%>
                    <%--});--%>
                <%--}--%>
                <%--function bindFunctionSelectGroupGroup(category_groups){--%>
                    <%--category_groups.find('.wj-ge-group').not('.wj-ge-more-group').on('click',function(){--%>
                        <%--var $this = $(this);--%>

                        <%--// ak group nema inu akciu, zobraz sidebar s vyberom bloku--%>
                        <%--if( !$this.is('[data-action]') ) {--%>
                            <%--var groupId = $this.data('group-id'),--%>
                                <%--categoryId = $this.data('category-id');--%>

                            <%--$.post("/service/grideditor/category", {categoryDirId : categoryId }, function(data, textStatus) {--%>
                                <%--createSideBar(data, groupId);--%>
                            <%--}, "json");--%>
                            <%--hideSelectGroup(false);--%>
                        <%--}else {--%>
                            <%--// group ma inu akciu - napr. je to block--%>
                            <%--switch( $this.data('action') ) {--%>
                                <%--case 'script':--%>
                                    <%--// fallthrough--%>
                                <%--case 'block':--%>
                                    <%--//  akcia ako block--%>
                                    <%--var categoryId = $this.data('category-id'),--%>
                                        <%--blockId = $this.data('group-id'),--%>
                                        <%--content= '';--%>

                                    <%--for (var i=0; i < jsonObjectSelectGroup.length; i++) {--%>
                                        <%--if(jsonObjectSelectGroup[i].id==categoryId){--%>
                                            <%--for(var j=0;j<jsonObjectSelectGroup[i].groups.length;j++){--%>
                                                <%--if(jsonObjectSelectGroup[i].groups[j].id==blockId){--%>
                                                    <%--content = jsonObjectSelectGroup[i].groups[j].content;--%>
                                                    <%--if( $this.data('action') == 'block' ) {--%>
                                                        <%--if (jsonObjectSelectGroup[i].groups[j].premium == true) {--%>
                                                            <%--insertPremiumBlock(blockId);--%>
                                                        <%--} else {--%>
                                                            <%--insertBlock(content);--%>
                                                        <%--}--%>
                                                    <%--}else if( $this.data('action') == 'script' ) {--%>
                                                        <%--eval(content);--%>
                                                    <%--}--%>
                                                    <%--break;--%>
                                                <%--}--%>
                                            <%--}--%>
                                            <%--break;--%>
                                        <%--}--%>
                                    <%--}--%>
                                    <%--hideSelectGroup();--%>
                                    <%--break;--%>
                                <%--default:--%>
                                    <%--alert( "nerozpoznana akcia: " +$this.data('action'));--%>
                            <%--}--%>
                        <%--}--%>
                    <%--});--%>
                <%--}--%>
                <%--function showSelectGroup($this){--%>

                    <%--if($this === 'undefined'){--%>
                        <%--$this = $('.ge-tools-drawerPlusSection');--%>
                    <%--}--%>

                    <%--// zatvor selectGroup ak je otvoreny inde--%>
                    <%--//hideSelectGroup();--%>

                    <%--selectGroup.addClass('active');--%>
                    <%--&lt;%&ndash;$this.parent().addClass('isOpened').find('i').removeClass('fa-plus').addClass('fa-minus');&ndash;%&gt;--%>
                    <%--$this.parent().addClass('isOpened');--%>

                    <%--var section =  $this.parents('section');--%>

                    <%--if(!section.is('section')){--%>
                        <%--// ak user klikol na uplne prve plusko -> prerabam na plusko TOP a BOTTOM, takze toto asi nikdy nezbehne--%>
                        <%--var firstSection = canvas.find('section')[0];--%>
                        <%--selectGroup.insertBefore(firstSection).fadeIn(500);--%>
                    <%--}else{--%>
                        <%--// vsetky ostatne pluska--%>
                        <%--if($this.parent().hasClass('ge-tools-drawerPlusSection-top')){--%>
                            <%--selectGroup.insertBefore(section).fadeIn(500);--%>
                        <%--}else {--%>
                            <%--selectGroup.insertAfter(section).fadeIn(500);--%>
                        <%--}--%>
                    <%--}--%>
                <%--}--%>
                <%--function hideSelectGroup(detach){--%>
                    <%--selectGroup.removeClass('active')--%>
                        <%--.fadeOut(500);--%>

                    <%--// v pripade ze vkladam blok, tak si chcem zapamatat poziciu.--%>
                    <%--if(typeof (detach) == 'undefined' ) detach = true;--%>
                    <%--if(detach) selectGroup.delay(500).detach();--%>

                    <%--&lt;%&ndash;canvas.find('.ge-tools-drawerPlusSection').removeClass('isOpened').find('i').removeClass('fa-minus').addClass('fa-plus');&ndash;%&gt;--%>
                    <%--canvas.find('.ge-tools-drawerPlusSection').removeClass('isOpened');--%>
                <%--}--%>
                <%--function showHideSelectGroup($this) {--%>
                    <%--// $this - button plusSection--%>

                    <%--var isOpened = selectGroup.hasClass('active');--%>
                    <%--&lt;%&ndash;var isFirstPlusSection = $this.parent().hasClass('ge-tools-dPS-canvas');&ndash;%&gt;--%>
                    <%--var minusClicked = $this.find('i.fa-minus').length;--%>
                    <%--var plusClicked = $this.find('i.fa-plus').length;--%>

                    <%--if(isOpened && minusClicked){--%>
                        <%--//iba zatvor--%>
                        <%--hideSelectGroup();--%>
                        <%--return;--%>
                    <%--}--%>
                    <%--if(isOpened && plusClicked){--%>
                        <%--//zatvor a otvor kde mas--%>
                        <%--hideSelectGroup(false);--%>
                        <%--showSelectGroup($this);--%>
                        <%--return;--%>
                    <%--}--%>
                    <%--if (!isOpened){--%>
                        <%--//otvor--%>
                        <%--showSelectGroup($this);--%>
                    <%--}--%>
                <%--}--%>
                <%--function createSideBar(jsonObjectSideBar, clickedGroupId){--%>
                    <%--if( typeof(sideBar) == "undefined" ) {--%>
                        <%--sideBar = $('<div class="wj-ge-sideBar" style="display:none;"></div>')--%>
                            <%--.appendTo(canvas);--%>
                    <%--}--%>
                    <%--sideBar.empty();--%>

                    <%--// pridaj overlay--%>
                    <%--sideBar.append(--%>
                        <%--$('<div class="wj-ge-sideBar-overlay"></div>')--%>
                        <%--.click(function(){--%>
                            <%--hideSideBar();--%>
                        <%--})--%>
                    <%--);--%>

                    <%--if(jsonObjectSideBar == null){--%>
                        <%--return sideBar;--%>
                    <%--}--%>
                    <%--var sideBarPanel = $('<div class="wj-ge-sideBarPanel"></div>')--%>
                        <%--.appendTo(sideBar);--%>

                    <%--&lt;%&ndash;var jsonObjectSideBar = JSON.parse(JSON.stringify(<%@include file="grideditor_sidebar_source_ajax.json"%>));&ndash;%&gt;--%>

                    <%--//inicializuj sidebar--%>
                    <%--var content = $('<div class="wj-ge-content"></div>')--%>
                        <%--.append($('<span class="close-sidebar"><i class="fa fa-times" aria-hidden="true"></i></span>')--%>
                            <%--.click(function(){--%>
                                <%--hideSideBar();--%>
                            <%--})--%>
                        <%--)--%>
                        <%--.append($('<h3 style="color:#fff;">'+jsonObjectSideBar.textKey+'</h3>')--%>
                            <%--.append(content)--%>
                        <%--)--%>
                        <%--.appendTo(sideBarPanel);--%>
                    <%--var select = $('<select class="form-control group-select"></select>')--%>
                        <%--.on('change',function(){--%>
                            <%--sideBarPanel.find('.wj-ge-groupBlocks').hide();--%>
                            <%--sideBarPanel.find('.wj-ge-groupBlocks[data-id="'+$(this).val()+'"]').show();--%>
                        <%--})--%>
                        <%--.appendTo(content);--%>
                    <%--var groupBlocksWrapper = $('<div class="wj-ge-groupBlocks-wrapper"></div>')--%>
                        <%--.appendTo(content);--%>
                    <%--for (var i = 0; i<jsonObjectSideBar.groups.length;i++){--%>
                        <%--var group = jsonObjectSideBar.groups[i];--%>
                        <%--var option = $('<option value='+group.id+'>'+group.textKey+'</option>');--%>
                        <%--var isSelected = false;--%>
                        <%--if(clickedGroupId == group.id) {--%>
                            <%--isSelected = true;--%>
                            <%--option.attr('selected','selected');--%>
                        <%--}--%>
                        <%--select.append( option );--%>
                        <%--var groupBlocks = $('<div class="wj-ge-groupBlocks"></div>')--%>
                            <%--.attr('data-id',group.id)--%>
                            <%--.appendTo(groupBlocksWrapper);--%>
                        <%--if( !isSelected ){ groupBlocks.hide(); }--%>
                        <%--for ( var j = 0; j<group.blocks.length; j++){--%>
                            <%--var blockObject = group.blocks[j];--%>
                            <%--var block = $('<div class="wj-ge-block"></div>')--%>
                                <%--.attr('data-id',blockObject.id)--%>
                                <%--.attr('data-group-id',group.id)--%>
                                <%--.append( $('<img src="'+blockObject.imagePath+'" alt="'+blockObject.textKey+'"/>') )--%>
                                <%--.append( $('<div class="wj-ge-block-name">'+blockObject.textKey+'</div>') )--%>
                                <%--.appendTo(groupBlocks);--%>

                            <%--if(blockObject.premium==true){--%>
                                <%--block.addClass('wj-ge-premium');--%>
                            <%--}--%>
                        <%--}--%>
                    <%--}--%>
                    <%--bindFunctionSideBarBlocks(sideBarPanel, jsonObjectSideBar.groups);--%>
                    <%--showSideBar();--%>
                    <%--return sideBar;--%>
                <%--}--%>
                <%--function bindFunctionSideBarBlocks(sideBarPanel,groups){--%>
                    <%--sideBarPanel.find('.wj-ge-groupBlocks .wj-ge-block').on('click',function(){--%>

                        <%--var $this = $(this),--%>
                            <%--groupId = $this.data('group-id'),--%>
                            <%--blockId = $this.data('id'),--%>
                            <%--blockHtml= '';--%>

                        <%--for (var i=0; i < groups.length; i++) {--%>
                            <%--if(groups[i].id==groupId){--%>
                                <%--for(var j=0;j<groups[i].blocks.length;j++){--%>
                                    <%--if(groups[i].blocks[j].id==blockId){--%>
                                        <%--if(groups[i].blocks[j].premium != 'undefined' && groups[i].blocks[j].premium==true){--%>
                                            <%--insertPremiumBlock(blockId);--%>
                                        <%--} else {--%>
                                            <%--blockHtml = groups[i].blocks[j].content;--%>
                                            <%--insertBlock(blockHtml);--%>
                                        <%--}--%>
                                        <%--break;--%>
                                    <%--}--%>
                                <%--}--%>
                                <%--break;--%>
                            <%--}--%>
                        <%--}--%>
                        <%--hideSideBar();--%>
                    <%--});--%>
                <%--}--%>
                function insertPremiumBlock(id){
                    alert('Prepacte, vkladanie premiovych blokov nie je momentalne dovolene.');
                }
                function insertBlock(data){

                    if(typeof (data) == 'undefined' ){
                        console.log('html data error.');
                        return;
                    }

                    // TODO:  $(data) bolo v plugin.js obalene ako CKEDITOR.dom.element.createFromHtml(data)
                    var elementInclude = $(data);

                    elementInclude.insertAfter( selectGroup );

                    // ak je v includeElement !INCLUDE, musime odstranit selectGroup a deinit, aby nam ckeditor nezobral tools a dalsie k sebe.
                    selectGroup.detach();

                    if ( typeof (elementInclude.html() ) == 'undefined' ) {
                        alert('<iwcm:text key="grideditor.section.add.error" />');
                        return;
                    }
                    if (elementInclude.html().indexOf("!INCLUDE")!=-1) {
                        deinit();

                        var scrollTop = 0;
                        try {
                            scrollTop = document.getElementById(ckEditorInstance.id + "_contents").getElementsByTagName('iframe')[0].contentWindow.scrollY
                        } catch (e) { console.log(e);  }

                        //toto musime spravit aby sme korektne inicializovali data, widgety a podobne
                        ckEditorInstance.setData(ckEditorInstance.getData());
                    }
                    // TODO: treba resetEditedElement ked hned za tym je init?
                    resetEditedElement(elementInclude);
                    init();

                    if (scrollTop > 0) {
                        setTimeout(function () {
                            try {
                                //console.log("Setting scroll to: "+scrollTop);
                                document.getElementById(ckEditorInstance.id + "_contents").getElementsByTagName('iframe')[0].contentWindow.scrollBy(0, scrollTop);
                            } catch (e) {
                                console.log(e);
                            }
                        }, 500);
                    }
                }

                <%--function showSideBar(){--%>
                    <%--&lt;%&ndash;sideBar.addClass('show');&ndash;%&gt;--%>
                    <%--sideBar.fadeIn(500);--%>
                <%--}--%>
                <%--function hideSideBar(){--%>
                    <%--&lt;%&ndash;sideBar.removeClass('show');&ndash;%&gt;--%>
                    <%--sideBar.fadeOut(500);--%>
                <%--}--%>

                /**
                 * Run custom content filter on init and deinit
                 */
                function runFilter(isInit) {
                    if (settings.custom_filter.length) {
                        $.each(settings.custom_filter, function(key, func) {
                            if (typeof func == 'string') {
                                func = window[func];
                            }

                            func(canvas, isInit);
                        });
                    }
                }

                /**
                 * Wrap column content in <div class="ge-content"> where neccesary
                 */
                function wrapContent() {
                    canvas.find('.column').each(function() {
                        var col = $(this);
                        var contents = $();
                        col.children().each(function() {
                            var child = $(this);
                            if (child.is('.row, .ge-tools-drawer, .ge-content,.ge-tools-drawerPlusColumn, .ge-tools-drawerPlusSection') ) {
                                doWrap(contents);
                            } else {
                                contents = contents.add(child);
                            }
                        });
                        doWrap(contents);
                    });
                }

                function doWrap(contents) {
                    if (contents.length) {
                        var container = createDefaultContentWrapper().insertAfter(contents.last());
                        contents.appendTo(container);
                    }
                }

                function createDefaultContentWrapper() {
                    return $('<div/>')
                        .addClass('ge-content');
                }

                function switchLayout(colClassIndex) {
                    curColClassIndex = colClassIndex;

                    var layoutClasses = ['ge-layout-desktop', 'ge-layout-tablet', 'ge-layout-phone'];
                    layoutClasses.forEach(function(cssClass, i) {
                        canvas.toggleClass(cssClass, i == colClassIndex);
                    });
                    <%--resetColumnTools();--%>
                }
                function getBootstrapColClasses(version){
                    version = version+"".split('.')[0];
                    switch(version){
                        case 4:
                            return ['col-lg-', 'col-md-', 'col-sm-'];
                        case 3:
                            // FALLTHROUGH
                        default:
                            return ['col-md-', 'col-sm-', 'col-xs-'];
                    }
                }

                function animateElementIn(el){
                    $([document.documentElement, document.body]).animate({
                        scrollTop: el.offset().top - 200
                    }, 300);

                    var animateInClass= "ge-animateIn";

                    // po scrolle zacni animovat
                    setTimeout(function(){
                        el.addClass(animateInClass);
                    },300);

                    // po skonceni scroll zober classu nech nemame balast.
                    setTimeout(function(){
                        el.removeClass(animateInClass);
                        <%--el.stop().animate( {transform:'scaleY(0.6)'}, {duration:300});--%>
                        <%--el.stop().animate( {transform:'scaleY(1.2)'}, {duration:300});--%>
                        <%--el.stop().animate( {transform:'scaleY(1)'}, {duration:300});--%>
                    },1000);
                }

                function clamp(input, min, max) {
                    return Math.min(max, Math.max(min, input));
                }

                baseElem.data('grideditor', {
                    init: init,
                    deinit: deinit,
                    resetColumnTools: resetColumnTools,
                    switchLayout : switchLayout
                });

            });

            return self;

        };

        <%--$.fn.gridEditor.RTEs = {};--%>

    })( jQuery );