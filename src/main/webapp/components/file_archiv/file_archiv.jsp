<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.components.file_archiv.*,sk.iway.iwcm.common.SearchTools" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%=Tools.insertJQuery(request)%>
<%
try
{
	PageParams pageParams = new PageParams(request);
	String dirPath = pageParams.getValue("dir",FileArchivatorKit.getArchivPath());
	if(!dirPath.endsWith("/"))
		dirPath += "/";

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

	<div class="documents">
		<h2><iwcm:text key="components.file_archiv.heading"/></h2>
		<ul>
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
			FileArchivatorBean paternFab;
			int count  =  0;
			do
			{
				for(int i=0;i<fabList.size();i++)
				{
					if(referenceId == -1 && fabList.get(i).getReferenceId() == -1)
					{
						%><li>
							<a class="download-link inline" target="blank" href="<%="/"+fabList.get(i).getFilePath()+fabList.get(i).getFileName() %>">
								<span class="media-body">
									<%=fabList.get(i).getVirtualFileName() %>
									<span class="note">[<%=Tools.formatFileSize(fabList.get(i).getFileSize())%>]  </span>
								</span>
								<span class="media-right">
									<span class="badge" title="<%=fabList.get(i).getFileName().contains(".")?fabList.get(i).getFileName().substring(fabList.get(i).getFileName().lastIndexOf(".")+1):""%>"><%=fabList.get(i).getFileName().contains(".")?fabList.get(i).getFileName().substring(fabList.get(i).getFileName().lastIndexOf(".")+1):""%></span>
								</span>
							</a>
							<%paternFab = FileArchivatorDB.getPatern(fabList.get(i).getFilePath()+fabList.get(i).getFileName());
							//System.out.println(fabList.get(i).getFilePath()+fabList.get(i).getFileName());
							if(paternFab != null){ %>
								<ul><li><a class="vzor-link inline" target="blank" href="<%="/"+paternFab.getFilePath()+paternFab.getFileName()%>"><iwcm:text key="components.file_archiv.pattern"/></a></li></ul>
							<%
							}
							%>

						<%
						if(Tools.isNotEmpty(fabList.get(i).getNote())) {%>
							<div class="info-box">(<%=SearchTools.htmlToPlain(fabList.get(i).getNote())%>)</div><%
						}

						if(FileArchivatorDB.getNumberOfReference(fabList, fabList.get(i).getId()) > 0)
						{
							referenceId = fabList.get(i).getId();
						}
						else
						{%><%}
						fabList.remove(i);
						canPrintH4 = true;
						%></li><%
						break;
					}

					if(referenceId > 0 && fabList.get(i).getReferenceId() == referenceId)
					{
						if(canPrintH4){%>
						<li class="archive">
							<div class="media-left"><h4><iwcm:text key="components.file_archiv.archiv"/></h4></div>
							<ul class="media-body">
							<%canPrintH4 = false;
							}

							for(FileArchivatorBean fab : FileArchivatorDB.getReference(referenceId, pageParams.getValue("order", "reference"), pageParams.getBooleanValue("asc", true)))
							{%>
								<li><a class="download-link inline" target="blank" href="<%="/"+fab.getFilePath()+fab.getFileName() %>">

									<%if(Tools.isEmpty(Tools.formatDate(fab.getValidFrom())) || Tools.isEmpty(Tools.formatDate(fab.getValidTo()))) {
										%><span class="media-body"><%=fab.getVirtualFileName()%>
											<span class="note">[<%=Tools.formatFileSize(fabList.get(i).getFileSize())%>]</span>
										</span><%
									}else{
										%><span class="media-body"><iwcm:text key="components.file_archiv.valid_from_till" param1="<%=Tools.formatDate(fab.getValidFrom())%>" param2="<%=Tools.formatDate(fab.getValidTo())%>"/>
									    	<span class="note">[<%=Tools.formatFileSize(fabList.get(i).getFileSize())%>]  </span>
										</span><%
									} %>
										<span class="media-right">
											<span class="badge" title="<%=fabList.get(i).getFileName().contains(".")?fabList.get(i).getFileName().substring(fabList.get(i).getFileName().lastIndexOf(".")+1):""%>"><%=fabList.get(i).getFileName().contains(".")?fabList.get(i).getFileName().substring(fabList.get(i).getFileName().lastIndexOf(".")+1):""%></span>
										</span>
									</a>
								</li>
								<%
								if(FileArchivatorDB.getNumberOfReference(fabList, referenceId) == 1)
								{
									referenceId = -1;
									%></ul></li><%
								}
								FileArchivatorDB.removeById(fabList, fab.getId());
							}
					}
				}
				count++;

				//out.print("count: "+count);
			}while(maxCount*3 > count && fabList.size() > 0);
		}
		else
		{
			%><p class="u-font-size-s"><iwcm:text key="components.file_archiv.files_not_found"/></p><%
		}
		%>
		</ul>
	</div>
	<%
}
catch(Exception ex)
{
	sk.iway.iwcm.Logger.error(ex);
}
 %>