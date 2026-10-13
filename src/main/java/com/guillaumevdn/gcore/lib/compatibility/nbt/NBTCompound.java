package com.guillaumevdn.gcore.lib.compatibility.nbt;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.guillaumevdn.gcore.ConfigGCore;
import com.guillaumevdn.gcore.lib.collection.CollectionUtils;
import com.guillaumevdn.gcore.lib.compatibility.Version;
import com.guillaumevdn.gcore.lib.object.ObjectUtils;
import com.guillaumevdn.gcore.lib.reflection.ReflectionObject;

/**
 * @author GuillaumeVDN
 */
public class NBTCompound extends NBTBase {

	public NBTCompound(NBTBase parent, String name, int depth, ReflectionObject tag) throws Throwable {
		super(parent, name, depth, tag);
	}

	// ----- keys
	/*
	 * private static final String getValueTypeMethod = Reflection.getForThisVersion(Version.MC_1_8_R3, "b",
	 * Version.MC_1_9_R2, "d", Version.MC_1_15_R1, "e", Version.MC_1_16_R1, "d");
	 * 
	 * public final NBTType getValueType(String key) throws Throwable { Object type =
	 * getTag().invokeMethod(getValueTypeMethod).get(); if (type == null) { return NBTType.UNKNOWN; } return
	 * NBTType.getByInternalId(type instanceof Byte ? ((Byte) type).intValue() : (Integer) type); }
	 */

	public final NBTType getValueType(String key) throws Throwable {
		return NBTType.getByWrappedClass(getTag().invokeMethod(Version.MOJANG_MAPPINGS ? "get" : Version.ATLEAST_1_18 ? "c" : "get", key).get());
	}

	// ----- keys
	public final Set<String> getKeys() throws Throwable {
		if (Version.MOJANG_MAPPINGS) {
			// getAllKeys was replaced by keySet in recent versions
			Object handle = getTag().get();
			boolean legacyKeys = false;
			for (java.lang.reflect.Method method : handle.getClass().getMethods()) {
				if (method.getParameterCount() == 0 && method.getName().equals("getAllKeys")) {
					legacyKeys = true;
					break;
				}
			}
			return getTag().invokeMethod(legacyKeys ? "getAllKeys" : "keySet").get();
		}
		return getTag().invokeMethod(Version.ATLEAST_1_19_3 ? "e" : (Version.ATLEAST_1_18 ? "d" : (Version.ATLEAST_1_13 ? "getKeys" : "c"))).get();
	}

	public final boolean hasKey(String key) throws Throwable {
		return getTag().invokeMethod(Version.MOJANG_MAPPINGS ? "contains" : Version.ATLEAST_1_18 ? "e" : "hasKey", key).get();
	}

	// ----- get value
	public final Object get(String key) throws Throwable {
		return get(getValueType(key), key);
	}

	public final Object get(NBTType type, String key) throws Throwable {
		return type.getValue(this, key);
	}

	public final Byte getByte(String key) throws Throwable {
		return NBTType.BYTE.getValue(this, key);
	}

	public final Short getShort(String key) throws Throwable {
		return NBTType.SHORT.getValue(this, key);
	}

	public final Integer getInt(String key) throws Throwable {
		return NBTType.INT.getValue(this, key);
	}

	public final Long getLong(String key) throws Throwable {
		return NBTType.LONG.getValue(this, key);
	}

	public final Float getFloat(String key) throws Throwable {
		return NBTType.FLOAT.getValue(this, key);
	}

	public final Double getDouble(String key) throws Throwable {
		return NBTType.DOUBLE.getValue(this, key);
	}

	public final String getString(String key) throws Throwable {
		return NBTType.STRING.getValue(this, key);
	}

	public final Boolean getBoolean(String key) throws Throwable {
		Byte value = getByte(key);
		return value != null && value.intValue() != 0;
	}

	public final Byte[] getByteArray(String key) throws Throwable {
		return NBTType.BYTE_ARRAY.getValue(this, key);
	}

	public final Integer[] getIntArray(String key) throws Throwable {
		return NBTType.INT_ARRAY.getValue(this, key);
	}

	public final NBTList getList(String key) throws Throwable {
		ReflectionObject tag = NBTType.getObject(this, key);
		return new NBTList(this, key, getDepth() + 1, tag);
	}

	public final NBTCompound getCompound(String key) throws Throwable {
		ReflectionObject tag = NBTType.getObject(this, key);
		return new NBTCompound(this, key, getDepth() + 1, tag);
	}

	// ----- set value
	public final void remove(String key) throws Throwable {
		getTag().invokeMethod(Version.MOJANG_MAPPINGS ? "remove" : Version.ATLEAST_1_18 ? "r" : "remove", key);
	}

	public final void set(String key, Object value) throws Throwable {
		NBTType type = value == null ? NBTType.UNKNOWN : NBTType.getByValueClass(value);
		if (type.equals(NBTType.UNKNOWN)) {
			remove(key);
			ConfigGCore.logspamItemNbt(this, () -> "Removed value " + key);
		} else {
			type.setValue(this, key, value);
			ConfigGCore.logspamItemNbt(this, () -> "Set value " + key + " of type " + type + " to " + value);
		}
	}

	public final void setByte(String key, byte value) throws Throwable {
		NBTType.BYTE.setValue(this, key, value);
	}

	public final void setShort(String key, short value) throws Throwable {
		NBTType.SHORT.setValue(this, key, value);
	}

	public final void setInt(String key, int value) throws Throwable {
		NBTType.INT.setValue(this, key, value);
	}

	public final void setLong(String key, long value) throws Throwable {
		NBTType.LONG.setValue(this, key, value);
	}

	public final void setFloat(String key, float value) throws Throwable {
		NBTType.FLOAT.setValue(this, key, value);
	}

	public final void setDouble(String key, double value) throws Throwable {
		NBTType.DOUBLE.setValue(this, key, value);
	}

	public final void setBoolean(String key, boolean value) throws Throwable {
		setByte(key, (byte) (value ? 1 : 0));
	}

	public final void setString(String key, String value) throws Throwable {
		NBTType.STRING.setValue(this, key, value);
	}

	public final void setByteArray(String key, byte[] value) throws Throwable {
		NBTType.BYTE_ARRAY.setValue(this, key, value);
	}

	public final void setIntArray(String key, int[] value) throws Throwable {
		NBTType.INT_ARRAY.setValue(this, key, value);
	}

	public final void setList(String key, NBTList list) throws Throwable {
		list.parent = this;
		getTag().invokeMethod(Version.MOJANG_MAPPINGS ? "put" : Version.ATLEAST_1_18 ? "a" : "set", key, list.getTag().get());
		ConfigGCore.logspamItemNbt(this, () -> "Set list " + key + " of size " + list.size() + " and type " + list.getValueType());
	}

	public final void setCompound(String key, NBTCompound compound) throws Throwable {
		compound.parent = this;
		getTag().invokeMethod(Version.MOJANG_MAPPINGS ? "put" : Version.ATLEAST_1_18 ? "a" : "set", key, compound.getTag().get());
		ConfigGCore.logspamItemNbt(this, () -> "Set compound " + key + " with keys " + compound.getKeys());
	}

	// ----- match
	public boolean match(NBTCompound reference, boolean exactMatch) throws Throwable {
		List<String> thisKeys = CollectionUtils.asList(getKeys());
		List<String> refKeys = CollectionUtils.asList(reference.getKeys());
		if (ObjectUtils.instanceOf(getSuperParent(), NBTItem.class) && getDepth() == 0) {
			thisKeys.removeAll(NBTItem.IGNORE_TAGS);
			refKeys.removeAll(NBTItem.IGNORE_TAGS);
		}
		ConfigGCore.logspamItemNbt(this, () -> "exactMatch " + exactMatch);
		ConfigGCore.logspamItemNbt(this, () -> "thisKeys " + thisKeys);
		ConfigGCore.logspamItemNbt(this, () -> "ref Keys " + refKeys);
		if (exactMatch) {
			if (!CollectionUtils.contentEquals(thisKeys, refKeys, false))
				return false;
		} else {
			for (String key : refKeys) {
				if (!thisKeys.contains(key)) {
					ConfigGCore.logspamItemNbt(this, () -> "Checking ; missing " + key);
					return false;
				}
			}
		}
		for (String key : refKeys) {
			NBTType type = getValueType(key);
			ConfigGCore.logspamItemNbt(this, () -> "Checking key " + key + ", type " + type);

			// type don't match in other
			NBTType typeOther = reference.getValueType(key);
			if (type == null || !type.equals(typeOther)) {
				ConfigGCore.logspamItemNbt(this, () -> "reference is not of same type (" + typeOther + ")");
				return false;
			}

			// list
			if (type.equals(NBTType.LIST)) {
				if (!getList(key).match(reference.getList(key), exactMatch)) {
					ConfigGCore.logspamItemNbt(this, () -> "list match failed");
					return false;
				}
			}
			// object
			else if (type.equals(NBTType.COMPOUND)) {
				if (!getCompound(key).match(reference.getCompound(key), exactMatch)) {
					ConfigGCore.logspamItemNbt(this, () -> "compound match failed");
					return false;
				}
			}
			// value
			else if (!type.equals(NBTType.UNKNOWN)) {
				Object obj = get(type, key);
				Object ref = reference.get(typeOther, key);
				if (!Objects.deepEquals(obj, ref)) {
					ConfigGCore.logspamItemNbt(this, () -> "value match failed, this " + obj + ", ref " + ref);
					return false;
				}
			}
		}
		// seems good
		return true;
	}

	// ----- add
	public boolean addNonClonedElementsFrom(NBTCompound reference) throws Throwable {
		Set<String> thisKeys = getKeys();
		List<String> refKeys = CollectionUtils.asList(reference.getKeys());
		if (ObjectUtils.instanceOf(getSuperParent(), NBTItem.class) && getDepth() == 0) {
			refKeys.removeAll(NBTItem.IGNORE_TAGS);
		}
		ConfigGCore.logspamItemNbt(this, () -> "Copying " + getName() + " from " + reference.getName());
		ConfigGCore.logspamItemNbt(this, () -> "- Ref keys " + reference.getKeys());
		ConfigGCore.logspamItemNbt(this, () -> "- This keys " + thisKeys);
		ConfigGCore.logspamItemNbt(this, () -> "- Copy keys " + refKeys);
		for (String key : refKeys) {
			// list
			NBTType type = reference.getValueType(key);
			ConfigGCore.logspamItemNbt(this, () -> " Copying key " + key + " of type " + type);
			if (type.equals(NBTType.LIST)) {
				if (!thisKeys.contains(key)) { // don't replace existing values
					NBTList list = reference.getList(key);
					if (list.size() != 0) { // don't replace empty lists
						NBTType listType = list.getValueType();
						ConfigGCore.logspamItemNbt(this, () -> "  Copying list " + key);
						NBTList copy = new NBTList(this, key, getDepth() + 1, NBTType.createListTag(listType));
						for (int i = 0; i < list.size(); ++i) {
							copy.add(listType, list.get(i, listType));
						}
						setList(key, copy);
					}
				}
			}
			// object
			else if (type.equals(NBTType.COMPOUND)) {
				if (!thisKeys.contains(key)) { // new compound, add it
					ConfigGCore.logspamItemNbt(this, () -> "  Creating empty compound " + key + " and copying");
					setCompound(key, new NBTCompound(this, key, getDepth() + 1, NBTType.createCompoundTag()));
				} else {
					ConfigGCore.logspamItemNbt(this, () -> "  Copying to existing compound " + key);
				}
				getCompound(key).addNonClonedElementsFrom(reference.getCompound(key));
			}
			// value
			else if (!type.equals(NBTType.UNKNOWN)) {
				if (!thisKeys.contains(key)) { // don't replace existing values
					ConfigGCore.logspamItemNbt(this, () -> "  Copying value " + key + " : " + reference.get(key));
					set(key, reference.get(key));
				}
			}
		}
		// seems good
		return true;
	}

}
