/*
 * Copyright (c) 2014 Meding Software Technik -- All Rights Reserved.
 */
package com.uwemeding.mojo.fuzzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Scans source directories for Fuzzer grammar files.
 * <p>
 * @author uwe
 */
public class FuzzerDirectoryScanner {

	/**
	 * The directory scanner used to scan the source directory for files.
	 */
	private final DirectoryScanner scanner;

	/**
	 * The absolute path to the output directory used to detect stale target
	 * files by timestamp checking, may be <code>null</code> if no stale
	 * detection should be performed.
	 */
	private File outputDirectory;

	// TODO: Once the parameter "packageName" from the fuzzer mojo has been deleted, remove this field, too.
	/**
	 * The package name for the generated parser, may be <code>null</code> to
	 * use the package declaration from the grammar file.
	 */
	private String parserPackage;

	/**
	 * The granularity in milliseconds of the last modification date for testing
	 * whether a grammar file needs recompilation because its corresponding
	 * target file is stale.
	 */
	private int staleMillis;

	/**
	 * A set of grammar infos describing the included grammar files, must never
	 * be <code>null</code>.
	 */
	private final List<FuzzerInfo> includedGrammars;

	/**
	 * Creates a new grammar directory scanner.
	 */
	public FuzzerDirectoryScanner() {
		this.scanner = new DirectoryScanner();
		this.scanner.setFollowSymlinks(true);
		this.includedGrammars = new ArrayList<>();
	}

	/**
	 * Sets the absolute path to the source directory to scan for grammar files.
	 * This directory must exist or the scanner will report an error.
	 * <p>
	 * @param directory The absolute path to the source directory to scan, must
	 *                  not be <code>null</code>.
	 */
	public void setSourceDirectory(File directory) {
		if (!directory.isAbsolute()) {
			throw new IllegalArgumentException("source directory is not absolute: " + directory);
		}
		this.scanner.setBasedir(directory);
	}

	/**
	 * Sets the package name for the generated parser.
	 * <p>
	 * @param packageName The package name for the generated parser, may be
	 *                    <code>null</code> to use the package declaration from
	 *                    the grammar file.
	 */
	public void setParserPackage(String packageName) {
		this.parserPackage = packageName;
	}

	/**
	 * Sets the Ant-like inclusion patterns.
	 * <p>
	 * @param includes The set of Ant-like inclusion patterns, may be
	 *                 <code>null</code> to include all files.
	 */
	public void setIncludes(String[] includes) {
		this.scanner.setIncludes(includes);
	}

	/**
	 * Sets the Ant-like exclusion patterns.
	 * <p>
	 * @param excludes The set of Ant-like exclusion patterns, may be
	 *                 <code>null</code> to exclude no files.
	 */
	public void setExcludes(String[] excludes) {
		this.scanner.setExcludes(excludes);
		this.scanner.addDefaultExcludes();
	}

	/**
	 * Sets the absolute path to the output directory used to detect stale
	 * target files.
	 * <p>
	 * @param directory The absolute path to the output directory used to detect
	 *                  stale target files by timestamp checking, may be
	 *                  <code>null</code> if no stale detection should be
	 *                  performed.
	 */
	public void setOutputDirectory(File directory) {
		if (directory != null && !directory.isAbsolute()) {
			throw new IllegalArgumentException("output directory is not absolute: " + directory);
		}
		this.outputDirectory = directory;
	}

	/**
	 * Sets the granularity in milliseconds of the last modification date for
	 * stale file detection.
	 * <p>
	 * @param milliseconds The granularity in milliseconds of the last
	 *                     modification date for testing whether a grammar file
	 *                     needs recompilation because its corresponding target
	 *                     file is stale.
	 */
	public void setStaleMillis(int milliseconds) {
		this.staleMillis = milliseconds;
	}

	/**
	 * Scans the source directory for grammar files that match at least one
	 * inclusion pattern but no exclusion pattern, optionally performing
	 * timestamp checking to exclude grammars whose corresponding parser files
	 * are up to date.
	 * <p>
	 * @throws IOException If a grammar file could not be analyzed for metadata.
	 */
	public void scan() throws IOException {
		this.includedGrammars.clear();
		this.scanner.scan();

		String[] includedFiles = this.scanner.getIncludedFiles();
		for (String includedFile : includedFiles) {
			FuzzerInfo grammarInfo = new FuzzerInfo(this.scanner.getBasedir(), includedFile, this.parserPackage);
			if (this.outputDirectory != null) {
				File sourceFile = grammarInfo.getInputFile();
				File[] targetFiles = getTargetFiles(this.outputDirectory, includedFile, grammarInfo);
				for (File targetFile : targetFiles) {
					if (!targetFile.exists()
							|| targetFile.lastModified() + this.staleMillis < sourceFile.lastModified()) {
						this.includedGrammars.add(grammarInfo);
						break;
					}
				}
			} else {
				this.includedGrammars.add(grammarInfo);
			}
		}
	}

	/**
	 * Determines the output files corresponding to the specified grammar file.
	 * <p>
	 * @param targetDirectory The absolute path to the output directory for the
	 *                        target files, must not be <code>null</code>.
	 * @param grammarFile     The path to the grammar file, relative to the
	 *                        scanned source directory, must not be
	 *                        <code>null</code>.
	 * @param grammarInfo     The grammar info describing the grammar file, must
	 *                        not be <code>null</code>
	 * @return A file array with target files, never <code>null</code>.
	 */
	protected File[] getTargetFiles(File targetDirectory, String grammarFile, FuzzerInfo grammarInfo) {
		File parserFile = new File(targetDirectory, grammarInfo.getParserFile());
		return new File[]{parserFile};
	}

	/**
	 * Gets the grammar files that were included by the scanner during the last
	 * invocation of {@link #scan()}.
	 * <p>
	 * @return An array of grammar infos describing the included grammar files,
	 *         will be empty if no files were included but is never
	 *         <code>null</code>.
	 */
	public FuzzerInfo[] getIncludedGrammars() {
		return this.includedGrammars.toArray(new FuzzerInfo[0]);
	}

}
