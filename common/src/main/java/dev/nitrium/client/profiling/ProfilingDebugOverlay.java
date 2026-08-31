package dev.nitrium.client.profiling;

import dev.nitrium.NitriumMod;
import dev.nitrium.client.culling.CullingPipeline;
import dev.nitrium.client.entity.EntityRenderOptimizer;
import dev.nitrium.client.governor.QualityGovernor;
import dev.nitrium.config.HardwareProfile;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.client.platform.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Optional overlay showing live Nitrium metrics (before the demo timer / debug HUD layer).
 */
public final class ProfilingDebugOverlay {
	private ProfilingDebugOverlay() {
	}

	public static void register() {
		ClientEvents.get().hud(
				Identifier.fromNamespaceAndPath(NitriumMod.MOD_ID, "profiling"),
				ProfilingDebugOverlay::render
		);
	}

	private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
		if (!NitriumConfigManager.get().enableDebugOverlay) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.options.hideGui) {
			return;
		}

		PerformanceMonitor monitor = PerformanceMonitor.get();
		FrameMetrics metrics = monitor.latestMetrics();
		GpuCapabilities gpu = GpuCapabilities.get();

		int y = 4;
		int color = 0xE0FFFFFF;
		int lineHeight = 10;

		draw(graphics, y, color, "Nitrium Profiler");
		y += lineHeight;
		draw(graphics, y, color, String.format("FPS: %.1f (avg %.1f)", metrics.instantaneousFps(), monitor.averageFps()));
		y += lineHeight;
		draw(graphics, y, color, String.format(
				"GPU: %.2f ms | Tick CPU: %.2f ms | Render CPU: %.2f ms",
				metrics.gpuWorldRenderMs(),
				metrics.clientTickCpuMs(),
				metrics.worldRenderCpuMs()
		));
		y += lineHeight;
		draw(graphics, y, color, "Bottleneck: " + metrics.bottleneck());

		if (gpu != null) {
			y += lineHeight;
			HardwareProfile profile = gpu.hardwareProfile();
			draw(graphics, y, color, String.format(
					"GPU: %s | VRAM: %s MB | Profile: %s",
					gpu.renderer(),
					gpu.dedicatedVramMb() > 0 ? Integer.toString(gpu.dedicatedVramMb()) : "?",
					profile
			));
			y += lineHeight;
			draw(graphics, y, color, String.format(
					"Shadow ceiling: %dpx @ %d blocks",
					profile.maxShadowMapResolution(),
					profile.maxShadowDistanceBlocks()
			));
		}

		y += lineHeight;
		draw(graphics, y, color, "Iris: " + (NitriumProfiler.get() != null && NitriumProfiler.get().isIrisLoaded() ? "yes" : "no"));

		QualityGovernor governor = QualityGovernor.get();
		y += lineHeight;
		draw(graphics, y, color, String.format(
				"Governor: shader %s (max %s) | scale %.0f%% | render dist %d ch",
				governor.shaderLevel(),
				governor.shaderCeiling(),
				governor.currentRenderScale() * 100.0f,
				governor.renderDistanceChunks()
		));
		y += lineHeight;
		draw(graphics, y, color, "Shader: " + governor.shaderProfile().summary()
				+ (governor.isIrisGoverned() ? " [iris]" : " [vanilla]"));

		CullingPipeline culling = CullingPipeline.get();
		if (culling != null) {
			var cullStats = culling.stats();
			y += lineHeight;
			draw(graphics, y, color, String.format(
					"Cull: HiZ %.0f%% | Shadow %.0f%% | Entity %.0f%%",
					cullStats.sectionHiZCullRate() * 100.0f,
					cullStats.shadowCullRate() * 100.0f,
					cullStats.entityCullRate() * 100.0f
			));
			y += lineHeight;
			draw(graphics, y, color, String.format(
					"Tested: %d sections, %d entities | Foliage opt: %d",
					cullStats.sectionsTested(),
					cullStats.entitiesTested(),
					cullStats.foliageSectionsOptimized()
			));
		}

		dev.nitrium.client.entity.EntityRenderCuller entityCuller = dev.nitrium.client.entity.EntityRenderCuller.get();
		y += lineHeight;
		draw(graphics, y, color, String.format(
				"Entity dist-cull: %d/%d (%.0f%%)",
				entityCuller.culled(), entityCuller.tested(), entityCuller.cullRate() * 100.0f
		));

		EntityRenderOptimizer entityOpt = EntityRenderOptimizer.get();
		if (entityOpt != null) {
			var entityStats = entityOpt.clientStats();
			y += lineHeight;
			draw(graphics, y, color, String.format(
					"Entities: %d rendered | %d anim-frozen | instanced: %d",
					entityStats.entitiesIndexed() - entityStats.ticksSkipped(),
					entityStats.dormantEntities(),
					entityOpt.instanceBatch().totalInstances()
			));
		}
	}

	private static void draw(GuiGraphics graphics, int y, int color, String text) {
		graphics.drawString(Minecraft.getInstance().font, Component.literal(text), 4, y, color, true);
	}
}
