package de.uni_freiburg.informatik.ultimate.web.backend;

import javax.servlet.http.HttpServletRequest;

public class getAPIrequest {
	private String ressource;
	
	public getAPIrequest(HttpServletRequest request) {
		setFromUrl(request.getPathInfo());
	}

	private void setFromUrl(String pathInfo) {
		// Get the URL parts. A request url might look like /api/job/job_id.
		// urlParts = {"", "job", "job_id"} in this example.
		String[] urlParts = (pathInfo != null) ? pathInfo.split("/") : null;
		ressource = (urlParts != null) ? urlParts[1] : null;
	}
}
