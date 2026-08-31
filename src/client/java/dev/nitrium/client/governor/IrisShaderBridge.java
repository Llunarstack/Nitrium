package dev.nitrium.client.governor;

import dev.nitrium.NitriumMod;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Talks to Iris through its stable {@code net.irisshaders.iris.api.v0} API by reflection, so Iris
 * stays an optional dependency. That API is coarse — pack-in-use plus a global shaders toggle — so
 * this only pulls the emergency lever: when the governor bottoms out at
 * {@link ShaderQualityLevel#SURVIVAL} under sustained GPU load it turns shaders off, and turns them
 * back on once quality recovers to {@link ShaderQualityLevel#BALANCED}. It only re-enables shaders
 * it disabled itself, so it never overrides someone who turned them off by hand. Per-option
 * overrides (shadow distance, volumetrics, SSR) aren't in the stable API and stay in
 * {@link ShaderProfile} for later. Any reflection failure is a silent no-op.
 */
public final class IrisShaderBridge {
	private boolean resolved;
	private boolean available;
	private Method getInstance;
	private Method isShaderPackInUse;
	private Method getConfig;
	private Method areShadersEnabled;
	private Method setShadersEnabledAndApply;
	private Object apiInstance;

	private boolean weDisabledShaders;

	private void resolve() {
		if (resolved) {
			return;
		}
		resolved = true;

		if (!FabricLoader.getInstance().isModLoaded("iris")) {
			return;
		}

		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Class<?> configClass = Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig");

			getInstance = apiClass.getMethod("getInstance");
			isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
			getConfig = apiClass.getMethod("getConfig");
			areShadersEnabled = configClass.getMethod("areShadersEnabled");
			setShadersEnabledAndApply = configClass.getMethod("setShadersEnabledAndApply", boolean.class);

			apiInstance = getInstance.invoke(null);
			available = apiInstance != null;
			if (available) {
				NitriumMod.LOGGER.info("Nitrium Iris bridge connected (v0 API)");
			}
		} catch (ReflectiveOperationException | LinkageError failure) {
			available = false;
			NitriumMod.LOGGER.debug("Iris v0 API unavailable — shader governor limited to vanilla graphics", failure);
		}
	}

	public boolean isAvailable() {
		resolve();
		return available;
	}

	public boolean isShaderPackInUse() {
		if (!isAvailable()) {
			return false;
		}
		try {
			return (boolean) isShaderPackInUse.invoke(apiInstance);
		} catch (ReflectiveOperationException failure) {
			return false;
		}
	}

	/**
	 * Reflect the governor level onto Iris' global shader toggle at the extremes.
	 *
	 * @return {@code true} if the shader-enabled state was changed
	 */
	public boolean apply(ShaderProfile profile) {
		if (!isShaderPackInUse()) {
			return false;
		}

		Boolean enabled = shadersEnabled();
		if (enabled == null) {
			return false;
		}

		if (profile.level() == ShaderQualityLevel.SURVIVAL && enabled) {
			if (setShadersEnabled(false)) {
				weDisabledShaders = true;
				NitriumMod.LOGGER.info("Nitrium Iris bridge: disabled shaders (emergency — sustained GPU deficit)");
				return true;
			}
		} else if (weDisabledShaders && !enabled
				&& profile.level().ordinal() >= ShaderQualityLevel.BALANCED.ordinal()) {
			if (setShadersEnabled(true)) {
				weDisabledShaders = false;
				NitriumMod.LOGGER.info("Nitrium Iris bridge: re-enabled shaders (headroom recovered)");
				return true;
			}
		}
		return false;
	}

	private Boolean shadersEnabled() {
		try {
			Object config = getConfig.invoke(apiInstance);
			if (config == null) {
				return null;
			}
			return (boolean) areShadersEnabled.invoke(config);
		} catch (ReflectiveOperationException failure) {
			return null;
		}
	}

	private boolean setShadersEnabled(boolean value) {
		try {
			Object config = getConfig.invoke(apiInstance);
			if (config == null) {
				return false;
			}
			setShadersEnabledAndApply.invoke(config, value);
			return true;
		} catch (ReflectiveOperationException failure) {
			return false;
		}
	}

	public void reset() {
		weDisabledShaders = false;
	}
}
