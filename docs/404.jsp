<%
    String path = (String)request.getAttribute("javax.servlet.error.request_uri");
    //System.out.println(path);
    if (path!=null) {
    	String currentYearPrefix = "/v"+java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

    	String[] unknownLanguages = {"cs", "cz", "de"};
		for (String lng : unknownLanguages) {
			String prefix = "/latest/"+lng+"/";
			//System.out.println("prefix="+prefix);
			if (path.startsWith(prefix)) {
				String newLng = "en";
				if (lng.equals("cs") || lng.equals("cz")) newLng = "sk";
				response.sendRedirect("/latest/"+newLng+"/"+path.substring(prefix.length()));
				return;
			}
		}
    	
    	//System.out.println(currentYearPrefix);
    	if (path.startsWith("/latest")) {
		    if (path.contains(".")==false) {
		    	response.setStatus(200);
		    	request.getRequestDispatcher("/latest/index.html").forward(request, response);
		    	return;
		    }
		} else if (path.startsWith(currentYearPrefix)) {
			response.sendRedirect("/latest"+path.substring(currentYearPrefix.length()));
			return;
		}		
	}
%>
Page not found