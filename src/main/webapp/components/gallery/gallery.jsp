<%@page import="java.awt.Dimension"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.gallery.*,java.util.*,java.io.*" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.apache.struts.util.ResponseUtils" %>
<%@ page import="sk.iway.iwcm.tags.WriteTag" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><stripes:useActionBean var="galleryActionBean" beanclass="sk.iway.iwcm.gallery.GalleryActionBean" /><%
    galleryActionBean.setPerexGroup("");
	pageContext.include("/sk/iway/iwcm/gallery/Gallery.action");

	String imgPrefix = "s_";
	if ("N".equals(GalleryDB.getResizeMode(galleryActionBean.getDir(), true))) imgPrefix = "";

	int imageWidth = 0;
	int imageHeight = 0;
	int smallImageWidth = 0;
	int smallImageHeight = 0;
	Dimension[] dim = GalleryDB.getDimension(galleryActionBean.getDir());
	if ((null != dim) && (dim.length >= 2) && (dim[1].width > 0))
	{
		imageWidth = dim[1].width;
		imageHeight = dim[1].height;

		smallImageWidth = dim[0].width;
		smallImageHeight = dim[0].height;
	}

	pageContext.setAttribute("image_width", Integer.valueOf(imageWidth));
	pageContext.setAttribute("image_height", Integer.valueOf(imageHeight));

	String style = galleryActionBean.getStyle();

	if (Tools.isNotEmpty(style) && style.equals("prettyPhoto")==false && style.equals("photoSwipe")==false)
	{
		//skus najst nahradny subor
      String stylePath = "/components/" + Constants.getInstallName() + "/gallery/gallery-" + style + ".jsp";
		File f = new File(sk.iway.iwcm.Tools.getRealPath(stylePath));
		if (f.exists())
		{
			pageContext.include(stylePath);
			return;
		}
		stylePath = "/components/gallery/gallery-" + style + ".jsp";

		f = new File(sk.iway.iwcm.Tools.getRealPath(stylePath));
		if (f.exists())
		{
			pageContext.include(stylePath);
			return;
		}
	}

	String bootstrapVersion = Constants.getString("bootstrapVersion");
	String paginationLink = "/components/gallery/pagination/jquery.simplePagination-bs4.js";
	if (bootstrapVersion.startsWith("3")) paginationLink = "/components/gallery/pagination/jquery.simplePagination.js";

	int counter = Tools.getIntValue(String.valueOf(request.getAttribute("webjet_gallery_counter")), 0) + 1;
	request.setAttribute("webjet_gallery_counter", counter);

%><%= Tools.insertJQuery(request) %>
<c:if test="${webjet_gallery_counter eq 1}">
	<iwcm:script type="text/javascript" src="<%=paginationLink%>"></iwcm:script>
	<iwcm:script type="text/javascript" src="/components/gallery/pagination/pagination.js"></iwcm:script>

	<style type="text/css">
		<%
		String path = WriteTag.getCustomPage("/components/gallery/gallery.css", request);
		out.println(FileTools.readFileContent(path));
		//pageContext.include(path);
		%>
	</style>
</c:if>

<c:if test="${galleryActionBean.pagination and galleryActionBean.itemsCount gt galleryActionBean.itemsOnPage}">
	<iwcm:script type="text/javascript">
	$(document).ready(function($) {
		$("#thumbs${webjet_gallery_counter} li").pagination(
		{
	      	items: Number('${galleryActionBean.itemsCount}'),
      		itemsOnPage: Number('${galleryActionBean.itemsOnPage}'),
      		paginationElement:'#pagination${webjet_gallery_counter}',
      		galleryCounter: ${webjet_gallery_counter}
		});
	});
	</iwcm:script>
</c:if>

<c:if test="${galleryActionBean.imagesInRow > 0}">
	<style type="text/css">
		#thumbs${webjet_gallery_counter} li:nth-child(${galleryActionBean.imagesInRow}n + 1) {clear:left}
	</style>
</c:if>
<c:if test="${image_height > 0}">
	<iwcm:style type="text/css">
		#thumbs${webjet_gallery_counter} li {
			width: <%=smallImageWidth%>px;
			min-height: <%=smallImageHeight%>px;
			text-align: center;
		}
	</iwcm:style>
</c:if>
<c:if test="${galleryActionBean.style eq 'prettyPhoto' and webjet_gallery_counter eq 1}">
	<iwcm:link rel="stylesheet" type="text/css" href="/components/gallery/prettyphoto/css/prettyPhoto.css"></iwcm:link>
	<iwcm:script src="/components/gallery/prettyphoto/js/jquery.prettyPhoto.js" type="text/javascript" charset="utf-8"></iwcm:script>
	<iwcm:script src="/components/gallery/prettyphoto/prettyphoto.js" type="text/javascript" charset="utf-8"></iwcm:script>
</c:if>


<c:if test="${galleryActionBean.style eq 'photoSwipe' and webjet_gallery_counter eq 1}">
<script type="text/javascript">
$(document).ready(function()
{
	try
	{
			var head = document.getElementsByTagName('HEAD')[0];

			if ($("#photoswipe1").length <= 0) {
				var link = document.createElement("script");
				link.src = "/components/gallery/photoswipe/js/photoswipe.js";
				link.id = "photoswipe1";
				head.appendChild(link);
			}
			if ($("#photoswipe2").length <= 0) {
				var link = document.createElement("script");
				link.src = "/components/gallery/photoswipe/js/photoswipe-ui-default.js";
				link.id = "photoswipe2";
				head.appendChild(link);
			}
			if ($("#photoswipe3").length <= 0) {
				var link = document.createElement("script");
				link.src = "/components/gallery/photoswipe/photoswipe.jsp";
				link.id = "photoswipe3";
				head.appendChild(link);
			}

	} catch (e) {}
});
</script>
</c:if>

<div id="thumbs${webjet_gallery_counter}" class="thumbs clearfix">
	<ul>
		<iwcm:forEach items="${galleryActionBean.photoList}" var="image" type="sk.iway.iwcm.gallery.GalleryBean"><li><%
					JSONObject title = new JSONObject();
					title.put("shortDescription", galleryActionBean.isShortDescription() ? image.getShortDescription() : "");
					title.put("longDescription", galleryActionBean.isLongDescription() ? image.getLongDescription() : "");
					title.put("author", galleryActionBean.isAuthor() ? image.getAuthor() : "");
					request.setAttribute("title", ResponseUtils.filter(title.toString()));
				%><a <c:if test="${galleryActionBean.style eq 'prettyPhoto'}">rel="prettyPhoto[pp_gal]"</c:if> title="${image.shortDescription}" data-title="${title}" class="thumb" href="${image.imagePath}/${image.imageName}" data-dimensions=${image.bigDimension}>
					<img src="${image.imagePath}/<%=imgPrefix %>${image.imageName}" alt="${image.shortDescription}" rel="<c:out value='${image.longDescription}'/>" loading="lazy" />
					<c:if test="${galleryActionBean.thumbsShortDescription}">
						<div class="galleryShortDescription"><%= image.getShortDescription() %></div>
					</c:if>
				</a>
			</li></iwcm:forEach>
	</ul>
</div>

<c:if test="${galleryActionBean.pagination and galleryActionBean.itemsCount gt galleryActionBean.itemsOnPage}">
	<div class="row">
		<div class="col-12 text-center"><div id="pagination${webjet_gallery_counter}"></div></div>
	</div>
</c:if>