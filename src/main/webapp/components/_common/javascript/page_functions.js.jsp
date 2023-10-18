<%@ page pageEncoding="utf-8" contentType="text/javascript" import="sk.iway.iwcm.*" %><%@page import="java.io.File"%><%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%

String lng = PageLng.getUserLng(request);

String lngParam = Tools.getRequestParameter(request, "language");
if (Tools.isNotEmpty(lngParam) && lngParam.length()==2)
{
	lng = lngParam;
	sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/page_common.js", null, request, response);
}

pageContext.setAttribute("lng", lng);
PageParams pageParams = new PageParams(request);

List<String> pageFunctionsInclude = new ArrayList<String>(Arrays.asList(Tools.getTokens(Constants.getString("pageFunctionsInclude"), "|")));

for (Iterator<String> iterator = pageFunctionsInclude.iterator(); iterator.hasNext();) {
	File f = new File(Tools.getRealPath(iterator.next()));
	if (!f.isFile()) {
		iterator.remove();
	}
}

request.setAttribute("pageFunctionsInclude", pageFunctionsInclude);
%>
function decodeEmailImpl(encodeEmail)
{
	var email = encodeEmail;
	if (email.indexOf("~") > -1)
		email = email.replace(/~/gi, '@');
	if(email.indexOf("!") > -1)
		email = email.replace(/!/gi, '.');

	<%--// cele to pre istotu otoc--%>
	var newEmail = new Array(email.length);
	for (i = 0; i < email.length; i++)
	{
		newEmail[i] = email.charAt(email.length - i - 1);
	}
	email = newEmail.join("");
	return email;
}
function decodeEmail(encodeEmail)
{
	window.location.href="mailto:"+decodeEmailImpl(encodeEmail);
}
function writeEmailToPage(encodeEmail)
{
	document.write(decodeEmailImpl(encodeEmail));
}
function openTargetBlank(link, event)
{
    var href = link.href;

    if (href.indexOf("javascript:")==0) return true;

    window.open(href,'_blank');
    event.returnValue = false;
    event.stopPropagation();
    return false;
}

<% if (Constants.getString("wjImageViewer").equals("photoswipe")) { %>
$(document).ready(function()
{
	try
	{
		  if ($("a[rel='wjimageviewer']").length > 0)
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
		  }

	} catch (e) {}
});
<% } else { %>
$(document).ready(function()
{
	try
	{
		  $("a[rel='wjimageviewer']").wjImageViewer();

		  if ($("a[rel='wjimageviewer']").length > 0)
		  {
			var head = document.getElementsByTagName('HEAD')[0];

			var link = document.createElement("link");

			link.href = "/components/gallery/ajax/wjImageViewer/wjImageViewer.css";
			link.type = "text/css";
			link.rel = "stylesheet";

			head.appendChild(link);
		  }

	} catch (e) {}
});

var opts;

(function($){
	$.fn.wjImageViewer = function(options){
		opts = $.extend({}, $.fn.wjImageViewer.defaults, options);

		document.onkeydown = function(e){
			if (e == null) {
				keycode = event.keyCode;
			}else{
				keycode = e.which;
			}
			if(keycode == 27) $.fn.wjImageViewer.end();
		};

		$.fn.wjImageViewer.initialize();
		return this.each(function(){
			$(this).click(function(){
				$(this).wjImageViewer.start(this);
				return false;
			});
		});
	};

	$.fn.wjImageViewer.initialize = function(){
		$('#overlay, #wjImageViewer').remove();

		var string = '<div id="overlay">&nbsp;</div><div id="wjImageViewer"><div id="outerImageContainer"><div id="imageContainer"><img id="wjImageViewerImage"/><div id="loading"><img src="'+opts.fileLoadingImage+'"/></div></div></div></div>';
		$("body").append(string);

		$("#overlay").click(function(){ $.fn.wjImageViewer.end(); }).hide();
		$("#wjImageViewer").click(function(){ $.fn.wjImageViewer.end();}).hide();
	};

	$.fn.wjImageViewer.getPageSize = function(){
		var xScroll, yScroll;

		if (window.innerHeight && window.scrollMaxY) {
			xScroll = window.innerWidth + window.scrollMaxX;
			yScroll = window.innerHeight + window.scrollMaxY;
		} else if (document.body.scrollHeight > document.body.offsetHeight){ <%--// all but Explorer Mac--%>
			xScroll = document.body.scrollWidth;
			yScroll = document.body.scrollHeight;
		} else { <%--// Explorer Mac...would also work in Explorer 6 Strict, Mozilla and Safari--%>
			xScroll = document.body.offsetWidth;
			yScroll = document.body.offsetHeight;
		}

		var windowWidth, windowHeight;

		if (self.innerHeight) { <%--// all except Explorer--%>
			if(document.documentElement.clientWidth){
				windowWidth = document.documentElement.clientWidth;
			} else {
				windowWidth = self.innerWidth;
			}
			windowHeight = self.innerHeight;
		} else if (document.documentElement && document.documentElement.clientHeight) { <%--// Explorer 6 Strict Mode--%>
			windowWidth = document.documentElement.clientWidth;
			windowHeight = document.documentElement.clientHeight;
		} else if (document.body) { <%--// other Explorers--%>
			windowWidth = document.body.clientWidth;
			windowHeight = document.body.clientHeight;
		}

		if(yScroll < windowHeight){
			pageHeight = windowHeight;
		} else {
			pageHeight = yScroll;
		}

		if(xScroll < windowWidth){
			pageWidth = xScroll;
		} else {
			pageWidth = windowWidth;
		}

		var arrayPageSize = new Array(pageWidth,pageHeight,windowWidth,windowHeight);
		return arrayPageSize;
	};


	$.fn.wjImageViewer.getPageScroll = function(){
		var xScroll, yScroll;

		if (self.pageYOffset) {
			yScroll = self.pageYOffset;
			xScroll = self.pageXOffset;
		} else if (document.documentElement && document.documentElement.scrollTop){  <%--// Explorer 6 Strict--%>
			yScroll = document.documentElement.scrollTop;
			xScroll = document.documentElement.scrollLeft;
		} else if (document.body) {<%--// all other Explorers--%>
			yScroll = document.body.scrollTop;
			xScroll = document.body.scrollLeft;
		}

		var arrayPageScroll = new Array(xScroll,yScroll);
		return arrayPageScroll;
	};

	$.fn.wjImageViewer.start = function(imageLink){
		$("select, embed, object").hide();
		var arrayPageSize = $.fn.wjImageViewer.getPageSize();
		$("#overlay").hide().css({width: '100%', height: arrayPageSize[1]+'px', opacity : opts.overlayOpacity}).fadeIn();
		imageNum = 0;
		<%--//opts.imageSrc.push(new Array(imageLink));--%>
		<%--//opts.imageSrc.push(new Array(imageLink.href));--%>
		opts.imageSrc = imageLink.href;

		var arrayPageScroll = $.fn.wjImageViewer.getPageScroll();
		var wjImageViewerTop = arrayPageScroll[1] + (arrayPageSize[3] / 10);
		var wjImageViewerLeft = arrayPageScroll[0];
		$('#wjImageViewer').css({top: wjImageViewerTop+'px', left: wjImageViewerLeft+'px'}).show();

		$.fn.wjImageViewer.changeImage();
	};

	<%--//WebJET doplnene - pozadovane WA pre podporu z povodnej funkcie wjPopup--%>
   $.fn.wjImageViewer.showDirect = function(imageUrl){
      $("select, embed, object").hide();
      var arrayPageSize = $.fn.wjImageViewer.getPageSize();
      $("#overlay").hide().css({width: '100%', height: arrayPageSize[1]+'px', opacity : opts.overlayOpacity}).fadeIn();
      imageNum = 0;
      opts.imageSrc = imageUrl;

      var arrayPageScroll = $.fn.wjImageViewer.getPageScroll();
      var wjImageViewerTop = arrayPageScroll[1] + (arrayPageSize[3] / 10);
      var wjImageViewerLeft = arrayPageScroll[0];
      $('#wjImageViewer').css({top: wjImageViewerTop+'px', left: wjImageViewerLeft+'px'}).show();

      $.fn.wjImageViewer.changeImage();
   };

	$.fn.wjImageViewer.changeImage = function(){
		$('#loading').show();
		$('#wjImageViewerImage').hide();

		$.fn.doChangeImage();
	};

	$.fn.doChangeImage = function(){
		imgPreloader = new Image();
		<%--//imgPreloader.src = opts.imageSrc[0];--%>
		imgPreloader.onload = function(){
		   var newWidth = imgPreloader.width;
		   var newHeight = imgPreloader.height;

		   var screenWidth = $(window).width();
	      var screenHeight = $(window).height();
	      var aspectRatio = newWidth / newHeight;
	      if (newWidth > screenWidth - (opts.borderSize * 2)) {
	        newWidth = screenWidth - (opts.borderSize * 2);
	        newHeight = newWidth / aspectRatio;
	      }
	      if (newHeight > screenHeight - (opts.borderSize * 2)) {
	        newHeight = screenHeight - (opts.borderSize * 2);
	        newWidth = newWidth * aspectRatio;
	      }

			$('#wjImageViewerImage').attr('src', opts.imageSrc).width(newWidth).height(newHeight);
			$.fn.wjImageViewer.resizeImageContainer(newWidth, newHeight);
		}
		<%--//console.log(opts.imageSrc);--%>
		imgPreloader.src = opts.imageSrc;
	}

	$.fn.wjImageViewer.end = function(){
		$('#wjImageViewer').hide();
		$('#overlay').fadeOut();
		$('select, object, embed').show();
	};

	$.fn.wjImageViewer.resizeImageContainer = function(imgWidth, imgHeight){
		var widthNew = (imgWidth  + (opts.borderSize * 2));
		var heightNew = (imgHeight  + (opts.borderSize * 2));

		$('#outerImageContainer').css({width: widthNew,height: heightNew}).show('fast',function(){
			$.fn.wjImageViewer.showImage();
		});
	};

	$.fn.wjImageViewer.showImage = function(){
		$('#loading').hide();
		$('#wjImageViewerImage').fadeIn("fast");
	};

	$.fn.wjImageViewer.defaults = {
		fileLoadingImage : '/components/_common/images/indicator.gif',
		overlayOpacity : 0.6,
		borderSize : 10,
		imageSrc : ''
	};
})(jQuery);
<%
}
%>

    $(document).ready(function()
    {
        try {
            if ("<iwcm:text key="common.pageFunctions.linkWillOpenInNewWindow"/>"!="")
            {
                //console.log("Doplnam target blank");
                $('a[onclick*="return openTargetBlank"]').each(function()
                {
                    var href = $(this).attr("href");

                    //console.log(href);

                    if (href != null) <%--//JKA tam tuto vynimku nechcel && href.indexOf("/files/")==-1 && href.indexOf("/images/")==-1)--%>
                    {
                        var actualTitle = $(this).attr("title");
                        if (actualTitle!=null && actualTitle!="") $(this).attr("title", actualTitle+" (<iwcm:text key="common.pageFunctions.linkWillOpenInNewWindow"/>)");
                        else  $(this).attr("title", "<iwcm:text key="common.pageFunctions.linkWillOpenInNewWindow"/>");
                    }
                });
            }
        }
        catch (e) {}
    });

var checkFormLoaded = true;
<%
pageContext.include(WriteTag.getCustomPage("/components/form/check_form_impl.jsp", request));
%>

<%--// Avoid `console` errors in browsers that lack a console.--%>
(function() {
    var method;
    var noop = function () {};
    var methods = [
        'assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error',
        'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log',
        'markTimeline', 'profile', 'profileEnd', 'table', 'time', 'timeEnd',
        'timeline', 'timelineEnd', 'timeStamp', 'trace', 'warn'
    ];
    var length = methods.length;
    var console = (window.console = window.console || {});

    while (length--) {
        method = methods[length];

        <%--// Only stub undefined methods.--%>
        if (!console[method]) {
            console[method] = noop;
        }
    }
}());

<c:forEach items="${pageFunctionsInclude}" var="include">
	<jsp:include page="${include}" flush="true" />
</c:forEach>
