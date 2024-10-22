/*
<%@ page pageEncoding="utf-8" contentType="text/javascript" %>
<%@ page import="sk.iway.iwcm.i18n.Prop"%>
<%@ page import="sk.iway.iwcm.PageLng"%>
<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
	sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/admin/webpages/page-builder/scripts/ninja-page-builder.js", null, request, response);
%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" />
*/

    ;(function ($, window, document, undefined) {

    var pluginName = 'ninjaPageBuilder';

    function Plugin (element, options) {
        this.element = element;
        this._name = pluginName;

        this.options = $.extend( {}, this.get_defaults(), options );
        this.generate_default_options();

        this.init();
        window.pageBuilder = this;
    }

    $.extend(Plugin.prototype, {

        /*==================================================================
        /*====================|> INIT NINJA PAGE BUILDER
        /*=================================================================*/

        init: function () {
            this.build_cache();
            this.set_settings();
            this.setTemplateData();
            this.mark_grid_elements();
            this.create_modal();
            this.create_notify();
            // <%--// this.create_library();--%>
            this.bind_events();
        },

        /*==================================================================
        /*====================|> GET CLEAR DATA
        /*=================================================================*/

        getClearNode: function() {

            var clone = $(this.$wrapper).clone(true);

            clone = this.remove_elements(clone);
            clone = this.remove_attributes(clone);

            var prefix = this.options.prefix;

            var node = $(clone);

            // <%--//odstran prazdne atributy--%>
            var removeAttrs = ["data-"+prefix+"-selector-class", "data-"+prefix+"-selector-id", "id"];
            for (var i=0; i<removeAttrs.length; i++)
            {
                var attrName = removeAttrs[i];
                node.find("*["+attrName+"=\"\"]").removeAttr(attrName);
            }

            return node;

        },

        /*==================================================================
        /*====================|> Remove CK Editor classes and attributes
        /*=================================================================*/

        clearEditorAttributes: function(node) {
            // <%--//musime odstranit vsetky CKeditor atributy a CSS triedy--%>
            node.find("*[class*='editableElement']").removeAttr("contenteditable data-ckeditor-instance tabindex spellcheck role aria-label");
            node.find("*[class*='editableElement']").attr('style', function(i, style)
            {
                // <%--//ckeditor vklada position:relative do elementu--%>
                return style && style.replace(/position[^;]+;?/g, '').trim();
            });
            node.find("*[class*='editableElement']").removeClass("editableElement cke_editable cke_editable_inline cke_contents_ltr cke_show_borders cke_focus");

            var htmlCode = node.html();
            if (htmlCode.indexOf("\""+this.options.prefix+"-")!=-1) {
                //odstran zabudnute PB elementy, ak je napr. vnoreny section do col a podobne nezmysly

                //console.log("htmlCode1=", htmlCode, "me=", this);
                var me = this;

                node.find("aside."+me.tag.toolbar).remove();
                node.find("aside."+me.tag.append).remove();
                node.find("aside."+me.tag.prepend).remove();
                node.find("aside."+me.tag._highlighter).remove();
                node.find("aside."+me.tag.dimmer).remove();
                node.find("aside."+me.tag.empty_placeholder).remove();
                node.find("aside."+me.tag.connection_button).remove();

                node.find("."+me.tag._grid_element)
                    .removeClass(me.tag.section)
                    .removeClass(me.tag.container)
                    .removeClass(me.tag.row)
                    .removeClass(me.tag.column)
                    //.removeClass(me.tag.editable_section)
                    //.removeClass(me.tag.editable_container)
                    .removeClass(me.tag.editable_element)
                    .removeClass(me.tag._grid_element);

                for (const key in me.state) {
                    //console.log(key);
                    let value = me.state[key];
                    if (typeof value === "string") {
                        //console.log("Removing class: "+value);
                        node.find("section."+value+",div."+value)
                            .removeClass(value);
                    }
                }

                //htmlCode = $html.html();
            }

            // <%--//odstran prazdne style atributy--%>
            node.find("*[style=\"\"]").removeAttr("style");

            // <%--//odstran prazdne class atributy--%>
            node.find("*[class=\"\"]").removeAttr("class");
        },

        /*==================================================================
        /*====================|> DESTROY NINJA PAGE BUILDER
        /*=================================================================*/

        destroy: function() {
            this.unbind_events();
            this.remove_elements();
            this.remove_attributes();
            this.$wrapper.removeData();
        },

        /*==================================================================
        /*====================|> SET ALL SETTINGS
        /*=================================================================*/

        set_settings: function() {

            var me = this,
                prefix = me.options.prefix+'-';

            me.tag = {
                wrapper:                        prefix+'wrapper',

                _grid_element:                  prefix+'grid-element',
                section:                        prefix+'section',
                container:                      prefix+'container',
                row:                            prefix+'row',
                column:                         prefix+'column',
                column_content:                 prefix+'column__content',

                editable_section:               'pb-section',
                editable_container:             'pb-container',
                editable_content:                prefix+'content',
                editable_element:                prefix+'editable',
                not_editable_element:            prefix+'not-editable',

                section_style_prefix:           'pb-style-section',
                container_style_prefix:         'pb-style-container',
                column_style_prefix:            'pb-style-column',

                _highlighter:                   prefix+'highlighter',
                highlighter_top:                prefix+'highlighter__top',
                highlighter_bottom:             prefix+'highlighter__bottom',
                highlighter_left:               prefix+'highlighter__left',
                highlighter_right:              prefix+'highlighter__right',

                dimmer:                         prefix+'dimmer',

                size_changer:                   prefix+'size-changer',
                size_changer_up:                prefix+'size-changer__up',
                size_changer_down:              prefix+'size-changer__down',
                size_changer_number:            prefix+'size-changer__number',

                toolbar:                        prefix+'toolbar',
                toolbar_content:                prefix+'toolbar__content',

                _toolbar_button:                prefix+'toolbar-button',
                toolbar_button_style:           prefix+'toolbar-button__style',
                toolbar_button_remove:          prefix+'toolbar-button__remove',
                toolbar_button_resize:          prefix+'toolbar-button__resize',
                toolbar_button_move:            prefix+'toolbar-button__move',
                toolbar_button_add_to_favorites:prefix+'toolbar-button__add_to_favorites',
                toolbar_button_duplicate:       prefix+'toolbar-button__duplicate',

                _plus_button:                   prefix+'plus-button',
                append:                         prefix+'append',
                prepend:                        prefix+'prepend',

                connection_button:              prefix+'connection-button',
                connection_reference:           prefix+'connection-reference',

                empty_placeholder:              prefix+'empty-placeholder',
                empty_placeholder_button:       prefix+'empty-placeholder__button',

                modal:                          prefix+'modal',
                modal_header:                   prefix+'modal__header',
                modal_header_button_show:       prefix+'modal__header__button-show',
                modal_content:                  prefix+'modal__content',
                modal_footer:                   prefix+'modal__footer',
                modal_footer_button_close:      prefix+'modal__footer__button-close',
                modal_footer_button_save:       prefix+'modal__footer__button-save',
                modal_footer_button_reset:      prefix+'modal__footer__button-reset',

                modal_zindex:                   prefix+'modal--z-index',

                style_input_group:              prefix+'style-input-group',

                style_input_group_four_in_row:                        prefix+'style-input-group-four-in-row',
                style_input_group_four_in_row_tools:                  prefix+'style-input-group-four-in-row-tools',
                style_input_group_four_in_row_checkbox:               prefix+'style-input-group-four-in-row-checkbox',
                style_input_group_four_in_row_checkbox_all:           prefix+'style-input-group-four-in-row-all',
                style_input_group_four_in_row_checkbox_first_second:  prefix+'style-input-group-four-in-row-first-second',
                style_input_group_four_in_row_checkbox_third_fourth:  prefix+'style-input-group-four-in-row-third-fourth',
                style_input_group_four_in_row_first:                  prefix+'style-input-group-four-in-row-first',
                style_input_group_four_in_row_second:                 prefix+'style-input-group-four-in-row-second',
                style_input_group_four_in_row_third:                  prefix+'style-input-group-four-in-row-third',
                style_input_group_four_in_row_fourth:                 prefix+'style-input-group-four-in-row-fourth',

                style_input_group_headline:     prefix+'style-input-group-headline',
                style_input_group_content:      prefix+'style-input-group-content',
                style_input_wrapper:            prefix+'style-input-wrapper',
                style_input:                    prefix+'style-input',
                style_label:                    prefix+'style-label',

                notify:                         prefix+'notify',
                notify_header:                  prefix+'notify__header',
                notify_content:                 prefix+'notify__content',
                notify_footer:                  prefix+'notify__footer',
                notify_footer_button:           prefix+'notify__footer__button',

                library:                        prefix+'library',

                library_column:                 prefix+'library--column',
                library_container:              prefix+'library--container',
                library_section:                prefix+'library--section',

                library_header:                 prefix+'library__header',
                library_header_title:           prefix+'library__header__title',
                library_content:                prefix+'library__content',
                library_footer:                 prefix+'library__footer',
                library_footer_button:          prefix+'library__footer__button',

                library_favorites:              prefix+'library__favorites'
            };

            me.tags = {
                section:                        me.tag.section                        +' '+ me.tag._grid_element,
                container:                      me.tag.container                      +' '+ me.tag._grid_element,
                row:                            me.tag.row                            +' '+ me.tag._grid_element,
                column:                         me.tag.column                         +' '+ me.tag._grid_element,

                append:                         me.tag.append                         +' '+ me.tag._plus_button,
                prepend:                        me.tag.prepend                        +' '+ me.tag._plus_button,

                toolbar_button_style:           me.tag.toolbar_button_style           +' '+ me.tag._toolbar_button,
                toolbar_button_remove:          me.tag.toolbar_button_remove          +' '+ me.tag._toolbar_button,
                toolbar_button_resize:          me.tag.toolbar_button_resize          +' '+ me.tag._toolbar_button,
                toolbar_button_move:            me.tag.toolbar_button_move            +' '+ me.tag._toolbar_button,
                toolbar_button_add_to_favorites:me.tag.toolbar_button_add_to_favorites+' '+ me.tag._toolbar_button,
                toolbar_button_duplicate:       me.tag.toolbar_button_duplicate       +' '+ me.tag._toolbar_button,

                highlighter_top:                me.tag.highlighter_top                +' '+ me.tag._highlighter,
                highlighter_bottom:             me.tag.highlighter_bottom             +' '+ me.tag._highlighter,
                highlighter_left:               me.tag.highlighter_left               +' '+ me.tag._highlighter,
                highlighter_right:              me.tag.highlighter_right              +' '+ me.tag._highlighter
            };

            // <%-- TAG Classes --%>
            me.tagc = {};

            $.each(me.tag, function(key, val) {
                me.tagc[key]= '.'+val;
            });

            me.state = {
                is_styling_column:              prefix+'is-styling-column',
                is_styling_container:           prefix+'is-styling-container',
                is_styling_section:             prefix+'is-styling-section',
                is_styling:                     prefix+'is-styling',
                has_same_style:                 prefix+'has-same-style',
                is_style_connected:             prefix+'is-style-connected',

                is_notify_active:               prefix+'is-notify-active',

                is_library_active:              prefix+'is-library-active',

                is_toolbar_active:              prefix+'is-toolbar-active',
                has_toolbar_active:             prefix+'has-toolbar-active',
                has_child_toolbar_active:       prefix+'has-child-toolbar-active',

                is_resize_columns:              prefix+'is-resize-columns',
                is_special_helper:              prefix+'is-special-helper',

                is_duplicating:                 prefix+'is-duplicating',
                is_moving:                      prefix+'is-moving',
                is_moving_child:                prefix+'is-moving-child',
                is_sibling_left:                prefix+'is-sibling-left',
                is_sibling_right:               prefix+'is-sibling-right',
                is_moving_type:                 function(type){
                    var new_type = type.replace(prefix, '');
                    return me.state.is_moving_child + ' ' + me.state.is_moving + '-' + new_type;
                },

                is_modal_open:                  prefix+'is-modal-open',
                is_blinking:                    prefix+"is-blinking"
            };

            me.statec = {};

            $.each(me.state, function(key, val) {
                if(key === 'is_moving_type') {
                    return;
                }
                me.statec[key]= '.'+val;
            });

            me.grid = me.options.grid || {
                section:                        'section:not(.pb-not-section)',
                container:                      'div[class^="container"]:not(.pb-not-container), div[class*="pb-custom-container"]',
                row:                            'div.row',
                column:                         'div[class*="col-"]:not(.pb-not-column), div[class*="pb-col"]',
                column_content:                 'div.column-content'
            };

            me.column = {
                min_size:                       1,
                max_size:                       me.options.max_col_size,
                valid_prefixes:                 ['col-', 'col-sm-', 'col-md-', 'col-lg-', 'col-xl-', 'pb-col-'],
                valid_classes:                  [],
                attr_prefix:                    'data-'+prefix
            };

            $.each(me.column.valid_prefixes, function(index, class_name) {
                for (var i = me.column.min_size; i < me.column.max_size+1; i++) {
                    me.column.valid_classes.push(class_name+i);
                }
            });
            //pridaj aj pb-col a pb-col-auto
            me.column.valid_classes.push("pb-col");
            me.column.valid_classes.push("pb-col-auto");
            me.column.valid_prefixes.push("pb-col");
            //console.log("me.column.valid_classes=", me.column.valid_classes);
            //console.log("valid_prefixes=", me.column.valid_prefixes);

            me.user_style = {
                counter: function(){
                    return Date.now() + '' + Math.floor(1000 + Math.random() * 9000);
                },
                unique_selector: function () {
                    return prefix + 'user-style-' + me.user_style.counter();
                },
                attr_name: 'data-' + prefix + 'user-style-id',
                properties: [
                    'background-color',
                    'background-image',

                    'margin-top',
                    'margin-bottom',
                    'margin-right',
                    'margin-left',

                    'padding-top',
                    'padding-right',
                    'padding-bottom',
                    'padding-left',

                    'border-style',
                    'border-color',
                    'border-top-width',
                    'border-right-width',
                    'border-bottom-width',
                    'border-left-width',

                    'border-top-left-radius',
                    'border-top-right-radius',
                    'border-bottom-left-radius',
                    'border-bottom-right-radius',

                    'box-shadow',

                    'z-index',

                    'text-align',

                    'selector-class',
                    'selector-id',

                    'visibility-xs',
                    'visibility-sm',
                    'visibility-md',
                    'visibility-lg',
                    'visibility-xl',

                    //standardne atributy
                    'attr-title'

                    /*,

                    'width',
                    'min-width',
                    'max-width',
                    'height',
                    'min-height',
                    'max-height'
                    */
                ],
                px_properties: [
                    'margin-top',
                    'margin-bottom',
                    'margin-right',
                    'margin-left',

                    'padding-top',
                    'padding-right',
                    'padding-bottom',
                    'padding-left',

                    'border-top-width',
                    'border-right-width',
                    'border-bottom-width',
                    'border-left-width',

                    'border-top-left-radius',
                    'border-top-right-radius',
                    'border-bottom-left-radius',
                    'border-bottom-right-radius'
                ],
                current_element: null,
                current_element_old_style: []
            };

            me.template = [];
            me.template.basic =[
                                {
                                    "id": "1",
                                    "textKey": "column",
                                    "groups": [
                                        // <%--{ "id": "id1.1",    "textKey": '<span class="pb-col-1">1</span>',        'content': '<div class="col-md-1 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.2",    "textKey": '<span class="pb-col-2">2</span>',        'content': '<div class="col-md-2 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.3",    'textKey': '<span class="pb-col-3">3</span>',        'content': '<div class="col-md-3 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.4",    'textKey': '<span class="pb-col-4">4</span>',        'content': '<div class="col-md-4 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.5",    'textKey': '<span class="pb-col-5">5</span>',        'content': '<div class="col-md-5 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.6",    'textKey': '<span class="pb-col-6">6</span>',        'content': '<div class="col-md-6 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.7",    'textKey': '<span class="pb-col-7">7</span>',        'content': '<div class="col-md-7 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.8",    'textKey': '<span class="pb-col-8">8</span>',        'content': '<div class="col-md-8 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.9",    'textKey': '<span class="pb-col-9">9</span>',        'content': '<div class="col-md-9 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.10",   'textKey': '<span class="pb-col-10">10</span>',      'content': '<div class="col-md-10 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.11",   'textKey': '<span class="pb-col-11">11</span>',      'content': '<div class="col-md-11 '+me.state.is_special_helper+'"></div>'},--%>
                                        // <%--{ "id": "id1.12",   'textKey': '<span class="pb-col-12">12</span>',      'content': '<div class="col-md-12 '+me.state.is_special_helper+'"></div>'}--%>
                                    ]
                                },
                                {
                                    "id": "2",
                                    "textKey": "container",
                                    "groups": [
                                        // <%--{"id": "id2.1",     'textKey': '<span class="pb-col-12">12</span>',                                                                                                                                                          'content': '<div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size+'"></div></div></div>'},--%>
                                        // <%--{"id": "id2.2",     'textKey': '<span class="pb-col-6">6</span><span class="pb-col-6">6</span>',                                                                                                                             'content': '<div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/2+'"></div><div class="col-md-'+ me.options.max_col_size/2+'"></div></div></div>'},--%>
                                        // <%--{"id": "id2.3",     'textKey': '<span class="pb-col-4">4</span><span class="pb-col-4">4</span><span class="pb-col-4">4</span>',                                                                                              'content': '<div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/3+'"></div><div class="col-md-'+ me.options.max_col_size/3+'"></div><div class="col-md-'+ me.options.max_col_size/3+'"></div></div></div>'},--%>
                                        // <%--{"id": "id2.4",     'textKey': '<span class="pb-col-3">3</span><span class="pb-col-3">3</span><span class="pb-col-3">3</span><span class="pb-col-3">3</span>',                                                               'content': '<div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/4+'"></div><div class="col-md-'+ me.options.max_col_size/4+'"></div><div class="col-md-'+ me.options.max_col_size/4+'"></div><div class="col-md-'+ me.options.max_col_size/4+'"></div></div></div>'},--%>
                                        // <%--{"id": "id2.5",     'textKey': '<span class="pb-col-2">2</span><span class="pb-col-2">2</span><span class="pb-col-2">2</span><span class="pb-col-2">2</span><span class="pb-col-2">2</span><span class="pb-col-2">2</span>', 'content': '<div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div></div></div>'},--%>
                                        // <%--{"id": "id2.6",     'textKey': '<span class="pb-col-4">4</span><span class="pb-col-8">8</span>',                                                                                                                             'content': '<div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/3+'"></div><div class="col-md-'+ (me.options.max_col_size/3)*2+'"></div></div>'},--%>
                                        // <%--{"id": "id2.7",     'textKey': '<span class="pb-col-8">8</span><span class="pb-col-4">4</span>',                                                                                                                             'content': '<div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ (me.options.max_col_size/3)*2+'"></div><div class="col-md-'+ me.options.max_col_size/3+'"></div></div>'},--%>
                                        // <%--{"id": "id2.8",     'textKey': '<span class="pb-col-3">3</span><span class="pb-col-6">6</span><span class="pb-col-3">3</span>',                                                                                              'content': '<div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/4+'"></div><div class="col-md-'+ (me.options.max_col_size/4)*2+'"></div><div class="col-md-'+ me.options.max_col_size/4+'"></div></div></div>'}--%>
                                    ]
                                },
                                {
                                    "id": "3",
                                    "textKey": "section",
                                    "groups": [
                                        // <%--{"id": "id3.1",     'textKey': '<span class="pb-col-12">12</span>',                                                                                                                                                          'content': '<section><div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size+'"></div></div></div></section>'},--%>
                                        // <%--{"id": "id3.2",     'textKey': '<span class="pb-col-6">6</span><span class="pb-col-6">6</span>',                                                                                                                             'content': '<section><div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/2+'"></div><div class="col-md-'+ me.options.max_col_size/2+'"></div></div></div></section>'},--%>
                                        // <%--{"id": "id3.3",     'textKey': '<span class="pb-col-4">4</span><span class="pb-col-4">4</span><span class="pb-col-4">4</span>',                                                                                              'content': '<section><div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/3+'"></div><div class="col-md-'+ me.options.max_col_size/3+'"></div><div class="col-md-'+ me.options.max_col_size/3+'"></div></div></div></section>'},--%>
                                        // <%--{"id": "id3.4",     'textKey': '<span class="pb-col-3">3</span><span class="pb-col-3">3</span><span class="pb-col-3">3</span><span class="pb-col-3">3</span>',                                                               'content': '<section><div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/4+'"></div><div class="col-md-'+ me.options.max_col_size/4+'"></div><div class="col-md-'+ me.options.max_col_size/4+'"></div><div class="col-md-'+ me.options.max_col_size/4+'"></div></div></div></section>'},--%>
                                        // <%--{"id": "id3.5",     'textKey': '<span class="pb-col-2">2</span><span class="pb-col-2">2</span><span class="pb-col-2">2</span><span class="pb-col-2">2</span><span class="pb-col-2">2</span><span class="pb-col-2">2</span>', 'content': '<section><div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div><div class="col-md-'+ me.options.max_col_size/6+'"></div></div></div></section>'},--%>
                                        // <%--{"id": "id3.6",     'textKey': '<span class="pb-col-4">4</span><span class="pb-col-8">8</span>',                                                                                                                             'content': '<section><div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/3+'"></div><div class="col-md-'+ (me.options.max_col_size/3)*2+'"></div></div></section>'},--%>
                                        // <%--{"id": "id3.7",     'textKey': '<span class="pb-col-8">8</span><span class="pb-col-4">4</span>',                                                                                                                             'content': '<section><div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ (me.options.max_col_size/3)*2+'"></div><div class="col-md-'+ me.options.max_col_size/3+'"></div></div></section>'},--%>
                                        // <%--{"id": "id3.8",     'textKey': '<span class="pb-col-3">3</span><span class="pb-col-6">6</span><span class="pb-col-3">3</span>',                                                                                              'content': '<section><div class="container"><div class="row '+me.state.is_special_helper+'"><div class="col-md-'+ me.options.max_col_size/4+'"></div><div class="col-md-'+ (me.options.max_col_size/4)*2+'"></div><div class="col-md-'+ me.options.max_col_size/4+'"></div></div></div></section>'}--%>
                                    ]
                                }
                            ];
            me.template.library = [];
            me.template.favorite = [];

        },

        setTemplateData: function(){
            var me = this;
            me.libraryDataFilled = false;
            me.favoriteDataFilled = false;

            me.load_data_library_basic();
            me.load_data_library_predefined();
            me.load_data_favorite();
        },
        create_library_after_getData: function(onlyUpdate){
            var me = this;

            onlyUpdate = (typeof onlyUpdate =='undefined') ? false : onlyUpdate;

            if( onlyUpdate ){
                me.update_library_content();
            } else if( me.libraryDataFilled && me.favoriteDataFilled )  {
                me.create_library();
            }
        },
        load_data_library_basic: function(){
            var me = this,
                ret = [];

            $.each(['column','container','section'],function(i,v){
                var groups;

                if(v == 'column') {
                    var cols = [];
                    for (var i = me.options.max_col_size; i > 0 ; i--) {
                        cols.push(i);
                    }
                    groups = getCols(i,cols);
                }
                else if(v == 'container') groups = getContainers(i,me.options.template_basic_containers_sizes);
                else if(v == 'section') groups = getSections(i,me.options.template_basic_containers_sizes);

                ret.push({
                    id: i,
                    textKey: v,
                    groups: groups
                });
            });
            me.template.basic = ret;

            // <%-- nested functions--%>
            function getCols(parentId,options){
                var ret = [];
                $.each(options, function(i,v){
                    ret.push(getCol(parentId,i,v));
                });
                return ret;
            }
            function getCol(parentId,id,v){
                var pb_col_size = (v/me.options.max_col_size)*12;
                return {
                    id: "id"+parentId+"."+id,
                    textKey: '<span class="'+me.options.prefix+'-col-'+pb_col_size+'">'+v+'</span>',
                    content: '<div class="col-md-'+v+' '+me.state.is_special_helper+'">Text</div>'
                }
            }
            function getContainers(parentId,options){
                var ret = [];
                $.each(options, function(i,v){
                    ret.push(getContainer(parentId,i,v));
                });
                return ret;
            }
            function getContainer(parentId,id,vals)
            {
                // <%--//div.container alebo div.containerInner podla konfiguracie--%>
                var containerClass = me.grid.container;
                var dot = containerClass.indexOf(".");
                if (dot!=-1) containerClass = containerClass.substring(dot+1);
                if (containerClass.indexOf(" ")!=-1) containerClass = "container";

                var textKey = '',
                    content = '<div class="'+containerClass+'"><div class="row '+me.state.is_special_helper+'">';
                $.each(vals,function(i,v){
                    var pb_col_size = (v/me.options.max_col_size)*12;
                    textKey += '<span class="'+me.options.prefix+'-col-'+pb_col_size+'">'+v+'</span>';
                });
                $.each(vals,function(i,v){ content += '<div class="col-md-'+v+'">Text</div>'; });
                content += '</div></div>';

                return {
                    id: "id"+parentId+"."+id,
                    textKey: textKey,
                    content: content
                }
            }
            function getSections(parentId,options){
                var containers = getContainers(parentId,options);
                $.each(containers,function(i,v){
                    v.content = '<section>'+v.content+'</section>';
                });
                return containers;
            }
        },
        load_data_library_predefined: function(){
            var me = this;

            $.getJSON( "/admin/grideditor/templates/library", {templateGroupId: me.options.template_group_id}, function(data) {
                me.template.library = data;
                me.libraryDataFilled = true;

                me.create_library_after_getData();

            }).fail(function( jqxhr, textStatus, error ) {
                console.error( "Request TEMPLATES LIBRARY Failed: " + textStatus + ", " + error );
            });
        },
        load_data_favorite: function(onlyUpdate){
            var me = this;

            $.getJSON( "/admin/grideditor/templates/favorite", {templateGroupId: me.options.template_group_id}, function(data) {
                me.template.favorite=data;
                me.favoriteDataFilled = true;

                me.create_library_after_getData(onlyUpdate);

            }).fail(function( jqxhr, textStatus, error ) {
                console.error( "Request TEMPLATES FAVORITES Failed: " + textStatus + ", " + error );
            });
        },


        /*==================================================================
        /*====================|> BUILD CACHE
        /*=================================================================*/

        build_cache: function () {
            this.$wrapper = $(this.element);
        },

        /*==================================================================
        /*====================|> MARK ALL GRID ELEMENTS
        /*=================================================================*/

        mark_grid_elements: function () {
            $(this.$wrapper).addClass(this.tag.wrapper);

            this.mark_sections(this.$wrapper);
            // <%--// this.mark_editable_elements(this.$wrapper);--%>
        },

        /*==================================================================
        /*====================|> MARK ALL EDITABLE ELEMENTS IN WRAPPER
        /*=================================================================*/
        // <%--// mark_editable_elements: function(wrapper){--%>
        // <%--//     var me = this;--%>
        // <%--//     var editable_elements = $(wrapper).find(me.tagc.editable_element);--%>
        // <%--//--%>
        // <%--//     editable_elements.each(function(index,el){--%>
        // <%--//         me.mark_editable_element(el);--%>
        // <%--//     });--%>
        // <%--// },--%>
        // <%--//--%>
        // <%--// mark_editable_element: function(){--%>
        // <%--//     // zapracovane na urovni inline.js.jsp -> inicializacia CK editora. Hladaj podla class pb-editable--%>
        // <%--//--%>
        // <%--// },--%>

        /*==================================================================
        /*====================|> MARK ALL SECTIONS IN WRAPPER
        /*=================================================================*/

        mark_sections: function (wrapper) {
            var me = this;
            var sections = $(wrapper).children(me.grid.section);

            if(sections.length==0){
                me.create_empty_placeholder(wrapper);
            }else {
                sections.each(function(index,section){
                    me.mark_section(section);
                });
            }
        },

        mark_section: function (section) {
            if($(section).hasClass(this.tag.not_editable_element)){
                return;
            }
            if($(section).hasClass("pb-not-section")){
                return;
            }

            $(section).addClass(this.tags.section);
            if( $(section).hasClass(this.tag.editable_section) ) this.create_controllers(section);
            this.mark_containers(section);
        },

        /*==================================================================
        /*====================|> MARK ALL CONTAINERS IN SECTION
        /*=================================================================*/

        mark_containers: function (section) {
            var me = this;
            // <%--//console.log("mark_containers, section: ",section, $(section).find(me.grid.container))--%>
            $(section).find(me.grid.container).each(function(index,container){
                me.mark_container(container);
            });
        },

        mark_container: function (container) {
            if($(container).hasClass(this.tag.not_editable_element)){
                return;
            }
            if($(container).hasClass("pb-not-container")){
                return;
            }

            $(container).addClass(this.tags.container);
            if( $(container).hasClass(this.tag.editable_container) ) this.create_controllers(container);
            this.mark_rows(container);
        },

        /*==================================================================
        /*====================|> MARK ALL ROWS IN CONTAINER
        /*=================================================================*/

        mark_rows: function (container) {
            var me = this;
            $(container).find(me.grid.row).each(function(index,row){
                me.mark_row(row);
            });
        },

        mark_row: function (row) {
            if($(row).hasClass(this.tag.not_editable_element)){
                return;
            }

            $(row).addClass(this.tags.row);
            this.create_empty_placeholder(row);
            this.mark_columns(row);
        },

        /*==================================================================
        /*====================|> MARK ALL COLUMNS IN ROW
        /*=================================================================*/

        mark_columns: function (row) {
            var me = this;
            // <%--//console.log("---- marking columns, row=", row, $(row).children(me.grid.column));--%>
            $(row).find(me.grid.column).each(function(index,column){
                me.mark_column(column);
            });
        },

        mark_column: function (column) {
            if($(column).hasClass(this.tag.not_editable_element) && $(column).hasClass("pb-always-mark")==false){
                return;
            }
            if($(column).hasClass("pb-not-column")){
                return;
            }

            var me = this,
                column_sizes = me.get_column_sizes(column);

            if (column_sizes.length < 1) {
                return;
            }


            $(column).addClass(me.tags.column);

            if ($(column).hasClass(this.tag.not_editable_element)==false) {
                me.wrap_column_content(column);
                me.clear_column_content(column);
            }
            me.set_column_sizes(column);
            me.create_controllers(column);
            me.create_column_size_changer(column);
        },

        wrap_column_content: function(column) {
            var me = this;
            if($(column).children(me.grid.column_content).length < 1) {
                // <%--console.log("wrapInner", $(column).html());--%>
                // <%--//jeeff: v col- kontajneri mozem mat len cisty text, wrapInner to pokazi, musim cisty text wrapnut do divu--%>
                var html = $(column).html();
                if (html.indexOf("<")==-1)
                {
                    $(column).html("<p>"+html+"</p>");
                }

                $(column).wrapInner('<div class="column-content '+me.tag.column_content+' '+me.tag.editable_content+'"></div>');
            } else {
                // <%--console.log("wrapChildren", column);--%>
                $(column).children(me.grid.column_content).addClass(me.tag.column_content).addClass(me.tag.editable_content);
            }
        },

        clear_column_content: function(column) {
            var me = this;
            //toto robilo problem, ked v column-content bol len text (napr. !INCLUDE aplikacie)
            /*if($(column).children(me.tagc.column_content).children().length < 1) {
                $(column).children(me.tagc.column_content).html('');
            }*/
        },

        set_column_sizes: function (column) {

            var me = this,
                column_sizes = me.get_column_sizes(column);

            $.each(column_sizes, function(index, size) {
                $(column).attr(me.column.attr_prefix + size.prefix, size.size);
            });

            $.each(me.column.valid_prefixes, function(index, class_name) {
                if (!$(column).attr(me.column.attr_prefix+class_name)) {
                    if(index===0) {
                        $(column).attr(me.column.attr_prefix+class_name, me.column.max_size);
                    } else {
                        var new_size = $(column).attr(me.column.attr_prefix+me.column.valid_prefixes[index-1]);
                        $(column).attr(me.column.attr_prefix+class_name, new_size);
                    }
                }
            });

        },

        /*==================================================================
        /*====================|> GET COLUMN ALL SIZES
        /*=================================================================*/

        get_column_sizes: function(column) {

            var me = this,
                result = [],
                column_classes = $(column).attr('class').split(/\s+/);

            $.each(me.column.valid_classes, function(index, valid_class_name) {

                $.each(column_classes, function(index, column_class_name) {

                    if(valid_class_name === column_class_name) {

                        var column_size = parseInt(column_class_name.match(/\d+/)),
                            column_prefix = column_class_name.replace(column_size, '');

                        result.push({
                            prefix: column_prefix,
                            class:  column_class_name,
                            size:   column_size
                        });

                    }

                });

            });

            return result;
        },

        /*==================================================================
        /*====================|> ROZTRIEDIT !!!
        /*=================================================================*/
        get_style_element_string: function(el){
            var me = this,
                prev_user_style_current_element = me.user_style.current_element,
                ret = "";

            me.user_style.current_element = $(el);
            var style_tag_id = me.get_current_element_style_id();
            me.user_style.current_element = prev_user_style_current_element;

            var style_el = $('style[style-id="'+style_tag_id+'"]');

            if(style_el.length >0){
                ret = style_el.clone().wrap('<span></span>').parent().html();
            }
            return ret;
        },
        add_to_favorites: function(el) {
            var parent = this.get_parent_grid_element(el);
            var me = this;


            // ---  deinit -------------------
            var special_selector_class=me.options.prefix+"-special-selector-return-this-element";
            $(parent).addClass(special_selector_class);

            var clone = me.getClearNode();
            me.clearEditorAttributes(clone);

            var el_to_be_saved = $(clone).find('.'+special_selector_class);

            $(parent).removeClass(special_selector_class);
            el_to_be_saved.removeClass(special_selector_class);


            // ---  html ---------------------
            var content_to_sent = el_to_be_saved.wrap('<div></div>').parent().html();


            // ---  style --------------------
            var style_to_sent = "";
            // <%--// skontroluje vsetky deti, ci nemaju style element--%>
            $(parent).find(me.tagc._grid_element).each(function(){
                var elem = $(this),
                    this_style = me.get_style_element_string(elem);

                if(this_style != "") {
                    if(style_to_sent != "") style_to_sent +="\n\n";
                    style_to_sent +=this_style;
                }
            });
            // <%--// pridaj aj svoj style element--%>
            if(style_to_sent != "") style_to_sent +="\n\n";
            style_to_sent += me.get_style_element_string(parent);

            // ---  type  ---------------------
            var type = "";
            var default_name = "<iwcm:text key='pagebuilder.favorites.name.element'/>";
            if($(parent).hasClass(me.tags.section)) {
                default_name = "<iwcm:text key='pagebuilder.favorites.name.section'/>";
                type = "section";
            }else if($(parent).hasClass(me.tags.container)) {
                default_name = "<iwcm:text key='pagebuilder.favorites.name.container'/>";
                type = "container";
            }else if($(parent).hasClass(me.tags.column)) {
                default_name = "<iwcm:text key='pagebuilder.favorites.name.column'/>";
                type = "column";
            }


            // --- send ---------------------
            var name = prompt("<iwcm:text key='pagebuilder.alert.favorites.confirm.name'/>", default_name);
            if( name != null ) {
                $.post("/admin/grideditor/save/element", {
                    html: content_to_sent,
                    name: name,
                    style: style_to_sent,
                    type: type,
                    templateGroupId: me.options.template_group_id
                }, function (data, textStatus) {

                    // <%--// console.log('toto su prichadzajuce data:');--%>
                    // <%--// console.log(data);--%>

                    if(data.result){
                        me.load_data_favorite(true);
                        alert("<iwcm:text key='pagebuilder.alert.favorites.saved'/>"+data.fileName);
                    } else {
                        alert("<iwcm:text key='pagebuilder.alert.favorites.save.failed'/>");
                        console.error(type+' element not saved!',data);
                    }
                }, "json")
                    .fail(function (response) {
                        console.error(response);
                    });
            }
        },

        /*==================================================================
        /*====================|> FIND CLOSEST GRID ELEMENT
        /*=================================================================*/

        get_parent_grid_element: function(el) {
            var me = this;

            var grid_element = $(el).closest(this.tagc._grid_element);

            if($(el).hasClass(this.tag._grid_element)) {
                grid_element = $(el).parent().closest(this.tagc._grid_element);
            }

            // <%--// pre plusko vo wrapper (ak wrapper nema section a chceme tam nejaku pridat)--%>
            if($(el).parent().not(me.grid.section).parent().hasClass(this.tag.wrapper)){
                grid_element = $(el).parent().parent();
            }

            if(grid_element.length < 1) {
                grid_element = null;
            }

            return grid_element;
        },

        /*==================================================================
        /*====================|> CREATE CONTROLLERS
        /*=================================================================*/

        create_controllers: function (el) {
            this.create_toolbar(el);
            this.create_plus_button(el);
            this.create_conntection_button(el);
            this.create_highlighter(el);
            this.create_empty_placeholder(el);
            this.create_dimmer(el)
        },

        create_toolbar: function (el) {
            var $el = $(el);
            if($el.children(this.tagc.toolbar).length < 1) {

                var buttons = this.build_button(this.tags.toolbar_button_style);

                if($el.hasClass(this.tag.column)) {
                    if ($el.hasClass("pb-col") || $el.hasClass("pb-col-auto")) {
                        //ak to mama classu pb-col alebo pb-col-auto nezobrazime nastavenie velkosti
                    } else {
                        buttons += this.build_button(this.tags.toolbar_button_resize);
                    }
                }

                buttons += this.build_button(this.tags.toolbar_button_move);
                buttons += this.build_button(this.tags.toolbar_button_duplicate);
                buttons += this.build_button(this.tags.toolbar_button_add_to_favorites);
                buttons += this.build_button(this.tags.toolbar_button_remove);

                var content = this.build_aside(this.tag.toolbar_content,buttons);

                $(el).append(this.build_aside(this.tag.toolbar,content));
            }
        },

        create_plus_button: function (el) {
            if($(el).children(this.tagc._plus_button).length < 1) {
                $(el)
                    .append(this.build_aside(this.tags.append))
                    .append(this.build_aside(this.tags.prepend));
            }
        },

        create_conntection_button: function (el) {
            if($(el).children(this.tagc.connection_button).length < 1) {
                $(el).append(this.build_aside(this.tag.connection_button));
            }
        },

        create_highlighter: function (el) {
            if($(el).children(this.tagc._highlighter).length < 1) {
                $(el)
                    .append(this.build_aside(this.tags.highlighter_top))
                    .append(this.build_aside(this.tags.highlighter_bottom))
                    .append(this.build_aside(this.tags.highlighter_left))
                    .append(this.build_aside(this.tags.highlighter_right));
            }
        },

        create_column_size_changer: function (el) {
            if($(el).children(this.tagc.size_changer).length < 1) {
                var content  = this.build_button(this.tag.size_changer_down);
                content += this.build_button(this.tag.size_changer_number,this.get_actual_column_size(el));
                content += this.build_button(this.tag.size_changer_up);
                $(el).append(this.build_aside(this.tag.size_changer,content));
            }
        },

        create_empty_placeholder: function(el){
            var me = this;
            if($(el).children(this.tagc.empty_placeholder).length < 1) {
                var content = this.build_button(this.tag.empty_placeholder_button);

                if( $(el).children().not('aside[class^="'+this.options.prefix+'"]').length < 1 ||
                    $(el).children(this.tagc.column_content).is(':empty')
                ) {
                    if($(el).hasClass(this.tag.container)) {
                        $(this.make_new_row()).prependTo($(el));
                    } else {
                        if($(el).hasClass(me.tag.row) || $(el).hasClass(me.tag.section)) {
                            $(el).append(this.build_aside(this.tag.empty_placeholder, content));
                        }
                    }
                }else if( $(el).hasClass(this.tag.wrapper) && $(el).children(this.tagc.section).length <1 ){
                    $(el).append(this.build_aside(this.tag.empty_placeholder,content));
                }
            }
        },

        create_dimmer: function(el){
            $(el).append(this.build_aside(this.tag.dimmer));
        },

        /*==================================================================
        /*====================|> BUILD ELEMENT
        /*=================================================================*/

        build_button: function (class_name,content) {
            if(typeof content === 'undefined') {
                content = '';
            }
            return '<span class="'+class_name+'">'+content+'</span>';
        },

        build_aside: function (class_name,content) {
            if(typeof content === 'undefined') {
                content = '';
            }
            return '<aside class="'+class_name+'">'+content+'</aside>';
        },

        /*==================================================================
        /*====================|> SIZE CHANGERS
        /*=================================================================*/

        get_actual_screen_size:function () {
            var colPrefix = 'col-xl-';

            var screenSize =  $(window).width();

            //console.log("get_actual_screen_size, screenSize=", screenSize, "element=", this.element);

            // <%--//https://getbootstrap.com/docs/4.0/layout/grid/--%>

            // <%--/*--%>
            // <%--if (screenSize < 576) colPrefix = "col-";--%>
            // <%--else if (screenSize < 768) colPrefix = "col-sm-";--%>
            // <%--else if (screenSize < 992) colPrefix = "col-md-";--%>
            // <%--else if (screenSize < 1200) colPrefix = "col-lg-";--%>
            // <%--*/--%>
            // <%--//standardne pouzivame len col- a col-md            --%>
            if (screenSize < 768) colPrefix = "col-";
            else if (screenSize < 1200) colPrefix = "col-md-";

            // <%--console.log("get_actual_screen_size: ", screenSize, "colPrefix: ", colPrefix);--%>

            //console.log("Returning: colPrefix=", colPrefix);

            return colPrefix;
        },

        get_actual_column_size: function(column) {
            var size = parseInt( $(column).attr(this.column.attr_prefix+this.get_actual_screen_size()) );
            //console.log("size=", size);
            //nema zadanu velkost pre dany breakpoint, nacitaj default bez prefixu
            if (Number.isNaN(size)) size = parseInt( $(column).attr(this.column.attr_prefix+"col-") );
            //console.log("size=", size)
            return size;
        },

        change_column_size_up: function (el) {
            var size_up = 1;

            if(this.shift_key_down) {
                size_up = this.options.max_col_size;
            }
            this.change_column_size(el,size_up);
        },

        change_column_size_down: function (el) {
            var size_down = -1;

            if(this.shift_key_down) {
                size_down = -this.options.max_col_size;
            }
            this.change_column_size(el,size_down);
        },

        change_column_size: function (el,size) {

            var column = this.get_parent_grid_element($(el)),
                actual_size = this.get_actual_column_size(column),
                new_size = actual_size + (size);

            //console.log("change_column_size, column=", column, "new_size=", new_size);

            if(new_size > this.options.max_col_size) {
                new_size = this.options.max_col_size;
            }

            if(new_size < 1) {
                new_size = 1;
            }

            $(column)
                .attr(this.column.attr_prefix+this.get_actual_screen_size(),new_size)
                .removeClass(this.get_actual_screen_size() + actual_size)
                .addClass(this.get_actual_screen_size() + new_size);

            $(column).find(this.tagc.size_changer_number).html(new_size);
        },

        listen_for_shift_key: function(e) {
            var event = e || window.event;

            this.shift_key_down = false;

            if (event.shiftKey) {
                this.shift_key_down = true;
            }
        },

        listen_for_esc_key: function(e) {
            var event = e || window.event;

            this.esc_key_down = false;

            if (event.key === "Escape") {
                this.esc_key_down = true;
            }
        },

        /*==================================================================
        /*====================|> ALLOW RESIZE COLUMN
        /*=================================================================*/

        allow_resize_columns: function (el) {
            var container = $(el).closest(this.tagc.container);
            $(container).addClass(this.state.is_resize_columns);
            this.update_notify_content("<iwcm:text key='pagebuilder.column.resize'/>",'');

            //aktualizuj cisla
            //console.log("element:", $(container).find(this.tagc.size_changer).find(this.tagc.size_changer_number));
            var me = this;
            $(container).find(this.tagc.size_changer).find(this.tagc.size_changer_number).each(function(index) {
                //console.log("update size, this=", this);
                me.change_column_size($(this), 0);
                //sizeEl.html(this.get_actual_column_size(el));
            })
        },

        /*==================================================================
        /*====================|> CANCEL RESIZE COLUMN
        /*=================================================================*/

        cancel_resize_columns: function () {
            $(this.tagc._grid_element).removeClass(this.state.is_resize_columns);
            this.set_toolbar_invisible();
        },

        /*==================================================================
        /*====================|> TOGGLE TOOLBAR VISIBILITY
        /*=================================================================*/

        toggle_toolbar_visibility: function (el) {
            if($(this.$wrapper).hasClass(this.state.has_child_toolbar_active)) {
                this.set_toolbar_invisible();
            } else {
                this.set_toolbar_visible(el);
            }
        },

        set_toolbar_invisible: function () {
            $(this.$wrapper).removeClass(this.state.has_child_toolbar_active);
            $(this.tagc.toolbar).removeClass(this.state.is_toolbar_active);
            $(this.tagc._grid_element)
                .removeClass(this.state.has_child_toolbar_active)
                .removeClass(this.state.has_toolbar_active);

            //jeeff: zakomentovane, lebo to schova sameho seba this.hide_notify();
        },

        set_toolbar_visible: function (el) {
            var parent = this.get_parent_grid_element(el);

            if($(el).hasClass(this.tag._grid_element)){
                parent = $(el);
                el = $(el).children(this.tagc.toolbar);
            }

            var parents = $(parent).parents(this.tagc._grid_element);

            $(el).addClass(this.state.is_toolbar_active);
            $(parent).addClass(this.state.has_toolbar_active);
            $(parents).addClass(this.state.has_child_toolbar_active);
            $(this.$wrapper).addClass(this.state.has_child_toolbar_active);
        },

        /*==================================================================
        /*====================|> REMOVE GRID ELEMENT
        /*=================================================================*/

        remove_grid_element: function (el) {

            if(!confirm("<iwcm:text key='pagebuilder.delete.confirm'/>")){
                return;
            }

            var grid_element = this.get_parent_grid_element(el),
                parent = this.get_parent_grid_element(grid_element);

            var style_id = grid_element.attr(this.user_style.attr_name);
            if( $(this.$wrapper).find(this.tagc._grid_element+'[data-pb-user-style-id='+style_id+']').length < 2 ) $('style[style-id="'+style_id+'"]').remove();

            $(grid_element).off().unbind().remove();

            this.set_toolbar_invisible();
            this.create_empty_placeholder($(parent));

            this.options.onElementRemoved(grid_element, parent);

            let pbElement = $(this.element);
            if (pbElement.find("section").length==0) {
                //zobraz tlacidlo na pridanie tabu
                if (pbElement.find("."+this.tag.empty_placeholder).length==0) {
                    pbElement.prepend(this.build_aside(this.tag.empty_placeholder, this.build_button(this.tag.empty_placeholder_button)));
                }
            }
        },

        /*==================================================================
        /*====================|> MOVE GRID ELEMENT
        /*=================================================================*/

        allow_move_grid_element: function (el) {
            var grid_element = this.get_parent_grid_element(el);

            $(grid_element).addClass(this.state.is_moving);
            $(grid_element).prev(this.tagc._grid_element).addClass(this.state.is_sibling_left);
            $(grid_element).next(this.tagc._grid_element).addClass(this.state.is_sibling_right);

            this.set_toolbar_invisible();

            if(this.duplicate) {
                $(this.$wrapper).addClass(this.state.is_duplicating);
                this.update_notify_content("<iwcm:text key='pagebuilder.notify_content.duplicate'/>",'');
            } else {
                this.update_notify_content("<iwcm:text key='pagebuilder.notify_content.move'/>",'');
            }

            if($(grid_element).hasClass(this.tag.column)) {
                $(this.$wrapper).addClass(this.state.is_moving_type(this.tag.column));
            }
            else if($(grid_element).hasClass(this.tag.row)) {
                $(this.$wrapper).addClass(this.state.is_moving_type(this.tag.row));
            }
            else if($(grid_element).hasClass(this.tag.container)) {
                $(this.$wrapper).addClass(this.state.is_moving_type(this.tag.container));
            }
            else if($(grid_element).hasClass(this.tag.section)) {
                $(this.$wrapper).addClass(this.state.is_moving_type(this.tag.section));
            }

        },

        move_grid_element_here: function (el) {

            var grid_element = this.get_parent_grid_element($(el)),
                moving = $(this.tagc._grid_element+this.statec.is_moving),
                clone = moving.clone().addClass(this.state.is_special_helper),
                moving_parent = this.get_parent_grid_element(moving);

            if(!this.duplicate) {
                $(moving).unbind().off().remove();
            }

            if ($(el).hasClass(this.tag.append)){
                $(clone).insertAfter(grid_element);
            }
            else if($(el).hasClass(this.tag.prepend)){
                $(clone).insertBefore(grid_element);
            }
            else {
                $(grid_element).children(this.tagc.empty_placeholder).remove();
                $(clone).prependTo(grid_element);
            }

            if(this.duplicate) {
                this.duplicatedElement = $(this.statec.is_special_helper);
                //remove binded editors
                this.duplicatedElement.find("*[class*='editableElement']").removeAttr("data-ckeditor-instance");
                this.duplicatedElement.find("*[class*='editableElement']").removeClass("editableElement cke_editable cke_editable_inline cke_contents_ltr cke_show_borders");

                this.options.onElementDuplicated();
                this.changedElement = $(this.statec.is_special_helper);
                this.options.onGridChanged();
            } else {
                this.movedElement = $(this.statec.is_special_helper);
                this.options.onElementMoved();
                this.changedElement = $(this.statec.is_special_helper);
                this.options.onGridChanged();
            }

            this.duplicate_user_style_element();
            this.set_toolbar_visible($(this.statec.is_special_helper));
            this.create_empty_placeholder(moving_parent);
            this.check_if_parent_is_empty_row(moving_parent);
            this.cancel_move_grid_element();
            this.hide_notify();


        },

        duplicate_user_style_element: function () {

            var el = $(this.statec.is_special_helper);

            if(typeof $(el).attr(this.user_style.attr_name) === 'undefined') {
                return;
            }

            var style_id = $(el).attr(this.user_style.attr_name),
                style_element = $('style[style-id="'+style_id+'"]').clone(),
                new_style_id = this.user_style.unique_selector(),
                new_html = $(style_element).html().replace(style_id,new_style_id);

            $(style_element).attr('style-id',new_style_id);
            $(style_element).html(new_html);
            $(style_element).appendTo(this.$wrapper);

            $(el).attr(this.user_style.attr_name,new_style_id);

        },

        check_if_parent_is_empty_row: function (moving_parent) {
            if( $(moving_parent).hasClass(this.tag.row) && $(moving_parent).children(this.tagc.empty_placeholder).length > 0 ) {
                var section = this.get_parent_grid_element(moving_parent);
                $(moving_parent).remove();
                this.create_empty_placeholder(section);
            }
        },

        cancel_move_grid_element: function () {
            this.removeClassStartingWith($(this.$wrapper),this.state.is_moving);
            $(this.$wrapper).removeClass(this.state.is_duplicating);

            $(this.tagc._grid_element)
                .removeClass(this.state.is_special_helper)
                .removeClass(this.state.is_moving)
                .removeClass(this.state.is_sibling_left)
                .removeClass(this.state.is_sibling_right)
                .removeClass("som-hover-append")
                .removeClass("som-hover-prepend");
        },

        removeClassStartingWith: function (el,filter) {
            $(el).removeClass(function (index, name) {
                return (name.match(new RegExp("\\S*" + filter + "\\S*", 'g')) || []).join(' ');
            });
        },


        /*==================================================================
        /*====================|> ADD GRID ELEMENT
        /*=================================================================*/

        add_new_grid_element: function (el) {

            var me = this,
                parent = me.get_parent_grid_element($(el)),
                empty = $(el).hasClass(me.tag.empty_placeholder_button),
                content,
                new_element;

            if(!me.shift_key_down) {
                if($(parent).hasClass(me.tag.column) && empty) {
                    return;
                }
                me.show_library_tab(el);
                return;
            }

            if($(parent).hasClass(me.tag.column)) {
                if(empty) {
                    return;
                } else {
                    content = me.make_new_column(parent);
                    new_element = me.get_insert_new_element(el, content, parent);
                    me.mark_column($(new_element));
                }
            }
            else if($(parent).hasClass(me.tag.row)) {

                if(empty) {
                    content = me.make_new_column();
                } else {
                    content = me.make_new_row();
                }

                new_element = me.get_insert_new_element(el, content, parent);

                if(empty) {
                    me.mark_column(new_element);
                } else {
                    me.mark_row(new_element);
                }
            }
            else if($(parent).hasClass(me.tag.container)) {

                if(empty) {
                    content = me.make_new_row();
                } else {
                    content = me.make_new_container();
                }

                new_element = me.get_insert_new_element(el, content, parent);

                if(empty) {
                    me.mark_row(new_element);
                    new_element = $(new_element).children(this.tagc.column);
                } else {
                    me.mark_container(new_element);
                }
            }
            else if($(parent).hasClass(me.tag.section)) {

                if(empty) {
                    content = me.make_new_container();
                } else {
                    content = me.make_new_section();
                }

                new_element = me.get_insert_new_element(el, content, parent);

                if(empty) {
                    me.mark_container(new_element);
                } else {
                    me.mark_section(new_element);
                }
            }

            $(new_element).css('opacity',0);

            setTimeout(function(){
                me.set_toolbar_invisible();
                me.set_toolbar_visible(new_element);
                $(me.tagc._grid_element).removeClass(me.state.is_special_helper);
                $(new_element).css('opacity',1);
            }, 300);

            this.newElement = $(new_element);
            this.options.onNewElementAdded();
            this.changedElement = $(new_element);
            this.options.onGridChanged();
        },

        getNewElement: function () {
            return this.newElement;
        },

        getDuplicatedElement: function () {
            return this.duplicatedElement;
        },

        getMovedElement: function () {
            return this.movedElement;
        },

        getChangedElement: function () {
            return this.changedElement;
        },

        get_insert_new_element: function (el, new_element, parent) {

            if($(el).hasClass(this.tag.append)){
                $(new_element).insertAfter(parent);
                return $(parent).next();
            }
            else if($(el).hasClass(this.tag.prepend)){
                $(new_element).insertBefore(parent);
                return $(parent).prev();
            }
            else if($(el).hasClass(this.tag.empty_placeholder_button)){
                $(new_element).addClass(this.state.is_special_helper).prependTo(parent);
                $(parent).children(this.tagc.empty_placeholder).off().unbind().remove();
                return $(parent).children(this.statec.is_special_helper);
            }
        },

        make_new_column: function (column) {
            var size = 3,
                parent = this.get_parent_grid_element($(column));

            if($(parent).children(this.tagc.column).length < 1) {
                size = this.options.max_col_size;
            }
            return '<div class="col-md-' + size + '"></div>';
        },

        make_new_row: function () {
            return '<div class="row"></div>';
        },

        make_new_container: function () {
            return '<div class="container"></div>';
        },

        make_new_section: function () {
            return '<section class=""></section>';
        },

        /*==================================================================
        /*====================|> CREATE LIBRARY
        /*=================================================================*/

        create_library: function () {

            var me = this;

            var library  = '<div class="'+this.tag.library+'">';
            library += '<div class="'+this.tag.library_header+'"><div class="'+this.tag.library_header_title+'"><iwcm:text key="pagebuilder.create_library.insert"/></div>'+this.create_library_tab_menu()+'</div>';
            library += '<div class="'+this.tag.library_content+'">'+this.create_library_tab_content()+'</div>';
            library += '<div class="'+this.tag.library_footer+'">'+ this.build_button(this.tag.library_footer_button, '<iwcm:text key="pagebuilder.escape"/>') + '</div>';
            library += '</div>';

            $(library).appendTo(this.$wrapper);

            $(this.tagc.library).draggable({
                handle: '.'+me.options.prefix+'-library__header__title',
                drag: function( event, ui ) {

                    var window_width = $(window).outerWidth(),
                        window_height = $(window).outerHeight(),
                        modal_width = $(me.tagc.modal).outerWidth(),
                        modal_height = $(me.tagc.modal).outerHeight(),
                        offset = 10,
                        min_left = offset,
                        max_left = window_width - modal_width - offset,
                        min_top = offset,
                        max_top = window_height - modal_height - offset;

                    if(ui.position.left < min_left)   { ui.position.left = min_left; }
                    if(ui.position.left > max_left)   { ui.position.left = max_left; }
                    if(ui.position.top < min_top)     { ui.position.top = min_top; }
                    if(ui.position.top > max_top)     { ui.position.top = max_top; }
                }
            });

        },

        // <%--// Updatuje kniznicu favorite blokov--%>
        update_library_content: function(){
            $(this.tagc.library_favorites).html(this.create_library_content_template('favorite'));
        },

        // <%--// otvara taby basic/library/favorite--%>
        show_library_tab: function (el) {
            this.clicked_button = $(el);

            var me = this,
                parent = me.get_parent_grid_element(me.clicked_button);
                isEmptyPlaceholderButton = this.clicked_button.hasClass(me.tag.empty_placeholder_button);

            $(me.tagc.library).removeClass(me.tag.library_column);
            $(me.tagc.library).removeClass(me.tag.library_container);
            $(me.tagc.library).removeClass(me.tag.library_section);

            $('.library-tab-link').removeClass('active');
            $('.library-tab-link').first().addClass('active');

            $('.library-tab-item').removeClass('active');
            $('.library-tab-item').first().addClass('active');

            //console.log("show_library_tab, parent=", $(parent), "css=", $(parent).attr("class"), "el=", this.clicked_button, "isEmptyPlaceholderButton=", isEmptyPlaceholderButton);

            if($(parent).hasClass(me.tag.column)) {
                $(me.tagc.library).addClass(me.tag.library_column);
            }

            if($(parent).hasClass(me.tag.row)) {
                $(me.tagc.library).addClass(me.tag.library_column);
            }

            if($(parent).hasClass(me.tag.container)) {
                $(me.tagc.library).addClass(me.tag.library_container);
            }

            if($(parent).hasClass(me.tag.section) || $(parent).hasClass(me.tag.wrapper) ) {
                if (isEmptyPlaceholderButton && $(me.element).find("section").length>0) $(me.tagc.library).addClass(me.tag.library_container);
                else $(me.tagc.library).addClass(me.tag.library_section);
            }

            this.show_library();
        },

        create_library_tab_menu: function() {

            var me = this;

            var tab = '<div class="library-tab-menu">';
            tab += '<span class="library-tab-link active" data-library-type="basic" data-tab-id="01"><iwcm:text key="pagebuilder.library.tab.main"/></span>';
            tab += '<span class="library-tab-link" data-library-type="library" data-tab-id="02"><iwcm:text key="pagebuilder.library.tab.library"/></span>';
            tab += '<span class="library-tab-link" data-library-type="favorite" data-tab-id="03"><iwcm:text key="pagebuilder.library.tab.favorites"/></span>';
            tab += '</div>';

            me.$wrapper.on('click', '.library-tab-link', function() {
                var id = $(this).attr('data-tab-id');
                if(!$(this).hasClass('active')) {
                    $('.library-tab-link').not($(this)).removeClass('active');
                    $(this).addClass('active');
                    $('.library-tab-item').removeClass('active');
                    $('.library-tab-item[data-tab-id="'+id+'"]').addClass('active');
                }
            });

            return tab;
        },

        create_library_tab_content: function() {
            var me = this,
                content_01 = me.create_library_content_template('basic'),
                content_02 = me.create_library_content_template('library'),
                content_03 = me.create_library_content_template('favorite');

            var tab = '<div class="library-tab-content">';
            tab += '<div class="library-tab-item library-tab-item--basic active" data-tab-id="01">'+content_01+'</div>';
            tab += '<div class="library-tab-item library-tab-item--library" data-tab-id="02">'+content_02+'</div>';
            tab += '<div class="library-tab-item library-tab-item--favorite '+me.tag.library_favorites+'" data-tab-id="03">'+content_03+'</div>';
            tab += '</div>';

            me.$wrapper.on('click', '.library-tab-item-button__toggler', function() {
                if($(this).hasClass("active")) {
                    $(this).removeClass("active");
                } else {
                    $(this).addClass("active");
                }
            });

            me.$wrapper.on('click', '.library-tab-item-button', function(e) {
                if($(e.target).hasClass('library-tab-item-delete-favorite')){
                    return;
                }

                var id = $(this).attr('data-library-item-id'),
                    parent = me.get_parent_grid_element(me.clicked_button),
                    insert_content = '',
                    empty = $(me.clicked_button).hasClass(me.tag.empty_placeholder_button),
                    template_type = $('.library-tab-link.active').attr('data-library-type');

                if($(this).hasClass('has-group')){
                    $(me.tagc.library).addClass('show-group');
                    return;
                }

                //console.log("Library item clicked, parent=", parent, "classes=", $(parent).attr("class"), "empty=", empty);

                if($(parent).hasClass(me.tag.column)) {
                    if(empty) {
                        alert('error 1: column empty. Please contact web administrator');
                        return;
                    }

                    var columns = me.get_json_object_by_attribute(me.template[template_type],'textKey','column');
                    var groups = me.get_json_object_by_attribute(columns.groups,'id',id);
                    var content = groups.content;
                    var custom_style = groups.style;

                    if(custom_style != undefined && custom_style != ''){
                        var styles = $(custom_style.replace(/\n/g,""));

                        $.each(styles,function(index, elem){
                            var custom_style_id_old = $(elem).attr('style-id'),
                                custom_style_id_new = me.user_style.unique_selector();

                            // <%--// vymenim style-id atribut v <style>--%>
                            $(elem).attr('style-id',custom_style_id_new);

                            // <%--// vymenim selector v <style>--%>
                            $(elem).html( $(elem).html().replace(custom_style_id_old,custom_style_id_new) );

                            // <%--// vymenim atribut v html (vsetky vyskyty -> pretoze style moze byt prepojene na viacere elementy)--%>
                            content = content.replace(new RegExp(custom_style_id_old, 'g'),custom_style_id_new);

                            // <%--// vlozim style kam patri--%>
                            $(elem).appendTo($(me.tagc.wrapper));
                        });
                    }
                    insert_content = me.get_insert_new_element(me.clicked_button, content, parent);
                    me.mark_column(insert_content);
                }
                else if($(parent).hasClass(me.tag.row)) {
                    // <%--// pri row je vzdy empty=true, pretoze sa sem mozno dostat len z placeholdera - takze ponukam len columns.--%>
                    // <%--// if(empty) {--%>
                    // <%--    // insert_content = me.get_insert_new_element(me.clicked_button, me.template[template_type].column[id].html, parent);--%>
                    // <%--    // me.mark_column($(insert_content));--%>
                    // <%--// } else {--%>
                        var columns = me.get_json_object_by_attribute(me.template[template_type],'textKey','column');
                        var groups = me.get_json_object_by_attribute(columns.groups,'id',id);
                        var content = groups.content;
                        var custom_style = groups.style;

                        if(custom_style != undefined && custom_style != ''){

                            var styles = $(custom_style.replace(/\n/g,""));

                            $.each(styles,function(index, elem){
                                var custom_style_id_old = $(elem).attr('style-id'),
                                    custom_style_id_new = me.user_style.unique_selector();

                                // <%--// vymenim style-id atribut v <style>--%>
                                $(elem).attr('style-id',custom_style_id_new);

                                // <%--// vymenim selector v <style>--%>
                                $(elem).html( $(elem).html().replace(custom_style_id_old,custom_style_id_new) );

                                // <%--// vymenim atribut v html (vsetky vyskyty -> pretoze style moze byt prepojene na viacere elementy)--%>
                                content = content.replace(new RegExp(custom_style_id_old, 'g'),custom_style_id_new);

                                // <%--// vlozim style kam patri--%>
                                $(elem).appendTo($(me.tagc.wrapper));
                            });

                        }
                        insert_content = me.get_insert_new_element(me.clicked_button, content, parent);
                        me.mark_column(insert_content);
                    // <%--// }--%>
                }
                else if($(parent).hasClass(me.tag.container)) {
                    if(empty) {
                        alert('error 2: container empty. Please contact web administrator.');
                        return;
                    }

                    var containers = me.get_json_object_by_attribute(me.template[template_type],'textKey','container');
                    var groups = me.get_json_object_by_attribute(containers.groups,'id',id);
                    var content = groups.content;
                    var custom_style = groups.style;

                    if(custom_style != undefined && custom_style != ''){

                        var styles = $(custom_style.replace(/\n/g,""));

                        $.each(styles,function(index, elem){
                            var custom_style_id_old = $(elem).attr('style-id'),
                                custom_style_id_new = me.user_style.unique_selector();

                            // <%--// vymenim style-id atribut v <style>--%>
                            $(elem).attr('style-id',custom_style_id_new);

                            // <%--// vymenim selector v <style>--%>
                            $(elem).html( $(elem).html().replace(custom_style_id_old,custom_style_id_new) );

                            // <%--// vymenim atribut v html (vsetky vyskyty -> pretoze style moze byt prepojene na viacere elementy)--%>
                            content = content.replace(new RegExp(custom_style_id_old, 'g'),custom_style_id_new);

                            // <%--// vlozim style kam patri--%>
                            $(elem).appendTo($(me.tagc.wrapper));
                        });

                    }
                    insert_content = me.get_insert_new_element(me.clicked_button, content, parent);
                    me.mark_container(insert_content);
                }
                else if($(parent).hasClass(me.tag.section) ) {
                    if(empty) {
                        // <%--// ak je prazdna section, potrebujem vlozit container--%>
                        var containers = me.get_json_object_by_attribute(me.template[template_type],'textKey','container');
                        var groups = me.get_json_object_by_attribute(containers.groups,'id',id);
                        var content = groups.content;
                        var custom_style = groups.style;

                        //console.log("content=", content);

                        if(custom_style != undefined && custom_style != ''){
                            var styles = $(custom_style.replace(/\n/g,""));

                            $.each(styles,function(index, elem){
                                var custom_style_id_old = $(elem).attr('style-id'),
                                    custom_style_id_new = me.user_style.unique_selector();

                                // <%--// vymenim style-id atribut v <style>--%>
                                $(elem).attr('style-id',custom_style_id_new);

                                // <%--// vymenim selector v <style>--%>
                                $(elem).html( $(elem).html().replace(custom_style_id_old,custom_style_id_new) );

                                // <%--// vymenim atribut v html (vsetky vyskyty -> pretoze style moze byt prepojene na viacere elementy)--%>
                                content = content.replace(new RegExp(custom_style_id_old, 'g'),custom_style_id_new);

                                // <%--// vlozim style kam patri--%>
                                $(elem).appendTo($(me.tagc.wrapper));
                            });

                        }
                        insert_content = me.get_insert_new_element(me.clicked_button, content, parent);
                        //console.log("insert_content=", insert_content, "button=", me.clicked_button, "content=", content, "parent=", parent);
                        me.mark_container(insert_content);
                    } else {
                        var sections = me.get_json_object_by_attribute(me.template[template_type],'textKey','section');
                        var groups = me.get_json_object_by_attribute(sections.groups,'id',id);
                        var content = groups.content;
                        var custom_style = groups.style;

                        if(custom_style != undefined && custom_style != ''){
                            var styles = $(custom_style.replace(/\n/g,""));

                            $.each(styles,function(index, elem){
                                var custom_style_id_old = $(elem).attr('style-id'),
                                    custom_style_id_new = me.user_style.unique_selector();

                                // <%--// vymenim style-id atribut v <style>--%>
                                $(elem).attr('style-id',custom_style_id_new);

                                // <%--// vymenim selector v <style>--%>
                                $(elem).html( $(elem).html().replace(custom_style_id_old,custom_style_id_new) );

                                // <%--// vymenim atribut v html (vsetky vyskyty -> pretoze style moze byt prepojene na viacere elementy)--%>
                                content = content.replace(new RegExp(custom_style_id_old, 'g'),custom_style_id_new);

                                // <%--// vlozim style kam patri--%>
                                $(elem).appendTo($(me.tagc.wrapper));
                            });

                        }
                        insert_content = me.get_insert_new_element(me.clicked_button, content, parent);
                        me.mark_section(insert_content);
                    }
                }
                else if( $(parent).hasClass(me.tag.wrapper) ){
                    var content = me.get_json_object_by_attribute(me.get_json_object_by_attribute(me.template[template_type],'textKey','section').groups,'id',id).content;
                    insert_content = me.get_insert_new_element(me.clicked_button, content, parent);
                    me.mark_section(insert_content);
                }

                me.set_toolbar_visible(insert_content);
                $(me.tagc._grid_element).removeClass(me.state.is_special_helper);

                me.newElement = $(insert_content);
                me.options.onNewElementAdded();
                me.changedElement = $(insert_content);
                me.options.onGridChanged();

                me.hide_library();

                //console.log("kliknutie na pridaj v zakladne, esc simulate");
                me.disable_after_esc_pressed(true);
            });

            me.$wrapper.on('click','.library-tab-item-delete-favorite',function(){
                if(!confirm("<iwcm:text key='pagebuilder.library.remove_from_favorites'/>")){
                    return;
                }

                var dis = this,
                    id = $(dis).parent().attr('data-library-item-id'),
                    parent = me.get_parent_grid_element(me.clicked_button),
                    template_type = $('.library-tab-link.active').attr('data-library-type'),  // <%--// asi bude vzdy favorites, ale preistotu necham takto--%>
                    type = '';

                if($(parent).hasClass(me.tag.column)) type = 'column';
                if($(parent).hasClass(me.tag.container)) type = 'container';
                if($(parent).hasClass(me.tag.section)) type = 'section';

                var columns = me.get_json_object_by_attribute(me.template[template_type], 'textKey', type);
                var groups = me.get_json_object_by_attribute(columns.groups, 'id', id);
                var filePath = groups.filePath;

                $.post( "/admin/grideditor/delete/element", {filePath:filePath, templateGroupId: me.options.template_group_id}, function(data) {
                    if(data.result){
                        // <%--// zmazanie elementu v kniznici - nech nedopytujeme znova vsetko a neparsujeme, tak proste len zmazeme ten element v kniznici. --%>
                        $.each(me.template.favorite,function(i,v){
                            if(v.textKey==type){
                                $.each(v.groups,function(ind,val){
                                    if(val.id==id) {
                                        me.template.favorite[i].groups.splice(ind,1);
                                    }
                                });
                            }
                        });
                        me.update_library_content();

                    }else {
                        console.error(data);
                    }
                }).fail(function( jqxhr, textStatus, error ) {
                    console.error( "Request DELETE FAVORITE ELEMENT Failed: " + textStatus + ", " + error );
                });


            });

            me.$wrapper.on('click', '.library-full-width-item', function() {

                var id = $(this).attr('data-library-item-id'),
                    group_id = $(this).attr('data-library-group-id'),
                    parent = me.get_parent_grid_element(me.clicked_button),
                    insert_content = '',
                    template_type = $('.library-tab-link.active').attr('data-library-type');

                var parentTag = null;
                if ($(parent).hasClass(me.tag.column)) parentTag = "column";
                else if($(parent).hasClass(me.tag.row)) parentTag = "container";
                else if($(parent).hasClass(me.tag.container)) parentTag = "container";
                else if($(parent).hasClass(me.tag.section)) parentTag = "section";

                //ak sa klikne na emptybutton musime posunut uroven parenta vyssie
                var emptyClick = me.clicked_button.hasClass(me.tag.empty_placeholder_button);
                //console.log("emptyClick=", emptyClick);
                //console.log("parentTag=", parentTag);
                if (emptyClick===true) {
                    //null je pri vlozeni do prazdnej stranky
                    if (parentTag==null) {
                        parentTag = "section";
                        parent = me.element;
                    }
                    else if(parentTag=="container") parentTag = "column";
                    else if (parentTag=="section") parentTag = "container";
                }
                //console.log("parentTag2=", parentTag, "parent=", parent, "button=", me.clicked_button);

                if(parentTag != null) {
                        var clicked_button = me.clicked_button;

                        var html = me.get_json_object_by_attribute(me.template[template_type], 'textKey', parentTag).groups[group_id].blocks[id].content;
                        //console.log("html=", html);

                        //ak vkladam tab-pane tak ho fyzicky potrebujem vlozit do div.tab-content (ak existuje)
                        if (html.indexOf("<div class=\"tab-pane")==0) {
                            var tabContent = parent.find("div.tab-content");
                            //toto je ked klikneme na nadpis nad tabmi
                            if (tabContent.length==0) tabContent = parent.parent().find("div.tab-content");
                            //console.log("tabContent=", tabContent);
                            if (tabContent.length>0) {
                                //console.log("html=", tabContent.html());
                                if (tabContent.find("div").length==0) {
                                    //je prazdny, zmenme ciel na div.tab-content akoby bolo kliknute na + tlacidlo
                                    parent = tabContent;
                                    //console.log("parent3=", parent);
                                    clicked_button = $("<div/>");
                                    clicked_button.addClass(me.tag.empty_placeholder_button);
                                }
                            }
                        }

                        //ak vkladam accordion vkladam ho do existujuceho div.pb-autoaccordion (ak existuje)
                        if (html.indexOf("<div class=\"md-accordion-item")==0) {
                            //console.log("parent=", parent);
                            var accordion = parent.find("div.pb-autoaccordion");
                            //toto je ked klikneme na nadpis nad accordionom
                            if (accordion.length==0) accordion = parent.parents("div.pb-autoaccordion");
                            if (accordion.length>0) {
                                if (accordion.find("div.card, div.md-accordion-item, div.accordion-item").length==0) {
                                    parent = accordion;
                                    //davame to az za nadpis
                                    var containers = parent.find("div.pb-container");
                                    if (containers.length==0) {
                                        clicked_button = $("<div/>");
                                        clicked_button.addClass(me.tag.empty_placeholder_button);
                                    } else {
                                        parent = $(containers[containers.length-1]);
                                        clicked_button = $("<div/>");
                                        clicked_button.addClass(me.tag.append);
                                    }
                                }
                                accordion.find("."+me.tag.empty_placeholder).remove();
                            }
                        }

                        var uniqueid = Math.round(new Date().getTime()/100);
                        html = html.replace(/__ID__/gi, "_"+uniqueid);

                        //console.log("Inserting HTML: ", html, " parentTag: ", parentTag, " template_type: ", template_type);
                        insert_content = me.get_insert_new_element(clicked_button, html, parent);
                        //console.log("insert_content: ", insert_content);
                        var iwcmWriteElement = insert_content.find("[data-iwcm-write]");
                        //console.log("iwcmWriteElement=", iwcmWriteElement);
                        if (iwcmWriteElement.length > 0) {
                            iwcmWriteElement.each(function(index) {
                                var writeElement = $(this);
                                var data = writeElement.data("iwcm-write");
                                var remove = writeElement.data("iwcm-remove");

                                writeElement.removeAttr("data-iwcm-write");
                                writeElement.removeAttr("data-iwcm-remove");

                                //console.log("writeElement=", writeElement, "remove=", remove, "data=", data);

                                if ("true"==remove || "tag"==remove) {
                                    writeElement[0].outerHTML = data;
                                } else {
                                    writeElement.html(data);
                                }
                            })
                        }

                        //TODO: execute tag
                        me.thExecuteTag("data-th-src", "src", insert_content);
                        me.thExecuteTag("data-th-href", "href", insert_content);

                        if ("column" == parentTag) {
                            //niekedy ako column potrebujeme vlozit aj nieco obsahujuce container, napr. taby do accordionu
                            if (html.indexOf("container")) me.mark_grid_elements();
                            else me.mark_column(insert_content);
                        }
                        else if ("row" == parentTag) me.mark_row(insert_content);
                        else if ("container" == parentTag) me.mark_container(insert_content);
                        else if ("section" == parentTag) me.mark_section(insert_content);
                }

                me.set_toolbar_visible(insert_content);
                $(me.tagc._grid_element).removeClass(me.state.is_special_helper);

                me.newElement = $(insert_content);
                me.options.onNewElementAdded();
                me.changedElement = $(insert_content);
                me.options.onGridChanged();

                me.hide_library();

                //console.log("kliknutie na pridaj v library, esc simulate");
                me.disable_after_esc_pressed(true);
            });

            return tab;
        },

        thExecuteTag: function(dataTagName, realTagName, insert_content) {
            var element = insert_content.find("["+dataTagName+"]");
            //console.log("thExecuteTag=", element);
            if (element.length > 0) {
                element.each(function(index) {
                    var writeElement = $(this);
                    //z tagName odstran data-
                    var data = writeElement.data(dataTagName.substring(5));

                    writeElement.removeAttr(dataTagName);

                    //uprav zakladne vyrazy
                    //console.log("ninja=", ninja)
                    if (typeof ninja != "undefined" && ninja != null && typeof ninja.temp != "undefined") {
                        $.each(ninja.temp, function( key, col ) {
                            //console.log("Executing ninja: key=", key, "col=", col);
                            data = data.replaceAll("$"+"{ninja.temp."+key+"}", col);
                        });
                    }

                    //console.log("writeElement=", writeElement, "data=", data);

                    writeElement.attr(realTagName, data);
                })
            }
        },

        /**
         * vytvori contenty jednotlivych tabov kniznice blokov.
         * @param type
         * @returns {string}
         */
        create_library_content_template: function(type) {
            var content = '';

            var libraryMainGroups = ['section', 'container', 'column'];
            var template = this.template;
            var that = this;
            libraryMainGroups.forEach(function (group, index) {
                // <%--console.log("create_library_content_template, type=", type, " group=", group, "template=", template);--%>
                content += '<div class="library-template-block library-template-block--'+group+'">';
                $.each(that.get_json_object_by_attribute(template[type],'textKey',group).groups, function( index, obj ) {
                    var has_group = '';
                    if( obj.blocks != null ){
                        has_group = 'has-group';

                        var libraryButtonClass = "";

                        if(type == "library") {
                            libraryButtonClass = "__toggler";
                        }

                        content += '<span class="library-tab-item-button'+libraryButtonClass+'" data-library-item-id="'+obj.id+'">'+obj.textKey+'</span>';
                        content += '<div class="library-full-width-item__wrapper">';
                            $.each(obj.blocks, function(indexBlock, block)
                            {
                                // <%--console.log("Block:", index, " ", block);--%>
                                content += '<span class="library-full-width-item" data-library-group-id="'+index+'" data-library-item-id="'+indexBlock+'" style="background-image: url('+block.imagePath+')"><i>'+block.textKey+'</i></span>';
                            });
                        content += '</div>';
                    }
                    else
                    {
                        var deleteTool = '';
                        if(type=='favorite') deleteTool = '<aside class="library-tab-item-delete-favorite"></aside>';
                        content += '<span class="library-tab-item-button" data-library-item-id="'+obj.id+'">'+obj.textKey+deleteTool+'</span>';
                    }
                });
                content += '</div>';
            });


            return content;
        },

        show_library: function () {
            $(this.$wrapper).addClass(this.state.is_library_active);
            this.set_toolbar_invisible();
        },
        hide_library: function () {
            $(this.$wrapper).removeClass(this.state.is_library_active);
        },

        /**
         * vrati json objekt z json pola na zaklade atributu a jeho hodnoty
         * @param json_array
         * @param attribute_name
         * @param attribute_value
         * @returns {{}}
         */
        get_json_object_by_attribute: function(json_array,attribute_name, attribute_value) {
            var ret = {};
            $.each(json_array,function( index,json_object ){
                if(json_object.hasOwnProperty(attribute_name) && json_object[attribute_name]===attribute_value){
                    ret =  json_object;
                }
            });
            return ret;
        },

        /*==================================================================
        /*====================|> CREATE NOTIFY PANEL
        /*=================================================================*/

        create_notify: function () {

            var notify  = '<div class="'+this.tag.notify+'">';
            notify += '<div class="'+this.tag.notify_header+'"><iwcm:text key="pagebuilder.helper"/></div>';
            notify += '<div class="'+this.tag.notify_content+'"></div>';
            notify += '<div class="'+this.tag.notify_footer+'">'+ this.build_button(this.tag.notify_footer_button, '<iwcm:text key="pagebuilder.escape"/>') + '</div>';
            notify += '</div>';

            $(notify).appendTo(this.$wrapper);
        },

        update_notify_content: function (headline,text) {
            $(this.tagc.notify_content).html('<span>'+headline+'</span>'+text);
            this.show_notify();
        },

        show_notify: function () {
            $(this.$wrapper).addClass(this.state.is_notify_active);
        },

        hide_notify: function () {
            $(this.$wrapper).removeClass(this.state.is_notify_active);
            $(this.tagc.library).removeClass('show-group');

            //prepni sa do normalneho rezimu
            this.disable_after_esc_pressed(true);
        },

        disable_after_esc_pressed: function (forceEsc) {
            //console.log("forceEsc=", forceEsc);
            if(this.esc_key_down || (typeof forceEsc != "undefined" && forceEsc==true)) {

                if($(this.$wrapper).hasClass(this.state.has_child_toolbar_active) && !$(this.$wrapper).hasClass(this.state.is_notify_active)){
                    this.set_toolbar_invisible();
                }

                if($(this.$wrapper).hasClass(this.state.is_notify_active)){
                    $(this.tagc.notify_footer_button).click().trigger('click');
                }

                if($(this.$wrapper).hasClass(this.state.is_library_active)){
                    $(this.tagc.library_footer_button).click().trigger('click');
                }
            }
        },

        /*==================================================================
        /*====================|> CREATE MODAL
        /*=================================================================*/

        create_modal: function () {
            var me = this,
                content = '';

            // <%--  ---------------  TAB 01  --%>

            var background_color = me.build_input('background-color',"<iwcm:text key='pagebuilder.modal.color'/>");
            var background_image = me.build_input_elfinder('background-image',"<iwcm:text key='pagebuilder.modal.background_image'/>");
            content += me.build_input_group(background_color,'01');
            content += me.build_input_group(background_image,'01');

            // <%--  ---------------  TAB 02  --%>

            var text_align_type = {
                inherit: '<iwcm:text key="pagebuilder.modal.text-align.inherit"/>',
                left: '<iwcm:text key="pagebuilder.modal.text-align.left"/>',
                center: '<iwcm:text key="pagebuilder.modal.text-align.center"/>',
                right: '<iwcm:text key="pagebuilder.modal.text-align.right"/>'
            };

            var text_align = me.build_radio(text_align_type,'text-align','<iwcm:text key="pagebuilder.modal.text-align"/>');

            content += me.build_input_group(text_align,'02');

            // <%--  ---------------  TAB 03  --%>

            var margin_padding = me.build_four_inputs_in_row("<iwcm:text key='pagebuilder.modal.margin'/>",[
                {   prop:'margin-top',      label: '<iwcm:text key="pagebuilder.modal.margin.top"/>' },
                {   prop: 'margin-bottom',  label: '<iwcm:text key="pagebuilder.modal.margin.bottom"/>'},
                {   prop: 'margin-left',    label: '<iwcm:text key="pagebuilder.modal.margin.left"/>'},
                {   prop: 'margin-right',   label: '<iwcm:text key="pagebuilder.modal.margin.right"/>'}
            ]);

            margin_padding += me.build_four_inputs_in_row("<iwcm:text key='pagebuilder.modal.padding'/>",[
                {   prop:'padding-top',      label: '<iwcm:text key="pagebuilder.modal.padding.top"/>' },
                {   prop: 'padding-bottom',  label: '<iwcm:text key="pagebuilder.modal.padding.bottom"/>'},
                {   prop: 'padding-left',    label: '<iwcm:text key="pagebuilder.modal.padding.left"/>'},
                {   prop: 'padding-right',   label: '<iwcm:text key="pagebuilder.modal.padding.right"/>'}
            ]);

            content += me.build_input_group(margin_padding,'03');

            // <%--  ---------------  TAB 04  --%>

            var border_radius = me.build_four_inputs_in_row("<iwcm:text key='pagebuilder.modal.border-radius'/>",[
                {   prop:'border-top-left-radius',      label: '<iwcm:text key="pagebuilder.modal.border-radius.top_left"/>' },
                {   prop: 'border-top-right-radius',  label: '<iwcm:text key="pagebuilder.modal.border-radius.top_right"/>'},
                {   prop: 'border-bottom-left-radius',    label: '<iwcm:text key="pagebuilder.modal.border-radius.bottom_left"/>'},
                {   prop: 'border-bottom-right-radius',   label: '<iwcm:text key="pagebuilder.modal.border-radius.bottom_right"/>'}
            ]);

            content += me.build_input_group(border_radius,'04');

            // <%--  ---------------  TAB 05  --%>

            var border = me.build_four_inputs_in_row("<iwcm:text key='pagebuilder.modal.border'/>",[
                {   prop:'border-top-width',      label: '<iwcm:text key="pagebuilder.modal.border.top"/>' },
                {   prop: 'border-bottom-width',    label: '<iwcm:text key="pagebuilder.modal.border.bottom"/>'},
                {   prop: 'border-right-width',  label: '<iwcm:text key="pagebuilder.modal.border.right"/>'},
                {   prop: 'border-left-width',   label: '<iwcm:text key="pagebuilder.modal.border.left"/>'}
            ]);

            var border_style_type = {
                none: '<iwcm:text key="pagebuilder.modal.border-style.none"/>',
                solid: '<iwcm:text key="pagebuilder.modal.border-style.solid"/>',
                dashed: '<iwcm:text key="pagebuilder.modal.border-style.dashed"/>',
                dotted: '<iwcm:text key="pagebuilder.modal.border-style.dotted"/>'
            };

            border += me.build_radio(border_style_type,'border-style', '<iwcm:text key="pagebuilder.modal.border-style"/>');
            border += me.build_input('border-color','<iwcm:text key="pagebuilder.modal.border-color"/>');

            content += me.build_input_group(border,'05');

            // <%--  ---------------  TAB 06  --%>

            var box_shadow = '<div class="'+me.tag.style_input_group_four_in_row+'">';
            box_shadow += me.build_input('box-shadow-horizontal', '<iwcm:text key="pagebuilder.modal.box-shadow.horizontal"/>');
            box_shadow += me.build_input('box-shadow-vertical', '<iwcm:text key="pagebuilder.modal.box-shadow.vertical"/>');
            box_shadow += me.build_input('box-shadow-blur', '<iwcm:text key="pagebuilder.modal.box-shadow.blur"/>');
            box_shadow += me.build_input('box-shadow-spread', '<iwcm:text key="pagebuilder.modal.box-shadow.spread"/>');
            box_shadow += '</div>';
            box_shadow += me.build_input_hidden('box-shadow');
            box_shadow += me.build_input('box-shadow-color','<iwcm:text key="pagebuilder.modal.box-shadow.color"/>');

            content += me.build_input_group(box_shadow,'06');

            // <%--  ---------------  POKROCILE  --%>
            var sizes = me.build_input('width','<iwcm:text key="pagebuilder.modal.width"/>');
            sizes += me.build_input('min-width','<iwcm:text key="pagebuilder.modal.width.min"/>');
            sizes += me.build_input('max-width','<iwcm:text key="pagebuilder.modal.width.max"/>');
            sizes += me.build_input('height','<iwcm:text key="pagebuilder.modal.width"/>');
            sizes += me.build_input('min-height','<iwcm:text key="pagebuilder.modal.width.min"/>');
            sizes += me.build_input('max-height','<iwcm:text key="pagebuilder.modal.width.max"/>');

            content += me.build_input_group(sizes,'07');

            var visibility = "";

            //visibility += me.build_checkbox('visibility-xs','<iwcm:text key="pagebuilder.modal.visibility.xs"/>');
            visibility += me.build_checkbox('visibility-sm','<iwcm:text key="pagebuilder.modal.visibility.sm"/>');
            visibility += me.build_checkbox('visibility-md','<iwcm:text key="pagebuilder.modal.visibility.md"/>');
            //visibility += me.build_checkbox('visibility-lg','<iwcm:text key="pagebuilder.modal.visibility.lg"/>');
            visibility += me.build_checkbox('visibility-xl','<iwcm:text key="pagebuilder.modal.visibility.xl"/>');
            content += me.build_input_group(visibility,'08');

            var selectors = me.build_input('selector-id','<iwcm:text key="pagebuilder.modal.id"/>');
            selectors += me.build_input('selector-class','<iwcm:text key="pagebuilder.modal.class"/>');
            selectors += me.build_input('attr-title','<iwcm:text key="editor.htmlprop.title"/>');
            content += me.build_input_group(selectors,'10');

            var style_connection = me.build_style_connections();
            content += me.build_input_group(style_connection,'11');

            var z_index = me.build_input('z-index','<iwcm:text key="pagebuilder.modal.z-index"/>');
            z_index += '<div class="'+me.tag.modal_zindex+'"></div>';
            content += me.build_input_group(z_index,'12');

            var header = '<span class="header-title"></span>'+ me.build_button(me.tag.modal_header_button_show, '') + me.build_tab_menu();

            var footer = me.build_button(me.tag.modal_footer_button_close, '<iwcm:text key="pagebuilder.modal.btn.close"/>');
            footer += me.build_button(me.tag.modal_footer_button_reset, '<iwcm:text key="pagebuilder.modal.btn.reset"/>');
            footer += me.build_button(me.tag.modal_footer_button_save, '<iwcm:text key="pagebuilder.modal.btn.save"/>');

            var modal = '<div class="'+me.tag.modal+'">';
            modal += '<div class="'+me.tag.modal_header+'">'+header+'</div>';
            modal += '<div class="'+me.tag.modal_content+'"><form name="'+me.options.prefix+'-form">'+content+'</form></div>';
            modal += '<div class="'+me.tag.modal_footer+'">'+footer+'</div>';
            modal += '</div>';

            $(modal).appendTo(me.$wrapper);

            me.make_modal_draggable();

            me.set_spinner('margin-top', -500, 500);
            me.set_spinner('margin-bottom', -500, 500);
            me.set_spinner('margin-left', -500, 500);
            me.set_spinner('margin-right', -500, 500);

            me.set_spinner('padding-top', -500, 500);
            me.set_spinner('padding-bottom', -500, 500);
            me.set_spinner('padding-left', -500, 500);
            me.set_spinner('padding-right', -500, 500);

            me.set_spinner('border-top-left-radius', 0, 100);
            me.set_spinner('border-top-right-radius', 0, 100);
            me.set_spinner('border-bottom-left-radius', 0, 100);
            me.set_spinner('border-bottom-right-radius', 0, 100);

            me.set_spinner('border-top-width', 0, 50);
            me.set_spinner('border-bottom-width', 0, 50);
            me.set_spinner('border-left-width', 0, 50);
            me.set_spinner('border-right-width', 0, 50);

            me.set_spinner('box-shadow-horizontal', -50, 50);
            me.set_spinner('box-shadow-vertical', -50, 50);
            me.set_spinner('box-shadow-blur', 0, 50);
            me.set_spinner('box-shadow-spread', 0, 50);

            me.set_spinner('z-index', 0, 10000);

            me.set_box_shadow('box-shadow');

            me.set_minicolors('box-shadow-color');
            me.set_minicolors('background-color');
            me.set_minicolors('border-color');

            me.$wrapper.on('click', me.tagc.modal_header_button_show, function() {

                var scrollTime = 600;

                $('html,body').animate({
                    scrollTop: $(me.user_style.current_element).offset().top - $(window).height()/2
                }, scrollTime);

                setTimeout(function(){
                    $(me.user_style.current_element).addClass(me.state.is_blinking);
                    setTimeout(function(){
                        $(me.user_style.current_element).removeClass(me.state.is_blinking);
                    },1500);
                },scrollTime);

            });

            me.$wrapper.on('click', me.tagc.connection_reference, function() {

                var $el = $(this).data('connection-reference');
                var scrollTime = 600;

                $('html,body').animate({
                    scrollTop: $el.offset().top - $(window).height()/2
                }, scrollTime);

                setTimeout(function(){
                    $el.addClass(me.state.is_blinking);
                    setTimeout(function(){
                        $el.removeClass(me.state.is_blinking);
                    },1500);
                },scrollTime);

            });

            me.$wrapper.on('change', me.tagc.style_input_group_four_in_row_checkbox, function() {
                var $dis = $(this),
                    disVal = $dis.is(':checked'),
                    $parent = $dis.closest(me.tagc.style_input_group_four_in_row);

                if($dis.hasClass(me.tag.style_input_group_four_in_row_checkbox_all)){
                    if(disVal){ $parent.addClass('all'); }
                    else{       $parent.removeClass('all'); }
                    $parent.find(me.tagc.style_input_group_four_in_row_checkbox_first_second+', '+me.tagc.style_input_group_four_in_row_checkbox_third_fourth).prop('disabled',disVal).prop('checked',disVal);
                    $parent.find(me.tagc.style_input_group_four_in_row_second+', '+me.tagc.style_input_group_four_in_row_third+', '+me.tagc.style_input_group_four_in_row_fourth).find('input').prop('disabled',disVal);
                } else if($dis.hasClass(me.tag.style_input_group_four_in_row_checkbox_first_second)){
                    if(disVal){ $parent.addClass('first-second'); }
                    else{       $parent.removeClass('first-second'); }
                    $parent.find(me.tagc.style_input_group_four_in_row_second+' input').prop('disabled',disVal);
                }else {
                    if(disVal){ $parent.addClass('third-fourth'); }
                    else{       $parent.removeClass('third-fourth'); }
                    $parent.find(me.tagc.style_input_group_four_in_row_fourth+' input').prop('disabled',disVal);
                }
                //always allow first input
                //console.log("change1=", $dis, "disVal=", disVal, "parent=", $parent);
                $parent.find(me.tagc.style_input_group_four_in_row_first).find('input').prop('disabled',false);
            });
            me.$wrapper.on('change', me.tagc.style_input_group_four_in_row+' .pb-style-input-wrapper input', function() {
                var $dis = $(this),
                    disVal = $dis.val(),
                    $parent = $dis.closest(me.tagc.style_input_group_four_in_row);
                if($parent.find(me.tagc.style_input_group_four_in_row_checkbox_all).is(':checked')){
                    $parent.find(me.tagc.style_input_group_four_in_row_second+', '+me.tagc.style_input_group_four_in_row_third+', '+me.tagc.style_input_group_four_in_row_fourth).find('input').val(disVal);
                }else {
                    if ($dis.closest(me.tagc.style_input_group_four_in_row_first).length > 0) {
                        if($parent.find(me.tagc.style_input_group_four_in_row_checkbox_first_second).is(':checked')) {
                            $parent.find(me.tagc.style_input_group_four_in_row_second+' input').val(disVal);
                        }
                    }
                    if ($dis.closest(me.tagc.style_input_group_four_in_row_third).length > 0) {
                        if($parent.find(me.tagc.style_input_group_four_in_row_checkbox_third_fourth).is(':checked')) {
                            $parent.find(me.tagc.style_input_group_four_in_row_fourth+' input').val(disVal);
                        }
                    }
                }
                //always allow first input
                //console.log("change2=", $dis, "disVal=", disVal, "parent=", $parent);
                $parent.find(me.tagc.style_input_group_four_in_row_first).find('input').prop('disabled',false);

            });
        },

        make_modal_draggable: function() {
            var me = this;
            $(this.tagc.modal).draggable({
                handle: '.header-title',
                drag: function( event, ui ) {

                    var window_width = $(window).outerWidth(),
                        window_height = $(window).outerHeight(),
                        modal_width = $(me.tagc.modal).outerWidth(),
                        modal_height = $(me.tagc.modal).outerHeight(),
                        offset = 10,
                        min_left = offset,
                        max_left = window_width - modal_width - offset,
                        min_top = offset,
                        max_top = window_height - modal_height - offset;

                    if(ui.position.left < min_left)   { ui.position.left = min_left; }
                    if(ui.position.left > max_left)   { ui.position.left = max_left; }
                    if(ui.position.top < min_top)     { ui.position.top = min_top; }
                    if(ui.position.top > max_top)     { ui.position.top = max_top; }
                }
            });
        },

        build_input_group: function(buttons, id) {

            var content = '<div class="'+this.tag.style_input_group+'" data-input-group-id="'+id+'">';
            content += '<div class="'+this.tag.style_input_group_content+'">'+buttons+'</div>';
            content += '</div>';

            return content;

        },

        build_input_number: function (prop,label,options) {
            var klass = (options!=undefined && 'class' in options)? options.class:'';
            var disabled = (options!=undefined && 'disabled' in options)? options.disabled:false;
            return '<div class="'+this.tag.style_input_wrapper+' '+klass+'"><div class="'+this.tag.style_label+'">'+label+'</div><input type="number" disabled="'+disabled+'" class="'+this.tag.style_input+'" name="'+prop+'" value="0" /></div>';
        },

        build_four_inputs_in_row: function(label,sets){
            var me = this,
                ret = '',
                index=0;

            ret += '<div class="'+me.tag.style_input_group_headline+'">'+label+'</div>';
            ret += '<div class="'+me.tag.style_input_group_four_in_row+' all">';
            ret +='<div class="'+me.tag.style_input_group_four_in_row_tools+'">';
            ret +='<input type="checkbox" class="'+me.tag.style_input_group_four_in_row_checkbox+' '+me.tag.style_input_group_four_in_row_checkbox_all+'" name="connection-all" checked="true" />';
            ret +='<input type="checkbox" class="'+me.tag.style_input_group_four_in_row_checkbox+' '+me.tag.style_input_group_four_in_row_checkbox_first_second+'" name="connection-first-second" checked="true" disabled="true"/>';
            ret +='<input type="checkbox" class="'+me.tag.style_input_group_four_in_row_checkbox+' '+me.tag.style_input_group_four_in_row_checkbox_third_fourth+'" name="connection-third-fourth" checked="true" disabled="true"/>';
            ret += '</div>';
            $.each(sets,function(i,v){
                ret += me.build_input_number(v.prop, v.label,{disabled:true,class:[me.tag.style_input_group_four_in_row_first,me.tag.style_input_group_four_in_row_second,me.tag.style_input_group_four_in_row_third,me.tag.style_input_group_four_in_row_fourth][index++]});
            });
            ret += '</div>';
            return ret;
        },

        build_input: function (prop,label) {
            return '<div class="'+this.tag.style_input_wrapper+'"><div class="'+this.tag.style_label+'">'+label+'</div><input type="text" class="'+this.tag.style_input+'" name="'+prop+'" value="" /></div>';
        },

        build_input_elfinder: function (prop,label) {
            var htmlCode =
                '<div class="'+this.tag.style_input_wrapper+'">'+
                    '<div class="'+this.tag.style_label+'">'+label+'</div>'+
                    '<div class="input-group">'+
                        '<input type="text" class="'+this.tag.style_input+'" name="'+prop+'" value="" style="width:80%" />'+
                        '<span class="input-group-addon" style="min-width: 34px; background-color: #304866;background-size:26px;background-repeat:no-repeat;background-position:center;background-image:url(\'/admin/webpages/page-builder/images/photo.png\')" onclick="openImageDialogWindow(\''+this.options.prefix+'-form\', \''+prop+'\', \'\')"></span>'+
                    '</div>'+
                '</div>';

            return htmlCode;
        },

        build_checkbox: function (prop,label) {
            return '<div class="'+this.tag.style_input_wrapper+'"><div class="'+this.tag.style_label+'">'+label+'</div><input type="checkbox" class="'+this.tag.style_input+'" name="'+prop+'" value="true" /></div>';
        },

        build_style_connections: function () {
            return '<div class="'+this.tag.style_input_wrapper+'"><div class="'+this.tag.style_label+'"><iwcm:text key="pagebuilder.modal.connected_elements"/></div><div class="style-connections-list"><iwcm:text key="pagebuilder.modal.connected_elements.empty"/></div></div>';
        },

        build_input_hidden: function (prop) {
            return '<input type="hidden" class="'+this.tag.style_input+'" name="'+prop+'" value="" />';
        },

        build_radio: function (values,prop,label) {
            var me = this,
                id,
                html = '<div class="'+me.tag.style_input_wrapper+'"><div class="'+me.tag.style_label+'">'+label+'</div><div class="radio-group">';

            $.each(values, function( key, value ) {
                id = prop + '-' + value;
                html += '<input type="radio" class="'+me.tag.style_input+'" name="'+prop+'" value="'+key+'" id="'+id+'"><label for="'+id+'">'+value+'</label>';
            });

            html += '</div></div>';

            return html;
        },

        build_tab_menu: function() {

            var tab = '<div class="tab-menu"><span class="tab-link active" data-tab-id="01"><iwcm:text key="pagebuilder.modal.settings.base"/></span><span class="tab-link"  data-tab-id="02"><iwcm:text key="pagebuilder.modal.settings.advanced"/></span></div>';

            var style_buttons = '<span class="tab-item-button" data-input-group-id="01"><iwcm:text key="pagebuilder.modal.tab.background"/></span>';
            style_buttons += '<span class="tab-item-button" data-input-group-id="02"><iwcm:text key="pagebuilder.modal.tab.text-align"/></span>';
            style_buttons += '<span class="tab-item-button" data-input-group-id="03"><iwcm:text key="pagebuilder.modal.tab.margin_padding"/></span>';
            style_buttons += '<span class="tab-item-button" data-input-group-id="04"><iwcm:text key="pagebuilder.modal.tab.border-radius"/></span>';
            style_buttons += '<span class="tab-item-button" data-input-group-id="05"><iwcm:text key="pagebuilder.modal.tab.border"/></span>';
            style_buttons += '<span class="tab-item-button" data-input-group-id="06"><iwcm:text key="pagebuilder.modal.tab.box-shadow"/></span>';

            var settings_buttons = '<span class="tab-item-button" data-input-group-id="07"><iwcm:text key="pagebuilder.modal.tab.size"/></span>';
            settings_buttons += '<span class="tab-item-button" data-input-group-id="08"><iwcm:text key="pagebuilder.modal.tab.visibility"/></span>';
            settings_buttons += '<span class="tab-item-button" data-input-group-id="09"><iwcm:text key="pagebuilder.modal.tab.animations"/></span>';
            settings_buttons += '<span class="tab-item-button" data-input-group-id="10"><iwcm:text key="pagebuilder.modal.tab.id_class"/></span>';
            settings_buttons += '<span class="tab-item-button" data-input-group-id="11"><iwcm:text key="pagebuilder.modal.tab.connections"/></span>';
            settings_buttons += '<span class="tab-item-button" data-input-group-id="12"><iwcm:text key="pagebuilder.modal.tab.z-index"/></span>';

            tab += '<div class="tab-content"><div class="tab-item active" data-tab-id="01">'+style_buttons+'</div><div class="tab-item" data-tab-id="02">'+settings_buttons+'</div></div>';

            var me = this;

            me.$wrapper.on('click', '.tab-menu .tab-link', function() {

                var id = $(this).attr('data-tab-id');
                if($(this).hasClass('active')) {
                } else {

                    $('.tab-item .tab-item-button').removeClass('active');
                    $(me.tagc.style_input_group).removeClass('active');

                    $('.tab-menu .tab-link').not($(this)).removeClass('active');
                    $(this).addClass('active');
                    $('.tab-content .tab-item').removeClass('active');
                    $('.tab-content .tab-item[data-tab-id="'+id+'"]').addClass('active');
                }
            });

            me.$wrapper.on('click', '.tab-item .tab-item-button', function() {
                var id = $(this).attr('data-input-group-id');

                if($(this).hasClass('active')) {
                    $('.tab-item .tab-item-button').removeClass('active');
                    $(me.tagc.style_input_group).removeClass('active');
                    $(this).removeClass('active');
                } else {
                    $('.tab-item .tab-item-button').removeClass('active');
                    $(me.tagc.style_input_group).removeClass('active');
                    $(this).addClass('active');
                    $(me.tagc.style_input_group+'[data-input-group-id="'+id+'"]').addClass('active');
                }
            });

            return tab;
        },

        set_box_shadow: function(prop) {
            $('[name="'+prop+'-color"], [name="'+prop+'-horizontal"], [name="'+prop+'-vertical"], [name="'+prop+'-blur"], [name="'+prop+'-spread"]').on('change', function () {

                var color = $('[name="'+prop+'-color"]').val(),
                    x = $('[name="'+prop+'-horizontal"]').val(),
                    y = $('[name="'+prop+'-vertical"]').val(),
                    blur = $('[name="'+prop+'-blur"]').val(),
                    spread = $('[name="'+prop+'-spread"]').val(),
                    new_val = color + ' '+ x + 'px ' + y + 'px ' + blur + 'px ' + spread + 'px';

                $('[name="'+prop+'"]').val(new_val);
            });
        },

        set_spinner: function (prop,min_val,max_val) {

            $('[name="'+prop+'"]').spinner({
                min: min_val,
                max: max_val,
                spin: function() {
                    var input = $(this);
                    setTimeout(function(){
                        input.change();
                    },100);
                }
            });

            $('[name="'+prop+'"]')
                .attr('min', min_val)
                .attr('max', max_val);

            $('[name="'+prop+'"]').on('keydown', function () {
                var input = $(this);

                setTimeout(function(){

                    var val = input.val();

                    if(val < min_val){
                        val = min_val;
                    } else if(val > max_val){
                        val = max_val;
                    } else if (val === '' || val === ' ' ) {
                        val = 0;
                    } else {
                        val = parseInt(val, 10);
                    }

                    input.val(val).change();

                },100);
            });

        },

        set_minicolors: function (element) {

            var input = $('[name="'+element+'"]');

            $(input).minicolors({
                inline:true,
                opacity: true,
                format:'rgb',
                swatches: [
                    '#001f3f',
                    '#0074D9',
                    '#7FDBFF',
                    '#39CCCC',
                    '#3D9970',
                    '#2ECC40',
                    '#01FF70',
                    '#FFDC00',
                    '#FF851B',
                    '#FF4136',
                    '#85144b',
                    '#F012BE',
                    '#B10DC9',
                    '#111111',
                    '#AAAAAA',
                    '#DDDDDD'
                ]
            });
        },

        /*==================================================================
        /*====================|> OPEN MODAL
        /*=================================================================*/

        open_modal: function (el) {

            var me = this,
                grid_element = me.get_parent_grid_element(el);

            me.user_style.current_element = $(grid_element);
            $(me.user_style.current_element).addClass(me.state.is_styling);
            me.set_current_element_style_id();
            me.set_modal_title();
            me.set_modal_actual_style(true);
            me.set_style_connections();
            me.set_modal_zindex(el);
            me.set_toolbar_invisible();
            me.set_modal_default_state();

            $(me.$wrapper).addClass(me.state.is_modal_open);
        },

        set_modal_title: function () {
            var me = this;

            if($(me.user_style.current_element).hasClass(me.tag.section)) {
                $(me.$wrapper).addClass(me.state.is_styling_section);
                $('.header-title').html('<iwcm:text key="pagebuilder.modal.title.section"/>');
            }

            else if($(me.user_style.current_element).hasClass(me.tag.container)) {
                $(me.$wrapper).addClass(me.state.is_styling_container);
                $('.header-title').html('<iwcm:text key="pagebuilder.modal.title.container"/>');
            }

            else if($(me.user_style.current_element).hasClass(me.tag.column)) {
                $(me.$wrapper).addClass(me.state.is_styling_column);
                $('.header-title').html('<iwcm:text key="pagebuilder.modal.title.column"/>');
            }

            else {

            }
        },

        set_modal_default_state: function(){
            $('.tab-menu .tab-link').first().click();
            $('.tab-item .tab-item-button').first().click();
        },

        set_modal_zindex: function(el){
            var me = this,
                ret = "",
                $gridEl = $(el).parents(this.tagc._grid_element),
                parents = $gridEl.parentsUntil(me.$wrapper).get().reverse();

            $(parents).each(function(i,p){
                var zindex = $(p).css("z-index");
                if(zindex!=""){
                    ret += p.nodeName.toLowerCase()+
                        "."+
                        p.className.replace(/ /g, ".")+
                        ':'+zindex+'<br>';
                }
            });

            $(me.tagc.modal_zindex).html(ret);
        },

        set_current_element_style_id: function () {
            var me = this;
            if(typeof me.get_current_element_style_id() === 'undefined') {
                me.user_style.current_element.attr(me.user_style.attr_name, me.user_style.unique_selector());
            }
        },

        /*==================================================================
        /*====================|> CLOSE MODAL
        /*=================================================================*/

        close_modal: function () {
            this.clear_after_close_modal();
            this.set_old_style();
        },

        /*==================================================================
        /*====================|> SAVE MODAL
        /*=================================================================*/

        save_modal: function () {
            this.clear_after_close_modal();
        },

        /*==================================================================
        /*====================|> CLEAR AFTER CLOSE MODAL
        /*=================================================================*/

        clear_after_close_modal: function () {
            $(this.$wrapper)
                .removeClass(this.state.is_modal_open)
                .removeClass(this.state.is_styling_column)
                .removeClass(this.state.is_styling_container)
                .removeClass(this.state.is_styling_section);

            $(this.tagc._grid_element).removeClass(this.state.is_styling)
                .removeClass(this.state.has_same_style);

            var style_id = this.get_current_element_style_id();

            if( $('['+this.user_style.attr_name+'="'+style_id+'"]').length < 2 ) {
                $(this.user_style.current_element).removeClass(this.state.is_style_connected);
            } else {
                $(this.user_style.current_element).addClass(this.state.is_style_connected);
            }
        },

        /*==================================================================
        /*====================|> RESET MODAL
        /*=================================================================*/

        reset_modal: function () {
            var me = this;

            if(typeof me.get_current_element_style_id() !== 'undefined') {
                var style_id = me.get_current_element_style_id();
                $('style[style-id="'+style_id+'"]').unbind().off().remove();
            }

            me.set_modal_actual_style(false);
        },

        /*==================================================================
        /*====================|> GET GRID ELEMENT STYLE
        /*=================================================================*/

        get_grid_element_style: function (set_old) {

            var style = $(this.user_style.current_element).css(this.user_style.properties);

            if(this.user_style.current_element.hasClass(this.tags.column)) {
                style = $(this.user_style.current_element).children(this.tagc.column_content).css(this.user_style.properties);
            }

            if(set_old) {
                this.user_style.current_element_old_style = style;
            }

            $.each(this.user_style.px_properties, function( index, value ) {
                style[value] = style[value].replace('px','');
            });

            if(style['box-shadow'] === 'none') {

                style['box-shadow-color'] = 'rgba(0,0,0,0)';
                style['box-shadow-horizontal'] = 0;
                style['box-shadow-vertical'] = 0;
                style['box-shadow-blur'] = 0;
                style['box-shadow-spread'] = 0;

            } else {

                var box_shadow;

                if(style['box-shadow'].indexOf('rgba') > -1) {
                    box_shadow = style['box-shadow'].match(/(-?\d+px)|(rgba\(.+\))/g);
                } else {
                    box_shadow = style['box-shadow'].match(/(-?\d+px)|(rgb\(.+\))/g);
                }

                style['box-shadow-color'] = box_shadow[0];
                style['box-shadow-vertical'] = box_shadow[1].replace('px','');
                style['box-shadow-horizontal'] = box_shadow[2].replace('px','');
                style['box-shadow-blur'] = box_shadow[3].replace('px','');
                style['box-shadow-spread'] = box_shadow[4].replace('px','');
            }

            style['selector-class'] = '';
            var selector_class = $(this.user_style.current_element).attr('data-' + this.options.prefix + '-selector-class');

            if( typeof selector_class !== 'undefined' || selector_class !== '') {
                style['selector-class'] = selector_class;
            }

            style['selector-id'] = '';
            var selector_id = $(this.user_style.current_element).attr('data-' + this.options.prefix + '-selector-id');
            if( typeof selector_id == 'undefined') selector_id = $(this.user_style.current_element).attr('id');

            if( typeof selector_id !== 'undefined' || selector_id !== '') {
                style['selector-id'] = selector_id;
            }

            style['visibility-xs']  = !$(this.user_style.current_element).hasClass('d-none');
            style['visibility-sm']  = !$(this.user_style.current_element).hasClass('d-sm-none');
            style['visibility-md']  = !$(this.user_style.current_element).hasClass('d-md-none');
            style['visibility-lg']  = !$(this.user_style.current_element).hasClass('d-lg-none');
            style['visibility-xl']  = !$(this.user_style.current_element).hasClass('d-xl-none');

            style['attr-title']  = $(this.user_style.current_element).attr("title");

            //console.log("get_grid_element_style, style=", style, " set_old=", set_old);

            return style;
        },

        get_new_style: function () {
            var me = this,
                new_style = {};

            var visibilityCounterTrue = 0;
            $.each(me.user_style.properties, function( index, propertie ) {

                if (propertie.indexOf('visibility-') === 0) {
                    new_style[propertie] = $('[name="'+propertie+'"]').is(":checked");
                    if (new_style[propertie] == true) visibilityCounterTrue++;
                }
                else if(propertie === 'border-style' || propertie === 'text-align') {
                    new_style[propertie] = $('[name="'+propertie+'"]:checked').val();
                } else if (propertie === 'background-image') {
                    let bgimage = $('[name="'+propertie+'"]').val();
                    if (""==bgimage) bgimage = "none";
                    else if (bgimage.indexOf("url(")==-1) bgimage = "url("+bgimage+")";

                    new_style[propertie] = bgimage;
                } else {
                    new_style[propertie] = $('[name="'+propertie+'"]').val();
                }

            });

            $.each(this.user_style.px_properties, function( index, propertie ) {
                new_style[propertie] += 'px';
            });

            //cleanup
            //TODO: nejako detekovat co sa zmenilo a nedavat tam zbytocne cele CSS
            var new_style_clean = {};
            $.each(new_style, function( name, value ) {
                //console.log("Iterating, name=", name, " value=", value);
                //odstran vsetky visibility, nema zmysel ich davat ak su zvolene vsetky
                if (visibilityCounterTrue == 5 && name.indexOf("visibility-")==0) {
                    return;
                }

                new_style_clean[name] = value;
            });

            //console.log("get_new_style, new_style=", new_style, " clean=", new_style_clean);

            return new_style_clean;
        },

        set_new_style: function () {
            if($(this.$wrapper).hasClass(this.state.is_modal_open)) {
                this.create_style_element(this.get_new_style());
                WJ.fireEvent("WJ.PageBuilder.styleChange");
            }
        },

        set_old_style: function () {
            this.create_style_element(this.user_style.current_element_old_style);
        },

        create_style_element: function (styles) {

            var me = this,
                column_content = '';

            me.set_current_element_style_id();

            if(me.user_style.current_element.hasClass(me.tags.column)) {
                column_content = '> .column-content';
            }

            var style_id = me.get_current_element_style_id(),
                style = 'html > body ['+me.user_style.attr_name+'="'+style_id+'"] '+column_content+'{';

            //console.log("Creating style, id=", style_id, " styles=", styles);

            $.each(styles, function( prop, value ) {

                if(prop.indexOf('attr-')==0) {
                    //jedna sa o standardny HTML atribut
                    me.user_style.current_element.attr(prop.substring("5"), value);
                }
                else if(prop === 'selector-class') {
                    if(me.user_style.current_element.attr('data-' + me.options.prefix + '-' + prop) !== value) {
                        me.user_style.current_element.removeClass(me.user_style.current_element.attr('data-' + me.options.prefix + '-' + prop));
                        me.user_style.current_element.attr('data-' + me.options.prefix + '-' + prop, value);
                        me.user_style.current_element.addClass(value);
                    }
                } else if(prop === 'selector-id') {
                    if(me.user_style.current_element.attr('data-' + me.options.prefix + '-' + prop) !== value) {
                        me.user_style.current_element.attr('data-' + me.options.prefix + '-' + prop, value);
                        me.user_style.current_element.attr('id', value);
                    }
                } else if (prop === 'visibility-xs') {
                    if(value === true) {
                        me.user_style.current_element.removeClass('d-none');
                        me.user_style.current_element.addClass('d-block');
                    } else {
                        me.user_style.current_element.removeClass('d-block');
                        me.user_style.current_element.addClass('d-none');
                    }
                } else if (prop === 'visibility-sm') {
                    if(value === true) {
                        me.user_style.current_element.removeClass('d-sm-none');
                        me.user_style.current_element.addClass('d-sm-block');
                    } else {
                        me.user_style.current_element.removeClass('d-sm-block');
                        me.user_style.current_element.addClass('d-sm-none');
                    }
                } else if (prop === 'visibility-md') {
                    if(value === true) {
                        me.user_style.current_element.removeClass('d-md-none');
                        me.user_style.current_element.addClass('d-md-block');
                    } else {
                        me.user_style.current_element.removeClass('d-md-block');
                        me.user_style.current_element.addClass('d-md-none');
                    }
                } else if (prop === 'visibility-lg') {
                    if(value === true) {
                        me.user_style.current_element.removeClass('d-lg-none');
                        me.user_style.current_element.addClass('d-lg-block');
                    } else {
                        me.user_style.current_element.removeClass('d-lg-block');
                        me.user_style.current_element.addClass('d-lg-none');
                    }
                } else if (prop === 'visibility-xl') {
                    if(value === true) {
                        me.user_style.current_element.removeClass('d-xl-none');
                        me.user_style.current_element.addClass('d-xl-block');
                    } else {
                        me.user_style.current_element.removeClass('d-xl-block');
                        me.user_style.current_element.addClass('d-xl-none');
                    }
                } else if (prop === 'background-image') {
                    //overwrite background-image because it's probably inline
                    me.user_style.current_element.css('background-image', value);
                } else {
                    style += prop+':'+value+';';
                }

            });

            style += '}';

            //console.log("Applying style=", style, "id=", style_id, "element=", $('style[style-id="'+style_id+'"]'));

            if($('style[style-id="'+style_id+'"]').length < 1) {
                $('<style style-id="'+style_id+'">')
                    .html(style)
                    .appendTo(me.$wrapper);
            } else {
                $('style[style-id="'+style_id+'"]').html(style);
            }
        },

        set_modal_actual_style: function (set_old) {
            var me = this,
                actual_style = me.get_grid_element_style(set_old);

            $.each( actual_style, function( prop, value ) {

                if(prop === 'background-color' || prop === 'border-color' || prop === 'box-shadow-color') {

                    $('[name="'+prop+'"]').minicolors('value', value);

                } else if (prop === 'border-style' || prop === 'text-align') {

                    $('[name="'+prop+'"][value="'+value+'"]').prop('checked',true);

                } else if (prop === 'visibility-xs' || prop === 'visibility-sm' || prop === 'visibility-md' || prop === 'visibility-lg' || prop === 'visibility-xl') {

                    $('[name="'+prop+'"]').prop('checked',value);

                } else if(prop === 'background-image') {

                    //console.log("BG IMAGE value=", value);
                    if ("none"==value) value = "";
                    else if (value.indexOf("url")==0) {
                        //vyparsuj url("http://iwcm.interway.sk/images/bg.jpg")
                        var url = value.replace(/"/gi, '');
                        url = url.replace(/'/gi, '');
                        var start = url.indexOf("(");
                        var end = url.indexOf(")");
                        if (start > 0 && end > start) {
                            url = url.substring(start+1, end);
                        }
                        if (url.indexOf("http")==0) {
                            start = url.indexOf("/", 9);
                            if (start > 0) url = url.substring(start);
                        }
                        value = url;
                    }
                    try { $('[name="'+prop+'"]').val(value).trigger("change"); } catch (e) {}

                } else {

                    try { $('[name="'+prop+'"]').val(value).trigger("change"); } catch (e) {}

                }

            });
        },

        get_current_element_style_id: function () {

            return this.user_style.current_element.attr(this.user_style.attr_name);
        },

        set_style_connections: function () {
            var me = this,
                style_id = me.get_current_element_style_id(),
                link,
                counter = 1;

            $('.style-connections-list').html('');

            if(typeof style_id === 'undefined') {
                return;
            }

            $.each($('['+me.user_style.attr_name+'="'+style_id+'"]'), function(index,el) {

                if($(el).hasClass(me.state.is_styling)) {
                    return;
                }

                link = $('<div class="'+me.tag.connection_reference+'">');
                link.data('connection-reference', $(el));
                link.html(counter+'. prepojenie');

                $('.style-connections-list').append(link);

                $(el).addClass(me.state.has_same_style);
                $(el).addClass(me.state.is_style_connected);

                ++counter;
            });

            if($('.style-connections-list').children(me.tagc.connection_reference).length < 1) {
                $('.style-connections-list').html('<iwcm:text key="pagebuilder.connections.none"/>');
            }

        },

        set_same_style_id: function (el) {

            var me = this,
                parent = me.get_parent_grid_element(el),
                clicked_style_id = $(parent).attr(me.user_style.attr_name),
                new_style_id = me.user_style.unique_selector();

            if($(parent).hasClass(me.state.has_same_style)) {

                $(parent).removeClass(me.state.has_same_style);
                $(parent).removeClass(me.state.is_style_connected);
                $(parent).attr(me.user_style.attr_name,new_style_id);

                if($('style[style-id="'+clicked_style_id+'"]').length > 0) {

                    var style_element = $('style[style-id="'+clicked_style_id+'"]').clone(),
                        new_html = $(style_element).html().replace(clicked_style_id,new_style_id);

                    $(style_element).html(new_html);
                    $(style_element).attr('style-id',new_style_id);
                    $(style_element).appendTo(this.$wrapper);
                }

            } else {

                $(parent).addClass(me.state.has_same_style);
                $(parent).addClass(me.state.is_style_connected);

                var style_id = me.get_current_element_style_id();

                $(me.user_style.current_element).removeClass(me.state.is_style_connected);
                $(parent).attr(me.user_style.attr_name,style_id);

                if($('style[style-id="'+clicked_style_id+'"]').length) {
                    $('style[style-id="'+clicked_style_id+'"]').unbind().off().remove();
                }

            }

            me.set_style_connections();

        },

        /*==================================================================
        /*====================|> BIND EVENTS
        /*=================================================================*/

        bind_events: function() {
            var me = this;

            me.$wrapper.on('click', me.tagc.toolbar, function(e) {
                if (e.target !== this) { return; }
                me.toggle_toolbar_visibility($(this));
            });

            me.$wrapper.on('click', me.tagc.toolbar_button_move, function() {
                me.duplicate = false;
                me.allow_move_grid_element($(this));
            });

            me.$wrapper.on('click', me.tagc.toolbar_button_add_to_favorites, function() {
                me.add_to_favorites($(this));
            });

            me.$wrapper.on('click', me.tagc.toolbar_button_duplicate, function() {
                me.duplicate = true;
                me.allow_move_grid_element($(this));
            });

            me.$wrapper.on('click', me.tagc.toolbar_button_remove, function() {
                me.remove_grid_element($(this));
            });

            me.$wrapper.on('click', me.tagc.toolbar_button_resize, function() {
                me.allow_resize_columns($(this));
            });

            me.$wrapper.on('click', me.tagc.toolbar_button_style, function() {
                me.open_modal($(this));
            });

            me.$wrapper.on('click', me.tagc.modal_footer_button_close, function() {
                me.close_modal($(this));
            });

            me.$wrapper.on('click', me.tagc.modal_footer_button_reset, function() {
                me.reset_modal($(this));
            });

            me.$wrapper.on('click', me.tagc.modal_footer_button_save, function() {
                me.save_modal($(this));
            });

            me.$wrapper.on('change', me.tagc.style_input, function() {
                me.set_new_style();
            });

            me.$wrapper.on('click', me.tagc.connection_button, function() {
                me.set_same_style_id($(this));
            });

            me.$wrapper.on('click', me.tagc.notify_footer_button, function() {
                me.cancel_move_grid_element();
                me.cancel_resize_columns();
                me.hide_notify();
            });

            me.$wrapper.on('click', me.tagc.library_footer_button, function() {
                me.hide_library();
            });

            me.$wrapper.on('click', me.tagc._plus_button, function(e) {
                if($(me.$wrapper).hasClass(me.state.is_moving_child)) {
                    me.move_grid_element_here($(this));
                } else {
                    me.listen_for_shift_key(e);
                    me.add_new_grid_element($(this));
                }
            });

            me.$wrapper.on('mouseover', me.tagc.append, function() {
                var parent = me.get_parent_grid_element($(this));
                $(parent).addClass("som-hover-append");
            });

            me.$wrapper.on('mouseout', me.tagc.append, function() {
                var parent = me.get_parent_grid_element($(this));
                $(parent).removeClass("som-hover-append");
            });

            me.$wrapper.on('mouseover', me.tagc.prepend, function() {
                var parent = me.get_parent_grid_element($(this));
                $(parent).addClass("som-hover-prepend");
            });

            me.$wrapper.on('mouseout', me.tagc.prepend, function() {
                var parent = me.get_parent_grid_element($(this));
                $(parent).removeClass("som-hover-prepend");
            });

            me.$wrapper.on('click', me.tagc.empty_placeholder_button, function(e) {
                if($(me.$wrapper).hasClass(me.state.is_moving_child)) {
                    me.move_grid_element_here($(this));
                } else {
                    me.listen_for_shift_key(e);
                    me.add_new_grid_element($(this));
                }
            });

            me.$wrapper.on('click', me.tagc.size_changer_up, function(e) {
                me.listen_for_shift_key(e);
                me.change_column_size_up($(this));
            });

            me.$wrapper.on('click', me.tagc.size_changer_down, function(e) {
                me.listen_for_shift_key(e);
                me.change_column_size_down($(this));
            });

            $(document).keyup(function(e) {
                me.listen_for_esc_key(e);
                me.disable_after_esc_pressed();
            });
        },

        /*==================================================================
        /*====================|> UNBIND EVENTS
        /*=================================================================*/

        unbind_events: function() {

            var me = this;
            me.$wrapper.unbind().off('.'+me._name);
            $(me.tagc.size_changer_down).unbind().off();
            $(me.tagc.size_changer_up).unbind().off();
            $(me.tagc.empty_placeholder_button).unbind().off();
            $(me.tagc._plus_button).unbind().off();
            $(me.tagc.notify_footer_button).unbind().off();
            $(me.tagc.connection_button).unbind().off();
            $(me.tagc.style_input).unbind().off();
            $(me.tagc.modal_footer_button_save).unbind().off();
            $(me.tagc.modal_footer_button_reset).unbind().off();
            $(me.tagc.modal_footer_button_close).unbind().off();
            $(me.tagc.toolbar_button_style).unbind().off();
            $(me.tagc.toolbar_button_resize).unbind().off();
            $(me.tagc.toolbar_button_remove).unbind().off();
            $(me.tagc.toolbar_button_duplicate).unbind().off();
            $(me.tagc.toolbar_button_add_to_favorites).unbind().off();
            $(me.tagc.toolbar_button_move).unbind().off();
            $(me.tagc.toolbar).unbind().off();
        },

        /*==================================================================
        /*====================|> REMOVE ELEMENTS
        /*=================================================================*/

        remove_elements: function (clone) {

            var me = this,
                wrapper = me.tagc.wrapper;

            if (typeof clone !== 'undefined') {
                wrapper = clone;
            }

            var $wrapper = $(wrapper);

            $wrapper.find(me.tagc.toolbar).remove();
            $wrapper.find(me.tagc._plus_button).remove();
            $wrapper.find(me.tagc._highlighter).remove();
            $wrapper.find(me.tagc.dimmer).remove();
            $wrapper.find(me.tagc.empty_placeholder).remove();
            $wrapper.find(me.tagc.size_changer).remove();
            $wrapper.find(me.tagc.connection_button).remove();
            $wrapper.find(me.tagc.notify).remove();
            $wrapper.find(me.tagc.modal).remove();
            $wrapper.find(me.tagc.library).remove();

            //console.log("remove_elements, html=", $wrapper.html());

            if (typeof clone !== 'undefined') {
                return $wrapper;
            }

        },

        /*==================================================================
        /*====================|> REMOVE ATTRIBUTES
        /*=================================================================*/

        remove_attributes: function (clone) {

            var me = this,
                wrapper = me.tagc.wrapper;

            if (typeof clone !== 'undefined') {
                wrapper = clone;
            }

            $(wrapper).find(me.tagc.column).removeAttr(me.tag.column);

            $.each(me.column.valid_prefixes, function(index, class_name) {
                $(wrapper).find(me.tagc.column).removeAttr(me.column.attr_prefix+class_name);
            });

            // <%--console.log("wrapper: ", wrapper, " editable_content: ", me.tag.editable_content, " me.tagc._grid_element: ", me.tagc._grid_element);--%>

            // <%--console.log("$(wrapper).find(me.tagc._grid_element): ", $(wrapper).find(me.tagc._grid_element));--%>

            $(wrapper).find(me.tagc._grid_element).children().removeClass(me.tag.column_content).removeClass(me.tag.editable_content);

            $(wrapper).find(me.tagc._grid_element)
                .removeClass(me.tag.section)
                .removeClass(me.tag.container)
                .removeClass(me.tag.row)
                .removeClass(me.tag.column)
                .removeClass(me.tag.editable_section)
                .removeClass(me.tag.editable_container)
                .removeClass(me.tag.editable_element)
                .removeClass(me.tag._grid_element);

            $(wrapper).removeClass(me.tag.wrapper);

            if (typeof clone !== 'undefined') {
                return $(wrapper);
            }

        },

        /*==================================================================
        /*====================|> DEFAULT SETTINGS
        /*=================================================================*/

        get_defaults: function() {
            return {
                prefix: 'pb',
                max_col_size: 12,
                template_group_id: 0,
                grid: '',
                onNewElementAdded: function () {
                    //console.log('Novy element bol pridany');
                    WJ.fireEvent("WJ.PageBuilder.newElementAdded");
                },
                onElementDuplicated: function () {
                    //console.log('Element bol duplikovany');
                    WJ.fireEvent("WJ.PageBuilder.elementDuplicated");
                },
                onElementMoved: function () {
                    //console.log('Element bol presunuty');
                    WJ.fireEvent("WJ.PageBuilder.elementMoved");
                },
                onGridChanged: function () {
                    //console.log('Grid bol zmeneny');
                    WJ.fireEvent("WJ.PageBuilder.gridChanged");
                },
                onElementRemoved: function (el, parent) {
                    //console.log('Grid bol zmeneny');
                    WJ.fireEvent("WJ.PageBuilder.elementRemoved", {
                        el: el,
                        parent: parent
                    });
                },
                template_basic_containers_sizes: [] // <%-- dopocitava sa automaticky z max_col_size vo funkcii generate_default_options--%>
            };
        },

        generate_default_options: function(){
            var me = this;

            // <%--// template_basic_containers_sizes--%>
            if(me.options.template_basic_containers_sizes.length==0){
                var max_col_size = me.options.max_col_size;
                me.options.template_basic_containers_sizes.push([max_col_size]);
                me.options.template_basic_containers_sizes.push([max_col_size / 2, max_col_size / 2]);
                me.options.template_basic_containers_sizes.push([max_col_size / 3, max_col_size / 3, max_col_size / 3]);
                me.options.template_basic_containers_sizes.push([max_col_size / 4, max_col_size / 4, max_col_size / 4, max_col_size / 4]);
                me.options.template_basic_containers_sizes.push([max_col_size / 6, max_col_size / 6, max_col_size / 6, max_col_size / 6, max_col_size / 6, max_col_size / 6]);
                me.options.template_basic_containers_sizes.push([(max_col_size / 3) * 2, max_col_size / 3]);
                me.options.template_basic_containers_sizes.push([max_col_size / 3, (max_col_size / 3) * 2]);
            }
        }

    });

    $.fn[ pluginName ] = function ( options ) {
        this.each(function() {
            if ( !$.data( this, 'plugin_' + pluginName ) ) {
                $.data( this, 'plugin_' + pluginName, new Plugin( this, options ) );
            }
        });
        return this;
    };

})( jQuery, window, document );