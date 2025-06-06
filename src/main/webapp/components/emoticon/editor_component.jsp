<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<% request.setAttribute("cmpName", "emoticon"); %>
<jsp:include page="/components/top.jsp"/>

<script type='text/javascript'>

function Ok()
{
	image = document.getElementById("imgPath").value;
	oEditor.FCK.InsertHtml( "<img src='/components/emoticon/"+image+"' border='0'>" ) ;
	return true ;
} // End function

if (isFck)
{

}
else
{
	resizeDialog(540, 380);
}
</script>

<script type="text/javascript">
function setLabelValue(refe)
{
  var label= document.getElementById("imgPath");
  label.value = refe;
}

</script>
<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">

<TABLE border=0 align="center"><tr>
<%
//nacitaj vsetky obrazky v adresari a vypis ich do tabulky
String dirPath = sk.iway.iwcm.Tools.getRealPath("/components/emoticon/");
if (dirPath!=null)
{
   File dir = new File(dirPath);
   File image;
   File files[] = dir.listFiles();

   if(files != null) {
      int size = files.length;
      int maxCols = 20;
      int i;
      boolean hasMenuIcon = false;
      int colCounter = 0;
      for (i=0; i<size; i++)
      {
         image = files[i];
         if (image.isFile()==false)
         {
            continue;
         }
         if (image.getName().endsWith(".gif")==false)
         {
            continue;
         }
         if (image.getName().equals("menuicon.gif") || image.getName().equals("editoricon.gif"))
         {
            continue;
         }

         if ((i+1)<size)
         {
            out.println("<td><img src='"+image.getName()+"' onclick=setLabelValue('"+image.getName()+"')></td>");
         }
         else
         {
            //dopo
            out.println("<td>&nbsp;</td>");
         }
         colCounter++;

         if (colCounter == maxCols)
         {
            out.println("</tr>\n");
            if ((i+1)<size)
            {
               out.println("<tr>");
            }
            colCounter = 0;
         }
      }

      if (colCounter>0)
      {
         for (i=colCounter; i<maxCols; i++)
         {
            out.println("<td>&nbsp;</td>");
         }
         out.println("</tr>\n");
      }
   }
}

%>

</table>
<br>
<center>
   <input type="text" id="imgPath" style="width:250px">
</center>

</div>
</div>

<jsp:include page="/components/bottom.jsp"/>