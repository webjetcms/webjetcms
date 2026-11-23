<%
    String path = (String)request.getAttribute("jakarta.servlet.error.request_uri");
    System.out.println("404.jsp, path="+path);
    if (path!=null) {

		// /latest/sk/https://img.youtube.com/vi/q8xs3qDq-G4/hqdefault.jpg
		if (path.contains("https://img.youtube.com/vi/")) {
			response.sendRedirect(path.substring(path.indexOf("https://img.youtube.com/vi/")));
			return;
		}

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
		int v = sk.iway.iwcm.Tools.getIntValue(request.getParameter("v"), 1);
	    response.sendRedirect("/latest/style.css?v="+v);
	    return;
	}

	//System.out.println(currentYearPrefix);
	if (path.startsWith("/latest")) {
	    if (path.contains(".")==false) {
	    	response.setStatus(200);
	    	request.getRequestDispatcher("/latest/index.html").forward(request, response);
	    	return;
	    }
	} else if (path.startsWith("/v2025")) {
		if (path.contains(".")==false) {
	    	response.setStatus(200);
	    	request.getRequestDispatcher("/v2025/index.html").forward(request, response);
	    	return;
	    }
		if (path.contains("/sk/") || path.contains("/en/") || path.contains("/cs/")) {
			response.sendRedirect("/v2025"+path.substring(currentYearPrefix.length()));
		} else {
			response.sendRedirect("/v2025/sk"+path.substring(currentYearPrefix.length()));
		}
		return;
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
