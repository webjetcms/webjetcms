<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%!

public String getValidityDates(FileArchivatorBean fab, Prop prop) {
	String from = Tools.formatDate(fab.getValidFrom());
	String to = Tools.formatDate(fab.getValidTo());
	if(Tools.isNotEmpty(from) && Tools.isNotEmpty(to)) {
		return prop.getText("components.file_archiv.validFromTo", from, to);
	} else if(Tools.isNotEmpty(from)) {
		return prop.getText("components.file_archiv.validFrom", from);
	} else if(Tools.isNotEmpty(to)) {
		return prop.getText("components.file_archiv.validTo", to);
	}
	return "";
}

%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(lng);

	PageParams pageParams = new PageParams(request);
	String dirPath = pageParams.getValue("dir",FileArchivatorKit.getArchivPath());
	if(!dirPath.endsWith("/")) dirPath += "/";

	List<FileArchivatorBean> fabListCache = new ArrayList<>();
	//ak chcem zobrazit len vybrane subory (tzn. nie doplnit do existujuceho zoznamu) zoznam necham prazdny
	if(pageParams.getBooleanValue("showOnlySelected",false) == false)
	{
		fabListCache = FileArchivatorDB.getByConditions(null, FileArchivatorDB.createCollection("product", request),
				FileArchivatorDB.createCollection("category", request), FileArchivatorDB.createCollection("productCode", request), dirPath,
				//podmienka na orderMain kvoli spatnej kompatibilite kedy ascMain este neexistovalo a riadilo sa na zaklade asc
				pageParams.getBooleanValue("subDirsInclude", false), pageParams.getValue("orderMain", null) != null ? pageParams.getBooleanValue("ascMain", false) : pageParams.getBooleanValue("asc", false), true, true);
	}

	// pocet includov
	int fileArchivCount = Tools.getIntValue(""+request.getAttribute("file_archiv_include_count"),-1);
	request.setAttribute("file_archiv_include_count", ++fileArchivCount);
	%>


<div class="table-responsive table-responsive-file-archive">
<table class="table table-striped table-small table-file-archive">
	<tbody>

		<%
		//pozadovane globalIds, zo String pola do Integer listu
		String[] globalIdsArray = Tools.getTokens(pageParams.getValue("globalIds", ""), "+");
		List<Integer> globalIdsList = new ArrayList<>();
		for (String s : globalIdsArray)
		{
			globalIdsList.add(Tools.getIntValue(s, -1));
		}

		List<FileArchivatorBean> fabList = new ArrayList<>(fabListCache);

		for(int i=0;i<fabList.size();i++)
		{
			//odstranime archiv
			if(!pageParams.getBooleanValue("archiv", true))
			{
				if(fabList.get(i).getReferenceId() != -1)
				{
					fabList.remove(i);
					i--;
					continue;
				}
			}
			//odstranime globalne Id z listu pridavanych, ak uz je zahrnute v zakladnom liste
			int indexGlobal = globalIdsList.indexOf(fabList.get(i).getGlobalId());
			if(indexGlobal > -1)
			{
				globalIdsList.remove(indexGlobal);
			}
		}

		//pridame dokumenty s pozadovanymi globalIds
		List<FileArchivatorBean> globalList = FileArchivatorDB.getByGlobalId(globalIdsList);
		if(globalList != null && globalList.size() > 0)
		{
			//pridame hlavne subory
			fabList.addAll(globalList);
			//ak pridavame aj archvine subory
			if(pageParams.getBooleanValue("archiv", true))
			{
				for(FileArchivatorBean archivFab:globalList)
				{
					fabList.addAll(FileArchivatorDB.getByReferenceId(archivFab.getId()));
				}
			}
			//12.4.2017 #21687
			for(int i=0;i<fabList.size();i++)
			{
				if(!fabList.get(i).getShowFile())
				{
					fabList.remove(i);
					i--;
				}
			}
		}

		if(pageParams.getValue("orderMain", null) != null)
			FileArchivatorDB.sortBy(fabList, pageParams.getValue("orderMain", "time"), pageParams.getBooleanValue("ascMain", true));

		int maxCount = fabList.size();
		if(fabList.size() > 0)
		{
			int referenceId = -1;
			//boolean isSetReference = false;
			boolean canPrintH4 = false;
			FileArchivatorBean paternFab = null;
			int count  =  0;
			do
			{
				for(int i=0;i<fabList.size();i++)
				{
					boolean liIsOpen = false;

					if(referenceId == -1 && fabList.get(i).getReferenceId() == -1)
					{
						liIsOpen = true;
						%>
						<tr>
							<td class="td-file-archive-name"><a href="<%="/"+fabList.get(i).getFilePath()+fabList.get(i).getFileName()+"?v="+fabList.get(i).getDateInsert().getTime() %>" target="_blank"><%=fabList.get(i).getVirtualFileName() %></a></td>
							<td class="td-file-archive-note"><%
								String note = "";
								String validityDates = getValidityDates(fabList.get(i), prop);
								if(Tools.isNotEmpty(fabList.get(i).getNote())) {
									note = "<span class=\"note\">"+fabList.get(i).getNote();
									if (Tools.isNotEmpty(validityDates)) {
										note += ", ";
									}
									note += "</span>";
								}
								if(Tools.isNotEmpty(validityDates)) {
									note += "<span class=\"validity\">"+validityDates+"</span>";
								}
								out.print(note);
							%></td>
							<td class="td-file-archive-size lighter"><%=fabList.get(i).getFileName().contains(".")?fabList.get(i).getFileName().substring(fabList.get(i).getFileName().lastIndexOf(".")+1).toUpperCase():""%> <%=Tools.formatFileSize(fabList.get(i).getFileSize())%></td>
							<td class="td-file-archive-link"><a class="icon download" href="<%="/"+fabList.get(i).getFilePath()+fabList.get(i).getFileName()+"?v="+fabList.get(i).getDateInsert().getTime() %>" target="_blank">Download</a></td>

								<%paternFab = FileArchivatorDB.getPatern(fabList.get(i).getFilePath()+fabList.get(i).getFileName());
							//System.out.println(fabList.get(i).getFilePath()+fabList.get(i).getFileName());
							if(paternFab != null){ %>
								<ul><li><a class="vzor-link inline" target="blank" href="<%="/"+paternFab.getFilePath()+paternFab.getFileName()%>">Vzor</a></li></ul>
							<% paternFab = null;
							}%>

						<%

						if(FileArchivatorDB.getNumberOfReference(fabList, fabList.get(i).getId()) > 0)
						{
							referenceId = fabList.get(i).getId();
						}
						else
						{%><%}
						fabList.remove(i);
						canPrintH4 = true;
						if (liIsOpen) {
						liIsOpen=false;
						%></tr><%
						}
						break;
					}

					if(referenceId > 0 && fabList.get(i).getReferenceId() == referenceId)
					{
						if(canPrintH4){%>
						<tr class="archive"><td colspan="4">
							<div class="media-left"><strong><iwcm:text key="components.file_archiv.archiv"/></strong></div>
							<ul class="media-body">
							<%canPrintH4 = false;
							}

							for(FileArchivatorBean fab : FileArchivatorDB.getReference(referenceId, pageParams.getValue("order", "reference"), pageParams.getBooleanValue("asc", true)))
							{%>
								<li>
                                    <a class="download-link inline" target="_blank" href="<%="/"+fab.getFilePath()+fab.getFileName() %>"><span class="media-body"><%=fab.getVirtualFileName()%></span></a>
                                    <%
                                    if(Tools.isNotEmpty(fab.getNote())) out.println("<span class=\"note\"> - "+fab.getNote()+", </span>");
									String validityDates = getValidityDates(fab, prop);
									if(Tools.isNotEmpty(validityDates)) {
										out.println("<span class=\"validity\">"+validityDates+", </span>");
									}%>
                                    <span class="fileSize"><%=fab.getFileName().contains(".")?fab.getFileName().substring(fab.getFileName().lastIndexOf(".")+1).toUpperCase():""%> <%=Tools.formatFileSize(fab.getFileSize())%></span>
								</li>
								<%
								if(FileArchivatorDB.getNumberOfReference(fabList, referenceId) == 1)
								{
									referenceId = -1;
									%></ul></li>
									<%
								}
								FileArchivatorDB.removeById(fabList, fab.getId());
							}
						//fabList.remove(i);

						//continue;

					}
					if (liIsOpen) {
						%></td></tr><%
					}
				}
				count++;

				//out.print("count: "+count);
			}while(maxCount*3 > count && fabList.size() > 0);
		}%>

		</tbody>
	</table>
	</div>

 <style type="text/css">
a.icon.download {
    background-image: url(/components/file_archiv/download.png);
}
a.icon.plus {
    background-image: url(/components/file_archiv/plus.png);
}
a.icon {
    display: inline-block;
    width: 20px;
    height: 20px;
    text-indent: -9000px;
    background-position: center center;
    background-repeat: no-repeat;
    background-size: contain;
}
 </style>