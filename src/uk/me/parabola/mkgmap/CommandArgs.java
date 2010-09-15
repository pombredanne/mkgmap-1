package uk.me.parabola.mkgmap;

import java.io.File;

import uk.me.parabola.util.EnhancedProperties;

public class CommandArgs {
	private final EnhancedProperties currentOptions;

	public CommandArgs() {
		currentOptions = new EnhancedProperties();
	}

	public CommandArgs(EnhancedProperties args) {
		currentOptions = new EnhancedProperties(args);
	}

	public EnhancedProperties getProperties() {
		return currentOptions;
	}

	public int get(String name, int def) {
		return currentOptions.getProperty(name, def);
	}

	public String get(String name, String def) {
		return currentOptions.getProperty(name, def);
	}

	public String getDescription() {
		return currentOptions.getProperty("description");
	}

	// ////
	// There are a number of methods to get specific arguments that follow.
	// There are many more options in use however.  New code should mostly
	// just use the get methods above.
	// ////

	public int getBlockSize() {
		return get("block-size", 512);
	}

	public String getMapname() {
		return currentOptions.getProperty("mapname");
	}

	public String getCharset() {
		String charset = currentOptions.getProperty("latin1");
		if (charset != null)
			return "latin1";

		// xcharset is the old value, use charset instead.
		charset = currentOptions.getProperty("charset", currentOptions.getProperty("xcharset"));
		if (charset != null)
			return charset;

		int cp = getCodePage();
		if (cp != 0)
			return "cp" + cp;

		return "ascii";
	}

	public int getCodePage() {
		int cp;

		// xcode-page is the old name
		String s = currentOptions.getProperty("code-page", currentOptions.getProperty("xcode-page", "0"));
		try {
			cp = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			cp = 0;
		}

		return cp;
	}

	public String getOutputDir() {
		String DEFAULT_DIR = ".";
		String fileOutputDir = currentOptions.getProperty("output-dir", DEFAULT_DIR);
 
		// Test if directory exists
		File outputDir = new File(fileOutputDir);
		if (!outputDir.exists()) {
			System.out.println("Output directory not found. Creating directory '" + fileOutputDir + "'");
			if (!outputDir.mkdirs()) {
				System.err.println("Unable to create output directory! Using default directory instead");
				fileOutputDir = DEFAULT_DIR;
			}
		} else if (!outputDir.isDirectory()) {
			System.err.println("The --output-dir parameter must specify a directory. The parameter is being ignored, writing to default directory instead.");
			fileOutputDir = DEFAULT_DIR;
		}
		
		return fileOutputDir;
	}
	
	public boolean isForceUpper() {
		return currentOptions.getProperty("lower-case") == null;
	}

	/**
	 * Test for the existence of an argument.
	 */
	public boolean exists(String name) {
		return currentOptions.containsKey(name);
	}
}
