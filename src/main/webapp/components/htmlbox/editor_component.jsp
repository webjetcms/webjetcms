<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%@page import="sk.iway.iwcm.PageParams"%>
<%@page import="sk.iway.iwcm.Tools"%>

<%@page import="sk.iway.iwcm.PageLng"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.FileTools"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.filebrowser.BrowseAction"%>

<%@page import="sk.iway.iwcm.Constants" %>

<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*, java.util.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%
   request.setAttribute("cmpName", "htmlbox");

   GroupsDB groupsDB = GroupsDB.getInstance();
   GroupDetails grpDet = groupsDB.getGroup(Constants.getInt("tempGroupId"));

   DocDB docDB = DocDB.getInstance();
   List docList = null;
   if(grpDet != null)
   {
   	docList = docDB.getBasicDocDetailsByGroup(grpDet.getGroupId(), DocDB.ORDER_PRIORITY);
   	if(docList.size() > 0)
   		request.setAttribute("docList", docList);
   }
   List<GroupDetails>	listPriecinkov;
   if (grpDet != null) listPriecinkov = groupsDB.getGroups(grpDet.getGroupId());
   else listPriecinkov = new ArrayList<GroupDetails>();

	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	//out.println(paramPageParams);
	//System.out.println(paramPageParams);
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>
<jsp:include page="/components/top.jsp"/>

<script type="text/javascript">
	var openTabNum = 1;

	$('document').ready(function(){
// 		resizeIframe(200,200);
	});

	function loadDirPreview(select){
		var selectedDir = $('#DirNameSelect').find(":selected").val();

		if(selectedDir==="otherTemplatesInsideRootDirectory"){
			selectedDir="";
		}
		var link = 	"<%=WriteTag.getCustomPage("/components/htmlbox/html_temp-ajax.jsp",request)%>?dirName="+selectedDir;
		dirPreview.location.href=link;
	}
	function Ok()
	{
		//console.log("Ok, openTabNum=", openTabNum, "field=", document.textForm.field.value);
	    //pre vlozenie ineho docid priamo nafejkujeme tab2
	    //if (openTabNum==1 && document.textForm.field.value=="otherDocId" && document.textForm.insertType.value=="static") openTabNum=2;

		if(openTabNum==1)
		{
            if (document.textForm.field.value=="otherDocId")
			{
				//htmlCode = previewWindow.document.getElementById("_iframeHtmlData").value;
                var docId = document.textForm.field.value
                if (document.textForm.docid.value != "") docId = document.textForm.docid.value;
                var htmlCode = "";
                if (document.textForm.insertType[1].checked)
                {
                    htmlCode = "!INCLUDE(/components/htmlbox/showdoc.jsp, docid="+docId+")!";
                }
                else
                {
                    htmlCode = previewWindow.document.getElementById("_iframeHtmlData").value;
                }
				//console.log("Inserting HTML code=", htmlCode);
                insertHtml(htmlCode);
                return true ;
			}else {

                if ($('input[name=DSsel]:checked').val() === "dynamic") {

                    var selectedObjectDocId = $('#previewWindow').contents().find('.selected').attr('data-docid');
                    if (document.textForm.field.value == "otherDocId") selectedObjectDocId = document.textForm.docid.value;

                    var htmlCode = "";
                    htmlCode = "!INCLUDE(/components/htmlbox/showdoc.jsp, docid=" + selectedObjectDocId + ")!";

                    if (htmlCode.indexOf("<span") == 0) {
                        insertHtml(htmlCode);
                        return true;
                    }

                    insertHtml(htmlCode);
                    return true;
                } else if ($('input[name=DSsel]:checked').val() === "static") {
                    var htmlCode = $("#previewWindow").contents().find("#WJTempPreview").contents().find('#_iframeHtmlData').val();

                    //window.alert(htmlCode);
                    //fix ked tam je len INCLUDE aby sa spravne opravili uvodzovky a nestalo sa z toho &quot;amp;
                    htmlCode = htmlCode.replace(/&quot;/gi, "\"");
                    //window.alert(htmlCode);

                    if (htmlCode.indexOf("<span") == 0) {
                        insertHtml(htmlCode);
                        return true;
                    }
                    insertHtml(htmlCode);
                    return true;

                } else {
                    return false;
                }
            }
		}
		else if (openTabNum==2)
		{
            if($('input[name=DSsel2]:checked').val()==="static"){
                //console.log("static chosen")
                var htmlCode = $('#dirPreview').contents().find('#previewWindow').contents().find('body').html();
				//console.log("htmlCode="+htmlCode);
                insertHtml(htmlCode);
                return true ;
			}else if($('input[name=DSsel2]:checked').val()==="dynamic") {

			}else {return false;}

		}
	} // End OK function

	function insertHtml(data) {
		//console.log("Inserting HTML code=", data, "oEditor=", oEditor);

		if (data.indexOf("<span") == 0) {
			//console.log("Inserting SPAN");
			//insert inline element
			oEditor.ckeditor.insertHtml(data);
        } else if (typeof oEditor.ckeditor != "undefined" && typeof oEditor.ckeditor.wjInsertHtml != "undefined") {
			//console.log("Inserting WJ HTML");
			//if (data.indexOf("!INCLUDE") == 0) oEditor.ckeditor.wjInsertUpdateComponent(data);
			//else oEditor.ckeditor.wjInsertHtml(data);
			oEditor.FCK.InsertHtml(data);
		}
		else {
			//console.log("Inserting FCK HTML");
			oEditor.FCK.InsertHtml(data);
		}

		oEditor.FCK.WJToogleBordersCommand_thisRef.RefreshBorders();
	}


	function preview(select)
	{
		if(select.value==="otherDocId"){  //vlozenie inej stranky
            $('#DSselector').hide();
			$('#otherSiteDiv').show();

			previewWindow.location.href="/components/empty.jsp";
		}else if(select.value==="otherTemplatesInsideRootDirectory"){ //priamo v root priecinku
			$('#otherSiteDiv').hide();
            $('#DSselector').show();
			previewWindow.location.href="/components/htmlbox/iframe2.jsp?dirDocId="+<%=grpDet.getGroupId()%>;
		}else {
			$('#otherSiteDiv').hide();
            $('#DSselector').show();
			previewWindow.location.href="/components/htmlbox/iframe2.jsp?dirDocId="+select.value;

		}
	}
	function setStandardDoc(doc_id)
	{
	   document.textForm.docid.value = doc_id;
	   previewDocId();
	}
	function previewDocId()
	{
		previewWindow.location.href="/components/htmlbox/iframe.jsp?docid="+document.textForm.docid.value;
	}
	function editDoc()
	{
		if (document.textForm.docid.value == "") return;
		previewWindow.location.href="/admin/v9/webpages/web-pages-list/?showOnlyEditor=true&showEditorFooterPrimary=true&docid="+document.textForm.docid.value;
	}

</script>
<style type="text/css">
	div.filterOptions { width:100%; padding: 10px;}
	div.filterOptions select{ min-width: 200px;}
	div.tab-pane { min-height:0; }
</style>
<div class="box_tab box_tab_thin left" style="min-width: 900px;">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst">
			<a href="#" onclick="showHideTab('1');openTabNum=1" id="tabLink1"><iwcm:text key="components.htmlbox.tabs.yours"/></a></li>
		<li><a href="#" onclick="showHideTab('2');openTabNum=2;loadDirPreview(document.getElementById('DirNameSelect'));" id="tabLink2"><iwcm:text key="components.htmlbox.tabs.common"/></a></li>
	</ul>
</div>
<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<form name="textForm">
			<div class="filterOptions" style="display:block; position:relative;">
		      	<select name="field" onchange="preview(this);this.form.docid.value='';">
		      	 	<option value="otherTemplatesInsideRootDirectory"><iwcm:text key="components.htmlbox.others"/></option>
		          	 <option value="otherDocId"><iwcm:text key="components.htmlbox.pageWithDocId"/></option>
		          	<%
				      	for(GroupDetails jeden: listPriecinkov){
				      		String nazovPriecinku = jeden.getGroupName();
				      		out.print("<option value='"+jeden.getGroupId()+"'>"+nazovPriecinku+"</option>");
				      	}
			      	%>
		         </select>
				 <span id="DSselector">
					 <label><input type="radio" name="DSsel" class="DSsel" value="dynamic"><iwcm:text key="components.htmlbox.dynamicBlock"/></label>
					 <label><input type="radio" name="DSsel" class="DSsel" value="static" checked="checked"><iwcm:text key="components.htmlbox.staticBlock"/></label>
				 </span>
		         <span id="otherSiteDiv" style="display:none;">
					<span><iwcm:text key="components.htmlbox.insertType"/>:</span>
					<span><label><input type="radio" name="insertType" value="static" checked="checked"/> <iwcm:text key="components.htmlbox.insertType.static"/></label></span>
					<span style="padding-right: 1em;"><label><input type="radio" name="insertType" value="dynamic" id="insertType" /> <iwcm:text key="components.htmlbox.insertType.dynamic"/></label></span>

					<iwcm:text key="components.popup.docid"/>:
					<input type="text" name="docid" value="<%
						String docIdValue = pageParams.getValue("docid", "");
						if ("-2".equals(docIdValue)) docIdValue = ""; //special value for Page Builder content block
						out.print(sk.iway.iwcm.tags.support.ResponseUtils.filter(docIdValue)); %>" size="5" onblur="previewDocId();"/>
					<input type="button" value="<iwcm:text key='components.tips.select'/>" name="bSelDoc" onClick='popupFromDialog("/admin/user_adddoc.jsp", 450, 340);' class="btn green" />
					<input type="button" value="<iwcm:text key='button.edit'/>" name="bEditDoc" onClick='editDoc();' class="btn yellow" />
	         	</span>
			</div>
         	<div class="thumbSelector tab-page-iframe" data-margin-top="85" style="width:100%; overflow: auto; background:white;">
         		<iframe id="previewWindow" name="previewWindow" data-margin-top="85" style="width:100%; height:100%; background: white; border:0;" src="iframe2.jsp?dirDocId=<%=grpDet.getGroupId()%>"></iframe>
	        </div>
		</form>
	</div>
	<div class="tab-page" id="tabMenu2" style="display: none; min-height: 400px;">
		<div class="filterOptions" style="display:block;">
			<select id="DirNameSelect" onchange="loadDirPreview(this);">
				<%
		            //prescanuj adresar a vylistuj vsetky priecinky do selectboxu
					IwcmFile dir = new IwcmFile(Tools.getRealPath("/components/"+Constants.getInstallName()+"/htmlbox/objects"));
					if (dir.exists()==false) dir = new IwcmFile(Tools.getRealPath("/components/htmlbox/objects"));
		            String dirName;
				   if (dir.exists() && dir.canRead())
				   {
				      IwcmFile files[] = FileTools.sortFilesByName(dir.listFiles());

					  if(files != null) {
						int size = files.length;
						IwcmFile f;
						for (int i=0; i<size; i++)
						{
							f = files[i];
							if (f.isDirectory())
							{
								dirName = f.getName();

								String dirNameDecoded = "";
								if (BrowseAction.hasForbiddenSymbol(dirName))
									continue;

									try
									{
										dirNameDecoded = dirName.replace('_', ' ');
									}
									catch (Exception ex){}
									out.print("<option value='"+Tools.escapeHtml(dirName)+"'>"+Tools.escapeHtml(dirNameDecoded)+"</option>");
							}
						}
					  }
				   }
	            %>
			</select>
			<span style="display: none;">
				 <label><input type="radio" name="DSsel2" value="dynamic">Dynamicky blok</label>
				 <label><input type="radio" name="DSsel2" value="static" checked="checked">Staticky blok</label>
			 </span>
		</div>
		<div>
			<form name="textForm2">
					<div class="thumbSelector tab-page-iframe" data-margin-top="70" style=" width:100%; height: 70%; overflow: auto; background:white;">
						<iframe src="<%=WriteTag.getCustomPage("/components/htmlbox/html_temp-ajax.jsp",request)%>" name="dirPreview" id="dirPreview"
							width="100%" height="100%" data-margin-top="70" marginwidth="0" marginheight="0" frameborder="0" scrolling="auto"></iframe>
					</div>
			</form>
		</div>
	</div>
</div>
<% if (pageParams.getIntValue("docid", -1) > 0 || pageParams.getIntValue("docid", -1) == -2) { %>
	<script type="text/javascript">
		editDoc();
		$('#DSselector').hide();
		$('#otherSiteDiv').show();
		document.textForm.field.value="otherDocId";
		$(document.textForm.insertType[1]).click();
	</script>
<%  } else if ((docList == null || docList.size()<3 ) && listPriecinkov.size()<1) { %>
	<script type="text/javascript">
		$("#tabLink2").click();
	</script>
<% } %>

<jsp:include page="/components/bottom.jsp"/>
