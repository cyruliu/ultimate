package de.uni_freiburg.informatik.ultimate.web.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Dictionary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.Activator;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.PluginFactory;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.SettingsManager;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.ToolchainManager;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.services.ToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.ToolchainData;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchain;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchainData;
import de.uni_freiburg.informatik.ultimate.core.model.IUltimatePlugin;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILoggingService;
import de.uni_freiburg.informatik.ultimate.util.CoreUtil;
import de.uni_freiburg.informatik.ultimate.web.backend.util.APIResponse;
import de.uni_freiburg.informatik.ultimate.web.backend.util.GetAPIrequest;


public class UltimateAPIServlet extends HttpServlet implements ICore<RunDefinition>, IUltimatePlugin {
	
	private static final long serialVersionUID = 1L;
	private static final boolean DEBUG = !false;
	private final ServletLogger mLogger;
	private ToolchainManager mToolchainManager;
	private ToolchainStorage mCoreStorage;
	private SettingsManager mSettingsManager;
	private PluginFactory mPluginFactory;
	private ILoggingService mLoggingService;
	private String mUltimateVersion;
	
	/**
	 * Constructor.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public UltimateAPIServlet() {
		super();
		mLogger = new ServletLogger(this, "Servlet", DEBUG);
		mCoreStorage = new ToolchainStorage();
		mLoggingService = mCoreStorage.getLoggingService();
		mSettingsManager = new SettingsManager(mLogger);
		mSettingsManager.registerPlugin(this);
		mPluginFactory = new PluginFactory(mSettingsManager, mLogger);
	}
	
	/**
	 * Process GET requests
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		mLogger.logDebug("Connection from " + request.getRemoteAddr() + ", GET: " + request.getQueryString());
		
		processAPIGetRequest(request, response);
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		mLogger.logDebug("Connection from " + request.getRemoteAddr() + ", POST: " + request.getRequestURI());
		final ServletLogger sessionLogger = new ServletLogger(this, request.getSession().getId(), DEBUG);
		final Request internalRequest = new Request(request, sessionLogger);
		
		processAPIPostRequest(internalRequest, response);
	}
	
	
	private void processAPIGetRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		GetAPIrequest apiRequest = new GetAPIrequest(request);
		APIResponse apiResponse = new APIResponse(response);
		
		try {
			switch (apiRequest.ressourceType) {
			case VERSION:
				apiResponse.put("ultimate_version", this.getUltimateVersionString());
				break;
			case JOB:
				handleJobGetRequest(apiRequest, apiResponse);
				break;
			default:
				apiResponse.setStatusError();
				apiResponse.setMessage("unknown request.");
				break;
			}
			apiResponse.write();
		} catch (Exception e) {
			apiResponse.invalidRequest(e.getMessage());
			if (DEBUG) {
				e.printStackTrace();
			}
		}
	}

	private void handleJobGetRequest(GetAPIrequest apiRequest, APIResponse apiResponse) 
			throws JSONException, IOException {
		if (apiRequest.urlParts.length < 4) {
			apiResponse.setStatusError();
			apiResponse.setMessage("No JobId provided.");
			return;
		}
		
		String jobId = apiRequest.urlParts[3];
		
		switch (apiRequest.taskType) {
		case GET:
			JobResult jobResult = new JobResult(jobId);
			jobResult.load();
			apiResponse.mergeJSON(jobResult.getJson());
			break;
		case DELETE:
			boolean canceled = cancelToolchainJob(jobId);
			String message = (canceled) ? "Job " + jobId + " canceled." : "No unfinished job " + jobId + " found."; 
			apiResponse.setMessage(message);
			break;
		default:
			apiResponse.setStatusError();
			apiResponse.setMessage("Task not supported for ressource " + apiRequest.ressourceType);
			break;
		}
	}

	/**
	 * Pass the Request to initiateUltimateRun.
	 * Write results to responseWriter.
	 * 
	 * @param internalRequest
	 * @param responseWriter
	 */
	private void processAPIPostRequest(Request internalRequest, HttpServletResponse response) throws IOException {
		APIResponse apiResponse = new APIResponse(response);
		
		try {
			mLogger.logDebug("Process API POST request.");

			if (internalRequest.getParameterList().containsKey("action")) {
				mLogger.logDebug("Initiate ultimate run for request: " + internalRequest.toString());
				apiResponse.mergeJSON(initiateUltimateRun(internalRequest));
			} else {
				apiResponse.setStatusError();
				apiResponse.setMessage("Invalid request: Missing `action` parameter.");
			}
			apiResponse.write();
		} catch (final JSONException e) {
			apiResponse.invalidRequest(e.getMessage());
			if (DEBUG) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Initiate ultimate run for the request.
	 * Return the results as a json object.
	 * 
	 * @param internalRequest
	 * @return
	 * @throws JSONException
	 */
	private JSONObject initiateUltimateRun(Request internalRequest) throws JSONException {
		try {
			final String action = internalRequest.getSingleParameter("action");
			if (action.equals("execute")) {
				final JSONObject json = new JSONObject();
				json.put("requestId", internalRequest.getRequestId());
				json.put("status", "creating");
				final UltimateAPIController controller = new UltimateAPIController(internalRequest, json);				
				int status = controller.init(this);
				mToolchainManager = new ToolchainManager(mLoggingService, mPluginFactory, controller);
				if (status == 0) {
					controller.run();
				}
				mToolchainManager.close();
				return json;
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
	
	private boolean cancelToolchainJob(String jobId) {
		Job[] jobs = getPendingToolchainJobs();
		for (int i = 0; i < jobs.length; i++) {
			WebBackendToolchainJob job = (WebBackendToolchainJob) jobs[i];
			if (job.getId().equals(jobId)) {
				job.cancelToolchain();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Jobs (by family "WebBackendToolchainJob") running or queued.
	 * @return
	 */
	private Job[] getPendingToolchainJobs() {
		IJobManager jobManager = Job.getJobManager();
		Job[] jobs = jobManager.find("WebBackendToolchainJob");

		return jobs;
	}

	/***************************** ICore Implementation *********************/
	
	@Override
	public IToolchainData<RunDefinition> createToolchainData(String filename)
			throws FileNotFoundException, JAXBException, SAXException {
		if (!new File(filename).exists()) {
			throw new FileNotFoundException("The specified toolchain file " + filename + " was not found");
		}

		final ToolchainStorage tcStorage = new ToolchainStorage();
		return new ToolchainData(filename, tcStorage, tcStorage);
	}

	@Override
	public IToolchainData<RunDefinition> createToolchainData() {
		return null;
	}

	@Override
	public IToolchain<RunDefinition> requestToolchain(File[] inputFiles) {
		return mToolchainManager.requestToolchain(inputFiles);
	}

	@Override
	public void releaseToolchain(IToolchain<RunDefinition> toolchain) {
		mToolchainManager.releaseToolchain(toolchain);
	}

	@Override
	public void savePreferences(String absolutePath) {
		
	}

	@Override
	public void loadPreferences(String absolutePath, boolean silent) {
		
	}

	@Override
	public void resetPreferences(boolean silent) {
		
	}

	@Override
	public IUltimatePlugin[] getRegisteredUltimatePlugins() {
		return null;
	}

	@Override
	public String[] getRegisteredUltimatePluginIDs() {
		return null;
	}

	@Override
	public ILoggingService getCoreLoggingService() {
		return null;
	}

	@Override
	public IPreferenceProvider getPreferenceProvider(String pluginId) {
		return null;
	}

	@Override
	public String getUltimateVersionString() {
		if (mUltimateVersion == null) {
			final Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
			if (bundle == null) {
				return "UNKNOWN";
			}
			final Dictionary<String, String> headers = bundle.getHeaders();
			if (headers == null) {
				return "UNKNOWN";
			}

			final String major = headers.get("Bundle-Version");
			final String gitVersion = CoreUtil.readGitVersion(getClass().getClassLoader());
			if (gitVersion == null) {
				return major;
			}
			mUltimateVersion = major + "-" + gitVersion;
		}
		return mUltimateVersion;
	}

	/************************* End ICore Implementation *********************/
	
	/************************* IUltimatePlugin Implementation *********************/

	@Override
	public String getPluginName() {
		return Activator.PLUGIN_NAME;
	}

	@Override
	public String getPluginID() {
		return Activator.PLUGIN_ID;
	}

	@Override
	public IPreferenceInitializer getPreferences() {
		return null;
	}
	
	/************************* End IUltimatePlugin Implementation *********************/
	
}
