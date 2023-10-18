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
