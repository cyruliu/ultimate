/**
 * 
 */
package de.uni_freiburg.informatik.ultimatetest.traceabstraction;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimatetest.UltimateTestCase;

/**
 * @author heizmann@informatik.uni-freiburg.de
 *
 */
public class PathInvariantsTest extends
		AbstractTraceAbstractionTestSuite {
	
	/**
	 * Limit the number of files per directory.
	 */
	private static int m_FilesPerDirectoryLimit = 10;
	
	private static final DirectoryFileEndingsPair[] m_SVCOMP_Examples = {
//		/*** Category 1. Arrays ***/
//		new DirectoryFileEndingsPair("examples/svcomp/array-examples/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
//		
//		/*** Category 2. Bit Vectors ***/
//		new DirectoryFileEndingsPair("examples/svcomp/bitvector/", new String[]{ ".i", ".c" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/bitvector-regression/", new String[]{ ".i", ".c" }, m_FilesPerDirectoryLimit) ,
//		
//		/*** Category 4. Control Flow and Integer Variables ***/
//		new DirectoryFileEndingsPair("examples/svcomp/ntdrivers-simplified/", new String[]{".c" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/ssh-simplified/", new String[]{".c" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/locks/", new String[]{".c" }, m_FilesPerDirectoryLimit) ,
//		
//		new DirectoryFileEndingsPair("examples/svcomp/loops/", new String[]{".i"}) ,
//		new DirectoryFileEndingsPair("examples/svcomp/loop-acceleration/", new String[]{".c" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/loop-invgen/", new String[]{".i"}, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/loop-lit/", new String[]{ ".i", ".c" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/loop-new/", new String[]{".i"}, m_FilesPerDirectoryLimit) ,
//		
//		new DirectoryFileEndingsPair("examples/svcomp/eca-rers2012/", new String[]{".c" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/product-lines/", new String[]{".c" }, m_FilesPerDirectoryLimit) ,
//		
//		/*** Category 6. Heap Manipulation / Dynamic Data Structures ***/
//		new DirectoryFileEndingsPair("examples/svcomp/heap-manipulation/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/list-properties/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/ldv-regression/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/ddv-machzwd/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
//		
//		/*** Category 7. Memory Safety ***/
//		new DirectoryFileEndingsPair("examples/svcomp/memsafety/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/list-ext-properties/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/memory-alloca/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/memory-unsafe/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
//
//		/*** Category 8. Recursive ***/
//		new DirectoryFileEndingsPair("examples/svcomp/recursive/", new String[]{ ".c" }, m_FilesPerDirectoryLimit) ,
//		
//		/*** Category 9. Sequentialized Concurrent Programs ***/
//		new DirectoryFileEndingsPair("examples/svcomp/systemc/", new String[]{ ".c" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/seq-mthreaded/", new String[]{ ".c" }, m_FilesPerDirectoryLimit) ,
//		new DirectoryFileEndingsPair("examples/svcomp/seq-pthread/", new String[]{ ".i" }, m_FilesPerDirectoryLimit) ,
	};
	
	private static final String[] m_UltimateRepository = {
//		"examples/programs/nonlinearArithmetic",
//		"examples/programs/quantifier",
//		"examples/programs/random",
//		"examples/programs/real-life",
//		"examples/programs/reals",
//		"examples/programs/recursivePrograms",
		"examples/programs/regression",
//		"examples/programs/scalable",
//		"examples/programs/toy",
	};
	
	
	/**
	 * List of path to setting files. 
	 * Ultimate will run on each program with each setting that is defined here.
	 * The path are defined relative to the folder "trunk/examples/settings/",
	 * because we assume that all settings files are in this folder.
	 * 
	 */
	private static final String[] m_Settings = {
		"automizer/pathInvariants/default.epf",
		"automizer/pathInvariants/defaultLbe.epf",
		"automizer/pathInvariants/pathInvariants.epf",
		"automizer/pathInvariants/pathInvariantsLbe.epf",
	};
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getTimeout() {
		return 60 * 1000;
	}
	

	@Override
	public Collection<UltimateTestCase> createTestCases() {
		for (String setting : m_Settings) {
			addTestCases("AutomizerC.xml", 
					setting, 
					m_SVCOMP_Examples);
		}
		
		for (String setting : m_Settings) {
			addTestCases(
					"AutomizerBpl.xml",
					setting,
					m_UltimateRepository,
				    new String[] {".bpl"});
		}
		for (String setting : m_Settings) {
			addTestCases(
					"AutomizerC.xml",
					setting,
					m_UltimateRepository,
				    new String[] {".c", ".i"});
		}
		return super.createTestCases();
	}
	
}
