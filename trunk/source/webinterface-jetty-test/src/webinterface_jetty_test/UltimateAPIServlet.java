package webinterface_jetty_test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;


public class UltimateAPIServlet extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Whether the API should be executed in Debug-Mode or not.
	 */
	private static final boolean DEBUG = !false;
	
	private final ServletLogger mLogger;
	
	/**
	 * Constructor.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public UltimateAPIServlet() {
		super();
		mLogger = new ServletLogger(this, "Servlet", DEBUG);
	}
	
	/**
	 * Process GET requests
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		mLogger.logDebug("Connection from " + request.getRemoteAddr() + ", GET: " + request.getQueryString());
	}
	
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		mLogger.logDebug("Connection from " + request.getRemoteAddr() + ", POST: " + request.getRequestURI()
				+ " Reading All Request Parameters");
		processPOSTRequest(request, response);
	}

	/**
	 * Handles POST requests sent to the API.
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void processPOSTRequest(HttpServletRequest request, HttpServletResponse response) 
		throws IOException{
		final ServletLogger sessionLogger = new ServletLogger(this, request.getSession().getId(), DEBUG);
		final Request internalRequest = new Request(request, sessionLogger);
		
		// Prepare the response to be a JSON response.
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		final PrintWriter responseWriter = response.getWriter();
		processAPIRequest(internalRequest, responseWriter);
	}

	private void processAPIRequest(Request internalRequest, PrintWriter responseWriter) {
		try {
			JSONObject jsonResult = new JSONObject();
			if (internalRequest.getParameterList().containsKey("action")) {
				jsonResult = handleAPIAction(internalRequest);
			} else {
				jsonResult.put("error", "Invalid request: Missing `action` parameter.");
			}

			jsonResult.write(responseWriter);

		} catch (final JSONException e) {
			final String message = "{\"error\" : \"Invalid request: " + e.getMessage() + " \"}";
			responseWriter.print(message);
			internalRequest.getLogger().logDebug(message);
			
			if (DEBUG) {
				e.printStackTrace();
			}
		}
	}

	private JSONObject handleAPIAction(Request internalRequest) throws JSONException {
		try {
			final String action = internalRequest.getSingleParameter("action");
			if (action.equals("execute")) {
				final UltimateAPIExecutor executor = new UltimateAPIExecutor(internalRequest.getLogger());
				return executor.executeUltimate(internalRequest);
			} else {
				internalRequest.getLogger().logDebug("Don't know how to handle action: " + action);
				final JSONObject json = new JSONObject();
				json.put("error", "Invalid request: Unknown `action` parameter ( " + action + ").");
				return json;
			}
		} catch (IllegalArgumentException e) {
			final JSONObject json = new JSONObject();
			json.put("error", "Invalid request: " + e.getMessage());
			return json;
		}
	}
}
