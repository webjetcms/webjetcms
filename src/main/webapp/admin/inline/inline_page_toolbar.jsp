<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.*"%>
<%@page import="sk.iway.iwcm.common.DocTools"%>
<%@page import="sk.iway.iwcm.doc.*"%>
<%@page import="sk.iway.iwcm.doc.groups.GroupsController"%>
<%@page import="sk.iway.iwcm.editor.EditorDB"%>
<%@page import="sk.iway.iwcm.editor.EditorForm"%>
<%@page import="sk.iway.iwcm.editor.InlineEditor"%>
<%@page import="sk.iway.iwcm.gallery.GalleryDB"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@
        taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
        taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
        taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
        taglib prefix="form" uri="http://www.springframework.org/tags/form"%><%@
        taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
        taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
        taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
        taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%!
    public static String getUserPhotoPath(HttpServletRequest request)
    {
        String photoPath = "/admin/skins/webjet8/assets/global/img/wj/avatar.png";
        UserDetails ud = UsersDB.getCurrentUser(request);
        if (ud != null)
        {
            String photo = ud.getPhoto();
            if (Tools.isNotEmpty(photo))
            {
                photoPath = "/thumb" + GalleryDB.getImagePathSmall(photo) + "?w=50&h=50&ip=5";
            }
        }
        return photoPath;
    }
%><%
    boolean isGridEditorEnabled = Constants.getBoolean("gridEditorEnabled") || InlineEditor.getEditingMode(request)==InlineEditor.EditingMode.gridEditor;
    String bootstrapVersion = Constants.getString("bootstrapVersion","3");

    // TODO takyto subor v projekte uz ani nieje...
    if ("iwcm.interway.sk".equals(request.getServerName()) && "old".equals(Tools.getRequestParameter(request, "toolbar")))
    {
        pageContext.include("inline_page_toolbar.old.jsp");
        return;
    }

    String lng = PageLng.getUserLng(request);
    String lngAdmin = (String)session.getAttribute(Prop.SESSION_I18N_PROP_LNG);
    pageContext.setAttribute("lng", lngAdmin);
//out.println("lng="+lng+" lngAdmin="+lngAdmin);
//nastavme aj jazyk pre Admin cast podla jazyka sablony
//if ("cloud".equals(Constants.getInstallName())) session.setAttribute(Prop.SESSION_I18N_PROP_LNG, lng);

    PageParams pageParams = new PageParams(request);
    Prop prop = Prop.getInstance(request);

    Identity user = UsersDB.getCurrentUser(request);
    if (user == null || user.isAdmin()==false) return;

    int docId = Tools.getDocId(request);
    if (WriteTag.isInlinePageEditable(user, docId, request)==false) return;

    int groupId = Tools.getIntValue((String)request.getAttribute("group_id"), -1);

    int parentGroupId = groupId;
    GroupDetails group = (GroupDetails)request.getAttribute("pageGroupDetails");
    DocDetails doc = (DocDetails)request.getAttribute("docDetails");

    if (group != null) parentGroupId = group.getParentGroupId();

    boolean disableToolbar = DocTools.isXssStrictUrlException(PathFilter.getOrigPath(request), "inlineEditingToolbarDisabledUrls");


    if (doc == null)
    {
        return;
    }
    pageContext.setAttribute("doc", doc);

    boolean isCloud = "cloud".equals(Constants.getInstallName()) && InitServlet.isTypeCloud();
    boolean hasShop = sk.iway.iwcm.common.CloudToolsForCore.hasShop(request);

    boolean isPreview = "true".equals(Tools.getCookieValue(request.getCookies(), "wjPreview", "false"));
    isPreview=!Constants.getBoolean("inlineEditingEnabledDefaultValue");

    //WebJET Cloud podmienky
    boolean isPremiumPro = user.isEnabledItem("premiumPro");


    //integracia do datatables editora
    boolean inlineEditorAdmin = "true".equals(request.getParameter("inlineEditorAdmin"));
    if (inlineEditorAdmin) {
        isPreview = false;
        if (doc != null) {
            request.setAttribute("doc_data", "");
            org.json.JSONObject jsonData = new org.json.JSONObject();
            jsonData.put("doc_data", doc.getData());
            //out.println(jsonData.toString(4));
            %>
            <script type="text/javascript">
                window.inlineEditorDocData = <%=jsonData.toString(4)%>;
            </script>
            <%
        }
    }
%>
<%=Tools.insertJQuery(request) %>
<script type="text/javascript" src="/admin/scripts/common.jsp"></script>

<script type="text/javascript">
    var ninja = {};
    ninja.temp = {};
    ninja.temp.basePath = "${ninja.temp.basePath}";
    ninja.temp.basePathAssets = "${ninja.temp.basePathAssets}";
    ninja.temp.basePathCss = "${ninja.temp.basePathCss}";
    ninja.temp.basePathJs = "${ninja.temp.basePathJs}";
    ninja.temp.basePathPlugins = "${ninja.temp.basePathPlugins}";
    ninja.temp.basePathImg = "${ninja.temp.basePathImg}";

    function getStats(){

        if($(".stats .day").html()!="-"){
            return; // aby ajax zbehol iba 1 krat
        }

        var day = 0;
        var month = 0;
        $.getJSON( "/admin/inline/stat_ajax.jsp?docid=<%=docId%>", function( data ) {
            $.each( data, function( key, val ) {
                if(key=="month"){month=val;}
                else if(key=="day"){day=val;}
            });
            $(".stats .day").html(day);
            $(".stats .month").html(month);
        });
    }

    function inlinePageEditImpl(url, autoheight)
    {
        var toolbar = $("#inlineEditorToolbar");
        var pageDiv = toolbar.parent();
        if (pageDiv != null && pageDiv.html().indexOf( ("inlineEditingAl"+"lreadyLoaded") )!=-1)
        {
            pageDiv = pageDiv.parent();
        }

        if (pageDiv != null)
        {
            $("body").addClass("editorOn");
            $("#header").css("overflow", "hidden");

            var height = 0;
            if (autoheight) height = pageDiv.height();
            if (height < 300) height = 300;

            //window.alert(height);

            //pridaj velkost toolbarov
            height = height + 210 + 21;
            //rezerva na dalsi text
            var heightReserve = 100;
            height = height + heightReserve;

            <%
            String htmlEditorAppendStart = "";
            String htmlEditorAppendEnd = "";
            %>

            //var toolbarHtml = toolbar.html();
            var toolbarHtml = '<div class=\"inlineComponentButtonsWrapper\" style="display: none;"><div id="inlineEditorToolbar" class="inlineComponentButtons inlineComponentsEditorToolbar inlineEditingAl'+'lreadyLoaded"><a class="inlineComponentButton inlineComponentButtonPublish" href="javascript:publishPage()"><span><iwcm:text key="admin.m.publish"/></span></a><a class="inlineComponentButton" href="javascript:window.location.reload();" style="background-image:url(/components/_common/admin/inline/icon-delete.png)"><span><iwcm:text key="editor.inline.quitInlineEditing"/></span></a></div></div>';
            var html = toolbarHtml + "<%=htmlEditorAppendStart%><iframe id='webjetEditorIframe' src='"+url+"' width='100%' height='"+height+"' frameborder='0' ALLOWTRANSPARENCY='true'></iframe><%=htmlEditorAppendEnd%>";

            pageDiv.html(html);

            $("#inlineToolbarPublishPage").show();
            $("#inlineToolbarQuitEditing").show();
            $("#inlineToolbarSaveEditing").show();
            $("#inlineToolbarEditPage").hide();
        }
    }

    function inlinePageEditDocId(docId)
    {
        var url = "/admin/editor.do?inline=true&docid="+docId;
        inlinePageEditImpl(url, true);
    }

    function inlinePageEdit()
    {
        inlinePageEditInit(true);
    }

    function inlinePageEditInit(setReadOnly)
    {
        if (isFullpageNews == true)
        {
            if (setReadOnly) wjSetReadOnly(false);
            var firstBox = null;
            $("[data-wjapp='newsInline']").each(function(index)
            {
                var id = $(this).attr("id");
                //toto preskakujeme
                if (id.indexOf("wjInline-docdata")!=-1) return true;

                if (firstBox == null)
                {
                    firstBox = id;
                    ckEditorAutofocusInstanceName = id;

                    $(this).click();
                }
            });
        }
        else
        {
            if (setReadOnly)
            {
                wjSetReadOnly(false);
                //setTimeout(function() { $("#wjInline-docdata").click(); }, 2000);
            }
            else
            {
                setTimeout(function()
                {
                    //toto nastane ked sa loadne web ako preview a potom sa prepne do edit
                    if (ckEditorInstance==null)
                    {
                        //console.log("DOUBLE CLICK DOCDATA");
                        $("#wjInline-docdata").dblclick();
                    }
                }, 500);
            }
            //window.alert("Inline page edit");
        }
    }

    function editHeader()
    {
        popupPageEditDocId("<%=request.getAttribute("doc_header-docId=")%>");
    }

    function editLogoAndHeader()
    {
        $("#inlineToolbarAddButtonsSpace > a:first-child")[0].click();
        $("#inlineEditorToolbarTop ul ul").hide();
    }

    function editFooter()
    {
        popupPageEditDocId("<%=request.getAttribute("doc_footer-docId=")%>");
    }

    function editSideLeftMenu()
    {
        popupPageEditDocId("<%=request.getAttribute("doc_left_menu-docId=")%>");
    }

    function editSideMenu()
    {
        popupPageEditDocId("<%=request.getAttribute("doc_right_menu-docId=")%>");
    }

    function editStructure()
    {
        var url = "/components/menu/admin_edit_structure.jsp";
        var options = "toolbar=no,scrollbars=yes,resizable=yes,width=980,height=600;";
        window.open(url,"_blank",options);
    }
    <% if(isGridEditorEnabled){ %>
    function GridEditorInit(){
        var elem = $("#wjInline-docdata");
        if (!( elem.hasClass('ge-editing') )) {
            window.getCkEditorInstance().setReadOnly(true);
            elem.gridEditor({
                content_types: ['ckeditor'],
                bootstrap_version : <%=bootstrapVersion%>,
                ckeditor: {
                    config: {
                        on: {
                            instanceReady: function (evt) {
//                                var instance = this;
//                                console.log('instance ready', evt, instance);
                            }
                        }
                    }
                }
            });
        }else {
            GridEditorDeinit();
        }
    }
    function GridEditorDeinit(){
        var elem = $("#wjInline-docdata");
        if ( elem.hasClass('ge-editing') ) {
            elem.data('grideditor').deinit();
        }
        window.getCkEditorInstance().setReadOnly(false);
    }
    <% } %>
    function openSportModule()
    {
        var url = "/components/cloud/sportclubs";
        var options = "toolbar=no,scrollbars=yes,resizable=yes,width=1160,height=890;";
        window.open(url,"_blank",options);
    }

    // Zmazanie aktualnej stranky - neni este presne doriesene, kam sa presmerujem po odstraneni stranky
    function deleteActualPage()
    {
        if (window.confirm("<iwcm:text key="groupslist.do_you_really_want_to_delete"/>"))
        {
            // presmeruje na adresar v ktorom sa nachadzala stranka
            var redirect = '<%=doc.getVirtualPath().substring(0, doc.getVirtualPath().lastIndexOf('/')) %>';
            //var redirect = '/';
            $.post("/admin/webpages/ajax_jstreeoperations.jsp?act=delete", {id: "<%=groupId %>_<%=docId %>"} )
                .done(function() {
                    window.location.href =redirect;
                });
        }
    }


    // nastavi stranke parameter available
    function setPageAvailable(isAvailable){
        if(isAvailable==null){isAvailable = true;}
        $.post("/admin/inline/page_save.jsp?available="+isAvailable+"&docid=<%=docId %>&groupid=<%=groupId %>")
            .done(function(data) {
                //alert(data);
                location.reload();
            });
    }

    function popupPageEditDocId(docId)
    {
        var url = "/admin/editor.do?docid="+docId+"&quitLink=2";
        var options = "toolbar=no,scrollbars=yes,resizable=yes,width=1000,height=600;";
        window.open(url,"_blank",options);
    }

    function popupPageEdit()
    {
        popupPageEditDocId(<%=docId %>);
    }



    function inlineAddPage(groupId)
    {
        var url = "/admin/editor.do?inline=true&docid=-1&groupId="+groupId;
        inlinePageEditImpl(url, false);
    }

    function inlineEditDir()
    {
        var url = "<%=GroupsController.BASE_URL%><%=groupId%>?Edit=Edituj&singlePopup=true";

        var options = "toolbar=no,scrollbars=yes,resizable=yes,width=500,height=480;";
        popupWindow=window.open(url,"_blank",options);
    }

    function inlineAddDir()
    {
        var url = "<%=GroupsController.BASE_URL%>-1/?myParentGroupId=<%=parentGroupId%>&Add=Pridaj&singlePopup=true";

        var options = "toolbar=no,scrollbars=yes,resizable=yes,width=500,height=480;";
        popupWindow=window.open(url,"_blank",options);
    }

    function inlineAddTopDir()
    {
        var url = "<%=GroupsController.BASE_URL%>-1/?myParentGroupId=<%=sk.iway.iwcm.common.CloudToolsForCore.getRootGroupId(request) %>&Add=Pridaj&singlePopup=true";

        var options = "toolbar=no,scrollbars=yes,resizable=yes,width=500,height=480;";
        popupWindow=window.open(url,"_blank",options);
    }

    function inlineAddSubDir()
    {
        var url = "<%=GroupsController.BASE_URL%>-1/?myParentGroupId=<%=groupId%>&Add=Pridaj&singlePopup=true";

        var options = "toolbar=no,scrollbars=yes,resizable=yes,width=500,height=480;";
        popupWindow=window.open(url,"_blank",options);
    }

    function publishPage()
    {
        <% if(isGridEditorEnabled){%>
        GridEditorDeinit();
        <%}%>
        //$(".cke_button__inlinepublish").trigger('click');
        ckEditorInstance.execCommand("wjInlinePublish");
    }

    function savePage()
    {
        $("#webjetEditorIframe").contents().find("#btn-save").trigger('click');
    }


    function openPagePopup(page,width,height){

        if(width==null){width = 500;}
        if(height==null){height = 220;}

        switch(page)
        {
            <% if (isCloud==false) { %>
            case "elFinder":
                //var url = "/admin/inline/page_elfinder.jsp";
                var url = "/admin/elFinder";
                break;
            <% } %>
            case "pageHistory":
                var url = "/admin/dochistory.jsp?docid=<%=doc.getDocId() %>";
                break;
            case "report":
                var url = "/admin/inline/form_report_problem.jsp?actualPage=<%=request.getScheme()+"://"+DocDB.getDomain(request)+doc.getVirtualPath() %>";
                break;
            default:
                var url = page;
                break;
        }

        var options = "toolbar=no,scrollbars=yes,resizable=yes,width="+width+",height="+height+";";
        window.open(url,"_blank",options);
    }

    function openImageDialog(button, fieldName, requestedImageDir)
    {
        openElFinderDialogWindow('editorForm', fieldName)
    }

    function saveNewMedium(input){
        $.get( "/admin/inline/media_save_ajax.jsp",{ act:'save', docId: <%=doc.getDocId()%>, mediaThumbLink: $(input).val()}, function( data ) {
            getMediaList();
        });
    }

    function deleteMedia(mediaID){
        if (window.confirm("<iwcm:text key="components.media.confirm_delete"/>"))
        {
            $.get( "/admin/inline/media_save_ajax.jsp",{ act:'delete', mediaId:mediaID, docId: <%=doc.getDocId()%>}, function( data ) {
                getMediaList();
            });
        }
    }

    function getMediaList(){
        $.get( "/admin/inline/media_list_ajax.jsp",{ docId: <%=doc.getDocId()%>}, function( data ) {
            $('.mediaList').html(data);
        });
    }

    $(document).ready(function(){
        <% if (inlineEditorAdmin==false) { %>
        getMediaList();
        <% } %>
        <% if (isPreview) { %>
        wjSetReadOnly(true);
        <% } else { %>
        window.setTimeout(inlinePageEdit, 100);
        <% } %>
        $("body").addClass("page-builder");
        <% if (inlineEditorAdmin) {%>
            $("body").addClass("page-builder-in-admin");
        <% } %>
    });
</script>
<style type="text/css">
    div.inlineComponentEdit div.inlineComponentButtonsWrapper { }
    <jsp:include page="/admin/css/perms-css.jsp"/>
    div.inlineComponentButtons a.inlineComponentButton span { white-space: nowrap; }

    div.inlineComponentButtons.inlineEditorAdmin div.wj-control-line { display: none}
    div.inlineComponentButtons.inlineEditorAdmin div.wj-toolbar-panel div.wj-content-line { top: 0px}
    body.page-builder-in-admin { padding-bottom: 4px; }
    body.page-builder-in-admin #inlineEditorToolbarTop, body.page-builder-in-admin #inlineEditorToolbarTopPlaceHolder { height: 94px !important; }
    body.page-builder-in-admin #inlineEditorToolbarTopPlaceHolder { margin-bottom: 80px; }
    body.page-builder-in-admin div.wjInlineHighlighter { display: none; }
    body.page-builder-in-admin #inlineEditorToolbarTop, body.page-builder-in-admin div.cke_panel { z-index: 101003 !important; }
    body.page-builder-in-admin div.pb-modal { z-index: 101004; }
    body.page-builder-in-admin .cke_button__htmlbox { display: none; }
    body.page-builder-in-admin .pb-notify { top: 77px; }

    <% if (inlineEditorAdmin) {
        //je to takto, aby to pri citani nebliklo, nemoze cakat na nastavenie CSS page-builder-in-admin na body elemente
        %>
        body>div.header, body>header, body>div.footer, body>footer { display: none; }
    <% } %>
</style>

<div id="inlineEditorToolbar" class="inlineComponentButtons adminBar"<% if (disableToolbar) out.print(" style='display: none;'"); %>>
    <% if (disableToolbar == false) { %>

    <div class="wj-toolbar-panel <% if (isPreview) { %>is-view-mode<% } %>">
        <%
            EditorForm ef = EditorDB.getEditorForm(request, doc.getDocId(), -1, doc.getGroupId());

            int rootGroupId = sk.iway.iwcm.common.CloudToolsForCore.getRootGroupId(request);
            GroupDetails rootGroup = GroupsDB.getInstance().getGroup(rootGroupId);

            if (Tools.isEmpty(ef.getFieldJ()))
            {
                String currency = null;
                if (rootGroup != null && Tools.isNotEmpty(rootGroup.getFieldC()))
                {
                    currency = sk.iway.iwcm.common.CloudToolsForCore.getValue(rootGroup.getFieldC(), "curr");
                }
                if (Tools.isEmpty(currency)) currency = Constants.getString("basketDisplayCurrency");

                ef.setFieldJ(currency);
            }
            if (Tools.isEmpty(ef.getFieldL()))
            {
                String vat = null;
                if (rootGroup != null && Tools.isNotEmpty(rootGroup.getFieldC()))
                {
                    vat = sk.iway.iwcm.common.CloudToolsForCore.getValue(rootGroup.getFieldC(), "vat");
                }
                if (Tools.isNotEmpty(vat)) ef.setFieldL(vat);
            }

            request.setAttribute("editorForm", ef);
            //toto robi problem pri formularoch, ktorym chyba CSRF token request.setAttribute("spamProtectionDisable", "true");
        %>
        <%
            if (inlineEditorAdmin==false) {
                //ak to nie je inline editor, potrebujeme aj jqui
                out.println(Tools.insertJQueryUI(pageContext, null));
                request.setAttribute("autocompleteInserted", true);
            }
        %>
        <% if (ef.getEditorCssPath() != null) { %>
            <link rel="stylesheet" href="<%=ef.getEditorCssPath()%>" />
        <% } %>
        <style>
            .ui-widget-content.ui-autocomplete{z-index: 100000;}
        </style>
        <script type="text/javascript">
            window.editorStyles = <%=JsonTools.objectToJSON(sk.iway.iwcm.editor.service.EditorService.getCssListJson(doc))%>;
        </script>

        <form:form modelAttribute="editorForm" name="editorForm" id="editorFormId" class="donotobfuscate" method="post" style="margin: 0px; padding: 0px">
            <% if (inlineEditorAdmin==false) { %>
                <div class="wj-control-line">
                    <a href="http://<iwcm:text key="welcome.info_url"/>" target="__blank" class="wj-logo">
                        <img src="/components/_common/admin/inline/toptoolbar/wj-logo.png" alt="Webjet CMS">
                    </a>

                    <div class="wj-switcher">
                        <a href="javascript:;" class="wj-edit"><iwcm:text key="components.inline.tabs.editing"/></a>
                        <div class="wj-shower">
                            <div class="wj-circle"></div>
                        </div>
                        <a href="javascript:;" class="wj-view"><iwcm:text key="editor.preview"/></a>
                    </div> <!-- /.wj-switcher -->

                    <ul class="wj-tab-menu">
                        <li><a class="top_save_button" onclick="publishPage()"><iwcm:text key="admin.m.publish"/></a> </li>
                        <li data-id="tab-01"  class="is-active">
                            <a href="javascript:;" class="wj-tab-menu-link"><iwcm:text key="admin.editor.title"/></a>
                        </li>
                        <li data-id="tab-02">
                            <a href="javascript:;" class="wj-tab-menu-link"><iwcm:text key="editor.tab.basic_info"/></a>
                        </li>
                        <li data-id="tab-05" style="display: none;">
                            <a href="javascript:;" class="wj-tab-menu-link"><iwcm:text key="editor.toolbar.customize"/></a>
                        </li>
                        <li data-id="tab-03">
                            <a href="javascript:;" class="wj-tab-menu-link"><iwcm:text key="components.seo.title"/></a>
                        </li>
                        <% if (isCloud && hasShop) { %>
                        <li data-id="tab-04">
                            <a href="javascript:;" class="wj-tab-menu-link"><iwcm:text key="components.inline.tabs.eshop"/></a>
                            <script type="text/javascript">
                                function calculateWithVatInit()
                                {
                                    try
                                    {
                                        calculateWithVat("editorForm");
                                    } catch (e) {}
                                }
                                setInterval(calculateWithVatInit, 1000);
                            </script>
                        </li>
                        <% } %>

                        <% if (isCloud) { %>
                        <li data-id="tab-07">
                            <a href="javascript:;" class="wj-tab-menu-link"><iwcm:text key="components.inline.tabs.components"/></a>
                        </li>
                        <% } %>
                    </ul> <!-- /.wj-tab-menu -->

                    <ul class="wj-right-menu">
                        <li class="wj-has-ul">
                            <a href="javascript:;" class="wj-right-menu-link"><iwcm:text key="inlineToolbar.option.help"/></a>
                            <ul class="wj-sub-menu">
                                <% if(isCloud){ %>
                                <li><a target="_blank" href="<iwcm:text key="components.cloud.supportLink"/>"><iwcm:text key="welcome.support"/></a></li>
                                <%}else{ %>
                                <li><a href="http://docs.webjetcms.sk" target="_blank"><iwcm:text key="inlineToolbar.option.help.helper"/></a></li>
                                <%} %>
                                <% if (Tools.isEmail(Constants.getString("problemReportEmail"))) { %>
                                <li><a href="javascript:openPagePopup('report',500,600)"><iwcm:text key="inlineToolbar.option.help.report"/></a></li>
                                <% } %>
                            </ul>
                        </li>
                        <% if (isCloud == false) { %>
                        <li class="wj-has-ul">
                            <a href="javascript:;" class="wj-right-menu-link <% if (doc.isAvailable()) out.print("pageActive"); else out.print("pageDisabled"); %> ">
                                ID: <%=Tools.getDocId(request) %>
                            </a>
                            <ul class="wj-sub-menu" style="width: 260px;">
                                <li><a class="editPage" href="javascript:popupPageEdit()"><iwcm:text key="inlineToolbar.option.page.edit"/> (popup)</a></li>
                                <% if(doc.isAvailable()){%>
                                <li><a class="hidePage" href="javascript:setPageAvailable(false)"><iwcm:text key="inlineToolbar.option.page.hide"/></a></li>
                                <%}else{ %>
                                <li><a class="editPage" href="javascript:setPageAvailable(true)"><iwcm:text key="inlineToolbar.option.page.show"/></a></li>
                                <%} %>
                                <li><a class="lastChange" href="javascript:openPagePopup('pageHistory',500,220)"><strong><iwcm:text key="inlineToolbar.option.page.lastModification"/>: </strong><bean:write name="doc" property="authorName"/><br><bean:write name="doc" property="dateCreatedString"/> <bean:write name="doc" property="timeCreatedString"/></a></li>
                                <li class="noperms-cmp_stat"><a class="pageStats" href="javascript:openPagePopup('/apps/stat/admin/top-details/?docId=<%=doc.getDocId()%>&dateRange=',900,700)"><iwcm:text key="inlineToolbar.option.page.stats"/></a></li>
                                <%
                                    TemplatesDB tempDB = TemplatesDB.getInstance();
                                    TemplateDetails temp = tempDB.getTemplate(doc.getTempId());
                                    if(temp != null){ %>
                                <li class="noperms-menuTemplates"><a class="template" href='javascript:webjetToolbarPopup("/admin/v9/templates/temps-list/?id=<%=temp.getTempId()%>", 900, 700);'><strong><iwcm:text key="inlineToolbar.option.page.template"/>: </strong><%=temp.getTempName()%></a></li>
                                <% }
                                    if (group != null)
                                    {%>
                                <li class="noperms-editDir"><a class="mainDir" href='javascript:webjetToolbarPopup("/admin/v9/webpages/web-pages-list/?groupid<%=group.getGroupId()%>", 900, 700);'><strong><iwcm:text key="inlineToolbar.option.page.group"/>: </strong><%=group.getGroupName()%></a></li>
                                <% } %>
                            </ul>
                        </li>
                        <% } %>
                        <li class="wj-has-ul">
                            <a href="javascript:;" class="wj-right-menu-link">
                                <img class="userImage" src="<%=getUserPhotoPath(request)%>"/>
                                <bean:write name="<%=Constants.USER_KEY %>" property="fullName"/>
                            </a>
                            <ul class="wj-sub-menu">
                                <% if (isCloud == false) { %><li><a href="/admin/" target="_blank"><iwcm:text key="inlineToolbar.option.settings.admin"/></a></li><% } %>
                                <% if (isCloud) { %><li><a href="<iwcm:text key="components.cloud.profileLink"/>" target="_blank"><iwcm:text key="inlineToolbar.option.settings.profile"/></a></li><% } %>
                                <% if (isCloud) { %><li><a href="http://login.webjet.eu/my-sites/"><iwcm:text key="inlineToolbar.option.settings.MySites"/></a></li><% } %>
                                <li><a href="/logoff.do"><iwcm:text key="inlineToolbar.option.settings.logout"/></a></li>
                            </ul>
                        </li>
                    </ul> <!-- /.wj-right-menu -->

                    <div class="clearfix"></div>

                </div> <!-- /.control-line -->

                <div class="wj-toolbar-toggler">
                    <span class="state-a"><iwcm:text key="components.inline.hidePannel"/></span>
                    <span class="state-b"></span>
                </div> <!-- /.wj-toolbar-toggler -->
            <% } %>

            <div class="wj-content-line" id="wjInlineCkEditorToolbarOffsetElement">

                <div class="wj-content-box is-active" data-id="tab-01" style="padding-top: 0px; padding-bottom: 0px; padding-left: 0px;">

                    <div id='wjInlineCkEditorToolbarElement'></div>
                    <% if ("true".equals(request.getParameter("inlineEditorAdmin"))) { %>
                    <div class="exit-inline-editor" id="DTE_Field_data-editorTypeSelector">
                        <iwcm:text key="editor.type_select.label.js"/><br>
                        <select onchange="window.parent.switchEditorType(this)">
                            <option value=""><iwcm:text key="editor.type_select.standard.js"/></option>
                            <option value="pageBuilder" selected="selected"><iwcm:text key="editor.type_select.page_builder.js"/></option>
                        </select>
                        <br/>
                        <iwcm:text key="pagebuilder.modal.tab.size"/>:
                        <br/>
                        <a href="javascript:pbSetWindowSize('phone')" title="<iwcm:text key='pagebuilder.modal.visibility.sm'/>"><img src="/components/grideditor/icon-layout-mobile.png"/></a>
                        <a href="javascript:pbSetWindowSize('tablet')" title="<iwcm:text key='pagebuilder.modal.visibility.md'/>"><img src="/components/grideditor/icon-layout-tablet.png"/></a>
                        <a href="javascript:pbSetWindowSize('desktop')" title="<iwcm:text key='pagebuilder.modal.visibility.xl'/>" style="padding-left: 4px;"><img src="/components/grideditor/icon-layout-desktop.png"/></a>
                    </div>
                    <% } %>

                </div> <!-- /.wj-content-box _tab-01 -->

                <div class="wj-content-box" data-id="tab-02">

                    <div class="wj-col-3" style="width:33%;">
                        <div class="wj-row  wj-full-width">
                            <div class="wj-row-label">
                                <label for="title" class="has-tooltip">
                                    <iwcm:text key="components.inline.tabs.pageTitleInMenu"/>
                                </label>
                            </div>
                            <div class="wj-row-input"><form:input id="title" path="title"/></div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline"><iwcm:text key="components.inline.tabs.pageTitle"/></div>
                        </div>
                    </div>
                    <% if (isCloud && isPremiumPro) { %>
                    <div class="wj-col-3" style="width:20%;">
                        <div class="wj-row  wj-full-width">
                            <div class="wj-row-label">
                                <label for="title" class="has-tooltip">
                                    <iwcm:text key="components.cloud.webjet_eu.index_price.starter.password_protect_pages"/>
                                </label>
                            </div>
                            <div class="wj-row-input"><input type="checkbox" name="passwordProtected" value="1" <% if ("1".equals(ef.getPasswordProtectedString())) out.print("checked='checked'"); %>/></div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline">Premium PRO</div>
                        </div>
                    </div>
                    <% } %>
                </div>


                <div class="wj-content-box" data-id="tab-03">

                    <div class="wj-col-3" style="width:33%;">
                        <div class="wj-row  wj-full-width">
                            <div class="wj-row-label">
                                <label for="fieldH" class="has-tooltip">
                                    <iwcm:text key="components.inline.tabs.seo.pageTitle"/>
                                    <div class="wj-tooltip-link">?<div class="wj-tooltip-text"><iwcm:text key="components.inline.tabs.seo.pageTitleHelp"/></div></div>
                                </label>
                            </div>
                            <div class="wj-row-input"><form:input id="fieldH" path="fieldH"/></div>
                        </div>
                        <div class="wj-row  wj-full-width">
                            <div class="wj-row-label"><label for="htmlHead"><iwcm:text key="components.inline.tabs.seo.pageDescription"/></div>
                            <div class="wj-row-input"><form:input id="htmlHead" path="htmlHead"/></div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline"><iwcm:text key="components.inline.tabs.seo.titleDescription"/></div>
                        </div>
                    </div>

                    <div class="wj-col-3" style="width:25%;">
                        <div class="wj-row wj-full-width">
                            <div class="wj-row-label"><label for="htmlData"><iwcm:text key="editor.subtitle"/></label></div>
                            <div class="wj-row-input"><form:input id="htmlData" path="htmlData"/></div>
                        </div>
                        <div class="wj-row wj-full-width">
                            <div class="wj-row-label"><label for="perexImageId"><iwcm:text key="editor.mainImage"/></label></div>
                            <div class="wj-row-input has-action"><form:input id="perexImageId" path="perexImage"/><i onclick="openImageDialogWindow('editorForm', 'perexImage');" class="wj-action-icon fa fa-image">&nbsp;</i></div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline"><iwcm:text key="components.news.title"/></div>
                        </div>
                    </div>

                    <div class="wj-col-3 last" style="width:42%;">
                        <div class="wj-row  wj-full-width">
                            <div class="wj-row-label">
                                <label for="virtualPath" class="has-tooltip">
                                    <iwcm:text key="components.inline.tabs.seo.virtualPath"/>
                                    <div class="wj-tooltip-link">?<div class="wj-tooltip-text"><iwcm:text key="components.inline.tabs.seo.virtualPathHelp"/></div></div>
                                </label>
                            </div>
                            <div class="wj-row-input"><form:input id="virtualPath" path="virtualPath"/></div>
                        </div>
                        <div class="wj-row  wj-full-width">
                            <div class="wj-row-label"><label for="externalLink"><iwcm:text key="components.inline.tabs.seo.externalLink"/></label></div>
                            <div class="wj-row-input has-action"><form:input id="externalLink" path="externalLink"/><i onclick="openLinkDialogWindow('editorForm', 'externalLink');" class="wj-action-icon fa fa-link">&nbsp;</i></div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline"><iwcm:text key="components.inline.tabs.seo.tabTitle"/></div>
                        </div>
                    </div>

                    <div class="clearfix"></div>

                </div> <!-- /.wj-content-box _tab-02 -->

                <% if (isCloud && hasShop) {       %>
                <div class="wj-content-box" data-id="tab-04">

                    <div class="wj-col-3" style="width:43%;">
                        <div class="wj-row">
                            <div class="wj-row-label"><label for="fieldInputK"><iwcm:text key="components.catalog.price_without_vat"/></label></div>
                            <div class="wj-row-input"><form:input id="fieldInputK" path="fieldK" onchange="calculateWithVat(this.form.name)" onkeyup="calculateWithVat(this.form.name)" onblur="calculateWithVat(this.form.name)" autocomplete="off"/></div>
                            <div class="wj-row-label"><label for="fieldInputL"><iwcm:text key="components.catalog.vat"/></label></div>
                            <div class="wj-row-input"><form:input id="fieldInputL" path="fieldL" style="width: 40px !important; display: inline; margin-right: 6px;" onchange="calculateWithVat(this.form.name)" onkeyup="calculateWithVat(this.form.name)" onblur="calculateWithVat(this.form.name)" autocomplete="off"/>%</div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-label"><label for="fieldInputPriceVat"><iwcm:text key="components.catalog.price_with_vat"/></label></div>
                            <div class="wj-row-input"><input type="text" id="fieldInputPriceVat" name="priceWithVat" onchange="calculateWithoutVat(this.form.name)" onkeyup="calculateWithoutVat(this.form.name)" onblur="calculateWithoutVat(this.form.name)" autocomplete="off"/></div>
                            <div class="wj-row-label"><label for="fieldInputJ"><iwcm:text key="components.basket.invoice.currency"/></label></div>
                            <div class="wj-row-input"><form:input id="fieldInputJ" path="fieldJ" style="width: 40px !important;"/></div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-label"><label for="fieldInputM"><iwcm:text key="components.basket.old_price"/></label></div>
                            <div class="wj-row-input"><form:input id="fieldInputM" path="fieldM" autocomplete="off"/></div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline">Nastavenie cien</div>
                        </div>
                    </div>

                    <div class="wj-col-3" style="width:37%;">
                        <div class="wj-row wj-half-width">
                            <div class="wj-row-label"><label for="fieldInputN"><iwcm:text key="components.basket.ean"/></label></div>
                            <div class="wj-row-input"><form:input id="fieldInputN" path="fieldN"/></div>
                        </div>
                        <div class="wj-row wj-half-width">
                            <div class="wj-row-label"><label for="fieldInputO"><iwcm:text key="components.catalog.manufacturer"/></label></div>
                            <div class="wj-row-input"><form:input id="fieldInputO" path="fieldO"/></div>
                        </div>
                        <div class="wj-row wj-half-width">
                            <div class="wj-row-label"><label for="kategoriaHeurekaId"><iwcm:text key="components.inline.tabs.eshop.heurekaCathegory"/></label></div>
                            <div class="wj-row-input">
                                <iwcm:autocomplete name="prefix" id="kategoriaHeurekaId" url="/components/basket/kategorie_heureka.jsp" value="<%=ResponseUtils.filter(ef.getFieldQ())%>" onchange="fillKategoriaHeureka()" onkeyup="fillKategoriaHeureka()" onblur="fillKategoriaHeureka()" onOptionSelect="fillKategoriaHeureka()"/>
                                <form:input id="fieldQId" path="fieldQ"/>
                            </div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline"><iwcm:text key="components.inline.tabs.eshop.additionalInfo"/></div>
                        </div>
                    </div>

                    <div class="wj-col-3 last" style="width:20%;">
                        <div class="wj-row wj-full-width">
                            <div class="wj-row-content mediaList">

                            </div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline"><iwcm:text key="editor.perex.image"/></div>
                        </div>
                    </div>

                    <div class="clearfix"></div>

                </div> <!-- /.wj-content-box _tab-04 -->
                <% } %>

                <%
                    if (isCloud) {
                %>

                <div class="wj-content-box noOverflow" data-id="tab-07">
                    <script>
                        $(document).ready(function () {
                            var showData = $('#draggableList-components');

                            $.getJSON('/admin/inline/draggable-list-components.jsp', function (data) {
                                var items = data.items.map(function (item) {
                                    return "<a title='<iwcm:text key="editor.draggable.tip"/>' data-name='"+item.name+"' class='draggable "+ item.class +"' href='javascript:void()' data-rel='"+ item.value +"'><span class='app-name'>"+item.name+"</span> <br><img class='' src='"+ item.img + "' alt='"+ item.name +"'></a>";
                                });
                                showData.empty();
                                if (items.length) {
                                    var content = '<li>'+  items.join('</li><li>') + '</li>';
                                    var list = $('<ul id="apps" />').html(content);
                                    showData.append(list);
                                }
                            });
                        });
                    </script>


                    <div class="clearfix draggableList" >
                        <div class="clearfix" id="draggableList-components"></div>
                        <div class="wj-row-headline"><iwcm:text key="components.cloud.apps.title"/></div>

                    </div>


                </div>

                <% } %>
                <div class="wj-content-box wj-edit-settings" data-id="tab-05">

                    <div class="wj-col-3" style="width:auto;">
                        <div class="wj-row  wj-full-width">
                            <div class="wj-row-input">
                                <a class="inlineComponentButton cke_button wj-edit-header" href="javascript:editHeader();"><iwcm:text key="inlineToolbar.wj.edit.header"/></a>
                                <a class="inlineComponentButton cke_button wj-edit-footer" href="javascript:editFooter();"><iwcm:text key="inlineToolbar.wj.edit.footer"/></a>

                                <% Integer leftMenuDocId = (Integer)request.getAttribute("doc_left_menu-docId=");
                                    if (leftMenuDocId != null && leftMenuDocId > 0)  { %>
                                <a class="inlineComponentButton cke_button wj-edit-left-menu" href="javascript:editSideLeftMenu();"><iwcm:text key="inlineToolbar.wj.edit.sidebar.left"/></a>
                                <% } %>

                                <%  Integer rightMenuDocId = (Integer)request.getAttribute("doc_right_menu-docId=");
                                    if (rightMenuDocId != null && rightMenuDocId > 0)  { %>
                                <a class="inlineComponentButton cke_button wj-edit-right-menu" href="javascript:editSideMenu();"><iwcm:text key="inlineToolbar.wj.edit.sidebar.right"/></a>
                                <% } %>

                            </div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline"><iwcm:text key="inlineToolbar.wj.edit.pageBlock"/></div>
                        </div>
                    </div>

                    <div class="wj-col-3" style="width:auto;">
                        <div class="wj-row  wj-full-width">
                            <div class="wj-row-input">
                                <a class="inlineComponentButton cke_button wj-edit-structure" href="javascript:editStructure();"><iwcm:text key="components.custommenu.menuitems"/></a>
                                <a style="display: none;" class="inlineComponentButton cke_button wj-edit-add-subpage" href="javascript:inlineAddPage(<%=groupId%>);"><iwcm:text key="inlineToolbar.wj.edit.add.site"/></a>
                                <a style="display: none;" class="inlineComponentButton cke_button wj-edit-add-sub-dir" href="javascript:inlineAddSubDir();"><iwcm:text key="inlineToolbar.wj.edit.add.folder"/></a>
                                <% if(isCloud){
                                    IwcmFile file = new IwcmFile(Tools.getRealPath("/templates/"+sk.iway.iwcm.common.CloudToolsForCore.getRootTempName(request)+"/assets/css/basic_"+sk.iway.iwcm.common.CloudToolsForCore.getRootTempName(request)+".css"));
                                    if(file.exists()){
                                %>
                                <a class="inlineComponentButton cke_button wj-edit-styles" href="javascript:openPagePopup('/components/cloud/admin/admin_style_edit.jsp',970,560);"><iwcm:text key="inlinetoolbar.styles"/></a>
                                <%}
                                }%>
                                <% if(isGridEditorEnabled){ %>
                                <a class="inlineComponentButton cke_button wj-grideditor" href="javascript:GridEditorInit();"><iwcm:text key="inlineToolbar.wj.edit.grideditor"/></a>
                                <%}%>
                            </div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline"><iwcm:text key="editor.toolbar.customize"/></div>
                        </div>
                    </div>
                    <% if(isCloud){ %>

                    <div class="wj-col-3" style="width:auto;">
                        <div class="wj-row  wj-full-width">
                            <div class="wj-row-input">
                                <a class="inlineComponentButton cke_button wj-edit-settings" href="javascript:openPagePopup('/components/cloud/admin/admin_site_customize.jsp',970,560);"><iwcm:text key="inlineToolbar.wj.edit.settings.site"/></a>
                            </div>
                        </div>
                        <div class="wj-row">
                            <div class="wj-row-headline"><iwcm:text key="inlineToolbar.wj.edit.settings"/></div>
                        </div>
                    </div>

                    <%} %>

                    <iwcm:menu name="addTopLevelPage">
                        <!-- Cez css class to nefungovalo, nieco to prepisovalo -->
                        <li  style="display: none;"><a class="inlineComponentButton cke_button" href="javascript:inlineAddTopDir();"><iwcm:text key="editor.inline.addTopPage"/></a></li>
                    </iwcm:menu>
                </div>

            </div> <!-- /.wj-content-line -->

            <%
                String cssStyle = EditorDB.getEditorCssStyle(request);
            %>
            <input type="hidden" name="docId" value="<%=doc.getDocId()%>"/>
            <input type="hidden" name="groupId" value="<%=doc.getGroupId()%>"/>
            <input type="hidden" name="ckEditorContentCss" value="<%=ResponseUtils.filter(cssStyle) %>"/>
            <input type="hidden" name="json" id="editorFormJsonField" value=""/>
        </form:form>
    </div>  <!-- /.wj-toolbar-panel -->


    <script type="text/javascript">
        var cloudFixBackgroundPosAllreadySet = false;
        function cloudFixBackgroundPos()
        {
            if (cloudFixBackgroundPosAllreadySet == true) return;
            cloudFixBackgroundPosAllreadySet = true;

            try
            {
                //console.log("cloudFixBackgroundPos");

                var bgPosition = $("body").css("backgroundPosition");
                //console.log("bgPosition="+bgPosition);
                if (bgPosition != null && bgPosition.length>3)
                {
                    if (bgPosition.indexOf("top")!=-1) bgPosition = bgPosition.replace("top", "45px");
                    else if (bgPosition.indexOf(" 0%")!=-1) bgPosition = bgPosition.replace(" 0%", " 45px");
                    else
                    {
                        //parse
                        var bgPosArr = bgPosition.split(" ");
                        if (bgPosArr.length == 2 && bgPosArr[1].indexOf("px")!=-1)
                        {
                            var value = parseInt(bgPosArr[1].substring(0, bgPosArr[1].length-2));
                            //console.log("bgpos value="+value);
                            value = value + 45;
                            bgPosition = bgPosArr[0]+" "+value+"px";
                        }
                    }
                }

                //console.log("setting bg="+bgPosition);
                $("body").css("backgroundPosition", bgPosition);

                if ($("#header").css("position")=="fixed") $("#header").css("top", "45px");
            }
            catch (e) {}
        }

        $( document ).ready(function()
        {

            $(".wj-tab-menu-link").on('click', function(e) {
                e.preventDefault();

                var el = $(this).closest("li"),
                    allEl = $(".wj-tab-menu li"),
                    tabId = el.attr("data-id"),
                    contentBoxAll = $(".wj-content-box"),
                    contentBox = $(".wj-content-box[data-id='" + tabId + "']");

                if (!el.hasClass("is-active")) {

                    allEl.not(el).removeClass("is-active");
                    el.addClass("is-active");

                    contentBoxAll.not(contentBox).removeClass("is-active");
                    contentBox.addClass("is-active");
                }

                $("#inlineEditorToolbarTopPlaceHolder").height($("#wjInlineCkEditorToolbarOffsetElement").height()+$(".wj-toolbar-panel").height());

            });

            $(".wj-right-menu-link").on('click', function(e) {
                e.preventDefault();

                var el = $(this).closest("li"),
                    allEl = $(".wj-right-menu li");

                if (el.hasClass("is-active")) {

                    el.removeClass("is-active");

                } else {

                    allEl.not(el).removeClass("is-active");
                    el.addClass("is-active");
                }

            });

            var toolbar = $(".wj-toolbar-panel");

            $(".wj-edit").on('click', function(e) {
                e.preventDefault();
                wjSetReadOnly(false);
            });

            $(".wj-view").on('click', function(e) {
                e.preventDefault();
                <% if(isGridEditorEnabled){ %>
                GridEditorDeinit();
                <%}%>
                wjSetReadOnly(true);
            });

            $(".wj-shower").on('click', function(e) {
                e.preventDefault();
                <% if(isGridEditorEnabled){ %>
                GridEditorDeinit();
                <%}%>
                wjSetReadOnly(!toolbar.hasClass("is-view-mode"));
            });

            $(".wj-toolbar-toggler").on('click', function(e) {
                e.preventDefault();
                if (toolbar.hasClass("is-toolbar-hidden"))
                {
                    toolbar.removeClass("is-toolbar-hidden");
                    $("body").removeClass("is-toolbar-hidden");
                }
                else
                {
                    toolbar.addClass("is-toolbar-hidden");
                    $("body").addClass("is-toolbar-hidden");
                }
            });


            window.setTimeout(cloudFixBackgroundPos, 100);
        });
    </script>
    <% } %>
</div>
<% if (disableToolbar==false)
{
    boolean isInBody = "body".equals(Constants.getString("inlineEditingToolbarSelectorPrepend"));
%>
<script type="text/javascript">
    $( document ).ready(function() {
        //console.log("HTML=", $("#inlineEditorToolbar").html());
        $("<%=Constants.getString("inlineEditingToolbarSelectorPrepend")%>").prepend("<div id='inlineEditorToolbarTop' class='<% if(isCloud){out.print("isCloud");}%> inlineComponentButtons adminBar<% if (isInBody==false) out.print(" inlineEditorToolbarTopNotBody"); %><% if (inlineEditorAdmin) out.print(" inlineEditorAdmin"); %>'>"+$("#inlineEditorToolbar").html()+"</div><% if (isInBody) { %><div id='inlineEditorToolbarTopPlaceHolder'></div><% } %>");
        $("#inlineEditorToolbar").hide();
        $("#inlineEditorToolbar").html("");

        $("span.inlineComponentButtonImage").each(function()
        {
            setAllCss($(this), $(this).parent());
        });
        $("div.inlineComponentEditButtonsTop").each(function()
        {
            $("#inlineToolbarAddButtonsSpace").html($("#inlineToolbarAddButtonsSpace").html()+$(this).html());
        });
    });

    setAllCss = function(el, parent) {
        var options = ["height","line-height"];

        $.each(options, function(index, value){
            var parentValue = parent.css(value);

            el.css(value, parentValue);
        });
    };

    $( document ).ready(function()
    {
        $( "body div.inlineEditingWrapperDiv" ).hover(
            function()
            {
                // $( this ).append( $( "<span> ***</span>" ) )
                var toolbar = $("#"+$(this).attr("id")+"Toolbar").find("div.inlineComponentButtonsWrapper");

                toolbar.stop(true, true);
                toolbar.fadeIn( { duration: 2000 });
            },
            function()
            {
                //$( this ).find( "span:last" ).remove();
                //console.log("HOVER OUT: "+this);
                var toolbar = $("#"+$(this).attr("id")+"Toolbar").find("div.inlineComponentButtonsWrapper");
                toolbar.fadeOut( { duration: 1000 });
            }
        );
    });
</script>
<%
    }

    pageContext.setAttribute("lng", lng);
%>


<%--
            Sidebar
--%>
<%--<style>--%>
   <%--.sidenav {--%>
        <%--height: 100%;--%>
        <%--width: 0;--%>
        <%--position: fixed;--%>
        <%--z-index: 1;--%>
        <%--top: 0;--%>
        <%--left: 0;--%>
        <%--background-color: #111;--%>
        <%--overflow-x: hidden;--%>
        <%--transition: 0.5s;--%>
        <%--padding-top: 60px;--%>
    <%--}--%>

    <%--.sidenav a {--%>
        <%--padding: 8px 8px 8px 32px;--%>
        <%--text-decoration: none;--%>
        <%--font-size: 25px;--%>
        <%--color: #818181;--%>
        <%--display: block;--%>
        <%--transition: 0.3s;--%>
    <%--}--%>

    <%--.sidenav a:hover {--%>
        <%--color: #f1f1f1;--%>
    <%--}--%>

    <%--.sidenav .closebtn {--%>
        <%--position: absolute;--%>
        <%--top: 0;--%>
        <%--right: 25px;--%>
        <%--font-size: 36px;--%>
        <%--margin-left: 50px;--%>
    <%--}--%>

    <%--@media screen and (max-height: 450px) {--%>
        <%--.sidenav {padding-top: 15px;}--%>
        <%--.sidenav a {font-size: 18px;}--%>
    <%--}--%>
<%--</style>--%>

<%--<div id="mySidenav" class="sidenav">--%>
    <%--<a href="javascript:void(0)" class="closebtn" onclick="closeNav()">&times;</a>--%>
    <%--<a href="#">About</a>--%>
    <%--<a href="#">Services</a>--%>
    <%--<a href="#">Clients</a>--%>
    <%--<a href="#">Contact</a>--%>
<%--</div>--%>

<%--<h2>Animated Sidenav Example</h2>--%>
<%--<p>Click on the element below to open the side navigation menu.</p>--%>
<%--<span style="font-size:30px;cursor:pointer" onclick="openNav()">&#9776; open</span>--%>

<%--<script>--%>
    <%--function openNav() {--%>
        <%--document.getElementById("mySidenav").style.width = "250px";--%>
    <%--}--%>

    <%--function closeNav() {--%>
        <%--document.getElementById("mySidenav").style.width = "0";--%>
    <%--}--%>
<%--</script>--%>
