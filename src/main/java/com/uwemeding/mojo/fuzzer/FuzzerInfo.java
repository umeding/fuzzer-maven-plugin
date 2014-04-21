/*
 * Copyright (c) 2014 Meding Software Technik -- All Rights Reserved.
 */
package com.uwemeding.mojo.fuzzer;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.FileUtils;

/**
 * This bean holds some output related information about a Fuzzer grammar file.
 * It assists in determining the exact output location for the generated parser
 * file.
 * <p>
 * @author uwe
 */
public final class FuzzerInfo {

	/**
	 * The absolute path to the base directory in which the grammar file
	 * resides.
	 */
	private final File sourceDirectory;

	/**
	 * The path to the grammar file (relative to its source directory, e.g.
	 * "fpl/MyFuzzer.fpl").
	 */
	private final String inputFile;

	/**
	 * The declared package for the generated fuzzy calculator (e.g. "com.uwemeding").
	 */
	private final String fuzzerPackage;

	/**
	 * The path to the directory of the fuzzer package (relative to a source
	 * root directory, e.g. "com/uwemeding").
	 */
	private final String fuzzerDirectory;

	/**
	 * The simple name of the generated parser (e.g. "MyFuzzer").
	 */
	private final String fuzzerName;

	/**
	 * The path to the generated parser file (relative to a source root
	 * directory, e.g. "com/uwemeding/MyFuzzer.java").
	 */
	private final String fuzzerFile;

	/**
	 * Creates a new info from the specified grammar file.
	 * <p>
	 * @param sourceDir The absolute path to the base directory in which the
	 *                  grammar file resides, must not be <code>null</code>.
	 * @param inputFile The path to the grammar file (relative to the source
	 *                  directory), must not be <code>null</code>.
	 * @throws IOException If reading the grammar file failed.
	 */
	public FuzzerInfo(File sourceDir, String inputFile)
			throws IOException {
		this(sourceDir, inputFile, null);
	}

	/**
	 * Creates a new info from the specified grammar file.
	 * <p>
	 * @param sourceDir   The absolute path to the base directory in which the
	 *                    grammar file resides, must not be <code>null</code>.
	 * @param inputFile   The path to the grammar file (relative to the source
	 *                    directory), must not be <code>null</code>.
	 * @param packageName The package name for the generated parser, may be
	 *                    <code>null</code> to use the package declaration from
	 *                    the grammar file.
	 * @throws IOException If reading the grammar file failed.
	 */
	public FuzzerInfo(File sourceDir, String inputFile, String packageName)
			throws IOException {
		if (!sourceDir.isAbsolute()) {
			throw new IllegalArgumentException("source directory is not absolute: " + sourceDir);
		}
		this.sourceDirectory = sourceDir;

		File inFile = new File(inputFile);
		if (!inFile.isAbsolute()) {
			this.inputFile = inFile.getPath();
		} else if (inFile.getPath().startsWith(sourceDir.getPath())) {
			this.inputFile = inFile.getPath().substring(sourceDir.getPath().length() + 1);
		} else {
			throw new IllegalArgumentException("input file is not relative to source directory:" + inputFile);
		}

		// NOTE: Fuzzer uses the platform default encoding to read files, so must we
		String grammar = FileUtils.fileRead(getInputFile());

		// TODO: Once the parameter "packageName" from the fuzzer mojo has been deleted, remove our parameter, too.
		if (packageName == null) {
			this.fuzzerPackage = findPackageName(grammar);
		} else {
			this.fuzzerPackage = packageName;
		}

		this.fuzzerDirectory = this.fuzzerPackage.replace('.', File.separatorChar);

		String name = findProgramName(grammar);
		if (name.length() <= 0) {
			this.fuzzerName = FileUtils.removeExtension(inFile.getName());
		} else {
			this.fuzzerName = name;
		}

		if (this.fuzzerDirectory.length() > 0) {
			this.fuzzerFile = new File(this.fuzzerDirectory, this.fuzzerName + ".java").getPath();
		} else {
			this.fuzzerFile = this.fuzzerName + ".java";
		}
	}

	/**
	 * Extracts the declared package name from the specified grammar file.
	 * <p>
	 * @param grammar The contents of the grammar file, must not be
	 *                <code>null</code>.
	 * @return The declared package name or an empty string if not found.
	 */
	private String findPackageName(String grammar) {
		final String packageDeclaration = "package\\s+([^\\s.;]+(\\.[^\\s.;]+)*)\\s*;";
		Matcher matcher = Pattern.compile(packageDeclaration).matcher(grammar);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}

	/**
	 * Extracts the simple parser name from the specified grammar file.
	 * <p>
	 * @param grammar The contents of the grammar file, must not be
	 *                <code>null</code>.
	 * @return The parser name or an empty string if not found.
	 */
	private String findProgramName(String grammar) {
		final String parserBegin = "program\\s+([^\\s.\\{]+(\\.[^\\s.\\{]+)*)\\s*\\{";
		Matcher matcher = Pattern.compile(parserBegin).matcher(grammar);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}

	/**
	 * Gets the absolute path to the base directory in which the grammar file
	 * resides. Note that this is not necessarily the parent directory of the
	 * grammar file.
	 * <p>
	 * @return The absolute path to the base directory in which the grammar file
	 *         resides, never <code>null</code>.
	 */
	public File getSourceDirectory() {
		return this.sourceDirectory;
	}

	/**
	 * Gets the absolute path to the grammar file.
	 * <p>
	 * @return The absolute path to the grammar file, never <code>null</code>.
	 */
	public File getInputFile() {
		return new File(this.sourceDirectory, this.inputFile);
	}

	/**
	 * Gets the path to the grammar file (relative to its source directory).
	 * <p>
	 * @return The path to the grammar file (relative to its source directory),
	 *         never <code>null</code>.
	 */
	public String getRelativeGrammarFile() {
		return this.inputFile;
	}

	/**
	 * Resolves the specified package name against the package name of the
	 * parser generated from this grammar. To reference the parser package, the
	 * input string may use the prefix "*". For example, if the package for the
	 * parser is "org.apache" and the input string is "*.node", the resolved
	 * package is "org.apache.node". The period after the asterisk is
	 * significant, i.e. in the previous example the input string "*node" would
	 * resolve to "org.apachenode".
	 * <p>
	 * @param packageName The package name to resolve, may be <code>null</code>.
	 * @return The resolved package name or <code>null</code> if the input
	 *         string was <code>null</code>.
	 */
	public String resolvePackageName(String packageName) {
		String resolvedPackageName = packageName;
		if (resolvedPackageName != null && resolvedPackageName.startsWith("*")) {
			resolvedPackageName = getParserPackage() + resolvedPackageName.substring(1);
			if (resolvedPackageName.startsWith(".")) {
				resolvedPackageName = resolvedPackageName.substring(1);
			}
		}
		return resolvedPackageName;
	}

	/**
	 * Gets the declared package for the generated parser (e.g. "org.apache").
	 * <p>
	 * @return The declared package for the generated parser (e.g. "org.apache")
	 *         or an empty string if no package declaration was found, never
	 *         <code>null</code>.
	 */
	public String getParserPackage() {
		return this.fuzzerPackage;
	}

	/**
	 * Gets the path to the directory of the parser package (relative to a
	 * source root directory, e.g. "org/apache").
	 * <p>
	 * @return The path to the directory of the parser package (relative to a
	 *         source root directory, e.g. "org/apache") or an empty string if
	 *         no package declaration was found, never <code>null</code>.
	 */
	public String getParserDirectory() {
		return this.fuzzerDirectory;
	}

	/**
	 * Gets the simple name of the generated parser (e.g. "MyParser")
	 * <p>
	 * @return The simple name of the generated parser (e.g. "MyParser"), never
	 *         <code>null</code>.
	 */
	public String getParserName() {
		return this.fuzzerName;
	}

	/**
	 * Gets the path to the parser file (relative to a source root directory,
	 * e.g. "org/apache/MyParser.java").
	 * <p>
	 * @return The path to the parser file (relative to a source root directory,
	 *         e.g. "org/apache/MyParser.java"), never <code>null</code>.
	 */
	public String getParserFile() {
		return this.fuzzerFile;
	}

	/**
	 * Gets a string representation of this bean. This value is for debugging
	 * purposes only.
	 * <p>
	 * @return A string representation of this bean.
	 */
	@Override
	public String toString() {
		return "FuzzerInfo{" + "sourceDirectory=" + sourceDirectory + ", grammarFile=" + inputFile + ", fuzzerPackage=" + fuzzerPackage + ", fuzzerDirectory=" + fuzzerDirectory + ", fuzzerName=" + fuzzerName + ", fuzzerFile=" + fuzzerFile + '}';
	}


}
