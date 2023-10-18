<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page  %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />

<title>:: Web JET admin ::</title>
<%
	response.setHeader("Pragma","No-Cache");
	response.setDateHeader("Expires",0);
	response.setHeader("Cache-Control","no-Cache");
%>
<meta http-equiv="Content-Type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">

<link rel="stylesheet" type="text/css" media="screen" href="<%=request.getContextPath()%>/admin/skins/webjet6/css/webjet6.css" />

<style>
	<jsp:include page="/admin/css/perms-css.jsp"/>
</style>

<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/jquery/jScrollPane.js"></script>
<script type="text/javascript">
<!--

	if (window.name && window.name=="componentIframe"){
		document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/components/iframe.css'>");
	}else{
		document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/admin/css/style.css'>");
	}

	
	var constHeight;
	var setContainerHeight;
	
	$(document).ready(function(){

		$('ul.submenu li a').live('click', function(){
			$(".submenu li a").removeClass('actual');
			$(this).addClass('actual');
		});
	
		$("#menu-list1 ul li a:not('#menu-list1 ul li ul.submenu li a')").live('click', function(){
			menuActive(this);
		});		

		var isResizing;
		
		setContainerHeight = function()
		{			
			if (!isResizing) {
				isResizing = true;
				$w = $(window);
				$c = $('#menu-list1');
				var menuLeft = $('div.menuLeft').height();
				
				if(menuLeft > ($w.height()-90)){
					
					$('body .jspContainer').css({'height': ($w.height()-90) + 'px', 'width': '229px', 'overflow':'hidden'});
					$c.css({'height': ($w.height()-90) + 'px !important', 'width': 'auto'});

				}else{
					$('body .jspContainer').css({'height': (menuLeft) + 'px', 'width': '229px', 'overflow':'hidden'});
					$c.css({'height': (menuLeft) + 'px !important', 'width': 'auto', 'overflow':'hidden'});
				}

				$c.jScrollPane({showArrows:true});

				isResizing = false;
			}
		}
		
		$(window).resize(function(){
			setContainerHeight();
		});
		
		setContainerHeight();
		setContainerHeight();
	});

	function menuActive(el)
	{
		try
		{
			submenu = $(el).parent().find('.submenu');
			$('.menu-list li').removeClass('actual');
			$(el).parent().addClass('actual');
			$('.submenu').hide();
			$(el).parent().find('.submenu').show();
			$(el).parent().find('.submenu li:first a').addClass('actual');// oznaci prve menu v zalozke
			isResizing = false;
			setContainerHeight();
		} catch (e) {}
	}

-->
</script>

</head>

<body id="leftFrameBody">