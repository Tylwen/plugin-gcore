package com.guillaumevdn.gcore.lib.compatibility.variants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.guillaumevdn.gcore.GCore;
import com.guillaumevdn.gcore.lib.collection.CollectionUtils;
import com.guillaumevdn.gcore.lib.collection.ReverseSplitLowerCaseMap;
import com.guillaumevdn.gcore.lib.compatibility.Version;
import com.guillaumevdn.gcore.lib.configuration.YMLConfiguration;
import com.guillaumevdn.gcore.lib.exception.ConfigError;
import com.guillaumevdn.gcore.lib.file.FileUtils;
import com.guillaumevdn.gcore.lib.logic.ComparisonType;
import com.guillaumevdn.gcore.lib.object.ObjectUtils;
import com.guillaumevdn.gcore.lib.object.Optional;

/**
 * @author GuillaumeVDN
 */
public abstract class Variants<V extends Variant, DE extends Enum<DE>, D extends VariantData<DE>> {

	private final String typeName;
	private final Class<V> valueClass;
	private final Class<D> dataClass;
	private final Class<DE> extraClass;
	private final boolean regenerate;
	private final boolean lenient;
	private final ReverseSplitLowerCaseMap<V> values = new ReverseSplitLowerCaseMap<>('_');

	public Variants(String typeName, Class<V> valueClass, Class<D> dataClass, Class<DE> extraClass, boolean regenerate, boolean lenient) {
		this.typeName = typeName;
		this.valueClass = valueClass;
		this.dataClass = dataClass;
		this.extraClass = extraClass;
		this.regenerate = regenerate;
		this.lenient = lenient;
	}

	// ----- get
	public final String getTypeName() {
		return typeName;
	}

	public Class<V> getValueClass() {
		return valueClass;
	}

	public Class<D> getDataClass() {
		return dataClass;
	}

	public final Class<DE> getExtraClass() {
		return extraClass;
	}

	public final boolean doRegenerate() {
		return regenerate;
	}

	public final boolean isLenient() {
		return lenient;
	}

	public final Collection<V> values() {
		return values.values();
	}

	public final void print() {
		values.print(GCore.inst().getDataFile("loaded_" + getTypeName() + ".txt"));
	}

	// ----- resolve values
	public final Optional<V> fromId(String id) {
		return Optional.of(values.get(id));
	}

	private final Map<String, Optional<V>> byDataNameQueryCache = new HashMap<>();

	public final Optional<V> fromIdOrDataName(String string) {
		Optional<V> fromId = fromId(string);
		if (fromId.isPresent()) {
			return fromId;
		}
		Optional<V> result = byDataNameQueryCache.get(string);
		if (result == null) {
			for (V mat : values.values()) {
				if (mat.getData().getDataName().equalsIgnoreCase(string)) {
					result = Optional.of(mat);
					break;
				}
			}
			byDataNameQueryCache.put(string, result != null ? result : (result = Optional.empty()));
		}
		return result;
	}

	// ----- set
	public final void register(V value) {
		values.put(value.getId(), value);
		onRegister(value);
		byDataNameQueryCache.clear();
	}

	protected void onRegister(V value) {
	}

	// ----- load
	public void reload(File file, String defaultResource) throws ConfigError {
		values.clear();
		// not custom, regenerate file
		if (file.exists() && regenerate) {
			try {
				file.delete();
				FileUtils.saveDefaultResource(GCore.inst(), defaultResource, file, true);
			} catch (Throwable exception) {
				throw new Error("an unknown error occured while regenerating file " + file, exception);
			}
		}
		// read
		YMLConfiguration config = new YMLConfiguration(GCore.inst(), file);
		for (String id : config.readKeysForSection("")) {
			id = id.toUpperCase();
			// read data
			List<D> data = new ArrayList<>();
			for (String line : config.readMandatoryStringList(id)) {
				try {
					data.add(loadData(line));
				} catch (Throwable exception) {
					ConfigError configError = ObjectUtils.findCauseOrNull(exception, ConfigError.class);
					throw new ConfigError("for " + typeName + " " + id + ", invalid logic line '" + line + "'"
							+ (configError != null ? " : " + configError.getMessage() : ""), configError != null ? null : exception);
				}
			}
			// register
			try {
				registerIfHasCurrentVersion(id, data);
			} catch (Throwable exception) {
				ConfigError configError = ObjectUtils.findCauseOrNull(exception, ConfigError.class);
				throw new ConfigError("Couldn't register " + typeName + " " + id + (configError != null ? " : " + configError.getMessage() : ""),
						configError != null ? null : exception);
			}
		}
	}

	public D loadData(String string) throws ConfigError {
		// separate settings correctly
		string = string.replace("\t", " ").trim().toUpperCase();
		while (string.contains("  "))
			string = string.replace("  ", " ");
		// get extras
		List<DE> elementExtra = null;
		if (extraClass != null) {
			for (DE extra : extraClass.getEnumConstants()) {
				String extraParam = "--" + extra.name().toUpperCase();
				int index = string.toUpperCase().indexOf(extraParam);
				if (index != -1) {
					string = string.substring(0, index) + string.substring(index + extraParam.length());
					if (elementExtra == null) {
						elementExtra = CollectionUtils.asList(extra);
					}
				}
			}
		}
		// find version
		string = string.trim();
		Version version;
		ComparisonType comparison;
		// any version
		if (string.startsWith("?")) {
			string = string.substring(1).trim();
			version = Version.UNKNOWN;
			comparison = ComparisonType.MORE_OR_EQUALS;
		}
		// specified version
		else {
			// comparison
			int digitIndex = -1;
			for (int i = 0; i < string.length(); ++i) {
				if (Character.isDigit(string.charAt(i))) {
					digitIndex = i;
					break;
				}
			}
			if (digitIndex == -1) {
				throw new ConfigError("couldn't find a comparison and a version number");
			}
			comparison = ComparisonType.fromSymbolOrNull(string.substring(0, digitIndex));
			if (comparison == null) {
				throw new ConfigError("'" + string.substring(0, digitIndex) + "' isn't a valid comparison");
			}
			string = string.substring(digitIndex);
			// version
			int spaceIndex = string.indexOf(' ');
			if (spaceIndex == -1) {
				throw new ConfigError("missing " + typeName + " config");
			}
			version = Version.fromNameOrNull(string.substring(0, spaceIndex));
			if (version == null) {
				throw new ConfigError("'" + string.substring(0, spaceIndex) + "' isn't a valid version");
			}
			string = string.substring(spaceIndex + 1); // + 1 is safe because the string was trimmed before, so if there's a space, it's in the middle
		}
		// get element config
		try {
			return loadElementConfigAndCreateData(version, comparison, elementExtra, string);
		} catch (Throwable exception) {
			throw new ConfigError("invalid " + typeName + " config " + string, exception);
		}
	}

	// ----- load element config
	public abstract D loadElementConfigAndCreateData(Version version, ComparisonType comparison, List<DE> extra, String rawData) throws Throwable;

	protected abstract V createElement(String id, D data) throws Throwable;

	// whether an entry whose data doesn't exist on the running version should prevent loading, or just be skipped
	protected boolean isMissingDataFatal() {
		return true;
	}

	public V registerIfHasCurrentVersion(String id, List<D> datas) throws Throwable {
		if (!datas.isEmpty()) {
			List<D> matching = datas.stream().filter(data -> data.getVersionComparison().compare(Version.CURRENT, data.getVersion())).collect(Collectors.toList());
			D first = matching.stream().filter(data -> {
				SimpleExistingVariantData<?, ?> existing = ObjectUtils.castOrNull(data, SimpleExistingVariantData.class);
				return existing == null || existing.getDataInstance() != null || existing.getDataName().equals("NONE");
			}).findFirst().orElse(matching.isEmpty() ? null : matching.get(0));
			if (first != null) {
				SimpleExistingVariantData existing = ObjectUtils.castOrNull(first, SimpleExistingVariantData.class);
				if (existing != null && existing.getDataInstance() == null) {
					if (existing.getDataName().equals("NONE")) {
						return null; // we do not want to register this material on this version, it's a choice here
					}
					String message = "variant " + typeName + " " + id + " has invalid " + first.getVersionComparison().getSymbol() + first.getVersion()
							+ " data '" + first.getDataName() + "' (don't exist even though we're running this version)";
					if (!isMissingDataFatal()) {
						GCore.inst().getMainLogger().warning("Skipping " + message);
						return null;
					}
					throw new ConfigError(message);
				}
				V value = createElement(id.toUpperCase(), first);
				register(value);
				return value;
			}
		}
		return null;
	}

	// ----- utils
	protected static int loadPositiveNumber(String string, String type) throws ConfigError {
		string = string.trim();
		int number = 0;
		try {
			if (!string.isEmpty()) {
				number = Integer.parseInt(string);
				if (number < 0)
					throw new NumberFormatException();
			}
		} catch (NumberFormatException ignored) {
			throw new ConfigError("'" + string + "' isn't a valid " + type);
		}
		return number;
	}

	protected static List<Integer> loadPositiveNumberList(String[] array, int startInclusive, int endExclusive, String type) throws ConfigError {
		List<Integer> list = new ArrayList<>(endExclusive - startInclusive);
		for (int i = startInclusive; i < endExclusive; ++i) {
			list.add(loadPositiveNumber(array[i], null));
		}
		return list;
	}

}
