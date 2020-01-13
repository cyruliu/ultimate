package webinterface_jetty_test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.uni_freiburg.informatik.ultimate.util.CoreUtil;
import de.uni_freiburg.informatik.ultimate.webbridge.website.Setting;
import de.uni_freiburg.informatik.ultimate.webbridge.website.Tasks;
import de.uni_freiburg.informatik.ultimate.webbridge.website.WebToolchain;
import de.uni_freiburg.informatik.ultimate.webbridge.website.Setting.SettingType;
import de.uni_freiburg.informatik.ultimate.webbridge.website.Tasks.TaskNames;

public class UltimateAPIExecutor {
	private final ServletLogger mLogger;

	/**
	 * Upper bound for the all timeouts that are set by {@link WebToolchain}s. While executing a toolchain Ultimate uses
	 * the minimum of this number and the timeout of the {@link WebToolchain}. Reducing this to a small number is
	 * helpful if the website is running on a computer that is not able to handle many requests in parallel.
	 */
	private static final long TIMEOUT = 24 * 60 * 60 * 1000;
	private File mInputFile;
	private File mToolchainFile;
	private File mSettingsFile;
	public static final boolean DEBUG = !false;

	public UltimateAPIExecutor(final ServletLogger logger) {
		mLogger = logger;
	}

	public JSONObject executeUltimate(final Request internalRequest) throws JSONException {
		mLogger.log("Start executing Ultimate for RequestId: " + internalRequest.getRequestId());
		JSONObject jsonResult = new JSONObject();
		
		// Prepare temporary files.
		try {
			final String timestamp = CoreUtil.getCurrentDateTimeAsString();
			setInputFile(internalRequest, timestamp);
			setToolchainFile(internalRequest, timestamp);
			setSettingsFile(internalRequest, timestamp);
			mLogger.log("Written temporary files to " + mInputFile.getParent() + " with timestamp " + timestamp);
		} catch (IOException e) {
			jsonResult = new JSONObject();
			jsonResult.put("error", "Internal server error: IO");
			mLogger.log("Internal server error: " + e.getClass().getSimpleName());
			mLogger.logDebug(e.toString());

			if (DEBUG) {				
				e.printStackTrace();
			}
			return jsonResult;
		}
		
		try {
			applyUserSettings(internalRequest);
		} catch (IOException e) {
			mLogger.log("Could not apply user settings: " + e.getMessage());
		}
		
		// run ultimate
		// TODO: Allow timeout to be set in the API request.
		final long timeout = Math.min(TIMEOUT, TIMEOUT);
		if (runUltimate(jsonResult, timeout)) {
			mLogger.log("Finished executing Ultimate.");
		} else {
			mLogger.log("Ultimate terminated abnormally.");
		}

		return jsonResult;
	}

	private void setInputFile(Request internalRequest, String timestamp) throws IOException {
		final String code = internalRequest.getSingleParameter("code");
		final String fileExtension = internalRequest.getSingleParameter("code_file_extension");
		mInputFile = writeTemporaryFile(timestamp + "_input", code, fileExtension);
	}

	private void setToolchainFile(Request internalRequest, String timestamp) throws IOException {
		final String ultimate_settings_epf = internalRequest.getSingleParameter("ultimate_settings_epf");
		mSettingsFile = writeTemporaryFile(timestamp + "_settings", ultimate_settings_epf, ".epf");
	}
	
	private void setSettingsFile(Request internalRequest, String timestamp) throws IOException {
		final String ultimate_toolchain_xml = internalRequest.getSingleParameter("ultimate_toolchain_xml");
		mToolchainFile = writeTemporaryFile(timestamp + "_toolchain", ultimate_toolchain_xml, ".xml");		
	}

	private boolean runUltimate(final JSONObject json, final long timeout) throws JSONException {
		try {
			mLogger.log("Starting Ultimate ...");
			final UltimateWebController uwc =
					new UltimateWebController(mLogger, mSettingsFile, mInputFile, mToolchainFile, timeout);
			uwc.runUltimate(json);
		} catch (final Throwable t) {
			mLogger.log("Failed to run Ultimate.");
			json.put("error", "Failed to run ULTIMATE: " + t.getMessage());
			return false;
		} finally {
			postProcessTemporaryFiles();
		}
		return true;
	}

	private void postProcessTemporaryFiles() {
		final File logDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "log" + File.separator);
		if (!logDir.exists()) {
			logDir.mkdir();
		}
		mLogger.log("Moving input, setting and toolchain file to " + logDir.getAbsoluteFile());
		if (mInputFile != null) {
			mInputFile.renameTo(new File(logDir, mInputFile.getName()));
		}
		if (mSettingsFile != null) {
			mSettingsFile.renameTo(new File(logDir, mSettingsFile.getName()));
		}
		if (mToolchainFile != null) {
			mToolchainFile.renameTo(new File(logDir, mToolchainFile.getName()));
		}
	}

	private static File writeTemporaryFile(final String name, final String content, final String fileExtension)
			throws IOException {
		final File codeFile = File.createTempFile(name, fileExtension);
		try (final Writer fstream = new OutputStreamWriter(new FileOutputStream(codeFile), StandardCharsets.UTF_8)) {
			fstream.write(content);
		}
		return codeFile;
	}

	private void applyUserSettings(final Request internalRequest) throws IOException {
		try {
			mLogger.log("Apply user settings to run configuration.");
			final JSONObject jsonParameter = new JSONObject(internalRequest.getSingleParameter("user_settings"));
			final JSONArray userSettings = jsonParameter.getJSONArray("user_settings");
			BufferedWriter settingsOutput = new BufferedWriter(new FileWriter(mSettingsFile.getAbsolutePath(), true));
			
			settingsOutput.newLine();
			settingsOutput.write("# User settings from the webfrontend: ");
			for (int i=0; i < userSettings.length(); i++) {
			    final JSONObject userSetting = userSettings.getJSONObject(i);
			    switch (userSetting.getString("type")) {
				case "bool":
					settingsOutput.newLine();
					settingsOutput.write(userSetting.getString("string") + "=" + userSetting.getBoolean("checked"));
					break;
				default:
					mLogger.log("User setting type " + userSetting.getString("type") + " is unknown. Ignoring");
				}
			}
			settingsOutput.newLine();
			settingsOutput.close();			
		} catch (JSONException e) {
			mLogger.log("Could not fetch user settings: " + e.getMessage());
		}
		
	}
}
