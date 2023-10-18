if (window.location.href.indexOf("htmlclean.do")!=-1 || window.location.href.indexOf("editor.do")!=-1 || window.location.href.indexOf("FCKeditor")!=-1)
{
	checkFormLoaded = true;
}

if (checkFormLoaded)
{
   //window.alert("skipping");
}
else
{
	var checkFormLoaded = true;	
	
	var el;
	if (typeof jQuery == 'undefined') 
	{  
		el = document.createElement('script');
		el.setAttribute("src", "/components/_common/javascript/jquery.min.js");
		el.setAttribute("type", "text/javascript");
		
		var head = document.getElementsByTagName('head')[0];
		head.appendChild(el);
	}
	
	var checkFormLng = null;
	try
	{
		var actualPath = window.location.pathname;
		if (actualPath.indexOf("/sk/")==0) checkFormLng = "sk";
		else if (actualPath.indexOf("/en/")==0) checkFormLng = "en";
		else if (actualPath.indexOf("/cz/")==0) checkFormLng = "cz";
		else if (actualPath.indexOf("/de/")==0) checkFormLng = "de";
		
		if (checkFormLng == null)
		{
			var metas = document.getElementsByTagName("meta"); 
			var i = 0;
			for (i=0; i<metas.length; i++) 
			{ 
				if (metas[i].getAttribute("http-equiv") == "Content-language")
				{ 
					checkFormLng = metas[i].getAttribute("content"); 					
		        } 
		   }
		}
	}
	catch (e) {}
	
	el = document.createElement('script');
	if (checkFormLng != null && checkFormLng.length==2) el.setAttribute("src", "/components/form/check_form_impl.jsp?lng="+checkFormLng);
	else el.setAttribute("src", "/components/form/check_form_impl.jsp");
	el.setAttribute("type", "text/javascript");
	
	var head = document.getElementsByTagName('head')[0];
	head.appendChild(el);	

	el = document.createElement('link');
	el.setAttribute("href", "/components/form/check_form.css");
	el.setAttribute("type", "text/css");
	el.setAttribute("media", "screen");
	el.setAttribute("rel", "stylesheet");
	
	var head = document.getElementsByTagName('head')[0];
	head.appendChild(el);
}

