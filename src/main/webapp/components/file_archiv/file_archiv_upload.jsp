<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.components.file_archiv.*,org.apache.struts.util.ResponseUtils" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<stripes:useActionBean var="ab" beanclass="sk.iway.iwcm.components.file_archiv.SaveFileAction"/>
<%
	sk.iway.iwcm.i18n.Prop prop = Prop.getInstance(request);

	int oldId = Tools.getIntValue(request.getParameter("oldId"),-1);
	//nemozeme mat null
	if(ab.getFab() == null)
	{
		out.print(prop.getText("components.file_archiv.upload.wrong_oldId", String.valueOf(oldId)));
		return;
	}

	String dialogTitleKey = prop.getText("components.file_archiv.insert_new_version");
	String dialogDescKey = " ";
	if(oldId < 1)
		dialogTitleKey = prop.getText("components.file_archiv.insert_new");
	else
		dialogDescKey = prop.getText("components.file_archiv.replace_content",ResponseUtils.filter(ab.getFab().getFilePath()),ResponseUtils.filter(ab.getFab().getFileName()));

	request.setAttribute("cmpName", prop.getText("components.file_archiv.title"));

	request.setAttribute("dialogTitleKey", dialogTitleKey);
	request.setAttribute("dialogDescKey", dialogDescKey);
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<link rel="stylesheet" type="text/css" href="/admin/css/datepicker.css" />
<link href="/components/form/check_form.css" type="text/css" media="screen" rel="stylesheet">
<style type="text/css">
	.tableFileUpload{ margin: 0;}
	#doNotSaveIntoArchiv{font-weight: bold;color:red;}
</style>
<script src="/components/form/check_form.js" type="text/javascript"></script>
<script type="text/javascript">
$(document).ready(function(){

	$('.deleteIcon').click(function(){
		$(this).prev().prev().val('');
	});

	setTimeout(function(){ $('successUpload').hide(); }, 8000);

	$('.no_action').click(function(){
		window.close();
	});

	showHideLaterUpload();
	setReplace();

	$('.generateNextMonth').click(function(){
		$(this).parent().find("input").val($(this).attr("data-date"));
	});
});/// END of Document Ready


function refreshParent() {
    window.opener.location.reload();
}

function setFileBrowserPath(path)
{
	$("#filePath").val(path);
	return true;
}

function showHideLaterUpload()
{
	const uploadLaterCheckbox = $("#uploadLaterCheckboxId").prop("checked");
	try {
        if(uploadLaterCheckbox)
        {
			$("#uploadLaterDateId").css("display", "");
			$("#uploadLaterTimeId").css("display", "");
			$("#uploadLaterEmailsId").css("display", "");
			$("#uploadLaterId").val("true");
        }
        else
        {
			$("#uploadLaterDateId").css("display", "none");
			$("#uploadLaterTimeId").css("display", "none");
			$("#uploadLaterEmailsId").css("display", "none");
			$("#uploadLaterId").val("false");
        }
    }
    catch(err) {
        console.log(err.message);
    }
}

function setReplace()
{
    try
    {
        if($('#replaceCheckboxId').prop("checked"))
        {
			$("#replaceId").val("true");
            $('#doNotSaveIntoArchiv').show();
        }
        else
        {
			$("#replaceId").val("false");
            $('#doNotSaveIntoArchiv').hide();
        }
    }
    catch(err) {
        console.log(err.message);
    }
}

function deleteFile(id)
{
	const data = {fab_id: id};
	$.ajax({
		url: "/components/file_archiv/file_archiv_delete_ajax.jsp",
		data: data,
		success: function(result){
			if($.trim(result) === 'true')
			{
				window.close();
			}
			else
			{
				alert("<iwcm:text key="components.file_archiv.file_delete_error"/>");
			}
		}
	});
}

function Ok()
{
	//musi to ist takto, inak by sa nezavolal check form
	$("#submit").click();
}
</script>
<%
if(!Constants.getBoolean("fileArchivCanEdit"))
{
	out.print(prop.getText("components.file_archiv.service"));
	return;
}

if (Tools.getRequestParameter(request, "save") != null)
{
	pageContext.include("/sk/iway/iwcm/components/file_archiv/SaveFile.action");
}

if(request.getAttribute("divSuccess") != null)
{
	%><div style="padding:10px;" class="successUpload"><p style="color: green; font-weight: bold;" ><iwcm:text key="components.file_archiv.uplod_successful"/>.</p>
	<%if( request.getAttribute("fileArchivSameFiles") == null)
	{
		%>
		<script type="text/javascript">
		setTimeout(function(){
			window.opener.location.href = window.opener.location.href;
			window.close();
		}, 2000);
		</script>
		<%
	}
	else
	{
		FileArchivatorBean fileArchBean = (FileArchivatorBean)request.getAttribute("fileArchivLastFile");
		%><div  class="successUpload"><p><%
		out.print(prop.getText("components.file_archiv.text") +" "+fileArchBean.getFileName()+" :"+ prop.getText("components.file_archiv.text2") );
		%></p></div><%
		List<FileArchivatorBean> listSameFiles = (List<FileArchivatorBean>)request.getAttribute("fileArchivSameFiles");
		for(FileArchivatorBean fab:listSameFiles)
		{
			%><a href="<%="/"+ResponseUtils.filter(fab.getFilePath()+fab.getFileName())%>" target="blank"><%=ResponseUtils.filter(fab.getFilePath()+fab.getFileName())%></a>
			<%
			if(Tools.isNotEmpty(fab.getReferenceToMain()))
				out.print("("+prop.getText("components.file_archiv.pattern")+")<br/>");
			else
				out.print("("+prop.getText("components.file_archiv.main_file")+")<br/>");
		}
		%>
		<br/><input type="button" class="button150 action_delete"  name="action_delete" onclick="deleteFile(<%=fileArchBean.getId() %>);" value="<iwcm:text key="components.file_archiv.delete_file"/>" />
		<input type="button" class="button150 no_action"  name="no_action" value="<iwcm:text key="components.file_archiv.keep_file"/>" />
		<%
		return;
	}
	%></div><%
}
else if(Tools.getRequestParameter(request, "save") != null)
{
	%><div style="padding:10px;" class="successUpload"><p style="color: red; font-weight: bold;" ><iwcm:text key="components.file_archiv.upload_error"/></p></div><%
}

Identity user = UsersDB.getCurrentUser(request);
if(user == null || (oldId > 0 && !user.isFolderWritable("/"+ab.getFab().getFilePath())))
{
    Logger.debug(null, "Pouzivatel "+((user != null)?"id: "+user.getUserId():"null")+" nema pravo na EDITACIU suboru: "+ab.getFab().getFilePath()+ab.getFab().getFileName()+" ");
    request.setAttribute("errorText",prop.getText("logon.err.unauthorized_group"));
	%><jsp:include page="/components/maybeError.jsp" /><%
    return;
}
%>
<iwcm:stripForm name="saveArchiveFileForm" id="saveArchiveFileForm" action="<%=PathFilter.getOrigPathUpload(request) %>" method="post" beanclass="sk.iway.iwcm.components.file_archiv.SaveFileAction">
<stripes:errors/>
	<table class="tableFileUpload">
		<tr>
			<td><iwcm:text key="components.file_archiv.target_directory"/>:</td>
			<td width="350;">
				<div class="input-group">
					<stripes:text name="fab.filePath" class="form-control" readonly="true" id="filePath" />
					<span class="input-group-btn"><input type="button"  class="btn green" name="groupSelect" value="Vybrať" onClick='popupFromDialog("<iwcm:cp/>/admin/dialog_select_dir.jsp?rootDir=<%=FileArchivatorKit.getArchivPath()%>", 500, 500);'></span>
				</div>
			</td>
		</tr>
		<tr>
			<td><label for="file"><iwcm:text key="components.file_archiv.upload_file"/></label>:</td>
			<td style="padding-top:15px;" >
				<stripes:file id="file" name="file" class="required" />
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td>
			<%if(oldId > 0){
				%><em><iwcm:text key="components.file_archiv.allowed_file"/>: &nbsp;<%=FileArchivatorKit.getFileExtension(ab.getFab().getFileName())%></em><br><br>
			<%}else{ %>
				<em><iwcm:text key="components.file_archiv.allowed_files"/>: &nbsp;<%=Constants.getString("fileArchivAllowExt")%></em><br><br>
			<%}%>
			</td>
		</tr>
		<tr>
			<td><label for="virtual_file_name"><iwcm:text key="components.file_archiv.virtualFileName_file"/></label>:</td>
			<td>
				<stripes:text id="virtual_file_name" name="fab.virtualFileName" class="required form-control" />
			</td>
		</tr>
		<tr <%=(oldId > 0)?"style=\"display:none;\"":""%>>
			<td><label for="product"><iwcm:text key="components.file_archiv.product"/> </label>:</td>
			<td>
				<%
				List<LabelValueDetails> selectProducts = new ArrayList<>();
				List<String> allProducts = FileArchivatorDB.getDistinctListByProperty("product");
				Collections.sort(allProducts);
				for(String cat : allProducts){if(Tools.isNotEmpty(cat)) selectProducts.add(new LabelValueDetails(cat, cat));}
				pageContext.setAttribute("selectProducts", selectProducts);
				%>
				<iwcm:select styleId="category" property="fab.product" styleClass="form-control" enableNewTextKey="components.file_archiv.new_product" value="<%=ab.getFab().getProduct()%>">
					<option value=""><iwcm:text key="components.file_archiv.without_product"/></option>
					<html:options collection="selectProducts" property="label" labelProperty="label"/>
				</iwcm:select>
			</td>
		</tr>
		<tr <%=(oldId > 0)?"style=\"display:none;\"":""%>>
			<td><label for="category"><iwcm:text key="components.bazar.category"/></label>:</td>
			<td>
				<%
				List<LabelValueDetails> selectCategories = new ArrayList<>();
				List<String> allCategories = FileArchivatorDB.getAllCategories();
				Collections.sort(allCategories);
				for(String cat : allCategories){selectCategories.add(new LabelValueDetails(cat, cat));}
				pageContext.setAttribute("selectCategories", selectCategories);
				%>
				<iwcm:select styleId="category" property="fab.category" styleClass="form-control" enableNewTextKey="components.file_archiv.new_category" value="<%=ab.getFab().getCategory()%>">
					<html:options collection="selectCategories" property="label" labelProperty="label"/>
				</iwcm:select>
			</td>
		</tr>
		<tr <%=(oldId > 0)?"style=\"display:none;\"":""%>>
			<td><label for="productCode"><iwcm:text key="components.file_archiv.kod_produktu"/></label>:</td>
			<td>
				<stripes:text id="productCode" class="form-control" name="fab.productCode" />
			</td>
		</tr>
		<tr>
			<td><label for="showFile"><iwcm:text key="components.tips.view"/></label>:</td>
			<td>
				<stripes:checkbox id="showFile" value="true" name="fab.showFile"/>
			</td>
		</tr>
		<tr>
			<td><label for="validFrom"><iwcm:text key="components.file_archiv.date_from"/></label>:</td>
			<td>
				<div class="row" style="margin: 0;">
					<div class="col-xs-6" style="padding: 0;">
						<div data-date-format="dd.mm.yyyy" class="input-group date date-picker">
							<stripes:text id="validFrom" name="fab.validFrom" class="form-control datepicker" />
							<span class="input-group-btn">
								<button type="button" class="btn default"><i class="fa fa-calendar"></i></button>
							</span>
						</div>
					</div>
				</div>
			</td>
		</tr>
		<tr>
			<td><label for="validTo"><iwcm:text key="components.file_archiv.date_to"/></label>:</td>
			<td>
				<div class="row" style="margin: 0;">
					<div class="col-xs-6" style="padding: 0;">
						<div data-date-format="dd.mm.yyyy" class="input-group date date-picker">
							<stripes:text id="validTo" name="fab.validTo" class="form-control datepicker" />
							<span class="input-group-btn">
								<button type="button" class="btn default"><i class="fa fa-calendar"></i></button>
							</span>
						</div>
					</div>
				</div>
			</td>
		</tr>
		<tr>
			<td><label for="priority"><iwcm:text key="components.banner.priority"/></label>:</td>
			<td>
				<stripes:text id="priority" class="form-control" name="fab.priority" />
			</td>
		</tr>
		<tr>
			<td><iwcm:text key="components.file_archiv.main_file"/>:</td>
			<td>
				<stripes:text name="fab.referenceToMain" class="form-control" />
			</td>
		</tr>
		<tr>
			<td style="vertical-align: top;"><label for="note"><iwcm:text key="components.file_archiv.note"/>: </label></td>
			<td>
				<stripes:textarea class="form-control" id="note" name="fab.note" style="width: 350px;"/>
			</td>
		</tr>
		<tr>
			<td><label for="uploadLaterCheckboxId"><iwcm:text key="components.file_archiv.save_later"/></label>:</td>
			<td>
				<stripes:checkbox id="uploadLaterCheckboxId" name="uploadLater" onchange="showHideLaterUpload()"/>
			</td>
		</tr>
		<tr id="uploadLaterDateId">
			<td><label for="dateUploadLater"><iwcm:text key="components.file_archiv.later_date"/>:</label></td>
			<td>
			<div class="row" style="margin: 0;">
				<div class="col-xs-6" style="padding: 0;">
					<div data-date-format="dd.mm.yyyy" class="input-group date date-picker">
						<stripes:text name="dateUploadLater" id="dateUploadLater" class=" datepicker form-control" />
						<span class="input-group-btn">
							<button type="button" class="btn default"><i class="fa fa-calendar"></i></button>
						</span>
					</div>
				</div>
			</div>
			</td>
		</tr>
		<tr id="uploadLaterTimeId">
			<td><label for="timeUploadLater"><iwcm:text key="components.file_archiv.later_time"/>:</label></td>
			<td>
				<div class="col-xs-6" style="padding: 0;">
					<div class="input-group">
						<stripes:text name="timeUploadLater" id="timeUploadLater" class="form-control timepicker timepicker-24" />
						<span class="input-group-btn">
							<button type="button" class="btn default"><i class="fa fa-clock-o"></i></button>
						</span>
					</div>
				</div>
			</td>
		</tr>
		<tr id="uploadLaterEmailsId">
			<td><label for="emailsUploadLater"><iwcm:text key="components.file_archiv.emails"/>:</label></td>
			<td>
				<stripes:textarea class="uploadLaterEmailsTextArea form-control" id="emailsUploadLater" name="fab.emails"></stripes:textarea>
			</td>
		</tr>
		<tr>
			<td><label for="fieldA"><iwcm:text key="editor.field_a"/></label></td>
			<td>
				<stripes:text id="fieldA" class="form-control" name="fab.fieldA"/>
			</td>
		</tr>
		<tr>
			<td><label for="fieldB"><iwcm:text key="editor.field_b"/></label></td>
			<td>
				<stripes:text id="fieldB" class="form-control" name="fab.fieldB"/>
			</td>
		</tr>
		<%if(oldId > 0){ %>
		<tr>
			<td><label for="replaceCheckboxId"><iwcm:text key="components.file_archiv.replace"/></label>:</td>
			<td>
				<stripes:checkbox id="replaceCheckboxId" name="replace" onchange="setReplace()"/>
				<span id="doNotSaveIntoArchiv"><iwcm:text key="components.file_archiv.not_save"/></span>
			</td>
		</tr>
		<%} %>
		<tr>
			<td>&nbsp;</td>
			<td>
				<input type="hidden" id="uploadLaterId" name="uploadLater" value="false" />
				<input type="hidden" id="replaceId" name="replace" value="false" />
				<input type="hidden" name="save" value="true" />
				<input type="hidden" name="oldId" value="<%=oldId%>" />
 				<input type="submit"  id="submit" style="display:none;" name="submit" />
			</td>
		</tr>
	</table>
</iwcm:stripForm>

<script type="text/javascript">
	if(<%=Tools.isNotEmpty(ab.getFab().getReferenceToMain())%>)
		$('#replaceCheckboxId').prop("checked", true);

	$(function(){
		$('#file', window.opener.document).val('/sk/');
	});
</script>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>
