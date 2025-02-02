<%@page import="java.util.List"%><%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.Adminlog"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Logger,sk.iway.iwcm.PathFilter,sk.iway.iwcm.Tools,
sk.iway.iwcm.io.IwcmFile,sk.iway.iwcm.stripes.SyncDirAction,sk.iway.iwcm.sync.WarningListener,sk.iway.iwcm.system.ConfDB,
sk.iway.iwcm.system.ConfDetails,java.beans.XMLDecoder,java.io.File"%>

<%@page import="java.io.FileInputStream, java.io.InputStream, java.util.*"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>


	<%Prop prop = Prop.getInstance();
	request.setAttribute("dialogTitle", prop.getText("admin.conf_import.import_konfigurácie"));%>

<%@ include file="/admin/layout_top_dialog.jsp" %>

<%!
private static String normalizeName(String s)
{
	return ((s.replace("[", "_")).replace("]", "_")).replace(".","_");
}

private static int findByName(List<ConfDetails> arr, String name)
{
	if(arr == null || name == null )
		return -1;

   for (int i=0; i<arr.size(); i++)
   {
      if (arr.get(i).getName().equals(name)== true)
      	return i;
   }
   return -1;
}

public class SortByName implements Comparator<ConfDetails> {
   public int compare(ConfDetails cd1, ConfDetails cd2) {
       return cd1.getName().compareTo(cd2.getName());
   }
}

%>

<script type="text/javascript">

	var check_all = false;

	$(document).ready(function(){
		$('.new_val').keyup(function repaintInputs()
		{
			$(' .org_val').each( function()
			{
				try {
					var act = $(this).attr("name");
					var act_val =  $(this).val();
					var neww = 'new'+act.substr(3,act.length);
					var check = act.substr(4,act.length);

					var neww_val = $("input[name$="+neww+"]").val();

					if(act_val == neww_val)
					{	<%--yelow & orange   bude po novom seda--%>
							$(this).css({'border': '1px solid #cccccc'});
							$("input[name$="+neww+"]").css({'border':'1px solid #cccccc'});
					}
					else if ( act_val.length > 0 )
					{	<%--red--%>
							$(this).css({'border':'1px solid #FF0000'});
							$("input[name$="+neww+"]").css({'border':'1px solid #FF0000'});
					}
					else
					{	<%--green--%>
							$(this).css({'border':'1px solid #4fd46e'});
							$("input[name$="+neww+"]").css({'border':'1px solid #4fd46e'});
					}
				} catch (e) {}
			});
		});

		$('.chk_cls').click(function reStyleBorderInputs()
		{
			$(' .chk_cls').each( function()
			{
				var newInput = $("input[name$='new_"+$(this).attr('name').substr(3,$(this).attr('name').length)+"']");
				var actInput = $("input[name$='act_"+$(this).attr('name').substr(3,$(this).attr('name').length)+"']");

				if($(this).is(':checked'))
				{
					newInput.css({'color':'#4fd46e','text-decoration':'none'});
					if( newInput.val() != actInput.val())
					actInput.css({'color':'#555555','text-decoration':'line-through'});
				}
				else
				{
					if( newInput.val() != actInput.val()) {
                        newInput.css({'color': '#555555', 'text-decoration': 'line-through'});
                        actInput.css({'color': '#4fd46e', 'text-decoration': 'none'});
                    }
                    else
					{
                        newInput.css({'color': '#555', 'text-decoration': 'none'});
                        actInput.css({'color': '#555', 'text-decoration': 'none'});
					}
				}
			});
		});

		$('.import_href').click(function()
			{
				$('.chk_cls').each(function()
				{
					if(check_all)
					{
						$(this).attr('checked',false);
					}
					else
					{
						$(this).attr('checked',true);
					}
				});
				if(check_all)
					check_all = false ;
				else
					check_all = true;
			});

 		$('.new_val').first().keyup();
	});

	$('#xmlFile').click(function uploadOK(){$('#saveFileForm').click();});

	function Ok()
	{
		if($("#saveFileForm").length > 0)
		{
			var file = $('#xmlFile').val();
			if(file.length > 0)
			{
				if(file.indexOf(".xml") > 0)
					$('#saveFileForm').click();
				else
					alert('Súbor nie je v správnom formáte');
			}
			else
			{
				alert('Nebol vybraný súbor');
			}
		}
		else if ($("#submitKonfForm").length > 0)
		{
			$('#submitKonfForm').click();
		}
		else
			window.close();
	}
</script>


<%
if(Tools.getRequestParameter(request, "konf_saved") != null && Tools.getRequestParameter(request, "konf_saved").equals("yes"))
{
	request.setAttribute("successful","yes_was");
}

if (Tools.getRequestParameter(request, "saveFile") != null)
{
	pageContext.include("/sk/iway/iwcm/system/ConfImport.action");
}

if( request.getAttribute("successful") == null )
{%>
<div class="padding10">
	<h3>
<%-- 	<iwcm:text key="admin.conf_import.import_konfigurácie"/> --%>
<iwcm:text key="components.conf_import_popup.choose_file"/>
	</h3>

	<iwcm:stripForm name="saveFileForm" id="saveFileForm_ID" action="<%=PathFilter.getOrigPathUpload(request)%>" method="post" beanclass="sk.iway.iwcm.system.ConfImportAction">
		<div style="min-width: 1000px; height: 400px;">
			<stripes:file name="xmlFile"  id="xmlFile"  />
			<input type="submit" class="button" id="saveFileForm" name="saveFile" style="display:none" value="Načítať">
		</div>
	</iwcm:stripForm>
</div>
<%
}
else
{
	List<ConfDetails> allInOne = new ArrayList<ConfDetails>();
	String fileName = ResponseUtils.filter(Tools.getParamAttribute("file_name",request));

	if( fileName == null || fileName.length() < 3)
	{
		out.print("Nebol vybrany subor");
		return;
	}

	File f = new File(Tools.getRealPath("/WEB-INF/tmp/"+fileName));
	InputStream is = new FileInputStream(f);
	is = SyncDirAction.checkXmlForAttack(is);
	XMLDecoder decoder = new XMLDecoder(is, null, new WarningListener());
	Object objUpdates = decoder.readObject();

	if (!(objUpdates instanceof ArrayList))
	{
		out.print("Nepodporovany typ suboru");
		decoder.close();
		return;
	}

	//zo suboru
	List<ConfDetails> updates = (List<ConfDetails>)objUpdates;

	decoder.close();
	if(updates != null)
	{
		for (int i=0; i<updates.size(); i++)
		{
			allInOne.add( updates.get(i));
		}

		// z aktualneho WJ
		if (ConfDB.getConfig() != null)
			request.setAttribute("conf", ConfDB.getConfig());
		List conf = (ArrayList)request.getAttribute("conf");
		List<ConfDetails> confSearch = new ArrayList<ConfDetails>();

		Iterator iter;
		iter = conf.iterator();
		ConfDetails con;

		while (iter.hasNext())
		{
			con = (ConfDetails)iter.next();
			confSearch.add(con);
		}

		for(int i = 0; i< confSearch.size(); i++)
		{
			allInOne.add(confSearch.get(i));
		}

		// vymazeme duplicity
		for(int i=0; i < ( allInOne.size() ); i++) {
			for(int j = i+1; j < (allInOne.size() ) ; j++) {
				if( allInOne.get(i).getName().equals(allInOne.get(j).getName())) {
					allInOne.remove(j);
				}
			}
		}

		Collections.sort(allInOne, new SortByName());

		if(request.getAttribute("successful").equals("true"))
		{
			%>
			<div class="padding10" style="padding:0px;">
			<div style="height: 463px; overflow: auto;">
			<form name=konf_var method="POST" action="<%=PathFilter.getOrigPath(request)%>">
			<table name="vypis_konfiguracie" class="sort_table"  cellspacing="0" cellpadding="1" >
			<%
				int actWjIndex = findByName(confSearch, "installName");
				int fileWjIndex = findByName(updates, "installName");
				String actWJ = (actWjIndex >= 0)?  confSearch.get(actWjIndex).getValue():"";
				String fileWJ = (fileWjIndex >= 0)?  updates.get(fileWjIndex).getValue():"";
			%>

			<tr><th class="sortable" ><iwcm:text key="admin.conf_import.meno_premennej"/>:</th><th><iwcm:text key="admin.conf_import.aktualna_hodnota"/>: <%=actWJ %></th><th>  <iwcm:text key="admin.conf_import.nova_hodnota"/>: <%=fileWJ %></th><th><a href="#" class="import_href"><iwcm:text key="admin.conf_import.importovat"/></a></th></tr>

			<%String actValue="", fileValue="";
			int parny=0,pom=-1;
			ConfDetails cfd;
			Date date;
			String actDate="", fileDate="", checkedChboxAct="", checkedChboxFile="";
			for(ConfDetails cf : allInOne)
			{
				parny++;
				checkedChboxAct = checkedChboxFile = actValue = fileValue = fileDate = actDate ="";
				fileDate = actDate = "<br>&nbsp;";
				date = null;
				pom = findByName(confSearch,cf.getName());
				if(pom >= 0)
				{
					cfd = confSearch.get(pom);
					actValue = cfd.getValue();
					actDate = (cfd.getDateChanged() != null) ? "<br>"+Tools.formatDateTimeSeconds((Date)cfd.getDateChanged()) : "<br>&nbsp;";
					date = cfd.getDateChanged();
				}
				else
					checkedChboxAct="checked=\"checked\"";

				pom = findByName(updates,cf.getName()) ;
				if(pom >= 0)
				{
					cfd = updates.get(pom);
					fileValue = cfd.getValue();
					if(fileValue.length() == 0 && actValue.length()>0)
						checkedChboxAct="checked=\"checked\"";

					fileDate = (cfd.getDateChanged() != null) ? "<br>"+Tools.formatDateTimeSeconds((Date)cfd.getDateChanged()) : "<br>&nbsp;";

					if(cfd.getDateChanged() != null && date != null && date.before(cfd.getDateChanged()) )
						checkedChboxAct="checked=\"checked\"";
				}

				String normalized = normalizeName(cf.getName());

				out.print("<tr class=\""); if(parny%2 == 0) out.print("even");else out.print("odd"); out.print( "\"><td>"+cf.getName()+"</td>"+
					"<td><input  type=\"text\" class=\"org_val \" name=\"act_"+normalized+"\" value=\""+ResponseUtils.filter(actValue)+"\" readonly size=\"40\" />"
					+actDate+"</td>"+
					"<td><input  type=\"text\" class=\"new_val\" name=\"new_"+normalized+"\" value=\""+ResponseUtils.filter(fileValue)+"\" size=\"40\"  />"
					+fileDate+"</td>"+
					"<td style=\"text-align:center;\"><input id=\""+normalized+"\" name=\"ch_"+normalized+"\" class=\"chk_cls\" "+checkedChboxAct+" type=\"checkbox\">&nbsp; </td></tr>");
			}%>
			</table>
			<input type="hidden" name="file_name" value="<%=request.getAttribute("file_name")%>">
			<input type="hidden" name="konf_saved" value="yes">
			<input type="submit" id="submitKonfForm" style="display: none;">
			</form>
			</div>
			</div>
			<%
		}
		else
		{
			%>
			<div class="padding10">
				<%
				int parny=0;
				for(ConfDetails cf : allInOne)
				{
					String normalized = normalizeName(cf.getName());
					String actValue = Tools.getRequestParameterUnsafe(request, "act_"+normalized);
					String newValue = Tools.getRequestParameterUnsafe(request, "new_"+normalized);
					if( "on".equals(Tools.getRequestParameter(request, "ch_"+normalized)) && actValue != null)
					{
						parny++;
						if(parny == 1)
						{%>
							<table name="vypis_konfiguracie_po_update" class="sort_table"  cellspacing="0" cellpadding="1">
							<tr><th class="sortable" ><iwcm:text key="admin.conf_import.meno_premennej"/></th><th><iwcm:text key="admin.conf_import.stara_hodnota"/> </th><th>  <iwcm:text key="admin.conf_import.nova_hodnota"/> </th></tr><%
						}
						out.print("<tr class=\""); if(parny%2 == 0) out.print("even");else out.print("odd"); out.print( "\"><td>"+cf.getName()+" </td><td> "+actValue+" </td><td> "+newValue+"</td>");
						ConfDB.setName(cf.getName(), newValue);
					}
				}
				if(parny >= 1){
					%></table><%}
				out.print("<p>"+parny+" ");%>
				<iwcm:text key="admin.conf_import.zmenene_premenne"/></p><%

				//vymazanie nacitaneho konfiguracneho suboru
				String file = "/WEB-INF/tmp/"+fileName;
				if (Tools.isNotEmpty(file))
				{
					try
					{
						IwcmFile fi = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(file));
						if (fi.exists())
						{
							Logger.debug(null,"conf_import.jsp: DELETING FILE: "+file);
							Adminlog.add(Adminlog.TYPE_FILE_DELETE, "Delete file "+file, -1, -1);
							fi.delete();
						}
					}
					catch (Exception ex)
					{
						%><b><iwcm:text key="admin.conf_import.subor_nebol_vymazany" param1="<%=file%>"/></b><%
					}
				}
				%>
				<script type="text/javascript">
					try {
						window.opener.configurationDatatable.ajax.reload();
					} catch (e) {}
				</script>
			</div>
			<%
		}
 	}
}

%>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>
