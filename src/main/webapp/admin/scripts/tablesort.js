

var dom = (document.getElementsByTagName) ? true : false;
var ie5 = (document.getElementsByTagName && document.all) ? true : false;
var arrowUp, arrowDown;

var firstColumnNumbers = true;

if (ie5 || dom)
   initSortTable();
   
//added by jeeff
function highlight(obj,on)
{
   if (ie5 || dom)
   {
      if (on==1){
		  obj.style.background="#ffffff";
	  }else{
		  obj.style.background="none";
	  }
   }
}
   

function initSortTable() 
{
   arrowUp = document.createElement("SPAN");
   //var tn = document.createTextNode("&uarr;");
   //arrowUp.appendChild(tn);
   arrowUp.innerHTML = "&uarr;";   
   arrowUp.className = "arrow";

   arrowDown = document.createElement("SPAN");
   //var tn = document.createTextNode("&darr;");
   //arrowDown.appendChild(tn);
   arrowDown.innerHTML = "&darr;";
   arrowDown.className = "arrow";
}



function sortTable(tableNode, nCol, bDesc, sType) 
{
   var tBody = tableNode.tBodies[0];
   var trs = tBody.rows;
   var a = new Array();
   
   for (var i=0; i<trs.length; i++) 
   {
      a[i] = trs[i];
   }
   
   a.sort(compareByColumn(nCol,bDesc,sType));
   
   for (var i=0; i<a.length; i++)    
   {      
      //modified by jeeff
      if (firstColumnNumbers == true)
      {
         j = i+1;      
         setInnerText(a[i].cells[0], j+".");
      }
      tBody.appendChild(a[i]);
   }
}

function CaseInsensitiveString(s)
{
   return String(s).toUpperCase();
}

function parseDate(s) 
{   
   //ak to mame ako 25.07.2008 11:28:53
   var dateTime = s.split(" ");
   //akoze split podla nbsp
   if (dateTime.length == 1) dateTime = s.split(" ");
   
   var day = 0;
   var month = 0;
   var year = 0;
   var hour = 0;
   var minute = 0;
   var second = 0;
   
   if (dateTime.length > 0)
   {
	   var date = dateTime[0].split(".");
	   if (date.length >= 3)
	   {
		   day = date[0];
		   month = date[1];
		   year = date[2];
	   }
   }
   if (dateTime.length > 1)
   {
	   var time = dateTime[1].split(":");
	   if (time.length >= 2)
	   {
		   hour = time[0];
		   minute = time[1];
	   }
	   if (time.length >= 3)
	   {
		   second = time[2];
	   }
   }
	
   //return Date.parse(s.replace(/\-/g, '/'));   
   var date = new Date(Number(year), Number(month)-1, Number(day), Number(hour), Number(minute), Number(second));   
   return date;   
}

/* alternative to number function
 * This one is slower but can handle non numerical characters in
 * the string allow strings like the follow (as well as a lot more)
 * to be used:
 *    "1,000,000"
 *    "1 000 000"
 *    "100cm"
 */

function toNumber(s) 
{
    return Number(s.replace(/[^0-9\.]/g, ""));
}

function compareByColumn(nCol, bDescending, sType) 
{
   var c = nCol;
   var d = bDescending;
   
   var fTypeCast = String;
   
   if (sType == "Number")
      fTypeCast = Number;
   else if (sType == "Date")
      fTypeCast = parseDate;
   else if (sType == "CaseInsensitiveString")
      fTypeCast = CaseInsensitiveString;

   return function (n1, n2) 
   {
      if (fTypeCast(getInnerText(n1.cells[c])) < fTypeCast(getInnerText(n2.cells[c])))
         return d ? -1 : +1;
      if (fTypeCast(getInnerText(n1.cells[c])) > fTypeCast(getInnerText(n2.cells[c])))
         return d ? +1 : -1;
      return 0;
   };
}


function sortColumn(e) 
{

   var tmp, el, tHeadParent;

   if (ie5)
      tmp = e.srcElement;
   else if (dom)
      tmp = e.target;

   tHeadParent = getParent(tmp, "THEAD");
   el = getParent(tmp, "TD");

   if (tHeadParent == null)
      return;
      
   if (el != null) {
      var p = el.parentNode;
      var i;

      if (el._descending)  // catch the null
         el._descending = false;
      else
         el._descending = true;
      
      if (tHeadParent.arrow != null) {
         if (tHeadParent.arrow.parentNode != el) {
            tHeadParent.arrow.parentNode._descending = null;   //reset sort order      
         }
         tHeadParent.arrow.parentNode.removeChild(tHeadParent.arrow);
      }

      if (el._descending)
         tHeadParent.arrow = arrowDown.cloneNode(true);
      else
         tHeadParent.arrow = arrowUp.cloneNode(true);

      el.appendChild(tHeadParent.arrow);        

      // get the index of the td
      for (i=0; i<p.cells.length; i++) 
      {
         if (p.cells[i] == el) break;
      }

      var table = getParent(el, "TABLE");
      // can't fail
      
      sortTable(table,i,el._descending, el.getAttribute("type"));
   }
}


function getInnerText(el) 
{
   if (ie5) return el.innerText; //Not needed but it is faster
   
   var str = "";
   
   for (var i=0; i<el.childNodes.length; i++) {
      switch (el.childNodes.item(i).nodeType) {
         case 1: //ELEMENT_NODE
            str += getInnerText(el.childNodes.item(i));
            break;
         case 3:  //TEXT_NODE
            str += el.childNodes.item(i).nodeValue;
            break;
      }
      
   }
   
   return str;
}

function setInnerText(el, text) 
{
   if (ie5) el.innerText=text; //added by jeeff
}

function getParent(el, pTagName) 
{
   if (el == null) return null;
   else if (el.nodeType == 1 && el.tagName.toLowerCase() == pTagName.toLowerCase()) // Gecko bug, supposed to be uppercase
      return el;
   else
      return getParent(el.parentNode, pTagName);
}