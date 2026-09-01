package dev.nitrium.client.profiling;

import dev.nitrium.config.GpuVendor;
import dev.nitrium.config.HardwareProfile;
import dev.nitrium.config.HardwareTuner;
import dev.nitrium.Nitrium;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

import java.util.Locale;

/**
 * Reads GPU vendor, renderer string, dedicated VRAM, and extension support for vendor-specific tuning.
 */
public final class GpuCapabilities {
	private static GpuCapabilities instance;

	private final GpuVendor vendor;
	private final String vendorString;
	private final String renderer;
	private final String version;
	private final int dedicatedVramMb;
	private final boolean vramQuerySupported;
	private final boolean integratedGpu;
	private final boolean computeShaders;
	private final boolean persistentBuffers;
	private final HardwareProfile hardwareProfile;

	private GpuCapabilities(
			GpuVendor vendor,
			String vendorString,
			String renderer,
			String version,
			int dedicatedVramMb,
			boolean vramQuerySupported,
			boolean integratedGpu,
			boolean computeShaders,
			boolean persistentBuffers
	) {
		this.vendor = vendor;
		this.vendorString = vendorString;
		this.renderer = renderer;
		this.version = version;
		this.dedicatedVramMb = dedicatedVramMb;
		this.vramQuerySupported = vramQuerySupported;
		this.integratedGpu = integratedGpu;
		this.computeShaders = computeShaders;
		this.persistentBuffers = persistentBuffers;
		this.hardwareProfile = HardwareProfile.resolve(vendor, dedicatedVramMb, integratedGpu, renderer);
	}

	public static GpuCapabilities get() {
		return instance;
	}

	public static void probe() {
		if (instance != null) {
			return;
		}

		String vendorString = safeString(GL11.glGetString(GL11.GL_VENDOR));
		String renderer = safeString(GL11.glGetString(GL11.GL_RENDERER));
		String version = safeString(GL11.glGetString(GL11.GL_VERSION));

		GpuVendor vendor = detectVendor(vendorString, renderer);
		boolean integrated = detectIntegrated(vendor, renderer);

		GLCapabilities capabilities = GL.getCapabilities();
		int dedicatedVramMb = estimateVramMb(capabilities, vendor, integrated);
		boolean vramQuerySupported = dedicatedVramMb > 0;
		boolean computeShaders = capabilities != null && capabilities.OpenGL43;
		boolean persistentBuffers = capabilities != null && capabilities.GL_ARB_buffer_storage;

		instance = new GpuCapabilities(
				vendor,
				vendorString,
				renderer,
				version,
				dedicatedVramMb,
				vramQuerySupported,
				integrated,
				computeShaders,
				persistentBuffers
		);

		HardwareTuner.tuneClient(instance);

		Nitrium.LOGGER.info(
				"GPU probe: {} / {} ({} MB VRAM, integrated={}, profile={}, compute={}, persistent={})",
				vendorString,
				renderer,
				dedicatedVramMb > 0 ? dedicatedVramMb : "unknown",
				integrated,
				instance.hardwareProfile,
				computeShaders,
				persistentBuffers
		);
	}

	private static int estimateVramMb(GLCapabilities capabilities, GpuVendor vendor, boolean integrated) {
		if (capabilities != null && capabilities.GL_NVX_gpu_memory_info) {
			int totalKb = GL11.glGetInteger(0x9048); // GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX
			if (totalKb > 0) {
				return totalKb / 1024;
			}
		}

		if (capabilities != null && capabilities.GL_ATI_meminfo) {
			int[] info = new int[4];
			GL11.glGetIntegerv(0x87FB, info); // GL_VBO_FREE_MEMORY_ATI
			int totalKb = info[0] + info[1] + info[2] + info[3];
			if (totalKb > 0) {
				return totalKb / 1024;
			}
		}

		// Intel integrated GPUs rarely expose dedicated VRAM queries — assume shared memory tier.
		if (vendor == GpuVendor.INTEL || integrated) {
			long heapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
			if (heapMb > 0) {
				return (int) Math.min(2048, Math.max(512, heapMb / 4));
			}
			return 1024;
		}

		return 0;
	}

	private static GpuVendor detectVendor(String vendor, String renderer) {
		String combined = (vendor + " " + renderer).toLowerCase(Locale.ROOT);
		if (combined.contains("nvidia") || combined.contains("geforce") || combined.contains("quadro")) {
			return GpuVendor.NVIDIA;
		}
		if (combined.contains("amd") || combined.contains("ati") || combined.contains("radeon")) {
			return GpuVendor.AMD;
		}
		if (combined.contains("intel") || combined.contains("iris") || combined.contains("uhd")
				|| combined.contains("arc")) {
			return GpuVendor.INTEL;
		}
		return GpuVendor.UNKNOWN;
	}

	private static boolean detectIntegrated(GpuVendor vendor, String renderer) {
		if (vendor == GpuVendor.INTEL) {
			String lower = renderer.toLowerCase(Locale.ROOT);
			return !lower.contains("arc");
		}
		return false;
	}

	private static String safeString(String value) {
		return value == null ? "unknown" : value;
	}

	public GpuVendor vendor() {
		return vendor;
	}

	public String vendorString() {
		return vendorString;
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

	public boolean integratedGpu() {
		return integratedGpu;
	}

	public boolean computeShadersSupported() {
		return computeShaders;
	}

	public boolean persistentBuffersSupported() {
		return persistentBuffers;
	}

	public HardwareProfile hardwareProfile() {
		return hardwareProfile;
	}

	public boolean isNvidia() {
		return vendor == GpuVendor.NVIDIA;
	}

	public boolean isAmd() {
		return vendor == GpuVendor.AMD;
	}

	public boolean isIntel() {
		return vendor == GpuVendor.INTEL;
	}

	public int recommendedMaxShadowMapResolution() {
		return hardwareProfile.maxShadowMapResolution();
	}
}
