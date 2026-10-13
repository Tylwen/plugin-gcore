package com.guillaumevdn.gcore.lib.compatibility;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import com.guillaumevdn.gcore.lib.collection.CollectionUtils;
import com.guillaumevdn.gcore.lib.object.ObjectUtils;

/**
 * @author GuillaumeVDN
 */
final class VersionUtils {

	/*
	 * static Version getCurrent() { String bukkitVersion = Bukkit.getBukkitVersion().split("-")[0]; List<Version> versions
	 * = CollectionUtils.asList(Version.values()); Collections.reverse(versions); for (Version version : versions) { if
	 * (version.getNames().stream().anyMatch(v -> bukkitVersion.contains(v))) { return version; } } return Version.UNKNOWN;
	 * }
	 */

	static Version getCurrent() {
		// forced value from config
		YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(Bukkit.getWorldContainer() + "/plugins/GCore/config.yml"));
		String force = cfg.getString("force_server_version");
		Version forced = ObjectUtils.safeValueOf(force, Version.class);
		if (forced != null) {
			return forced;
		}
		// otherwise, detect it
		List<Version> versions = CollectionUtils.asList(Version.values());
		Collections.reverse(versions);
		// ... by version ?
		String bukkitVersion = Bukkit.getBukkitVersion();
		int[] actual = parseNumbers(bukkitVersion.split("-")[0]);
		if (actual != null) {
			// latest known version that isn't newer than the running one (a substring match would take 1.21.11 for 1.21.1)
			for (Version version : versions) {
				int[] first = version.getNames().stream().map(VersionUtils::parseNumbers).filter(Objects::nonNull).min(VersionUtils::compareNumbers).orElse(null);
				if (first != null && compareNumbers(first, actual) <= 0) {
					return version;
				}
			}
		}
		for (Version version : versions) {
			if (version.getNames().stream().anyMatch(name -> bukkitVersion.contains(name))) {
				return version;
			}
		}
		// ... by package name (<1.17)
		for (Version version : versions) {
			if (Package.getPackage(version.getNMSPackage()) != null) {
				return version;
			}
		}
		// didn't find a version ; try to find package name
		Package nms = Arrays.stream(Package.getPackages()).filter(p -> p.getName().startsWith("net.minecraft.server.v")).findFirst().orElse(null);
		if (nms != null) {
			String packageName = nms.getName().substring("net.minecraft.server.".length()).split("\\.")[0];
			Version.UNSUPPORTED.nmsPackage = packageName;
			return Version.UNSUPPORTED;
		} else {
			return Version.UNKNOWN;
		}
	}

	private static int[] parseNumbers(String version) {
		try {
			return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private static int compareNumbers(int[] a, int[] b) {
		for (int i = 0; i < Math.max(a.length, b.length); ++i) {
			int compare = Integer.compare(i < a.length ? a[i] : 0, i < b.length ? b[i] : 0);
			if (compare != 0) {
				return compare;
			}
		}
		return 0;
	}

}
