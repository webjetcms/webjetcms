<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@page import="sk.iway.iwcm.tags.WriteTag"%>
<% if (request.getAttribute("layout_closeMainTable")==null) { %>
	</TD>
  </TR>
<% } else { %>
   <TABLE border="0" cellpadding="0" cellspacing="0" width="100%">
<% } %>
  <tr class="hideComponentIframe">
		  <td valign="bottom" height="26">

				<TABLE border=0 cellpadding=0 cellspacing=0  width="100%">
				  <TR>
					 <TD width="166"><img src="<iwcm:cp/>/admin/images/hmskin/bottom_left.gif" width="166" height="26" border=0></TD>
					 <TD width="99%" background="<iwcm:cp/>/admin/images/hmskin/bottom_right.gif" class="mhskinGenerationTime" align="top">

							<%
							try
							{
								if (request.getAttribute("generationStartDate")!=null)
								{
									java.util.Date generationStartDate = (java.util.Date)request.getAttribute("generationStartDate");
									long startTime = generationStartDate.getTime();
									long endTime = (new java.util.Date()).getTime();
									long generationTimeDiff = endTime - startTime;
									%><iwcm:text key="layout.page_generated_at"/>: <%=generationTimeDiff%> ms<%
									//session.removeAttribute("generationStartDate");
								}
							}
							catch (Exception ex)
							{
								sk.iway.iwcm.Logger.error(ex);
							}
							%>

					 </TD>
				  </TR>
				</TABLE>

		</td>
	</tr>
</table>
<script type="text/javascript">
if (document.getElementById("waitDiv")!=null) document.getElementById("waitDiv").style.display="none";
</script>
</body>
<html>
