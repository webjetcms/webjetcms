<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="java.io.IOException"%>
<%@page import="sk.iway.iwcm.io.IwcmFsDB"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit"%>
<%@page import="net.sourceforge.stripes.action.FileBean"%>
<%@page import="java.io.File"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB"%><%@
page import="java.util.List"%><%@
page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<%
	request.setAttribute("cmpName", "components.file_archiv.name");
	request.setAttribute("dialogTitleKey", "components.file_archiv.save_after_this_title");
	request.setAttribute("dialogDescKey", " ");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<%!
	/**
	 * ulozi subor na novu poziciu, vrati jeho novy nazov
	 * @param fab FileArchivatorBean
	 * @param destinationPath cielova destinacia
	 * @return uniqueFileName
	 */
	private static String saveFile(FileArchivatorBean fab, String destinationPath)
	{
		File fOld = new File(Tools.getRealPath(fab.getFilePath()+fab.getFileName()));
		FileBean fileBean = new FileBean(fOld, "", fab.getFileName());

		String newPath = newPath(destinationPath);
		String dateStamp = FileArchivatorKit.getDateStampAsString(fab.getDateInsert());
		String uniqueFileName = FileArchivatorKit.getUniqueFileName(fileBean, newPath, dateStamp);

		String realPath = Tools.getRealPath(newPath+uniqueFileName);
		File f = new File(realPath);
		try
		{
			IwcmFsDB.writeFiletoDest(fileBean.getInputStream(), f, (int)fileBean.getSize());
		}
		catch(IOException ioex)
		{
			//indikuje chybu pri ukladani
			uniqueFileName = null;

			//Logger.debug(SaveFileAction.class, "Nastala chyba '"+ioex.getMessage()+"' pri ukladani suboru "+f.getPath());
			sk.iway.iwcm.Logger.error(ioex);
		}
		return uniqueFileName;
	}

	private static String newPath(String oldPath)
	{
		return Tools.replace(oldPath, "files/archiv/archiv_insert_later/", "");
	}
 %><iwcm:checkLogon/><%
/*
Vlozi subor do archivu medzi dva konkretne archivy
*/


String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);
List<FileArchivatorBean> fileArchiveList;
fileArchiveList = FileArchivatorDB.getWaitingFileList();

request.setAttribute("fileArchiveList", fileArchiveList);
int fabId = -1;

int move_below_archiv_file = Tools.getIntValue(""+Tools.getRequestParameter(request, "move_below_archiv_file"),-1);
int later_insert_id = Tools.getIntValue(""+Tools.getRequestParameter(request, "later_insert_id"),-1);

if(move_below_archiv_file > 0 && later_insert_id > 0)
{
	//ziskam si subor later edit,
	//ulozim ho tam, kde je jeho predchodca
	//updatnem mu bean a ulozim ho
	//update urobim tiez podla predchodcu ?
	//zmazem povodny subor
	FileArchivatorBean fileLaterInsertBean = FileArchivatorDB.getInstance().getById(later_insert_id);
	FileArchivatorBean fileMoveBelowBean = FileArchivatorDB.getInstance().getById(move_below_archiv_file);
	if(fileLaterInsertBean == null || fileMoveBelowBean == null)
	{
		out.print(prop.getText("components.file_archiv.not_found_archiv_record"));
		return;
	}
	String insertLaterOldPath = fileLaterInsertBean.getFilePath() + fileLaterInsertBean.getFileName();

	String newFileName = saveFile(fileLaterInsertBean, fileMoveBelowBean.getFilePath());
	if(Tools.isEmpty(newFileName))
	{
		out.print(prop.getText("components.file_archiv.move_file_failed"));
		return;
	}

	fileLaterInsertBean.setFileName(newFileName);
	fileLaterInsertBean.setFilePath(fileMoveBelowBean.getFilePath());
	fileLaterInsertBean.setUploaded(-1);
	fileLaterInsertBean.setGlobalId(fileMoveBelowBean.getGlobalId());
	fileLaterInsertBean.setReferenceId(fileMoveBelowBean.getReferenceId());
	// ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !
	//fileLaterInsertBean.setOrderId(orderId)
	// ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !
	//prebrat aj kod, produktovy kod atd ?

	//treba zmazat povodny subor
	IwcmFile iwFile = new IwcmFile(Tools.getRealPath(insertLaterOldPath));
	if(!iwFile.exists() || !iwFile.delete())
		out.print(prop.getText("components.file_archiv.main_file_delete_fail"));

	if(fileLaterInsertBean.save())
		out.print(prop.getText("components.file_archiv.record_between_saved_success"));
	else
		out.print(prop.getText("components.file_archiv.record_saved_fail"));
	//Malo by fungovat OK, potrebujem, aby to MHO este otestoval
	//tu sme skoncili treba otestovat

	//inkrementujeme order id
	for(FileArchivatorBean archivBean : FileArchivatorDB.getByReferenceId(fileMoveBelowBean.getReferenceId()))
	{
		if(archivBean.getOrderId() > fileMoveBelowBean.getOrderId())
		{
			archivBean.setOrderId(archivBean.getOrderId() + 1);
			archivBean.save();
		}
		else if(archivBean.getOrderId() == -1)// prave vlozeny fileLaterInsertBean
		{
			archivBean.setOrderId(fileMoveBelowBean.getOrderId() + 1);
			archivBean.save();
		}
	}
	//premazeme cache
	FileArchivatorKit.deleteFileArchiveCache();
	return;
}
if(fileArchiveList != null && fileArchiveList.size() > 0)
{
%>
<display:table class="sort_table" name="fileArchiveList" defaultsort="1" pagesize="20" defaultorder="descending"  uid="row">
<% if(row != null)
	{
		fabId = ((FileArchivatorBean)row).getId();
	}%>
	<display:column title="Id" property="nnFileArchiveId"/>
	<display:column titleKey="formslist.tools" class="addWidth">
		<a href="?move_below_archiv_file=<%=move_below_archiv_file%>&later_insert_id=<%=fabId%>"><iwcm:text key="components.file_archiv.insert_between"/></a>
	</display:column>
	<display:column titleKey="components.file_archiv.virtualFileName" property="virtualFileName"/>
	<display:column titleKey="components.file_archiv.real_name" property="fileName"/>
	<display:column titleKey="components.file_archiv.directory" property="filePath"/>
	<display:column titleKey="components.file_archiv.file_list_cakajuce.nahrat_po" property="dateUploadLater" decorator="sk.iway.displaytag.DateTimeDecorator"/>
	<display:column titleKey="inquiry.valid_since" property="validFrom" decorator="sk.iway.displaytag.DateDecorator"/>
	<display:column titleKey="inquiry.valid_till" property="validTo" decorator="sk.iway.displaytag.DateDecorator"/>
	<display:column titleKey="components.file_archiv.product" property="product"/>
	<display:column titleKey="components.bazar.category" property="category"/>
	<display:column titleKey="components.file_archiv.code" property="productCode"/>
	<display:column titleKey="editor.show" property="showFile" decorator="sk.iway.displaytag.BooleanDecorator"/>
	<display:column titleKey="components.banner.priority" property="fieldC"/>
	<display:column titleKey="components.file_archiv.pattern" >
	<% if(row != null && Tools.isNotEmpty(((FileArchivatorBean)row).getReferenceToMain()))
	{
		out.print(((FileArchivatorBean)row).getReferenceToMain());
	}else{%>
		<img height="12px" width="12px" src="/components/_common/images/icon_false.png" alt="">
	<%} %>
	</display:column>
	<display:column titleKey="components.file_archiv.reference" property="referenceId"/>
</display:table>
<%
}
else
{
	out.print(prop.getText("components.file_archiv.waiting_list_not_found"));
}
%>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>