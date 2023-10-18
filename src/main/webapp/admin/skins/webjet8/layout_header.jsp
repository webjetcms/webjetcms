<%@page import="java.util.List"%><%@ page pageEncoding="utf-8" %>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.InitServlet"%>
<%@page import="sk.iway.iwcm.FileTools"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@page import="sk.iway.iwcm.gallery.GalleryDB"%>

<%!
  public static String getUserPhotoPath(HttpServletRequest request)
  {
    String photoPath = "/admin/skins/webjet8/assets/global/img/wj/avatar1.jpg";
    UserDetails ud = UsersDB.getCurrentUser(request);
    if (ud != null)
    {
      String photo = ud.getPhoto();
      if (Tools.isNotEmpty(photo))
      {
        photoPath = "/thumb" + GalleryDB.getImagePathSmall(photo) + "?w=50&h=50&ip=5";
      }
    }
    return photoPath;
  }
%>

<iwcm:checkLogon admin="true"/>
<div class="page-header navbar navbar-fixed-top">
	<!-- BEGIN TOP NAVIGATION BAR -->
	<div id="SmallWindow-MiniHeader">
		<div id="SmallWindow-HeaderHelper">
		    <div id="SmallWindow-HeaderHelper2"></div>
	   		<!-- <img src="/admin/skins/webjet8/assets/global/img/wj/logo.png" alt="logo" class="img-responsive"> -->
			<!-- <img src="/admin/skins/webjet8/assets/admin/layout/img/sidebar_toggler_icon_darkblue.png" alt="logo" class="img-responsive">
   -->
		</div>
	</div>
	<div id="SmallWindow-BigHeader">
		<div class="header-inner">
			<!-- BEGIN LOGO -->
			<style>.page-header.navbar .navbar-brand { padding-top: 4px; }</style>
			<a class="navbar-brand" href="/admin/v9/">
				<img src="/admin/v9/dist/images/logo-<%=InitServlet.getBrandSuffix()%>.png" alt="logo" data-toggle="tooltip" data-title="<iwcm:text key="admin.top.webjet_version"/> <%=sk.iway.iwcm.InitServlet.getActualVersionLong()%>" data-placement="bottom" />
			</a>
			<%
				if (InitServlet.isTypeCloud()==false && Constants.getBoolean("enableStaticFilesExternalDir"))
				{
					List<String> userDomains = GroupsDB.getInstance().getUserRootDomainNames(UsersDB.getCurrentUser(request).getEditableGroups());
					request.setAttribute("userDomains", userDomains);
					request.setAttribute("actualDomain", DocDB.getDomain(request));
					%>
					<script type="text/javascript">
						function changeDomain()
						{
							if (confirm("<iwcm:text key='admin.top.domena.confirm'/>")) {
								$.ajax({method: "POST", url: "/admin/skins/webjet8/change_domain_ajax.jsp", data: {domain: $("#actualDomainNameSelectId").val()}}).done(function(msg) {location.reload();});
							}
						}
					</script>
					<div class="actual-domain" id="actualDomainNameDiv" <% if (userDomains.size()<1) out.print("style='display: none;'"); %>>
						<i class="far fa-browser"></i>
						<select  name="actualDomainNameSelect" id="actualDomainNameSelectId" style="color: #000;" onchange="changeDomain();">
						<c:forEach items="${userDomains}" var="domain">
							<option value="${domain}" <c:if test="${domain == actualDomain}">selected="selected"</c:if>>${domain}</option>
						</c:forEach>
						</select>
					</div>
					<%
				}
			%>
			<!-- END LOGO -->
	        <!-- BEGIN VESION -->
	        <div class="navbar-version">

	        </div>
	        <!-- END VESION -->
			<!-- BEGIN RESPONSIVE MENU TOGGLER -->
			<script>
				function showHide_topMenu(element){
					if($('.top-menu').length>0)
						$('.top-menu').toggle();
				}
			</script>
			<a href="javascript:showHide_topMenu(this);" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
				<img src="/admin/skins/webjet8/assets/global/img/wj/menu-toggler.png" alt=""/>
			</a>
			<!-- END RESPONSIVE MENU TOGGLER -->
			<!-- BEGIN TOP NAVIGATION MENU -->
	        <div class="top-menu">
			<ul class="nav navbar-nav pull-right">

				<li class="dropdown helper">
					<a href="javascript:m_click_help()"><i class="far fa-question-circle"></i><iwcm:text key="menu.top.help"/></a>
				</li>

				<li class="dropdown" style="padding-right: 1px;">
					<a class="btn btn-sm btn-outline-secondary js-search-toggler" href="/admin/searchall.jsp">
						<i class="far fa-search"></i>
					</a>
				</li>

				<!-- BEGIN USER LOGIN DROPDOWN -->
				<li class="dropdown user">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown" data-close-others="true">
						<i class="far fa-address-card"></i>
						<span class="username">
							 <bean:write name="iwcm_useriwcm" property="fullName"/>
						</span>
						<i class="fa fa-angle-down"></i>
					</a>
					<ul class="dropdown-menu">
						<li style="display: none;">
							<a href="javascript:openPopupDialogFromLeftMenu('/admin/edituser.do?userid=<bean:write name="iwcm_useriwcm" property="userId"/>');">
								<i class="fa fa-user"></i> <iwcm:text key="components.forum.bb.profile"/>
							</a>
						</li>
						<li>
							<a href="javascript:openPopupDialogFromLeftMenu('/admin/2factorauth.jsp?userid=<bean:write name="iwcm_useriwcm" property="userId"/>');">
								<i class="fa fa-phone"></i> <iwcm:text key="user.gauth.title"/>
							</a>
						</li>
						<iwcm:menu name="cmp_crypto">
						<li>
							<a href="javascript:openPopupDialogFromLeftMenu('/components/crypto/admin/keymanagement');">
								<i class="fa fa-key"></i> <iwcm:text key="admin.keymanagement.title"/>
							</a>
						</li>
						</iwcm:menu>
						<li>
							<a href="<%=Constants.getString("adminLogoffLink") %>">
								<i class="icon-logout"></i> <iwcm:text key="menu.logout"/>
							</a>
						</li>
					</ul>
				</li>

	            <li class="dropdown dropdown-quick-sidebar-toggler">
	                <a class="dropdown-toggle" href="/logoff.do?forward=/admin/index.jsp">
						<i class="far fa-sign-out"></i>
	                </a>
	            </li>


				<!-- END USER LOGIN DROPDOWN -->
			</ul>

	        </div>
			<!-- END TOP NAVIGATION MENU -->
		</div>
	</div>
	<!-- END TOP NAVIGATION BAR -->
</div>