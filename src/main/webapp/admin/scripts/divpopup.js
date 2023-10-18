var ox=100;
var oy=100;
var leftOffset=0;
var topOffset=10;
var linked;

$(document).mousemove(function(e){ 
	ox = e.pageX;
	oy = e.pageY;
});


function setPopupDivHeight(height)
{
	var iframeEl = document.getElementById("popupIframeId");	
	if (iframeEl != null)
	{
		iframeEl.height = height;		
	}	
}


function popupDIVLong(url)
{
	var el = document.getElementById("divPopUp");
	el.className = "divPopUp divPopUpLong";
	setPopupDivHeight(275);
	popupDIVImpl(url);
}

function popupDIV(url)
{
	var el = document.getElementById("divPopUp");
	el.className = "divPopUp";
	setPopupDivHeight(125);
	popupDIVImpl(url);
}

function popupDIVImpl(url)
{
    popupIframe.location.href = url;

    if (isNaN(oy)) 
    {
       ox = 340;
       oy = 95;
    }

    var el;
    if (document.all)
    {
      el = document.all['divPopUp'];      
    }
    else
    {
      el = document.getElementById("divPopUp");
    }

    el.style.visibility='visible';
    var pom = ox+leftOffset;
    if (pom < 0) pom = 3;
    el.style.top=oy+topOffset+"px";
    el.style.left=pom+"px";
    
    //window.alert("top="+oy+" left="+ox); 
}

function popupHide()
{
   var el;
   if (document.all)
   {
     el = document.all['divPopUp'];      
   }
   else
   {
     el = document.getElementById("divPopUp");
   }
   
   if (el)
   {
      el.style.visibility='hidden';
      popupIframe.location.href = "/admin/divpopup-blank.jsp";
   }
   
   el = document.getElementById("nodeContextMenu");
   if (el != null)
   {
      el.style.visibility='hidden';
   }
   el = document.getElementById("rootNodeContextMenu");
   if (el != null)
   {
      el.style.visibility='hidden';
   }
   el = document.getElementById("pageContextMenu");
   if (el != null)
   {
      el.style.visibility='hidden';
   }
}

function popupMenu( menuDiv , _linked)
{
    linked = _linked;
    if (isNaN(oy)) {
       ox = 340;
       oy = 95;
    }

    document.all[menuDiv].style.visibility='visible';
    var pom = ox+leftOffset;
    if (pom < 0) pom = 3;
    document.all[menuDiv].style.posTop=oy+topOffset;
    document.all[menuDiv].style.posLeft=pom;
}

function popupMenuHide( menu )
{
   if (document.all[menu])
   {
      document.all[menu].style.visibility='hidden';
	ox = ox - leftOffset;
	oy = oy - topOffset;
   }
}

function getLinkedNode() 
{
	return (linked);
}

document.onclick = popupHide;