function printPage()
{
   var options = "menubar=yes,toolbar=yes,scrollbars=yes,resizable=yes,width=630,height=460;"
   url = top.location.href;
   if (url.indexOf("#")>0) url = url.substring(0, url.indexOf("#"));
   if (url.indexOf("?")>0) url = url + "&print=yes";
   else url = url + "?print=yes";
   printWindow=window.open(url,"_blank",options);
}

function printPageEn()
{
   var options = "menubar=yes,toolbar=yes,scrollbars=yes,resizable=yes,width=630,height=460;"
   url = top.location.href;
   if (url.indexOf("#")>0) url = url.substring(0, url.indexOf("#"));
   if (url.indexOf("?")>0) url = url + "&print=yes&eng=yes";
   else url = url + "?print=yes&eng=yes";
   printWindow=window.open(url,"_blank",options);
}

function popup(url, width, height)
{
	  var options = "toolbar=no,scrollbars=yes,resizable=yes,width="+width+",height="+height+";"
	  popupWindow=window.open(url,"_blank",options);
}

function wjPopup(url, width, height)
{
	  var options = "toolbar=no,scrollbars=yes,resizable=yes,width="+width+",height="+height+";"
	  popupWindow=window.open(url,"_blank",options);
}

$(document).ready(function(){ 

  	try
  	{
  		$("#menu").cloudMenu({
  			liText: "viac",
  			liClass: 'viac last'
  		});
  	} catch (e) {
  		console.log(e);
  	}

	$("#menu li").not("#more").hover(function(){
		$(this).toggleClass("active");
	});  

	$("#menu > li").not("#more").first().addClass("first");
	$("#menu > li").not("#more").last().addClass("last");
	$("#menu li ul li:first-child").not("#more").addClass("first");
	$("#menu li ul li:last-child").not("#more").addClass("last");
	$("#menu li ul li ul li:first-child").not("#more").addClass("first");
	$("#menu li ul li ul li:last-child").not("#more").addClass("last");   	
	$("#footer div.footBox").not("#more").first().addClass("first");
	
	if($('#menu').hasClass('menu02c')){
		$("#menu.menu02c li").not("#more").append("<span class='arrow'></span>");
	};
  
	if($('#menu').hasClass('menu03c')){
		$("#menu.menu03c li").not("#more").append("<span class='arrow'></span>");
	};

	$("#menu li").not("#more").find(" > ul").append("<span class='arrow'></span>");
	$("#menu li").not("#more").find("ul a").append("<span class='square'></span>");

	$("#menu li ul li").find("ul").parent().addClass("subSub");      
	$("#menu li ul li.subSub").append("<span class='arrowR'></span>");

	$("#menu").before("<a href='javascript:void(0);' id='mobMenu'>Menu</a>");
	$("#mobMenu").click(function () {
		$("#menu").toggle();
	});

  $("table tr:odd").addClass("odd");
	$("table th:first-child").addClass("first");
	$("table th:last-child").addClass("last");
	$("table td:first-child").addClass("first");
	$("table td:last-child").addClass("last");
	$("table tr").hover(function(){
    $(this).toggleClass("hover");
  });
  
  /*
  $("div.products div.itemImageDiv img").each(function(){
    var itemDivImg = $("div.products div.itemImageDiv").height();
    var itemImg = $(this).height();
    var margin = (itemDivImg - itemImg) / 2; 
    $(this).css("margin-top", margin);
  });
  */

	$("ul.thumbs.thumbs").after("<span class='cleaner'></span>");
	
	// News Components 
	if ($("div.newsBoxes1").length > 0) {
		$("div.newsBoxes1 div:first-child").addClass("first");
	}

	if ($("ul.newsBox02").length > 0) {
		$("ul.newsBox02 li:first-child").addClass("first");  
	}

	if ( $(window).width() > 767) {
		if ($("div.newsBox06").length > 0) 
    {
      try {
  			$("div.newsBox06 div.boxes").cycle({ 
  				fx: 'scrollHorz',
  				timeout:  5000,
  				pagerEvent: 'click',
  				fastOnEvent: false,
  				next: '.nextBtn',
  				prev: '.prevBtn',
  				pager: '.pager'
  			}); 
			} catch (e) {}
		}
	}

	// hadze undefined
	if ( $(window).width() > 767) {
		if ($("div.slider-wrapper").length > 0) { 
			$("div.slider-wrapper").each(function()
      {
			   try {
  			   var effect = $(this).attr("data-effect"),
  			   		showControlNav = $(this).find("img").length > 1;

    			 $(this).nivoSlider({
    				effect: effect,
    				slices: 15,
    				boxCols: 8,
    				boxRows: 4,
    				animSpeed: 500,
    				pauseTime: 3000,
    				startSlide: 0,
    				directionNav: showControlNav,
    				controlNav: true,
    				controlNavThumbs: true,
    				pauseOnHover: true,
    				manualAdvance: false
    			});
  			} catch (e) {}
			
			});
		}
	}	

	$("form input[type=submit]").addClass("btn");

	setTimeout(function(){	
		$("div.newsSlider04 a.nivo-imageLink").each(function() {	
			var title2 = $(this).find("img").attr("title");
		});

	}, 200); 

	setTimeout(function(){	
		$("div.newsSlider04 div.nivo-controlNav img").each(function() {
			var title = $(this).attr("alt");
			$(this).after("<span class='bg'></span><span>" + title + "</span>")
			$(this).hide();      
		});

	}, 200); 

	if($.browser.msie && parseInt($.browser.version, 10) == 7) {
		$(".clearfix, div.photoSwipe").each(function() {
			$(this).after("<span class='cleaner'></span>");
		});   
	} 

	// hover pre mobilne zariadenia
	var isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry/i.test(navigator.userAgent);
	if (isMobile) {
		$("#menu a").hover(function(){
			return false;
		});

		$("#menu a").click(function () {
			var hoverClass = "active";
			var el = $(this);
			var ul = el.next("ul");
			
			if (ul.length > 0) {
				var isVIsible = $(this).parent("li").hasClass(hoverClass);
				console.log($(this).parent("li"))
				if (!isVIsible) {
					$("#menu li."+hoverClass).removeClass(hoverClass);
					$(this).parents("li").addClass(hoverClass);
				} else {
					return true;
				}

				return false;
			}
			return true;
		});
	}
	
	setTimeout(function(){
	if ($("div#inlineEditorToolbarTop").length > 0) {    
    $("body").addClass("test");  
  }
  }, 1000);
  	
	/**
	if(window.location.href.indexOf(".template.webjet.eu") != -1 && self==top){
		$.ajax({
		  	url: "/components/webjet_eu/template-redirect.jsp"
		}).done(function(url) {
			window.location.href = url;
		});
	}
	/**/
	
	///koniec ready
});

// console log - pokial objekt console neexsituje, tak si ho vytvorime
if (typeof console === "undefined" || typeof console.log === "undefined") {
 console = {};
 console.log = function() {};
}