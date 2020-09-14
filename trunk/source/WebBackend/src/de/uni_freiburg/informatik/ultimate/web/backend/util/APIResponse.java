package de.uni_freiburg.informatik.ultimate.web.backend.util;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

enum APIResponseStatus {
	SUCCESS, ERROR
}

public class APIResponse {
	private JSONObject mJSON = new JSONObject();
	private PrintWriter mPrintWriter;
	
	public APIResponse(HttpServletResponse response) throws IOException {
		prepareJSONWiter(response);
		setStatus(APIResponseStatus.SUCCESS);
	}
	
	private void prepareJSONWiter(HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		mPrintWriter = response.getWriter();
	}

	public void write() throws JSONException {
		mJSON.write(mPrintWriter);
	}
	
	private void setStatus(APIResponseStatus status) {
		try {
			mJSON.put("status", status.name());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void setMessage(String message) throws JSONException {
		mJSON.put("msg", message);
	}

	public void put(String key, String value) throws JSONException {
		mJSON.put(key, value);
	}

	public void invalidRequest(String message) {
		try {
			setStatus(APIResponseStatus.ERROR);
			setMessage("Invalid request: " + message);
			write();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void setStatusError() {
		setStatus(APIResponseStatus.ERROR);		
	}
	
	public void setStatusSuccess() {
		setStatus(APIResponseStatus.SUCCESS);
	}

	/**
	 * Merges given JSON into the response.
	 * @param json
	 * @throws JSONException
	 */
	public void mergeJSON(JSONObject json) throws JSONException {
		for (String key : JSONObject.getNames(json)) {
			mJSON.put(key, json.get(key));
		}
	}
}
