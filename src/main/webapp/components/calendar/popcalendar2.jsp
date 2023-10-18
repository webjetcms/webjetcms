<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%>

document.write("<style>");
document.write("#calendar table");
document.write("{");
document.write("   padding: 0px;");
document.write("   margin: 0px;");
document.write("   border: 0px !important;");
document.write("   width: 100%");
document.write("}");
document.write("#calendar span, #calendar div");
document.write("{");
document.write("   padding: 0px;");
document.write("   margin: 0px;");
document.write("}");
document.write("#calendar table td");
document.write("{");
document.write("   padding: 0px;");
document.write("   margin: 0px;");
document.write("   font-size: 10px;");
document.write("   border: 0px;");
document.write("}");
document.write("#calendar a { cursor: hand; padding: 0px; margin: 0px; }");
document.write("</style>");

<%
String lng = PageLng.getUserLng(request);
%>
//	written	by Tan Ling	Wee	on 2 Dec 2001
//	last updated 28 Jul 2003
//	email : fuushikaden@yahoo.com
//	website : www.pengz.com
//	TabSize: 4
//
//	modified by ALQUANTO 30 July 2003 - german language included.
//									  - modified languageLogic with the ISO-2letter-strings
//									  - changes in in showCalendar: defaultLanguage is already set...
//									  - js and html corrected... more xhtml-compliant... simplier css
//	email: popcalendar@alquanto.de
//
//	modified by PinoToy 25 July 2003  - new logic for multiple languages (English, Spanish and ready for more).
//									  - changes in popUpMonth & popDownMonth methods for hidding	popup.
//									  - changes in popDownYear & popDownYear methods for hidding	popup.
//									  - new logic for disabling dates in	the past.
//									  - new method showCalendar, dynamic	configuration of language, enabling	past & position.
//									  - changes in the styles.
//	email  : pinotoy@yahoo.com

	var language = '<%=lng%>';	// Default Language: en - english ; es - spanish; de - german
	var enablePast = 1;		// 0 - disabled ; 1 - enabled
	var enableFuture = 1;	// 0 - disabled ; 1 - enabled
	var fixedX = -1;		// x position (-1 if to appear below control)
	var fixedY = -1;		// y position (-1 if to appear below control)
	var startAt = 1;		// 0 - sunday ; 1 - monday
	var showWeekNumber = 0;	// 0 - don't show; 1 - show
	var showToday = 1;		// 0 - don't show; 1 - show
	var imgDir = '<iwcm:cp/>/components/calendar/';		// directory for images ... e.g. var imgDir="/img/"
	var dayName = '';

	var gotoString = {
		en : 'Go To Current Month',
		es : 'Ir al Mes Actual',
		de : 'Gehe zu aktuellem Monat',
		sk : 'Choď na aktuálny mesiac',
		cz : 'Choď na aktuálny mesiac'
	};
	var todayString = {
		en : 'Today is',
		es : 'Hoy es',
		de : 'Heute ist',
		sk : 'Dnes je',
		cz : 'Dnes je'
	};
	var weekString = {
		en : 'Wk',
		es : 'Sem',
		de : 'KW',
		sk : 'T',
		cz : 'T'
	};
	var scrollLeftMessage = {
		en : 'Click to scroll to previous month. Hold mouse button to scroll automatically.',
		es : 'Presione para pasar al mes anterior. Deje presionado para pasar varios meses.',
		de : 'Klicken um zum vorigen Monat zu gelangen. Gedrückt halten, um automatisch weiter zu scrollen.',
		sk : 'Kliknite sem pre prechod na predchádzajúci mesiac',
		cz : 'Kliknite sem pre prechod na predchádzajúci mesiac'
	};
	var scrollRightMessage = {
		en : 'Click to scroll to next month. Hold mouse button to scroll automatically.',
		es : 'Presione para pasar al siguiente mes. Deje presionado para pasar varios meses.',
		de : 'Klicken um zum nächsten Monat zu gelangen. Gedrückt halten, um automatisch weiter zu scrollen.',
		sk : 'Kliknite sem pre prechod na nasledujúci mesiac',
		cz : 'Kliknite sem pre prechod na nasledujúci mesiac'
	};
	var selectMonthMessage = {
		en : 'Click to select a month.',
		es : 'Presione para seleccionar un mes',
		de : 'Klicken um Monat auszuwählen',
		sk : 'Kliknite pre výber mesiaca',
		cz : 'Kliknite pre výber mesiaca'
	};
	var selectYearMessage = {
		en : 'Click to select a year.',
		es : 'Presione para seleccionar un ańo',
		de : 'Klicken um Jahr auszuwählen',
		sk : 'Kliknite pre výber roku',
		cz : 'Kliknite pre výber roku'
	};
	var selectDateMessage = {		// do not replace [date], it will be replaced by date.
		en : 'Select [date] as date.',
		es : 'Seleccione [date] como fecha',
		de : 'Wähle [date] als Datum.',
		sk : 'Zvoľte [date] ako dátum',
		cz : 'Zvoľte [date] ako dátum'
	};
	var	monthName = {
		en : new Array('January','February','March','April','May','June','July','August','September','October','November','December'),
		es : new Array('Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'),
		de : new Array('Januar','Februar','März','April','Mai','Juni','Juli','August','September','Oktober','November','Dezember'),
		sk : new Array('Január','Február','Marec','Apríl','Máj','Jún','Júl','August','September','Október','November','December'),
		cz : new Array('Leden','Únor','Březen','Duben','Květen','Červen','Červenec','Srpen','Záři','Říjen','Listopad','Prosinec')
	};
	var	monthName2 = {
		en : new Array('JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'),
		es : new Array('ENE','FEB','MAR','ABR','MAY','JUN','JUL','AGO','SEP','OCT','NOV','DIC'),
		de : new Array('JAN','FEB','MRZ','APR','MAI','JUN','JUL','AUG','SEP','OKT','NOV','DEZ'),
		sk : new Array('JAN','FEB','MAR','APR','MÁJ','JÚN','JÚL','AUG','SEP','OKT','NOV','DEC'),
		cz : new Array('Led','Úno','Bře','Dub','Kvě','Črn','Črc','Srp','Zář','Říj','Lis','Pro')
	};

	if (startAt==0) {
		dayName = {
			en : new Array('Sun','Mon','Tue','Wed','Thu','Fri','Sat'),
			es : new Array('Dom','Lun','Mar','Mie','Jue','Vie','Sab'),
			de : new Array('So','Mo','Di','Mi','Do','Fr','Sa'),
			sk : new Array('Ne','Po','Ut','Str','Štv','Pia','So'),
			cz : new Array('Ne','Po','Út','St','Čt','Pá','So')
		};
	} else {
		dayName = {
			en : new Array('Mon','Tue','Wed','Thu','Fri','Sat','Sun'),
			es : new Array('Lun','Mar','Mie','Jue','Vie','Sab','Dom'),
			de : new Array('Mo','Di','Mi','Do','Fr','Sa','So'),
			sk : new Array('Po','Ut','Str','Štv','Pia','So','Ne'),
			cz : new Array('Po','Út','St','Čt','Pá','So','Ne')
		};
	}

	var frameObj,crossobj, crossMonthObj, crossYearObj, monthSelected, yearSelected, dateSelected, omonthSelected, oyearSelected, odateSelected, monthConstructed, yearConstructed, intervalID1, intervalID2, timeoutID1, timeoutID2, ctlToPlaceValue, ctlNow, dateFormat, nStartingYear, selDayAction, isPast;
	var visYear  = 0;
	var visMonth = 0;
	var bPageLoaded = false;
	var ie  = document.all;
	var dom = document.getElementById;
	var ns4 = document.layers;
	var today    = new Date();
	var dateNow  = today.getDate();
	var monthNow = today.getMonth();
	var yearNow  = today.getYear();
	if (today.getFullYear) yearNow = today.getFullYear();
	var imgsrc   = new Array('drop1.gif','drop2.gif','left1.gif','left2.gif','right1.gif','right2.gif');
	var img      = new Array();
	var bShow    = false;
	var popCalendarDisableHide = false;
	var calendarRows = 5;
	
	//added by jeeff
	var yearNowPast = yearNow;
	var monthNowPast = monthNow;
	var dateNowPast = dateNow;

	/* hides <select> and <applet> objects (for IE only) */
	function hideElement( elmID, overDiv ) {
		if(ie && popCalendarDisableHide==false) {
			for(i = 0; i < document.all.tags( elmID ).length; i++) {
				obj = document.all.tags( elmID )[i];
				if(!obj || !obj.offsetParent) continue;

				// Find the element's offsetTop and offsetLeft relative to the BODY tag.
				objLeft   = obj.offsetLeft;
				objTop    = obj.offsetTop;
				objParent = obj.offsetParent;

				while(objParent != null && objParent.tagName.toUpperCase() != 'BODY') 
				{
				
					objLeft  += objParent.offsetLeft;
					objTop   += objParent.offsetTop;
					objParent = objParent.offsetParent;
				}
				objHeight = obj.offsetHeight;
				objWidth  = obj.offsetWidth;

				if((overDiv.offsetLeft + overDiv.offsetWidth) <= objLeft);
				else if((overDiv.offsetTop + overDiv.offsetHeight) <= objTop);
				/* CHANGE by Charlie Roche for nested TDs*/
				else if(overDiv.offsetTop >= (objTop + objHeight + obj.height));
				/* END CHANGE */
				else if(overDiv.offsetLeft >= (objLeft + objWidth));
				else {
					obj.style.visibility = 'hidden';
				}
			}
		}
	}

	/*
	* unhides <select> and <applet> objects (for IE only)
	*/
	function showElement(elmID) {
		if(ie && popCalendarDisableHide==false) {
			for(i = 0; i < document.all.tags( elmID ).length; i++) {
				obj = document.all.tags(elmID)[i];
				if(!obj || !obj.offsetParent) continue;
				obj.style.visibility = '';
			}
		}
	}

	function HolidayRec (d, m, y, desc) {
		this.d = d;
		this.m = m;
		this.y = y;
		this.desc = desc;
	}

	var HolidaysCounter = 0;
	var Holidays = new Array();

	function addHoliday (d, m, y, desc) {
		Holidays[HolidaysCounter++] = new HolidayRec (d, m, y, desc);
	}

	if (dom) {
		for	(i=0;i < imgsrc.length;i++) {
			img[i] = new Image;
			img[i].src = imgDir + imgsrc[i];
		}
		document.write ('<iframe id="calendarFrame" src="/components/iframe_blank.jsp" style="z-index:+900;position:absolute;visibility:hidden;width:220px;height:150px;border:0px;" frameborder="0"></iframe><div onclick="bShow=true" id="calendar" style="z-index:+999;position:absolute;visibility:hidden;width:220px;">');
      document.write ('   <table style="font-family:Arial;font-size:11px;border: 1px solid #A0A0A0;width:'+((showWeekNumber==1)?250:220)+'px;" bgcolor="#ffffff">');
      document.write ('      <tr style="background-color: #255596;"><td>'); //006400
      document.write ('         <table style="margin: 0px;" cellspacing=0 cellpadding=0 width="'+((showWeekNumber==1)?248:218)+'"><tr><td style="padding:2px;font-family:Arial;font-size:11px;color:#ffffff"><b><span id="caption"></span></b></td><td align="right" style="width:15px"><a onClick="hideCalendar()"><img src="'+imgDir+'close.gif" width="15" height="13" border="0"></a></td></tr></table>');
      //document.write ('         <table cellspacing=0 cellpadding=0><tr><td style="width:200px">LAVA</td><td align="right" style="width:15px">PR</td></tr></table>');
      document.write ('      </td></tr><tr><td style="padding:0px" bgcolor="#ffffff"><span id="calendarContent"></span></td></tr>');

		if (showToday == 1) {
			document.write ('<tr bgcolor="#f0f0f0"><td style="padding:5px" align="center"><span id="lblToday"></span></td></tr>');
		}
			
		document.write ('   </table>');      
      document.write ('</div>');   
      
      document.write ('<div id="selectMonth" style="z-index:+999;position:absolute;visibility:hidden;"></div>');
      document.write ('<div id="selectYear" style="z-index:+999;position:absolute;visibility:hidden;"></div>');
      
         
      
      
	}

	var	styleAnchor = 'text-decoration:none;color:black;';
	var	styleLightBorder = 'border:1px solid #a0a0a0;';

	function swapImage(srcImg, destImg) {
		if (ie) document.getElementById(srcImg).setAttribute('src',imgDir + destImg);
	}

	function initCalendar() {
		if (!ns4 && !bPageLoaded)
		{
			crossobj=(dom)?document.getElementById('calendar').style : ie? document.all.calendar : document.calendar;
			frameObj = null;
			
			if (ie) frameObj=(dom)?document.getElementById('calendarFrame').style : ie? document.all.calendar : document.calendar;			
			if (navigator.userAgent.indexOf("Opera")!=-1) frameObj = null;
			
			hideCalendar();

			crossMonthObj = (dom) ? document.getElementById('selectMonth').style : ie ? document.all.selectMonth : document.selectMonth;

			crossYearObj = (dom) ? document.getElementById('selectYear').style : ie ? document.all.selectYear : document.selectYear;

			monthConstructed = false;
			yearConstructed = false;

			if (showToday == 1) {
				document.getElementById('lblToday').innerHTML =	'<font color="#000066">' + todayString[language] + ' <a onmousemove="window.status=\''+gotoString[language]+'\'" onmouseout="window.status=\'\'" title="'+gotoString[language]+'" style="'+styleAnchor+'" onClick="monthSelected=monthNow;yearSelected=yearNow;constructCalendar();">'+dayName[language][(today.getDay()-startAt==-1)?6:(today.getDay()-startAt)]+', ' + dateNow + ' ' + monthName[language][monthNow].substring(0,3) + ' ' + yearNow + '</a></font>';
			}

			sHTML1 = '<span id="spanLeft" style="cursor:pointer; cursor:hand;" onmouseover="swapImage(\'changeLeft\',\'left2.gif\');this.style.borderColor=\'#8af\';window.status=\''+scrollLeftMessage[language]+'\'" onclick="decMonth()" onmouseout="clearInterval(intervalID1);swapImage(\'changeLeft\',\'left1.gif\');this.style.borderColor=\'#36f\';window.status=\'\'" onmousedown="clearTimeout(timeoutID1);timeoutID1=setTimeout(\'StartDecMonth()\',500)" onmouseup="clearTimeout(timeoutID1);clearInterval(intervalID1)">&nbsp<img id="changeLeft" src="'+imgDir+'left1.gif" width="10" height="11" border="0">&nbsp</span>&nbsp;';
			sHTML1 += '<span id="spanRight" style="cursor:pointer; cursor:hand;" onmouseover="swapImage(\'changeRight\',\'right2.gif\');this.style.borderColor=\'#8af\';window.status=\''+scrollRightMessage[language]+'\'" onmouseout="clearInterval(intervalID1);swapImage(\'changeRight\',\'right1.gif\');this.style.borderColor=\'#36f\';window.status=\'\'" onclick="incMonth()" onmousedown="clearTimeout(timeoutID1);timeoutID1=setTimeout(\'StartIncMonth()\',500)" onmouseup="clearTimeout(timeoutID1);clearInterval(intervalID1)">&nbsp<img id="changeRight" src="'+imgDir+'right1.gif" width="10" height="11" border="0">&nbsp</span>&nbsp;';
			sHTML1 += '<span id="spanMonth" style="cursor:pointer; cusror:hand; white-space:nowrap;" onmouseover="swapImage(\'changeMonth\',\'drop2.gif\');this.style.borderColor=\'#8af\';window.status=\''+selectMonthMessage[language]+'\'" onmouseout="swapImage(\'changeMonth\',\'drop1.gif\');this.style.borderColor=\'#36f\';window.status=\'\'" onclick="popUpMonth()"></span>&nbsp;';
			sHTML1 += '<span id="spanYear" style="cursor:pointer; cursor:hand; white-space:nowrap;" onmouseover="swapImage(\'changeYear\',\'drop2.gif\');this.style.borderColor=\'#8af\';window.status=\''+selectYearMessage[language]+'\'" onmouseout="swapImage(\'changeYear\',\'drop1.gif\');this.style.borderColor=\'#36f\';window.status=\'\'" onclick="popUpYear()"></span>&nbsp;';

			document.getElementById('caption').innerHTML = sHTML1;

			bPageLoaded=true;
		}
	}

	function hideCalendar() {
		if (crossobj) crossobj.visibility = 'hidden';
		if (frameObj!=null) frameObj.visibility = 'hidden';
		if (crossMonthObj != null) crossMonthObj.visibility = 'hidden';
		if (crossYearObj  != null) crossYearObj.visibility = 'hidden';
		showElement('SELECT');
		showElement('APPLET');
	}

	function padZero(num) {
		return (num	< 10) ? '0' + num : num;
	}

	function constructDate(d,m,y) {
		sTmp = dateFormat;
		sTmp = sTmp.replace ('dd','<e>');
		sTmp = sTmp.replace ('d','<d>');
		sTmp = sTmp.replace ('<e>',padZero(d));
		sTmp = sTmp.replace ('<d>',d);
		sTmp = sTmp.replace ('mmmm','<p>');
		sTmp = sTmp.replace ('mmm','<o>');
		sTmp = sTmp.replace ('mm','<n>');
		sTmp = sTmp.replace ('m','<m>');
		sTmp = sTmp.replace ('<m>',m+1);
		sTmp = sTmp.replace ('<n>',padZero(m+1));
		sTmp = sTmp.replace ('<o>',monthName[language][m]);
		sTmp = sTmp.replace ('<p>',monthName2[language][m]);
		sTmp = sTmp.replace ('yyyy',y);
		return sTmp.replace ('yy',padZero(y%100));
	}

	function closeCalendar() {
	   ctlToPlaceValue.value = constructDate(dateSelected,monthSelected,yearSelected);
		hideCalendar();		
	}

	/*** Month Pulldown	***/
	function StartDecMonth() {
		intervalID1 = setInterval("decMonth()",80);
	}

	function StartIncMonth() {
		intervalID1 = setInterval("incMonth()",80);
	}

	function incMonth () {
		monthSelected++;
		if (monthSelected > 11) {
			monthSelected = 0;
			yearSelected++;
		}
		constructCalendar();
	}

	function decMonth () {
		monthSelected--;
		if (monthSelected < 0) {
			monthSelected = 11;
			yearSelected--;
		}
		constructCalendar();
	}

	function constructMonth() {
		popDownYear()
		if (!monthConstructed) {
			sHTML = "";
			for (i=0; i<12; i++) {
				sName = monthName[language][i];
				if (i == monthSelected){
					sName = '<b>' + sName + '</b>';
				}
				sHTML += '<tr><td id="m' + i + '" onmouseover="this.style.backgroundColor=\'#909090\'" onmouseout="this.style.backgroundColor=\'\'" style="cursor:pointer; cursor:hand;" onclick="monthConstructed=false;monthSelected=' + i + ';constructCalendar();popDownMonth();event.cancelBubble=true"><font color="#000066">&nbsp;' + sName + '&nbsp;</font></td></tr>';
			}

			document.getElementById('selectMonth').innerHTML = '<table  style="font-family:Arial;font-size:11px;border:1px solid #a0a0a0; width:70px;" bgcolor="#f0f0f0" cellspacing="0" onmouseover="clearTimeout(timeoutID1)" onmouseout="clearTimeout(timeoutID1);timeoutID1=setTimeout(\'popDownMonth()\',100);event.cancelBubble=true">' + sHTML + '</table>';

			monthConstructed = true;
		}
	}

	function popUpMonth() 
	{
		if (visMonth == 1) {
			popDownMonth();
			visMonth--;
		} else {
			constructMonth();
			crossMonthObj.visibility = (dom||ie) ? 'visible' : 'show';
			crossMonthObj.left = (parseInt(crossobj.left) + 45)+"px";
			crossMonthObj.top =	(parseInt(crossobj.top) + 20)+"px";
			hideElement('SELECT', document.getElementById('selectMonth'));
			hideElement('APPLET', document.getElementById('selectMonth'));
			visMonth++;
		}
	}

	function popDownMonth() {
		crossMonthObj.visibility = 'hidden';
		visMonth = 0;
	}

	/*** Year Pulldown ***/
	function incYear() {
		for	(i=0; i<7; i++) {
			newYear	= (i + nStartingYear) + 1;
			if (newYear == yearSelected)
				txtYear = '<span style="color:#006;font-weight:bold;">&nbsp;' + newYear + '&nbsp;</span>';
			else
				txtYear = '<span style="color:#006;">&nbsp;' + newYear + '&nbsp;</span>';
			document.getElementById('y'+i).innerHTML = txtYear;
		}
		nStartingYear++;
		bShow=true;
	}

	function decYear() {
		for	(i=0; i<7; i++) {
			newYear = (i + nStartingYear) - 1;
			if (newYear == yearSelected)
				txtYear = '<span style="color:#006;font-weight:bold">&nbsp;' + newYear + '&nbsp;</span>';
			else
				txtYear = '<span style="color:#006;">&nbsp;' + newYear + '&nbsp;</span>';
			document.getElementById('y'+i).innerHTML = txtYear;
		}
		nStartingYear--;
		bShow=true;
	}

	function selectYear(nYear) {
		yearSelected = parseInt(nYear + nStartingYear);
		yearConstructed = false;
		constructCalendar();
		popDownYear();
	}

	function constructYear() {
		popDownMonth();
		sHTML = '';
		if (!yearConstructed) {
			sHTML = '<tr><td align="center" onmouseover="this.style.backgroundColor=\'#909090\'" onmouseout="clearInterval(intervalID1);this.style.backgroundColor=\'\'" style="cursor:pointer" onmousedown="clearInterval(intervalID1);intervalID1=setInterval(\'decYear()\',30)" onmouseup="clearInterval(intervalID1)"><font color="#000066">-</font></td></tr>';

			j = 0;
			nStartingYear =	yearSelected - 3;
			for ( i = (yearSelected-3); i <= (yearSelected+3); i++ ) {
				sName = i;
				if (i == yearSelected) sName = '<b>' + sName + '</b>';
				sHTML += '<tr><td id="y' + j + '" onmouseover="this.style.backgroundColor=\'#909090\'" onmouseout="this.style.backgroundColor=\'\'" style="cursor:pointer" onclick="selectYear('+j+');event.cancelBubble=true"><font color="#000066">&nbsp;' + sName + '&nbsp;</font></td></tr>';
				j++;
			}

			sHTML += '<tr><td align="center" onmouseover="this.style.backgroundColor=\'#909090\'" onmouseout="clearInterval(intervalID2);this.style.backgroundColor=\'\'" style="cursor:pointer" onmousedown="clearInterval(intervalID2);intervalID2=setInterval(\'incYear()\',30)" onmouseup="clearInterval(intervalID2)"><font color="#000066">+</font></td></tr>';

			document.getElementById('selectYear').innerHTML = '<table cellspacing="0" bgcolor="#f0f0f0" style="width:44px; font-family:Arial;font-size:11px;border:1px solid #a0a0a0;" onmouseover="clearTimeout(timeoutID2)" onmouseout="clearTimeout(timeoutID2);timeoutID2=setTimeout(\'popDownYear()\',100)">' + sHTML + '</table>';

			yearConstructed = true;
		}
	}

	function popDownYear() {
		clearInterval(intervalID1);
		clearTimeout(timeoutID1);
		clearInterval(intervalID2);
		clearTimeout(timeoutID2);
		crossYearObj.visibility= 'hidden';
		visYear = 0;
	}

	function popUpYear() {
		var leftOffset
		if (visYear==1) {
			popDownYear();
			visYear--;
		} else {
			constructYear();
			crossYearObj.visibility	= (dom||ie) ? 'visible' : 'show';
			leftOffset = parseInt(crossobj.left) + document.getElementById('spanYear').offsetLeft;
			if (ie) leftOffset += 6;
			crossYearObj.left = leftOffset+"px";
			crossYearObj.top = (parseInt(crossobj.top) + 20)+"px";
			visYear++;
		}
	}

	/*** calendar ***/
	function WeekNbr(n) {
		// Algorithm used:
		// From Klaus Tondering's Calendar document (The Authority/Guru)
		// http://www.tondering.dk/claus/calendar.html
		// a = (14-month) / 12
		// y = year + 4800 - a
		// m = month + 12a - 3
		// J = day + (153m + 2) / 5 + 365y + y / 4 - y / 100 + y / 400 - 32045
		// d4 = (J + 31741 - (J mod 7)) mod 146097 mod 36524 mod 1461
		// L = d4 / 1460
		// d1 = ((d4 - L) mod 365) + L
		// WeekNumber = d1 / 7 + 1

		year = n.getFullYear();
		month = n.getMonth() + 1;
		if (startAt == 0) {
			day = n.getDate() + 1;
		} else {
			day = n.getDate();
		}

		a = Math.floor((14-month) / 12);
		y = year + 4800 - a;
		m = month + 12 * a - 3;
		b = Math.floor(y/4) - Math.floor(y/100) + Math.floor(y/400);
		J = day + Math.floor((153 * m + 2) / 5) + 365 * y + b - 32045;
		d4 = (((J + 31741 - (J % 7)) % 146097) % 36524) % 1461;
		L = Math.floor(d4 / 1460);
		d1 = ((d4 - L) % 365) + L;
		week = Math.floor(d1/7) + 1;

		return week;
	}

	function constructCalendar () {
		var aNumDays = Array (31,0,31,30,31,30,31,31,30,31,30,31);
		var dateMessage;
		var startDate = new Date (yearSelected,monthSelected,1);
		var endDate;

		if (monthSelected==1) {
			endDate = new Date (yearSelected,monthSelected+1,1);
			endDate = new Date (endDate - (24*60*60*1000));
			numDaysInMonth = endDate.getDate();
		} else {
			numDaysInMonth = aNumDays[monthSelected];
		}

		datePointer = 0;
		dayPointer = startDate.getDay() - startAt;
		
		if (dayPointer<0) dayPointer = 6;

		sHTML = '<table border="0" style="font-family:verdana;font-size:10px;"><tr>';

		if (showWeekNumber == 1) {
			sHTML += '<td width="27"><b>' + weekString[language] + '</b></td><td width="1" rowspan="7" bgcolor="#d0d0d0" style="padding:0px"><img src="'+imgDir+'divider.gif" width="1"></td>';
		}

		for (i = 0; i<7; i++) {
			sHTML += '<td width="27" align="right"><b><font color="#000066">' + dayName[language][i] + '</font></b></td>';
		}

		sHTML += '</tr><tr>';
		
		if (showWeekNumber == 1) {
			sHTML += '<td align="right">' + WeekNbr(startDate) + '&nbsp;</td>';
		}

		for	( var i=1; i<=dayPointer;i++ ) {
			sHTML += '<td>&nbsp;</td>';
		}
	
		calendarRows = 1;
		for	( datePointer=1; datePointer <= numDaysInMonth; datePointer++ ) {
			dayPointer++;
			sHTML += '<td align="right">';
			sStyle=styleAnchor;
			if ((datePointer == odateSelected) && (monthSelected == omonthSelected) && (yearSelected == oyearSelected))
			{ sStyle+=styleLightBorder }

			sHint = '';
			for (k = 0;k < HolidaysCounter; k++) {
				if ((parseInt(Holidays[k].d) == datePointer)&&(parseInt(Holidays[k].m) == (monthSelected+1))) {
					if ((parseInt(Holidays[k].y)==0)||((parseInt(Holidays[k].y)==yearSelected)&&(parseInt(Holidays[k].y)!=0))) {
						sStyle+= 'background-color:#fdd;';
						sHint += sHint=="" ? Holidays[k].desc : "\n"+Holidays[k].desc;
					}
				}
			}

			sHint = sHint.replace('/\"/g', '&quot;');

			dateMessage = 'onmousemove="window.status=\''+selectDateMessage[language].replace('[date]',constructDate(datePointer,monthSelected,yearSelected))+'\'" onmouseout="window.status=\'\'" ';


			//////////////////////////////////////////////
			//////////  Modifications PinoToy  //////////
			//////////////////////////////////////////////
			if (enablePast == 0 && ((yearSelected < yearNowPast) || (monthSelected < monthNowPast) && (yearSelected == yearNowPast) || (datePointer < dateNowPast) && (monthSelected == monthNowPast) && (yearSelected == yearNowPast))) {
				selDayAction = '';
				isPast = 1;
			} else {
				selDayAction = 'onClick="dateSelected=' + datePointer + ';closeCalendar();"';
				isPast = 0;
			}
			
			if (enableFuture == 0 && ((yearSelected > yearNowPast) || (monthSelected > monthNowPast) && (yearSelected == yearNowPast) || (datePointer > dateNowPast) && (monthSelected == monthNowPast) && (yearSelected == yearNowPast))) {
				selDayAction = '';
				isPast = 1;
			} else {
				selDayAction = 'onClick="dateSelected=' + datePointer + ';closeCalendar();"';
				isPast = 0;
			}

			if ((datePointer == dateNow) && (monthSelected == monthNow) && (yearSelected == yearNow)) {	///// today
				sHTML += "<b><a "+dateMessage+" title=\"" + sHint + "\" style='"+sStyle+"' "+selDayAction+"><font color=#ff0000>&nbsp;" + datePointer + "</font>&nbsp;</a></b>";
			} else if (dayPointer % 7 == (startAt * -1)+1) {									///// SI ES DOMINGO
				if (isPast==1)
					sHTML += "<a "+dateMessage+" title=\"" + sHint + "\" style='"+sStyle+"' "+selDayAction+">&nbsp;<font color=#909090>" + datePointer + "</font>&nbsp;</a>";
				else
					sHTML += "<a "+dateMessage+" title=\"" + sHint + "\" style='"+sStyle+"' "+selDayAction+">&nbsp;<font color=#54A6E2>" + datePointer + "</font>&nbsp;</a>";
			} else if ((dayPointer % 7 == (startAt * -1)+7 && startAt==1) || (dayPointer % 7 == startAt && startAt==0)) {	///// SI ES SABADO
				if (isPast==1)
					sHTML += "<a "+dateMessage+" title=\"" + sHint + "\" style='"+sStyle+"' "+selDayAction+">&nbsp;<font color=#909090>" + datePointer + "</font>&nbsp;</a>";
				else
					sHTML += "<a "+dateMessage+" title=\"" + sHint + "\" style='"+sStyle+"' "+selDayAction+">&nbsp;<font color=#54A6E2>" + datePointer + "</font>&nbsp;</a>";
			} else {																			///// CUALQUIER OTRO DIA
				if (isPast==1)
					sHTML += "<a "+dateMessage+" title=\"" + sHint + "\" style='"+sStyle+"' "+selDayAction+">&nbsp;<font color=#909090>" + datePointer + "</font>&nbsp;</a>";
				else
					sHTML += "<a "+dateMessage+" title=\"" + sHint + "\" style='"+sStyle+"' "+selDayAction+">&nbsp;<font color=#000066>" + datePointer + "</font>&nbsp;</a>";
			}

			sHTML += '';
			if ((dayPointer+startAt) % 7 == startAt) {
				sHTML += '</tr><tr>';				
			   if (datePointer < numDaysInMonth) calendarRows++;
				if ((showWeekNumber == 1) && (datePointer < numDaysInMonth)) { 
					sHTML += '<td align="right">' + (WeekNbr(new Date(yearSelected,monthSelected,datePointer+1))) + '&nbsp;</td>';
				}
			}
		}
		
		document.getElementById('calendarContent').innerHTML   = sHTML
		document.getElementById('spanMonth').innerHTML = '&nbsp;' +	monthName[language][monthSelected] + '&nbsp;<img id="changeMonth" src="'+imgDir+'drop1.gif" width="12" height="10" border="0">'
		document.getElementById('spanYear').innerHTML  = '&nbsp;' + yearSelected	+ '&nbsp;<img id="changeYear" src="'+imgDir+'drop1.gif" width="12" height="10" border="0">';
	}

	function showCalendar(ctl, ctl2, format, lang, past, future, fx, fy, datePast)
	{
		if (lang != null && lang != '') language = lang;
		if (past != null) enablePast = past;
		//else enablePast = 1;
		if (future != null) enableFuture = future;
		//else enableFuture = 1;
		if (fx != null) fixedX = fx;
		else fixedX = -1;
		if (fy != null) fixedY = fy;
		else fixedY = -1;
		
		if (datePast != null && datePast != "")
		{
			var dates = datePast.split(".");
			dateNowPast = Number(dates[0]);
			monthNowPast = Number(dates[1]) - 1;
			yearNowPast = Number(dates[2]);
			
			//window.alert(datePast+"="+dateNowPast+"."+monthNowPast+"."+yearNowPast+" tm="+today.getMonth());
		}
		else
		{
			yearNowPast = yearNow;
			monthNowPast = monthNow;
			dateNowPast = dateNow;		
		}

		if (showToday == 1) {
			document.getElementById('lblToday').innerHTML = '<font color="#000066">' + todayString[language] + ' <a onmousemove="window.status=\''+gotoString[language]+'\'" onmouseout="window.status=\'\'" title="'+gotoString[language]+'" style="'+styleAnchor+'" onClick="monthSelected=monthNow;yearSelected=yearNow;constructCalendar();">'+dayName[language][(today.getDay()-startAt==-1)?6:(today.getDay()-startAt)]+', ' + dateNow + ' ' + monthName[language][monthNow].substring(0,3) + ' ' + yearNow + '</a></font>';
		}
		popUpCalendar(ctl, ctl2, format);
	}

	function popUpCalendar(ctl, ctl2, format) {
		var leftpos = 0;
		var toppos  = 0;

		if (bPageLoaded) {
			if (crossobj.visibility == 'hidden') 
			{
				ctlToPlaceValue = ctl2;
				dateFormat = format;
				formatChar = ' ';
				aFormat = dateFormat.split(formatChar);
				if (aFormat.length < 3) {
					formatChar = '/';
					aFormat = dateFormat.split(formatChar);
					if (aFormat.length < 3) {
						formatChar = '.';
						aFormat = dateFormat.split(formatChar);
						if (aFormat.length < 3) {
							formatChar = '-';
							aFormat = dateFormat.split(formatChar);
							if (aFormat.length < 3) {
								formatChar = '';					// invalid date format

							}
						}
					}
				}

				tokensChanged = 0;
				if (formatChar != "") {
					aData =	ctl2.value.split(formatChar);			// use user's date

					for (i=0; i<3; i++) {
						if ((aFormat[i] == "d") || (aFormat[i] == "dd")) {
							dateSelected = parseInt(aData[i], 10);
							tokensChanged++;
						} else if ((aFormat[i] == "m") || (aFormat[i] == "mm")) {
							monthSelected = parseInt(aData[i], 10) - 1;
							tokensChanged++;
						} else if (aFormat[i] == "yyyy") {
							yearSelected = parseInt(aData[i], 10);
							tokensChanged++;
						} else if (aFormat[i] == "mmm") {
							for (j=0; j<12; j++) {
								if (aData[i] == monthName[language][j]) {
									monthSelected=j;
									tokensChanged++;
								}
							}
						} else if (aFormat[i] == "mmmm") {
							for (j=0; j<12; j++) {
								if (aData[i] == monthName2[language][j]) {
									monthSelected = j;
									tokensChanged++;
								}
							}
						}
					}
				}

				if ((tokensChanged != 3) || isNaN(dateSelected) || isNaN(monthSelected) || isNaN(yearSelected)) {
					dateSelected  = dateNow;
					monthSelected = monthNow;
					yearSelected  = yearNow;
				}

				odateSelected  = dateSelected;
				omonthSelected = monthSelected;
				oyearSelected  = yearSelected;

				aTag = ctl;
				var classNameOK = true;
				do
				{
					aTag     = aTag.offsetParent;
					
					if (aTag != null)
					{
					   if ( aTag.className && aTag.className.indexOf("calendarPopupStop")!=-1) classNameOK = false;
					   
					   if (classNameOK)
					   {
							leftpos += aTag.offsetLeft;
							toppos  += aTag.offsetTop;
						}
					}
										
					
				} while (aTag && aTag.tagName != 'BODY' && classNameOK);
				
				//window.status = ctl.offsetTop + " p="+toppos+" o="+ctl.offsetHeight;
				crossobj.left = (fixedX == -1) ? (ctl.offsetLeft + leftpos + "px") : (fixedX+"px");
				crossobj.top = (fixedY == -1) ? (ctl.offsetTop + toppos + ctl.offsetHeight + 2 + "px") : (fixedY+"px");
				
				//posunieme ak by to presahovalo sirku okna
				if(fixedX == -1 && document.body && document.body.clientWidth) 
				{
    				//IE 4 compatible
    				var clientWidth = document.body.clientWidth;
    				var objLeft = ctl.offsetLeft + leftpos;
    				if (objLeft + 220 > clientWidth)
    				{
    					crossobj.left = (clientWidth - 235) + "px"
    				}
				}
				
				if (frameObj != null)
				{
					var crossobjRuntime = document.getElementById('calendarContent').runtimeStyle;
				
				   frameObj.left = crossobj.left;
				   frameObj.top = crossobj.top;				
				   if (crossobj.width!="") frameObj.width = crossobj.width;
				   if (crossobj.height!="")
				   {
				      frameObj.height = crossobj.height;
				   }
				   else
				   {
				   	if (calendarRows == 5) frameObj.height = "137px";
				   	if (calendarRows == 6) frameObj.height = "150px";
				   }
				   frameObj.visibility = (dom||ie) ? "visible" : "show";
				}
				
				constructCalendar (1, monthSelected, yearSelected);
				crossobj.visibility = (dom||ie) ? "visible" : "show";
				

				hideElement('SELECT', document.getElementById('calendar'));
				hideElement('APPLET', document.getElementById('calendar'));			

				bShow = true;
			} else {
				hideCalendar();
				if (ctlNow!=ctl) popUpCalendar(ctl, ctl2, format);
			}
			ctlNow = ctl;
		}
	}

	document.onkeypress = function hidecal1 () {
		//if (event && event.keyCode == 27) hideCalendar();
	}
	document.onclick = function hidecal2 () {
		if (!bShow) hideCalendar();
		bShow = false;
	}

	if(ie) {
		initCalendar();
	} else {
		window.onload = initCalendar;
	}
