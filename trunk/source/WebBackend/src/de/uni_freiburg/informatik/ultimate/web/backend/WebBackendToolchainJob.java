package de.uni_freiburg.informatik.ultimate.web.backend;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONException;
import org.json.JSONObject;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.Activator;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.RcpProgressMonitorWrapper;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.exceptions.ParserInitializationException;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.toolchain.DefaultToolchainJob;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.IController;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchainData;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchainProgressMonitor;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchain.ReturnCode;

public class WebBackendToolchainJob extends DefaultToolchainJob {

	private JSONObject mResult;
	private ServletLogger mLogger;
	private Request mRequest;
	private File mToolchainFile;
	private String mId;

	public WebBackendToolchainJob(String name, ICore<RunDefinition> core, IController<RunDefinition> controller,
			ServletLogger logger, File[] input, JSONObject result, Request request, File toolchainFile, String id) {
		super(name, core, controller, logger, input);
		mResult = result;
		mLogger = logger;
		mRequest = request;
		mToolchainFile = toolchainFile;
		mId = id;
	}
	
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		final IToolchainProgressMonitor tpm = RcpProgressMonitorWrapper.create(monitor);
		tpm.beginTask(getName(), IProgressMonitor.UNKNOWN);

		try {
			setToolchain(mCore.requestToolchain(mInputFiles));
			tpm.worked(1);

			mToolchain.init(tpm);
			tpm.worked(1);

			if (!mToolchain.initializeParsers()) {
				throw new ParserInitializationException();
			}
			tpm.worked(1);

			final IToolchainData<RunDefinition> chain = mToolchain.makeToolSelection(tpm);
			if (chain == null) {
				mLogger.fatal("Toolchain selection failed, aborting...");
				return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, IStatus.CANCEL, "Toolchain selection canceled",
						null);
			}
			setServices(chain.getServices());
			tpm.worked(1);

			mToolchain.runParsers();
			tpm.worked(1);

			return convert(mToolchain.processToolchain(tpm));
		} catch (final Throwable e) {
			mLogger.log("Error running the Toolchain: " + e.getMessage());
			return handleException(e);
		} finally {
			tpm.done();
			releaseToolchain();
		}
	}
	
	@Override
	protected IStatus convert(final ReturnCode result) {
		switch (result) {
		case Ok:
		case Cancel:
			try {
				UltimateResultProcessor.processUltimateResults(
						mLogger, mToolchain.getCurrentToolchainData().getServices(), mResult);
				cleanupTempFiles();
				storeResults();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return Status.OK_STATUS;
		case Error:
		default:
			return super.convert(result);
		}
	}
	
	@Override
    public boolean belongsTo(Object family) {
       return family == "WebBackendToolchainJob";
    }
	
	private void storeResults() {
		try {
			JobResult jobResult = new JobResult(mId);
			mResult.put("status", "done");
			jobResult.setJson(mResult);
			jobResult.store();
            mLogger.log("Stored tollchain result to: " + jobResult.getFilePath()); 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}

	private void cleanupTempFiles() {
		final File logDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "log" + File.separator);
		if (!logDir.exists()) {
			logDir.mkdir();
		}
		mLogger.log("Moving input, setting and toolchain file to " + logDir.getAbsoluteFile());
		for (int i = 0; i < mInputFiles.length; i++) {
			File file = mInputFiles[i];
			file.renameTo(new File(logDir, file.getName()));
		}
		if (mToolchainFile != null) {
			mToolchainFile.renameTo(new File(logDir, mToolchainFile.getName()));
		}
	}

	public String getId() {
		return mId;
	}

}
