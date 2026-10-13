package com.guillaumevdn.gcore.lib.compatibility.sound;

import java.util.List;

import com.guillaumevdn.gcore.lib.compatibility.Version;
import com.guillaumevdn.gcore.lib.compatibility.sound.SoundData.SoundExtra;
import com.guillaumevdn.gcore.lib.compatibility.variants.Variants;
import com.guillaumevdn.gcore.lib.logic.ComparisonType;
import com.guillaumevdn.gcore.lib.object.ObjectUtils;

/**
 * @author GuillaumeVDN
 */
public final class Sounds extends Variants<Sound, SoundExtra, SoundData> {

	public Sounds(boolean regenerate) {
		super("sound", Sound.class, SoundData.class, SoundExtra.class, regenerate, false);
	}

	// ----- load
	@Override
	public SoundData loadElementConfigAndCreateData(Version version, ComparisonType comparison, List<SoundExtra> extra, String rawData) throws Throwable {
		return new SoundData(version, comparison, rawData, ObjectUtils.soundTypeOrNull(rawData), extra);
	}

	@Override
	protected boolean isMissingDataFatal() {
		return false;
	}

	@Override
	protected Sound createElement(String id, SoundData data) throws Throwable {
		return new Sound(id, data);
	}

}
