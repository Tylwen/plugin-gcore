package com.guillaumevdn.gcore.lib.compatibility;

import com.guillaumevdn.gcore.lib.collection.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author GuillaumeVDN
 */
public enum Version {
    // ----- values ; versions are checked from bottom to top so it'll take latest if necessary

    UNKNOWN(CollectionUtils.asList("UNKNOWN"), null),

    MC_1_7_R1(CollectionUtils.asList("1.7.2"), "v1_7_R1"),
    MC_1_7_R3(CollectionUtils.asList("1.7.9"), "v1_7_R3"),
    MC_1_7_R4(CollectionUtils.asList("1.7.10"), "v1_7_R4"),

    MC_1_8_R3(CollectionUtils.asList("1.8"), "v1_8_R3"),
    MC_1_9_R2(CollectionUtils.asList("1.9"), "v1_9_R2"),
    MC_1_10_R1(CollectionUtils.asList("1.10"), "v1_10_R1"),
    MC_1_11_R1(CollectionUtils.asList("1.11"), "v1_11_R1"),
    MC_1_12_R1(CollectionUtils.asList("1.12"), "v1_12_R1"),
    MC_1_13_R2(CollectionUtils.asList("1.13"), "v1_13_R2"),
    MC_1_14_R1(CollectionUtils.asList("1.14"), "v1_14_R1"),
    MC_1_15_R1(CollectionUtils.asList("1.15"), "v1_15_R1"),

    MC_1_16_R1(CollectionUtils.asList("1.16"), "v1_16_R1"),
    MC_1_16_R2(CollectionUtils.asList("1.16.2", "1.16.3"), "v1_16_R2"),
    MC_1_16_R3(CollectionUtils.asList("1.16.4", "1.16.5"), "v1_16_R3"),

    MC_1_17_R1(CollectionUtils.asList("1.17"), "v1_17_R1", true),
    MC_1_17_R2(CollectionUtils.asList("1.17.1"), "v1_17_R1", true),

    MC_1_18_R1(CollectionUtils.asList("1.18"), "v1_18_R1", true),
    MC_1_18_R2(CollectionUtils.asList("1.18.2"), "v1_18_R2", true),

    MC_1_19_R1(CollectionUtils.asList("1.19"), "v1_19_R1", true),
    MC_1_19_R2(CollectionUtils.asList("1.19.3"), "v1_19_R2", true),
    MC_1_19_R3(CollectionUtils.asList("1.19.4"), "v1_19_R3", true),

    MC_1_20_R1(CollectionUtils.asList("1.20", "1.20.1"), "v1_20_R1", true),
    MC_1_20_R2(CollectionUtils.asList("1.20.2"), "v1_20_R2", true),
    MC_1_20_R3(CollectionUtils.asList("1.20.4"), "v1_20_R3", true),
    MC_1_20_R4(CollectionUtils.asList("1.20.5"), "v1_20_R4", true),
    
    UNSUPPORTED(CollectionUtils.asList("UNSUPPORTED"), null)  // this will be used if we find a package name that's not listed above (a not yet updated newer version most likely)
    ;

    public static final Version CURRENT = VersionUtils.getCurrent();
    public static final boolean IS_1_7 = CURRENT.names.get(0).contains("1.7");
    public static final boolean ATLEAST_1_7_2 = CURRENT.isMoreOrEqualsTo(MC_1_7_R1);
    public static final boolean ATLEAST_1_7_9 = CURRENT.isMoreOrEqualsTo(MC_1_7_R3);
    public static final boolean ATLEAST_1_7_10 = CURRENT.isMoreOrEqualsTo(MC_1_7_R4);
    public static final boolean ATLEAST_1_8 = CURRENT.isMoreOrEqualsTo(MC_1_8_R3);
    public static final boolean ATLEAST_1_9 = CURRENT.isMoreOrEqualsTo(MC_1_9_R2);
    public static final boolean ATLEAST_1_10 = CURRENT.isMoreOrEqualsTo(MC_1_10_R1);
    public static final boolean ATLEAST_1_11 = CURRENT.isMoreOrEqualsTo(MC_1_11_R1);
    public static final boolean ATLEAST_1_12 = CURRENT.isMoreOrEqualsTo(MC_1_12_R1);
    public static final boolean ATLEAST_1_13 = CURRENT.isMoreOrEqualsTo(MC_1_13_R2);
    public static final boolean ATLEAST_1_14 = CURRENT.isMoreOrEqualsTo(MC_1_14_R1);
    public static final boolean ATLEAST_1_15 = CURRENT.isMoreOrEqualsTo(MC_1_15_R1);
    public static final boolean ATLEAST_1_16 = CURRENT.isMoreOrEqualsTo(MC_1_16_R1);
    public static final boolean ATLEAST_1_17 = CURRENT.isMoreOrEqualsTo(MC_1_17_R1);
    public static final boolean ATLEAST_1_17_1 = CURRENT.isMoreOrEqualsTo(MC_1_17_R2);
    public static final boolean ATLEAST_1_18 = CURRENT.isMoreOrEqualsTo(MC_1_18_R1);
    public static final boolean ATLEAST_1_19 = CURRENT.isMoreOrEqualsTo(MC_1_19_R1);
    public static final boolean ATLEAST_1_19_3 = CURRENT.isMoreOrEqualsTo(MC_1_19_R2);
    public static final boolean ATLEAST_1_20 = CURRENT.isMoreOrEqualsTo(MC_1_20_R1);
    public static final boolean ATLEAST_1_20_2 = CURRENT.isMoreOrEqualsTo(MC_1_20_R2);
    public static final boolean ATLEAST_1_20_5 = CURRENT.isMoreOrEqualsTo(MC_1_20_R4);
    public static final boolean REMAPPED = CURRENT.useRemappedNMSPackages();

    private List<String> names;
    String nmsPackage, craftbukkitPackage;
    private boolean remappedNMSPackages;

    Version(List<String> names, String craftbukkitPackage) {
        this(names, craftbukkitPackage, false);
    }

    Version(List<String> names, String craftbukkitPackage, boolean remappedNMSPackages) {
        this.names = Collections.unmodifiableList(names);
        this.nmsPackage = remappedNMSPackages ? "net.minecraft" : "net.minecraft.server." + craftbukkitPackage;
        this.craftbukkitPackage = "org.bukkit.craftbukkit." + craftbukkitPackage;
        this.remappedNMSPackages = remappedNMSPackages;
    }

    // ----- get
    public List<String> getNames() {
        return names;
    }

    public String getNMSPackage() {
        return nmsPackage;
    }

    public String getCraftbukkitPackage() {
        return craftbukkitPackage;
    }

    public boolean useRemappedNMSPackages() {
        return remappedNMSPackages;
    }

    public boolean isLessThan(Version version) {
        return compareTo(version) < 0;
    }

    public boolean isLessOrEqualsTo(Version version) {
        return compareTo(version) <= 0;
    }

    public boolean isMoreThan(Version version) {
        return compareTo(version) > 0;
    }

    public boolean isMoreOrEqualsTo(Version version) {
        return compareTo(version) >= 0;
    }

    public boolean isCurrent() {
        return equals(CURRENT);
    }

    // ----- static methods
    public static Version fromNameOrNull(String name) {
        for (Version version : Version.values()) {
            if (CollectionUtils.containsIgnoreCase(version.names, name)) {
                return version;
            }
        }
        return null;
    }

}
