package de.uni_freiburg.informatik.ultimate.webinterfacejetty;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class UltimateWebInterface extends HttpServlet
{
	private static final String CALLBACK = "callback";
	/**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Whether the Servlet should be executed in Debug-Mode or not.
	 */
	private static final boolean DEBUG = !false;

//	private final ServletLogger mLogger;

	public UltimateWebInterface() {
		super();
	}
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>Ultimate Servlet</h1>");
        response.getWriter().println("session=" + request.getSession(true).getId());
    }
}
