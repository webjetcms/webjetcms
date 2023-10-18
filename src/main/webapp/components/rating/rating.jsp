<%@page import="java.util.List"%><%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ page import="sk.iway.iwcm.users.*" %>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.components.rating.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
String status = "";
String errMessage = "";

if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(org.apache.struts.Globals.XHTML_KEY, "true", PageContext.PAGE_SCOPE);
PageParams pageParams = new PageParams(request);
int pageDocId = Integer.valueOf(request.getParameter("docid").toString()).intValue();
int fromDocId = pageParams.getIntValue("fromDocId", pageDocId); //docId stranky, v ktorej je vlozena komponenta
int ratingType = pageParams.getIntValue("ratingType", 1);
boolean checkLogon = pageParams.getBooleanValue("checkLogon", true);
int range = pageParams.getIntValue("range", 10);
int ratingDocId = pageParams.getIntValue("ratingDocId", -1);
//pokial je > 0, tak umozni uzivatelovi hlasovat viackrat s min odstupom rateAgainCycle hodin. Inak umozni zahlasovat len raz
int rateAgainCycleInHours = pageParams.getIntValue("rateAgainCycleInHours", 0);
if (ratingDocId < 1) ratingDocId = Tools.getDocId(request);

RatingBean rBean = new RatingBean();
DocDB docDB = DocDB.getInstance();

if (checkLogon)
{
	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
	if (user != null)
	{
			if (ratingType == 1)
			{
				   RatingBean rBean2 = RatingDB.getRatingByUserByDoc(user.getUserId(), ratingDocId, rateAgainCycleInHours);
					if (rBean2 != null)
					{
						rBean = rBean2;
						request.setAttribute("isRated", "");
					}
					else
					{
						request.setAttribute("ratingBar", "");
					}
			}
	}
	else
	{
			if (ratingType == 1)
			{
				request.setAttribute("notLogged", "");
			}
	}
}
else
{
		if (ratingType == 1)
		{
			//	request.setAttribute("ratingBar", "");
			//	session.setAttribute("checkLogon", "false");
		}
}

if (ratingType == 2)
{
	rBean = RatingDB.getDocIdRating(ratingDocId);
	request.setAttribute("docIdRating", "true");
}

if (ratingType == 3)
{
	int usersLength = pageParams.getIntValue("usersLength", 10);
	List topUsers = RatingDB.getUsersTopList(usersLength);
	if (topUsers.size() > 0)
	{
		request.setAttribute("usersTopList", topUsers);
	}
}

if (ratingType == 4)
{
	int docsLength = pageParams.getIntValue("docsLength", 10);
	int period = pageParams.getIntValue("period", 7);
	List topDocIds = RatingDB.getDocIdTopList(docsLength, period);
	if (topDocIds.size() > 0)
	{
		request.setAttribute("docIdTopList", topDocIds);
	}
}

%>

<script type="text/javascript">
	<!--
		function showDiv(){

			el = document.getElementById("registracia");
			if (el != null){
				el.style.display = "block";
				}
			}
	//-->
</script>

<div class="rating">

<!--  RATING STRANKY -->

	<logic:present name="docIdRating">
		<b>Rating: <%= rBean.getRatingValue()%> /<%= range%></b><br />
		<b>Users: <%= rBean.getTotalUsers()%></b><br />
		<br />
	</logic:present>

	<!--  TOP POUZIVATELIA -->

	<logic:present name="usersTopList">
		<table style="width:400px;">
			 <thead>
			   <tr>
				 <th style="width:30px;">#</th>
				 <th style="width:270px;">Nick</th>
				 <th style="width:50px;">Activity</th>
				 <th style="width:50px;">Rank</th>
			   </tr>
			 </thead>
			 <tbody>
				 <logic:iterate name="usersTopList" id="u" type="sk.iway.iwcm.components.rating.RatingBean" indexId="index" >
							<% UserDetails userDet = UsersDB.getUser(u.getUserId());
								 if (userDet != null)
								 {%>
						  <tr>
								<td><%=index.intValue()+1%>.&nbsp;</td>
								<td><%=userDet.getLogin()%></td>
								<td><bean:write name="u" property="ratingStat"/></td>
								<td>
									<%
										if(userDet.getRatingRank() < 100)
											out.println("rookie");
										else if(userDet.getRatingRank() >= 100 && userDet.getRatingRank() < 500)
											out.println("cooler");
										else if(userDet.getRatingRank() >= 500)
											out.println("guru");
										out.println("<br/>");
									%>
								</td>
						 </tr>
							<%}%>
					</logic:iterate>
			 </tbody>
		</table>
	</logic:present>

	<!--  TOP STRANKY -->

	<logic:present name="docIdTopList">

		<table width="400">
			 <thead>
			   <tr>
				 <th width="30">Rank</th>
				 <th width="270">Doc title</th>
				 <th width="50">Activity</th>
				 <th width="50">Rating</th>
			   </tr>
			 </thead>
			 <tbody>
				 <logic:iterate name="docIdTopList" id="d" type="sk.iway.iwcm.components.rating.RatingBean" indexId="index" >
							<% DocDetails docDet = docDB.getDoc(d.getDocId());
								 if (docDet != null)
								 {%>
						<tr>
								<td><%=index.intValue()+1%>.&nbsp;</td>
								<td><%=docDet.getTitle()%></td>
								<td><bean:write name="d" property="ratingStat"/></td>
								<td><bean:write name="d" property="ratingValue"/> /<%= range%></td>
						</tr>
						<%}%>
					</logic:iterate>
			 </tbody>
		</table>

	</logic:present>

</div>

	<logic:present name="ratingBar">

		<b>
			*** Funkcny form ***
		</b>

		<html:form name="ratingForm" action="/rating.do" method="get" type="sk.iway.iwcm.components.rating.RatingBean">
			<table>
				<tr align=center style='font-weight: bold;'>
						<%for (int i=0; i<range; i++)
						{%>
					<td>
						<%=(i+1)%>
					</td>
						<%}%>
				</tr>

				<tr>
						<%for (int i=0; i<range; i++)
						{%>
					<td>
						<input type='radio' name='ratingValue' class="border" value='<%=(i+1)%>'/>
					</td>
						<%}%>
					<td>
						<input type='hidden' name='fromDocId' value='<%=Tools.getDocId(request)%>' />
						<input type='hidden' name='docId' value='<%=ratingDocId%>' />
						<input type='hidden' name='rateAgainCycleInHours' value='<%=rateAgainCycleInHours%>' />
						<input type='hidden' name='action' value='save' />
						<input type='submit' value='Rate!' />
					</td>
				</tr>
			</table>
		</html:form>
	</logic:present>

	<!--User prihlaseny, ak sa uz za stranku hlasovalo, tu bude ratingBar ktory updatne udaje o existujucom ratingu v DB-->

	<logic:present name="isRated" >

		<b>
			DocID <%=ratingDocId%> uz bolo hodnotene !!!<br />
			<%if(rateAgainCycleInHours > 0){%>
				<iwcm:text key="components.rating.hlasovat_opat" param1="<%=String.valueOf(rateAgainCycleInHours)%>"/>
			<%}%>
		</b>
	<br />

		<b>
			Rating: <%= rBean.getRatingValue()%>
		</b>
	<br />
		<%if(rateAgainCycleInHours < 1){%>
		<html:form name="ratingForm" action="/rating.do" method="post" type="sk.iway.iwcm.components.rating.RatingBean">
			<table>
				<tr align=center style='font-weight: bold;'>
						<%for (int i=0; i<range; i++)
						{%>
					<td>
						<%=(i+1)%>
					</td>
						<%}%>
				</tr>

				<tr>
						<%for (int i=0; i<range; i++)
						{%>
					<td>
						<input type='radio' name='ratingValue' class="border" value='<%=(i+1)%>' />
					</td>
						<%}%>
					<td>
						<input type='hidden' name='fromDocId' value='<%=Tools.getDocId(request)%>' />
						<input type='hidden' name='docId' value='<%=ratingDocId%>' />
						<input type='hidden' name='rateAgainCycleInHours' value='<%=rateAgainCycleInHours%>' />
						<input type='hidden' name='action' value='update' />
						<input type='submit' value='Rate!' />
					</td>
				</tr>
			</table>
		</html:form>
		<%}%>
	</logic:present>

	<!--Ak user nie je prihlaseny /registrovany, pricom namiesto submit sa zobrazi DIV s hlaskou o registracii-->

	<logic:present name="notLogged" >

		<b>Uživatel nie je prihlasený !!! </b>

		<table>
			<tr align='center' style='font-weight: bold;'>
					<%for (int i=0; i<range; i++)
					{%>
				<td>
					<%=(i+1)%>
				</td>
					<%}%>
			</tr>

			<tr>
					<%for (int i=0; i<range; i++)
					{%>
				<td>
					<input type='radio' name='ratingValue' class="border" value='<%=(i+1)%>' />
				</td>
					<%}%>
				<td>
					<input type='button' value='Rate!' onclick="javascript: showDiv();" />
				</td>
			</tr>
		</table>

		<div id="registracia" style="display: none;" >
			<p>
				<b>
					<iwcm:text key="components.rating.not_logged_user"/> !
				</b>
			</p>
		</div>
	</logic:present>

	<%
		request.removeAttribute("isRated");
		request.removeAttribute("notLogged");
		request.removeAttribute("ratingBar");
		request.removeAttribute("docIdRating");
		request.removeAttribute("usersTopList");
		request.removeAttribute("docIdTopList");
		rBean = null;
	%>
