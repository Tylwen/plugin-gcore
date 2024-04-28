package com.guillaumevdn.gcore.lib.element.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import com.guillaumevdn.gcore.TextEditorGeneric;
import com.guillaumevdn.gcore.lib.collection.CollectionUtils;
import com.guillaumevdn.gcore.lib.compatibility.Version;
import com.guillaumevdn.gcore.lib.compatibility.material.CommonMats;
import com.guillaumevdn.gcore.lib.compatibility.material.Mat;
import com.guillaumevdn.gcore.lib.configuration.YMLConfiguration;
import com.guillaumevdn.gcore.lib.element.editor.EditorGUI;
import com.guillaumevdn.gcore.lib.element.struct.list.referenceable.Node;
import com.guillaumevdn.gcore.lib.element.struct.map.AbstractMapElement;
import com.guillaumevdn.gcore.lib.exception.ConfigError;
import com.guillaumevdn.gcore.lib.gui.struct.ClickCall;
import com.guillaumevdn.gcore.lib.gui.struct.ClickCall.ClickType;
import com.guillaumevdn.gcore.lib.gui.struct.GUIItem;
import com.guillaumevdn.gcore.lib.item.ItemUtils;
import com.guillaumevdn.gcore.lib.object.NeedType;
import com.guillaumevdn.gcore.lib.object.ObjectUtils;
import com.guillaumevdn.gcore.lib.string.StringUtils;
import com.guillaumevdn.gcore.lib.string.Text;
import com.guillaumevdn.gcore.lib.tuple.IntegerPair;

/**
 * @author GuillaumeVDN
 */
public abstract class Element implements IElement, Comparable<Element> {

	private Element parent;
	private final String id;
	private final NeedType need;
	private final Text editorDescription;

	private String forcedConfigurationPath = null;  // used in YMLConfiguration, to load elements quickly with no parent
	private Map<String, String> extra = null;  // to have additionnal data

	public Element(Element parent, String id, NeedType need, Text editorDescription) {
		this.id = id.toLowerCase();
		this.need = need;
		this.editorDescription = editorDescription;
		setParent(parent);
	}

	// ----- get
	public String getTypeName() {
		return getClass().getSimpleName().replace("Element", "");
	}

	public final Element getParent() {
		return parent;
	}

	public final <T extends Element> T getParentAsOrNull(Class<T> elementClass) {
		return ObjectUtils.castOrNull(getParent(), elementClass);
	}

	public final <T extends Element> T getAncestorAsOrNull(Class<T> elementClass) {
		final T parent = getParentAsOrNull(elementClass);
		if (parent != null) {
			return parent;
		}
		return getParent() != null ? getParent().getAncestorAsOrNull(elementClass) : null;
	}

	@Override
	public final SuperElement getSuperElement() {
		// super element must be the highest parent in chain
		// (since some super elements are sometimes overriden to make more specific elements)

		Element parent = this;
		while (parent.getParent() != null) {
			parent = parent.getParent();
		}
		SuperElement sup = ObjectUtils.castOrNull(parent, SuperElement.class);
		if (sup != null) {
			return sup;
		}

		throw new IllegalArgumentException("no super element found in chain");
	}

	public final <T extends SuperElement> T getSuperElementAsOrNull(Class<T> elementClass) {
		return ObjectUtils.castOrNull(getSuperElement(), elementClass);
	}

	public final String getId() {
		return id;
	}

	public final NeedType getNeed() {
		return need;
	}

	public final boolean isRequiredInContext() {
		Element parent = this.parent;
		if (parent == null || parent.readContains()) {
			return getNeed().equals(NeedType.REQUIRED);
		}
		return false;
	}

	public Text getEditorDescription() {
		return editorDescription;
	}

	public String getConfigurationPath() {
		if (forcedConfigurationPath != null) {
			return forcedConfigurationPath;
		}

		// this method is overriden for super elements, so parent can't be null here
		String parentPath = parent.getConfigurationPath();
		return parentPath == null || parentPath.isEmpty() ? id : parentPath + "." + id;
	}

	public final String getExtra(String key) {
		return extra != null ? extra.get(key) : null;
	}

	@Override
	public abstract boolean hasParseableLocations();
	public abstract boolean isCurrentlyDefault();

	// ----- set
	public final void setParent(Element parent) {
		this.parent = parent;
		getSuperElement();  // throws an error if none found
	}

	public final void setForcedConfigurationPath(String forcedConfigurationPath) {
		this.forcedConfigurationPath = forcedConfigurationPath;
	}

	public final void setExtra(String key, String value) {
		if (extra == null) {
			extra = new HashMap<>(2, 1f);
		}
		extra.put(key, value);
	}

	// ----- object
	@Override
	public final String toString() {
		return id;
	}

	@Override
	public final int compareTo(Element other) {
		return StringUtils.compareAlphabeticallyWithNumbersIgnoreCase(id, other.id);
	}

	@Override
	public final int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		Element other = ObjectUtils.castOrNull(obj, getClass());
		return other != null && other.getId().equals(id);
	}

	// ----- loading and saving
	private boolean readContains = false;

	public final boolean readContains() {
		return readContains;
	}

	public void resetCache() {
	}

	/** @throws Throwable if any error, other than a ConfigError or one with a ConfigError parent, occurred */
	@SuppressWarnings("unlikely-arg-type")
	public final void read() throws Throwable {
		readContains = false;
		clearBeforeRead();
		try {
			// contains
			YMLConfiguration config = getSuperElement().getConfiguration();
			String path = getConfigurationPath();
			if (config.contains(path)) {
				readContains = true;
				doRead();
			}
			// not in config
			else {
				if (getNeed().equals(NeedType.REQUIRED)) {
					getSuperElement().addLoadError("missing " + getTypeName() + " at path " + path);
				}
			}
		} catch (Throwable exception) {
			ConfigError configError = ObjectUtils.findCauseOrNull(exception, ConfigError.class);
			if (configError != null) {  // a config error that wasn't catched already ? :think:
				getSuperElement().addLoadError(configError.getMessage());
			} else {  // regular error, re-throw it
				throw exception;
			}
		}

		// is super element ? notify errors
		SuperElement sup = getSuperElement();
		if (equals(sup)) {
			// notify loading errors
			if (!sup.getLoadErrors().isEmpty()) {
				sup.getPlugin().getMainLogger().error("Errors were found when loading " + getTypeName() + " " + id + " :", true);
				sup.getLoadErrors().forEach(error -> sup.getPlugin().getMainLogger().error("- " + error, true));
			}
		}
	}

	/** @throws Throwable if any error occurred */
	public final void write() throws Throwable {
		doWrite();
	}

	protected abstract void clearBeforeRead();
	protected abstract void doRead() throws Throwable;
	protected abstract void doWrite() throws Throwable;

	// ----- editor
	public Mat editorIconType() {
		return CommonMats.COBBLESTONE;
	}

	public List<String> editorCurrentValue() {
		return null;
	}

	public final List<String> editorCurrent() {
		List<String> value = editorCurrentValue();
		if (value == null || value.isEmpty() || value.get(0).isEmpty()) {
			if (this instanceof SuperElement) {
				return null;
			}
			return (isCurrentlyDefault() ? TextEditorGeneric.elementCurrentValueNoneDefault : TextEditorGeneric.elementCurrentValueNone).parseLines();
		}
		else if (value.size() == 1) {
			if (value.get(0).contains("{value}")) {  // the value itself can contain the {value} placeholder... causes an infinite replacer loop #2345
				Text text = isCurrentlyDefault() ? TextEditorGeneric.elementCurrentValueSingleDefault : TextEditorGeneric.elementCurrentValueSingle;
				return Text.of(text.getCurrentLines().stream().map(str -> str.replace("{value}", "{current}")).collect(Collectors.toList())).replace("{current}", () -> value.get(0)).parseLines();
			} else {
				return (isCurrentlyDefault() ? TextEditorGeneric.elementCurrentValueSingleDefault : TextEditorGeneric.elementCurrentValueSingle).replace("{value}", () -> value.get(0)).parseLines();
			}
		}
		else {
			List<String> valueList = new ArrayList<>();
			value.forEach(line -> valueList.addAll(TextEditorGeneric.elementCurrentValueListLine.replace("{line}", () -> line).parseLines()));
			if (value.get(0).contains("{value}")) {  // the value itself can contain the {value} placeholder... causes an infinite replacer loop #2345
				Text text = isCurrentlyDefault() ? TextEditorGeneric.elementCurrentValueListDefault : TextEditorGeneric.elementCurrentValueList;
				return Text.of(text.getCurrentLines().stream().map(str -> str.replace("{value}", "{current}")).collect(Collectors.toList())).replace("{current}", () -> valueList).parseLines();
			} else {
				return (isCurrentlyDefault() ? TextEditorGeneric.elementCurrentValueListDefault : TextEditorGeneric.elementCurrentValueList).replace("{value}", () -> valueList).parseLines();
			}
		}
	}

	public List<String> nonControlEditorIconLore() {
		List<String> lore = CollectionUtils.asList();
		// current value
		List<String> current = editorCurrent();
		if (current != null) {
			lore.add("§r");
			lore.addAll(current);
		}
		// description
		if (getEditorDescription() != null) {
			if (current != null) lore.add("§r");
			lore.addAll(TextEditorGeneric.elementDescription.replace("{description}", () -> getEditorDescription().parseLines()).parseLines());
		}
		// type
		if (!(this instanceof SuperElement)) {
			if (current != null || getEditorDescription() != null) lore.add("§r");
			lore.addAll((getNeed().equals(NeedType.REQUIRED) ? TextEditorGeneric.elementTypeMandatory : TextEditorGeneric.elementTypeOptional).replace("{type}", () -> getTypeName()).parseLines());
		}
		return lore;
	}

	public List<String> editorIconLore() {
		return nonControlEditorIconLore();
	}

	public final ItemStack editorIcon() {
		return editorIcon(false);
	}

	public final ItemStack editorIcon(boolean nonControl) {
		List<String> lore = nonControl ? nonControlEditorIconLore() : editorIconLore();
		lore = lore == null ? null : StringUtils.splitLongText(lore, 50);
		if (lore != null && lore.size() > 30) {
			while (lore.size() > 30) lore.remove(lore.size() - 1);
			lore.add("§7...");
		}
		ItemStack icon = ItemUtils.addAllFlags(ItemUtils.createItem(editorIconType(), "§6" + getId(), lore));
		if (!isCurrentlyDefault()) {
			icon.addUnsafeEnchantment(
			Enchantment.getByName(Version.ATLEAST_1_20_5 ? "UNBREAKING" : "DURABILITY"), 1);
		}
		if (this instanceof AbstractMapElement) {
			int abstraction = getAbstraction(this, 1);
			icon.setAmount(abstraction > 64 ? 64 : abstraction);
		}
		return icon;
	}

	private int getAbstraction(Element element, int current) {
		int biggest = current;
		if (element instanceof AbstractMapElement) {
			for (Object elem : ((AbstractMapElement<?, ?>) element).values()) {
				if (elem instanceof Element) {
					int level = getAbstraction((Element) elem, current + 1);
					if (level > biggest) {
						biggest = level;
					}
				} else if (elem instanceof Node) {
					int level = getAbstraction(((Node) elem).getValue(), current + 1);
					if (level > biggest) {
						biggest = level;
					}
				}
			}
		}
		return biggest;
	}

	public final GUIItem buildEditorItem(int page, int slot) {
		return new GUIItem("element_" + getId(), CollectionUtils.asList(IntegerPair.of(page, slot)), ItemUtils.addAllFlags(editorIcon()), call -> onEditorClick(call));
	}

	public EditorGUI editorGUI(ClickCall fromCall) {
		return null;
	}

	public void onEditorClick(ClickCall call) {
		// left-click : edit
		if (call.getType().equals(ClickType.LEFT)) {
			EditorGUI editor = editorGUI(call);
			if (editor != null) {
				editor.openFor(call.getClicker(), call);
			}
		}
	}

}
