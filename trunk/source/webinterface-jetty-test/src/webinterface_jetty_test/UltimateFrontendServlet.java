package webinterface_jetty_test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UltimateFrontendServlet extends HttpServlet {
	
	private final ServletLogger mLogger;
	private static final boolean DEBUG = !false;
	
	/**
	 * Constructor.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public UltimateFrontendServlet() {
		super();
		mLogger = new ServletLogger(this, "Servlet", DEBUG);
	}
	
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
    }
}
