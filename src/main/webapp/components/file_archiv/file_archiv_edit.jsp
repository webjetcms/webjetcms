<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.components.file_archiv.*" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<stripes:useActionBean var="ab" beanclass="sk.iway.iwcm.components.file_archiv.FileArchivatorAction"/>
<%
	sk.iway.iwcm.i18n.Prop prop = Prop.getInstance(request);

	//nemozeme mat null
	if(ab.getFab() == null)
	{
		out.print(prop.getText("components.file_archiv.upload.wrong_oldId", request.getParameter("edit")));
		return;
	}

	request.setAttribute("cmpName", prop.getText("components.file_archiv.name"));
	request.setAttribute("dialogTitleKey", prop.getText("components.file_archiv.name"));
	String dialogDescKey;
	if(ab.getFab().getId() > 0)
	{
		dialogDescKey = prop.getText("components.file_archiv.edit_file", ab.getFab().getFilePath()+ab.getFab().getFileName());
	}
	else
	{
		out.print(prop.getText("components.file_archiv.upload.wrong_oldId", request.getParameter("edit")));
		return;
	}
	request.setAttribute("dialogDescKey", dialogDescKey);
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<script src="/components/form/check_form.js" type="text/javascript"></script>
<script>

function selectText(containerid) {
	let range;
	if (document.selection) {
        range = document.body.createTextRange();
        range.moveToElementText(containerid);
        range.select();
    } else if (window.getSelection) {
        range = document.createRange();
        range.selectNode(containerid);
        window.getSelection().addRange(range);
    }
}

function Ok()
{
	//musi to ist takto, inak by sa nezavolal check form
	$("#submit").click();
}
</script>
<style type="text/css">
	.tableFileUpload{ margin: 0;}
</style>
<%
if(!Constants.getBoolean("fileArchivCanEdit"))
{
	out.print(prop.getText("components.file_archiv.service"));
	return;
}

Identity user = UsersDB.getCurrentUser(request);

if(user == null || !user.isFolderWritable("/"+ab.getFab().getFilePath()))
{
	Logger.debug(null, "Pouzivatel "+((user != null)?"id: "+user.getUserId():"null")+" nema pravo na EDITACIU suboru: "+ab.getFab().getFilePath()+ab.getFab().getFileName()+" ");
	request.setAttribute("errorText",prop.getText("logon.err.unauthorized_group"));
	%><jsp:include page="/components/maybeError.jsp" /><%
	return;
}

if(ab.getFab().getReferenceId() != -1)
{
	if(user.isDisabledItem("cmp_fileArchiv_edit_del_rollback"))
	{
		//@Deprecated nechavame len kvoli historii
		if(Tools.isEmpty(Constants.getString("fileArchivCanEditUsers")) ||
			Tools.containsOneItem(Tools.getTokens(Constants.getString("fileArchivCanEditUsers"), ","), user.getLogin()) == false)
		{
			out.println(prop.getText("components.file_archiv.access"));
			return;
		}
	}
}

if (Tools.getRequestParameter(request, "save") != null  )
{

	pageContext.include("/sk/iway/iwcm/components/file_archiv/FileArchivator.action");
	%>
	<script type="text/javascript">
	if(window.opener.location.indexOf('file_list.jsp') != -1)
		window.opener.location.href = "/components/file_archiv/file_list.jsp";
	else if(window.opener.location.indexOf('file_sub_list.jsp') != -1)
		window.opener.location.href = "/components/file_archiv/file_list.jsp";
	</script><%
}
%>

<iwcm:stripForm name="saveArchiveFileForm" id="saveArchiveFileForm" action="<%=PathFilter.getOrigPath(request)%>" method="post" beanclass="sk.iway.iwcm.components.file_archiv.FileArchivatorAction">
<stripes:errors/>
<table class="tableFileUpload">
	<tr>
		<td><label for="virtual_file_name"><iwcm:text key="components.file_archiv.virtualFileName_file"/></label>:</td>
		<td><stripes:text id="virtual_file_name" name="fab.virtualFileName" size="30" class="required form-control"/></td>
	</tr>
	<tr>
		<td><label for="product"><iwcm:text key="components.file_archiv.product"/> </label>:</td>
		<td>
			<%
			List<LabelValueDetails> selectProducts = new ArrayList<>();
			List<String> allProducts = FileArchivatorDB.getDistinctListByProperty("product");
			Collections.sort(allProducts);
			for(String cat : allProducts){if(Tools.isNotEmpty(cat)) selectProducts.add(new LabelValueDetails(cat, cat));}
			pageContext.setAttribute("selectProducts", selectProducts);
			%>
			<iwcm:select styleId="product" property="fab.product" styleClass="form-control" enableNewTextKey="components.file_archiv.new_product" value="<%=ab.getFab().getProduct()%>">
				<option value=""><iwcm:text key="components.file_archiv.without_product"/></option>
				<html:options collection="selectProducts" property="label" labelProperty="label" />
			</iwcm:select>
		</td>
	</tr>
	<tr>
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
				<html:options collection="selectCategories" property="label" labelProperty="label" />
			</iwcm:select>
		</td>
	</tr>
	<tr>
		<td><label for="productCode"><iwcm:text key="components.file_archiv.kod_produktu"/></label>:</td>
		<td>
			<stripes:text id="productCode" class="form-control" name="fab.productCode"/>
		</td>
	</tr>
	<tr>
		<td><label for="showFile"><iwcm:text key="components.tips.view"/></label>:</td>
		<td>
			<stripes:checkbox id="showFile" checked="true" value="true" name="fab.showFile"/>
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
								<button type="button" class="btn default"><i class="ti ti-calendar"></i></button>
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
								<button type="button" class="btn default"><i class="ti ti-calendar"></i></button>
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
	<%-- vzor moze byt iba jeden samostatny subor ktory nie je vo vlakne a nema historicke subory --%>
	<tr <%=!Tools.isEmpty(FileArchivatorDB.getByReferenceId(Tools.getIntValue(Tools.getRequestParameter(request, "edit"), -1))) ? "style=\"display:none;\"" : "" %>>
		<td><iwcm:text key="components.file_archiv.link_to_main_file"/>:</td>
		<td>
			<stripes:text class="form-control"  name="fab.referenceToMain" />
		</td>
	</tr>
		<tr>
		<td style="vertical-align: top;"><label for="note"><iwcm:text key="components.file_archiv.note"/>:</label></td>
		<td>
			<stripes:textarea id="note" class="form-control" name="fab.note" style="width:350px;"/>
		</td>
	</tr>
	<tr>
		<td><iwcm:text key="components.file_archiv.link_on_file"/>:</td>
		<td>
			<div class="form-control" style="border-style: solid; border-color: #bdbcbc; border-width: 1px; background-color: #edeff1;" onclick="selectText(this)">&lt;a href="/${ab.fab.filePath}${ab.fab.fileName}"&gt;${ab.fab.virtualFileName}&lt;/a&gt;</div>
		</td>
	</tr>
	<tr>
		<td><iwcm:text key="components.file_archiv.link_for_link"/>:</td>
		<td>
			<div class="form-control" style="border-style: solid; border-color: #bdbcbc; border-width: 1px; background-color: #edeff1;" class="divFileHref" onclick="selectText(this)">/${ab.fab.filePath}${ab.fab.fileName}</div>
		</td>
	</tr>
	<tr>
		<td><label for="fieldA"><iwcm:text key="editor.field_a"/></label></td>
		<td>
			<stripes:text id="fieldA" class="form-control" name="fab.fieldA" />
		</td>
	</tr>
	<tr>
		<td><label for="fieldB"><iwcm:text key="editor.field_b"/></label></td>
		<td>
			<stripes:text id="fieldB" class="form-control" name="fab.fieldB" />
		</td>
	</tr>
</table>

<input type="hidden" name="fab.nnFileArchiveId" value="${ab.fab.id}" />
<input type="hidden" name="edit" value="${ab.fab.id}"/>
<stripes:hidden name="fab.fieldC"/>
<stripes:hidden name="fab.fieldD"/>
<stripes:hidden name="fab.fieldE"/>
<input type="hidden" name="save" value="true" />
<input type="hidden" name="setMd5" value="true" />
<input type="submit"  id="submit" style="display:none;" name="submit" />
</iwcm:stripForm>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>
