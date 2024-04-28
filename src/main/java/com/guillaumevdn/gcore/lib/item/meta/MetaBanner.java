package com.guillaumevdn.gcore.lib.item.meta;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.guillaumevdn.gcore.GCore;
import com.guillaumevdn.gcore.TextEditorGeneric;
import com.guillaumevdn.gcore.lib.collection.CollectionUtils;
import com.guillaumevdn.gcore.lib.compatibility.Version;
import com.guillaumevdn.gcore.lib.element.editor.SlotPlacement;
import com.guillaumevdn.gcore.lib.element.struct.Need;
import com.guillaumevdn.gcore.lib.element.type.basic.ElementDyeColor;
import com.guillaumevdn.gcore.lib.element.type.container.ElementItem;
import com.guillaumevdn.gcore.lib.element.type.map.ElementPatternTypeColorMap;
import com.guillaumevdn.gcore.lib.item.ItemCheck;
import com.guillaumevdn.gcore.lib.item.ItemReference;
import com.guillaumevdn.gcore.lib.object.ObjectUtils;
import com.guillaumevdn.gcore.lib.reflection.ReflectionObject;
import com.guillaumevdn.gcore.lib.serialization.data.DataIO;
import com.guillaumevdn.gcore.lib.string.placeholder.Replacer;

/**
 * @author GuillaumeVDN
 */
public final class MetaBanner {

	public static boolean match(ItemMeta itemMeta, ItemReference reference, ItemCheck check) {
		if (!reference.hasMeta(BannerMeta.class)) return true;
		BannerMeta meta = ObjectUtils.castOrNull(itemMeta, BannerMeta.class);  // might be null if exact match is false

		// base color (<1.13, now stored as material type)
		if (!Version.ATLEAST_1_13) {
			try {
				if (check.isExact() && (meta == null || !Objects.deepEquals(ReflectionObject.of(meta).invokeMethod("getBaseColor").justGet(), reference.getBaseColor()))) return false;
				else if (!check.isExact() && reference.getBaseColor() != null && (meta == null || !Objects.deepEquals(ReflectionObject.of(meta).invokeMethod("getBaseColor").justGet(), reference.getBaseColor()))) return false;
			} catch (Throwable e) {
				GCore.inst().getMainLogger().error("Could not check potion meta", e);
				return false;
			}
		}

		// patterns
		if (check.isExact()) {
			if (!CollectionUtils.contentEquals(meta.getPatterns(), reference.getPatterns())) return false;
		} else {
			List<Pattern> refPatterns = reference.getPatterns();
			if (refPatterns != null && !refPatterns.isEmpty() && (meta == null || meta.getPatterns() == null)) return false;
			main: for (Pattern refPattern : refPatterns) {
				for (Pattern pattern : meta.getPatterns()) {
					if (pattern.equals(refPattern)) {
						continue main;
					}
				}
				return false;
			}
		}

		// seems good
		return true;
	}

	public static void write(ItemMeta itemMeta, DataIO writer) {
		BannerMeta meta = ObjectUtils.castOrNull(itemMeta, BannerMeta.class);
		if (meta != null) {
			// base color (<1.13, now stored as material type)
			if (!Version.ATLEAST_1_13) {
				try {
					writer.write("baseColor", ReflectionObject.of(meta).invokeMethod("getBaseColor").justGet());
				} catch (Throwable e) {
					GCore.inst().getMainLogger().error("Could not write potion meta", e);
				}
			}
			// patterns
			writer.writeSerializedList("patterns", meta.getPatterns());
		}
	}

	public static void read(ItemMeta itemMeta, DataIO reader) {
		BannerMeta meta = ObjectUtils.castOrNull(itemMeta, BannerMeta.class);
		if (meta != null) {
			// base color (<1.13, now stored as material type)
			DyeColor baseColor = Version.ATLEAST_1_13 ? null : reader.readEnum("baseColor", DyeColor.class);
			if (baseColor != null) {
				try {
					ReflectionObject.of(meta).invokeMethod("setBaseColor", baseColor);
				} catch (Throwable e) {
					GCore.inst().getMainLogger().error("Could not read potion meta", e);
				}
			}
			// patterns
			List<Pattern> patterns = reader.readSerializedList("patterns", Pattern.class);
			if (patterns != null) {
				meta.setPatterns(patterns.stream().filter(pattern -> pattern != null).collect(Collectors.toList()));
			}
		}
	}

	public static void fillElements(ItemMeta sampleMeta, ElementItem item) {
		if (ObjectUtils.instanceOf(sampleMeta, BannerMeta.class)) {
			if (!Version.ATLEAST_1_13) {
				item.addDyeColor("base_color", Need.optional(), SlotPlacement.START_ROW, TextEditorGeneric.descriptionItemBannerBaseColor);
			}
			item.addPatternTypeColorMap("patterns", Need.optional(), Version.ATLEAST_1_13 ? SlotPlacement.START_ROW : SlotPlacement.ANY, TextEditorGeneric.descriptionItemBannerPatterns);
		}
	}

	public static void clearElements(ElementItem item) {
		item.remove("base_color");
		item.remove("patterns");
	}

	public static void writeElements(ElementItem item, DataIO writer, Replacer replacer) {
		if (!Version.ATLEAST_1_13) {
			item.parseElementAs("base_color", replacer).ifPresentDo(v -> writer.write("base_color", v));
		}
		item.parseElementAsMap("patterns", PatternType.class, DyeColor.class, replacer).ifPresentDo(patterns -> {
			writer.writeObject("patterns", w -> patterns.forEach((type, color) -> w.write(((PatternType) type).name(), color)));
		});
	}

	public static void importElements(ElementItem item, ItemMeta itemMeta) {
		BannerMeta meta = ObjectUtils.castOrNull(itemMeta, BannerMeta.class);
		if (meta != null) {
			if (!Version.ATLEAST_1_13) {
				try {
					item.getElementAs("base_color", ElementDyeColor.class).setValue(CollectionUtils.asList(ReflectionObject.of(meta).invokeMethod("getBaseColor").invokeMethod("name").get(String.class)));
				} catch (Throwable e) {
					GCore.inst().getMainLogger().error("Could not import potion meta", e);
				}
			}
			ElementPatternTypeColorMap map = item.getElementAs("patterns");
			map.clear();
			for (Pattern pattern : meta.getPatterns()) {
				map.createAndAddElement(pattern.getPattern()).setValue(CollectionUtils.asList(pattern.getColor().name()));
			}
		}
	}

}
