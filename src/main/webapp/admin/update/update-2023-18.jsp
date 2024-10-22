<%@page import="java.io.IOException"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.system.datatable.OptionDto"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.system.WJResponseWrapper" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.struts.util.ResponseUtils" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>

<%!

private static List<OptionDto> replaces = new ArrayList<>();
static {
	//list
	replaces.add(new OptionDto("ArrayList ", "List ", null));
	replaces.add(new OptionDto("ArrayList\t", "List\t", null));
	replaces.add(new OptionDto("ArrayList<", "List<", null));
	replaces.add(new OptionDto("new List", "new ArrayList", null));
	//API changes
	replaces.add(new OptionDto("Constants.INSTALL_NAME", "Constants.getInstallName()", null));
	replaces.add(new OptionDto("InitServlet.SERVER_START_DATETIME", "InitServlet.getServerStartDatetime()", null));
	replaces.add(new OptionDto("UserForm.PASS_UNCHANGED", "sk.iway.iwcm.common.UserTools.PASS_UNCHANGED", null));
	replaces.add(new OptionDto("Tools.geteratePassword(", "Tools.generatePassword(", null));
	replaces.add(new OptionDto("Password.geteratePassword(", "Password.generatePassword(", null));
	//Hashtable
	replaces.add(new OptionDto("Hashtable ", "Map ", null));
	replaces.add(new OptionDto("Hashtable\t", "Map\t", null));
	replaces.add(new OptionDto("Hashtable<", "Map<", null));
	replaces.add(new OptionDto("new Map(", "new Hashtable(", null));
	replaces.add(new OptionDto("new Map<", "new Hashtable<", null));
	//HashMap
	replaces.add(new OptionDto("HashMap ", "Map ", null));
	replaces.add(new OptionDto("HashMap<", "Map<", null));
	replaces.add(new OptionDto("new Map(", "new HashMap(", null));
	replaces.add(new OptionDto("new Map<", "new HashMap<", null));
	//fix LinkedMap
	replaces.add(new OptionDto("new LinkedMap", "new LinkedHashMap", null));
	replaces.add(new OptionDto("LinkedMap<", "LinkedHashMap<", null));
	replaces.add(new OptionDto("LinkedMap ", "LinkedHashMap ", null));
	//fix Struts links
	replaces.add(new OptionDto("/admin/photogallery.do", "/admin/v9/apps/gallery/", null));

	//logon
	replaces.add(new OptionDto("<html:form action=\"/usrlogon\" focus=\"username\" name=\"logonForm\" styleClass=\"form-horizontal\" type=\"sk.iway.iwcm.LogonForm\">", "<form action=\"/usrlogon.do\" method=\"post\" name=\"logonForm\" class=\"form-horizontal\">", null));
	replaces.add(new OptionDto("<html:text property=\"username\" size=\"16\" maxlength=\"64\" styleClass=\"form-control\" styleId=\"name\" />", "<input type=\"text\" name=\"username\" size=\"16\" maxlength=\"64\" class=\"form-control\" id=\"name\" value=\"<"+"% if (request.getParameter(\"username\")!=null) out.print(org.apache.struts.util.ResponseUtils.filter(request.getParameter(\"username\"))); %"+">\" />", null));
	replaces.add(new OptionDto("<html:password property=\"password\" size=\"16\" maxlength=\"16\" redisplay=\"true\" styleClass=\"form-control\" styleId=\"pass\" />", "<input type=\"password\" name=\"password\" size=\"16\" maxlength=\"16\" class=\"form-control\" id=\"pass\" />", null));
	replaces.add(new OptionDto("<html:hidden property=\"docId\"/>", "<input type=\"hidden\" name=\"docId\" value=\"<"+"%=docId%"+">\"/>", null));
	replaces.add(new OptionDto("<html:form action=\"/usrlogon\" focus=\"username\" name=\"logonForm\" type=\"sk.iway.iwcm.LogonForm\">", "<form action=\"/usrlogon.do\" method=\"post\" name=\"logonForm\">", null));
	replaces.add(new OptionDto("<html:password property=\"password\" size=\"16\" maxlength=\"16\" redisplay=\"true\" styleClass=\"input\" styleId=\"oldpass\" />", "<input type=\"password\" name=\"password\" size=\"16\" maxlength=\"16\" class=\"input\" id=\"oldpass\" />", null));
	replaces.add(new OptionDto("<html:password property=\"newPassword\" size=\"16\" maxlength=\"16\" redisplay=\"true\" styleClass=\"input\" styleId=\"newpass\" />", "<input type=\"password\" name=\"newPassword\" size=\"16\" maxlength=\"16\" class=\"input\" id=\"newpass\" />", null));
	replaces.add(new OptionDto("<html:password property=\"retypeNewPassword\" size=\"16\" maxlength=\"16\" redisplay=\"true\" styleClass=\"input\" styleId=\"retypepass\" />", "<input type=\"password\" name=\"retypeNewPassword\" size=\"16\" maxlength=\"16\" class=\"input\" id=\"retypepass\" />", null));
	replaces.add(new OptionDto("user.setPassword(PasswordSecurity.calculateHash(newPassword, user.getSalt()));", "user.setPassword(newPassword);", null));

	//Import XLS
	replaces.add(new OptionDto("uri=\"/WEB-INF/struts-html.tld\" prefix=\"html\"", "prefix=\"form\" uri=\"http://www.springframework.org/tags/form\"", "import_xls_struct.jsp"));
	replaces.add(new OptionDto("<html:form action=\"/admin/importxls.do\" name=\"xlsImportForm\" type=\"sk.iway.iwcm.xls.ImportXLSForm\" enctype=\"multipart/form-data\">", "<form:form method=\"post\" modelAttribute=\"xlsImportForm\" action=\"/admin/import/excel/\" name=\"xlsImportForm\" enctype=\"multipart/form-data\">", null));
	replaces.add(new OptionDto("<input type=\"File\"", "<input type=\"file\"", "import_xls_struct.jsp"));
	replaces.add(new OptionDto("request.setAttribute(\"dialogDesc\", prop.getText(\"components.import_web_pages.xls.dialogDesc\"));", "request.setAttribute(\"dialogDesc\", prop.getText(\"components.import_web_pages.xls.dialogDesc\"));\nrequest.setAttribute(\"xlsImportForm\", new sk.iway.iwcm.xls.ImportXLSForm());", "import_xls_struct.jsp"));
	replaces.add(new OptionDto("</form>", "</form:form>", "import_xls_struct.jsp"));
	replaces.add(new OptionDto("/admin/importxls.do", "/admin/import/excel/", null));

	//forum/new.jsp
	replaces.add(
		new OptionDto(
			"<html:form name=\"forumForm\" scope=\"request\" method=\"post\" action=\"/saveforum.do\" type=\"sk.iway.iwcm.forum.ForumForm\">",
			"<form:form method=\"post\" modelAttribute=\"forumForm\" action=\"/apps/forum/saveforum\" name=\"forumForm\">",  "new.jsp")
	);
	replaces.add(new OptionDto("<label><iwcm:text key=\"forum.new.name\"/>:</label>", "<form:label path=\"authorName\"><iwcm:text key=\"forum.new.name\"/>:</form:label>", "new.jsp"));
	replaces.add(
		new OptionDto(
			"<html:text property=\"authorFullName\" styleClass=\"required form-control\" size=\"40\" maxlength=\"255\" onfocus=\"<"+"%=onfocus %"+">\"/>",
			"<form:input id=\"authorName\" path=\"authorName\" cssClass=\"required form-control\" size=\"40\" maxlength=\"255\" required=\"required\" disabled=\"true\"/>", "new.jsp")
	);
	replaces.add(new OptionDto("<label><iwcm:text key=\"forum.new.email\"/>:</label>", "<form:label path=\"authorEmail\"><iwcm:text key=\"forum.new.email\"/>:</form:label>", "new.jsp"));
	replaces.add(
		new OptionDto(
			"<html:text property=\"authorEmail\" styleClass=\"email form-control\" size=\"40\" maxlength=\"255\" onfocus=\"<"+"%=onfocus %"+">\"/>",
			"<form:input id=\"authorEmail\" path=\"authorEmail\" cssClass=\"email form-control\" size=\"40\" maxlength=\"255\" disabled=\"true\"/>", "new.jsp")
	);
	replaces.add(new OptionDto("<label><iwcm:text key=\"forum.new.subject\"/>:</label>", "<form:label path=\"subject\"><iwcm:text key=\"forum.new.subject\"/>:</form:label>", "new.jsp"));
	replaces.add(new OptionDto("<html:text property=\"subject\" styleClass=\"required form-control\" size=\"40\" maxlength=\"255\"/>", "<form:input path=\"subject\" cssClass=\"required form-control\" size=\"40\" maxlength=\"255\"/>", "new.jsp"));
	replaces.add(new OptionDto("<html:checkbox property=\"sendNotif\"/> <iwcm:text key=\"components.forum.send_answer_notif\"/>", "<form:checkbox path=\"sendAnswerNotif\"/> <iwcm:text key=\"components.forum.send_answer_notif\"/>", "new.jsp"));
	replaces.add(new OptionDto("<html:hidden property=\"parentId\"/>", "<form:hidden path=\"parentId\"/>", "new.jsp"));
	replaces.add(new OptionDto("<html:hidden property=\"forumId\"/>", "<form:hidden path=\"id\"/>", "new.jsp"));
	replaces.add(new OptionDto("</form>", "</form:form>", "new.jsp"));
	replaces.add(
		new OptionDto(
			"<html:textarea property=\"question\" styleClass=\"input required wysiwyg form-control\" styleId=\"wysiwygForum\" rows=\"15\" cols=\"35\"/></td>",
			"<form:textarea path=\"question\" cssClass=\"input required wysiwyg form-control\" id=\"wysiwygForum\" rows=\"15\" cols=\"35\" />", "new.jsp")
	);
	replaces.add(new OptionDto("<"+"%@ taglib uri=\"/WEB-INF/struts-html.tld\" prefix=\"html\" %"+">", "<"+"%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\"%"+">", "new.jsp"));
	replaces.add(new OptionDto("ForumForm forumForm = new ForumForm();", "DocForumEntity forumForm = new DocForumEntity();", "new.jsp"));
	replaces.add(new OptionDto("request.setAttribute(\"forumForm\", forumForm);", "request.setAttribute(\"forumForm\", forumForm);", "new.jsp"));
	replaces.add(new OptionDto(".setForumId(forumId);", ".setId((long) forumId);", "new.jsp"));
	replaces.add(new OptionDto(".setAuthorFullName", ".setAuthorName", "new.jsp"));
	replaces.add(new OptionDto(".setSendNotif", ".setSendAnswerNotif", "new.jsp"));
	replaces.add(
		new OptionDto(
			"<html:textarea property=\"question\" styleClass=\"input required wysiwyg\" styleId=\"wysiwygForum\" rows=\"15\" cols=\"35\"/>",
			"<form:textarea path=\"question\" cssClass=\"input required wysiwyg form-control\" id=\"wysiwygForum\" rows=\"15\" cols=\"35\" />", "new.jsp")
	);
	replaces.add(
		new OptionDto(
			"<label class=\"form-check-label\" for=\"sentNotif\"><iwcm:text key=\"components.forum.send_answer_notif\"/>:</label>",
			"<form:label path=\"sendAnswerNotif\"><iwcm:text key=\"components.forum.send_answer_notif\"/>:</form:label>", "new.jsp")
	);
	replaces.add(
		new OptionDto(
			"<html:checkbox property=\"sendNotif\" styleClass=\"form-check-input\" styleId=\"sentNotif\"/>",
			"<form:checkbox path=\"sendAnswerNotif\" styleClass=\"form-check-input\" styleId=\"sentNotif\"/>", "new.jsp")
	);

	//forum/new_file.jsp
	replaces.add(new OptionDto("System.out.println(docId);", "", "new_file.jsp"));
	replaces.add(new OptionDto("<" + "%@ taglib uri=\"/WEB-INF/struts-bean.tld\" prefix=\"bean\" %" + ">", "<" + "%@ taglib prefix=\"form\" uri=\"http://www.springframework.org/tags/form\"%" + ">", "new_file.jsp"));

	replaces.add(
		new OptionDto(
			"<script type=\"text/javascript\" src=\"<iwcm:cp/>/components/calendar/popcalendar.jsp\"></script>",
			"", "new_file.jsp")
	);
	replaces.add(
		new OptionDto(
			"<html:form scope=\"request\" method=\"post\" enctype=\"multipart/form-data\" action=\"/saveforum.do\" type=\"sk.iway.iwcm.forum.ForumForm\">",
			"<form:form method=\"post\" modelAttribute=\"forumForm\" action=\"/apps/forum/saveForumFile\" name=\"forumForm\" enctype=\"multipart/form-data\">", "new_file.jsp")
	);
	replaces.add(
		new OptionDto(
			"<input type=\"hidden\" name=\"parentId\" value=\"<" + "%=ResponseUtils.filter(request.getParameter(\"parent\"))%" + ">\" />",
			"<input type=\"hidden\" name=\"forumId\" value=\"<" + "%=ResponseUtils.filter(request.getParameter(\"forumId\"))%" + ">\" />", "new_file.jsp")
	);
	replaces.add(new OptionDto("<html:file property=\"uploadedFile\" size=\"30\"/>", "<input type=\"file\" name=\"uploadedFile\" size=\"30\"/>", "new_file.jsp"));
	replaces.add(new OptionDto("<input type=\"hidden\" name=\"parentId\" value=\"<" + "%=ResponseUtils.filter(request.getParameter(\"parent\"))%" + ">\" />", "", "new_file.jsp"));
	replaces.add(new OptionDto("<html:hidden property=\"forumId\"/>", "<input type=\"hidden\" name=\"type\" value=\"upload\" />", "new_file.jsp"));
	replaces.add(new OptionDto("</form>", "</form:form>", "new_file.jsp"));
	replaces.add(new OptionDto("<input type=\"hidden\" name=\"docid\" value=\"<" + "%=ResponseUtils.filter(request.getParameter(\"docid\"))%" + ">\" />", "", "new_file.jsp"));
	replaces.add(new OptionDto("<input type=\"hidden\" name=\"docId\" value=\"<" + "%=ResponseUtils.filter(request.getParameter(\"docid\"))%" + ">\" />", "", "new_file.jsp"));

	//forum/forum_mb.jsp
	replaces.add(new OptionDto("boolean rootGroup = false;", "boolean rootGroup = pageParams.getBooleanValue(\"rootGroup\", false);", "forum_mb.jsp"));
	replaces.add(new OptionDto("if(!pageParams.getValue(\"rootGroup\", \"\").equals(\"\") || \"true\".equals(request.getParameter(\"rootGroup\")))", "if(\"true\".equals(request.getParameter(\"rootGroup\")) || rootGroup==true)", "forum_mb.jsp"));

	//forum/forum_mb_open.jsp
	replaces.add(new OptionDto("function popupNewUpload(parent, docId)", "function popupNewUpload(forumId, docId)", "forum_mb_open.jsp"));
	replaces.add(new OptionDto("editwindow=window.open(\"/components/forum/new_file.jsp?parent=\"+parent+\"&docid=\"+docId,'forumNew',options);", "editwindow=window.open(\"/components/forum/new_file.jsp?forumId=\"+forumId+\"&docid=\"+docId,'forumNew',options);", "forum_mb_open.jsp"));
	replaces.add(
		new OptionDto(
			"<a class=\"btn btn-default\" href=\"javascript:popupNewUpload(<bean:write name=\"field\" property=\"forumId\"/>, <" + "%=docId%" + ">);\">",
			"<a class=\"btn btn-default\" href=\"javascript:popupNewUpload(<bean:write name=\"field\" property=\"forumId\"/>, <" + "%=docId%" + ">, <" + "%=parentId%" + ">);\">", "forum_mb_open.jsp")
		);
	replaces.add(new OptionDto("Map emoticons = new Hashtable();", "Map<String, String> emoticons = new Hashtable<>();", "forum_mb_open.jsp"));

	//forum/saveok.jsp
	replaces.add(new OptionDto("param1=\"<" + "%=Constants.getInt(SpamProtection.HOURLY_LIMIT_KEY)+\"\"%" + ">\"", "param1='<" + "%=Constants.getInt(SpamProtection.HOURLY_LIMIT_KEY)+\"\"%" + ">'", "saveok.jsp"));
	replaces.add(new OptionDto("param2=\"<" + "%=Constants.getInt(SpamProtection.TIMEOUT_KEY)+\"\"%" + ">\"", "param2='<" + "%=Constants.getInt(SpamProtection.TIMEOUT_KEY)+\"\"%" + ">'", "saveok.jsp"));
	replaces.add(new OptionDto("\"<" + "%=(String)request.getAttribute(\"errorKey\")%" + ">\"", "'<" + "%=(String)request.getAttribute(\"errorKey\")%" + ">'", "saveok.jsp"));

	//All forum files
	replaces.add(new OptionDto("sk.iway.iwcm.forum.ForumBean", "sk.iway.iwcm.components.forum.jpa.DocForumEntity", ""));
	replaces.add(new OptionDto("sk.iway.iwcm.forum.ForumGroupBean", "sk.iway.iwcm.components.forum.jpa.ForumGroupEntity", ""));
	replaces.add(new OptionDto("<" + "%@ page import=\"sk.iway.iwcm.forum.ForumBean\" %" + ">", "<" + "%@ page import=\"sk.iway.iwcm.components.forum.jpa.DocForumEntity\" %" + ">", ""));
	replaces.add(new OptionDto("type=\"sk.iway.iwcm.forum.ForumBean\"", "type=\"DocForumEntity\"", ""));

	replaces.add(new OptionDto("ForumGroupBean", "ForumGroupEntity", ""));
	replaces.add(new OptionDto(" ForumBean", " DocForumEntity", ""));
	replaces.add(new OptionDto("<ForumBean", "<DocForumEntity", ""));
	replaces.add(new OptionDto("(ForumBean", "(DocForumEntity", ""));
	replaces.add(new OptionDto("\tForumBean", "\tDocForumEntity", ""));
	replaces.add(new OptionDto("\nForumBean", "\nDocForumEntity", ""));
	replaces.add(new OptionDto("property=\"autorFullName\"", "property=\"authorName\"", ""));
	replaces.add(new OptionDto("property=\"autorEmail\"", "property=\"authorEmail\"", ""));
	replaces.add(new OptionDto(".setAutorFullName(", ".setAuthorName(", ""));
	replaces.add(new OptionDto(".setAutorEmail(", ".setAuthorEmail(", ""));
	replaces.add(new OptionDto(".getAutorFullName(", ".getAuthorName(", ""));
	replaces.add(new OptionDto(".getAutorEmail(", ".getAuthorEmail(", ""));
	replaces.add(new OptionDto("<" + "%@page import=\"sk.iway.iwcm.forum.ForumBean\"%" + ">", "", ""));

	//forum/forum_mb_open.jsp
	replaces.add(new OptionDto("Map<String, String> emoticons = new Hashtable<>();", "Hashtable<String, String> emoticons = new Hashtable<>();", null));

	//HtmlEncoder
	replaces.add(new OptionDto("com.lowagie.text.html.HtmlEncoder", "org.apache.struts.util.ResponseUtils", null));
	replaces.add(new OptionDto("HtmlEncoder.encode(", "ResponseUtils.filter(", null));

	//Some  JSP using ForumSearchBEan still use name "autorFullName" so it will crash (because we rewrited it to "authorName") -> change it back
	replaces.add(new OptionDto("property=\"authorName\"", "property=\"autorFullName\"", "forum_mb_search_user_posts.jsp"));
	replaces.add(new OptionDto("property=\"authorName\"", "property=\"autorFullName\"", "forum_mb_search.jsp"));

	//thymeleaf 3.1 unsupported objects - https://www.thymeleaf.org/doc/articles/thymeleaf31whatsnew.html
	replaces.add(new OptionDto("$"+"{#request.", "$"+"{request.", ".html"));
	replaces.add(new OptionDto("$"+"{#session.", "$"+"{session.", ".html"));

	//packages change
	replaces.add(new OptionDto("sk.iway.iwcm.components.monitoring.MonitoringManager", "sk.iway.iwcm.components.monitoring.rest.MonitoringManager", null));

	//export dat in custom 404 file
	replaces.add(new OptionDto("if(\"xml\".equals(format)){", "if(\"xml\".equals(format)||\"rss\".equals(format)){", ".jsp"));

	//rating
	replaces.add(new OptionDto("rating.RatingDB", "rating.RatingService", null));
	replaces.add(new OptionDto("rating.RatingBean", "rating.jpa.RatingEntity", null));
	replaces.add(new OptionDto("RatingDB.", "RatingService.", null));
	replaces.add(new OptionDto("RatingBean", "RatingEntity", null));

	//Menu
	replaces.add(new OptionDto("<form ", "<form:form ", "/components/restaurant_menu/"));
	replaces.add(new OptionDto("<" + "%@page import=\"sk.iway.iwcm.components.restaurant_menu.MealDB\"%" + ">", "", "/components/restaurant_menu/"));
	replaces.add(new OptionDto("sk.iway.iwcm.components.restaurant_menu.MenuDB", "sk.iway.iwcm.components.restaurant_menu.rest.RestaurantMenuService", "/components/restaurant_menu/"));
	replaces.add(new OptionDto("<" + "%@page import=\"sk.iway.iwcm.components.restaurant_menu.MealBean\"%" + ">", "", "/components/restaurant_menu/"));
	replaces.add(new OptionDto("sk.iway.iwcm.components.restaurant_menu.MenuBean", "sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuEntity", "/components/restaurant_menu/"));
	replaces.add(new OptionDto("MenuBean", "RestaurantMenuEntity", "/components/restaurant_menu/"));
	replaces.add(new OptionDto("MenuDB.getInstance().getByDate(day)", "RestaurantMenuService.getByDate(day,prop)", "/components/restaurant_menu/"));
	replaces.add(new OptionDto("MenuDB", "RestaurantMenuService", "/components/restaurant_menu/"));

	//Quiz
	replaces.add(
		new OptionDto("<" + "%@page import=\"sk.iway.iwcm.components.quiz.QuizBean\"%" + ">",
		"<" + "%@page import=\"sk.iway.iwcm.components.quiz.QuizBean\"%" + "> \n <" + "%@page import=\"sk.iway.iwcm.components.quiz.jpa.QuizType\"%" + "> \n <" + "%@page import=\"sk.iway.iwcm.components.quiz.jpa.QuizResultEntity\"%" + "> \n <" + "%@page import=\"sk.iway.iwcm.components.quiz.jpa.QuizAnswerEntity\"%" + ">",
		"/components/quiz/")
	);
	replaces.add(new OptionDto("sk.iway.iwcm.components.quiz.QuizQuestionBean", "sk.iway.iwcm.components.quiz.jpa.QuizQuestionEntity", "/components/quiz/"));
	replaces.add(new OptionDto("sk.iway.iwcm.components.quiz.QuizBean", "sk.iway.iwcm.components.quiz.jpa.QuizEntity", "/components/quiz/"));
	replaces.add(new OptionDto("sk.iway.iwcm.components.quiz.QuizService", "sk.iway.iwcm.components.quiz.rest.QuizService", "/components/quiz/"));
	replaces.add(new OptionDto("QuizBean", "QuizEntity", "/components/quiz/"));
	replaces.add(new OptionDto("QuizQuestionBean", "QuizQuestionEntity", "/components/quiz/"));
	replaces.add(new OptionDto(".getImage()", ".getImageUrl()", "/components/quiz/"));
	replaces.add(new OptionDto("if(option != null)", "if(option != null && !(\"\".equals(option)))", "/components/quiz/"));
	replaces.add(new OptionDto("", "", "/components/quiz/"));


	//old v8 datatables
	replaces.add(new OptionDto("pagingType: \"bootstrap_extended\"", "pagingType: datatables_globalConfig.pagingType", null));

	//iwcm:forEach
	replaces.add(new OptionDto("<iwcm:forEach items=\"$"+"{galleryActionBean.photoList}\" var=\"image\" type=\"sk.iway.iwcm.gallery.GalleryBean\">", "<c:forEach items=\"$"+"{galleryActionBean.photoList}\" var=\"image\"><"+"%sk.iway.iwcm.gallery.GalleryBean image = (sk.iway.iwcm.gallery.GalleryBean)pageContext.getAttribute(\"image\");%"+">", null));
	replaces.add(new OptionDto("<iwcm:forEach items=\"$"+"{perexGroupsNot}\" var=\"option\" type=\"sk.iway.iwcm.doc.PerexGroupBean\">", "<c:forEach items=\"$"+"{perexGroupsNot}\" var=\"option\"><"+"%sk.iway.iwcm.doc.PerexGroupBean option = (sk.iway.iwcm.doc.PerexGroupBean)pageContext.getAttribute(\"option\");%"+">", null));
	replaces.add(new OptionDto("</iwcm:forEach>", "</c:forEach>", null));

	//remove datetime.tld
	replaces.add(new OptionDto("<"+"%@ taglib uri=\"/WEB-INF/datetime.tld\" prefix=\"dt\" %"+">", "", null));
	replaces.add(new OptionDto("<"+"%@ taglib uri=\"/WEB-INF/datetime.tld\" prefix=\"dt\"", "<"+"%", null));

	//font-awesome
	replaces.add(new OptionDto("fa fa-angle-right", "ti ti-chevron-right", null));
	replaces.add(new OptionDto("fa fa-chevron-left", "ti ti-chevron-left", null));
	replaces.add(new OptionDto("fa fa-chevron-right", "ti ti-chevron-right", null));
	replaces.add(new OptionDto("fa fa-cog", "ti ti-settings", null));
	replaces.add(new OptionDto("far fa-plus", "ti ti-plus", null));
	replaces.add(new OptionDto("fa fa-plus", "ti ti-plus", null));
	replaces.add(new OptionDto("fa fa-times", "ti ti-x", null));
	replaces.add(new OptionDto("far fa-chart-simple", "ti ti-chart-line", null));
	replaces.add(new OptionDto("far fa-chart-line", "ti ti-chart-line", null));
	replaces.add(new OptionDto("far fa-chart-bar", "ti ti-chart-line", null));
	replaces.add(new OptionDto("fas fa-circle-check", "ti ti-circle-check", null));
	replaces.add(new OptionDto("fas fa-circle-xmark", "ti ti-circle-x", null));
	replaces.add(new OptionDto("fas fa-thumbs-up", "ti ti-thumb-up", null));
	replaces.add(new OptionDto("fas fa-thumbs-down", "ti ti-thumb-down", null));
	replaces.add(new OptionDto("far fa-eye", "ti ti-eye", null));
	replaces.add(new OptionDto("fas fa-toggle-on", "ti ti-toggle-right", null));
	replaces.add(new OptionDto("fas fa-toggle-off", "ti ti-toggle-left", null));
	replaces.add(new OptionDto("fas fa-repeat", "ti ti-repeat", null));
	replaces.add(new OptionDto("fas fa-trash-can-undo", "ti ti-trash-off", null));
	replaces.add(new OptionDto("fal fa-dumpster", "ti ti-trash-x", null));
	replaces.add(new OptionDto("far fa-trash-alt", "ti ti-trash", null));
	replaces.add(new OptionDto("fas fa-check", "ti ti-check", null));
	replaces.add(new OptionDto("fas fa-xmark", "ti ti-x", null));
	replaces.add(new OptionDto("fas fa-rotate-right", "ti ti-restore", null));
	replaces.add(new OptionDto("fas fa-rotate-left", "ti ti-rotate-left", null));
	replaces.add(new OptionDto("fa-duotone fa-rotate", "ti ti-refresh", null));
	replaces.add(new OptionDto("far fa-search", "ti ti-search", null));
	replaces.add(new OptionDto("far fa-crosshairs", "ti ti-focus-2", null));
	replaces.add(new OptionDto("fa fa-exclamation-triangle", "ti ti-alert-triangle", null));
	replaces.add(new OptionDto("fa fa-image", "ti ti-photo", null));
	replaces.add(new OptionDto("fa fa-file", "ti ti-file", null));
	replaces.add(new OptionDto("fa fa-file-alt", "ti ti-file", null));
	replaces.add(new OptionDto("fa fa-file-text", "ti ti-file", null));
	replaces.add(new OptionDto("fa fa-link", "ti ti-focus-2", null));
	replaces.add(new OptionDto("fa fa-calendar", "ti ti-calendar", null));
	replaces.add(new OptionDto("fa fa-clock", "ti ti-clock", null));
	replaces.add(new OptionDto("fa fa-clock-o", "ti ti-clock", null));
	replaces.add(new OptionDto("fas fa-angle-right", "ti ti-chevron-right", null));
	replaces.add(new OptionDto("fas fa-angle-left", "ti ti-chevron-left", null));
	replaces.add(new OptionDto("fab fa-facebook-f", "ti ti-brand-facebook", null));
	replaces.add(new OptionDto("fab fa-twitter", "ti ti-brand-twitter", null));
	replaces.add(new OptionDto("fab fa-linkedin-in", "ti ti-brand-linkedin", null));
	replaces.add(new OptionDto("far fa-envelope", "ti ti-envelope", null));
	replaces.add(new OptionDto("fas fa-th-large", "ti ti-layout-grid", null));
	replaces.add(new OptionDto("fas fa-list", "ti ti-list", null));
	replaces.add(new OptionDto("fas fa-shopping-basket", "ti ti-basket", null));
	replaces.add(new OptionDto("fas fa-thumbs-up", "ti ti-thumbs-up", null));
	replaces.add(new OptionDto("far fa-pencil", "ti ti-pencil", null));
	replaces.add(new OptionDto("far fa-wrench", "ti ti-adjustments-horizontal", null));
	replaces.add(new OptionDto("far fa-info-circle", "ti ti-info-circle", null));
	replaces.add(new OptionDto("far fa-question-circle", "ti ti-help", null));
	replaces.add(new OptionDto("far fa-browser", "ti ti-browser", null));
	replaces.add(new OptionDto("far fa-comment-alt", "ti ti-message", null));
	replaces.add(new OptionDto("far fa-bell", "ti ti-bell", null));
	replaces.add(new OptionDto("far fa-address-card", "ti ti-id", null));
	replaces.add(new OptionDto("far fa-user", "ti ti-user", null));
	replaces.add(new OptionDto("far fa-phone", "ti ti-device-mobile-message", null));
	replaces.add(new OptionDto("far fa-key", "ti ti-key", null));
	replaces.add(new OptionDto("far fa-sign-out", "ti ti-logout", null));
	replaces.add(new OptionDto("fas fa-folder-times", "ti ti-folder-x", null));
	replaces.add(new OptionDto("fas fa-folder", "ti ti-folder-filled", null));
	replaces.add(new OptionDto("far fa-folder", "ti ti-folder", null));
	replaces.add(new OptionDto("far fa-check-circle", "ti ti-circle-check", null));
	replaces.add(new OptionDto("fas fa-lock", "ti ti-lock-filled", null));
	replaces.add(new OptionDto("fas fa-home", "ti ti-home", null));

	replaces.add(new OptionDto("fa fa-snowflake", "ti ti-snowflake", null));
	replaces.add(new OptionDto("fas fa-external-link-alt", "ti ti-external-link", null));
	replaces.add(new OptionDto("fas fa-globe", "ti ti-map-pin", null));
	replaces.add(new OptionDto("far fa-globe", "ti ti-map-pin-off", null));
	replaces.add(new OptionDto("fas fa-eye-slash", "ti ti-eye-off", null));
	replaces.add(new OptionDto("fas fa-chevron-down", "ti ti-chevron-down", null));
	replaces.add(new OptionDto("fas fa-restroom", "ti ti-a-b", null));
	replaces.add(new OptionDto("far fa-link-slash", "", null));
	replaces.add(new OptionDto("fas fa-star", "ti ti-star", null));
	replaces.add(new OptionDto("fas fa-map-marker-alt", "ti ti-map-pin", null));
	replaces.add(new OptionDto("far fa-map-marker-alt-slash", "ti ti-map-pin-off", null));
	replaces.add(new OptionDto("far fa-th-list", "ti ti-list-details", null));
	replaces.add(new OptionDto("far fa-image", "ti ti-photo", null));
	replaces.add(new OptionDto("far fa-retweet", "ti ti-repeat", null));
	replaces.add(new OptionDto("far fa-camera", "ti ti-camera", null));
	
	replaces.add(new OptionDto("org.apache.commons.dbcp.ConfigurableDataSource", "sk.iway.iwcm.system.dbpool.ConfigurableDataSource", null));
}

private void checkDir(String url, boolean saveFile, boolean compileFile, JspWriter out, HttpServletRequest request, HttpServletResponse response) throws IOException
{
	String realPath = Tools.getRealPath(url);
	IwcmFile rootDir = new IwcmFile(realPath);
	IwcmFile[] files;
	if (rootDir.isDirectory()) {
		files = FileTools.sortFilesByName(rootDir.listFiles());
	} else {
		files = new IwcmFile[1];
		url = url.substring(0, url.lastIndexOf("/"));
		if (Tools.isEmpty(url)) url = "/";
		files[0] = rootDir;
	}
	for (IwcmFile f : files)
	{
		if (f.isDirectory())
		{
			if ("node_modules".equals(f.getName()) || "dist".equals(f.getName())) return;
			checkDir(url+f.getName()+"/", saveFile, compileFile, out, request, response);
		}
		else if (f.getName().endsWith(".jsp") || f.getName().endsWith(".html"))
		{
			String fullUrl = url+f.getName();

			//sam sebe sam, rekurzia pri kompilacii
			if ("/admin/update/update-2023-18.jsp".equals(fullUrl)) continue;
			if ("/admin/update/ldap-conn-test.jsp".equals(fullUrl)) continue;

			System.out.println(fullUrl);
			out.println(Tools.escapeHtml(fullUrl));
			out.flush();

			String encoding = "utf-8";
			String content = FileTools.readFileContent(fullUrl, encoding);
			String contentOriginal = content;
			if (content.contains("pageEncoding=\"windows-1250") || content.contains("pageEncoding=\"cp-1250")) {
				out.println("   Varovanie: otváram súbor v kódovaní windows-1250");
				encoding = "windows-1250";
				content = FileTools.readFileContent(fullUrl, encoding);
			}

			StringBuilder foundContent = new StringBuilder();
			boolean hasChange = false;
			//prejdi zaznamy v hashtabulke a nahrad
			for (OptionDto entry : replaces) {
				String from = entry.getLabel();
				String to = entry.getValue();
				Object original = entry.getOriginal();
				if (original != null && original instanceof String) {
					String ext = (String)original;
					if (Tools.isNotEmpty(ext)) {
						if (ext.startsWith(".")) {
							//it's file extension
							if (fullUrl.endsWith(ext)==false) continue;
						} else if(ext.startsWith("/")) {
							//it's folder
							if (fullUrl.startsWith(ext)==false) continue;
						} else {
							//it's file name, it must end with it
							if (fullUrl.endsWith(ext)==false) continue;
						}
					}
				}
				//original null je mozne pouzit len na jsp subory
				if (original == null && f.getName().endsWith(".jsp")==false) continue;

				//FA icons replace only for admin files
				if (to.startsWith("ti ti-")) {
					boolean isAdmin = false;
					if (fullUrl.contains("admin")) isAdmin = true;
					else if (fullUrl.contains("editor_component")) isAdmin = true;
					else if (content.contains("layout_top.jsp")) isAdmin = true;
					else if (content.contains("<"+"iwcm:checkLogon")) isAdmin = true;

					if (isAdmin == false) continue;
				}

				if (content.contains(from)) {
					content = Tools.replace(content, from, to);
					foundContent.append("   Našiel som: ").append(Tools.escapeHtml(from)).append("\n");
					hasChange = true;

					//fix struts form close tag
					if (from.contains("<html:form")) {
						if (to.contains("<form:form")) {
							content = Tools.replace(content, "</html:form>", "</form:form>");
							content = Tools.replace(content, "uri=\"/WEB-INF/struts-html.tld\" prefix=\"html\"", "prefix=\"form\" uri=\"http://www.springframework.org/tags/form\"");
							content = Tools.replace(content, "prefix=\"html\" uri=\"/WEB-INF/struts-html.tld\"", "prefix=\"form\" uri=\"http://www.springframework.org/tags/form\"");
						} else content = Tools.replace(content, "</html:form>", "</form>");

						//fix FILE input type for upload
						content = Tools.replace(content, "<html:file property=\"file\" styleClass=\"input\"", "<input type=\"file\" name=\"file\" class=\"input\"");
						content = Tools.replace(content, "<html:file property=\"file\"", "<input type=\"file\" name=\"file\"");
					}
				}
			}

			{
				String origString = "sk.iway.iwcm.forum.*";
				String bonusString = ",sk.iway.iwcm.components.forum.jpa.*";
				if(content.contains(origString + bonusString)) {
					//was allready added
				} else {
					content = Tools.replace(content, origString, origString + bonusString);
				}
			}

			if("new_file.jsp".equals(f.getName()) && url.contains("forum")) {
				String origString = "LabelValueDetails uploadLimits = ForumDB.getUploadLimits(docId, request);";
				String bonusString = "request.setAttribute(\"forumForm\", new DocForumEntity());";
				if(content.contains(origString + bonusString)) {
					//was allready added
				} else {
					content = Tools.replace(content, origString, origString + bonusString);
				}
			}

			if (hasChange && content.equals(contentOriginal)==false) {
				//FIX import
				if (content.contains("List ") || content.contains("List\t") || content.contains("List<")) {
					if (content.contains("java.util.List")==false) {
						//pridaj import
						content = "<"+"%@page import=\"java.util.List\"%"+">"+content;
					}
				}
				if (content.contains("Map ") || content.contains("Map\t") || content.contains("Map<")) {
					if (content.contains("java.util.Map")==false) {
						//pridaj import
						content = "<"+"%@page import=\"java.util.Map\"%"+">"+content;
					}
				}

				out.print(foundContent.toString());

				if (saveFile) {
					FileTools.saveFileContent(fullUrl, content, encoding);
					out.println("   <strong>Ukladám súbor</strong>: "+fullUrl);
				} else {
					out.println("   <strong>Súbor obsahuje zmenu, uloženie ale nie je aktivované</strong>");
				}
			}

			if (compileFile && f.getName().endsWith(".jsp")) {
				//dlho trvajuce/zacyklenie kompilacie
				if ("test_jspcompilation.jsp".equals(f.getName())) continue;
				if (f.getVirtualPath().startsWith("/admin/update/")) continue;
				if ("keep_alive_test.jsp".equals(f.getName())) continue;
				//includes
				if ("layout_sidebar.jsp".equals(f.getName())) continue;

				try
				{
					WJResponseWrapper respWrapper = new WJResponseWrapper(response, request);
					request.getRequestDispatcher(fullUrl).include(request, respWrapper);
				}
				catch (Exception ex)
				{
					StringWriter sw = new StringWriter();
					ex.printStackTrace(new PrintWriter(sw));

					String stack = sw.toString();

					if (stack.contains("Unable to compile class for JSP") || stack.contains(") JSP file ["))
					{
						//skompilovalo ale nevedelo najst objekt
						if (stack.contains("cannot be resolved to a variable")) {
							if (stack.contains("Syntax error") || stack.contains("Type mismatch") || stack.contains("is not visible")) {
								//obsahuje aj inu chybu, musime zobrazit
							} else {
								out.println("&nbsp;&nbsp;&nbsp;Neviem nájsť objekt, pravdepodobne sa jedná o include JSP, skontrolujte manuálne, že sú zobrazené len chyby cannot be resolved to a variable.<br/>");
								continue;
							}
						}

						int i = stack.indexOf("Stacktrace:");
						if (i > 20) stack = stack.substring(0, i);

						out.println("CHYBA:<br/>");
						out.println(ResponseUtils.filter(stack));
					}
				}
			}
		}
	}
}


%>

<div class="row title">
	<h1 class="page-title">Upraviť kód v JSP súboroch pre WebJET 2023.18</h1>
</div>

<%
	String subdir = Constants.getInstallName();
	if (Tools.getRequestParameter(request, "subdir")!=null) subdir = Tools.getRequestParameter(request, "subdir");
%>

<form action="update-2023-18.jsp" method="post">
	<div class="container" style="margin-top: 16px">
		<div class="row">
			<div class="col-4">Meno priečinka:</div>
			<div class="col-8">
				<div class="input-group input-group-sm">
					<div class="input-group-prepend">
						<span class="input-group-text" id="basic-addon3">/components/
					</div>
					<input type="text" name="subdir" class="form-control" value="<%=ResponseUtils.filter(subdir)%>"/>
				</div>
			</div>
		</div>
		<div class="row">
			<div class="col-4"></div>
			<div class="col-8">
				Zadajte prázdnu hodnotu alebo znak * pre kontrolu všetkých priečinkov
				<br/>
				Zadajte hodnotu templates pre kontrolu priečinka /templates/ a hodnotu admin pre kontrolu priečinka /admin/.
			</div>
		</div>
		<div class="row">
			<div class="col-4">Nastavenia:</div>
			<div class="col-8">
				<div class="form-check">
					<input class="form-check-input toggle" type="checkbox" value="true" name="saveFile" id="saveFile">
					<label class="form-check-label" for="saveFile">
						Uložiť zmenu v súbore
					</label>
				</div>
			</div>
		</div>
		<div class="row">
			<div class="col-4"></div>
			<div class="col-8">
				<div class="form-check">
					<input class="form-check-input toggle" type="checkbox" value="true" name="compileFile" id="compileFile">
					<label class="form-check-label" for="compileFile">
						Vykonať kontrolu kompilácie - technicky sa spustí/vykoná JSP súbor, verím, že tam nemáte nič čo by len spustením JSP vykonalo niečo zlé (zmazalo dáta)
					</label>
				</div>
			</div>
		</div>
		<div class="row">
			<div class="col-12"><input type="submit" class="btn btn-primary" value="Spustiť"/></div>
		</div>
	<div>
</form>

<% if (Tools.getRequestParameter(request, "subdir")!=null) {
	boolean saveFile = "true".equals(request.getParameter("saveFile"));
	boolean compileFile = "true".equals(request.getParameter("compileFile"));
%>

	<div style="white-space: pre"><%
		if (Tools.isEmpty(subdir) || "*".equals(subdir)) {
			checkDir("/admin/", saveFile, compileFile, out, request, response);
			checkDir("/components/", saveFile, compileFile, out, request, response);
			checkDir("/apps/", saveFile, compileFile, out, request, response);
			checkDir("/templates/", saveFile, compileFile, out, request, response);
		}
		else if ("admin".equals(subdir) || "*".equals(subdir)) {
			checkDir("/admin/", saveFile, compileFile, out, request, response);
		}
		else if ("templates".equals(subdir) || "*".equals(subdir)) {
			checkDir("/templates/", saveFile, compileFile, out, request, response);
		}
		else {
			checkDir("/components/" + subdir + "/", saveFile, compileFile, out, request, response);
			checkDir("/apps/" + subdir + "/", saveFile, compileFile, out, request, response);
		}
		checkDir("/403.jsp", saveFile, compileFile, out, request, response);
		checkDir("/404.jsp", saveFile, compileFile, out, request, response);
		checkDir("/500.jsp", saveFile, compileFile, out, request, response);
		%>
	</div>
	<p>
		Hotovo
	</p>
<% } %>


<%@ include file="/admin/layout_bottom.jsp" %>