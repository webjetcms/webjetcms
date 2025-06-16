<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%><%@page import="org.apache.commons.codec.binary.Base64"%><%@page import="sk.iway.iwcm.system.multidomain.MultiDomainFilter"%><%@page import="sk.iway.iwcm.tags.JSEscapeTag"%><%@page import="java.io.File"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="
sk.iway.iwcm.*,
java.util.*,
sk.iway.iwcm.components.banner.model.*" %><%@ page import="sk.iway.iwcm.tags.WriteTag" %>
 <%@ page import="sk.iway.iwcm.doc.DocDetails" %>
 <%@ page import="sk.iway.iwcm.io.IwcmFile" %>
 <%@ page import="sk.iway.iwcm.components.banner.BannerDB" %>
 <%@ page import="sk.iway.iwcm.stat.BrowserDetector" %>

<style>
div.jumbotron-has-video {
	overflow: hidden;
	position: relative;
}
div.jumbotron-has-video video, div.jumbotron-has-video iframe {
	width: 100%;
    position: absolute;
    object-fit: cover;
	top: 0px;
	left: 0px;
	z-index: 0;
}
div.jumbotron-has-video iframe {
	height: 100%;
}
div.jumbotron-has-video div.masthead-content-wrapper, div.jumbotron-has-video div.masthead-links {
	position: relative;
	z-index: 1;
}

div.jumbotron-has-video-fullscreen {
	background: rgba(0, 0, 0, 0%);
}
div.jumbotron-has-video-fullscreen video, div.jumbotron-has-video-fullscreen iframe {
	position: fixed;
	top: 50%;
	left: 50%;
	min-width: 100%;
	min-height: 100%;
	z-index: -100;
	width: auto;
	height: auto;
	transform: translateX(-50%) translateY(-50%);
	background-size: cover;
	background: rgba(0, 0, 0, 0%);
}
div.jumbotron-has-video-fullscreen:before {
    content: '';
    display: block;
    width: 100%;
    height: 100%;
    position: fixed;
    background: #000;
    opacity: .2;
    top: 0;
    bottom: 0;
    right: 0;
    left: 0;
    margin: auto;
    z-index: -1;
}
</style>

<%!
	public String prepareVideoCode(String link) {
		String htmlCode = "";
		if(link.contains("youtube.com")) {
			//YouTue version
			String urlParamsString = link.split("\\?")[1];
			if(Tools.isNotEmpty(urlParamsString)) {
				String urlParamsList[] = urlParamsString.split("&");
				String videoParam = null;
				for(String urlParam : urlParamsList) {
					if(urlParam.startsWith("v=")) {
						videoParam = urlParam.split("v=")[1];
						break;
					}
				}

				if(videoParam != null) {
					htmlCode += "	<iframe id=\"video\" src=\"//www.youtube.com/embed/" + videoParam + "?enablejsapi=1&amp;html5=1&amp;mute=1&amp;showinfo=0&autoplay=1\" frameborder=\"0\" allowfullscreen=\"\" class=\"video\" data-gtm-yt-inspected-2=\"true\"></iframe>";
				}
			}
		} else if(link.endsWith(".mp4")) {
				htmlCode += "<video autoplay=\"\" loop=\"\" muted=\"\"><source src=\"" + link + "\" type=\"video/mp4\" /></video>";
		}

		return htmlCode;
	}
%>

 <%

try
{

	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	String url = PathFilter.getOrigPath(request);
	if (Tools.isNotEmpty(paramPageParams))
	{
		paramPageParams = Tools.replace(paramPageParams, "|", "=");
		Base64 b64 = new Base64();
		String decoded = new String(b64.decode(paramPageParams.getBytes()));
		//System.out.println("ENCODED: "+paramPageParams+"|");
		//System.out.println("DECODED: "+decoded+"|");
		request.setAttribute("includePageParams", decoded);
	}

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	PageParams pageParams = new PageParams(request);
	String group = pageParams.getValue("group", null);
	String status = pageParams.getValue("status", "disabled");
	String showAll = pageParams.getValue("showAll", "false");
	int statClickDocId = pageParams.getIntValue("statClickDocId",4);
	int displayMode = pageParams.getIntValue("displayMode",1);
	int viewType = pageParams.getIntValue("viewType",1);
	int refreshRate = pageParams.getIntValue("refreshRate",0);
	String bannerIndex = pageParams.getValue("bannerIndex", "");
	boolean showInIframe = pageParams.getBooleanValue("showInIframe", false);
	boolean targetBlank = pageParams.getBooleanValue("targetBlank", true);
	boolean useAjax = pageParams.getBooleanValue("useAjax", false);
	boolean insertPerexPre = pageParams.getBooleanValue("insertPerexPre",true);

	String baseJumbotronClass = "jumbotron-has-video";
	if (group != null && group.contains("fullscreen")) baseJumbotronClass = "jumbotron-has-video-fullscreen";
	String jumbotronVideoClass = pageParams.getValue("jumbotronVideoClass", baseJumbotronClass);

	String videoWrapperClass = pageParams.getValue("videoWrapperClass", "embed-responsive embed-responsive-16by9 ratio ratio-16x9 banner-has-video");

	boolean usingPerexImage=false;
	DocDetails docDetails = null;

	String campaignBanner = request.getParameter(Constants.getString("bannerCampaignParamName"));

    if (Tools.isEmpty(bannerIndex)) bannerIndex = group;

	//ajax
	if (useAjax && paramPageParams==null)
	{
		Tools.insertJQuery(request);

		//vygeneruj unikatne divID
		Integer divId = (Integer)request.getAttribute("bannerDivId");
		if (divId == null) divId = Integer.valueOf(1);

		paramPageParams = (String)request.getAttribute("includePageParams");
		Base64 b64 = new Base64();
		String base64encoded = new String(b64.encode(paramPageParams.getBytes()));
		base64encoded = Tools.replace(base64encoded, "=", "|");
		%>
		<div class="bannerAjaxDiv" id="bannerAjaxDiv<%=divId.intValue()%>"></div><script type="text/javascript">
		$(document).ready( function() {
			$.ajax( { url: "/components/banner/banner.jsp?did=<%=divId.intValue()%>&pageParams=<%=Tools.URLEncode(base64encoded) %>", cache: false, success: function(data) { writeFlashWebJETToDiv(data, "bannerAjaxDiv<%=divId.intValue()%>"); if (data.indexOf("OBJECT")!=-1 || data.indexOf("object")!=-1) { $("#bannerAjaxDiv<%=divId.intValue()%>").html( $("#bannerAjaxDiv<%=divId.intValue()%> > OBJECT").html() ); } } } );
		});
		</script>
		<%
		request.setAttribute("bannerDivId", Integer.valueOf(divId.intValue()+1));
		return;
	}

	BannerBean banner = new BannerBean();

	//iframe
	if (showInIframe && paramPageParams==null)
	{
		paramPageParams = (String)request.getAttribute("includePageParams");

		Base64 b64 = new Base64();
		String base64encoded = new String(b64.encode(paramPageParams.getBytes()));
		base64encoded = Tools.replace(base64encoded, "=", "|");

		out.print("<iframe class='bannerIframe' scrolling='no' width='"+pageParams.getIntValue("iframeWidth", 468)+"' height='"+pageParams.getIntValue("iframeHeight", 60)+"' src='/components/banner/banner.jsp?pageParams="+Tools.URLEncode(base64encoded)+"' marginWidth=0 marginHeight=0 frameBorder=0></iframe>");
		return;
	}

	//Sposob zobrazovania bannerov

	//za sebou
	if (displayMode == 1)
	{
		//out.println("MODE 1 ses="+session.getAttribute("bannerIndex"+bannerIndex)+" group="+group);
		if (session.getAttribute("bannerIndex"+bannerIndex) == null)
		{
			List bannerList = new ArrayList();
			banner = BannerDB.getNextBanner(group, session, bannerList, bannerIndex, null, campaignBanner);
			session.setAttribute("bannerIndex"+bannerIndex, "");
		}
		else
		{
			banner = BannerDB.getNextBanner( group, session, (ArrayList)session.getAttribute("bannerList"+bannerIndex), bannerIndex, null, campaignBanner);
		}
	}

	//nahodne
	try
	{
		if (displayMode == 2) banner = BannerDB.getRandomBanner(group, campaignBanner);
	}
	catch (Exception ex)
	{
	   sk.iway.iwcm.Logger.error(ex);
	}

	//vaha
	if (displayMode == 3)
	{
	   //System.out.println("som priority baner");
		banner = BannerDB.getPriorityBanner(group, campaignBanner);
	}

	// len pre dane url - prvy banner na zaklade priority
	if( displayMode == 4)
	{
		banner = BannerDB.getFirstBannerForUrlByPriority(url,group,campaignBanner);
	}

	// len pre dane url - nahodny
	if( displayMode == 5)
	{
		banner = BannerDB.getBannerForUrlRandom(url,group,campaignBanner);
	}

	//System.out.println("m=" + displayMode + " ban=" + banner);
	try
	{
		//skusim ziskat banner obrazok z perexImage alebo z konf. premennej bannerDefaultImageUrl
		if (banner==null || banner.getBannerGroup()==null)
		{
			docDetails = (DocDetails) request.getAttribute("docDetails");

			//skusim ziskat banner obrazok z perexImage
			String defaultBannerImg = "";
			if (Tools.isNotEmpty(docDetails.getPerexImage()))
			{
				defaultBannerImg = docDetails.getPerexImage();
				Logger.debug("banner.jsp","Skusim nastavit banner obrazok z perexImage="+docDetails.getPerexImage());
				usingPerexImage=true;
			}
			else
			{
				//ak nenajdem, skusim ziskat z cesty definovanej v konf. premennej bannerDefaultImageUrl
				defaultBannerImg = Constants.getString("bannerDefaultImageUrl");
				Logger.debug("banner.jsp","Skusim nastavit banner obrazok z konfiguracnej premennej bannerDefaultImageUrl="+defaultBannerImg);
			}

			if(Tools.isNotEmpty(defaultBannerImg))
			{
				IwcmFile defaultBannerImgFile=new IwcmFile(Tools.getRealPath(defaultBannerImg));
				if(defaultBannerImgFile.exists())
				{
					banner=new BannerBean();
					banner.setBannerType(4);
					banner.setPrimaryHeader(docDetails.getTitle());
					banner.setBannerLocation(url);
					banner.setActive(true);
					banner.setImageLink(defaultBannerImg);
					banner.setImageLinkMobile(defaultBannerImg);
				}
			}
			else
			{
				banner = null;
			}
		}

		//niekedy dole nastavala chyba Error resolving fault, no matching row exists in the database
		banner.getBannerType();
		banner.getBannerLocation();
		banner.getBannerRedirect();
	}
	catch (Exception ex)
	{
		return;
	}

	String objectLink;
	String redirectLink;
	String target;
	String htmlCode = "";

	String statClick = "/components/_common/clk.jsp?";

	if (status.equals("enabled"))
	{
		if (banner != null)
		{
			try
			{
				if ("true".equals((String)request.getAttribute("StatDB.isSearchEngine")))
				{
					//priznak vyhladavaca pre clk.jsp
					statClick += "se=1&";
				}
				//out.println("Banner ID = "+banner.getBannerId()+"<br>priority = "+banner.getPriority());

				statClick += "bid="+banner.getBannerId();
				statClick = Tools.replace(statClick, "&", "&amp;");
				redirectLink = banner.getBannerRedirect();
				target= banner.getTarget();

				if (banner.getBannerType().intValue() == 1 && banner.getActive().booleanValue()==true) { //picture
					objectLink = MultiDomainFilter.fixDomainPaths(banner.getBannerLocation(), request);

					if (Tools.isEmpty(redirectLink))
					{
						htmlCode = "<img src='" + objectLink + "' alt='"+ResponseUtils.filter(banner.getName())+"' title='"+ResponseUtils.filter(banner.getName())+"'/>";
					}
					else
					{
						htmlCode = "<a href='" +statClick+ "'";
						if (Tools.isNotEmpty(target) && "_self".equalsIgnoreCase(target)==false) htmlCode += " target='"+target+"'";
						 htmlCode += "><img src='" + objectLink + "' alt='"+ResponseUtils.filter(banner.getName())+"' title='"+ResponseUtils.filter(banner.getName())+"'/></a>";
					}
				}

				if (banner.getBannerType().intValue() == 3 && banner.getActive().booleanValue()==true) { //html code

					htmlCode = banner.getHtmlCode();
				}

				if (banner.getBannerType().intValue() == 4 && banner.getActive().booleanValue()==true) { //content banner
					objectLink = MultiDomainFilter.fixDomainPaths(banner.getBannerLocation(), request);

					if(BannerDB.isBannerForUrl(banner,url)){
						String mobileImageLink = banner.getImageLinkMobile();
						String imageLink = banner.getImageLink();
						htmlCode = "";
						if (Tools.isNotEmpty(banner.getImageLink()) && Tools.isEmpty(banner.getImageLinkMobile())) {
							htmlCode += "<style type='text/css'>\n";
								htmlCode += "	#jumbotron-"+banner.getBannerId()+" { background-image: url(/thumb"+banner.getImageLink()+"?w=1280&ip=1); background-size: cover; background-position: center; } }\n";
							htmlCode += "</style>\n";
						} else if (Tools.isNotEmpty(banner.getImageLink()) && Tools.isNotEmpty(banner.getImageLinkMobile())) {
							htmlCode += "<style type='text/css'>\n";
								htmlCode += "	#jumbotron-"+banner.getBannerId()+" { background-image: url(/thumb"+banner.getImageLinkMobile()+"?w=576&ip=1); background-size: cover; background-position: center; }\n";
								htmlCode += "	@media (min-width: 760px) {\n		#jumbotron-"+banner.getBannerId()+" { background-image: url(/thumb"+banner.getImageLink()+"?w=1280&ip=1); }\n	}\n";
							htmlCode += "</style>\n";
						}

						String videoCode = null;
						if(Tools.isNotEmpty(banner.getImageLinkMobile()) && BrowserDetector.getInstance(request).isPhone())
							videoCode = prepareVideoCode(banner.getImageLinkMobile());
						else if(Tools.isNotEmpty(banner.getImageLink()))
							videoCode = prepareVideoCode(banner.getImageLink());

						htmlCode += "<div class='jumbotron c-masthead";
						if (Tools.isNotEmpty(videoCode)) htmlCode += " "+jumbotronVideoClass;
						htmlCode += "' data-module='masthead' id='jumbotron-"+banner.getBannerId()+"'>";

						if (Tools.isNotEmpty(videoCode)) htmlCode += videoCode;

						/*if (Tools.isNotEmpty(banner.getImageLink()) && Tools.isNotEmpty(banner.getImageLinkMobile())) {
							htmlCode += "><div class='masthead-image-wrapper'>" +
									"          <picture class='masthead-image'>" +
									"              <source data-size='xl' media='(min-width: 760px)' srcset='/thumb"+ banner.getImageLink()+"?w=1280&h=474&ip=5'>" +
									"              <source data-size='l' media='(min-width: 320px)' srcset='/thumb"+banner.getImageLinkMobile()+"?w=576&h=162&ip=5'>" +
									"              <img class='hidden' src='/thumb"+banner.getImageLinkMobile()+"?w=576&h=162&ip=5' alt=''>" +
									"          </picture>" +
									"	</div>";
						} else if (Tools.isNotEmpty(banner.getImageLink())) {
							htmlCode += " style='background-image:url(/thumb"+banner.getImageLink()+"?w=1280&ip=1); background-size: cover;'>";
						} else htmlCode += ">";*/
						htmlCode += "<div class='masthead-content'>";

						//System.out.println("")

						if (Tools.isNotEmpty(banner.getPrimaryHeader())) {
							htmlCode += "<h1>"+banner.getPrimaryHeader()+"</h1>\n";
						}
						htmlCode += "<div class='masthead-content-wrapper'>\n";
							//ak zobrazujem vychodzi banner, tak sekundarny text beriem z perex miesta a popis z perexu
							if(usingPerexImage && docDetails != null){
								if (Tools.isNotEmpty(docDetails.getPerexPlace())) {
									htmlCode += "<p class='lead headingM'>"+docDetails.getPerexPlace()+"</p>\n";
								}
								if (insertPerexPre && Tools.isNotEmpty(docDetails.getPerexPre())) {
									htmlCode += "<div class='description'>"+docDetails.getPerexPre()+"</div>\n";
								}
							}else{
								if (Tools.isNotEmpty(banner.getSecondaryHeader())) {
									htmlCode += "<p class='lead headingM'>"+banner.getSecondaryHeader()+"</p>\n";
								}
								if (Tools.isNotEmpty(banner.getDescriptionText())) {
									htmlCode += "<div class='description'>"+banner.getDescriptionText()+"</div>\n";
								}
							}
						htmlCode += "</div>";
						Boolean primaryLink = (Tools.isNotEmpty(banner.getPrimaryLinkUrl()) && Tools.isNotEmpty(banner.getPrimaryLinkTitle()));
						Boolean secondaryLink = (Tools.isNotEmpty(banner.getSecondaryLinkUrl()) && Tools.isNotEmpty(banner.getSecondaryLinkTitle()));
						if (primaryLink || secondaryLink) {
							htmlCode += "<div class='masthead-links'>";
							if (primaryLink) {
								htmlCode += "<a class='btn btn-primary' href='"+ banner.getPrimaryLinkUrl() +"' target='"+ banner.getPrimaryLinkTarget() +"'>"+banner.getPrimaryLinkTitle()+"</a>";
							}
							if (secondaryLink) {
								htmlCode += "<a class='btn btn-secondary' href='"+ banner.getSecondaryLinkUrl() +"' target='"+ banner.getSecondaryLinkTarget() +"'>"+banner.getSecondaryLinkTitle()+"</a>";
							}
							htmlCode += "</div>";
						}
						htmlCode += "</div>";
						htmlCode += "</div>";
					} else {
						htmlCode = "";
					}
				}

				if (banner.getBannerType().intValue() == 5 && banner.getActive().booleanValue() == true) { //video
					String videoCode = prepareVideoCode(banner.getBannerLocation());
					if (Tools.isNotEmpty(videoCode)) {
						//click event
						if (Tools.isNotEmpty(redirectLink)) {
							htmlCode += "<a href='" +statClick+ "'";
							if (Tools.isNotEmpty(target) && "_self".equalsIgnoreCase(target)==false) htmlCode += " target='"+target+"'";
							htmlCode += ">";
						}

						htmlCode += "<div class=\""+videoWrapperClass+"\">";
						htmlCode += videoCode;
						htmlCode += "</div>";

						if (Tools.isNotEmpty(redirectLink)) htmlCode += "</a>";
					}
				}

				if ("true".equals((String)request.getAttribute("StatDB.isSearchEngine"))==false)
				{
					//zarataj statistiku videni
					BannerDB.statAddView(banner.getBannerId());
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
	}

	if (showInIframe && paramPageParams!=null)
	{
		out.println("<html>");
		out.println("<head>");
		if (refreshRate != 0)
		{
			url = PathFilter.getOrigPath(request);
			//out.println("<META HTTP-EQUIV=Refresh CONTENT='" +refreshRate+ "; URL="+url+"'>");
			out.println("<META HTTP-EQUIV=Refresh CONTENT='" +refreshRate+ "'>");
		}
		out.println("   <META HTTP-EQUIV='Content-Type' CONTENT='text/html; charset="+SetCharacterEncodingFilter.getEncoding()+"'>");
		out.println("   <link rel='stylesheet' href='/css/page.css' type='text/css'>");
		out.println("</head>");
		out.println("<body LEFTMARGIN=0 TOPMARGIN=0 MARGINWIDTH=0 MARGINHEIGHT=0>");
	}

	out.print(htmlCode);

	if (showInIframe && paramPageParams!=null)
	{
		out.print("</body></html>");
	}
}
catch (Exception wexp)
{
	//riesi problem s Cayenne chybou Error Resolving Fault, no matching row exists in database (asi nejaky problem s cache)
}
%>
