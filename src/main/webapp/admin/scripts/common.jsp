<%@ page pageEncoding="utf-8" contentType="text/javascript" %><%@page import="org.json.JSONArray"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.SetCharacterEncodingFilter"%><%@page import="sk.iway.iwcm.Tools"%><%@page import="sk.iway.iwcm.i18n.Prop"%><%@page import="sk.iway.iwcm.tags.WriteTag"%><%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
	sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/a-common.js", null, request, response);
%><%@page import="java.util.Arrays"%>
    <%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" />

<%
String include = WriteTag.getCustomPageAdmin("/admin/scripts/common.jsp", request);
if (include!=null)
{
	pageContext.include(include);
	return;
}

Prop prop = Prop.getInstance(request);

JSONArray adminLoaderBannedUrls = new JSONArray(Arrays.asList(Tools.getTokens(Constants.getString("adminLoaderBannedUrls"), "|")));
request.setAttribute("adminLoaderBannedUrls", adminLoaderBannedUrls);
%>
var helpLink = "";
window.bootstrapVersion = "<%=Constants.getString("bootstrapVersion")%>";

/**
@deprecated
*/
function popup(url, width, height)
{
    console.log('Deprecated, use WJ.popup(url, width, height)');
    return WJ.popup(url, width, height);
}

/**
@deprecated
*/
function encodeValue(value)
{
    console.log('Deprecated, use WJ.encodeValue(value)');
    return WJ.encodeValue(value);
}

/**
@deprecated
*/
function unencodeValue(value)
{
    console.log('Deprecated, use WJ.unencodeValue(value)');
    return WJ.unencodeValue(value);
}

/**
@deprecated
*/
function popupFromDialog(url, width, height) {
    console.log('Deprecated, use WJ.popupFromDialog(url, width, height)');
    return WJ.popupFromDialog(url, width, height);
}

/**
@deprecated
*/
function confirmRestart()
{
    console.log('Deprecated, use WJ.confirmRestart()');
    return WJ.confirmRestart();
}

/**
@deprecated
*/
function wjConfirmDelete(text)
{
    console.log('Deprecated, use WJ.confirmDelete(text)');
    return WJ.confirmDelete(text);
}

/**
@deprecated
*/
function fixFileName(fileName)
{
    console.log('Deprecated, use WJ.fixFileName(fileName)');
    return WJ.fixFileName(fileName);
}

/**
@deprecated
*/
function internationalToEnglish(text)
{
    console.log('Deprecated, use WJ.internationalToEnglish(text)');
    return WJ.internationalToEnglish(text);
}

/**
@deprecated
*/
function removeChars(text)
{
    console.log('Deprecated, use WJ.removeChars(text)');
    return WJ.removeChars(text);
}

/**
@deprecated
*/
function removeSpojky(text)
{
    console.log('Deprecated, use WJ.removeSpojky(text)');
    return WJ.removeSpojky(text);
}

//otvorenie dialogoveho okna IE/Mozilla - based on FCKDialog
var WJDialog = new Object() ;
var WJDialogArguments = null;

// This method opens a dialog window using the standard dialog template.
WJDialog.OpenDialog = function( dialogName, dialogTitle, dialogPage, width, height, customValue, parentWindow )
{
	// Setup the dialog info.
	var oDialogInfo = new Object() ;
	oDialogInfo.Title = dialogTitle ;
	oDialogInfo.Page = dialogPage ;
	oDialogInfo.Editor = window ;
	oDialogInfo.CustomValue = customValue ;		// Optional

	//window.alert("WJDialog");

	window.FCKLang = new Object();
	window.FCKConfig = new Object();
	//link dialog setup
	window.FCKConfig.LinkDlgHideAdvanced = true;

	var sUrl = '<iwcm:cp/>/admin/dialogframe.jsp' ;
	this.Show( oDialogInfo, dialogName, sUrl, width, height, parentWindow ) ;
}

WJDialog.Show = function( dialogInfo, dialogName, pageUrl, dialogWidth, dialogHeight, parentWindow )
{
	if (navigator.userAgent.indexOf("MSIE")!=-1)
	{
	   if ( !parentWindow )
	      parentWindow = window ;

   		parentWindow.showModalDialog( pageUrl, dialogInfo, "dialogWidth:" + dialogWidth + "px;dialogHeight:" + dialogHeight + "px;help:no;scroll:no;status:no") ;
   		return;
	}

	var iTop  = (screen.height - dialogHeight) / 2 ;
	var iLeft = (screen.width  - dialogWidth)  / 2 ;

	if (iLeft < 0) iLeft = 0;
	if (iTop < 0) iTop = 0;

	if (navigator.userAgent.indexOf("Firefox/")!=-1)
	{
		dialogHeight = dialogHeight + 62;
	}
	else if (navigator.userAgent.indexOf("Trident/")!=-1)
	{
		dialogHeight = dialogHeight + 35;
	}

	var sOption  = "location=no,menubar=no,resizable=no,toolbar=no,dependent=yes,dialog=yes,minimizable=no,modal=yes,alwaysRaised=yes" +
		",width="  + dialogWidth +
		",height=" + dialogHeight +
		",top="  + iTop +
		",left=" + iLeft ;

	if ( !parentWindow )
		parentWindow = window ;

	var oWindow = parentWindow.open( pageUrl, 'FCKEditorDialog_' + dialogName, sOption, true ) ;
	//window.alert("iLeft="+iLeft+" iTop="+iTop);

	if (navigator.userAgent.indexOf("Chrome")==-1)
	{
		//ked tu bolo toto, Chrome zle pozicioval okno
		oWindow.moveTo( iLeft, iTop ) ;
		if (dialogWidth>0 && dialogHeight>0) oWindow.resizeTo( dialogWidth, dialogHeight ) ;
	}

	oWindow.focus() ;
	oWindow.dialogArguments = dialogInfo;

	//oWindow.location.href = pageUrl ;
	WJDialogArguments = dialogInfo;

	// On some Gecko browsers (probably over slow connections) the
	// "dialogArguments" are not set to the target window so we must
	// put it in the opener window so it can be used by the target one.
	parentWindow.FCKLastDialogInfo = dialogInfo ;

	this.Window = oWindow ;

	// Try/Catch must be used to avoit an error when using a frameset
	// on a different domain:
	// "Permission denied to get property Window.releaseEvents".
	try
	{
		/*parentWindow.captureEvents( Event.CLICK | Event.MOUSEDOWN | Event.MOUSEUP | Event.FOCUS ) ;
		parentWindow.top.addEventListener( 'mousedown', this.CheckFocus, true ) ;
		parentWindow.top.addEventListener( 'mouseup', this.CheckFocus, true ) ;
		parentWindow.top.addEventListener( 'click', this.CheckFocus, true ) ;
		parentWindow.top.addEventListener( 'focus', this.CheckFocus, true ) ;*/

		window.captureEvents( Event.CLICK | Event.MOUSEDOWN | Event.MOUSEUP | Event.FOCUS ) ;
		window.addEventListener( 'mousedown', this.CheckFocus, true ) ;
		window.addEventListener( 'mouseup', this.CheckFocus, true ) ;
		window.addEventListener( 'click', this.CheckFocus, true ) ;
		window.addEventListener( 'focus', this.CheckFocus, true ) ;
	}
	catch (e)
	{}
}

WJDialog.CheckFocus = function()
{
	//WJDialog.Window.status = "check focus: " + new Date();

	// It is strange, but we have to check the WJDialog existence to avoid a
	// random error: "WJDialog is not defined".
	if ( typeof( WJDialog ) != "object" )
		return ;

	if ( WJDialog.Window && !WJDialog.Window.closed )
	{
	   try
	   {
			//WJDialog.Window.focus();
			WJDialog.Window.document.getElementById('frmMain').contentWindow.focus();
		}
		catch (e)
		{}
		//WJDialog.Window.status = WJDialog.Window.location.href + " " + new Date();
		return false ;
	}
	else
	{
		//WJDialog.Window.status = "XXX: " + new Date();

		// Try/Catch must be used to avoit an error when using a frameset
		// on a different domain:
		// "Permission denied to get property Window.releaseEvents".
		try
		{
			window.top.releaseEvents(Event.CLICK | Event.MOUSEDOWN | Event.MOUSEUP | Event.FOCUS) ;
			window.top.parent.removeEventListener( 'onmousedown', FCKDialog.CheckFocus, true ) ;
			window.top.parent.removeEventListener( 'mouseup', FCKDialog.CheckFocus, true ) ;
			window.top.parent.removeEventListener( 'click', FCKDialog.CheckFocus, true ) ;
			window.top.parent.removeEventListener( 'onfocus', FCKDialog.CheckFocus, true ) ;
		}
		catch (e)
		{}
	}
}

function openElFinderDialogWindow(form, elementName, requestedImageDir, volume="all")
{
	var url = '/admin/elFinder/dialog.jsp';

	if (form != null && elementName != null) {
		url = url + "?form=" + form;
		url = url + "&elementName=" + encodeURIComponent(elementName);
		url = url + "&volume=" + encodeURIComponent(volume);

        try {
            var link = null;
            if ("ckEditorDialog"==form)
			{
                var dialog = CKEDITOR.dialog.getCurrent();
                var tabNamePair = elementName.split(":");
                var element = dialog.getContentElement(tabNamePair[0], tabNamePair[1]);
                link = element.getValue();
			}
			else {
                link = document.forms[form].elements[elementName].value;
            }
            if (link != null && link!=""){
                url = url + "&link=" + encodeURIComponent(link);
            }else  if (requestedImageDir!=undefined && requestedImageDir!=null && requestedImageDir!="") url += '&link=' + requestedImageDir;
        } catch (e) { console.log(e); }
	}
	//window.alert(navigator.userAgent);
   WJDialog.OpenDialog( 'WJDialog_Image' , "Image", url, 800, 604);
}

function openImageDialogWindow(formName, fieldName, requestedImageDir)
{
	/*
	var url = '/admin/FCKeditor/editor/dialog/fck_image.html?setfield='+formName+'.elements["'+fieldName+'"]';
	if (requestedImageDir!=null && requestedImageDir!="") url += '&requestedImageDir='+requestedImageDir;
	WJDialog.OpenDialog( 'WJDialog_Image' , "Image", url, 620, 500 ) ;
	*/

	openElFinderDialogWindow(formName, fieldName, requestedImageDir, "images");
}

function openLinkDialogWindow(formName, fieldName, requestedImageDir, requestedFileDir)
{
	/*
	var url = '/admin/FCKeditor/editor/dialog/editor_link.jsp?setfield='+formName+'.elements["'+fieldName+'"]';
	if (requestedImageDir!=null) url += '&requestedImageDir='+requestedImageDir;
	if (requestedFileDir!=null) url += '&requestedFileDir='+requestedFileDir;

   WJDialog.OpenDialog( 'WJDialog_Link' , "Image", url, 620, 500 ) ;
   */

   openElFinderDialogWindow(formName, fieldName, null, "link");
}

function getPageElement(name)
{
   var el = null;
   if (document.getElementById)
   {
      el = document.getElementById(name);
   }
   return(el);
}
function writeHTML(elementName, html)
{
   var el = getPageElement(elementName);
   if (el != null)
   {
      el.innerHTML = html;
   }
}

function addParamToUrl(url, paramName, paramValue)
{
   if (url.indexOf("?")==-1) url = url+"?";
   else url = url + "&";
   url = url + paramName + "=" + paramValue;
   return(url);
}

var ComponentsPickers = function () {

    //console.log("ComponentsPickers");

    var handleDatePickers = function () {

        if (jQuery().datepicker) {
            $('.date-picker, .datepicker').datepicker({
                rtl: Metronic.isRTL(),
                orientation: "left",
                autoclose: true,
                format: "dd.mm.yyyy",
                language: "<%=Prop.getLngForJavascript(request)%>",
                keyboardNavigation: false,
                todayHighlight: true,
                forceParse: false,
                weekStart: 1
            });
            //$('body').removeClass("modal-open"); // fix bug when inline picker is used in modal
        }

        /* Workaround to restrict daterange past date select: http://stackoverflow.com/questions/11933173/how-to-restrict-the-selectable-date-ranges-in-bootstrap-datepicker */
    }

    var handleTimePickers = function () {

        if (jQuery().timepicker) {
            $('.timepicker-default').timepicker({
                autoclose: true,
                showSeconds: true,
                minuteStep: 1,
                defaultTime: false
            });

            $('.timepicker-no-seconds').timepicker({
                autoclose: true,
                minuteStep: 5,
                defaultTime: false
            });

            $('.timepicker-24').timepicker({
                autoclose: true,
                minuteStep: 5,
                showSeconds: false,
                showMeridian: false,
                defaultTime: false
            });

            // handle input group button click
            $('.timepicker').parent('.input-group').on('click', '.input-group-btn', function(e){
                e.preventDefault();
                $(this).parent('.input-group').find('.timepicker').timepicker('showWidget');
            });
        }
    }

    var handleDateRangePickers = function () {
        if (!jQuery().daterangepicker) {
            return;
        }

        $('#defaultrange').daterangepicker({
                opens: (Metronic.isRTL() ? 'left' : 'right'),
                format: 'MM/DD/YYYY',
                separator: ' to ',
                startDate: moment().subtract(29, 'days'),
                endDate: moment(),
                minDate: '01/01/2012',
                maxDate: '12/31/2018',
            },
            function (start, end) {
                $('#defaultrange input').val(start.format('MMMM D, YYYY') + ' - ' + end.format('MMMM D, YYYY'));
            }
        );

        $('#defaultrange_modal').daterangepicker({
                opens: (Metronic.isRTL() ? 'left' : 'right'),
                format: 'MM/DD/YYYY',
                separator: ' to ',
                startDate: moment().subtract(29, 'days'),
                endDate: moment(),
                minDate: '01/01/2012',
                maxDate: '12/31/2018',
            },
            function (start, end) {
                $('#defaultrange_modal input').val(start.format('MMMM D, YYYY') + ' - ' + end.format('MMMM D, YYYY'));
            }
        );

        // this is very important fix when daterangepicker is used in modal. in modal when daterange picker is opened and mouse clicked anywhere bootstrap modal removes the modal-open class from the body element.
        // so the below code will fix this issue.
        $('#defaultrange_modal').on('click', function(){
            if ($('#daterangepicker_modal').is(":visible") && $('body').hasClass("modal-open") == false) {
                $('body').addClass("modal-open");
            }
        });

        $('#reportrange').daterangepicker({
                opens: (Metronic.isRTL() ? 'left' : 'right'),
                startDate: moment().subtract(29, 'days'),
                endDate: moment(),
                minDate: '01/01/2012',
                maxDate: '12/31/2014',
                dateLimit: {
                    days: 60
                },
                showDropdowns: true,
                showWeekNumbers: true,
                timePicker: false,
                timePickerIncrement: 1,
                timePicker12Hour: true,
                ranges: {
                    'Today': [moment(), moment()],
                    'Yesterday': [moment().subtract(1, 'days'), moment().subtract(1, 'days')],
                    'Last 7 Days': [moment().subtract(6, 'days'), moment()],
                    'Last 30 Days': [moment().subtract(29, 'days'), moment()],
                    'This Month': [moment().startOf('month'), moment().endOf('month')],
                    'Last Month': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')]
                },
                buttonClasses: ['btn'],
                applyClass: 'green',
                cancelClass: 'default',
                format: 'MM/DD/YYYY',
                separator: ' to ',
                locale: {
                    applyLabel: 'Apply',
                    fromLabel: 'From',
                    toLabel: 'To',
                    customRangeLabel: 'Custom Range',
                    daysOfWeek: ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'],
                    monthNames: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'],
                    firstDay: 1
                }
            },
            function (start, end) {
                $('#reportrange span').html(start.format('MMMM D, YYYY') + ' - ' + end.format('MMMM D, YYYY'));
            }
        );
        //Set the initial state of the picker label
        $('#reportrange span').html(moment().subtract(29, 'days').format('MMMM D, YYYY') + ' - ' + moment().format('MMMM D, YYYY'));
    }

    var handleDatetimePicker = function () {

        if (!jQuery().datetimepicker) {
            return;
        }

        $(".form_datetime").datetimepicker({
            autoclose: true,
            isRTL: Metronic.isRTL(),
            format: "dd MM yyyy - hh:ii",
            pickerPosition: (Metronic.isRTL() ? "bottom-right" : "bottom-left")
        });

        $(".form_advance_datetime").datetimepicker({
            isRTL: Metronic.isRTL(),
            format: "dd MM yyyy - hh:ii",
            autoclose: true,
            todayBtn: true,
            startDate: "2013-02-14 10:00",
            pickerPosition: (Metronic.isRTL() ? "bottom-right" : "bottom-left"),
            minuteStep: 10
        });

        $(".form_meridian_datetime").datetimepicker({
            isRTL: Metronic.isRTL(),
            format: "dd MM yyyy - HH:ii P",
            showMeridian: true,
            autoclose: true,
            pickerPosition: (Metronic.isRTL() ? "bottom-right" : "bottom-left"),
            todayBtn: true
        });

        $('body').removeClass("modal-open"); // fix bug when inline picker is used in modal
    }

    var handleClockfaceTimePickers = function () {

        if (!jQuery().clockface) {
            return;
        }

        $('.clockface_1').clockface();

        $('#clockface_2').clockface({
            format: 'HH:mm',
            trigger: 'manual'
        });

        $('#clockface_2_toggle').click(function (e) {
            e.stopPropagation();
            $('#clockface_2').clockface('toggle');
        });

        $('#clockface_2_modal').clockface({
            format: 'HH:mm',
            trigger: 'manual'
        });

        $('#clockface_2_modal_toggle').click(function (e) {
            e.stopPropagation();
            $('#clockface_2_modal').clockface('toggle');
        });

        $('.clockface_3').clockface({
            format: 'H:mm'
        }).clockface('show', '14:30');
    }

    var handleColorPicker = function ()
    {
        if (typeof $.minicolors == "undefined") {
            return;
        }

        $(".colorpicker-rgba").minicolors({
				changeDelay: 400,
				opacity: true,
				format: 'rgb'
		  });
    }

    return {
        //main function to initiate the module
        init: function () {
        	//console.log("INIT DATEPICKER");
            handleDatePickers();
            handleTimePickers();
            handleDatetimePicker();
            handleDateRangePickers();
            handleClockfaceTimePickers();
            handleColorPicker();
        }
    };

}();


//WebJET6 - prepinanie zaloziek v admin casti
function showHideTab(id)
{
	//nove taby
	//po vlozeni noveho jquery vyhadzovalo chybu
	//$("a[href=#tabMenu"+id+"]").click()

	//stare taby
	//window.alert(window.location.pathname);
	try
	{
		$.cookie("showHideTab", id); //jeeff: toto nefungovalo v IE7, { path: window.location.pathname })
	} catch (e) {}

	$('ul.tab_menu li').removeClass('open openFirst openLast');

	el = $("#tabLink" +id).parent();

	if ($(el).attr('class')!=undefined)
	{
		if($(el).attr('class').indexOf("first") != -1 && $(el).attr('class').indexOf("openOnlyFirst") )
		{
			el.addClass('openFirst');
		}
		else if($(el).attr('class').indexOf("last") != -1 && $(el).attr('class').indexOf("openOnlyFirst"))
		{
			el.addClass('openLast');
		}
	}
	else
	{
		el.addClass('open');
	}

	$("ul.tab_menu li a").each(function(i)
	{
		$(this).removeClass("activeTab");
	});

	$("#tabLink" +id).addClass("activeTab");

	$("div.toggle_content div").each(function(i){
		++i;
		$("div#tabMenu0, div#tabMenu" + i).hide();
	});

	$("div#tabMenu" + id).show();
}

//resize na inner velkost okna
function getInnerSize () {
	var x,y;
	if (self.innerHeight) // all except Explorer
	{
		x = self.innerWidth;
		y = self.innerHeight;
	}
	else if (document.documentElement && document.documentElement.clientHeight)
		// Explorer 6 Strict Mode
	{
		x = document.documentElement.clientWidth;
		y = document.documentElement.clientHeight;
	}
	else if (document.body) // other Explorers
	{
		x = document.body.clientWidth;
		y = document.body.clientHeight;
	}

	return [x,y];
}

function resizeToInner (w, h, x, y)
{
	try
	{
		var innerWidth = window.innerWidth;
		var innerHeight = window.innerHeight;

		var outerWidth = window.outerWidth;
		var outerHeight = window.outerHeight;

		var diffWidth = outerWidth - innerWidth;
		var diffHeight = outerHeight - innerHeight;

		var newWidth = w + diffWidth;
		var newHeight = h + diffHeight;

		//window.alert("required 2: "+w+"x"+h+" inner: "+innerWidth+"x"+innerHeight+" outer:"+outerWidth+"x"+outerHeight+" new:"+newWidth+"x"+newHeight);
        //window.alert("avail 3="+screen.availWidth+":"+screen.availHeight);

        if (screen.availWidth>200 && newWidth > screen.availWidth)
		{
		    newWidth = screen.availWidth;
		    w = newWidth;
        }
        if (screen.availHeight>200 && newHeight > screen.availHeight)
		{
		    newHeight = screen.availHeight;
		    h = newHeight;
        }
        //lebo ked su mega velke dialogy to haluzilo (napr. qa/admin_answer.jsp)
        if (newHeight > 1100) newHeight = 1100;
        if (h > 1100) h = 1100;

        newWidth = Math.floor(newWidth);
        newHeight = Math.floor(newHeight);

		if (innerWidth > 90 && outerWidth > 100 && innerHeight > 90 && outerHeight > 100 && newWidth < 1200 && newHeight <= 1100 && newWidth >= w && newHeight >= h)
		{
			//window.alert("resizing, newWidth="+newWidth+" newHeight="+newHeight);
			window.resizeTo(newWidth, newHeight);
			return;
		}


	} catch (e) { console.log(e); window.alert(e); }

	//window.alert("avail 1="+screen.availWidth+":"+screen.availHeight);
	// make sure we have a final x/y value
	// pick one or the other windows value, not both
	if (x==undefined) x = window.screenLeft || window.screenX;
	if (y==undefined) y = window.screenTop || window.screenY;
	// for now, move the window to the top left
	// then resize to the maximum viewable dimension possible
	window.moveTo(0,0);
	window.resizeTo(screen.availWidth,screen.availHeight);
	// now that we have set the browser to it's biggest possible size
	// get the inner dimensions.  the offset is the difference.

	//window.alert(navigator.userAgent);
	if (navigator.userAgent.indexOf("WebKit")!=-1)
	{
		setTimeout(resizeToInnerAsync, 800, w, h, x, y);
		//setTimeout(resizeToInnerAsync, 10000, w, h, x, y);
	}
	else resizeToInnerAsync(w, h, x, y);
}

function resizeToInnerAsync (w, h, x, y)
{

	var inner = getInnerSize();

	//window.alert(inner[0]+"x"+inner[1]+" "+screen.availWidth+"x"+screen.availHeight);

	if (inner[0]==undefined)
	{
		window.resizeTo(w, h);
	}
	else
	{
		var ox = screen.availWidth-inner[0];
		var oy = screen.availHeight-inner[1];
		// now that we have an offset value, size the browser
		// and position it
		var newWidth = w+ox+2;
		var newHeight = h+oy+2;
		window.resizeTo(newWidth, newHeight);
		window.moveTo(x,y);

		//window.alert("ox="+ox+" oy="+oy+" i0="+inner[0]+" i1="+inner[1]+" nw="+newWidth+" nh="+newHeight+" w="+w+" h="+h+" sw="+screen.availWidth+" sh="+screen.availHeight);
	}
}

function resizeDialog(width, height)
{
    /*
    //toto potrebujeme napr. na okno editacie banneru ktore sa inak neresizne spravne
	var ua = navigator.userAgent.toLowerCase();
	if (ua.indexOf("phone")!=-1 || ua.indexOf("ipad")!=-1 || ua.indexOf("android")!=-1)
	{
		return;
	}

	resizeToInner(width, height);

	//nastav vysku centralneho riadku (len pre FF)
	var centralRow = document.getElementById("dialogCentralRow");
	if (centralRow != null && navigator.userAgent.indexOf("Gecko")!=-1)
	{
		centralRow.style.height = (height - 125) + "px";
	}
    */
}

function resizeDialogCK(width, height)
{
	if (typeof CKEDITOR != 'undefined' && CKEDITOR.dialog.getCurrent() != null) {
		CKEDITOR.dialog.getCurrent().resize(width, height);
	}
	else {
		console.warn("Var CKEDITOR is undefined or CKEDITOR.dialog.getCurrent() is null");
	}
}

function resizeDialogNew(width, height)
{
	var ua = navigator.userAgent.toLowerCase();
	if (ua.indexOf("phone")!=-1 || ua.indexOf("ipad")!=-1 || ua.indexOf("android")!=-1)
	{
		return;
	}
	//alert(width+", "+ height);
	resizeToInner(width, height);
	//alert((height - 125))
	//nastav vysku centralneho riadku (len pre FF)
	//LON - ide o resize dialogoveho okna, teraz uz tato podmienka nieje potrebna,
	/*var centralRow = document.getElementById("dialogCentralRow");
	if (centralRow != null && navigator.userAgent.indexOf("Gecko")!=-1)
	{
		centralRow.style.height = (height - 125) + "px";
	}*/
}


function showHideRow(name, show)
{
   if (document.getElementById)
   {
      el = document.getElementById(name);
      if (el!=null)
      {
         if (show)
         {
            try { el.style.display = "table-row"; } catch (e) { el.style.display = "block"; }
         }
         else
         {
            el.style.display = "none";
         }
      }
   }
}

/* UPRAVA milti selectu na presuvanie vlavo a vpravo */
function perexGroupSearchChange(field, originalName, name)
{
	if (name == undefined || name == null) name = "disabledItems";
	if (originalName == undefined || originalName == null) originalName = "perexGroup";

	f = field.form;
	options = f.elements[originalName].options;

	//vymaz cely option
	clearAllValues(f.elements[name+"Left"]);

	var searchText = field.value.toLowerCase();
	if (options)
	{
		var leftCounter = 0;
		var rightCounter = 0;

		var maxItemsInList = 1000;
		if (options.length>1000) maxItemsInList = 200;
		if ("*"==searchText) maxItemsInList = options.length;

		var addEmpty = false;
		if (""==searchText && options.length < 1000) addEmpty = true;

		var itemsCounter = 0;
		for (var i=0; i < options.length; i++)
		{
			if (itemsCounter < maxItemsInList && options[i].selected == false && ("*"==searchText || addEmpty || (searchText.length > 0 && options[i].text.toLowerCase().indexOf(searchText)!=-1)) ||
					(""==searchText && ( options[i].text.indexOf("+")==0 || options[i].text.indexOf("#")==0 )))
			{
				var option = new Option();
				option.text = options[i].text;
				option.value = options[i].value;
				option.selected = options[i].defaultselected;
				option.defaultSelected = options[i].defaultselected;
				f.elements[name+"Left"].options[leftCounter++] = option;
				itemsCounter++;
			}
		}
	}
}

function clearAllValues(field)
{
	var options = field.options;
	while (options.length > 0)
	{
		options[0] = null;
	}
}

function initializeDisabledItems(f, originalName, name)
{
	try
	{
		if (name == undefined || name == null) name = "disabledItems";
		if (originalName == undefined || originalName == null) originalName = "perexGroup";

		//window.alert("name="+name+" original="+originalName);
		var options = f.elements[originalName].options;
		//console.log(f, originalName, options);

		if (options)
		{
			clearAllValues(f.elements[name+"Left"]);
			clearAllValues(f.elements[name+"Right"]);

			var leftCounter = 0;
			var rightCounter = 0;
			for (var i=0; i < options.length; i++)
			{
				var option = new Option();
				option.text = options[i].text;
				option.value = options[i].value;
				option.selected = options[i].defaultselected;
				option.defaultSelected = options[i].defaultselected;
                //console.log(options[i].title);
                if (typeof options[i].title != 'undefined') {
                    option.title = options[i].title;
                }
				//window.alert("lef="+leftCounter+" right="+rightCounter+" opt="+option.text);
				//window.alert("left="+f.elements[name+"Left"].options.length);
				if (options[i].selected == false)
				{
					if (options.length < 1000 || options[i].text.indexOf("+")==0 || options[i].text.indexOf("#")==0)
					{
						f.elements[name+"Left"].options[leftCounter++] = option;
					}
				}
				else
				{
					f.elements[name+"Right"].options[rightCounter++] = option;
				}
			}
		}
	}
	catch (e)
	{
		//window.alert(e);
		//console.log(e);
	}
}
function moveLeft(f, originalName, name)
{
	if (name == undefined || name == null) name = "disabledItems";
	if (originalName == undefined || originalName == null) originalName = "perexGroup";

	var failsafe = 0;
	while (f.elements[name+"Right"].selectedIndex>=0 && failsafe < 100)
	{
	   options = f.elements[name+"Right"].options;
		i = f.elements[name+"Right"].selectedIndex;
		if (i < 0) return;
		var option = new Option();
		option.text = options[i].text;
		option.value = options[i].value;
		option.selected = options[i].defaultselected;
		option.defaultSelected = options[i].defaultselected;
        //console.log(options[i].title);
        if (typeof options[i].title != 'undefined') {
            option.title = options[i].title;
        }
		f.elements[name+"Left"].options[f.elements[name+"Left"].options.length] = option;

		removeOptionMove(f.elements[name+"Right"]);
		setMainItem(f.elements[originalName], option.value, false);
		failsafe++;
	}
}
function moveRight(f, originalName, name)
{
	if (name == undefined || name == null) name = "disabledItems";
	if (originalName == undefined || originalName == null) originalName = "perexGroup";

	var failsafe = 0;
	while (f.elements[name+"Left"].selectedIndex>=0 && failsafe < 30)
	{
	   options = f.elements[name+"Left"].options;
		i = f.elements[name+"Left"].selectedIndex;
		if (i < 0) return;
		var option = new Option();
		option.text = options[i].text;
		option.value = options[i].value;
		option.selected = options[i].defaultselected;
		option.defaultSelected = options[i].defaultselected;
		//console.log(options[i].title);
        if (typeof options[i].title != 'undefined') {
            option.title = options[i].title;
        }
		f.elements[name+"Right"].options[f.elements[name+"Right"].options.length] = option;

		removeOptionMove(f.elements[name+"Left"]);
		setMainItem(f.elements[originalName], option.value, true);
		failsafe++;
	}
}
function removeOptionMove(select)
{
	var selected = select.selectedIndex;
	select.options[select.selectedIndex] = null;
}
function setMainItem(select, value, selected)
{
    if (select == null || typeof(select) == 'undefined') {
        console.log('common.jsp -> setMainItem: select is: ' + select);
        return;
    }

	options = select.options;
	for (var i=0; i < options.length; i++)
	{
		if (options[i].value == value)
		{
			options[i].selected = selected;
		}
	}
}
function m_click_help()
{
   try
	{
		if (parent.mainFrame && parent.mainFrame.helpLink)
		{
		   helpLink = parent.mainFrame.helpLink;
		}
	}
	catch(e)
	{
	}

	try
	{
		if (""==helpLink)
		{
			try
			{

			   var url = window.location.pathname;
				var start = url.indexOf("/components/");
				if (start != -1)
				{
					start += "/components".length;
					var end = url.indexOf("/", start+1)
					if (end != -1)
					{
						var cmpName = url.substring(start+1, end);
						//window.alert(cmpName);
						helpLink = "components/"+cmpName+".jsp&book=components";
					}
				}
			}
			catch(e)
			{
			}
		}

		showHelpWindow(helpLink);
	}
	catch (e) { showHelpWindow(""); }
}

function showHelpWindow(helpLink)
{
	var url = "/admin/help/index.jsp";
	if (helpLink != "")
	{
		url = url + "?link="+helpLink;
	}
   var options = "menubar=no,toolbar=no,scrollbars=yes,resizable=yes,width=880,height=540;"
   popWindow=window.open(url,"_blank",options);
}
/* UPRAVA MULTi selectu koniec */

//ak je nastavene UTF-8 enkoduje parameter URL cez encodeUriComponent, ak je windows-1250 nerobi nic
function wjEncodeUriComponent(value)
{
	<%
	if ("utf-8".equals(SetCharacterEncodingFilter.getEncoding())) out.println("	value = encodeURIComponent(value);");
	%>
	return value;
}

function redirect(url) {
	window.location.href = url;
}

//konverzia checkboxov na zoznam nieoc+nieco1+nieco2
function checkboxesToList(cb)
{
	var list = "";
	for (i=0; i < cb.length; i++)
	{
		//window.alert(cb[i].value+" ce="+cb[i].checked);
		if (cb[i].checked)
		{
			if (list=="") list = cb[i].value;
			else list = list+"+"+cb[i].value;
		}
	}
	return list;
}

//upravi hodnotu aby mohla byt vkladana do include komponenty
function sanitizeIncludeParam(param)
{
	param = param.replace(/"/gi, "\\\"");

	return param;
}

// escapuje string, ktory sa vklada do include.
// a vrati ho naformatovany presne ako v include ma byt, tzn.: ,paramName="param"
function addIncludeParameter(paramName,param){
    // ak by ste chceli z nejakeho dovodu NEescapovat parameter, nepouzite tuto fn, alebo vytvorte string ako var str = new String("Pozadovana hodnota"); - pretoze typeof str == "object";
    if(typeof param === 'string') {
		param = param.replace(/"/gi, "\\\"");
		param = param.replace('<',"&lt;");
		param = param.replace('</',"&lt;/");
        param = param.replace('>',"&gt;");
    }
    return ", " + paramName + "=\"" +	param + "\"";
}

function showHideElement(name, show)
{
	var element = $('#' + name);
	if (element.length > 0) {
		if (show)
         {
            element.show();
         }
         else
         {
            element.hide();
         }
	}
}

function datatableClearSaveParams(settings, data)
{
	try
  	{
  		//console.log("stateSaveParams");
  		//console.log(data);
    	data.search.search = "";
    	data.start = 0;
    	for (var i=0; i < data.columns.length; i++)
    	{
    		//console.log(data.columns[i].search);
    		data.columns[i].search.search = "";
    	}
  	}
  	catch (e)
  	{
  		console.log(e);
  	}

  	return data;
}

var datatable2SpringData = function(sSource, aoData, fnCallback) {

	var url = sSource+"/all";
	var columnName;
	var isAnyColumnSearch = false;
	var restParams = new Array();
	var searchColumnParams = new Array();
	var paramMap = {};


	// prechod vsetkymi vstupnymi parametrami od datatables
	for ( var i = 0; i < aoData.length; i++) {

		// extract name/value pairs into a simpler map for use later
		paramMap[aoData[i].name] = aoData[i].value;

		// zistenie nazvu stlpca (vsetky nasledujuce parametre sa viazu k nemu)
		if (aoData[i].name.includes('mDataProp_') ) {
			columnName = aoData[i].value;
		}

		// ak je to vyhladavacie policko naplnime prislusny parameter (id ignorujeme)
		if (aoData[i].name.includes('sSearch_') && columnName != "id") {

			if (!isAnyColumnSearch && aoData[i].value) {
				isAnyColumnSearch = true;
			}
			columnName = columnName.charAt(0).toUpperCase() + columnName.slice(1);
			searchColumnParams.push({"name" : "search" + columnName, "value" : aoData[i].value});
			//console.log("Search: '" + columnName + "' value: '" + aoData[i].value + "'");
		}
	}

	// zmena url a restovych parametrov ak sa ide vyhladavat
	if (isAnyColumnSearch) {
		url =  sSource+"/search/findByColumns";
		restParams = searchColumnParams;
	}

	console.log("Datatable url: '" + url + "' sSource: "+sSource);

	//page calculations
	var pageSize = paramMap.iDisplayLength;
	var start = paramMap.iDisplayStart;
	var pageNum = (start == 0) ? 0 : (start / pageSize);

	//chceme vsetky zaznamy
	if (pageSize == -1) pageSize = <%=Integer.MAX_VALUE%>;

	// extract sort information
	var sortCol = paramMap.iSortCol_0;
	var pageSort = paramMap['mDataProp_' + sortCol] + "," + paramMap.sSortDir_0;

	console.log("Datatable pageSort: '" + pageSort + "'");

	//create new json structure for parameters for REST request
	restParams.push({"name" : "size", "value" : pageSize});
	restParams.push({"name" : "page", "value" : pageNum });
	restParams.push({ "name" : "sort", "value" : pageSort });

	//finally, make the request
	$.ajax({
		"dataType" : 'json',
		"type" : "GET",
		"url" : url,
		"data" : restParams,
		"success" : function(data) {
			//console.log(data);
			var totalElements = data.totalElements;
			data = data.content;
			data.iTotalRecords = totalElements;
			data.iTotalDisplayRecords = totalElements;
			fnCallback(data);
		}
	});
};

var debugTimerStartTimestamp = 0;
var debugTimerTitle = "";
var debugTimerEnabled = false;
function dtStart(title)
{
	debugTimerTitle = title;
	debugTimerStartTimestamp = new Date().getTime();
}

function dtDiff(subtitle)
{
	if (debugTimerEnabled==false) return;

	var debugTimerDiff = new Date().getTime() - debugTimerStartTimestamp;
	console.log(debugTimerTitle+":"+subtitle+" "+debugTimerDiff);
}

function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}


var Loader = (function() {
    var el = null,
    	elClass = 'WJLoaderDiv',
        timeout = null,
        bannedUrls = ${adminLoaderBannedUrls},
        src = '/admin/skins/webjet8/assets/admin/layout/img/ajax-loading.svg';

	function initialization() {
		  var urlParams = window.location.search;
		  if (urlParams.indexOf("inlineEditorAdmin")!=-1) {
			  el = null;
			  return;
		  }

        el = $('.' + elClass);
        if (el.length == 0) {
           el = $('<div class="' + elClass + '"><img src="' + src + '" alt="<iwcm:text key="groupslist.page_history_loading" />"> <iwcm:text key="groupslist.page_history_loading" /></div>').appendTo('body').hide();
        }

		$( document ).ajaxSend(Loader.showLoader);
		$( document ).ajaxStop(Loader.hideLoader);
	}

    function isAllowedUrl(url) {
        var result = true;

        $.each(bannedUrls, function(i, v){
            if (url.indexOf(v) == 0) {
                result = false;
                return false;
            }
        });

        return result;
    }

	return {
		init: initialization,
		showLoader: function(event, jqXHR, ajaxOptions) {
            if (!isAllowedUrl(ajaxOptions.url)) {
                return;
            }

            clearTimeout(timeout);

            if (el!=null && !el.is(":visible")) {
                el.show();
            }
		},
		hideLoader: function() {
            timeout = setTimeout(function(){
                if (el != null && el.is(":visible")) {
                    el.hide();
                }
            });
		}
	}
})();

if (typeof $ != "undefined") {
	$(function(){
		Loader.init();
	});
}

function showGalleryTooltip(imgid, desc, e)
{
   try
   {
	   if (!e && window.event) e = window.event;

	   var ox=0;
	   var oy=0;

	   var IE = document.all?true:false;
	   if (!IE) document.captureEvents(Event.MOUSEMOVE);

	   if (IE)
	   {
	      if (document.documentElement && document.documentElement.scrollTop)
	      {
	         ox = event.clientX + document.documentElement.scrollLeft;
	         oy = event.clientY + document.documentElement.scrollTop;
	      }
	      else
	      {
	         ox = event.clientX + document.body.scrollLeft;
	         oy = event.clientY + document.body.scrollTop;
	      }
	   }
	   else
	   {
	      ox = e.pageX;
	      oy = e.pageY;
	   }

	   if (ox < 10){ox = 10}
	   if (oy < 10){oy = 10}

	   var text = "";
	   var imgEl = document.getElementById(imgid)
	   if (imgEl == null) return;
	   if (imgEl.alt)
	   {
	      imgEl.galText = imgEl.alt;
	      imgEl.alt = "";
	   }
	   text = imgEl.galText;
	   if (text == undefined || text == "undefined") text = "";

	   if (text=="" && desc=="") return;

	   if (document.getElementById||document.all)
	   {
	      lnk = "galleryTooltipLayerAuto";
	      var curLnk = (document.getElementById)? document.getElementById(lnk): document.all[lnk];

	      if (curLnk == null)
	      {
	    	  var galDiv = document.createElement('div');
	    	  galDiv.id = "galleryTooltipLayerAuto";
	    	  galDiv.style.cssText = "position:absolute; z-index:50; display: none; background-color: white; border: 1px solid black; color: black; padding: 2px; text-align: left; filter: Alpha( Opacity=70, FinishOpacity=0, Style=0); opacity:.7;";
	    	  galDiv.setAttribute("class", "galleryTooltipDiv");
	    	  var bodyEl = document.getElementsByTagName("body")[0];
	    	  bodyEl.appendChild(galDiv);
	    	  curLnk = galDiv;
	      }

	      if (curLnk != null)
	      {
	    	  //vypocitaj poziciu
		      curLnk.style.display = "block";
		      curLnk.style.top=(oy+10)+"px";
		      curLnk.style.left=(ox+10)+"px";
		      if (desc!="")
		      {
		         if (text=="")
		         {
		            text = desc;
		         }
		         else
		         {
		            text = text + "<br><br>"+desc;
		         }
		      }
		      curLnk.innerHTML = text;
	      }
	   }
   }
   catch (exception)
   {

   }
}

function hideGalleryTooltip()
{
   if (document.getElementById||document.all)
   {
      lnk = "galleryTooltipLayerAuto";
      var curLnk = (document.getElementById)? document.getElementById(lnk): document.all[lnk];
      if (curLnk != null)
      {
	      curLnk.style.display = "none";
      }
   }
}

function highlightGalleryTD(obj,on)
{
   if (document.getElementById||document.all)
   {
      if (on==1) obj.bgColor=highlightGalleryTDColor;
      else obj.bgColor="";
   }
}

function openPopupDialogFromLeftMenu(url)
{
	var options = "status=no,menubar=no,toolbar=no,scrollbars=yes,resizable=yes,width=650,height=550";
	var popupwindow = null;

	popupwindow=window.open(url, "", options);
	if (window.focus && popupwindow!=null)
	{
		popupwindow.focus();
	}
}

function openPopupDialogFromTopFrame(url)
{
	var options = "status=no,menubar=no,toolbar=no,scrollbars=yes,resizable=yes,width=650,height=550";
	popupwindow=window.parent.open(url, "", options);
	if (window.focus)
	{
		popupwindow.focus();
	}
}

function IeVersion() {
	//Set defaults
	var value = {
		IsIE: false,
		TrueVersion: 0,
		ActingVersion: 0,
		CompatibilityMode: false
	};

	//Try to find the Trident version number
	var trident = navigator.userAgent.match(/Trident\/(\d+)/);
	if (trident) {
		value.IsIE = true;
		//Convert from the Trident version number to the IE version number
		value.TrueVersion = parseInt(trident[1], 10) + 4;
	}

	//Try to find the MSIE number
	var msie = navigator.userAgent.match(/MSIE (\d+)/);
	if (msie) {
		value.IsIE = true;
		//Find the IE version number from the user agent string
		value.ActingVersion = parseInt(msie[1]);
	} else {
		//Must be IE 11 in "edge" mode
		value.ActingVersion = value.TrueVersion;
	}

	//If we have both a Trident and MSIE version number, see if they're different
	if (value.IsIE && value.TrueVersion > 0 && value.ActingVersion > 0) {
		//In compatibility mode if the trident number doesn't match up with the MSIE number
		value.CompatibilityMode = value.TrueVersion != value.ActingVersion;
	}
	return value;
}

    function escapeOutput(value) {
        return value.replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

var WJ = (function(){

    //WebJET - odstranenie diakritiky
    function internationalToEnglish(text, whitespaceToDash)
    {
        if (typeof  whitespaceToDash == 'undefined') {
            whitespaceToDash = true;
		}

        var Diacritic =   "áäčďéěíĺľňóôőöŕšťúůűüýřžÁÄČĎÉĚÍĹĽŇÓÔŐÖŔŠŤÚŮŰÜÝŘŽ ";
        var DiacRemoved = "aacdeeillnoooorstuuuuyrzAACDEEILLNOOOORSTUUUUYRZ";
        DiacRemoved += whitespaceToDash ? '-' : ' ';

        var ptext=""; // pomocná proměnná
        var i = 0;
        for(i=0; i < text.length; i++)
        {
            // projít zadaný text po znaku
            if (Diacritic.indexOf(text.charAt(i))!=-1) // pokud je znak v textu obsažen v řetezci Diacritic
                ptext+=DiacRemoved.charAt(Diacritic.indexOf(text.charAt(i))); // předat do pomocného řetězce znak z pole DiacRemoved
            else
                ptext+=text.charAt(i); // jinak předat původní znak
        }

        //console.log("international to english, text="+text+" ptext="+ptext);

        return ptext;
    }

    //WebJET - ponecha len safe znaky
    function removeChars(text)
    {
        var safeChars =   "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_.";

        var ptext="";
        for(i=0;i < text.length;i++)
        {
            if (safeChars.indexOf(text.charAt(i))==-1)
                ptext+="-";
            else
                ptext+=text.charAt(i);
        }

        //ponahradzuj
        ptext = ptext.replace(/---/gi, "-");
        ptext = ptext.replace(/--/gi, "-");
        ptext = ptext.replace(/___/gi, "_");
        ptext = ptext.replace(/__/gi, "_");
        ptext = ptext.replace(/_-_/gi, "-");
        ptext = ptext.replace(/-_/gi, "-");
        ptext = ptext.replace(/_-/gi, "-");
        ptext = ptext.replace(/-_-/gi, "-");

        ptext = ptext.replace(/--/gi, "-");
        ptext = ptext.replace(/--/gi, "-");
        ptext = ptext.replace(/--/gi, "-");
        ptext = ptext.replace(/--/gi, "-");
        ptext = ptext.replace(/__/gi, "_");
        ptext = ptext.replace(/__/gi, "_");
        ptext = ptext.replace(/__/gi, "_");
        ptext = ptext.replace(/__/gi, "_");

        return ptext;
    }

    function removeSpojky(text)
    {
        try
        {
            <%
            if (Constants.getBoolean("urlRemoveSpojky"))
            {
                String spojky[] = Tools.getTokens(Constants.getString("urlRemoveSpojkyList"), ",");
                for (String spojka : spojky)
                {
                    spojka = Tools.replace(spojka, ".", "\\.");
                    %>
            text = text.replace(/-<%=spojka%>-/gi, "-");
            text = text.replace(/-<%=spojka%>\//gi, "/");
            <%
				}
			}
			%>
        }
        catch (ex)
        {

        }
        return text;
    }

    //WebJET - odstranenie znakov z mena suboru pri uploade (kontrola existencie)
    function fixFileName(fileName)
    {
        fileName = fileName.replace(/&/gi, "");
        fileName = internationalToEnglish(fileName);
        fileName = removeChars(fileName);
        return fileName.toLowerCase();
    }

    function popup(url, width, height)
    {
        var options = "toolbar=no,scrollbars=yes,resizable=yes,width="+width+",height="+height+";"
        popupWindow=window.open(url,"_blank",options);
        //return(popupWindow);
    }

    function encodeValue(value)
    {
        value = escape(value);
        //value = value.replace(/aaa/g, "%20");
        return(value);
    }

    function unencodeValue(value)
    {
        value = unescape(value);
        //value = value.replace(/aaa/g, "%20");
        return(value);
    }

    function popupFromDialog(url, width, height)
    {
        var options = "toolbar=no,scrollbars=yes,resizable=yes,width="+width+",height="+height+";"
        popupWindow=window.open(url,"_blank",options);
        return(popupWindow);
    }

    function confirmRestart()
    {
        if (window.confirm("<iwcm:text key="admin.conf_editor.do_you_really_want_to_restart"/>"))
        {
            $.ajax({
                type: "POST",
                url: "/admin/conf_editor.jsp",
                data: "act=restart&name=",
                success: function(msg)
                {
                    alert('<iwcm:text key="admin.conf_editor.restarted"/>');
                }
            });
        }
    }

    function confirmDelete(text)
    {
        if (window.confirm(text))
        {
            return true;
        }
        return false;
    }

    function showLoader(el) {
        var imageClass = 'loader',
			imageSource = '/admin/skins/webjet8/assets/admin/layout/img/ajax-loading.svg',
        	parent = el != 'undefined' ? el : $('body');

        var imageElement = parent.find('.' + imageClass);
        if (imageElement.length == 0) {
            imageElement = parent.append('<img id="dynamic">');
            imageElement.prop('src', imageSource);

            parent.append(imageElement);
        }

        imageElement.show();
    }

    function hideLoader() {

    }

    function showNotification(level, title, message, autoclose)
	 {
	 	var timeout = 8000;
	 	var extendedTimeout = 1000;
	 	if (autoclose==false)
		{
			timeout = 0;
			extendedTimeout = 0;
		}

	 	toastr[level](message, title, {
	 		"closeButton": true,
			"newestOnTop": true,
			"progressBar": true,
			"tapToDismiss": false,
			"closeOnHover": false,
			"timeOut": timeout,
			"extendedTimeOut": extendedTimeout
	 	});
	 }

	 function fireEvent(name, detail) {
		 if (typeof detail == "undefined") detail = "";
		//firni event
		var event = new CustomEvent(name, {
				detail: detail
		});
		//console.log("WJ.fireEvent, name=", name, "detail=", detail);
		window.dispatchEvent(event);
	 }

    return {
        init: function() {

		},
		showLoader: function(el) {
            return showLoader(el);
        },
		hideLoader: function(el) {
            return hideLoader(el);
        },
        internationalToEnglish: function(text, whitespaceToDash) {
            return internationalToEnglish(text, whitespaceToDash);
		},
        removeChars: function(text) {
            return removeChars(text);
        },
        removeSpojky: function(text) {
            return removeSpojky(text);
        },
        fixFileName: function(fileName) {
            return fixFileName(fileName);
        },
        popup: function(url, width, height) {
            return popup(url, width, height);
        },
        encodeValue: function(value) {
			return encodeValue(value);
        },
        unencodeValue: function(value) {
            return unencodeValue(value);
        },
        popupFromDialog: function(url, width, height) {
			return popupFromDialog(url, width, height);
        },
        confirmRestart: function() {
            return confirmRestart();
        },
        confirmDelete: function(text) {
            return confirmDelete(text);
        },
        capitalizeFirstLetter: function (string) {
        	return string.charAt(0).toUpperCase() + string.slice(1);
    	},
		isNotEmpty: function(string) {
            return typeof string != 'undefined' && string != null && $.trim(string) != '';
		},
		isEmpty: function(string) {
            return !isNotEmpty(string);
        },
		showError: function(message, title) {
			showNotification("error", message, title, false);
        },
		showInfo: function(message, title) {
			 showNotification("info", message, title, true);
		 },
		 showSuccess: function(message, title) {
			 showNotification("success", message, title, true);
		 },
		 fireEvent: function(name, detail) {
			 fireEvent(name, detail);
		 }

	 }
})();

if (typeof $ != "undefined") {
    $(function () {
        if (IeVersion().CompatibilityMode) {
            toastr["warning"]("<%= prop.getText("admin.compatibility.on") %>", "<%= prop.getText("admin.compatibility.on.header") %>");
        }
    });
}


//aktualizacia spojenia na server cez refresher.jsp s kontrolou prihlasenia
var toastrLogoffMessageShown = false;
function callRefresher()
{
	var now = new Date();
	$.ajax({
		url: "/admin/refresher.jsp?t="+now.getTime(),
		success: function(data)
		{
			if (data.indexOf("logonForm")!=-1)
			{
				var logonTimeout = 5*60*1000;

				try {
					if (toastrLogoffMessageShown == false)
					{
						toastr.error("<iwcm:text key="admin.logon.timeoutMessage"/>", "<iwcm:text key="admin.logon.timeoutTitle"/>", {
							"positionClass": "toast-top-full-width",
							"timeOut": logonTimeout,
							"progressBar": true,
							"closeButton": true,
							"tapToDismiss": false,
							"closeOnHover": false
						});

						toastrLogoffMessageShown = true;
					}
				}
				catch (e) {}

				setTimeout(function() {window.location.href = "/admin/";}, logonTimeout);
			}
			else
			{
				$("#refresherDataDiv").html(data);
			}
		}
	});
}
//ak nie sme inline editor v admine, tak inicializuj refresher
if (window.location.href.indexOf("inlineEditorAdmin=true")==-1) {
	window.setInterval(function() {
		callRefresher();
	}, 60000);
	//spusti to aj hned na zaciatku
	$(document).ready(function() { callRefresher(); } );
}

<%
//ak existuje custom kod ktory chceme pridat
String footerInclude = WriteTag.getCustomPageAdmin("/admin/scripts/common-append.jsp", request);
if (footerInclude!=null)
{
	pageContext.include(footerInclude);
}
%>
