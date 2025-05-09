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
		<div class="header-inner ly-header">
			<!-- BEGIN LOGO -->
			<style>.page-header.navbar .navbar-brand { padding-top: 4px; }</style>
			<a class="navbar-brand" href="/admin/v9/">
				<img src="/admin/v9/dist/images/logo-<%=InitServlet.getBrandSuffix()%>.svg" alt="logo" data-bs-toggle="tooltip" title="<iwcm:text key="admin.top.webjet_version"/> <%=sk.iway.iwcm.InitServlet.getActualVersionLong()%>" data-bs-placement="bottom" />
			</a>
			<div class="header-container">

			<div class="header-title"></div>
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
						$("body").toggleClass("top-menu-open");
					}
				</script>
				<a href="javascript:showHide_topMenu(this);" class="navbar-toggle">
					<img src="/admin/skins/webjet8/assets/global/img/wj/menu-toggler.png" alt=""/>
				</a>
				<!-- END RESPONSIVE MENU TOGGLER -->
				<!-- BEGIN TOP NAVIGATION MENU -->
				<div class="top-menu">
				<ul class="nav navbar-nav pull-right">

					<li class="dropdown helper">
						<a href="javascript:m_click_help()"><i class="ti ti-help"></i><iwcm:text key="menu.top.help"/></a>
					</li>

					<li class="dropdown" style="padding-right: 1px;">
						<a class="btn btn-sm js-search-toggler" href="/admin/searchall.jsp">
							<i class="ti ti-search fs-6"></i>
						</a>
					</li>

					<!-- BEGIN USER LOGIN DROPDOWN -->
					<li class="dropdown user">
						<a id="userDropdownMenu" href="#" class="dropdown-toggle" data-bs-toggle="dropdown" data-close-others="true">
							<i class="ti ti-user"></i>
							<span class="username">
								<iwcm:strutsWrite name="iwcm_useriwcm" property="fullName"/>
							</span>
							<i class="ti ti-chevron-down"></i>
						</a>
						<ul class="dropdown-menu" aria-labelledby="userDropdownMenu">
							<li style="display: none;">
								<a href="javascript:openPopupDialogFromLeftMenu('/admin/edituser.do?userid=<iwcm:strutsWrite name="iwcm_useriwcm" property="userId"/>');">
									<i class="ti ti-user"></i> <iwcm:text key="components.forum.bb.profile"/>
								</a>
							</li>
							<li>
								<a href="javascript:openPopupDialogFromLeftMenu('/admin/2factorauth.jsp?userid=<iwcm:strutsWrite name="iwcm_useriwcm" property="userId"/>');">
									<i class="ti ti-device-mobile-message"></i> <iwcm:text key="user.gauth.title"/>
								</a>
							</li>
							<iwcm:menu name="cmp_crypto">
							<li>
								<a href="javascript:openPopupDialogFromLeftMenu('/components/crypto/admin/keymanagement');">
									<i class="ti ti-key"></i> <iwcm:text key="admin.keymanagement.title"/>
								</a>
							</li>
							</iwcm:menu>
							<li>
								<a href="javascript:document.adminLogoffForm.submit()">
									<i class="ti ti-logout"></i> <iwcm:text key="menu.logout"/>
								</a>
							</li>
						</ul>
					</li>

					<li class="dropdown dropdown-quick-sidebar-toggler">
						<a class="dropdown-toggle js-logout-toggler" href="javascript:document.adminLogoffForm.submit()">
							<i class="ti ti-logout"></i>
						</a>
						<form action="<%=Constants.getString("adminLogoffLink") %>" method="post" name="adminLogoffForm" style="display:none">
							<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfTokenInputFiled(session)%>
						</form>
					</li>


					<!-- END USER LOGIN DROPDOWN -->
				</ul>

				</div>
				<!-- END TOP NAVIGATION MENU -->
			</div>
		</div>

		<div class="ly-submenu"></div>
	</div>
	<!-- END TOP NAVIGATION BAR -->
</div>