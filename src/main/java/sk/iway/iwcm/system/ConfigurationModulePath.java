package sk.iway.iwcm.system;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses hierarchical configuration module paths.
 */
public final class ConfigurationModulePath {

	private static final Pattern PATH_PATTERN = Pattern.compile("[a-z0-9_-]+(?:\\.[a-z0-9_-]+)*");

	private ConfigurationModulePath() {
		// Utility class.
	}

	/**
	 * Splits semicolon-separated module memberships, removes invalid entries and
	 * preserves the declaration order while removing duplicates.
	 *
	 * @param modules semicolon-separated module paths
	 * @return normalized module paths
	 */
	public static List<String> parse(String modules) {
		if (modules == null || modules.isBlank()) return List.of();

		Set<String> paths = new LinkedHashSet<>();
		for (String module : modules.split(";")) {
			String path = module.trim();
			if (isValidPath(path)) paths.add(path);
		}
		return new ArrayList<>(paths);
	}

	/**
	 * Returns the canonical semicolon-separated representation of module paths.
	 *
	 * @param modules semicolon-separated module paths
	 * @return normalized module memberships
	 */
	public static String normalize(String modules) {
		return String.join(";", parse(modules));
	}

	/**
	 * Tests whether any declared path is the selected branch or its descendant.
	 *
	 * @param modules semicolon-separated module paths
	 * @param branch selected hierarchy branch
	 * @return true when a membership belongs to the branch
	 */
	public static boolean isInBranch(String modules, String branch) {
		if (isValidPath(branch) == false) return false;

		for (String path : parse(modules)) {
			if (path.equals(branch) || path.startsWith(branch + ".")) return true;
		}
		return false;
	}

	/**
	 * Matches the historical flat module identifier against a complete segment of
	 * a hierarchical path. A hierarchical module identifier must match the full
	 * path.
	 *
	 * @param modules semicolon-separated module paths
	 * @param module historical module identifier or a full module path
	 * @return true when the module matches exactly
	 */
	public static boolean matchesLegacyModule(String modules, String module) {
		if (isValidPath(module) == false) return false;

		for (String path : parse(modules)) {
			if (path.equals(module)) return true;
			if (module.indexOf('.') == -1) {
				for (String segment : path.split("\\.")) {
					if (segment.equals(module)) return true;
				}
			}
		}
		return false;
	}

	/**
	 * Tests whether a value consists only of valid dot-separated path segments.
	 *
	 * @param path module path
	 * @return true for a valid module path
	 */
	public static boolean isValidPath(String path) {
		return path != null && PATH_PATTERN.matcher(path).matches();
	}
}
