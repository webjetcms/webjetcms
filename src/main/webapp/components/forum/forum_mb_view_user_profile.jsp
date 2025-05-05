<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.*,sk.iway.iwcm.doc.*" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

Prop prop = Prop.getInstance(lng);

int allowedUserGroup = -1;
if(request.getAttribute("allowedUserGroup") != null)
	allowedUserGroup = sk.iway.iwcm.Tools.getIntValue((String)request.getAttribute("allowedUserGroup"), -1);
int docId = Tools.getIntValue(request.getParameter("docid"), -1);
DocDB docDB = DocDB.getInstance();

int userId = Tools.getIntValue(request.getParameter("uId"), -1);

int rootForumId = Tools.getIntValue(request.getParameter("rootForumId"),-1);
if (request.getParameter("rootForumId")==null)
{
   //vydedukuj
   GroupDetails actualGroup = (GroupDetails)request.getAttribute("pageGroupDetails");
   GroupDetails parentGroup = null;
   if(actualGroup != null) parentGroup = GroupsDB.getInstance().getGroup(actualGroup.getParentGroupId());
   if (parentGroup != null) rootForumId = parentGroup.getDefaultDocId();
}

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

UserDetails userDet = null;
if(userId > 0 )
{

	String[] groups;
	userDet = UsersDB.getUser(userId); // (UserDetails) it.next();
	//zobrazime usera len ak odoslal nejaky prispevok (aby nam nevykradali user DB)
	if (userDet != null && userDet.getForumRank()>0)
	{
		if(allowedUserGroup == -1) {
			request.setAttribute("userInfo", "");
		} else {
			groups = Tools.getTokens(userDet.getUserGroupsIds(), ",");
			for(int i=0; i<groups.length; i++)
			{
				if(allowedUserGroup == Tools.getIntValue(groups[i], -1))
				{
					request.setAttribute("userInfo", "");
				}
			}
		}
		request.setAttribute("userDet", userDet);
	}
}

if (userDet!=null && user!=null && userDet.getUserId()==user.getUserId())
{

}
%>
<script type="text/javascript">
<!--
document.write('<style type="text/css" media="screen">	@import "/components/forum/forum_mb_silver.css"; </style>');
-->
</script>
<iwcm:present name="userInfo">
   <h1><iwcm:text key="components.forum.bb.profile.info_about"/> <bean:write name="userDet" property="fullName"/></h1>

   <div class="row mobile-fix">
		<div class="col-md-12 col-xs-12">
			<nav aria-label="breadcrumb">
				<ol class="breadcrumb">
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(rootForumId)%>"><iwcm:text key="components.forum.show_topics"/></a></li>
					<li class="breadcrumb-item"><a href="<%=docDB.getDocLink(docId)%>"><bean:write name="doc_title"/></a></li>
				</ol>
			</nav>
		</div>
	</div>

	<div class="panel panel-primary">
		<div class="panel-heading">
			<iwcm:text key="components.forum.bb.profile.info_about"/>: <bean:write name="userDet" property="fullName"/>
		</div>
		<div class="panel-body no-padding">
			<div class="row">
				<div class="col-md-3 col-xs-12 post-info post-info-left">
					<b><span class="gen"><iwcm:text key="components.forum.bb.profile.avatar"/></span></b>
				</div>
				<div class="col-md-9 col-xs-12">
					<div class="row">
						<div class="col-md-4 text-right">
							<b><span class="gen"><iwcm:text key="components.forum.bb.profile.all_about"/> <bean:write name="userDet" property="fullName"/></span></b>
						</div>
					</div>
				</div>
			</div>
			<div class="row">
				<div class="col-md-3 col-xs-12 post-info post-info-left">
					<%
			   String photo = userDet.getPhoto();
				if (Tools.isEmpty(photo)) photo = Constants.getString("ntlmDefaultUserPhoto");
			   if(Tools.isNotEmpty(photo)){ %>
			   		<img style="margin:5px 0px 5px 0px;" src="/thumb<%=photo%>?w=150&amp;h=150&amp;ip=1" style="border:0px;">
				<% } %>
			   <br /><span class="postdetails"><%if(userDet.getForumRank() < 100) out.println(prop.getText("components.forum.bb.rank.lt_100"));
							else if(userDet.getForumRank() >= 100 && userDet.getForumRank() < 500) out.println(prop.getText("components.forum.bb.rank.100-500"));
							else if(userDet.getForumRank() >= 500) out.println(prop.getText("components.forum.bb.rank.gt-500"));%></span>

				</div>
				<div class="col-md-9 col-xs-12">
					<div class="row">
						<div class="col-md-4 text-right">
							<span class="gen"><iwcm:text key="components.forum.bb.profile.joined"/>:</span>
						</div>
						<div class="col-md-8">
							<span class="gen"><%=Tools.formatDateTime(userDet.getRegDate())%></span>
						</div>
					</div>
					<div class="row">
						<div class="col-md-4 text-right">
							<span class="gen"><iwcm:text key="components.forum.bb.profile.total_posts"/>:</span>
						</div>
						<div class="col-md-8">
							<span class="gen"><bean:write name="userDet" property="forumRank"/></span> <br /><span class="genmed"><a href="<%=Tools.addParameterToUrl(docDB.getDocLink(docId),"uId",String.valueOf(userDet.getUserId()))%>&amp;type=user_posts"><iwcm:text key="components.forum.user_posts"/></a></span>
						</div>
					</div>
					<div class="row">
						<div class="col-md-4 text-right">
							<span class="gen"><iwcm:text key="components.forum.bb.profile.location"/>:</span>
						</div>
						<div class="col-md-8">
							<span class="gen"><bean:write name="userDet" property="city"/><logic:notEmpty name="userDet" property="country">,
							<bean:write name="userDet" property="country"/></logic:notEmpty></span>
						</div>
					</div>
					<div class="row">
						<div class="col-md-4 text-right">
							<span class="gen"><iwcm:text key="components.forum.bb.profile.email"/>:</span>
						</div>
						<div class="col-md-8">
							<span class="gen"><a href="mailto:<bean:write name="userDet" property="email"/>"><bean:write name="userDet" property="email"/></a></span>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</iwcm:present>
<logic:notPresent name="userInfo">
   <iwcm:text key="components.forum.view_profile.user_not_found"/>
</logic:notPresent>
