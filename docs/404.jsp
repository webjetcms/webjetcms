<%
    String path = (String)request.getAttribute("javax.servlet.error.request_uri");
    //System.out.println(path);
    if (path!=null) {
    	String currentYearPrefix = "/v"+java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

    	String[] unknownLanguages = {"cz, de"};
		for (String lng : unknownLanguages) {
			String prefix = "/latest/"+lng+"/";
			//System.out.println("prefix="+prefix);
			if (path.startsWith(prefix)) {
				String newLng = "en";
				if (lng.equals("cz")) newLng = "cs";
				response.sendRedirect("/latest/"+newLng+"/"+path.substring(prefix.length()));
				return;
			}
		}

		if ("/style.css".equals(path)) {
			response.sendRedirect("/latest/style.css");
			return;
		}

    	//System.out.println(currentYearPrefix);
    	if (path.startsWith("/latest")) {
		    if (path.contains(".")==false) {
		    	response.setStatus(200);
		    	request.getRequestDispatcher("/latest/index.html").forward(request, response);
		    	return;
		    }
		} else if (path.startsWith("/v2024")) {
			if (path.contains(".")==false) {
		    	response.setStatus(200);
		    	request.getRequestDispatcher("/v2024/index.html").forward(request, response);
		    	return;
		    }
			if (path.contains("/sk/") || path.contains("/en/") || path.contains("/cs/")) {
				response.sendRedirect("/v2024"+path.substring(currentYearPrefix.length()));
			} else {
				response.sendRedirect("/v2024/sk"+path.substring(currentYearPrefix.length()));
			}
			return;
		} else if (path.startsWith(currentYearPrefix)) {
			if (path.contains("/sk/") || path.contains("/en/") || path.contains("/cs/")) {
				response.sendRedirect("/latest"+path.substring(currentYearPrefix.length()));
			} else {
				response.sendRedirect("/latest/sk"+path.substring(currentYearPrefix.length()));
			}
			return;
		}
	}
%>
Page not found