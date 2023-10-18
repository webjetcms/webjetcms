<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.common.DocTools"%>
<%@page import="sk.iway.iwcm.editor.appstore.AppBean"%>
<%@page import="sk.iway.iwcm.editor.appstore.AppManager"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.system.*,sk.iway.iwcm.users.*,sk.iway.iwcm.*,java.util.*,java.io.*,sk.iway.iwcm.i18n.Prop"
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><iwcm:checkLogon admin="true" perms="menuWebpages"/><% request.setAttribute("closeTable", true);
%><%
	//Cache c = Cache.getInstance();
	//c.removeObject("cloud.AppManager.appsList");

	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);
	request.setAttribute("dialogTitle", prop.getText("components.cloud.apps.title"));
	request.setAttribute("dialogDesc", prop.getText("components.cloud.apps.description"));
	request.setAttribute("iconLink", "/components/_common/admin/inline/editor-components.gif");

	response.setHeader("X-UA-Compatible", "IE=edge");
	request.setAttribute("X-UA-Compatible", "IE=edge");

	int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docId"), -1);
	int groupId = Tools.getIntValue(Tools.getRequestParameter(request, "groupId"), -1);

%><!DOCTYPE html>
<html>

<head>
<iwcm:combine type="js" set="adminJqueryJs" />


<script type="text/javascript" src="/components/_common/javascript/jquery.cookie.js"></script>
<script type="text/javascript" src="jquery.mousewheel.js"></script>
<script type="text/javascript" src="jquery.cycle.js"></script>

<iwcm:combine type="css" set="adminStandardCssWj9" />

<script type="text/javascript">

var pageSize = 4,
	 actualHeight = 0,
	 pageHeight = 0;

var activeDetail = null;

function Ok(){
	if(activeDetail!=null){
		componentClick(activeDetail);
	}
	else{
	return false;
	}
}

    var isSmall = false;

// zmensovanie headera
$(window).scroll(function (event) {
    var scroll = $(window).scrollTop();
    if(scroll>100){



    	$(".block-header span").fadeOut(function(){

    		$(".block-header").addClass("small");
    		$(".block-header").height(60);

    	});

    	if(!isSmall){
	$(".block-header input.search").stop().animate({
    			"right": -360,
    			"left": "auto"},1000
    			 );		}

   		isSmall = true;


    }else{
    	//$(".block-header input.search").stop().fadeOut();
    	$(".block-header span").stop().fadeIn();
    	$(".block-header").removeClass("small");
    	$(".block-header").stop().height(170);

if(isSmall){
	$(".block-header input.search").stop().animate({
    			"right": "50%",
    			"left": "50%"},1000
    			 );		}
	    	isSmall = false;

    }
});


$("document").ready(function()
{
	$("div.gallery ul").each(function(i){

		// zobrazujem pretoze cycle nieco pokazi na skrytom dive
		$(this).parents(".app").show();

		// vytvaram pagere pre vsetky galerie
		$(this).parent(".gallery").append('<div class="cycle-pager cycle-pager'+ i +'"></div>');

		// cycle na vsetky ul v galeriach
		$(this).cycle({
			pager:  '.cycle-pager' + i,
			/*
		   next:   '.cycle-next',
		   prev:   '.cycle-prev',
		 	timeout: 0,
		 	*/
		   cleartypeNoBg: true
		});

		$(this).parents(".app").hide();
	});


	$(".app .button-back").click(function(){
		hideApps();
		$(".menu .menu-app").removeClass("selected");
		$(".menu,.promoApp, .block-header").fadeIn();

	});

	$("div.promo .promoApp").click(function(event){
		showFeatured($(this).attr("id").replace("promo-", ""));
	});

	$(".menu .menu-app").click(function(){

		// pokial je selected tak skryjem apps
		if ($(this).hasClass("selected")) {
			hideApps();
			activeDetail = null;
			return;
		}
		// odstranim classu selected
		$(".menu .menu-app").removeClass("selected");

		// podla li si najdem nazov divu v store
		var app = $(this).data("app");

		// skryjem vsetky predosle app
		$(".store .app").hide();

		// zobrazim app podla premennej app
	//$(".promoApp, .block-header").fadeOut();
		$(".menu,.promoApp, .block-header").fadeOut(function(){'fast',	$(".store").find("." + app).fadeIn();});


		activeDetail = $(this).data("app-action");
		// pre li nastavim classu selected
		$(this).addClass("selected");
	});

	// vyhladavanie

	// prepisem filter aby bol case a accent insensitive
	jQuery.expr[':'].contains = function(a, i, m) {
    var rExps=[
        {re: /[\xC0-\xC6]/g, ch: "A"},
        {re: /[\xE0-\xE6]/g, ch: "a"},
        {re: /[\xC8-\xCB]/g, ch: "E"},
        {re: /[\xE8-\xEB]/g, ch: "e"},
        {re: /[\xCC-\xCF]/g, ch: "I"},
        {re: /[\xEC-\xEF]/g, ch: "i"},
        {re: /[\xD2-\xD6]/g, ch: "O"},
        {re: /[\xF2-\xF6]/g, ch: "o"},
        {re: /[\xD9-\xDC]/g, ch: "U"},
        {re: /[\xF9-\xFC]/g, ch: "u"},
        {re: /[\xC7-\xE7]/g, ch: "c"},
        {re: /[\xD1]/g, ch: "N"},
        {re: /[\xF1]/g, ch: "n"}
    ];

    var element = $(a).text();
    var search  = m[3];

    $.each(rExps, function() {
         element    = element.replace(this.re, this.ch);
         search     = search.replace(this.re, this.ch);
    });

    return element.toUpperCase()
        .indexOf(search.toUpperCase()) >= 0;
};

	$("#search").on("keyup",function(){
		if($(this).val()!=""){
			$(".promoBox").fadeOut();
			$(".menu-app").stop().fadeOut();
			$(".menu-app").find(".title:contains("+$(this).val()+")").parent().parent().parent().stop().fadeIn();
		} else {
			$(".menu-app, .promoBox").fadeIn();
		}
	});
	/// end vyhladavanie

	/// end of ready
});

function showFeatured(key)
{
	var topPosition = $("#"+key).offset().top + actualHeight;
	if (topPosition < 0) topPosition = 0;

	var failsafe = 0;
	if (topPosition > (actualHeight + pageHeight))
	{
		while (topPosition > (actualHeight+pageHeight) && failsafe++ < 10)
		{
			//window.alert(topPosition+" vs "+(actualHeight));
			//scrollerDown();
			actualHeight = actualHeight + pageHeight;
		}

		$(".menuBox ul").animate({
			top: "-" + (actualHeight) + "px"
		});
	}
	else if (topPosition < actualHeight)
	{
		while (topPosition < actualHeight && failsafe++ < 10)
		{
			//window.alert(topPosition+" vs "+(actualHeight));
			actualHeight = actualHeight - pageHeight;
		}
		$(".menuBox ul").animate({
			top: "-" + (actualHeight) + "px"
		});
	}

	$("#"+key).click();
}

function hideApps () {
	$(".menu li").removeClass("selected");
	$(".store .app").hide();
}

function scrollerUp(){
	if (actualHeight - pageHeight < 0 || $(".menuBox ul").is(':animated')) {
		return false;
	}

	hideApps();

	actualHeight = actualHeight - pageHeight;

	$(".menuBox ul").animate({
		top: "-" + actualHeight + "px"
	});
}

function scrollerDown()
{
	//window.alert("false, actualHeight="+actualHeight+" top="+$(".menu li:last").offset().top+" pageHeight="+pageHeight);
	if (pageHeight > $(".menu li:last").offset().top || $(".menuBox ul").is(':animated'))
	{
		return false;
	}

	hideApps();

	$(".menuBox ul").animate({
		top: "-" + (actualHeight + pageHeight) + "px"
	});

	actualHeight = actualHeight + pageHeight;
}

function componentClick(componentName, width, height)
{
	if (typeof width == "undefined") {
		width = 400;
	}

	if (typeof height == "undefined") {
		height = 330;
	}

  	var url = '/components/'+componentName+'/editor_component.jsp?docId=<%=docId%>&groupId=<%=groupId%>';
  	if(componentName.indexOf("/")==0) url = componentName;
  	window.location.href = url;
}



</script>
<link rel="stylesheet" type="text/css" href="appstore-new.css?t=<%=Tools.getNow() %>" />
<style type="text/css">
	div.menu-app .col-sm-4 {
		text-align: center;
	}
	div.menu-app .fa-icon {
		font-size: 38px;
		vertical-align: middle;
		padding-top: 14px;
	}
	div.promoBox .fa-icon {
		font-size: 45px;
		line-height: 60px;
	}
	div.app-info .fa-icon {
		font-size: 60px;
		line-height: 83px;
	}
</style>

</head>
<body>

<div class="appStore clearfix">
	<div class="block-header">
		<div class="content">
			<div class="container">
				<h1><iwcm:text key="components.appstore.heading"/></h1>
				<span><iwcm:text key="components.appstore.desc"/></span>
				<input type="text" id="search" class="search">
			</div>
		</div>
	</div>
	<div class="store">

		<div class="promo">
			<div class="promoBox">
				<%

				String appstorePromo = Constants.getString("appstorePromo");
				List<String> appstorePromoList = null;

				if(Tools.isNotEmpty(appstorePromo)){
					appstorePromoList = Arrays.asList(appstorePromo.split(","));
				}

				String domain = DocDB.getDomain(request);
				if (domain != null) domain = domain.toLowerCase();

				List<AppBean> appsListAll = AppManager.getAppsList(request);
				List<AppBean> appsList = new ArrayList<AppBean>();
				for (AppBean app : appsListAll)
				{
					//filtrujeme az tu, lebo zoznamALL je cachovany
					if (Tools.isNotEmpty(app.getDomainName()) && app.getDomainName().contains(domain)==false) continue;
					if ("components.user.title".equals(app.getNameKey())) continue;
					if ("components.form.title".equals(app.getNameKey())) continue;

					appsList.add(app);
				}


				int appCounter = 1;
				%>
			<div class="content">
				<div class="container">
					<div class="row">
				<%
				for (AppBean app : appsList)
				{
					if (!appstorePromoList.contains(app.getComponentClickAction()))
					{
						continue;
					}
				%>
			<div class=" col-sm-3 promoApp-col">
						<div class="promoApp<%=appCounter %> promoApp <%= (appCounter%2==0) ? "promoAppLeft" : "promoAppRight" %>"  id="promo-<%=DocTools.removeChars(app.getNameKey(), true).replace('.', '-') %>">
							<div class="img">
								<%
								if (app.getImagePath().contains("/") )out.println("<img src=\""+app.getImagePath()+"\" alt=\"\" />");
								else if (app.getImagePath().startsWith("fa") && app.getImagePath().contains(" ")) out.println("<i class=\"fa-icon "+app.getImagePath()+"\"></i>");
								%>
							</div>
							<div class="info">
								<h3><iwcm:text key="<%=app.getNameKey() %>"/></h3>
								<%	if (app.isFree()) {%><span class="price free"><iwcm:text key="components.cloud.appstore.free"/></span><% } else { %><span class="price"><%=app.getPriceEur() %> €</span><% } %>
								<a href="javascript:componentClick('<%=app.getComponentClickAction()%>')" class="buy"><iwcm:text key="components.cloud.apps.insertToYourSite"/></a>
							</div>
							<div class="clearer"></div>
						</div>
		</div>

				<%
					appCounter++;
				}
				if (appCounter%2==1) {
					//out.print("</div>");
				}
				%>
			</div>

			</div></div>
		</div>

		<%

		appCounter = 1;
		for (AppBean app : appsList)
		{
			%>
			<div class="app<%=appCounter %> app">
				<div class="app-header content">
					<div class="container">
					<a class="button-back"><img alt="<iwcm:text key="components.appstore.return"/>" src="/admin/appstore/images/btn-back.png"><iwcm:text key="components.appstore.return"/></a>
					</div>
				</div>
				<div class="content app-info">
					<div class="container">
						<div class="row">
							<div class="col-sm-2 img">
								<%
								if (app.getImagePath().contains("/") )out.println("<img src=\""+app.getImagePath()+"\" alt=\"\" />");
								else if (app.getImagePath().startsWith("fa") && app.getImagePath().contains(" ")) out.println("<i class=\"fa-icon "+app.getImagePath()+"\"></i>");
								%>
							</div>
							<div class="col-sm-6 app-name">
								<h2><iwcm:text key="<%=app.getNameKey() %>"/></h2>
								<p><iwcm:text key="components.appstore.desc"/></p>
							</div>
							<div class="col-sm-4 app-buy">
							<a href="javascript:componentClick('<%=app.getComponentClickAction()%>')" class="buy"><iwcm:text key="components.cloud.apps.insertToYourSite"/></a>
							<%	if (app.isFree()) {%><span class="price free"><iwcm:text key="components.cloud.appstore.free"/></span><% } else { %><span class="price"><%=app.getPriceEur() %> €</span><% } %>
							</div>

						</div>
					</div>
				</div>

				<div class="content app-desc">
					<div class="container">
						<div class="row">
						<%
						int colWidth = 12;
						if (app.getGalleryImages()!=null && app.getGalleryImages().size()>0) {
						colWidth = 6;
						%>
							<div class="col-sm-6 gallery">

							<ul>
								<% for (String path : app.getGalleryImages()) { %>
								<li><img src="<%=path %>" alt="" /></li>
								<% } %>
							</ul>
						</div>

						<% } %>

						<div class="col-sm-<%=colWidth %>">
						<div class="description">
						<h3><iwcm:text key="components.appstore.popis"/></h3>
							<%
						   if(Tools.isNotEmpty(app.getDescriptionKey()) && app.getDescriptionKey().indexOf(" ") > -1) out.print(app.getDescriptionKey());
						   else out.print(prop.getText(app.getDescriptionKey()));
						   %>
						</div>
						</div>

			</div>

			</div>

			</div>



			</div>
			<%
			appCounter++;
		}
		%>

	</div>

	<div class="menu content">

		<div class="menuBox container">
			<div class="row">
				<%
				appCounter = 1;

				for (AppBean app : appsList)
				{
					//System.out.println("domain NAME="+app.getDomainName()+" domain="+DocDB.getDomain(request));

					//System.out.println(app.getNameKey());
					%>
					<div class="menu-app col-sm-3" data-app-action="<%=app.getComponentClickAction() %>" data-app="app<%=appCounter%>" id="<%=DocTools.removeChars(app.getNameKey(), true).replace('.', '-')%>">
						<div class="row">
							<div class="col-sm-4">
								<div class="img">
									<%
									if (app.getImagePath().contains("/") )out.println("<img src=\""+app.getImagePath()+"\" alt=\"\" />");
									else if (app.getImagePath().startsWith("fa") && app.getImagePath().contains(" ")) out.println("<i class=\"fa-icon "+app.getImagePath()+"\"></i>");
									%>
								</div>
							</div>
							<div class="col-sm-8 info">

									<span class="title"><iwcm:text key="<%=app.getNameKey() %>"/></span>
									<%	if (app.isFree()) {%><br><span class="price free"><iwcm:text key="components.cloud.appstore.free"/></span><% } else { %><br><span class="price"><%=app.getPriceEur() %> €</span><% } %>

							</div>
						</div>
					</div>
					<%
					appCounter++;
				} %>
			</div>
		</div>


	</div>

</div>
</div>
</body>
</html>
