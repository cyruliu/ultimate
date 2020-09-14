package de.uni_freiburg.informatik.ultimate.web.backend;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;

public class JobResult {
	private final String mJobId;
	private JSONObject mJsonResult = new JSONObject();
	
	public JobResult(String jobId) {
		mJobId = jobId;
		validateJobId();
	}

	private void validateJobId() {
		String cleanedId = mJobId.replaceAll("\\W+", "");
		if (cleanedId != mJobId) {
			throw new IllegalArgumentException();
		}
	}
	
	String getFilePath() {
		String filepath = System.getProperty("java.io.tmpdir") + File.separator + "log" + File.separator;
        filepath += mJobId + ".result.json";
        return filepath;
	}
	
	public void store() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(getFilePath()));
        writer.write(mJsonResult.toString());
        writer.close();
	}
	
	public void load() throws JSONException, IOException {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(getFilePath()));
	        String resultString = new String(encoded);
	        mJsonResult = new JSONObject(resultString);
		} catch (IOException e) {
			mJsonResult = new JSONObject();
			mJsonResult.put("error", "Job not found.");
		}
	}
	
	public JSONObject getJson() {
		return mJsonResult;
	}

	public void setJson(JSONObject jsonObject) {
		mJsonResult = jsonObject;
	}
}
