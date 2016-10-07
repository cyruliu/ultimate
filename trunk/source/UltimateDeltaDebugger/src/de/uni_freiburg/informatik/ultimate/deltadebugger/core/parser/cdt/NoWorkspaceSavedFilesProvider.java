package de.uni_freiburg.informatik.ultimate.deltadebugger.core.parser.cdt;

import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;

/**
 * The original SavedFilesProvider implementation accesses the current workspace
 * to read included files, which may not be initialized and so it crashes.
 */
@SuppressWarnings("restriction")
public class NoWorkspaceSavedFilesProvider extends InternalFileContentProvider {

	public NoWorkspaceSavedFilesProvider() {
	}

	@Override
	public InternalFileContent getContentForInclusion(String path, IMacroDictionary macroDictionary) {
		if (!getInclusionExists(path))
			return null;

		return (InternalFileContent) FileContent.createForExternalFileLocation(path);
	}

	@Override
	public InternalFileContent getContentForInclusion(IIndexFileLocation ifl, String astPath) {
		return (InternalFileContent) FileContent.create(ifl);
	}
}
