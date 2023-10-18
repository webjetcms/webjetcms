<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%@ page pageEncoding="utf-8"
	import="java.util.*"
	import="java.awt.Dimension"
	import="sk.iway.iwcm.*"
	import="sk.iway.iwcm.io.IwcmFile"
	import="sk.iway.iwcm.i18n.Prop"
	import="sk.iway.iwcm.gallery.*"
%>
<%@page import="org.apache.struts.util.ImageButtonBean"%>
<%@page import="sk.iway.iwcm.io.IwcmFileFilter"%>
<%@page import="sk.iway.iwcm.io.IwcmFsDB"%>
<%@page import="java.text.Normalizer"%><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"  %>
<%!
private static String getGalleryName(String imagePath)
{
	if (Tools.isEmpty(imagePath)) return "&nbsp;";
	try
	{
		imagePath = (imagePath.lastIndexOf("/") == (imagePath.length()-1)) ? imagePath.substring(0,imagePath.length()-2):imagePath;
		GalleryDimension gd = GalleryDB.getGalleryInfo(imagePath, -1);
		String galleryName = null;
		if (gd != null) galleryName = gd.getGalleryName();
		if (Tools.isEmpty(galleryName))
		{
			int index = imagePath.lastIndexOf('/');
			if (index > 0) galleryName = imagePath.substring(index+1);
			else galleryName = Tools.replace(imagePath, "/", " ").trim();
		}
		return galleryName;
	}
	catch (Exception ex)
	{
		return imagePath;
	}
}

private static  String normalizeString(String s){
    s = Normalizer.normalize(s, Normalizer.Form.NFD);
    s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    s = s.replaceAll(" ", "-");
    return s;
}

%><%
	Prop prop = Prop.getInstance();
	//komponenta nepodporuje packager ControlJS
	request.setAttribute("packagerEnableControljs", Boolean.FALSE);
	request.setAttribute("packagerMode", "none");

	Encoding.setResponseEnc(request, response, "text/html");
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	PageParams pageParams = new PageParams(request);

	final String NUMBER_ATTRIBUTE = "components.gallery.photoSwipe.number";
	Object numberAttribute = request.getAttribute(NUMBER_ATTRIBUTE);
	int number = ((null == numberAttribute) ? 0 : ((Integer) numberAttribute).intValue()) + 1;
	request.setAttribute(NUMBER_ATTRIBUTE, Integer.valueOf(number));
	pageContext.setAttribute("slider_number", Integer.valueOf(number));

	String slider_id = "photoSwipe" + number;
	pageContext.setAttribute("slider_id", slider_id);

	String dir = pageParams.getValue("dir", "/images/gallery");
	boolean originalImages = pageParams.getBooleanValue("originalImages", false);

	String param="groupName"+number;
	String paramValue = Tools.getStringValue(Tools.getRequestParameter(request, param), "") ;

	//boolean alsoSubfolders = pageParams.getBooleanValue("alsoSubfolders", true);
	boolean alsoSubfolders = true;
	String orderBy = pageParams.getValue("orderBy", "title");
	String orderDirection = pageParams.getValue("orderDirection", "asc");
	String perexImagePath = pageParams.getValue("perexImagePath", "");

	int imageWidth = 0;
	int imageHeight = 0;
	int smallImageWidth = 0;
	int smallImageHeight = 0;
	String directory = (paramValue.length() <= 1) ? dir : (dir+paramValue);
	Dimension[] dim = GalleryDB.getDimension(directory);
	if ((null != dim) && (dim.length >= 2) && (dim[1].width > 0))
	{
		imageWidth = dim[1].width;
		imageHeight = dim[1].height;

		smallImageWidth = dim[0].width;
		smallImageHeight = dim[0].height;
	}

	pageContext.setAttribute("image_width", Integer.valueOf(imageWidth));
	pageContext.setAttribute("image_height", Integer.valueOf(imageHeight));

	List<GalleryBean> images = new ArrayList<GalleryBean>();

	if(paramValue.length() <= 1)
	{
		//System.out.println("Galeria, paramValue="+paramValue+" images="+images+" dir="+dir);

		images = GalleryDB.getImages(dir, alsoSubfolders, lng, (String) request.getAttribute("perexGroup"), orderBy, orderDirection, request);
	}
	else
	{
		images.addAll(GalleryDB.getImages(dir+paramValue, alsoSubfolders, lng, (String) request.getAttribute("perexGroup"), orderBy, orderDirection, request));
	}


    LinkedHashSet<String> adresare = new LinkedHashSet();

	for(GalleryBean image:images)
	{
		//adresare.add(image.getImagePath().substring(image.getImagePath().lastIndexOf('/')+1, image.getImagePath().length()));
		adresare.add(getGalleryName(image.getImagePath()));

	}


	// vyhodim root
	adresare.remove(dir.substring(dir.lastIndexOf('/')+1, dir.length()));

	// sortujem, najskor to prerobim na List a potom naspat
	List adresareList = new ArrayList(adresare);
	Collections.sort(adresareList);
    adresare = new LinkedHashSet(adresareList);

	request.setAttribute("images", images);
	request.setAttribute("dir", dir);
	request.setAttribute("originalImages", originalImages);

	int num = 0;
	String directory_name="";


%>

<%=Tools.insertJQuery(request)%>

	<link rel="stylesheet" href="/components/gallery/ajax/photoSwipe.css" type="text/css" media="screen" />

	<script type="text/javascript" src="/components/gallery/ajax/photoSwipe/klass.min.js"></script>
	<script type="text/javascript" src="/components/gallery/ajax/photoSwipe/code.photoswipe.jquery-3.0.5.min.js"></script>


 <script src="/components/gallery/scripts/jquery.mixitup.js"></script>
        <script type="text/javascript">

        $(document).ready(function(){

        	//$('.gallery').mixItUp();
        	/*
        	$(".photoSwipe").find("a").photoSwipe({
    			imageScaleMethod: "fitNoUpscale"
    		});*/

        		});

        </script>
<%

		//testuje sa priamo null, pretoze priamo pre gallery adresar to moze byt prazdne
		if (Tools.getRequestParameter(request, param)==null)
		{
			int tagCounter = 0;
			String tag = ".cathegory"+tagCounter;
			 int i=0;
			%>


	<div class="preview-pic tab-content gallery photoSwipe photoSwipeImages">
	<%if(!perexImagePath.isEmpty()){ %>
		<div class="tab-pane active" id="pic-<%=i%>"><img src="/thumb<%=perexImagePath %>?w=390&ip=1"/></div>
		<%i++;
		} %>

		<c:forEach items="${images}" var="image">


	<%

	GalleryBean image = (GalleryBean) pageContext.getAttribute("image");
String imageUrl = image.getImageUrl();

	if(!perexImagePath.equals(imageUrl)){ %>
	<div class="tab-pane <% if(i==0) out.print("active"); %>" id="pic-<%=i%>"><img src="/thumb${image.imagePath}/${image.imageName}?w=390&ip=1"/></div>
	<%} %>
		<% i++; %>
		</c:forEach>

	</div>
		<% i=0; %>
	<ul class="preview-thumbnail nav nav-tabs photoSwipe photoSwipeImages">
		<%if(!perexImagePath.isEmpty()){ %>

			<li class="<% if(i==0) out.print("active"); %>"><a data-target="#pic-<%=i%>" data-toggle="tab"><img src="/thumb<%=perexImagePath %>?w=390&ip=1" /></a></li>
	<%}
		i++; %>
		<c:forEach items="${images}" var="image">


		<%
		GalleryBean image = (GalleryBean) pageContext.getAttribute("image");
	String imageUrl = image.getImageUrl();

		if(!perexImagePath.equals(imageUrl)){ %>

		<li class="<% if(i==0) out.print("active"); %>"><a data-target="#pic-<%=i%>" data-toggle="tab"><img src="/thumb${image.imagePath}/${image.imageName}?w=390&ip=1" /></a></li>
		<% i++; %>
		<%} %>
		</c:forEach>

	</ul>

<%}%>



<%
WriteTag.setInlineComponentEditTextKey("components.gallery.editStyleAndPhotos", request);
%>
