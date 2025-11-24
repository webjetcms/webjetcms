<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.editor.*,sk.iway.iwcm.i18n.Prop,sk.iway.iwcm.common.WriteTagToolsForCore"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" perms="menuWebpages|cmp_news"/>

<style type="text/css">
    body>div.header, body>header, body>div.footer, body>footer { display: none; }
</style>

<%
//get language from administration, not from page
String lng = Prop.getLng(request, false);
pageContext.setAttribute("lng", lng);

DocDetails doc = (DocDetails)request.getAttribute("docDetails");

InlineEditor.EditingMode editingMode = InlineEditor.getEditingMode(request);
String pbPrefix = Constants.getString("pageBuilderPrefix", "pb");

%>
<%
if (editingMode == InlineEditor.EditingMode.pageBuilder) { %>
    <!-- NINJA PAGE BUILDER -->

    <script>
        window.CKEDITOR_BASEPATH = "/admin/skins/webjet8/ckeditor/dist/";
    </script>
    <iwcm:combine type="css" set="adminInlineCss"/>
    <iwcm:combine type="js" set="adminInlineJs"/>

    <!-- /NINJA PAGE BUILDER -->
    <style>
        p.text-right { text-align: right; }
        p.text-center { text-align: center; }
        p.text-justify { text-align: justify; }

        .wj-toolbar-panel .wj-content-line { top: 0px; }
        #inlineEditorToolbarTop { height: 91px; z-index: 102000; }
        .cke_dialog_container {
            z-index: 102010 !important;
        }

        .cke_dialog_footer_buttons a.cke_dialog_ui_button {
            background-color: transparent;
            border-radius: 6px;
            color: #13151B;
            border: 1px solid #13151B;
            box-shadow: none;
            height: 36px;
            line-height: 36px;
        }
        .cke_dialog_footer_buttons a.cke_dialog_ui_button:hover {
            background-color: #13151B;
            color: #fff;
        }
        .cke_dialog_footer_buttons a.cke_dialog_ui_button_ok {
            background-color: #0063FB;
            border-radius: 6px;
            color: white;
            border: 1px solid #0063FB;
            height: 36px;
            line-height: 36px;
        }
        .cke_dialog_footer_buttons a.cke_dialog_ui_button_ok:hover {
            background-color: #0054d5;
            border-color: #0054d5;
        }
    </style>
<% } %>

<script>
    var templateGroupId = ${ninja.temp.group.templatesGroupBean.templatesGroupId};
    var ckEditorInstance = null;
    function getCkEditorInstance()
    {
        return ckEditorInstance;
    }

    function initializePageBuilder() {
        let pageDiv = $("#wjInline-docdata");

        $("body").addClass("is-edit-mode");

        pageDiv.data('ckEditorInitialized','true');
        pageDiv.ninjaPageBuilder({
            max_col_size: <%=Constants.getInt("bootstrapColumns", 12)%>,
            prefix: "<%=pbPrefix%>",
            //toto sa berie z inline_script.jsp kde sa hodnota inicializuje
            template_group_id: templateGroupId,
            <%
            String pageBuilderGrid = Constants.getString("pageBuilderGrid");
            if (Tools.isNotEmpty(pageBuilderGrid))
            {
                %>grid: { <%=pageBuilderGrid%> },<%
            }
            %>

            onGridChanged: function() {
                //console.log('Element bol zmeneny');
                var changedElement = pageDiv.data('plugin_ninjaPageBuilder').getChangedElement();
                //console.log(changedElement);
                //console.log($(changedElement).html());
                //console.log($(changedElement).find("div.npb-column__content"));
                //musime zrusit oznacenie editableElement lebo po naklonovani sa skopiruje aj toto a potom sa nam neinicializuje ckeditor
                $(changedElement).find("div.<%=pbPrefix%>-column__content").removeClass("editableElement");
                initPageBuilderEditors(pageDiv);
                WJ.dispatchEvent("WJ.PageBuilder.gridChanged");
            },
        });

        initPageBuilderEditors(pageDiv);
    }

    function initPageBuilderEditors(pageDiv)
    {
        <%--var editableElements = pageDiv.find("* [class*='npb-column__content']");--%>
        //console.log("initPageBuilderEditors, pageDiv=", pageDiv);
        var editableElements = pageDiv.find("*[class*='<%=pbPrefix%>-editable'], *[class*='<%=pbPrefix%>-content']");
        editableElements.each(function()
        {
            //console.log("Has class editableElement: "+$(this).hasClass("editableElement"), this);
            var $this = $(this);
            var $that = $this;

            //na tomto elemente je to uz inicializovane, preskocme
            if ($this.hasClass("editableElement")) return;

            this.form = document.editorForm;

            //console.log(this);
            $this.attr('contenteditable','true');
            $this.addClass('editableElement');

            //console.log("Initializing editor on element:", $this);

            var mainWindow = window.parent;
            //window.CKEDITOR = mainWindow.CKEDITOR;
            window.userLng = mainWindow.userLng;
            window.WJ = mainWindow.WJ;
            //BEWARE: this must be here, because WJ is binded to window in parent frame
            window.WJ.fireEvent = function(name, detail) {
                //console.log("Firing event: ", name, detail, "window=", window.location.href);
                //firni event
                const event = new CustomEvent(name, {
                    detail: detail
                });
                //console.log('Dispatching event, name=', name, 'detail=', detail);
                window.dispatchEvent(event);
            }
            var DatatablesCkEditor = mainWindow.datatablesCkEditorModule.DatatablesCkEditor;

            //console.log("DatatablesCkEditor=", DatatablesCkEditor, "window=", window);

            var wjeditor = null;
            const options = {
                datatable: null,
                fieldid: $that.attr("id"),
                ckEditorElementId: $that[0],
                ckEditorInitFunction: CKEDITOR.inline,
                ckEditorConfig: "/admin/skins/webjet8/ckeditor/config.jsp?inline=true&inlineMode=pageBuilder",
                ckEditorObject: CKEDITOR,
                window: window,
                constants: {
                    pixabayEnabled: mainWindow.pixabayEnabled,
                    editorImageAutoTitle: mainWindow.editorImageAutoTitle
                },
                lang: {
                    projectName: mainWindow.WJ.translate("default.project.name")
                },
                onReady: function(instance) {
                    //console.log("onReady, instance=", instance, "wjeditor=", wjeditor);
                    //setStylesDef(window.editorStyles, ckEditorInstanceInitialized);
                    WJ.dispatchEvent("WJ.PageBuilder.instanceReady", {ckinstance: instance});
                    //setTimeout(()=> {
                        wjeditor.setStyleComboList(window.editorStyles);
                        //setStylesDef(window.editorStyles, instance);
                    //}, 100);
                }
            };
            wjeditor = new DatatablesCkEditor(options);
            //console.log("wjeditor=", wjeditor);

            $(this).attr("data-ckeditor-instance", wjeditor.ckEditorInstance.name);

            //inicializuj ckEditor
            /*var ckEditorInstanceInitialized = initializeCkEditorImpl(this, CKEDITOR.inline, "/admin/skins/webjet8/ckeditor/config.jsp?inline=true&inlineMode=pageBuilder");
            $(this).attr("data-ckeditor-instance", ckEditorInstanceInitialized.name);

            //nastav CSS styly
            ckEditorInstanceInitialized.on("instanceReady", function() {
                //console.log("instanceReady");
                setStylesDef(window.editorStyles, ckEditorInstanceInitialized);
                WJ.fireEvent("WJ.PageBuilder.instanceReady", {ckinstance: ckEditorInstanceInitialized});
            });*/
        });
    }

    function getSaveData()
    {
        //console.log("Som getSaveData()");

        var saveData = {
            editable : []
        };

        $("[data-wjapp='pageBuilder']").each(function(index)
        {
            var docId = $(this).data("wjappkey");
            var wjApp = $(this).data("wjapp");
            var wjAppField = $(this).data("wjappfield");
            if (wjAppField === undefined) wjAppField = "";

            //var node = $(this).clone();
            //console.log($(this));

            if ($(this).data('plugin_ninjaPageBuilder') === undefined)
            {
                //console.log("PageBuilder["+wjAppField+"] is not defined, skipping");
                return;
            }

            var pageBuilderInstance = $(this).data('plugin_ninjaPageBuilder');
            var node = pageBuilderInstance.getClearNode();
            //console.log(node);

            //console.log("Node html:");
            //console.log(node.html());

            var editableElements = node.find("*[class*='editableElement']");
            editableElements.each(function()
            {
                var editorName =  $(this).attr("data-ckeditor-instance");
                //console.log("editorName=", editorName);
                    //console.log($(this));
                //console.log($(this).html());

                var editorData = CKEDITOR.instances[editorName].getData();
                $(this).html(editorData);
            });

            //console.log("Before unwrap HTML:", node.html());
            pageBuilderInstance.clearEditorAttributes(node);

            var htmlCode = node.html();

            htmlCode = htmlCode.replace(/data-<%=pbPrefix%>-toggle="/gi, 'data-bs-toggle="');

            //console.log("Final HTML: ", htmlCode);

            var item = {wjApp: wjApp, wjAppKey: docId, wjAppField: wjAppField, data: htmlCode};
            saveData.editable.push(item);
        });

        return saveData;
    }

    function getWysiwygEditors(wjAppField) {
        //returns array of all editors HTML code
        let wysiwygEditors = [];

        $("[data-wjapp='pageBuilder']").each(function(index)
        {
            if (wjAppField != $(this).data("wjappfield")) return;

            if ($(this).data('plugin_ninjaPageBuilder') === undefined)
            {
                console.log("PageBuilder is not defined, skipping");
                return;
            }

            var pageBuilderInstance = $(this).data('plugin_ninjaPageBuilder');
            var node = pageBuilderInstance.getClearNode();

            var editableElements = node.find("*[class*='editableElement']");
            editableElements.each(function()
            {
                var editorName =  $(this).attr("data-ckeditor-instance");
                var editor = CKEDITOR.instances[editorName];
                wysiwygEditors.push(editor);
            });
        });
        return wysiwygEditors;
    }

    /**
     * Returns ARRAY of content in all editors in wjAppField
     */
    function getEditorsContent(wjAppField) {
        //returns array of all editors HTML code
        let editorsContent = [];

        $("[data-wjapp='pageBuilder']").each(function(index)
        {
            if (wjAppField != $(this).data("wjappfield")) return;

            if ($(this).data('plugin_ninjaPageBuilder') === undefined)
            {
                //console.log("PageBuilder is not defined, skipping");
                return;
            }

            var pageBuilderInstance = $(this).data('plugin_ninjaPageBuilder');
            var node = pageBuilderInstance.getClearNode();

            var editableElements = node.find("*[class*='editableElement']");
            editableElements.each(function()
            {
                var editorName =  $(this).attr("data-ckeditor-instance");
                var editorData = CKEDITOR.instances[editorName].getData();
                editorsContent.push(editorData);
            });
        });
        return editorsContent;
    }

    function markPbElements(wjAppField) {
        $("[data-wjapp='pageBuilder']").each(function(index)
        {
            if (wjAppField != $(this).data("wjappfield")) return;

            if ($(this).data('plugin_ninjaPageBuilder') === undefined)
            {
                //console.log("PageBuilder is not defined, skipping");
                return;
            }

            var pageBuilderInstance = $(this).data('plugin_ninjaPageBuilder');
            pageBuilderInstance.mark_grid_elements();

            initPageBuilderEditors($(this));
        });
    }

    /**
     * Set content in all editors in wjAppField from editorsContent (ARRAY). If i is set it will set only i editor.
     */
    function setEditorsContent(wjAppField, editorsContent, i = null) {
        //returns array of all editors HTML code
        $("[data-wjapp='pageBuilder']").each(function(index)
        {
            if (wjAppField != $(this).data("wjappfield")) return;

            if ($(this).data('plugin_ninjaPageBuilder') === undefined)
            {
                //console.log("PageBuilder is not defined, skipping");
                return;
            }

            var pageBuilderInstance = $(this).data('plugin_ninjaPageBuilder');
            var node = pageBuilderInstance.getClearNode();

            var editableElements = node.find("*[class*='editableElement']");
            var counter = 0;
            editableElements.each(function()
            {
                var editorName =  $(this).attr("data-ckeditor-instance");
                var editor = CKEDITOR.instances[editorName];
                if (i==null) editor.setData(editorsContent[counter]);
                else if (i==counter) editor.setData(editorsContent);
                counter++;
            });
        });
    }

    $(document).ready(()=> {

        CKEDITOR.disableAutoInline = true;
        CKEDITOR.dtd.$editable.span = 1
        CKEDITOR.dtd.$editable.a = 1
        var ckEditorAutofocusInstanceName = null;

        CKEDITOR.on('instanceReady', function(ev) {
            //console.log("Ckeditor "+ev.editor.name+" instance ready");
            //console.log(ev);
            ckEditorInstance = ev.editor;

                //ev.editor.on("blur", function(event) { console.log("blured"); $("#wjInlineCkEditorToolbarElement").hide(); });
            ev.editor.on("focus", function(event)
            {
                //console.log("editor focused, name="+event.editor.name);
                ckEditorInstance = event.editor;
            });

            if (ckEditorAutofocusInstanceName != null && ckEditorAutofocusInstanceName == ev.editor.name)
            {
                ev.editor.focus();
            }
        });

        initializePageBuilder();

        document.addEventListener("keydown", function(e) {
            //zachytenie CTRL+S/CMD+S pre ulozenie stranky
            if ((window.navigator.platform.match("Mac") ? e.metaKey : e.ctrlKey)  && e.key === 's') {
                e.preventDefault();
                //console.log("Dispatching WJ.DTE.save");
                try {
                    window.parent.WJ.dispatchEvent("WJ.DTE.save", {});
                } catch (ex) {}
            }
        }, false);
    });

    window.editorStyles = <%=JsonTools.objectToJSON(sk.iway.iwcm.editor.service.EditorService.getCssListJson(doc))%>;

    //otvorenie dialogoveho okna IE/Mozilla - based on FCKDialog
    var WJDialog = new Object() ;
    var WJDialogArguments = null;

    // This method opens a dialog window using the standard dialog template.
    WJDialog.OpenDialog = function( dialogName, dialogTitle, dialogPage, width, height, customValue, parentWindow )
    {
        // Setup the dialog info.
        var oDialogInfo = new Object() ;
        oDialogInfo.Title = dialogTitle ;
        oDialogInfo.Page = dialogPage ;
        oDialogInfo.Editor = window ;
        oDialogInfo.CustomValue = customValue ;		// Optional

        //window.alert("WJDialog");

        window.FCKLang = new Object();
        window.FCKConfig = new Object();
        //link dialog setup
        window.FCKConfig.LinkDlgHideAdvanced = true;

        var sUrl = '/admin/dialogframe.jsp' ;
        this.Show( oDialogInfo, dialogName, sUrl, width, height, parentWindow ) ;
    }

    WJDialog.Show = function( dialogInfo, dialogName, pageUrl, dialogWidth, dialogHeight, parentWindow )
    {
        if (navigator.userAgent.indexOf("MSIE")!=-1)
        {
        if ( !parentWindow )
            parentWindow = window ;

            parentWindow.showModalDialog( pageUrl, dialogInfo, "dialogWidth:" + dialogWidth + "px;dialogHeight:" + dialogHeight + "px;help:no;scroll:no;status:no") ;
            return;
        }

        var iTop  = (screen.height - dialogHeight) / 2 ;
        var iLeft = (screen.width  - dialogWidth)  / 2 ;

        if (iLeft < 0) iLeft = 0;
        if (iTop < 0) iTop = 0;

        if (navigator.userAgent.indexOf("Firefox/")!=-1)
        {
            dialogHeight = dialogHeight + 62;
        }
        else if (navigator.userAgent.indexOf("Trident/")!=-1)
        {
            dialogHeight = dialogHeight + 35;
        }

        var sOption  = "location=no,menubar=no,resizable=no,toolbar=no,dependent=yes,dialog=yes,minimizable=no,modal=yes,alwaysRaised=yes" +
            ",width="  + dialogWidth +
            ",height=" + dialogHeight +
            ",top="  + iTop +
            ",left=" + iLeft ;

        if ( !parentWindow )
            parentWindow = window ;

        var oWindow = parentWindow.open( pageUrl, 'FCKEditorDialog_' + dialogName, sOption, true ) ;
        //window.alert("iLeft="+iLeft+" iTop="+iTop);

        if (navigator.userAgent.indexOf("Chrome")==-1)
        {
            //ked tu bolo toto, Chrome zle pozicioval okno
            oWindow.moveTo( iLeft, iTop ) ;
            if (dialogWidth>0 && dialogHeight>0) oWindow.resizeTo( dialogWidth, dialogHeight ) ;
        }

        oWindow.focus() ;
        oWindow.dialogArguments = dialogInfo;

        //oWindow.location.href = pageUrl ;
        WJDialogArguments = dialogInfo;

        // On some Gecko browsers (probably over slow connections) the
        // "dialogArguments" are not set to the target window so we must
        // put it in the opener window so it can be used by the target one.
        parentWindow.FCKLastDialogInfo = dialogInfo ;

        this.Window = oWindow ;

        // Try/Catch must be used to avoit an error when using a frameset
        // on a different domain:
        // "Permission denied to get property Window.releaseEvents".
        try
        {
            /*parentWindow.captureEvents( Event.CLICK | Event.MOUSEDOWN | Event.MOUSEUP | Event.FOCUS ) ;
            parentWindow.top.addEventListener( 'mousedown', this.CheckFocus, true ) ;
            parentWindow.top.addEventListener( 'mouseup', this.CheckFocus, true ) ;
            parentWindow.top.addEventListener( 'click', this.CheckFocus, true ) ;
            parentWindow.top.addEventListener( 'focus', this.CheckFocus, true ) ;*/

            window.captureEvents( Event.CLICK | Event.MOUSEDOWN | Event.MOUSEUP | Event.FOCUS ) ;
            window.addEventListener( 'mousedown', this.CheckFocus, true ) ;
            window.addEventListener( 'mouseup', this.CheckFocus, true ) ;
            window.addEventListener( 'click', this.CheckFocus, true ) ;
            window.addEventListener( 'focus', this.CheckFocus, true ) ;
        }
        catch (e)
        {}
    }

    WJDialog.CheckFocus = function()
    {
        //WJDialog.Window.status = "check focus: " + new Date();

        // It is strange, but we have to check the WJDialog existence to avoid a
        // random error: "WJDialog is not defined".
        if ( typeof( WJDialog ) != "object" )
            return ;

        if ( WJDialog.Window && !WJDialog.Window.closed )
        {
        try
        {
                //WJDialog.Window.focus();
                WJDialog.Window.document.getElementById('frmMain').contentWindow.focus();
            }
            catch (e)
            {}
            //WJDialog.Window.status = WJDialog.Window.location.href + " " + new Date();
            return false ;
        }
        else
        {
            //WJDialog.Window.status = "XXX: " + new Date();

            // Try/Catch must be used to avoit an error when using a frameset
            // on a different domain:
            // "Permission denied to get property Window.releaseEvents".
            try
            {
                window.top.releaseEvents(Event.CLICK | Event.MOUSEDOWN | Event.MOUSEUP | Event.FOCUS) ;
                window.top.parent.removeEventListener( 'onmousedown', FCKDialog.CheckFocus, true ) ;
                window.top.parent.removeEventListener( 'mouseup', FCKDialog.CheckFocus, true ) ;
                window.top.parent.removeEventListener( 'click', FCKDialog.CheckFocus, true ) ;
                window.top.parent.removeEventListener( 'onfocus', FCKDialog.CheckFocus, true ) ;
            }
            catch (e)
            {}
        }
    }

    function openElFinderDialogWindow(form, elementName, requestedImageDir, volume="all")
    {
        var url = '/admin/elFinder/dialog.jsp';

        if (form != null && elementName != null) {
            url = url + "?form=" + form;
            url = url + "&elementName=" + encodeURIComponent(elementName);
            url = url + "&volumes=" + encodeURIComponent(volume);

            try {
                var link = null;
                if ("ckEditorDialog"==form)
                {
                    var dialog = CKEDITOR.dialog.getCurrent();
                    var tabNamePair = elementName.split(":");
                    var element = dialog.getContentElement(tabNamePair[0], tabNamePair[1]);
                    link = element.getValue();
                }
                else {
                    link = document.forms[form].elements[elementName].value;
                }
                if (link != null && link!=""){
                    url = url + "&link=" + encodeURIComponent(link);
                }else  if (requestedImageDir!=undefined && requestedImageDir!=null && requestedImageDir!="") url += '&link=' + requestedImageDir;
            } catch (e) { console.log(e); }
        }
        //window.alert(navigator.userAgent);
        WJDialog.OpenDialog( 'WJDialog_Image' , "Image", url, 820, 604);
    }
    function openImageDialogWindow(formName, fieldName, requestedImageDir)
    {
        //console.log("openImageDialogWindow, formName=", formName, "fieldName=", fieldName, "requestedImageDir=", requestedImageDir);
        openElFinderDialogWindow(formName, fieldName, requestedImageDir, "images");
    }

    function openLinkDialogWindow(formName, fieldName, requestedImageDir, requestedFileDir)
    {
        openElFinderDialogWindow(formName, fieldName, null, "link");
    }
</script>
<%
int inlineDocId = doc.getDocId();
String inlineTitle = doc.getTitle();
if ("true".equals(request.getParameter("inlineEditingNewPage"))) {
    inlineDocId = -1;
    inlineTitle = (String)request.getAttribute("title");
}
%>
<div id="inlineEditorToolbarTop" class="wj-toolbar-panel">
    <div class="wj-content-line" id="wjInlineCkEditorToolbarOffsetElement">
        <form name="editorForm" id="editorFormId" class="donotobfuscate" method="post">
            <input type="hidden" name="docId" value="<%=inlineDocId%>"/>
            <input type="hidden" name="groupId" value="<%=doc.getGroupId()%>"/>
            <input type="hidden" name="title" value="<%=inlineTitle%>"/>
            <input type="hidden" name="virtualPath" value="<%=doc.getVirtualPath()%>"/>

            <div id='wjInlineCkEditorToolbarElement'></div>
        </form>
    </div>
</div>
<div id="inlineEditorToolbarTopPlaceHolder"></div>
<div id="wjInline-docdata" data-wjapp="pageBuilder" data-wjappkey="<%=doc.getDocId()%>" data-wjapptemp="<%=doc.getTempId()%>" data-wjappfield="doc_data">
    <% if ("true".equals(request.getParameter("inlineEditingNewPage"))) { %>
        <p><iwcm:text key='editor.newDocumentName'/></p>
    <%
    } else {
        out.println(doc.getData());
    }
    %>
</div>