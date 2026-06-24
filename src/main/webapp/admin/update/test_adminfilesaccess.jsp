<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@page import="java.net.HttpURLConnection"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.io.BufferedInputStream"%>
<%@page import="java.net.URL"%>
<%@page import="java.io.IOException"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.common.FileBrowserTools" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/><%!

//safe or non admin files
private String[] safePages = new String[] {
	"/components/_common/cleditor/jquery.cleditor.js.jsp",
	"/components/_common/combine.jsp",
	"/components/_common/fulltext_preview.jsp",
	"/components/_common/image_info_ajax.jsp",
	"/components/_common/javascript/ajax_form_send.js.jsp",
	"/components/_common/javascript/page_functions.js.jsp",
	"/components/_common/javascript/password_strenght.js.jsp",
	"/components/_common/thymeleaf/write.jsp",
	"/components/_common/universal_edit_dialog.jsp",
	"/components/_common/universal_list.jsp",
	"/components/_common/upload/upload.jsp",
	"/components/_common/wysiwyg/empty.jsp",
	"/components/_common/wysiwyg/wysiwyg.jsp",
	"/components/adresar/main.jsp",
	"/components/adresar/list.jsp",
	"/components/app-date/actual_year.jsp",
	"/components/app-social_icon/social_icon.jsp",
	"/components/app-testimonials/news.jsp",
	"/components/banner/db_convert.jsp",
	"/components/basket/repeat_payment.jsp",
	"/components/basket/order_payment_reply.jsp",
	"/components/basket/_invoice_security_guard.jsp",
	"/components/basket/navbar.jsp",
	"/components/basket/jscript.jsp",
	"/components/basket/order_form.jsp",
	"/components/basket/basket_page.jsp",
	"/components/basket/basket_small.jsp",
	"/components/basket/bootstrap_products.jsp",
	"/components/basket/invoice_detail.jsp",
	"/components/basket/js.jsp",
	"/components/basket/product_perex.jsp",
	"/components/basket/products.jsp",
	"/components/blog/blog_user_toolbar.jsp",
	"/components/carousel_slider/carousel_slider.jsp",
	"/components/date/actual_year.jsp",

	"/components/eshop/basket/basket-order.jsp",
	"/components/eshop/shop/includes/blind-friendly-panel.jsp",
	"/components/eshop/shop/includes/bootstrap-elements-2.jsp",
	"/components/eshop/shop/includes/bootstrap-elements.jsp",
	"/components/eshop/shop/includes/breadcrumb.jsp",
	"/components/eshop/shop/includes/browser-support.jsp",
	"/components/eshop/shop/includes/debug-info.jsp",
	"/components/eshop/shop/includes/head.jsp",
	"/components/eshop/shop/includes/header.jsp",
	"/components/eshop/shop/includes/html-attributes.jsp",
	"/components/eshop/shop/includes/sidebar.jsp",
	"/components/eshop/shop/modules/md-banner.jsp",
	"/components/eshop/shop/modules/md-category-header.jsp",
	"/components/eshop/shop/modules/md-our-benefits.jsp",
	"/components/eshop/shop/modules/md-product-detail.jsp",
	"/components/eshop/shop/modules/md-product-list.jsp",
	"/components/eshop/shop/modules/md-product-slider.jsp",
	"/components/eshop/shop/modules/md-subcategory-selector.jsp",

	"/components/export/json.jsp",

	"/components/form/check_form_impl.jsp",
	"/components/form/double_opt_in.jsp",
	"/components/form/invisible_captcha_ajax.jsp",
	"/components/form/set_invisible_catpcha_ajax.jsp",
	"/components/form/set_re_catpcha_ajax.jsp",
	"/components/form/spamprotectiondisable.jsp",

	"/components/forum/forum.jsp",
	"/components/forum/forum_mb.jsp",
	"/components/forum/forum_mb_open.jsp",
	"/components/forum/forum_mb_search.jsp",
	"/components/forum/forum_mb_search_user_posts.jsp",
	"/components/forum/iframe.jsp",
	"/components/forum/new.jsp",
	"/components/forum/paging_component.jsp",
	"/components/forum/paging_component_search.jsp",
	"/components/forum/saveok.jsp",

	"/components/gallery/gallery.jsp",
	"/components/gallery/photoswipe/photoswipe.jsp",

	"/components/gdpr/admin_list_search_detail_velocity.jsp",
	"/components/gdpr/cookie_save_ajax.jsp",
	"/components/gdpr/gtm_init.jsp",

	"/components/grideditor/phantom/phantom_sablona_ajax.jsp",
	"/components/iframe_blank.jsp",
	"/components/iframe_blank_transparent.jsp",
	"/components/inquiry/ajax_vote.jsp",
	"/components/inquiry/fail.jsp",
	"/components/inquiry/ok.jsp",
	"/components/map/google_map.jsp",
	"/components/maybeError.jsp",
	"/components/messages/message_popup.jsp",
	"/components/messages/message_popup_bottom.jsp",
	"/components/messages/message_popup_refresher.jsp",
	"/components/messages/message_popup_top.jsp",
	"/components/messages/message_popup_top_most.jsp",
	"/components/messages/refresher-ac.jsp",

	"/components/news/news-velocity.jsp",
	"/components/news/tags.jsp",
	"/components/news-calendar/news_calendar-ajax_utf-8.jsp",
	"/components/qa/qa.jsp",
	"/components/rating/ajax-star-rating.jsp",
	"/components/reloadParentClose.jsp",
	"/components/reloadParentFrame.jsp",

	"/components/reservation/addReservation.jsp",
	"/components/reservation/email_reservation_detail.jsp",
	"/components/reservation/reservation-ajax_utf-8.jsp",
	"/components/reservation/reservation_getimage.jsp",
	"/components/reservation/reservation_list.jsp",
	"/components/reservation/reservation_object_reservation_form.jsp",
	"/components/reservation/room_list.jsp",

	"/components/restaurant_menu/menu-01.jsp",
	"/components/restaurant_menu/menu-02.jsp",
	"/components/restaurant_menu/menu-03.jsp",
	"/components/restaurant_menu/menu-04.jsp",
	"/components/restaurant_menu/menu.jsp",
	"/components/search/ac.jsp",
	"/components/send_link/send_link_form.jsp",
	"/components/server_monitoring/monitor.jsp",
	"/components/site_browser/site_browser.jsp",
	"/components/slider/slider.jsp",
	"/components/stat/stat_async_ajax.jsp",

	"/components/top-public-ajax.jsp",
	"/components/top-public.jsp",
	"/components/universal_component/universal_edit_dialog.jsp",
	"/components/universal_component/universal_list.jsp",
	"/components/user/logon.jsp",
	"/components/user/newuser.jsp",

	"/components/empty.jsp",

	"/components/bottom-public.jsp",
	"/components/bottom-public-ajax.jsp",
	"/components/top.jsp",
	"/components/dialog.jsp",
	"/components/content-block/template-content.jsp",
	"/components/crypto/admin_keymanagement.jsp",

	//these are blocked on server by pathFilterBlockedPaths, so must be set as safe
	"/components/grideditor/phantom/generate_image.jsp",
	"/components/grideditor/phantom/generator.jsp"

};

public static int downloadUrl(String url, String servletContextUserKey, StringBuilder data, StringBuilder statusHeader, Identity logUser)
{
	int responseStatus = -1;

	if (url.startsWith("http://") || url.startsWith("https://"))
	{
		try
		{
			url = Tools.natUrl(url);

			if (url.startsWith("https://"))
			{
				Tools.doNotVerifyCertificates();
			}

			Logger.debug(Tools.class,"DownloadUrl: " + url);

			//body obsahuje URL adresu, ktoru je treba stiahnut
			HttpURLConnection conn = null;
			URL urlObj = new URL(Tools.natUrl(url));
			conn = (HttpURLConnection)urlObj.openConnection();

			conn.setAllowUserInteraction(false);
			conn.setDoInput(true);
			conn.setDoOutput(false);
			conn.setInstanceFollowRedirects(false);

			if (logUser!=null)
			{
				Constants.getServletContext().setAttribute(Constants.USER_KEY+"_"+servletContextUserKey, logUser);
				conn.setRequestProperty("userInServletContext", servletContextUserKey);
			}
			else
			{
				Constants.getServletContext().removeAttribute(Constants.USER_KEY+"_"+servletContextUserKey);
			}

			conn.connect();
			String location = conn.getHeaderField("Location");
			String encoding = conn.getHeaderField("Content-Type");

			if (encoding==null || encoding.indexOf("charset=")==-1)
			{
				encoding = SetCharacterEncodingFilter.getEncoding();
			}
			else
			{
				encoding = encoding.substring(encoding.indexOf("charset=")+8).trim();
			}

			responseStatus = conn.getResponseCode();
			statusHeader.append(conn.getResponseMessage());

			Logger.debug(Tools.class,"---> ENCODING: " + encoding + " responseStatus="+responseStatus+" location="+location);

			BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
			InputStreamReader in = new InputStreamReader(is, encoding);
			char buffer[] = new char[8000];
			int n = 0;
			while (true)
			{
				 n = in.read(buffer);
				 if (n < 1) break;
				 data.append(buffer, 0, n);
			}
			in.close();

			if (Tools.isNotEmpty(location))
			{
				data.append("REDIRECT TO:").append(location);
			}

		}
		catch (Exception ex)
		{
			Logger.printlnError(Tools.class,"ERROR downloadUrl("+url+")");
			sk.iway.iwcm.Logger.error(ex);
		}
	}
	return(responseStatus);
}

private boolean isAdminFile(String url, String fileContent)
{
	//check admin exceptions / safe files
	for (String exc : safePages)
	{
		if (url.trim().equals(exc.trim())) return false;
		if (Tools.replace(url.trim(), "/components/"+Constants.getInstallName()+"/", "/components/").equals(exc.trim())) return false;
	}

	//unsafe code, must be explicitly allowed
	if (fileContent.contains("pageContext.include") || fileContent.contains("pageContext.forward") || fileContent.contains("request.getRequestDispatcher")) return true;

	if (url.contains("admin")) return true;

	if (fileContent.contains("admin")) return true;

	if (fileContent.contains("iwcm:checkLogon")) return true;

	//probably normal JSP component
	if (fileContent.contains("PageParams") || fileContent.contains("PageLng") || fileContent.contains("WebJETEditorBody")) return false;

	return true;
}

private int checkDir(String url, String servletContextUserKey, JspWriter out, String baseHref, Identity logUser) throws IOException
{
	IwcmFile[] files = FileTools.sortFilesByName(new IwcmFile(Tools.getRealPath(url)).listFiles());
	int counter = 0;
	for (IwcmFile f : files)
	{
//	    out.println(url+f.getName() + "<br>");

		if (f.isDirectory())
		{
			counter += checkDir(url+f.getName()+"/", servletContextUserKey, out, baseHref, logUser);
		}
		else if (f.getName().endsWith(".jsp"))
		{
			String fullUrl = url+f.getName();

			//aby nedoslo k zacykleniu
			if ("/admin/update/test_adminfilesaccess.jsp".equals(fullUrl)) continue;

			String fileContent = FileTools.readFileContent(fullUrl);

			if (isAdminFile(fullUrl, fileContent)==false) continue;


			if (fileContent.indexOf("iwcm:checkLogon")!=-1 && fileContent.indexOf("iwcm.tld")==-1)
			{
				out.println("<span style='color: red;'>"+Tools.escapeHtml(fullUrl)+" MISSING iwcm.tld ************************************************</span><br/>");
			}

			StringBuilder data = new StringBuilder();
			StringBuilder statusHeader = new StringBuilder();

			String httpAddr = baseHref+fullUrl;

			int responseStatus = downloadUrl(httpAddr, servletContextUserKey, data, statusHeader, logUser);

			String dataStr = data.toString();

			if(f.getName().equalsIgnoreCase("conf_export.jsp")) {
			    out.println("<h1>" + Tools.escapeHtml(url+f.getName()) + "</h1>");
			    out.println("<p>responseStatus=" + responseStatus + "; dataStr=" + Tools.escapeHtml(dataStr) + "</p>");
			}

			if (responseStatus == 403) continue;
			if (responseStatus == 302 && dataStr.startsWith("REDIRECT TO:") && dataStr.contains("/admin/logon.jsp")) continue;
			if (responseStatus == 302 && dataStr.startsWith("REDIRECT TO:") && dataStr.contains("/admin/logon/")) continue;
			if (responseStatus == 302 && dataStr.startsWith("REDIRECT TO:") && dataStr.contains("/admin/m/logon.jsp")) continue;
			if (responseStatus == 200 && dataStr.contains("<!-- LOGIN CONTENT Start -->")) continue;
			if (responseStatus == 200 && dataStr.contains("mobile BETA logon")) continue;
			//if (fullUrl.contains("/admin/scripts/")) continue;
			if (fullUrl.contains("/admin/help/")) continue;

			String aStyle = "";
			if (responseStatus!=302)
			{
				aStyle="font-weight: bold; color: red;";
			}

			if (responseStatus!=302)
			{
				out.println("<a style='" + aStyle + "' href='" + Tools.escapeHtml(httpAddr) + "' target='_blank'>" + Tools.escapeHtml(httpAddr) + "</a> status:" + Tools.escapeHtml(responseStatus + " " + statusHeader.toString()) + "<br/>");
				//if (dataStr.startsWith("REDIRECT TO:"))
				{
					out.println(ResponseUtils.filter(DB.prepareString(dataStr, 1000)) + "<br/>");
				}
				//out.println("	\""+fullUrl+"\",<br/>");

				counter++;
			}
		}
	}
	return counter;
}


%>
<%@ include file="/admin/layout_top.jsp" %>

<h1>Komponenta pre overenie povolenia pristupu k JSP suborom z /admin/ adresara, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>
<p>
	S parametrom ?userid=true simuluje aktualne prihlaseneho pouzivatela.
	Odporucame vytvorit admin usera, ktory nema ziadne prava (len pristup na welcome obrazovku), s nim sa prihlasit
	a zavolat /admin/update/test_adminfilesaccess.jsp?userid=true pre overenie, kam vsade ma pristup. Parametrom search
	je mozne zadat meno komponenty, napr. /admin/update/test_adminfilesaccess.jsp?userid=true&search=banner
</p>
<%
if ("fix".equals(request.getParameter("act"))) {
	Identity logUser = null;
	if ("true".equals(Tools.getRequestParameter(request, "userid")))
	{
		logUser = UsersDB.getCurrentUser(request);
	}

	//checkDir("/admin/", out, Tools.getBaseHref(request), logUser);

	String componentName = Tools.getRequestParameter(request, "search");
	if (Tools.isEmpty(componentName) || FileBrowserTools.hasForbiddenSymbol(componentName)) componentName = "form";

	String baseUrl = "/components/"+componentName+"/";
	if (componentName.startsWith("admin"))
	{
		baseUrl = "/"+componentName+"/";
	} else if ("all".equals(componentName))
	{
		baseUrl = "/components/";
	}

	out.println("<h3>Testing: "+baseUrl+"</h3>");

	String servletContextUserKey = sk.iway.Password.generateStringHash(64);

	int counter = checkDir(baseUrl, servletContextUserKey, out, Tools.getBaseHref(request), logUser);

	out.println("<h3>Found "+counter+" admin files</h3>");

	Constants.getServletContext().removeAttribute(Constants.USER_KEY+"_"+servletContextUserKey);
}
%>

<%@ include file="/admin/layout_bottom.jsp" %>
