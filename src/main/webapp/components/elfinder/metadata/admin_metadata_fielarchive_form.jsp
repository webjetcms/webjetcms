<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.util.HashMap,java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>
<%@ page import="sk.iway.iwcm.Identity" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="sk.iway.iwcm.Constants" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.FileTools" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorSearchBean" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %><iwcm:checkLogon admin="true" perms="menuFbrowser|menuWebpages"/><%!

%><script src="/admin/scripts/combine.jsp?t=js&amp;set=adminJqueryJs&amp;v=<%=Tools.getNow()%>&amp;lng=sk" type="text/javascript"></script>
<script src="/admin/scripts/combine.jsp?t=js&amp;set=adminStandardJs&amp;v=<%=Tools.getNow()%>&amp;lng=sk" type="text/javascript"></script>
<link href="/admin/scripts/combine.jsp?t=css&amp;set=adminStandardCss&amp;v=1548661620875&amp;lng=sk" rel="stylesheet" type="text/css"/>
<script  type="text/javascript"   src="/components/_common/javascript/jqui/jquery-ui-core.js"></script>
<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type="text/javascript">
    $(document).ready(function(){
        $('.closeModal',parent.document).on('click' ,function() {
            //alert('blabla: '+uploadedFiles);
            $.ajax({
                url:"/components/elfinder/metadata/admin_metadata_filearchive_save_ajax.jsp",
                type:"POST",
                data:{deleteTempFiles:uploadedFiles}
            });
        })
        ComponentsPickers.init();
    });
        function Ok() {
            $('.validate').html("");
            var data = $('.metadataFileArchiv').serialize();
            //console.log(data);
            var ok = true;
            $.ajax({
                url: '/components/elfinder/metadata/admin_metadata_filearchive_save_ajax.jsp',
                data: $('.metadataFileArchiv').serialize(),
                method:'POST',
                dataType: 'json',
                success: function (response) {
                    ok = true;
                    var count = Object.keys(response).length;
                   // console.log('response:'+ response.toString());
                    var i,clasIndex,fabIndex;
                    for (i = 0; i < count; i++) {
                        clasIndex = '.validateErrors_'+(i+1);
                        fabIndex = 'fab_'+[(i+1)];
                        //console.log('clasIndex: '+clasIndex);
                        //console.log('fabIndex: '+fabIndex);
                        //console.log('Object.keys(response): '+Object.keys(response)[i]);
                        if(response[Object.keys(response)[i]] != 'ok' )
                        {
                            if(ok == true ) {
                                ok = false;
                            }
                            //console.log('Object.keys(response)[]: '+response[Object.keys(response)[i]]);
                            $(clasIndex).html(response[Object.keys(response)[i]]);
                        }
                        //console.log('ok : '+ok);
                    }
                    if(ok)
                    {
                        $('.closeModal',parent.document).click();
                    }
                }
            });
        }

        var uploadedFiles = "";
</script>
<div class="tab-pane toggle_content tab-pane-fullheight tab-pane-single">
<%

    Identity user = UsersDB.getCurrentUser(request);
    Prop prop = Prop.getInstance(Constants.getServletContext(), request);

    String[] files = request.getParameterValues("files");
    if(user == null || !user.isAdmin())
    {
        out.print(prop.getText("logon.err.noadmin"));
    }

    if (files == null || files.length == 0) {
        out.print("<p>No files</p>");
        return;
    }
    int fileCount = 1;
    for(String uploadedFilePath:files)
    {
        if(!FileTools.isFile(uploadedFilePath))
        {
            out.print("<p>"+prop.getText("components.docman.error.db")+" "+uploadedFilePath+"</p>");
            return;
        }

        String fileWitoutRoot = FileArchivatorKit.getArchivPath();
        if(fileWitoutRoot.endsWith("/"))
        {
            fileWitoutRoot = fileWitoutRoot.substring(0,fileWitoutRoot.length()-1);
        }
        if(!fileWitoutRoot.startsWith("/"))
        {
            fileWitoutRoot = "/"+fileWitoutRoot;
        }
        fileWitoutRoot = Tools.replace(uploadedFilePath,fileWitoutRoot,"");
        //out.print("<p> fileWitoutRoot: "+fileWitoutRoot+"</p>");

        int lastSeparator = fileWitoutRoot.lastIndexOf("/");
        if(lastSeparator == -1 || lastSeparator > fileWitoutRoot.length())
        {
            out.print("Nepodarilo sa ziskat meno suboru z cesty "+fileWitoutRoot);
            return;
        }
        String fileName = fileWitoutRoot.substring(fileWitoutRoot.lastIndexOf("/")+1,fileWitoutRoot.length());
        //out.print("<p> fileName: "+fileName+"</p>");

        out.print("<h3>"+fileName+"</h3>");

        int index = fileWitoutRoot.indexOf("/",1);
        //out.print("<p> index: "+ index+"</p>");
        String category_1 = "";
        String category_2 = "";
        if(index > 0)
        {
            category_1 = fileWitoutRoot.substring(1,index);
        }

        //out.print("<p> category_1: "+ category_1+"</p>");
        index = fileWitoutRoot.indexOf("/", index+1);
        //out.print("<p> index2: "+ index+"</p>");
        if(Tools.isNotEmpty(category_1) && index > 0)
        {
            category_2 = fileWitoutRoot.substring(fileWitoutRoot.indexOf("/",1)+1,index);
        }

        FileArchivatorSearchBean fabSearch = new FileArchivatorSearchBean();
        String filePath= uploadedFilePath.substring(1,uploadedFilePath.lastIndexOf("/")+1);

        fabSearch.setFileName(fileName);
        fabSearch.setDirPath(filePath);

        List<FileArchivatorBean> archivResultList = FileArchivatorDB.search(fabSearch);
        FileArchivatorBean fab = new FileArchivatorBean();
        fab.setCategory(category_1);
        fab.setFieldA(category_2);
        if(archivResultList.size() == 1)
        {
            fab = archivResultList.get(0);
        }

        %>
        <div class="validateErrors_<%=fileCount%> validate" style="color:red;"></div>
        <form class="metadataFileArchiv fileCount form-horizontal" name="editorForm<%="_"+fileCount%>" action="/" method="POST">
            <table class="tableFileUpload">
                <tbody>
                <tr>
                    <td><label for="virtual_file_name<%="_"+fileCount%>" class="">Virtuálne meno súboru</label>:</td>
                    <td>
                        <input name="virtual_file_name<%="_"+fileCount%>" id="virtual_file_name<%="_"+fileCount%>" type="text" class="required form-control inputtext" value="<%=fab.getVirtualFileName() != null ?fab.getVirtualFileName(): "" %>" title="povinné pole">
                </tr>
                <tr style="display:none;">
                    <td>Doména:</td>
                    <td>

                        <select name="domain<%="_"+fileCount%>">

                            <option value="Všetky">Všetky</option>

                        </select>
                    </td>
                </tr>
                <tr>
                    <td><label for="product<%="_"+fileCount%>">Produkt </label>:</td>
                    <td>
                        <input name="product<%="_"+fileCount%>" id="product<%="_"+fileCount%>" type="text" class="form-control inputtext" value="<%=fab.getProduct() != null ? fab.getProduct():"" %>">
                    </td>
                </tr>
                <tr>
                    <td><label for="category<%="_"+fileCount%>">Kategória</label>:</td>
                    <td>
                        <input name="category<%="_"+fileCount%>" id="category<%="_"+fileCount%>" type="text" class="form-control inputtext" value="<%=fab.getCategory() != null ? fab.getCategory() : ""%>">
                    </td>
                </tr>
                <tr>
                    <td><label for="productCode<%="_"+fileCount%>">Kód produktu</label>:</td>
                    <td>
                        <input name="productCode<%="_"+fileCount%>" id="productCode<%="_"+fileCount%>" type="text" class="form-control inputtext" value="<%=fab.getProductCode() != null ? fab.getProductCode():""%>">
                    </td>
                </tr>
                <tr>
                    <td><label for="showFile<%="_"+fileCount%>">Zobraziť</label>:</td>
                    <td>
                        <div class="checker" id="uniform-showFile<%="_"+fileCount%>"><span class="checked"><input name="showFile<%="_"+fileCount%>" checked="checked" id="showFile<%="_"+fileCount%>" type="checkbox" value="true" class="  inputcheckbox"></span></div>
                    </td>
                </tr>
                <tr>
                    <td><label for="validFrom<%="_"+fileCount%>">Dátum začiatku platnosti</label>:</td>
                    <td>
                        <div class="row" style="margin: 0px;">
                            <div class="col-xs-6" style="padding: 0px;">
                                <div data-date-format="dd.mm.yyyy" class="input-group date date-picker">
                                    <input name="validFrom<%="_"+fileCount%>" id="validFrom<%="_"+fileCount%>" type="text" class="form-control datepicker inputtext" value="<%=fab.getValidFrom() != null ? fab.getValidFrom():"" %>">
                                    <span class="input-group-btn">
                                        <button type="button" class="btn default"><i class="ti ti-calendar-week"></i></button>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td><label for="validTo<%="_"+fileCount%>">Dátum konca platnosti</label>:</td>
                    <td>
                        <div class="row" style="margin: 0px;">
                            <div class="col-xs-6" style="padding: 0px;">
                                <div data-date-format="dd.mm.yyyy" class="input-group date date-picker">
                                    <input name="validTo<%="_"+fileCount%>" id="validTo<%="_"+fileCount%>" type="text" class="form-control datepicker inputtext" value="<%=fab.getValidTo() != null ? fab.getValidTo():"" %>">
                                    <span class="input-group-btn">
                                        <button type="button" class="btn default"><i class="ti ti-calendar-week"></i></button>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td><label for="priority<%="_"+fileCount%>">Priorita</label>:</td>
                    <td>
                        <input name="priority<%="_"+fileCount%>" id="priority<%="_"+fileCount%>" type="text" class="form-control  inputtext" value="0">
                    </td>
                </tr>
                <tr>
                    <td style="vertical-align: top;"><label for="note<%="_"+fileCount%>">Poznámka: </label></td>
                    <td>
                        <textarea class="fileArchivTextArea form-control inputtextarea" id="note<%="_"+fileCount%>" name="note<%="_"+fileCount%>"><%=fab.getNote() != null ? fab.getNote() : "" %></textarea>
                    </td>
                </tr>
                <tr>
                    <td><label for="fieldA<%="_"+fileCount%>">Pole A</label></td>
                    <td>
                        <input name="fieldA<%="_"+fileCount%>" id="fieldA<%="_"+fileCount%>" type="text" class="form-control inputtext" value="<%=fab.getFieldA()  != null ? fab.getFieldA():""%>">
                    </td>
                </tr>
                <tr>
                    <td><label for="fieldB<%="_"+fileCount%>">Pole B</label></td>
                    <td>
                        <input name="fieldB<%="_"+fileCount%>" id="fieldB<%="_"+fileCount%>" type="text" class="form-control inputtext" value="<%=fab.getFieldB()  != null ?fab.getFieldB() : ""%>">
                        <input type="hidden" name="save" value="true">
                        <input type="hidden" name="fab_id<%="_"+fileCount%>" value="<%=fab.getId()%>">
                        <input type="hidden" name="uploadedFilepath<%="_"+fileCount%>" value="<%=uploadedFilePath%>">
                        <input type="submit" id="submit" style="display:none;" name="submit">
                    </td>
                </tr>
                </tbody></table>
        </form>
    <%--<input type="button" value="Posli" onclick="Ok()">--%>
    <script type="text/javascript">
        uploadedFiles += '<%=uploadedFilePath%>,';
    </script>

<%
        fileCount++;
    }%></div>

