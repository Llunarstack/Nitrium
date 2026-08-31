package dev.nitrium.client.profiling;

import dev.nitrium.config.HardwareProfile;
import dev.nitrium.NitriumMod;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

import java.util.Locale;

/**
 * Reads GPU vendor, renderer string, and dedicated VRAM where driver extensions allow.
 */
public final class GpuCapabilities {
	private static GpuCapabilities instance;

	private final String vendor;
	private final String renderer;
	private final String version;
	private final int dedicatedVramMb;
	private final boolean vramQuerySupported;
	private final HardwareProfile hardwareProfile;

	private GpuCapabilities(
			String vendor,
			String renderer,
			String version,
			int dedicatedVramMb,
			boolean vramQuerySupported
	) {
		this.vendor = vendor;
		this.renderer = renderer;
		this.version = version;
		this.dedicatedVramMb = dedicatedVramMb;
		this.vramQuerySupported = vramQuerySupported;
		this.hardwareProfile = HardwareProfile.fromVramMb(dedicatedVramMb);
	}

	public static GpuCapabilities get() {
		return instance;
	}

	public static void probe() {
		if (instance != null) {
			return;
		}

		String vendor = safeString(GL11.glGetString(GL11.GL_VENDOR));
		String renderer = safeString(GL11.glGetString(GL11.GL_RENDERER));
		String version = safeString(GL11.glGetString(GL11.GL_VERSION));

		GLCapabilities capabilities = GL.getCapabilities();
		int dedicatedVramMb = 0;
		boolean vramQuerySupported = false;

		if (capabilities != null && capabilities.GL_NVX_gpu_memory_info) {
			int totalKb = GL11.glGetInteger(0x9048); // GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX
			if (totalKb > 0) {
				dedicatedVramMb = totalKb / 1024;
				vramQuerySupported = true;
			}
		} else if (capabilities != null && capabilities.GL_ATI_meminfo) {
			int[] info = new int[4];
			GL11.glGetIntegerv(0x87FB, info); // GL_VBO_FREE_MEMORY_ATI
			int totalKb = info[0] + info[1] + info[2] + info[3];
			if (totalKb > 0) {
				dedicatedVramMb = totalKb / 1024;
				vramQuerySupported = true;
			}
		}

		instance = new GpuCapabilities(vendor, renderer, version, dedicatedVramMb, vramQuerySupported);
		NitriumMod.LOGGER.info(
				"GPU probe: {} / {} ({} MB VRAM, profile={}, vramQuery={})",
				vendor,
				renderer,
				dedicatedVramMb > 0 ? dedicatedVramMb : "unknown",
				instance.hardwareProfile,
				vramQuerySupported
		);
	}

	private static String safeString(String value) {
		return value == null ? "unknown" : value;
	}

	public String vendor() {
		return vendor;
	}

	public String renderer() {
		return renderer;
	}

	public String version() {
		return version;
	}

	public int dedicatedVramMb() {
		return dedicatedVramMb;
	}

	public boolean vramQuerySupported() {
		return vramQuerySupported;
	}

	public HardwareProfile hardwareProfile() {
		return hardwareProfile;
	}

	public boolean isNvidia() {
		return vendor.toLowerCase(Locale.ROOT).contains("nvidia");
	}

	public boolean isAmd() {
		return vendor.toLowerCase(Locale.ROOT).contains("ati")
				|| vendor.toLowerCase(Locale.ROOT).contains("amd");
	}

	/**
	 * Recommended shadow map ceiling for structural governor decisions.
	 */
	public int recommendedMaxShadowMapResolution() {
		return hardwareProfile.maxShadowMapResolution();
	}
}
